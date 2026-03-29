from __future__ import annotations

import argparse
import json
import pathlib
import urllib.parse

from ci_common import github_api_request, repo_root, require_env, require_github_token, run


def ensure_tag(root: pathlib.Path, tag_name: str, repository: str, token: str) -> None:
    verify = run(
        ["git", "rev-parse", "--verify", f"refs/tags/{tag_name}"],
        cwd=root,
        capture_output=True,
        check=False,
    )
    if verify.returncode == 0 and verify.stdout.strip():
        print(f"이미 존재하는 버전 태그입니다: {tag_name}")
        return
    run(["git", "config", "user.name", "github-actions[bot]"], cwd=root)
    run(["git", "config", "user.email", "41898282+github-actions[bot]@users.noreply.github.com"], cwd=root)
    run(["git", "tag", tag_name], cwd=root)
    run(
        ["git", "push", f"https://x-access-token:{token}@github.com/{repository}.git", tag_name],
        cwd=root,
    )


def push_tag(root: pathlib.Path, tag_name: str, repository: str, token: str) -> None:
    run(["git", "tag", "-f", tag_name], cwd=root)
    run(
        ["git", "push", "--force", f"https://x-access-token:{token}@github.com/{repository}.git", tag_name],
        cwd=root,
    )


def get_or_create_release(repository: str, token: str, tag_name: str, body: str) -> dict:
    status, payload = github_api_request(
        "GET",
        f"https://api.github.com/repos/{repository}/releases/tags/{tag_name}",
        token=token,
        acceptable_not_found=True,
    )
    if status == 404:
        _, created = github_api_request(
            "POST",
            f"https://api.github.com/repos/{repository}/releases",
            token=token,
            data=json.dumps(
                {
                    "tag_name": tag_name,
                    "name": tag_name,
                    "body": body,
                    "draft": False,
                    "prerelease": False,
                }
            ).encode("utf-8"),
            extra_headers={"Content-Type": "application/json"},
        )
        return created

    release_id = payload["id"]
    _, updated = github_api_request(
        "PATCH",
        f"https://api.github.com/repos/{repository}/releases/{release_id}",
        token=token,
        data=json.dumps({"name": tag_name, "body": body}).encode("utf-8"),
        extra_headers={"Content-Type": "application/json"},
    )
    return updated


def upload_asset(repository: str, token: str, release: dict, apk_path: pathlib.Path) -> None:
    for asset in release.get("assets", []):
        if asset.get("name") == apk_path.name:
            github_api_request(
                "DELETE",
                f"https://api.github.com/repos/{repository}/releases/assets/{asset['id']}",
                token=token,
            )

    upload_url_template = release["upload_url"].split("{", 1)[0]
    upload_url = f"{upload_url_template}?name={urllib.parse.quote(apk_path.name)}"
    github_api_request(
        "POST",
        upload_url,
        token=token,
        data=apk_path.read_bytes(),
        extra_headers={"Content-Type": "application/vnd.android.package-archive", "Accept": "application/vnd.github+json"},
    )


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--version-tag", required=True)
    parser.add_argument("--apk-path", required=True)
    parser.add_argument("--release-notes-file", default="")
    parser.add_argument("--additional-tag", default="")
    args = parser.parse_args()

    root = repo_root()
    token = require_github_token()
    repository = require_env("GITHUB_REPOSITORY")
    source_tag = require_env("GITHUB_REF_NAME")
    commit_sha = require_env("GITHUB_SHA")

    apk_path = pathlib.Path(args.apk_path)
    if not apk_path.exists():
        raise SystemExit(f"APK not found: {apk_path}")

    ensure_tag(root, args.version_tag, repository, token)
    if args.additional_tag:
        push_tag(root, args.additional_tag, repository, token)

    if args.release_notes_file:
        notes_path = pathlib.Path(args.release_notes_file)
        if not notes_path.is_absolute():
            notes_path = (root / notes_path).resolve()
        if not notes_path.exists():
            raise SystemExit(f"Release notes file not found: {notes_path}")
        body = notes_path.read_text(encoding="utf-8").strip()
    else:
        body = "\n".join(
            [
                f"Version: {args.version_tag}",
                f"Source tag: {source_tag}",
                f"Commit: {commit_sha}",
            ]
        )
    release = get_or_create_release(repository, token, args.version_tag, body)
    upload_asset(repository, token, release, apk_path)
    print(f"GitHub Release ready: {release['html_url']}")


if __name__ == "__main__":
    main()
