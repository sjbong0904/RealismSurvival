# -*- coding: utf-8 -*-
"""Minimal daily board: repeatable FTB quests; gold capped by Overworld day (3/day)."""

from __future__ import annotations

from .core import Quest, biome_task, daily_coin_reward, item_task, kill_task, q_check


def _daily(qid: str, title: str, tasks: list[str], coins: int, desc: list[str]) -> Quest:
    return Quest(
        qid,
        title,
        0,
        0,
        tasks,
        [daily_coin_reward(qid, coins)],
        ["D0-00"],
        desc
        + [
            "오버월드 하루 동안 골드 보상은 최대 3건입니다. 과제는 더 할 수 있지만 보상은 한도까지만 받습니다.",
        ],
        can_repeat=True,
        repeat_cooldown=0,
        optional=True,
    )


def build_daily() -> list[Quest]:
    return [
        q_check(
            "D0-00",
            "일일 의뢰판",
            deps=[],
            desc=[
                "가벼운 반복 의뢰입니다. 보상을 수령하면 골드가 들어옵니다.",
                "오버월드 하루 기준 골드 보상은 3건까지입니다. 한도는 서버 설정(dailyBoardClaimLimit)으로 바꿀 수 있습니다.",
                "안내를 읽었으면 체크하세요.",
            ],
            size=1.5,
            shape="gear",
        ),
        _daily(
            "D0-01",
            "소금 네 개",
            [item_task("D0-01", "realismsurvival:salt", 4, consume=True)],
            70,
            ["소금 네 개를 제출하세요."],
        ),
        _daily(
            "D0-02",
            "증류수 세 병",
            [item_task("D0-02", "realismsurvival:distilled_water_bottle", 3, consume=True)],
            90,
            ["증류수 병 세 개를 제출하세요."],
        ),
        _daily(
            "D0-03",
            "빵 여덟",
            [item_task("D0-03", "minecraft:bread", 8, consume=True)],
            60,
            ["빵 여덟 개를 제출하세요."],
        ),
        _daily(
            "D0-04",
            "석탄 열여섯",
            [item_task("D0-04", "minecraft:coal", 16, consume=True)],
            65,
            ["석탄 열여섯 개를 제출하세요."],
        ),
        _daily(
            "D0-05",
            "좀비 열",
            [kill_task("D0-05", "minecraft:zombie", 10)],
            80,
            ["좀비 열 마리를 처치하세요."],
        ),
        _daily(
            "D0-06",
            "스켈레톤 여덟",
            [kill_task("D0-06", "minecraft:skeleton", 8)],
            85,
            ["스켈레톤 여덟 마리를 처치하세요."],
        ),
        _daily(
            "D0-07",
            "익힌 필렛",
            [item_task("D0-07", "aquaculture:fish_fillet_cooked", 3, consume=True)],
            100,
            ["익힌 생선 필렛 세 개를 제출하세요."],
        ),
        _daily(
            "D0-08",
            "양배추·토마토",
            [
                item_task("D0-08a", "farmersdelight:cabbage", 2, consume=True),
                item_task("D0-08b", "farmersdelight:tomato", 2, consume=True),
            ],
            75,
            ["양배추와 토마토를 각각 두 개씩 제출하세요."],
        ),
        _daily(
            "D0-09",
            "평원 산책",
            [biome_task("D0-09", "minecraft:plains")],
            50,
            ["평원에 들어가면 완료됩니다."],
        ),
        _daily(
            "D0-10",
            "필터 카트리지",
            [item_task("D0-10", "realismsurvival:filter_cartridge", 1, consume=True)],
            110,
            ["필터 카트리지 한 개를 제출하세요."],
        ),
        _daily(
            "D0-11",
            "붕대 네 장",
            [item_task("D0-11", "rsmedicine:bandage", 4, consume=True)],
            95,
            ["붕대 네 장을 제출하세요."],
        ),
        _daily(
            "D0-12",
            "철 원석",
            [item_task("D0-12", "minecraft:raw_iron", 8, consume=True)],
            120,
            ["철 원석 여덟 개를 제출하세요."],
        ),
    ]
