import json
import re
from pathlib import Path

PH = re.compile(r"(\{[0-9]+(?:,[^}]*)?\}|%[0-9]?\$?[sd]|%s|%d|\\n|\[[^\]]+\])")
BASE = Path(__file__).resolve().parents[1]


def placeholders(s: str) -> list[str]:
    return PH.findall(s)


for mod in ("jade", "jei"):
    en = json.loads((BASE / "tools/missing_ko_kr" / f"{mod}.json").read_text(encoding="utf-8"))
    ko = json.loads((BASE / "src/main/resources/assets" / mod / "lang/ko_kr.json").read_text(encoding="utf-8"))
    bad = [k for k, v in en.items() if k not in ko or placeholders(v) != placeholders(ko[k])]
    print(mod, "keys", len(ko), "bad", len(bad))
    for k in bad[:10]:
        print(" ", k, placeholders(en[k]), placeholders(ko[k]))
