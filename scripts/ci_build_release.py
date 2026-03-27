from __future__ import annotations

import argparse
import os
import pathlib
import sys

from ci_common import append_github_output, repo_root, run


def gradlew_name() -> str:
    return "gradlew.bat" if sys.platform.startswith("win") else "./gradlew"


def python_name() -> str:
    return "python"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--version-tag", required=True)
    parser.add_argument("--apk-path", default="app/build/outputs/apk/release/app-release.apk")
    args = parser.parse_args()

    root = repo_root()
    env = os.environ.copy()
    env["APP_VERSION_TAG"] = args.version_tag

    print("Generate prebuilt seed database")
    run([python_name(), "scripts/generate_seed_db.py"], cwd=root, env=env)

    print("Build release APK")
    run([gradlew_name(), "assembleRelease"], cwd=root, env=env)

    apk_path = root / args.apk_path
    if not apk_path.exists():
        raise SystemExit(f"APK not found: {apk_path}")

    print(f"APK created: {apk_path}")
    append_github_output("apk_path", str(apk_path))


if __name__ == "__main__":
    main()
