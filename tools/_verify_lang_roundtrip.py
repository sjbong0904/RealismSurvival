# -*- coding: utf-8 -*-
"""Simulate FTB makeKey and verify every chapter/quest has a matching lang entry."""
import json
from pathlib import Path

root = Path("run/config/ftbquests/quests")
lang = {}
for p in (root / "lang" / "ko_kr").rglob("*.json5"):
    lang.update(json.loads(p.read_text(encoding="utf-8")))

missing_ch = []
missing_q = []
for p in sorted((root / "chapters").glob("*.json5")):
    ch = json.loads(p.read_text(encoding="utf-8"))
    cid = ch["id"]
    key = f"chapter.{cid}.title"
    if key not in lang or not lang[key]:
        missing_ch.append((p.stem, key))
    for q in ch.get("quests") or []:
        qid = q["id"]
        qk = f"quest.{qid}.title"
        if qk not in lang or not lang[qk]:
            missing_q.append((p.stem, qid, qk))

print("chapters missing title", len(missing_ch))
for x in missing_ch[:10]:
    print(" ", x)
print("quests missing title", len(missing_q))
for x in missing_q[:10]:
    print(" ", x)

# groups
g = json.loads((root / "chapter_groups.json5").read_text(encoding="utf-8"))
for gg in g["chapter_groups"]:
    k = f"chapter_group.{gg['id']}.title"
    print("group", k, "->", lang.get(k))

print("total lang", len(lang))
print("title keys", sum(1 for k in lang if k.endswith(".title")))
print("desc keys", sum(1 for k in lang if "quest_desc" in k))
