#!/usr/bin/env python3
"""Pin already-published app images and their compatible runtime naming together.

Only edits checked-in manifests; does not build, push or deploy anything. All inputs and
all three source manifests are validated before any file is written.
"""
import argparse
from pathlib import Path
import re

SERVICES = ("backend", "frontend", "agent")


def update_manifests(apps: Path, registry: str, version: str, digests: dict[str, str]) -> None:
    if not re.fullmatch(r"[a-z0-9]+(?:[._-][a-z0-9]+)*", registry):
        raise ValueError("registry must be a Docker Hub account name")
    if not re.fullmatch(r"\d+\.\d+\.\d+", version):
        raise ValueError("version must be semver without v")
    pending = {}
    for service in SERVICES:
        digest = digests[service]
        if not re.fullmatch(r"sha256:[0-9a-f]{64}", digest):
            raise ValueError(f"invalid {service} digest")
        path = apps / f"{service}.yaml"
        source = path.read_text()
        pattern = rf"(?m)^(\s*image: )[^\s/]+/(?:imdb-clone|popcorn-society)-{service}:v\d+\.\d+\.\d+@sha256:[0-9a-f]{{64}}$"
        reference = f"{registry}/popcorn-society-{service}:v{version}@{digest}"
        updated, count = re.subn(pattern, lambda m: m[1] + reference, source)
        if count != 1:
            raise ValueError(f"expected exactly one pinned {service} app image, got {count}")
        updated = updated.replace("name: IMDB_CLONE_", "name: POPCORN_SOCIETY_")
        updated = updated.replace("name: IMDB_AGENT_", "name: POPCORN_SOCIETY_AGENT_")
        if service == "backend":
            updated = updated.replace(
                "# Preserve the deployed log/trace identity until the observability migration.",
                "# Log, trace and profile identity changes with the compatible release image.",
            )
            for key in ("SPRING_APPLICATION_NAME", "PYROSCOPE_APPLICATION_NAME"):
                updated, count = re.subn(
                    rf"(name: {key}\n\s*value: )(?:imdb-clone|popcorn-society)-backend",
                    r"\g<1>popcorn-society-backend", updated,
                )
                if count != 1:
                    raise ValueError(f"expected exactly one {key}")
        pending[path] = updated
    for path, content in pending.items():
        path.write_text(content)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apps-dir", type=Path, default=Path("infrastructure/clusters/home/apps"))
    parser.add_argument("--registry", required=True)
    parser.add_argument("--version", required=True)
    for service in SERVICES:
        parser.add_argument(f"--{service}-digest", required=True)
    args = parser.parse_args()
    update_manifests(args.apps_dir, args.registry, args.version,
                     {service: getattr(args, f"{service}_digest") for service in SERVICES})


if __name__ == "__main__":
    main()
