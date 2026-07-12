import { afterEach, beforeEach, type MockInstance, vi } from "vitest";

type ConsoleMethod = "error" | "warn";

interface UnexpectedConsoleCall {
  args: unknown[];
  method: ConsoleMethod;
}

let unexpectedCalls: UnexpectedConsoleCall[] = [];
let consoleSpies: MockInstance[] = [];

const capture = (method: ConsoleMethod) =>
  vi.spyOn(console, method).mockImplementation((...args: unknown[]) => {
    unexpectedCalls.push({ args, method });
  });

const formatArgument = (argument: unknown): string => {
  if (argument instanceof Error) {
    return argument.stack ?? argument.message;
  }
  return typeof argument === "string" ? argument : String(argument);
};

beforeEach(() => {
  unexpectedCalls = [];
  consoleSpies = [capture("error"), capture("warn")];
});

afterEach(() => {
  consoleSpies.forEach((spy) => spy.mockRestore());

  if (unexpectedCalls.length === 0) {
    return;
  }

  const details = unexpectedCalls
    .map(
      ({ args, method }, index) =>
        `${index + 1}. console.${method}: ${args.map(formatArgument).join(" ")}`,
    )
    .join("\n");

  throw new Error(
    `Unexpected console output. Assert intentional warnings/errors explicitly.\n${details}`,
  );
});
