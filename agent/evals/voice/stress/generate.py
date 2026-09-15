"""Regenerate synthetic English stress fixtures locally with macOS's stock voice."""

import subprocess
from pathlib import Path

FIXTURES = {
    "open-forrest": "Please open the movie Forrest Gump.",
    "trailer-forrest": "Let me watch the trailer of Forrest Gump.",
    "open-interstellar": "Please open the movie Interstellar.",
    "trailer-interstellar": "Let me watch the trailer of Interstellar.",
    "open-arrival": "Please open the movie Arrival.",
    "details": "Tell me about Forrest Gump in detail, including its story and main characters.",
    "correction": "Stop. Instead, open the movie Interstellar.",
}


def main() -> None:
    for name, text in FIXTURES.items():
        subprocess.run(  # noqa: S603 - fixed local executable and synthetic fixture text
            [
                "/usr/bin/say",
                "-v",
                "Samantha",
                "-r",
                "175",
                "--file-format=WAVE",
                "--data-format=LEI16@24000",
                "-o",
                str(Path(__file__).with_name(name + ".wav")),
                text,
            ],
            check=True,
        )


if __name__ == "__main__":
    main()
