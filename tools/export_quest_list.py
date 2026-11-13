# -*- coding: utf-8 -*-
"""Export all v1 quests to docs/QUEST_LIST.md (and CSV)."""
from __future__ import annotations

import csv
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))

from quest_v1.bundles import (  # noqa: E402
    build_bundle_explore,
    build_bundle_kitchen,
    build_bundle_ranch,
    build_bundle_survival,
    build_bundle_workshop,
)
from quest_v1.collections import (  # noqa: E402
    build_collection_artifacts,
    build_collection_biomes,
    build_collection_creatures,
    build_collection_crops,
    build_collection_fish,
    build_collection_food,
    build_collection_minerals,
)
from quest_v1.perfection import build_perfection  # noqa: E402
from quest_v1.tutorial import (  # noqa: E402
    build_c1,
    build_c2,
    build_c3,
    build_c4,
    build_c5,
    build_t0,
)


def parse_tasks(task_snbts: list[str]) -> str:
    parts: list[str] = []
    for t in task_snbts:
        if 'type: "checkmark"' in t:
            parts.append("체크마크")
            continue
        m = re.search(r'stat: "([^"]+)"', t)
        if m and 'type: "stat"' in t:
            val = re.search(r"value: (\d+)", t)
            n = val.group(1) if val else "?"
            parts.append(f"통계 `{m.group(1)}` ≥{n}")
            continue
        m = re.search(r'advancement: "([^"]+)"', t)
        if m:
            parts.append(f"발전과제 `{m.group(1)}`")
            continue
        m = re.search(r'biome: "([^"]+)"', t)
        if m:
            parts.append(f"생물군계 방문 `{m.group(1)}`")
            continue
        m = re.search(r'structure: "([^"]+)"', t)
        if m:
            parts.append(f"구조물 `{m.group(1)}`")
            continue
        m = re.search(r'entity: "([^"]+)"', t)
        if m:
            val = re.search(r"value: (\d+)", t)
            n = val.group(1) if val else "1"
            parts.append(f"처치 `{m.group(1)}` ×{n}")
            continue
        item = re.search(r'item:\s*\{\s*id:\s*"([^"]+)"', t)
        if item and 'type: "item"' in t:
            # required amount is task-level count: NL (stack count stays 1)
            cnt = re.search(r"(?m)^\s*count:\s*(\d+)L\s*$", t)
            n = cnt.group(1) if cnt else "1"
            consume = "consume_items: true" in t
            verb = "제출" if consume else "보유"
            parts.append(f"{verb} `{item.group(1)}` ×{n}")
            continue
        parts.append("기타 과제")
    return " · ".join(parts) if parts else "—"


def parse_rewards(reward_snbts: list[str]) -> str:
    parts: list[str] = []
    for r in reward_snbts:
        m = re.search(r"xp_levels: (\d+)", r)
        if m and 'type: "xp_levels"' in r:
            parts.append(f"레벨 {m.group(1)}")
            continue
        m = re.search(r"xp: (\d+)", r)
        if m and 'type: "xp"' in r:
            parts.append(f"경험치 {m.group(1)}")
            continue
        item = re.search(r'item:\s*\{\s*id:\s*"([^"]+)"', r)
        if item and 'type: "item"' in r:
            # reward amount is top-level count: N (not inside ItemStack)
            cnt = re.search(r"(?m)^\s*count:\s*(\d+)\s*$", r)
            n = cnt.group(1) if cnt else "1"
            parts.append(f"`{item.group(1)}` ×{n}")
            continue
        parts.append("기타 보상")
    return " · ".join(parts) if parts else "—"


def esc_cell(s: str) -> str:
    return s.replace("|", "\\|").replace("\n", " ")


def chapter_rows(chapter: str, quests, titles: dict[str, str]) -> list[dict]:
    rows = []
    for q in quests:
        unlock = "없음 (시작)"
        if q.deps:
            names = [f"{d} ({titles.get(d, d)})" for d in q.deps]
            unlock = ", ".join(names)
            if q.min_required_dependencies is not None:
                unlock += f" — 이 중 {q.min_required_dependencies}개만 완료하면 됨"
        if q.optional:
            unlock += " 〔선택〕"
        desc = " ".join(q.desc) if q.desc else "—"
        # quest-level consume also affects item tasks
        task_text = parse_tasks(q.tasks)
        if q.consume is True and "보유" in task_text and "제출" not in task_text:
            task_text = task_text.replace("보유", "제출")
        rows.append(
            {
                "chapter": chapter,
                "id": q.qid,
                "title": q.title,
                "unlock": unlock,
                "complete": task_text,
                "rewards": parse_rewards(q.rewards),
                "desc": desc,
                "optional": "예" if q.optional else "",
            }
        )
    return rows


def main() -> None:
    from build_collection_lists import main as build_lists

    build_lists()
    col = json.loads((ROOT / "tools/quest_catalog/collections.json").read_text(encoding="utf-8"))

    from quest_v1.daily import build_daily

    chapters: list[tuple[str, list]] = [
        ("0. 시작하기", build_t0()),
        ("I. 생존", build_c1()),
        ("II. 밭과 부엌", build_c2()),
        ("III. 밖으로", build_c3()),
        ("IV. 깊은 곳", build_c4()),
        ("V. 자동화", build_c5()),
        ("일일 의뢰", build_daily()),
        ("번들 · 생존 방", build_bundle_survival()),
        ("번들 · 부엌 방", build_bundle_kitchen()),
        ("번들 · 목축 방", build_bundle_ranch()),
        ("번들 · 탐험 방", build_bundle_explore()),
        ("번들 · 공방 방", build_bundle_workshop()),
        ("도감 · 작물", build_collection_crops([(a, b) for a, b in col["crops"]])),
        ("도감 · 음식·음료", build_collection_food(col["cooking"], col["drinks"])),
        ("도감 · 물고기", build_collection_fish([(a, b) for a, b in col["fish"]])),
        ("도감 · 생물군계", build_collection_biomes([(a, b) for a, b in col["biomes"]])),
        ("도감 · 생물", build_collection_creatures(col["mobs"], col["animals"])),
        ("도감 · 광물", build_collection_minerals([(a, b) for a, b in col["minerals"]])),
        ("도감 · 유물", build_collection_artifacts([(a, b) for a, b in col["artifacts"]])),
        ("도감 · Perfection", build_perfection()),
    ]

    titles: dict[str, str] = {}
    for _, quests in chapters:
        for q in quests:
            titles[q.qid] = q.title

    all_rows: list[dict] = []
    for ch, quests in chapters:
        all_rows.extend(chapter_rows(ch, quests, titles))

    out_md = ROOT / "docs" / "QUEST_LIST.md"
    out_csv = ROOT / "docs" / "QUEST_LIST.csv"

    groups = {"본편(시작+I~V)": 0, "일일 의뢰": 0, "번들": 0, "도감": 0, "Perfection": 0}
    optional_n = 0
    for ch, quests in chapters:
        if ch.startswith("번들"):
            key = "번들"
        elif ch == "일일 의뢰":
            key = "일일 의뢰"
        elif ch == "도감 · Perfection":
            key = "Perfection"
        elif ch.startswith("도감"):
            key = "도감"
        else:
            key = "본편(시작+I~V)"
        groups[key] += len(quests)
        optional_n += sum(1 for q in quests if q.optional)

    roman = {"I": "C1", "II": "C2", "III": "C3", "IV": "C4", "V": "C5"}
    cross: list[str] = []
    for ch, quests in chapters:
        head = ch.split(".", 1)[0]
        expected = roman.get(head)
        if not expected:
            continue
        for q in quests:
            m = re.match(r"(C\d)-", q.qid)
            if m and m.group(1) != expected:
                cross.append(f"`{q.qid}` {q.title} — 실제 위치: {ch} (ID는 저장 안정 키로 유지)")

    lines: list[str] = [
        "# Realism Survival — 퀘스트 전체 목록",
        "",
        "진행표 v1 생성기(`tools/quest_v1/`) 기준 **생성 스냅샷**입니다. 정본은 생성기와 `pack/config/ftbquests/`입니다.",
        "갱신: `python tools/export_quest_list.py`",
        "",
        f"총 **{len(all_rows)}**개 퀘스트 (선택 과제 {optional_n}개 포함).",
        "",
        "| 구분 | 퀘스트 수 |",
        "|---|---|",
        *[f"| {k} | {v} |" for k, v in groups.items()],
        "",
        "플레이 시간은 측정값이 아닙니다. 핵심 안내(본편)는 약 60시간을 **목표 예산**으로 두고, 도감·Perfection은 그보다 긴 장기 목표입니다.",
        "",
    ]
    if cross:
        lines += ["### ID 접두사와 실제 장이 다른 퀘스트", "", *[f"- {c}" for c in cross], ""]
    lines += [
        "## 목차",
        "",
    ]
    for ch, quests in chapters:
        anchor = ch.replace(" ", "-").replace("·", "").replace(".", "")
        lines.append(f"- [{ch}](#{anchor}) ({len(quests)})")
    lines.append("")

    for ch, quests in chapters:
        rows = [r for r in all_rows if r["chapter"] == ch]
        lines.append(f"## {ch}")
        lines.append("")
        lines.append("| ID | 제목 | 해금(선행) | 완료 조건 | 보상 | 설명 |")
        lines.append("|---|---|---|---|---|---|")
        for r in rows:
            opt = " 〔선택〕" if r["optional"] else ""
            lines.append(
                "| "
                + " | ".join(
                    [
                        f"`{esc_cell(r['id'])}`",
                        esc_cell(r["title"] + opt),
                        esc_cell(r["unlock"]),
                        esc_cell(r["complete"]),
                        esc_cell(r["rewards"]),
                        esc_cell(r["desc"]),
                    ]
                )
                + " |"
            )
        lines.append("")

    out_md.write_text("\n".join(lines) + "\n", encoding="utf-8")

    with out_csv.open("w", encoding="utf-8-sig", newline="") as f:
        w = csv.DictWriter(
            f,
            fieldnames=["chapter", "id", "title", "unlock", "complete", "rewards", "desc", "optional"],
        )
        w.writeheader()
        w.writerows(all_rows)

    print(f"Wrote {out_md} ({len(all_rows)} quests)")
    print(f"Wrote {out_csv}")


if __name__ == "__main__":
    main()
