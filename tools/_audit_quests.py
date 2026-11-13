# -*- coding: utf-8 -*-
import json
from pathlib import Path

root = Path("pack/config/ftbquests/quests")
lang = {}
for p in (root / "lang" / "ko_kr").rglob("*.json5"):
    lang.update(json.loads(p.read_text(encoding="utf-8")))
print("lang keys", len(lang))

str_items = airish = miss_title = miss_desc = 0
for p in sorted((root / "chapters").glob("*.json5")):
    ch = json.loads(p.read_text(encoding="utf-8"))
    cid = ch["id"]
    title = lang.get(f"chapter.{cid}.title")
    if not title:
        print("NO CHAPTER TITLE", p.stem, cid)
    nq = len(ch.get("quests") or [])
    for q in ch.get("quests") or []:
        qid = q["id"]
        if f"quest.{qid}.title" not in lang:
            miss_title += 1
            if miss_title <= 5:
                print("miss title", p.stem, qid)
        if f"quest.{qid}.quest_desc" not in lang:
            miss_desc += 1
        ic = q.get("icon")
        if isinstance(ic, dict) and ic.get("id") in ("minecraft:air", "air", None, ""):
            airish += 1
            print("AIR icon", lang.get(f"quest.{qid}.title"))
        elif isinstance(ic, str):
            print("STR icon", p.stem, lang.get(f"quest.{qid}.title"), ic)
        for t in q.get("tasks") or []:
            if t.get("type") != "item":
                continue
            item = t.get("item")
            if isinstance(item, str):
                str_items += 1
                if str_items <= 5:
                    print("STR item", lang.get(f"quest.{qid}.title"), item)
            elif isinstance(item, dict):
                if item.get("id") in ("minecraft:air", "air", None, ""):
                    airish += 1
                    print("AIR task", lang.get(f"quest.{qid}.title"))
            else:
                airish += 1
                print("BAD item", item)
        for r in q.get("rewards") or []:
            if r.get("type") != "item":
                continue
            item = r.get("item")
            if isinstance(item, str):
                str_items += 1
            elif isinstance(item, dict) and item.get("id") in ("minecraft:air", "air", None, ""):
                airish += 1
    print(f"OK {p.stem:28} title={title!r:20} n={nq}")

print(
    f"SUMMARY miss_title={miss_title} miss_desc={miss_desc} str_items={str_items} airish={airish}"
)
# sample item task
ch = json.loads((root / "chapters" / "tutorial_i_survival.json5").read_text(encoding="utf-8"))
for q in ch["quests"][:3]:
    print("SAMPLE", lang.get(f"quest.{q['id']}.title"), q.get("tasks"))
