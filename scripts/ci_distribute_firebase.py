from __future__ import annotations

import argparse
import os
import pathlib
import sys

from ci_common import repo_root, run


def gradlew_name() -> str:
    return "gradlew.bat" if sys.platform.startswith("win") else "./gradlew"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--version-tag", required=True)
    parser.add_argument("--artifact-path", default="app/build/outputs/apk/release/app-release.apk")
    parser.add_argument("--credentials-path", default="")
    args = parser.parse_args()

    root = repo_root()
    artifact_path = root / args.artifact_path
    if not artifact_path.exists():
        raise SystemExit(f"Artifact path does not exist: {artifact_path}")

    credentials_path = pathlib.Path(args.credentials_path or os.environ.get("GOOGLE_APPLICATION_CREDENTIALS", ""))
    if not credentials_path.exists():
        raise SystemExit(f"Service credentials file does not exist: {credentials_path}")

    env = os.environ.copy()
    env["APP_VERSION_TAG"] = args.version_tag
    env["FIREBASE_DISTRIBUTION_ARTIFACT_PATH"] = str(artifact_path)

    run(
        [
            gradlew_name(),
            "appDistributionUploadRelease",
            f"-PfirebaseServiceCredentialsFile={credentials_path}",
            f"-PfirebaseArtifactPath={artifact_path}",
        ],
        cwd=root,
        env=env,
    )


if __name__ == "__main__":
    main()
