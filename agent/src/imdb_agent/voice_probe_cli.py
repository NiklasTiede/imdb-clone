"""Opt-in local replay of a synthetic voice fixture, with no production tools."""

from __future__ import annotations

import argparse
import asyncio
import logging
import wave
from pathlib import Path
from typing import TYPE_CHECKING

from pydantic import ValidationError

from imdb_agent.adapters.voice_probe import (
    SAMPLE_RATE,
    ProbeOptions,
    VoiceProbeError,
    build_voice_model,
    load_probe_audio,
    run_voice_probe,
)
from imdb_agent.settings import (
    ConfigurationError,
    DeploymentEnvironment,
    load_local_voice_secrets,
    load_settings,
)

if TYPE_CHECKING:
    from collections.abc import Sequence

FIXTURE = Path(__file__).resolve().parents[2] / "evals" / "voice" / "forrest-gump-en.wav"
ARTIFACTS = Path(__file__).resolve().parents[2] / ".artifacts" / "voice-probe"


def main(arguments: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Replay the synthetic English Grok voice probe.")
    parser.add_argument("--live", action="store_true", help="Allow one billable xAI session.")
    parser.add_argument("--session-seconds", type=float, default=45.0)
    parser.add_argument("--interrupt-after-audio-ms", type=int)
    parsed = parser.parse_args(arguments)
    if not parsed.live:
        print("Live probe disabled. Pass --live and set IMDB_AGENT_LIVE_EVALS_ENABLED=true.")
        return 2

    # This standalone CLI emits only its allowlisted report, never SDK/frame debug logs.
    previous_logging_level = logging.root.manager.disable
    logging.disable(logging.CRITICAL)
    try:
        settings = load_settings()
        if (
            not settings.live_evals_enabled
            or settings.environment is not DeploymentEnvironment.LOCAL
        ):
            print("Voice probe requires local mode and IMDB_AGENT_LIVE_EVALS_ENABLED=true.")
            return 2
        options = ProbeOptions(
            session_seconds=parsed.session_seconds,
            interrupt_after_audio_ms=parsed.interrupt_after_audio_ms,
        )
        audio = load_probe_audio(FIXTURE)
        model = build_voice_model(load_local_voice_secrets())
        result = asyncio.run(run_voice_probe(model, audio, options))
        # Fixed filenames bound local retention to the most recent run of each scenario.
        ARTIFACTS.mkdir(parents=True, exist_ok=True, mode=0o700)
        name = "interrupted" if options.interrupt_after_audio_ms is not None else "completed"
        with wave.open(str(ARTIFACTS / f"{name}.wav"), "wb") as output:
            output.setnchannels(1)
            output.setsampwidth(2)
            output.setframerate(SAMPLE_RATE)
            output.writeframes(result.audio)
        report = result.report.model_dump_json(indent=2)
        (ARTIFACTS / f"{name}.json").write_text(report + "\n", encoding="utf-8")
        print(report)
        return 0 if result.report.passed else 1
    except ConfigurationError as error:
        print(str(error))
        return 2
    except ValidationError:
        print("Invalid voice probe options.")
        return 2
    except VoiceProbeError as error:
        print(str(error))
        return 1
    except Exception:
        print("Voice probe failed; check local files and provider availability.")
        return 1
    finally:
        logging.disable(previous_logging_level)


if __name__ == "__main__":
    raise SystemExit(main())
