# -*- coding: utf-8 -*-
"""Generate Realism Survival FTB Quests from progression table v1.

Usage:
    python tools/generate_ftbquests.py

Writes pack/config/ftbquests/quests (JSON5 + lang) and syncs to run/config.
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))

from quest_v1.bundles import (  # noqa: E402
    BUNDLE_SEALS,
    build_bundle_explore,
    build_bundle_kitchen,
    build_bundle_ranch,
    build_bundle_survival,
    build_bundle_workshop,
)
from quest_v1.collections import (  # noqa: E402
    COLLECTION_CLEARS,
    COLLECTION_GATES,
    build_collection_artifacts,
    build_collection_biomes,
    build_collection_creatures,
    build_collection_crops,
    build_collection_fish,
    build_collection_food,
    build_collection_minerals,
)
from quest_v1.core import (  # noqa: E402
    G_BUNDLES,
    G_COLLECTION,
    G_DAILY,
    G_TUTORIAL,
    OUT,
    RUN,
    convert_snbt_tree_to_json5,
    gate_extra,
    sync_to_run,
    write_chapter,
    write_data_and_groups,
)
from quest_v1.daily import build_daily  # noqa: E402
from quest_v1.perfection import PERF_EXTRA_IDS, build_perfection  # noqa: E402
from quest_v1.tutorial import (  # noqa: E402
    build_c1,
    build_c2,
    build_c3,
    build_c4,
    build_c5,
    build_t0,
)


def main() -> None:
    write_data_and_groups()

    # Tutorial mainline
    write_chapter("welcome", G_TUTORIAL, "0. 시작하기", "minecraft:book", 0, build_t0())
    write_chapter(
        "tutorial_i_survival",
        G_TUTORIAL,
        "I. 생존",
        "realismsurvival:distilled_water_bottle",
        1,
        build_c1(),
        extra_ids=gate_extra("T0-15", "T0-05"),
    )
    write_chapter(
        "tutorial_ii_farm",
        G_TUTORIAL,
        "II. 밭과 부엌",
        "farmersdelight:cooking_pot",
        2,
        build_c2(),
        extra_ids=gate_extra("C1-23"),
    )
    write_chapter(
        "tutorial_iii_explore",
        G_TUTORIAL,
        "III. 밖으로",
        "naturescompass:naturescompass",
        3,
        build_c3(),
        extra_ids=gate_extra("C2-27", "C2-15", "C2-02"),
    )
    write_chapter(
        "tutorial_iv_deep",
        G_TUTORIAL,
        "IV. 깊은 곳",
        "artifacts:umbrella",
        4,
        build_c4(),
        extra_ids=gate_extra("C3-28", "C3-27", "C3-09", "C3-03", "C3-20", "C2-21", "C1-20", "C4-09"),
    )
    write_chapter(
        "tutorial_v_auto",
        G_TUTORIAL,
        "V. 자동화",
        "pipez:item_pipe",
        5,
        build_c5(),
        extra_ids=gate_extra("C4-24", "C2-27", "C2-05", "C2-26", "C1-14", "C3-32"),
    )
    write_chapter(
        "daily_board",
        G_DAILY,
        "일일 의뢰",
        "minecraft:writable_book",
        0,
        build_daily(),
    )

    # Bundles
    write_chapter(
        "bundle_survival",
        G_BUNDLES,
        "번들: 생존 방",
        "realismsurvival:water_still",
        0,
        build_bundle_survival(),
        extra_ids=gate_extra("C1-23"),
    )
    write_chapter(
        "bundle_kitchen",
        G_BUNDLES,
        "번들: 부엌 방",
        "farmersdelight:stove",
        1,
        build_bundle_kitchen(),
        extra_ids=gate_extra("C2-27"),
    )
    write_chapter(
        "bundle_ranch",
        G_BUNDLES,
        "번들: 목축 방",
        "minecraft:hay_block",
        2,
        build_bundle_ranch(),
        extra_ids=gate_extra("C2-27"),
    )
    write_chapter(
        "bundle_explore",
        G_BUNDLES,
        "번들: 탐험 방",
        "minecraft:spyglass",
        3,
        build_bundle_explore(),
        extra_ids=gate_extra("C3-28"),
    )
    write_chapter(
        "bundle_workshop",
        G_BUNDLES,
        "번들: 공방 방",
        "ironchest:iron_chest",
        4,
        build_bundle_workshop(),
        extra_ids=gate_extra("C5-14"),
    )

    # Collections from catalog
    from build_collection_lists import main as build_lists

    build_lists()
    col = json.loads((ROOT / "tools" / "quest_catalog" / "collections.json").read_text(encoding="utf-8"))
    col_extra = gate_extra(*COLLECTION_GATES)

    write_chapter(
        "collection_crops",
        G_COLLECTION,
        "도감: 작물",
        "minecraft:wheat",
        0,
        build_collection_crops([(a, b) for a, b in col["crops"]]),
        auto_layout=False,
        extra_ids=col_extra,
    )
    write_chapter(
        "collection_food",
        G_COLLECTION,
        "도감: 음식·음료",
        "farmersdelight:cooking_pot",
        1,
        build_collection_food(col["cooking"], col["drinks"]),
        auto_layout=False,
        extra_ids=col_extra,
    )
    write_chapter(
        "collection_fish",
        G_COLLECTION,
        "도감: 물고기",
        "aquaculture:iron_fishing_rod",
        2,
        build_collection_fish([(a, b) for a, b in col["fish"]]),
        auto_layout=False,
        extra_ids=col_extra,
    )
    write_chapter(
        "collection_biomes",
        G_COLLECTION,
        "도감: 생물군계",
        "naturescompass:naturescompass",
        3,
        build_collection_biomes([(a, b) for a, b in col["biomes"]]),
        auto_layout=False,
        extra_ids=col_extra,
    )
    write_chapter(
        "collection_creatures",
        G_COLLECTION,
        "도감: 생물",
        "minecraft:iron_sword",
        4,
        build_collection_creatures(col["mobs"], col["animals"]),
        auto_layout=False,
        extra_ids=col_extra,
    )
    write_chapter(
        "collection_minerals",
        G_COLLECTION,
        "도감: 광물",
        "minecraft:diamond",
        5,
        build_collection_minerals([(a, b) for a, b in col["minerals"]]),
        auto_layout=False,
        extra_ids=col_extra,
    )
    write_chapter(
        "collection_artifacts",
        G_COLLECTION,
        "도감: 유물",
        "artifacts:umbrella",
        6,
        build_collection_artifacts([(a, b) for a, b in col["artifacts"]]),
        auto_layout=False,
        extra_ids=col_extra,
    )

    write_chapter(
        "collection_perfection",
        G_COLLECTION,
        "도감: 완벽",
        "minecraft:nether_star",
        10,
        build_perfection(),
        extra_ids=gate_extra(*PERF_EXTRA_IDS),
    )

    convert_snbt_tree_to_json5(OUT)
    sync_to_run()

    from fix_ftbquests_v26 import main as fix_v26

    fix_v26()

    chapters = list((OUT / "chapters").glob("*.json5"))
    total_q = 0
    for p in chapters:
        data = json.loads(p.read_text(encoding="utf-8"))
        total_q += len(data.get("quests") or [])
    print(f"Wrote {len(chapters)} chapters, {total_q} quests (+ lang/ko_kr)")
    print(f"Source: {OUT}")
    print(f"Synced: {RUN}")
    print(f"Seals: {BUNDLE_SEALS}")
    print(f"Collection clears: {COLLECTION_CLEARS}")


if __name__ == "__main__":
    main()
