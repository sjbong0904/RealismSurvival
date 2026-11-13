"""Generate jei_ko_translations.json from missing English keys."""
from __future__ import annotations

import json
import re
import time
from pathlib import Path

from deep_translator import MyMemoryTranslator

ROOT = Path(__file__).resolve().parent
MISSING = ROOT / "missing_ko_kr" / "jei.json"
OUT = ROOT / "jei_ko_translations.json"

PLACEHOLDER_RE = re.compile(
    r"(\{[0-9]+(?:,[^}]*)?\}|%[0-9]?\$?[sd]|%s|%d|\\n|\[[^\]]+\])"
)


def translate_one(tr: MyMemoryTranslator, en: str) -> str:
    placeholders: list[tuple[str, str]] = []

    def repl(match: re.Match[str]) -> str:
        token = f"@@PH{len(placeholders)}@@"
        placeholders.append((token, match.group(0)))
        return token

    protected = PLACEHOLDER_RE.sub(repl, en)
    for attempt in range(8):
        try:
            ko = tr.translate(protected)
            break
        except Exception:
            time.sleep(2.0 * (attempt + 1))
    else:
        raise RuntimeError(f"translate failed: {en!r}")
    for token, orig in placeholders:
        ko = ko.replace(token, orig)
    return ko


def main() -> None:
    missing: dict[str, str] = json.loads(MISSING.read_text(encoding="utf-8"))
    out: dict[str, str] = {}
    if OUT.is_file():
        out = json.loads(OUT.read_text(encoding="utf-8"))
    tr = MyMemoryTranslator(source="en-GB", target="ko-KR")
    todo = [(k, en) for k, en in missing.items() if k not in out]
    print(f"resume: have {len(out)}, need {len(todo)} more")
    for idx, (key, en) in enumerate(todo, start=1):
        out[key] = translate_one(tr, en)
        time.sleep(0.25)
        if idx % 25 == 0:
            print(f"translated {idx}/{len(todo)}")
            OUT.write_text(json.dumps(out, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    if len(out) != len(missing):
        missing_keys = set(missing) - set(out)
        raise SystemExit(f"still missing {len(missing_keys)} keys")
    OUT.write_text(json.dumps(out, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"wrote {len(out)} keys to {OUT.name}")


if __name__ == "__main__":
    main()
