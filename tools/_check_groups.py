# -*- coding: utf-8 -*-
import json
from pathlib import Path

root = Path("run/config/ftbquests/quests")
g = json.loads((root / "chapter_groups.json5").read_text(encoding="utf-8"))
gids = {x["id"] for x in g["chapter_groups"]}
print("defined groups", gids)
for p in sorted((root / "chapters").glob("*.json5")):
    ch = json.loads(p.read_text(encoding="utf-8"))
    grp = ch.get("group", "")
    ok = grp in gids
    print(f"{p.stem:30} group={grp} ok={ok}")
