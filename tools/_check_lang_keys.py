# -*- coding: utf-8 -*-
from pathlib import Path
import json

pack = Path("pack/config/ftbquests/quests/lang/ko_kr/chapter.json5")
run = Path("run/config/ftbquests/quests/lang/ko_kr/chapter.json5")
print("pack", pack.exists(), "run", run.exists())
p = json.loads(pack.read_text(encoding="utf-8"))
r = json.loads(run.read_text(encoding="utf-8"))
print("keys", len(p), len(r), "same", p == r)

ch = json.loads(Path("run/config/ftbquests/quests/chapters/welcome.json5").read_text(encoding="utf-8"))
cid = ch["id"]
print("chapter id", cid)
print("upper key", p.get(f"chapter.{cid}.title"))
print("lower key", p.get(f"chapter.{cid.lower()}.title"))

w = json.loads(Path("run/config/ftbquests/quests/lang/ko_kr/chapters/welcome.json5").read_text(encoding="utf-8"))
qid = ch["quests"][0]["id"]
print("quest id", qid)
print("quest title upper", w.get(f"quest.{qid}.title"))
print("quest title lower", w.get(f"quest.{qid.lower()}.title"))
print("desc key", f"quest.{qid}.quest_desc", "->", w.get(f"quest.{qid}.quest_desc"))
