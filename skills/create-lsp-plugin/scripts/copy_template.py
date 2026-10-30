#!/usr/bin/env python3
"""Copy template sources, including the wrapper, without local build/IDE state."""

import argparse
from pathlib import Path
import shutil


EXCLUDED = (
    ".git", ".gradle", ".idea", ".intellijPlatform", ".kotlin",
    "build", "out", ".DS_Store", "*.iml", "__pycache__",
)
REQUIRED = (
    "README.md", "settings.gradle.kts", "build.gradle.kts", "gradle.properties",
    "gradlew", "gradlew.bat", "gradle/wrapper/gradle-wrapper.jar",
    "gradle/wrapper/gradle-wrapper.properties", "frontend/build.gradle.kts",
    "src/main/resources/META-INF/plugin.xml",
)


def copy_template(source: Path, destination: Path) -> None:
    template = source.expanduser().resolve() / "lsp-plugin-template"
    destination = destination.expanduser().absolute()
    if destination.exists() or destination.is_symlink():
        raise ValueError(f"Destination already exists: {destination}")
    if template == destination.resolve() or template in destination.resolve().parents:
        raise ValueError("Destination must be outside the source template")
    missing = [name for name in REQUIRED if not (template / name).is_file()]
    if missing:
        raise ValueError(f"Incomplete template at {template}; missing: {', '.join(missing)}")
    shutil.copytree(template, destination, ignore=shutil.ignore_patterns(*EXCLUDED))
    wrapper = destination / "gradlew"
    wrapper.chmod(wrapper.stat().st_mode | 0o111)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, required=True, help="Samples repository checkout")
    parser.add_argument("--destination", type=Path, required=True, help="New plugin directory")
    args = parser.parse_args()
    try:
        copy_template(args.source, args.destination)
    except (OSError, ValueError) as error:
        parser.exit(1, f"Cannot copy template: {error}\n")
    print(f"Copied template to {args.destination.expanduser().absolute()}")


if __name__ == "__main__":
    main()
