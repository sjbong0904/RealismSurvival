# -*- coding: utf-8 -*-
"""Bundle rooms B1–B5 — 4 packs each + seal."""
from __future__ import annotations

from .core import Quest, adv_task, item_reward, q_check, q_item, xp_reward

RS = "realismsurvival"
MED = "rsmedicine"
FD = "farmersdelight"


def _pack(
    room: str,
    pack_id: str,
    title: str,
    items: list[tuple[str, int, bool]],
    reward_item: str,
    reward_count: int,
    gate: str,
    extra_rewards: list[str] | None = None,
) -> list[Quest]:
    """items: (item_id, count, consume)."""
    quests: list[Quest] = []
    item_qids: list[str] = []
    for i, (item, count, consume) in enumerate(items):
        qid = f"{pack_id}-{i + 1}"
        item_qids.append(qid)
        quests.append(
            q_item(
                qid,
                f"{title} · 품목 {i + 1}",
                item,
                count,
                deps=[gate] if i == 0 else [item_qids[0]],
                consume=consume,
                desc=[f"「{title}」꾸러미 품목입니다."],
            )
        )
    # parallel deps from gate for all items for freer fill — redo: all depend on gate
    for q in quests:
        q.deps = [gate]
    done = f"{pack_id}-done"
    quests.append(
        q_check(
            done,
            f"{title} · 완료",
            deps=item_qids,
            desc=[f"「{title}」꾸러미를 완성했습니다. 체크하세요."],
            rewards=[item_reward(done, reward_item, reward_count), *(extra_rewards or [])],
        )
    )
    return quests


def build_bundle_survival() -> list[Quest]:
    gate = "B1-gate"
    q = [
        q_check(
            gate,
            "생존 방 입장",
            deps=["C1-23"],
            desc=["생존 번들 방입니다. 오가며 꾸러미를 채우세요. 입장하려면 체크하세요."],
        )
    ]
    packs = [
        _pack(
            "B1",
            "B1-P1",
            "물의 네 얼굴",
            [
                (f"{RS}:distilled_water_bottle", 1, True),
                (f"{RS}:fresh_water_bottle", 1, True),
                (f"{RS}:stagnant_water_bucket", 1, True),
                (f"{RS}:salt_water_bucket", 1, True),
            ],
            f"{RS}:filter_cartridge",
            4,
            gate,
        ),
        _pack(
            "B1",
            "B1-P2",
            "차가움과 따뜻함",
            [
                (f"{RS}:fresh_ice_bucket", 1, False),
                (f"{RS}:snow_bucket", 1, False),
                (f"{RS}:thermometer", 1, False),
                (f"{MED}:warm_pack", 1, False),
            ],
            f"{MED}:warm_pack",
            2,
            gate,
        ),
        _pack(
            "B1",
            "B1-P3",
            "거름과 소금",
            [
                (f"{RS}:water_filter", 1, False),
                (f"{RS}:filter_cartridge", 1, False),
                (f"{RS}:salt", 4, True),
                (f"{RS}:salt_block", 1, False),
            ],
            f"{RS}:salt_block",
            4,
            gate,
        ),
        _pack(
            "B1",
            "B1-P4",
            "응급 상자",
            [
                (f"{MED}:bandage", 1, False),
                (f"{MED}:trauma_kit", 1, False),
                (f"{MED}:antidote", 1, False),
                (f"{MED}:burn_salve", 1, False),
            ],
            f"{MED}:bandage",
            6,
            gate,
        ),
    ]
    for p in packs:
        q.extend(p)
    for quest in q:
        if quest.qid == "B1-P4-done":
            quest.rewards.append(item_reward("B1-P4-done-b", f"{MED}:antidote", 1))
    q.append(
        q_check(
            "B1-seal",
            "생존 인장",
            deps=["B1-P1-done", "B1-P2-done", "B1-P3-done", "B1-P4-done"],
            desc=["생존 방의 인장입니다. 보상을 받으려면 체크하세요."],
            rewards=[
                item_reward("B1-seal-a", f"{RS}:filter_cartridge", 4),
                item_reward("B1-seal-b", "minecraft:gold_ingot", 2),
                xp_reward("B1-seal", 40),
            ],
            size=1.5,
            shape="gear",
        )
    )
    return q


def build_bundle_kitchen() -> list[Quest]:
    gate = "B2-gate"
    q = [
        q_check(
            gate,
            "부엌 방 입장",
            deps=["C2-27"],
            desc=["부엌 번들 방입니다. 입장하려면 체크하세요."],
        )
    ]
    packs = [
        _pack(
            "B2",
            "B2-P1",
            "텃밭의 네 가지",
            [
                (f"{FD}:cabbage", 1, True),
                (f"{FD}:tomato", 1, True),
                (f"{FD}:onion", 1, True),
                (f"{FD}:rice", 1, True),
            ],
            f"{FD}:cabbage_seeds",
            8,
            gate,
            extra_rewards=[
                item_reward("B2-P1-extra-tomato", f"{FD}:tomato_seeds", 8),
                item_reward("B2-P1-extra-onion", f"{FD}:onion", 8),
                item_reward("B2-P1-extra-rice", f"{FD}:rice", 8),
            ],
        ),
        _pack(
            "B2",
            "B2-P2",
            "손에 쥔 도구",
            [
                (f"{FD}:cutting_board", 1, False),
                (f"{FD}:flint_knife", 1, False),
                (f"{FD}:stove", 1, False),
                (f"{FD}:cooking_pot", 1, False),
                (f"{FD}:skillet", 1, False),
            ],
            f"{FD}:onion",
            8,
            gate,
        ),
        _pack(
            "B2",
            "B2-P3",
            "한 상 차림",
            [
                (f"{FD}:vegetable_soup", 1, True),
                (f"{FD}:beef_stew", 1, True),
                (f"{FD}:roast_chicken_block", 1, False),
                (f"{FD}:fruit_salad", 1, True),
            ],
            "minecraft:bread",
            16,
            gate,
            extra_rewards=[
                item_reward("B2-P3-extra-bowl", "minecraft:bowl", 8),
                item_reward("B2-P3-extra-beef", "minecraft:cooked_beef", 8),
            ],
        ),
        _pack(
            "B2",
            "B2-P4",
            "열대의 과일",
            [
                ("fishofthieves:mango", 1, True),
                ("fishofthieves:pineapple", 1, True),
                ("fishofthieves:banana", 1, True),
                ("fishofthieves:pomegranate", 1, True),
            ],
            "fishofthieves:mango",
            8,
            gate,
            extra_rewards=[
                item_reward("B2-P4-extra-pine", "fishofthieves:pineapple", 4),
                item_reward("B2-P4-extra-banana", "fishofthieves:banana", 4),
                item_reward("B2-P4-extra-pom", "fishofthieves:pomegranate", 4),
            ],
        ),
    ]
    for p in packs:
        q.extend(p)
    q.append(
        q_check(
            "B2-seal",
            "부엌 인장",
            deps=["B2-P1-done", "B2-P2-done", "B2-P3-done", "B2-P4-done"],
            desc=["부엌 방의 인장입니다. 보상을 받으려면 체크하세요."],
            rewards=[
                item_reward("B2-seal-a", f"{FD}:cabbage_seeds", 8),
                item_reward("B2-seal-b", f"{FD}:tomato_seeds", 8),
                item_reward("B2-seal-c", f"{FD}:onion", 8),
                item_reward("B2-seal-d", "minecraft:gold_ingot", 2),
                xp_reward("B2-seal", 40),
            ],
            size=1.5,
            shape="gear",
        )
    )
    return q


def build_bundle_explore() -> list[Quest]:
    gate = "B3-gate"
    q: list[Quest] = [
        q_check(
            gate,
            "탐험 방 입장",
            deps=["C3-28"],
            desc=["탐험 번들 방입니다. 입장하려면 체크하세요."],
        )
    ]
    q.extend(
        _pack(
            "B3",
            "B3-P1",
            "생물군계의 선물",
            [
                ("biomesoplenty:rose_quartz_chunk", 1, True),
                ("biomesoplenty:reed", 1, True),
                ("biomesoplenty:cattail", 1, True),
                ("biomesoplenty:glowshroom", 1, True),
            ],
            "minecraft:paper",
            32,
            gate,
            extra_rewards=[
                item_reward("B3-P1-extra-quartz", "biomesoplenty:rose_quartz_chunk", 4),
            ],
        )
    )
    q.append(
        q_check(
            "B3-P2-1",
            "구조물 도장 · 마을",
            deps=[gate],
            desc=["마을 한 곳을 방문한 뒤 체크하세요."],
        )
    )
    q.append(q_check("B3-P2-2", "구조물 도장 · 탑", deps=[gate], desc=["탑 한 곳을 방문한 뒤 체크하세요."]))
    q.append(q_check("B3-P2-3", "구조물 도장 · 전초", deps=[gate], desc=["전초기지 한 곳을 방문한 뒤 체크하세요."]))
    q.append(q_check("B3-P2-4", "구조물 도장 · 폐허", deps=[gate], desc=["폐허 한 곳을 방문한 뒤 체크하세요."]))
    q.append(
        q_check(
            "B3-P2-done",
            "구조물 도장 · 완료",
            deps=["B3-P2-1", "B3-P2-2", "B3-P2-3", "B3-P2-4"],
            rewards=[
                item_reward("B3-P2a", "minecraft:torch", 32),
                item_reward("B3-P2b", "waystones:return_scroll", 4),
            ],
        )
    )
    q.append(
        Quest(
            "B3-P3-1",
            "열어 본 상자 · 1",
            0,
            0,
            [adv_task("B3-P3-1", "lootr:1chest")],
            [],
            [gate],
            ["Lootr 상자를 하나 여세요."],
        )
    )
    q.append(
        Quest(
            "B3-P3-2",
            "열어 본 상자 · 10",
            0,
            0,
            [adv_task("B3-P3-2", "lootr:10loot")],
            [],
            [gate],
            ["Lootr 상자를 열 개 여세요."],
        )
    )
    q.append(
        Quest(
            "B3-P3-3",
            "열어 본 상자 · 더 멀리",
            0,
            0,
            [adv_task("B3-P3-3", "lootr:25loot")],
            [],
            [gate],
            ["Lootr 상자를 25개 열면 완료됩니다."],
        )
    )
    q.append(
        Quest(
            "B3-P3-4",
            "열어 본 상자 · 100개",
            0,
            0,
            [adv_task("B3-P3-4", "lootr:100loot")],
            [],
            [gate],
            [
                "Lootr 상자를 100개 열면 완료됩니다.",
                "상자 50개 열기 과제도 25개와 100개 과제 사이에서 자동으로 완료됩니다.",
            ],
        )
    )
    q.append(
        q_check(
            "B3-P3-done",
            "열어 본 상자 · 완료",
            deps=["B3-P3-1", "B3-P3-2", "B3-P3-3", "B3-P3-4"],
            rewards=[item_reward("B3-P3", "minecraft:gold_ingot", 16)],
        )
    )
    q.extend(
        _pack(
            "B3",
            "B3-P4",
            "길잡이의 도구",
            [
                ("naturescompass:naturescompass", 1, False),
                ("waystones:andesite_waystone", 1, False),
                ("waystones:return_scroll", 1, False),
                ("minecraft:compass", 1, False),
            ],
            "waystones:return_scroll",
            2,
            gate,
        )
    )
    q.append(
        q_check(
            "B3-seal",
            "탐험 인장",
            deps=["B3-P1-done", "B3-P2-done", "B3-P3-done", "B3-P4-done"],
            desc=["탐험 방의 인장입니다. 보상을 받으려면 체크하세요."],
            rewards=[
                item_reward("B3-seal-a", "waystones:return_scroll", 4),
                item_reward("B3-seal-b", "minecraft:gold_ingot", 2),
                xp_reward("B3-seal", 40),
            ],
            size=1.5,
            shape="gear",
        )
    )
    return q


def build_bundle_ranch() -> list[Quest]:
    gate = "B4-gate"
    q = [
        q_check(
            gate,
            "목축 방 입장",
            deps=["C2-27"],
            desc=["목축 번들 방입니다. 입장하려면 체크하세요."],
        )
    ]
    packs = [
        _pack(
            "B4",
            "B4-P1",
            "외양간의 산물",
            [
                ("minecraft:milk_bucket", 1, True),
                ("minecraft:egg", 4, True),
                ("minecraft:leather", 4, True),
                ("minecraft:white_wool", 4, True),
            ],
            "minecraft:oak_fence",
            16,
            gate,
            extra_rewards=[item_reward("B4-P1-extra-leather", "minecraft:leather", 8)],
        ),
        _pack(
            "B4",
            "B4-P2",
            "벌통",
            [
                ("minecraft:honey_bottle", 1, True),
                ("minecraft:beehive", 1, False),
                ("minecraft:honey_block", 1, False),
                ("minecraft:honeycomb", 1, True),
            ],
            "minecraft:sugar",
            16,
            gate,
            extra_rewards=[item_reward("B4-P2-extra-honey", "minecraft:honey_bottle", 4)],
        ),
        _pack(
            "B4",
            "B4-P3",
            "한 식구",
            [
                ("minecraft:bone", 8, True),
                ("minecraft:cod", 8, True),
                ("pet_home:pet_bed_red", 1, False),
                ("waystones:return_scroll", 1, False),
            ],
            "minecraft:bone",
            16,
            gate,
            extra_rewards=[item_reward("B4-P3-extra-cod", "minecraft:cooked_cod", 8)],
        ),
        _pack(
            "B4",
            "B4-P4",
            "달리는 친구",
            [
                ("minecraft:saddle", 1, False),
                ("minecraft:carrot", 8, True),
                ("minecraft:leather_horse_armor", 1, False),
                ("minecraft:hay_block", 1, False),
            ],
            "minecraft:leather",
            8,
            gate,
        ),
    ]
    for p in packs:
        q.extend(p)
    q.append(
        q_check(
            "B4-seal",
            "목축 인장",
            deps=["B4-P1-done", "B4-P2-done", "B4-P3-done", "B4-P4-done"],
            desc=["목축 방의 인장입니다. 보상을 받으려면 체크하세요."],
            rewards=[
                item_reward("B4-seal-a", "minecraft:saddle", 1),
                item_reward("B4-seal-b", "minecraft:name_tag", 1),
                item_reward("B4-seal-c", "minecraft:gold_ingot", 2),
                xp_reward("B4-seal", 40),
            ],
            size=1.5,
            shape="gear",
        )
    )
    return q


def build_bundle_workshop() -> list[Quest]:
    gate = "B5-gate"
    q = [
        q_check(
            gate,
            "공방 방 입장",
            deps=["C5-14"],
            desc=["급수 시설 이후의 공방 번들입니다. 본선에서 이미 만든 상자·관·탱크로는 채워지지 않습니다. 입장하려면 체크하세요."],
        )
    ]
    packs = [
        _pack(
            "B5",
            "B5-P1",
            "큰 상자",
            [
                ("ironchest:gold_to_diamond_chest_upgrade", 1, False),
                ("ironchest:diamond_chest", 1, False),
                ("ironchest:diamond_to_crystal_chest_upgrade", 1, False),
                ("ironchest:crystal_chest", 1, False),
            ],
            "minecraft:obsidian",
            24,
            gate,
        ),
        _pack(
            "B5",
            "B5-P2",
            "관의 도구",
            [
                ("pipez:item_pipe", 32, False),
                ("pipez:fluid_pipe", 32, False),
                ("pipez:energy_pipe", 8, False),
                ("pipez:filter_destination_tool", 1, False),
            ],
            "minecraft:copper_ingot",
            16,
            gate,
        ),
        _pack(
            "B5",
            "B5-P3",
            "물 저장",
            [
                ("fluidtank:tank_gold", 1, False),
                ("fluidtank:tank_diamond", 1, False),
                ("fluidtank:reservoir_iron", 2, False),
                (f"{RS}:distilled_water_bottle", 16, True),
            ],
            f"{RS}:filter_cartridge",
            8,
            gate,
        ),
        _pack(
            "B5",
            "B5-P4",
            "넵튜늄 가공",
            [
                ("aquaculture:neptunium_ingot", 4, False),
                ("aquaculture:neptunium_chestplate", 1, False),
                ("aquaculture:neptunium_leggings", 1, False),
                ("waystones:warp_scroll", 4, True),
            ],
            "waystones:warp_scroll",
            8,
            gate,
        ),
    ]
    for p in packs:
        q.extend(p)
    q.append(
        q_check(
            "B5-seal",
            "공방 인장",
            deps=["B5-P1-done", "B5-P2-done", "B5-P3-done", "B5-P4-done"],
            desc=["공방 방의 인장입니다. 보상을 받으려면 체크하세요."],
            rewards=[
                item_reward("B5-seal-a", "fluidtank:tank_gold", 1),
                item_reward("B5-seal-b", "minecraft:gold_ingot", 4),
                xp_reward("B5-seal", 60),
            ],
            size=1.5,
            shape="gear",
        )
    )
    return q


BUNDLE_SEALS = ["B1-seal", "B2-seal", "B3-seal", "B4-seal", "B5-seal"]
BUNDLE_GATES = {
    "survival": ["C1-23"],
    "kitchen": ["C2-27"],
    "explore": ["C3-28"],
    "ranch": ["C2-27"],
    "workshop": ["C5-09"],
}
