from pathlib import Path
import sys

from pypdf import PdfReader


def main() -> int:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")

    if len(sys.argv) < 2:
        print("usage: inspect_pdf.py <pdf-path> [pages]")
        return 1

    pdf_path = Path(sys.argv[1])
    max_pages = int(sys.argv[2]) if len(sys.argv) > 2 else 3

    reader = PdfReader(str(pdf_path))
    print(f"pages={len(reader.pages)}")
    for index, page in enumerate(reader.pages[:max_pages]):
        print(f"--- page {index + 1} ---")
        text = page.extract_text() or ""
        print(text[:4000])
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
