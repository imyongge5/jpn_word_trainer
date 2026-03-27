from __future__ import annotations

import argparse
import pathlib

from ci_common import append_github_output, repo_root, run, write_lines


PREFIX_TO_SECTION = {
    "added_feat:": "새 기능",
    "bug_fix:": "버그 수정",
    "design_fix:": "디자인 변경",
    "changed:": "동작 변경",
}


def previous_build_commit(root: pathlib.Path) -> str | None:
    tags_raw = run(
        ["git", "tag", "--list", "build-history/*", "--sort=-creatordate"],
        cwd=root,
        capture_output=True,
    ).stdout
    tags = [line.strip() for line in tags_raw.splitlines() if line.strip()]
    if not tags:
        return None
    latest_tag = tags[0]
    return run(
        ["git", "rev-list", "-n", "1", latest_tag],
        cwd=root,
        capture_output=True,
    ).stdout.strip() or None


def collect_messages(root: pathlib.Path, start_commit: str | None, end_commit: str) -> list[str]:
    command = ["git", "log", "--format=%s"]
    if start_commit:
        command.append(f"{start_commit}..{end_commit}")
    else:
        command.extend(["-n", "1", end_commit])
    raw = run(command, cwd=root, capture_output=True).stdout
    return [line.strip() for line in raw.splitlines() if line.strip()]


def build_note_lines(messages: list[str], version_tag: str, source_tag: str, commit_sha: str) -> list[str]:
    grouped: dict[str, list[str]] = {section: [] for section in PREFIX_TO_SECTION.values()}
    for message in reversed(messages):
        for prefix, section in PREFIX_TO_SECTION.items():
            if message.startswith(prefix):
                item = message[len(prefix):].strip()
                if item:
                    grouped[section].append(f"- {item}")
                break

    lines = [
        f"Version: {version_tag}",
        f"Source tag: {source_tag}",
        f"Commit: {commit_sha}",
        "",
    ]
    used_section = False
    for section, items in grouped.items():
        if not items:
            continue
        used_section = True
        lines.append(section)
        lines.extend(items)
        lines.append("")
    if not used_section:
        lines.append("이번 빌드에는 사용자 노출용 패치노트 항목이 없습니다.")
    elif lines[-1] == "":
        lines.pop()
    return lines


def create_history_tag(root: pathlib.Path, commit_sha: str) -> str:
    tag_name = f"build-history/{commit_sha[:12]}"
    verify = run(
        ["git", "rev-parse", "--verify", f"refs/tags/{tag_name}"],
        cwd=root,
        capture_output=True,
        check=False,
    )
    if verify.returncode != 0:
        run(["git", "tag", "-f", tag_name, commit_sha], cwd=root)
    return tag_name


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--version-tag", required=True)
    parser.add_argument("--source-tag", required=True)
    parser.add_argument("--commit-sha", required=True)
    parser.add_argument("--output-path", required=True)
    args = parser.parse_args()

    root = repo_root()
    output_path = pathlib.Path(args.output_path)
    if not output_path.is_absolute():
        output_path = (root / output_path).resolve()

    previous_commit = previous_build_commit(root)
    messages = collect_messages(root, previous_commit, args.commit_sha)
    lines = build_note_lines(messages, args.version_tag, args.source_tag, args.commit_sha)
    write_lines(output_path, lines)
    history_tag = create_history_tag(root, args.commit_sha)

    append_github_output("patch_notes_file", str(output_path))
    append_github_output("build_history_tag", history_tag)
    print(f"Patch notes file written to: {output_path}")
    print(f"Build history tag: {history_tag}")


if __name__ == "__main__":
    main()
