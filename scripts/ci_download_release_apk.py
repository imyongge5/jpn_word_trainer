from __future__ import annotations

import argparse
import pathlib

from ci_common import ensure_parent, github_api_request, require_env


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--release-tag", required=True)
    parser.add_argument("--output-path", default="app/build/outputs/apk/release/app-release.apk")
    args = parser.parse_args()

    repository = require_env("GITHUB_REPOSITORY")
    token = require_env("WORKFLOW_PUSH_TOKEN")
    output_path = pathlib.Path(args.output_path)
    ensure_parent(output_path)

    _, release = github_api_request(
        "GET",
        f"https://api.github.com/repos/{repository}/releases/tags/{args.release_tag}",
        token=token,
    )

    target = next((asset for asset in release.get("assets", []) if asset.get("name") == "app-release.apk"), None)
    if target is None:
        raise SystemExit(f"app-release.apk asset not found in release {args.release_tag}")

    _, binary = github_api_request(
        "GET",
        target["url"],
        token=token,
        extra_headers={"Accept": "application/octet-stream"},
    )
    output_path.write_bytes(binary if isinstance(binary, bytes) else b"")
    print(f"Downloaded release asset to {output_path}")


if __name__ == "__main__":
    main()
