# -*- coding: utf-8 -*-
"""Rewrite docs/Realism Survival — 퀘스트 진행표 v1.md from current quest_v1 + QUEST_LIST.csv."""
from __future__ import annotations

import csv
import re
import subprocess
import sys
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "docs" / "Realism Survival — 퀘스트 진행표 v1.md"


def short_item(s: str) -> str:
    """minecraft:bread → 빵 id 유지하되 읽기 쉽게: keep registry id, drop backticks noise."""
    return s.replace("`", "")


def fmt_done(complete: str) -> str:
    s = short_item(complete)
    s = s.replace("보유 ", "보유: ").replace("제출 ", "제출: ")
    s = s.replace("발전과제 ", "발전과제: ").replace("생물군계 방문 ", "생물군계: ")
    s = s.replace("처치 ", "처치: ")
    return s


def fmt_rew(rewards: str) -> str:
    if rewards == "—":
        return "—"
    return short_item(rewards).replace(" · ", ", ")


def fmt_unlock(unlock: str) -> str:
    # "T0-01 (퀘스트북 펼치기)" → keep; strip optional marker duplication later
    s = unlock.replace(" 〔선택〕", "")
    # Titles may themselves contain parentheses, e.g. "첫 매입 (선택)".
    s = re.sub(r" \([^()]*(?:\([^()]*\)[^()]*)*\)", "", s)
    return s if s != "없음 (시작)" else "—"


def learn_from_desc(desc: str, title: str) -> str:
    if not desc or desc == "—":
        return title
    # first sentence as "배우는 것"
    part = re.split(r"[.。]", desc)[0].strip()
    return part if part else title


def load_rows() -> list[dict]:
    subprocess.run([sys.executable, str(ROOT / "tools" / "export_quest_list.py")], check=True, cwd=ROOT)
    with (ROOT / "docs" / "QUEST_LIST.csv").open(encoding="utf-8-sig") as f:
        return list(csv.DictReader(f))


def table(rows: list[dict], *, include_optional_col: bool = False) -> str:
    lines = [
        "| ID | 퀘스트 | 과제 | 선행 | 배우는 것 | 보상 |",
        "| --- | --- | --- | --- | --- | --- |",
    ]
    for r in rows:
        title = r["title"]
        if r.get("optional") == "예" and "(선택)" not in title:
            title = title + " (선택)"
        lines.append(
            "| "
            + " | ".join(
                [
                    r["id"],
                    title,
                    fmt_done(r["complete"]),
                    fmt_unlock(r["unlock"]),
                    learn_from_desc(r["desc"], r["title"]),
                    fmt_rew(r["rewards"]),
                ]
            )
            + " |"
        )
    return "\n".join(lines)


def section_intro_quests(by: dict, chapter: str) -> list[dict]:
    return by[chapter]


def main() -> None:
    rows = load_rows()
    by: dict[str, list[dict]] = defaultdict(list)
    for r in rows:
        by[r["chapter"]].append(r)

    counts = {ch: len(rs) for ch, rs in by.items()}
    mainline = (
        counts["0. 시작하기"]
        + counts["I. 생존"]
        + counts["II. 밭과 부엌"]
        + counts["III. 밖으로"]
        + counts["IV. 깊은 곳"]
        + counts["V. 자동화"]
    )
    bundles = sum(counts[c] for c in by if c.startswith("번들"))
    collections = sum(counts[c] for c in by if c.startswith("도감") and "Perfection" not in c)

    # I chapter: split side branches
    c1 = by["I. 생존"]
    c1_main = [r for r in c1 if not r["id"].startswith("C1-S")]
    c1_side = [r for r in c1 if r["id"].startswith("C1-S")]
    c2 = by["II. 밭과 부엌"]
    c2_main = [r for r in c2 if not r["id"].startswith("C2-S")]
    c2_side = [r for r in c2 if r["id"].startswith("C2-S")]

    def side_section(rows: list[dict]) -> str:
        if not rows:
            return ""
        return f"### 곁가지 (선택)\n\n{table(rows)}\n"

    # Bundle packs summary from titles
    def bundle_summary(chapter: str) -> str:
        rs = by[chapter]
        packs = []
        for r in rs:
            m = re.match(r"(B\d-P\d)-done", r["id"])
            if m:
                title = r["title"].replace(" · 완료", "")
                packs.append(title)
        gate = next((r for r in rs if r["id"].endswith("-gate")), None)
        seal = next((r for r in rs if r["id"].endswith("-seal")), None)
        gate_from = fmt_unlock(gate["unlock"]) if gate else "?"
        seal_rew = fmt_rew(seal["rewards"]) if seal else "—"
        pack_lines = "\n".join(f"- {p}" for p in packs)
        return f"**열리는 시점:** `{gate_from}` 완료 후\n\n**꾸러미:**\n{pack_lines}\n\n**인장 보상:** {seal_rew}"

    # Collection metas
    col_meta = [
        ("도감 · 작물", "C2-05", "보유", counts["도감 · 작물"]),
        ("도감 · 음식·음료", "C2-12", "보유", counts["도감 · 음식·음료"]),
        ("도감 · 물고기", "C2-20", "보유", counts["도감 · 물고기"]),
        ("도감 · 생물군계", "C3-02", "생물군계 방문", counts["도감 · 생물군계"]),
        ("도감 · 생물", "C3-28", "처치 / 제출", counts["도감 · 생물"]),
        ("도감 · 광물", "C4-09", "보유", counts["도감 · 광물"]),
        ("도감 · 유물", "C4-05", "보유", counts["도감 · 유물"]),
    ]

    doc = f"""# Realism Survival — 퀘스트 진행표 v1

구현 반영본 · 생성기 `tools/quest_v1/` · 전체 표 `docs/QUEST_LIST.md`  
갱신: `python tools/sync_progression_doc.py`

## 설계 원칙

이 퀘스트북은 "새 시스템 하나를 소개하고, 직접 쓰게 만들고, 다음 발에 쓸 재료를 보상으로 준다"는 한 가지 리듬으로 짠다. 튜토리얼(0~V장)이 진행의 길이고, 번들·도감은 길 옆의 놀이터다.

| Sunlit Valley의 장치 | 이 팩의 대응 |
| --- | --- |
| 퀘스트북이 진행 순서를 단계별로 안내 | 튜토리얼 0·I~V장 (모드를 한 번에 하나씩 소개) |
| 번들: 아이템을 모아 제출하면 보상 | 번들 5개 방(생존·부엌·목축·탐험·공방)과 방별 인장 |
| 번 돈을 다시 투자하는 경제 | 공급소 상점 + 골드(SG Economy). 필수가 아닌 선택 갈래 |
| 분야별 수집 챕터 | 도감 7종 + 30/60/90/100% 단계 |
| 분야별 스킬 트리 | 없음. 인장과 도감 등급이 "숙련"을 대신함 |

규칙:

1. **한 퀘스트 = 새로 배우는 것 한 가지.**
2. **장 안은 순서가 있다. 장의 첫 퀘스트와 도감 항목은 서로 막지 않는다.** 번들은 그 장을 마친 뒤에 열린다.
3. **보상은 "지금 할 수 있게 된 것" 또는 "바로 다음 재료"만.** 힘 자체를 올리는 보상은 주지 않는다.
4. **위험을 먼저, 해결책은 그다음.**
5. **돈·상점은 선택 갈래.**
6. **멀티에서 제출·마시기는 각자.** 땅과 파티만 공유.

## 전체 지도와 진행 잠금

```text
0·I·II·III·IV·V 각 장의 첫 퀘스트는 처음부터 열려 있다.
장 안의 순서는 유지된다. 번들만 그 장을 마치면 열린다.
                │            │              │                    │
                └ 생존 번들   ├ 부엌·목축 번들  └ 탐험 번들         └ 공방 번들(C5-14)
                             │
                    도감 7종 (각 입장 퀘스트에서 열림)
                             │
              인장 5 + 도감 100% 7 → Perfection P-01~P-10
```

| 큰 탭 | 구성 | 현재 퀘스트 수 | 비고 |
| --- | --- | --- | --- |
| 튜토리얼 | 0·I~V | **{mainline}** | 0={counts['0. 시작하기']}, I={counts['I. 생존']}(본선 {len(c1_main)}+곁가지 {len(c1_side)}), II={counts['II. 밭과 부엌']}, III={counts['III. 밖으로']}, IV={counts['IV. 깊은 곳']}, V={counts['V. 자동화']} |
| 번들 | 5방 | **{bundles}** | 방마다 입장+꾸러미×4+인장 |
| 도감 | 7종 | **{collections}** | 시작+항목+30/60/90/100% |
| 완벽 | P-01~P-10 | **{counts['도감 · Perfection']}** | 도감 탭 안 |

총합 약 **{len(rows)}**개 (`docs/QUEST_LIST.md`와 동일).

**장·방 열림**

| 조건 | 열리는 것 |
| --- | --- |
| 처음부터 | I~V장 첫 퀘스트 |
| C1-23 | 생존 번들 |
| C2-27 | 부엌·목축 번들 |
| C3-28 | 탐험 번들 |
| C5-14 | 공방 번들 |
| C2-05 / C2-12 / C2-20 / C3-02 / C3-28 / C4-09 / C4-05 | 각 도감 |
| 인장 5 + 도감 100%×7 | Perfection 문 (P-01·P-02) |

## 0장 시작하기 ({counts['0. 시작하기']}개)

화면·땅·지도를 익히고 갈증 문제를 던져 I장으로 넘긴다.

{table(by['0. 시작하기'])}

## I 생존 (본선 {len(c1_main)} + 곁가지 {len(c1_side)})

증류·정수·소금·체온·회복을 한 번씩 손에 쥐게 한다.

{table(c1_main)}

{side_section(c1_side)}
## II 밭과 부엌 (본선 {len(c2_main)} + 곁가지 {len(c2_side)})

계절 → 밭 → 부엌 → 골고루 먹기. 목축·낚시·백팩을 곁에 붙인다.  
C2-05는 작물 네 갈래(C2-05a~d) 중 **2개만** 끝내면 통과한다.

{table(c2_main)}

{side_section(c2_side)}
## III 밖으로 ({counts['III. 밖으로']}개)

생물군계·거점·펫·구조물·마을·공급소·의료. 워프스톤은 IV로 미루고 예고만.

{table(by['III. 밖으로'])}

## IV 깊은 곳 ({counts['IV. 깊은 곳']}개)

유물·광물·넵튜늄·워프스톤·극한 원정·상위 의료.

{table(by['IV. 깊은 곳'])}

## V 자동화 ({counts['V. 자동화']}개)

상자·관·탱크·물 라인. Pipez는 **개선 업그레이드까지**만 본선. 시설 라인은 체크마크.

{table(by['V. 자동화'])}

## 번들: 5개 방

방마다 **입장 퀘스트 → 꾸러미 4개(품목+완료) → 인장**.  
소모품·재료는 제출(소비), 도구·장비·블록은 보유. 상세 품목은 `docs/QUEST_LIST.md` 번들 절.

### 생존 방 ({counts['번들 · 생존 방']}퀘)

{bundle_summary('번들 · 생존 방')}

### 부엌 방 ({counts['번들 · 부엌 방']}퀘)

{bundle_summary('번들 · 부엌 방')}

### 목축 방 ({counts['번들 · 목축 방']}퀘)

{bundle_summary('번들 · 목축 방')}

### 탐험 방 ({counts['번들 · 탐험 방']}퀘)

{bundle_summary('번들 · 탐험 방')}

### 공방 방 ({counts['번들 · 공방 방']}퀘)

{bundle_summary('번들 · 공방 방')}

## 도감 7종

항목 하나 = 퀘스트 하나. 대부분 보유·방문·처치. 전 항목 목록은 `docs/QUEST_LIST.md`.

| 도감 | 열리는 시점 | 인정 방식 | 현재 퀘스트 수(시작+항목+단계) |
| --- | --- | --- | --- |
"""
    for name, gate, how, n in col_meta:
        doc += f"| {name.replace('도감 · ', '')} | {gate} | {how} | {n} |\n"

    doc += f"""
도감마다 **30% / 60% / 90% / 100%** 단계 보상이 있다. 희귀 계열은 도감 한 곳에만 둔다. 네더라이트·네더의 별은 광물, 토템은 생물, 겉날개는 생물군계, 마법 황금 사과는 음식. 100%가 도감 인장 역할(P-02 선행).

## 완벽 (Perfection) ({counts['도감 · Perfection']}개)

{table(by['도감 · Perfection'])}

P-04~P-09는 순서가 없고 병행 가능. P-04·P-09는 C4-24 이후 열려 도감과 함께 진행한다.  
P-04는 마지막 사망 이후 8일 생존, P-09는 월드 생성 이후 누적 96일이다. P-09 목표를 먼저 채웠다면 퀘스트가 열릴 때 즉시 완료된다(`QUEST_IMPL_NOTES.md`).

## 보상 눈금

| 등급 | 쓰는 곳 | 예 |
| --- | --- | --- |
| 소 | 본선 개별 | 빵·횃불·석탄·씨앗·빈 병 |
| 중 | 장 마무리·꾸러미 완료 | 그 장에서 쓴 재료를 더 두툼하게, 카트리지·이동 소모품 |
| 대 | 도감 30~100% | 다이아 블록·네더라이트·토템·겉날개·네더의 별, 레벨 |
| 특 | P-10 | 금 다수, 네더의 별, 경험치 (칭호·서버 공지는 미구현) |

주지 않는 것: 자석/자동줍기 백팩, Pipez 고급·궁극 유도, 워프 거점 남발, 갈증·체온 영구 해제.

## 구현·동기화

| 파일 | 역할 |
| --- | --- |
| `tools/quest_v1/` | 퀘스트 정의 (이 표의 정본 소스) |
| `tools/generate_ftbquests.py` | FTB JSON5 + lang 생성 |
| `tools/export_quest_list.py` | `QUEST_LIST.md` / `.csv` |
| `tools/sync_progression_doc.py` | **이 진행표 문서** 재생성 |
| `docs/QUEST_IMPL_NOTES.md` | 소금·의약품·UI 감지 한계 등 메모 |

FTB 규칙 요약:

1. ID 접두어: T0 / C1~C5 / G / B1~B5 / D / P  
2. 선택 퀘스트(T0-08, T0-14, C3-22, 곁가지): 다음 본선 선행에 걸지 않음  
3. UI 전용 행동(HUD·JEI·도전과제 화면)은 체크마크 또는 우회 과제(지도 아이템 등)  
4. 첫 밤(T0-06)은 `minecraft:adventure/sleep_in_bed` (침대 색 무관)
"""

    OUT.write_text(doc, encoding="utf-8")
    print(f"Wrote {OUT} ({len(rows)} quests reflected)")


if __name__ == "__main__":
    main()
