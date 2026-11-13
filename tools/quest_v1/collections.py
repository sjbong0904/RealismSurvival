# -*- coding: utf-8 -*-
"""Collection chapters with milestone rewards (30/60/90/100%)."""
from __future__ import annotations

import math

from .core import (
    Quest,
    biome_task,
    check_task,
    item_reward,
    item_task,
    josa,
    kill_task,
    q_check,
    xp_levels_reward,
)


# Rare or reusable animal goods are held, not submitted.
ANIMAL_HOLD = {
    "minecraft:rabbit_foot",
    "minecraft:turtle_scute",
    "minecraft:goat_horn",
    "minecraft:saddle",
    "minecraft:name_tag",
    "minecraft:lead",
    "pet_home:pet_bed_red",
    "pet_home:collar_tag",
    "pet_home:wayward_lantern",
    "pet_home:feather_on_a_stick",
}


# 각 단계에는 서로 다른 아이템 2종을 1개씩 보상으로 줍니다. 같은 아이템은 한 도감의 한 단계에만 배정합니다.
# 30 → 100으로 희귀도가 올라간다. 채우기 어려운 도감일수록 천장이 높다.
#   생물군계(132곳 방문) > 유물(구조물) > 물고기(낚시) ≈ 생물(희귀 몹)
#   > 광물(항목은 적어도 100%는 별·네더라이트 이후) > 음식(부엌) > 작물(밭)
# 겉날개는 생물군계, 토템은 생물, 네더의 별은 광물, 마법 황금 사과는 음식.
# 레벨은 곁들임이라 작게만 올린다.
_LATE_REWARDS: dict[str, dict[float, tuple[list[tuple[str, int]], int]]] = {
    "crops": {
        0.30: ([("minecraft:spore_blossom", 1), ("minecraft:beehive", 1)], 2),
        0.60: ([("minecraft:bee_nest", 1), ("minecraft:honeycomb_block", 1)], 3),
        0.90: (
            [
                ("minecraft:flower_banner_pattern", 1),
                ("minecraft:globe_banner_pattern", 1),
            ],
            5,
        ),
        1.00: ([("minecraft:sniffer_egg", 1), ("minecraft:beacon", 1)], 8),
    },
    "food": {
        0.30: (
            [
                ("minecraft:glistering_melon_slice", 1),
                ("farmersdelight:honey_glazed_ham_block", 1),
            ],
            2,
        ),
        0.60: (
            [
                ("farmersdelight:roast_chicken_block", 1),
                ("farmersdelight:rice_roll_medley_block", 1),
            ],
            4,
        ),
        0.90: (
            [
                ("minecraft:honey_block", 1),
                ("farmersdelight:stuffed_pumpkin_block", 1),
            ],
            6,
        ),
        1.00: (
            [
                ("minecraft:experience_bottle", 1),
                ("minecraft:enchanted_golden_apple", 1),
            ],
            10,
        ),
    },
    "fish": {
        0.30: ([("minecraft:nautilus_shell", 1), ("minecraft:sea_lantern", 1)], 3),
        0.60: ([("minecraft:sponge", 1), ("minecraft:heart_of_the_sea", 1)], 5),
        0.90: (
            [
                ("minecraft:tide_armor_trim_smithing_template", 1),
                ("aquaculture:neptunium_block", 1),
            ],
            8,
        ),
        1.00: (
            [
                ("aquaculture:neptunium_fishing_rod", 1),
                ("minecraft:conduit", 1),
            ],
            12,
        ),
    },
    "bio": {
        0.30: ([("minecraft:ender_chest", 1), ("minecraft:ender_eye", 1)], 4),
        0.60: ([("waystones:warp_scroll", 1), ("minecraft:recovery_compass", 1)], 7),
        0.90: ([("minecraft:shulker_shell", 1), ("minecraft:shulker_box", 1)], 11),
        1.00: ([("minecraft:elytra", 1), ("minecraft:phantom_membrane", 1)], 18),
    },
    "crea": {
        0.30: ([("minecraft:magma_cream", 1), ("minecraft:slime_ball", 1)], 3),
        0.60: (
            [
                ("minecraft:wither_skeleton_skull", 1),
                ("minecraft:wither_rose", 1),
            ],
            5,
        ),
        0.90: ([("minecraft:creaking_heart", 1), ("minecraft:dragon_breath", 1)], 8),
        1.00: ([("minecraft:wolf_armor", 1), ("minecraft:totem_of_undying", 1)], 12),
    },
    "ore": {
        0.30: ([("minecraft:respawn_anchor", 1), ("minecraft:lodestone", 1)], 2),
        0.60: (
            [
                ("minecraft:snout_armor_trim_smithing_template", 1),
                ("minecraft:rib_armor_trim_smithing_template", 1),
            ],
            4,
        ),
        0.90: (
            [
                ("minecraft:ward_armor_trim_smithing_template", 1),
                ("minecraft:silence_armor_trim_smithing_template", 1),
            ],
            8,
        ),
        1.00: (
            [
                ("minecraft:netherite_upgrade_smithing_template", 1),
                ("minecraft:nether_star", 1),
            ],
            14,
        ),
    },
    "art": {
        0.30: ([("minecraft:trial_key", 1), ("minecraft:disc_fragment_5", 1)], 3),
        0.60: (
            [
                ("minecraft:ominous_trial_key", 1),
                ("minecraft:music_disc_pigstep", 1),
            ],
            6,
        ),
        0.90: (
            [
                ("minecraft:flow_armor_trim_smithing_template", 1),
                ("minecraft:music_disc_otherside", 1),
            ],
            10,
        ),
        1.00: ([("minecraft:heavy_core", 1), ("minecraft:dragon_head", 1)], 16),
    },
}


def _milestone_rewards(kind: str, pct: float) -> list[str]:
    qid = f"D-{kind}-m{int(pct * 100)}"
    items, levels = _LATE_REWARDS[kind][pct]
    rewards = [
        item_reward(f"{qid}:{item}", item, count) for item, count in items
    ]
    rewards.append(xp_levels_reward(qid, levels))
    return rewards


def _milestones(kind: str, entry_qids: list[str], intro: str) -> list[Quest]:
    n = len(entry_qids)
    if n == 0:
        return []
    out: list[Quest] = []
    for pct in (0.30, 0.60, 0.90, 1.00):
        need = max(1, math.ceil(n * pct))
        qid = f"D-{kind}-m{int(pct * 100)}"
        # gate (intro) + entries; min_required counts all deps
        deps = [intro] + list(entry_qids)
        min_req = (1 + need) if pct < 1.0 else None
        out.append(
            Quest(
                qid,
                f"도감 {int(pct * 100)}% 달성",
                0,
                0,
                [check_task(qid)],
                _milestone_rewards(kind, pct),
                deps,
                [
                    f"시작 과제를 포함해 {need}개 이상을 완료하면 이 단계가 열립니다."
                    if pct < 1.0
                    else "시작 과제를 포함해 모든 항목을 완료하면 도감을 마칩니다.",
                ],
                size=1.25 if pct < 1.0 else 1.5,
                shape="gear" if pct >= 1.0 else None,
                min_required_dependencies=min_req,
                hide_dependency_lines=True,
            )
        )
    return out


def _collection_intro(kind: str, title: str) -> list[str]:
    lines = [f"{title} 도감입니다. 항목을 하나씩 완료해 보세요."]
    if kind == "art":
        lines.append("유물을 보유하면 Artifacts 탭이 열립니다. 모험 탭이 먼저 열려 있어야 합니다.")
    elif kind == "food":
        lines.append("해당 아이템을 보유하면 완료됩니다. 수집한 요리를 먹으면 Farmer's Delight 탭의 음식 과제도 진행됩니다.")
    return lines


def _grid_cols(n: int) -> int:
    """Keep a codex about five rows tall so it stays near one screen."""
    if n <= 0:
        return 1
    return max(6, math.ceil(n / 5))


def _build_item_collection(
    kind: str,
    title: str,
    entries: list[tuple[str, str]],
    gate: str,
    *,
    consume: bool,
    cols: int | None = None,
) -> list[Quest]:
    intro = f"D-{kind}-intro"
    cols = cols or _grid_cols(len(entries))
    step = 1.45
    q: list[Quest] = [
        q_check(
            intro,
            f"{title} 도감 시작",
            deps=[gate],
            desc=_collection_intro(kind, title),
            size=1.25,
        )
    ]
    entry_qids: list[str] = []
    for i, (item, name) in enumerate(entries):
        qid = f"D-{kind}-{i:03d}"
        entry_qids.append(qid)
        q.append(
            Quest(
                qid,
                name,
                (i % cols) * step,
                2.6 + (i // cols) * step,
                [item_task(qid, item, 1, consume=True if consume else None)],
                [],
                [intro],
                [f"{josa(name, '을')} {'제출' if consume else '보유'}하세요."],
                consume=True if consume else None,
                hide_dependency_lines=True,
            )
        )
    miles = _milestones(kind, entry_qids, intro)
    for j, mq in enumerate(miles):
        mq.x = cols * step + 2.4
        mq.y = 2.6 + j * 1.9
    q.extend(miles)
    return q


def build_collection_crops(entries: list[tuple[str, str]]) -> list[Quest]:
    return _build_item_collection("crops", "작물", entries, "C2-05", consume=False)


def build_collection_food(cooking: list, drinks: list) -> list[Quest]:
    entries = [(a, b) for a, b in cooking] + [(a, b) for a, b in drinks]
    return _build_item_collection("food", "음식·음료", entries, "C2-12", consume=False)


def build_collection_fish(entries: list[tuple[str, str]]) -> list[Quest]:
    return _build_item_collection("fish", "물고기", entries, "C2-20", consume=False)


def build_collection_minerals(entries: list[tuple[str, str]]) -> list[Quest]:
    return _build_item_collection("ore", "광물", entries, "C4-09", consume=False)


def build_collection_artifacts(entries: list[tuple[str, str]]) -> list[Quest]:
    return _build_item_collection("art", "유물", entries, "C4-05", consume=False)


def build_collection_biomes(entries: list[tuple[str, str]]) -> list[Quest]:
    intro = "D-bio-intro"
    gate = "C3-02"
    cols = _grid_cols(len(entries))
    step = 1.45
    q: list[Quest] = [
        q_check(
            intro,
            "생물군계 도감 시작",
            deps=[gate],
            desc=[
                "생물군계를 방문해 도감을 채우세요. 방문하는 동안 Biomes O' Plenty 탭도 함께 채워지고, 그 탭의 생물군계를 모두 방문하면 전체 방문 과제도 끝납니다.",
                "맨 끝 도적의 섬을 방문하면 Fish of Thieves 탭의 섬 방문 과제도 끝납니다.",
            ],
            size=1.25,
        )
    ]
    entry_qids: list[str] = []
    for i, (biome, name) in enumerate(entries):
        qid = f"D-bio-{i:03d}"
        entry_qids.append(qid)
        q.append(
            Quest(
                qid,
                name,
                (i % cols) * step,
                2.6 + (i // cols) * step,
                [biome_task(qid, biome)],
                [],
                [intro],
                [f"{josa(name, '을')} 방문하세요."],
                hide_dependency_lines=True,
            )
        )
    miles = _milestones("bio", entry_qids, intro)
    for j, mq in enumerate(miles):
        mq.x = cols * step + 2.4
        mq.y = 2.6 + j * 1.9
    q.extend(miles)
    return q


def build_collection_creatures(mobs: list, animals: list) -> list[Quest]:
    intro = "D-crea-intro"
    gate = "C3-28"
    cols = _grid_cols(max(len(mobs), len(animals), 1))
    step = 1.45
    q: list[Quest] = [
        q_check(
            intro,
            "생물 도감 시작",
            deps=[gate],
            desc=["왼쪽에는 몬스터, 오른쪽에는 동물과 동물 부산물이 표시됩니다."],
            size=1.25,
        )
    ]
    entry_qids: list[str] = []
    for i, (entity, name) in enumerate(mobs):
        qid = f"D-mob-{i:03d}"
        entry_qids.append(qid)
        q.append(
            Quest(
                qid,
                name,
                (i % cols) * step,
                2.6 + (i // cols) * step,
                [kill_task(qid, entity, 1)],
                [],
                [intro],
                [f"{josa(name, '을')} 처치하세요."],
                hide_dependency_lines=True,
            )
        )
    animal_x = cols * step + 1.8
    animal_gate = "D-animal-gate"
    q.append(
        Quest(
            animal_gate,
            "동물·부산물 구역",
            animal_x,
            0.8,
            [check_task(animal_gate)],
            [],
            [intro],
            ["동물을 길들이거나 부산물을 모으세요."],
            hide_dependency_lines=True,
        )
    )
    for i, (item, name) in enumerate(animals):
        qid = f"D-animal-{i:03d}"
        entry_qids.append(qid)
        q.append(
            Quest(
                qid,
                name,
                animal_x + (i % cols) * step,
                2.6 + (i // cols) * step,
                [item_task(qid, item, 1, consume=True if item not in ANIMAL_HOLD else None)],
                [],
                [intro],
                [f"{josa(name, '을')} {'보유' if item in ANIMAL_HOLD else '제출'}하세요."],
                consume=True if item not in ANIMAL_HOLD else None,
                hide_dependency_lines=True,
            )
        )
    miles = _milestones("crea", entry_qids, intro)
    for j, mq in enumerate(miles):
        mq.x = animal_x + cols * step + 1.6
        mq.y = 2.6 + j * 1.9
    q.extend(miles)
    return q


COLLECTION_CLEARS = [
    "D-crops-m100",
    "D-food-m100",
    "D-fish-m100",
    "D-bio-m100",
    "D-crea-m100",
    "D-ore-m100",
    "D-art-m100",
]

COLLECTION_GATES = [
    "C2-05",
    "C2-12",
    "C2-20",
    "C3-02",
    "C3-28",
    "C4-09",
    "C4-05",
]
