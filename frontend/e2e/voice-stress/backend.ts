import { spawn } from "node:child_process";
import path from "node:path";
import { createConnection, createServer } from "node:net";

async function catalogAvailable() {
  return new Promise<boolean>((resolve) => {
    const socket = createConnection({ host: "localhost", port: 8080 });
    const finish = (available: boolean) => {
      socket.destroy();
      resolve(available);
    };
    socket.once("connect", () => finish(true));
    socket.once("error", () => finish(false));
    socket.setTimeout(500, () => finish(false));
  });
}

/** Separate local process: do not restart the developer's active microphone session. */
export async function startBackend() {
  await new Promise<void>((resolve, reject) => {
    const probe = createServer();
    probe.once("error", () =>
      reject(
        new Error("Stress port 8096 is occupied; no process was changed."),
      ),
    );
    probe.listen(8096, "127.0.0.1", () => probe.close(() => resolve()));
  });
  const logs: object[] = [];
  let catalogInterrupted = false;
  let monitoring = true;
  let lastAvailability: boolean | undefined;
  const sampleCatalog = async () => {
    const available = await catalogAvailable();
    if (!monitoring) return;
    catalogInterrupted ||= !available;
    if (available !== lastAvailability) {
      logs.push({
        event: "stress_catalog_availability",
        outcome: available ? "up" : "down",
        timestamp: new Date().toISOString(),
      });
      lastAvailability = available;
    }
  };
  await sampleCatalog();
  const healthMonitor = setInterval(() => void sampleCatalog(), 1000);
  const stopMonitoring = () => {
    monitoring = false;
    clearInterval(healthMonitor);
  };
  const child = spawn(
    "uv",
    [
      "run",
      "--locked",
      "uvicorn",
      "server:create_stress_app",
      "--factory",
      "--app-dir",
      "evals/voice/stress",
      "--host",
      "127.0.0.1",
      "--port",
      "8096",
      "--no-access-log",
    ],
    {
      cwd: path.resolve("../agent"),
      detached: true, // Own process group includes uv and its Python child.
      env: {
        ...process.env,
        IMDB_AGENT_VOICE_ENABLED: "true",
        IMDB_AGENT_VOICE_LIVE_ENABLED: "true",
      },
      stdio: ["ignore", "pipe", "pipe"],
    },
  );
  let pending = "";
  child.stdout.on("data", (chunk: Buffer) => {
    pending += chunk.toString();
    const lines = pending.split("\n");
    pending = lines.pop() ?? "";
    for (const line of lines) {
      try {
        const event: unknown = JSON.parse(line);
        if (event && typeof event === "object") logs.push(event);
      } catch {
        /* Only retain the app's structured, allowlisted telemetry. */
      }
    }
  });
  child.stderr.resume(); // Do not save SDK exceptions or unrestricted HTTP diagnostics.
  let spawnError = false;
  child.on("error", () => {
    spawnError = true;
  });
  const stop = async () => {
    stopMonitoring();
    if (spawnError || !child.pid) return;
    const signalGroup = (signal: NodeJS.Signals) => {
      try {
        process.kill(-child.pid!, signal);
      } catch (error) {
        if ((error as NodeJS.ErrnoException).code !== "ESRCH") throw error;
      }
    };
    signalGroup("SIGTERM");
    await Promise.race([
      new Promise<void>((resolve) => child.once("exit", () => resolve())),
      new Promise<void>((resolve) =>
        setTimeout(() => {
          signalGroup("SIGKILL");
          resolve();
        }, 5000),
      ),
    ]);
  };
  try {
    for (let attempt = 0; attempt < 100; attempt++) {
      if (spawnError || child.exitCode !== null)
        throw new Error(
          "Stress backend could not start; check keys and that port 8096 is free.",
        );
      try {
        const response = await fetch("http://127.0.0.1:8096/healthz", {
          signal: AbortSignal.timeout(500),
        });
        if (
          response.ok &&
          logs.some((event) =>
            JSON.stringify(event).includes("stress_backend_ready"),
          )
        )
          return {
            logs,
            stop,
            stopMonitoring,
            get catalogInterrupted() {
              return catalogInterrupted;
            },
          };
      } catch {
        /* Startup is still in progress. */
      }
      await new Promise((resolve) => setTimeout(resolve, 200));
    }
    throw new Error("Stress backend startup timed out");
  } catch (error) {
    await stop();
    throw error;
  }
}
