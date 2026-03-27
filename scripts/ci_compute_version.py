from __future__ import annotations

import argparse

from ci_common import append_github_env, append_github_output, compute_version_tag, repo_root


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--env-name", default="APP_VERSION_TAG")
    parser.add_argument("--output-name", default="version_tag")
    args = parser.parse_args()

    version_tag = compute_version_tag(repo_root())
    print(f"This commit version is {version_tag}.")
    append_github_output(args.output_name, version_tag)
    append_github_env(args.env_name, version_tag)


if __name__ == "__main__":
    main()
