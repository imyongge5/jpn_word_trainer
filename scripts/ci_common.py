from __future__ import annotations

import json
import os
import pathlib
import subprocess
import sys
import urllib.error
import urllib.parse
import urllib.request
from typing import Iterable


def repo_root() -> pathlib.Path:
    return pathlib.Path(__file__).resolve().parent.parent


def scripts_root() -> pathlib.Path:
    return pathlib.Path(__file__).resolve().parent


def run(
    args: list[str],
    *,
    cwd: pathlib.Path | None = None,
    env: dict[str, str] | None = None,
    capture_output: bool = False,
    check: bool = True,
) -> subprocess.CompletedProcess[str]:
    result = subprocess.run(
        args,
        cwd=str(cwd) if cwd else None,
        env=env,
        check=False,
        text=True,
        capture_output=capture_output,
    )
    if check and result.returncode != 0:
        if result.stdout:
            print(result.stdout, end="")
        if result.stderr:
            print(result.stderr, end="", file=sys.stderr)
        raise SystemExit(f"Command failed with exit code {result.returncode}: {' '.join(args)}")
    return result


def require_env(name: str) -> str:
    value = os.environ.get(name, "").strip()
    if not value:
        raise SystemExit(f"Required environment variable is missing: {name}")
    return value


def require_github_token() -> str:
    for name in ("WORKFLOW_PUSH_TOKEN", "GITHUB_TOKEN"):
        value = os.environ.get(name, "").strip()
        if value:
            return value
    raise SystemExit("Required environment variable is missing: WORKFLOW_PUSH_TOKEN or GITHUB_TOKEN")


def append_github_output(name: str, value: str) -> None:
    path = os.environ.get("GITHUB_OUTPUT")
    if not path:
        return
    with open(path, "a", encoding="utf-8") as handle:
        handle.write(f"{name}={value}\n")


def append_github_env(name: str, value: str) -> None:
    path = os.environ.get("GITHUB_ENV")
    if not path:
        return
    with open(path, "a", encoding="utf-8") as handle:
        handle.write(f"{name}={value}\n")


def read_properties(path: pathlib.Path) -> dict[str, str]:
    properties: dict[str, str] = {}
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        properties[key.strip()] = value.strip()
    return properties


def compute_version_tag(workdir: pathlib.Path) -> str:
    version_props = read_properties(workdir / "version-series.properties")
    version_a = version_props.get("VERSION_A")
    version_b = version_props.get("VERSION_B")
    if not version_a or not version_b:
        raise SystemExit("VERSION_A or VERSION_B is missing in version-series.properties")

    base_commit = run(
        ["git", "log", "-n", "1", "--format=%H", "--", "version-series.properties"],
        cwd=workdir,
        capture_output=True,
    ).stdout.strip()
    if not base_commit:
        raise SystemExit("Failed to resolve base commit from version-series.properties")

    commits_after_base = run(
        ["git", "rev-list", "--count", f"{base_commit}..HEAD"],
        cwd=workdir,
        capture_output=True,
    ).stdout.strip()
    if not commits_after_base:
        raise SystemExit("Failed to count commits after base commit")

    patch = int(commits_after_base) + 1
    return f"v{version_a}.{version_b}.{patch}"


def github_api_request(
    method: str,
    url: str,
    *,
    token: str,
    data: bytes | None = None,
    extra_headers: dict[str, str] | None = None,
    acceptable_not_found: bool = False,
) -> tuple[int, dict | bytes]:
    headers = {
        "Authorization": f"Bearer {token}",
        "Accept": "application/vnd.github+json",
        "User-Agent": "jpn-word-trainer-ci",
    }
    if extra_headers:
        headers.update(extra_headers)
    request = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request) as response:
            body = response.read()
            content_type = response.headers.get("Content-Type", "")
            if "application/json" in content_type:
                return response.status, json.loads(body.decode("utf-8"))
            return response.status, body
    except urllib.error.HTTPError as exc:
        if acceptable_not_found and exc.code == 404:
            return exc.code, {}
        detail = exc.read().decode("utf-8", errors="ignore")
        raise SystemExit(f"GitHub API request failed ({exc.code}) {method} {url}\n{detail}") from exc


def ensure_parent(path: pathlib.Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)


def write_lines(path: pathlib.Path, lines: Iterable[str]) -> None:
    ensure_parent(path)
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")
