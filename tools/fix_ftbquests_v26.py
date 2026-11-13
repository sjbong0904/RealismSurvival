# -*- coding: utf-8 -*-
"""Fix FTB Quests 26.1 pack: ItemStack icons + lang/ title/description tables.

Idempotent: re-running after titles were stripped preserves existing lang keys
instead of wiping them to empty {} (which shows as 「이름 없음」 in-game).
"""
from __future__ import annotations

import json
import re
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
QUESTS = ROOT / "pack" / "config" / "ftbquests" / "quests"
RUN = ROOT / "run" / "config" / "ftbquests" / "quests"
LOCALES = ("ko_kr",)


def item_icon(item_id: str) -> dict:
    if isinstance(item_id, dict):
        return item_id
    return {"id": item_id, "count": 1}


def fix_icon_field(obj: dict) -> None:
    icon = obj.get("icon")
    if isinstance(icon, str) and icon:
        obj["icon"] = item_icon(icon)


def fix_item_stack_field(obj: dict, field: str = "item") -> None:
    """FTB 26.1 requires ItemStack objects; string ids become air ('공기')."""
    val = obj.get(field)
    if isinstance(val, str) and val:
        obj[field] = item_icon(val)
    elif isinstance(val, dict) and "id" in val and "count" not in val:
        val["count"] = 1


def ensure_quest_icon(q: dict) -> None:
    """Prefer an item from tasks/rewards so quests don't fall back to missing icons."""
    if q.get("icon"):
        fix_icon_field(q)
        return
    for task in q.get("tasks") or []:
        if not isinstance(task, dict):
            continue
        item = task.get("item")
        if task.get("type") == "item":
            if isinstance(item, str) and item:
                q["icon"] = item_icon(item)
                return
            if isinstance(item, dict) and item.get("id"):
                q["icon"] = item_icon(item["id"])
                return
    for reward in q.get("rewards") or []:
        if not isinstance(reward, dict):
            continue
        item = reward.get("item")
        if reward.get("type") == "item":
            if isinstance(item, str) and item:
                q["icon"] = item_icon(item)
                return
            if isinstance(item, dict) and item.get("id"):
                q["icon"] = item_icon(item["id"])
                return


def _load_json5_dict(path: Path) -> dict:
    text = path.read_text(encoding="utf-8")
    # Hand-edited quest lang may use JSON5 trailing commas.
    text = re.sub(r",(\s*[}\]])", r"\1", text)
    data = json.loads(text)
    return data if isinstance(data, dict) else {}


def _load_existing_lang(locale: str) -> dict:
    """Flatten every *.json5 under lang/<locale>/ into one dict (best-effort)."""
    base = QUESTS / "lang" / locale
    if not base.is_dir():
        return {}
    flat: dict = {}
    for path in sorted(base.rglob("*.json5")):
        try:
            flat.update(_load_json5_dict(path))
        except json.JSONDecodeError:
            continue
    return flat


def collect_and_strip_quest(q: dict, lang: dict, existing: dict) -> None:
    qid = q.get("id")
    if not qid:
        return
    title = q.pop("title", None)
    desc = q.pop("description", None)
    tkey = f"quest.{qid}.title"
    dkey = f"quest.{qid}.quest_desc"
    # Quest source text is canonical; generated locale files must reflect edits to quest_v1.
    if title:
        lang[tkey] = title
    elif tkey in existing:
        lang[tkey] = existing[tkey]
    if desc:
        if isinstance(desc, str):
            desc = [desc]
        lang[dkey] = desc
    elif dkey in existing:
        lang[dkey] = existing[dkey]
    ensure_quest_icon(q)
    fix_icon_field(q)
    for task in q.get("tasks") or []:
        if isinstance(task, dict):
            tid = task.get("id")
            ttitle = task.pop("title", None)
            ttkey = f"task.{tid}.title" if tid else None
            if tid and ttitle:
                lang[ttkey] = ttitle
            elif ttkey and ttkey in existing:
                lang[ttkey] = existing[ttkey]
            fix_icon_field(task)
            fix_item_stack_field(task, "item")
    for reward in q.get("rewards") or []:
        if isinstance(reward, dict):
            fix_icon_field(reward)
            fix_item_stack_field(reward, "item")


def main() -> None:
    if not QUESTS.exists():
        raise SystemExit(f"missing {QUESTS}")

    # Preserve prior lang so a second run cannot wipe titles to empty {}.
    existing_ko = _load_existing_lang("ko_kr")
    existing_en = _load_existing_lang("en_us")
    existing = {**existing_en, **existing_ko}

    # chapter group titles
    groups_path = QUESTS / "chapter_groups.json5"
    groups = json.loads(groups_path.read_text(encoding="utf-8"))
    group_lang: dict = {}
    group_title_ko = {
        "Tutorial": "튜토리얼",
        "Guides": "가이드",
        "Bundles": "번들",
        "Collection": "도감",
        "Daily": "일일",
    }
    for g in groups.get("chapter_groups", []):
        gid = g.get("id")
        title = g.pop("title", None)
        gkey = f"chapter_group.{gid}.title" if gid else None
        if gid and title:
            group_lang[gkey] = group_title_ko.get(title, title)
        elif gkey and gkey in existing:
            group_lang[gkey] = existing[gkey]
        fix_icon_field(g)
    groups_path.write_text(json.dumps(groups, ensure_ascii=False, indent="\t") + "\n", encoding="utf-8")

    chapter_lang: dict = {}
    per_chapter_lang: dict[str, dict] = {}
    chapter_files = 0

    chapters_dir = QUESTS / "chapters"
    for path in sorted(chapters_dir.glob("*.json5")):
        chapter_files += 1
        ch = json.loads(path.read_text(encoding="utf-8"))
        cid = ch.get("id")
        filename = ch.get("filename") or path.stem
        title = ch.pop("title", None)
        ckey = f"chapter.{cid}.title" if cid else None
        if cid and title:
            chapter_lang[ckey] = title
        elif ckey and ckey in existing:
            chapter_lang[ckey] = existing[ckey]
        fix_icon_field(ch)

        qlang: dict = {}
        for q in ch.get("quests") or []:
            if isinstance(q, dict):
                collect_and_strip_quest(q, qlang, existing)
        per_chapter_lang[filename] = qlang

        path.write_text(json.dumps(ch, ensure_ascii=False, indent="\t") + "\n", encoding="utf-8")

    if chapter_files > 0 and len(chapter_lang) == 0:
        raise SystemExit(
            "Refusing to write empty chapter lang — titles missing from chapters/ "
            "and no salvageable lang/. Re-run: python tools/generate_ftbquests.py"
        )

    # data.json5 — book icon as ItemStack; bump version; fallback locale
    data_path = QUESTS / "data.json5"
    data = json.loads(data_path.read_text(encoding="utf-8"))
    fix_icon_field(data)
    data["version"] = 13
    data["fallback_locale"] = "ko_kr"
    data_path.write_text(json.dumps(data, ensure_ascii=False, indent="\t") + "\n", encoding="utf-8")

    # Korean-only pack: ko_kr is canonical; other locales fall back to it via fallback_locale.
    for stale in (QUESTS / "lang").glob("*"):
        if stale.is_dir() and stale.name not in LOCALES:
            shutil.rmtree(stale)
    for locale in LOCALES:
        base = QUESTS / "lang" / locale
        if base.exists():
            shutil.rmtree(base)
        base.mkdir(parents=True)

        (base / "chapter_group.json5").write_text(
            json.dumps(group_lang, ensure_ascii=False, indent="\t") + "\n", encoding="utf-8"
        )
        (base / "chapter.json5").write_text(
            json.dumps(chapter_lang, ensure_ascii=False, indent="\t") + "\n", encoding="utf-8"
        )
        ch_lang_dir = base / "chapters"
        ch_lang_dir.mkdir(parents=True)
        flat: dict = {}
        flat.update(group_lang)
        flat.update(chapter_lang)
        for filename, qlang in per_chapter_lang.items():
            (ch_lang_dir / f"{filename}.json5").write_text(
                json.dumps(qlang, ensure_ascii=False, indent="\t") + "\n", encoding="utf-8"
            )
            flat.update(qlang)
        (base / "all.json5").write_text(
            json.dumps(flat, ensure_ascii=False, indent="\t") + "\n", encoding="utf-8"
        )

    if RUN.exists():
        shutil.rmtree(RUN)
    shutil.copytree(QUESTS, RUN)

    n_quests = sum(len(v) for v in per_chapter_lang.values())
    print(
        f"Fixed icons + lang: {len(chapter_lang)} chapters, "
        f"{len(group_lang)} groups, ~{n_quests} quest strings × {len(LOCALES)} locale(s)"
    )
    print(f"Synced → {RUN}")


if __name__ == "__main__":
    main()
