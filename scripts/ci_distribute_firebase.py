from __future__ import annotations

import argparse
import json
import os
import pathlib
import sys

from ci_common import repo_root, run


def gradlew_name() -> str:
    return "gradlew.bat" if sys.platform.startswith("win") else "./gradlew"


def log(message: str) -> None:
    print(f"[firebase-distribute] {message}")


def normalize_service_account(raw_value: str) -> str:
    normalized = raw_value.strip().lstrip("\ufeff")
    if len(normalized) >= 2 and normalized[0] == normalized[-1] and normalized[0] in ("'", '"'):
        candidate = normalized[1:-1].strip()
        if candidate.startswith("{") and candidate.endswith("}"):
            return candidate
    return normalized


def describe_preview(value: str, length: int = 5) -> str:
    if not value:
        return "<empty>"
    preview = value[:length]
    parts: list[str] = []
    for char in preview:
        if char == "\ufeff":
            parts.append("BOM(U+FEFF)")
        elif char.isspace():
            parts.append(f"whitespace(U+{ord(char):04X})")
        else:
            parts.append(f"{char!r}(U+{ord(char):04X})")
    return ", ".join(parts)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--version-tag", required=True)
    parser.add_argument("--artifact-path", default="app/build/outputs/apk/release/app-release.apk")
    parser.add_argument("--credentials-path", default="")
    parser.add_argument("--credentials-file-name", default="firebase-service-account.json")
    parser.add_argument("--release-notes-file-name", default="release-notes.txt")
    parser.add_argument("--release-notes-path", default="")
    parser.add_argument("--note-line", action="append", default=[])
    args = parser.parse_args()

    root = repo_root()
    log("Checking Firebase distribution target env variables.")
    testers = os.environ.get("FIREBASE_APP_DISTRIBUTION_TESTERS", "").strip()
    groups = os.environ.get("FIREBASE_APP_DISTRIBUTION_GROUPS", "").strip()
    if not testers and not groups:
        raise SystemExit("Either FIREBASE_APP_DISTRIBUTION_GROUPS or FIREBASE_APP_DISTRIBUTION_TESTERS is required.")
    log("Firebase distribution target env variables are present.")

    artifact_path = (root / args.artifact_path).resolve()
    log(f"Checking artifact path: {artifact_path}")
    if not artifact_path.exists():
        raise SystemExit(f"Artifact path does not exist: {artifact_path}")
    log("Artifact file exists.")

    runner_temp = pathlib.Path(os.environ.get("RUNNER_TEMP", root / "build" / "tmp" / "firebase-ci")).resolve()
    runner_temp.mkdir(parents=True, exist_ok=True)

    raw_service_account = os.environ.get("FIREBASE_SERVICE_ACCOUNT", "")
    log("Checking FIREBASE_SERVICE_ACCOUNT environment variable.")
    if not raw_service_account.strip():
        raise SystemExit("FIREBASE_SERVICE_ACCOUNT is missing.")
    log("FIREBASE_SERVICE_ACCOUNT environment variable is present.")

    log("Checking whether FIREBASE_SERVICE_ACCOUNT looks like a JSON string.")
    normalized_service_account = normalize_service_account(raw_service_account)
    log(f"First significant characters of FIREBASE_SERVICE_ACCOUNT: {describe_preview(normalized_service_account)}")
    try:
        parsed_service_account = json.loads(normalized_service_account)
    except json.JSONDecodeError as exc:
        raise SystemExit(f"FIREBASE_SERVICE_ACCOUNT is not valid JSON: {exc}") from exc
    if not isinstance(parsed_service_account, dict):
        raise SystemExit("FIREBASE_SERVICE_ACCOUNT JSON must be an object.")
    for required_key in ("type", "project_id", "private_key", "client_email"):
        if not parsed_service_account.get(required_key):
            raise SystemExit(f"FIREBASE_SERVICE_ACCOUNT JSON is missing required key: {required_key}")
    log("FIREBASE_SERVICE_ACCOUNT JSON validation succeeded.")

    if args.credentials_path:
        credentials_path = pathlib.Path(args.credentials_path).resolve()
        log(f"Using provided credentials path: {credentials_path}")
        if not credentials_path.exists():
            raise SystemExit(f"Provided credentials path does not exist: {credentials_path}")
    else:
        credentials_path = (runner_temp / args.credentials_file_name).resolve()
        log(f"Creating credentials file: {credentials_path}")
        credentials_path.write_text(normalized_service_account, encoding="utf-8")
        log("Checking whether the credentials file was created.")
        if not credentials_path.exists():
            raise SystemExit(f"Credentials file was not created: {credentials_path}")
        log("Credentials file creation confirmed.")

    log("Checking whether the credentials file contents are valid JSON.")
    try:
        file_json = json.loads(credentials_path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        raise SystemExit(f"Credentials file content is not valid JSON: {exc}") from exc
    if not isinstance(file_json, dict) or file_json.get("client_email") != parsed_service_account.get("client_email"):
        raise SystemExit("Credentials file content does not match FIREBASE_SERVICE_ACCOUNT.")
    log("Credentials file content validation succeeded.")

    credentials_absolute = credentials_path.resolve()
    log(f"Resolved credentials absolute path: {credentials_absolute}")
    if not credentials_absolute.exists():
        raise SystemExit(f"Credentials file does not exist at absolute path: {credentials_absolute}")
    log("Credentials file exists at the resolved absolute path.")

    if args.release_notes_path:
        release_notes_path = pathlib.Path(args.release_notes_path).resolve()
        log(f"Using provided release notes file: {release_notes_path}")
        if not release_notes_path.exists():
            raise SystemExit(f"Provided release notes file does not exist: {release_notes_path}")
    else:
        release_notes_path = (runner_temp / args.release_notes_file_name).resolve()
        release_notes_lines = args.note_line or [f"Version: {args.version_tag}"]
        log(f"Creating release notes file: {release_notes_path}")
        release_notes_path.write_text("\n".join(release_notes_lines) + "\n", encoding="utf-8")
        if not release_notes_path.exists():
            raise SystemExit(f"Release notes file was not created: {release_notes_path}")
        log("Release notes file creation confirmed.")

    env = os.environ.copy()
    env["APP_VERSION_TAG"] = args.version_tag
    env["FIREBASE_DISTRIBUTION_ARTIFACT_PATH"] = str(artifact_path.resolve())
    env["GOOGLE_APPLICATION_CREDENTIALS"] = str(credentials_absolute)
    env["FIREBASE_RELEASE_NOTES_FILE"] = str(release_notes_path)

    command = [
        gradlew_name(),
        "appDistributionUploadRelease",
        f"-PfirebaseServiceCredentialsFile={credentials_absolute}",
        f"-PfirebaseArtifactPath={artifact_path.resolve()}",
    ]
    log(f"Passing credentials path to Gradle: {credentials_absolute}")
    log(f"Passing artifact path to Gradle: {artifact_path.resolve()}")
    log(f"Running command: {' '.join(command)}")

    run(command, cwd=root, env=env)


if __name__ == "__main__":
    main()
