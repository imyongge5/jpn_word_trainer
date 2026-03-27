from __future__ import annotations

import argparse
import os
import pathlib

from ci_common import append_github_env, require_env, write_lines


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--service-account-env", default="FIREBASE_SERVICE_ACCOUNT")
    parser.add_argument("--credentials-file-name", default="firebase-service-account.json")
    parser.add_argument("--notes-file-name", default="release-notes.txt")
    parser.add_argument("--note-line", action="append", default=[])
    args = parser.parse_args()

    testers = os.environ.get("FIREBASE_APP_DISTRIBUTION_TESTERS", "").strip()
    groups = os.environ.get("FIREBASE_APP_DISTRIBUTION_GROUPS", "").strip()
    if not testers and not groups:
        raise SystemExit("Either FIREBASE_APP_DISTRIBUTION_GROUPS or FIREBASE_APP_DISTRIBUTION_TESTERS is required.")

    service_account = require_env(args.service_account_env)
    runner_temp = pathlib.Path(os.environ.get("RUNNER_TEMP", pathlib.Path.cwd()))

    credentials_path = runner_temp / args.credentials_file_name
    credentials_path.write_text(service_account, encoding="utf-8")
    if credentials_path.stat().st_size == 0:
        raise SystemExit("Firebase service account file is empty")

    notes_path = runner_temp / args.notes_file_name
    write_lines(notes_path, args.note_line or ["Firebase distribution"])

    append_github_env("GOOGLE_APPLICATION_CREDENTIALS", str(credentials_path))
    append_github_env("FIREBASE_RELEASE_NOTES_FILE", str(notes_path))

    print(f"Firebase credentials file created: {credentials_path}")
    print(f"Release notes file created: {notes_path}")


if __name__ == "__main__":
    main()
