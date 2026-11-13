# -*- coding: utf-8 -*-
"""Perfection P-01–P-10."""
from __future__ import annotations

from .bundles import BUNDLE_SEALS
from .collections import COLLECTION_CLEARS
from .core import Quest, item_reward, q_check, stat_task, xp_reward

# Serene Seasons: 8 days × 12 sub-seasons
YEAR_DAYS = 96


def build_perfection() -> list[Quest]:
    return [
        q_check(
            "P-01",
            "첫 번째 문",
            deps=list(BUNDLE_SEALS),
            desc=["번들 인장 다섯 개를 모두 모은 뒤 체크하세요."],
            size=1.25,
        ),
        q_check(
            "P-02",
            "두 번째 문",
            deps=list(COLLECTION_CLEARS),
            desc=["도감 100% 인장 일곱 개를 모두 모은 뒤 체크하세요."],
            size=1.25,
        ),
        q_check(
            "P-03",
            "완벽의 장",
            deps=["P-01", "P-02"],
            desc=["다섯 인장과 일곱 도감을 모았습니다. 시험을 시작하려면 체크하세요."],
            rewards=[xp_reward("P-03", 50)],
            size=1.5,
            shape="gear",
        ),
        Quest(
            "P-04",
            "생존의 시험",
            0,
            0,
            [stat_task("P-04", "minecraft:time_since_death", (YEAR_DAYS // 12) * 24000)],
            [item_reward("P-04", "minecraft:gold_ingot", 8)],
            ["C4-24"],
            [f"마지막으로 죽은 뒤 게임 시간으로 {YEAR_DAYS // 12}일을 버티세요. 통계 항목은 time_since_death입니다."],
        ),
        q_check(
            "P-05",
            "식탁의 시험",
            deps=["P-03"],
            desc=[
                "음식 책에 서로 다른 음식 50종이 기록되었는지 확인한 뒤 직접 체크하세요.",
                "먹은 음식 종류 수는 퀘스트가 자동으로 세지 못합니다.",
            ],
            rewards=[item_reward("P-05", "minecraft:gold_ingot", 8)],
        ),
        Quest(
            "P-06",
            "길의 시험",
            0,
            0,
            [stat_task("P-06", "minecraft:walk_one_cm", 2_000_000)],
            [item_reward("P-06", "minecraft:gold_ingot", 8)],
            ["P-03"],
            ["도보로 20km를 걸으세요. 게임 통계 `walk_one_cm` 기준이며, 전력질주와 탈것 이동은 포함되지 않습니다."],
        ),
        Quest(
            "P-07",
            "가축의 시험",
            0,
            0,
            [stat_task("P-07", "minecraft:animals_bred", 20)],
            [item_reward("P-07", "minecraft:gold_ingot", 8)],
            ["P-03"],
            [
                "동물을 20번 번식시키세요. 통계 항목은 animals_bred입니다. 처음 번식시키면 농사 탭의 교배 과제도 완료됩니다.",
            ],
        ),
        q_check(
            "P-08",
            "공방의 시험",
            deps=["P-03"],
            desc=[
                "아래 세 설비 조건을 모두 갖춘 뒤 직접 체크하세요.",
                "1) 증류기가 유체 파이프에 연결되어 있다.",
                "2) 정수 필터가 탱크에 연결되어 있다.",
                "3) 철 저장조에 물이 들어 있다.",
            ],
            rewards=[item_reward("P-08", "minecraft:gold_ingot", 8)],
        ),
        Quest(
            "P-09",
            "한 해",
            0,
            0,
            [stat_task("P-09", "minecraft:play_time", YEAR_DAYS * 24000)],
            [item_reward("P-09", "minecraft:gold_ingot", 8)],
            ["C4-24"],
            [
                f"게임 시간으로 {YEAR_DAYS}일을 보내세요. Serene Seasons 한 해이며, 통계는 play_time {YEAR_DAYS * 24000}틱입니다.",
                "월드 생성 이후 누적 시간 기준입니다. 퀘스트가 열리기 전에 목표를 채웠다면 열릴 때 바로 완료됩니다.",
            ],
        ),
        q_check(
            "P-10",
            "완벽",
            deps=["P-04", "P-05", "P-06", "P-07", "P-08", "P-09"],
            desc=[
                "완벽한 생존자가 되었습니다. 모험은 계속됩니다. 보상을 받으려면 체크하세요.",
                "모드팩의 주요 여정을 모두 마쳤습니다. 기념으로 거점에 둘 드래곤 알을 받습니다.",
            ],
            rewards=[
                item_reward("P-10", "minecraft:dragon_egg", 1),
                xp_reward("P-10", 500),
            ],
            size=2.0,
            shape="gear",
        ),
    ]


PERF_EXTRA_IDS = list(BUNDLE_SEALS) + list(COLLECTION_CLEARS) + ["C4-24"]
