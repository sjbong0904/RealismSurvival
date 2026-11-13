# FTB Quests (Realism Survival)

권위 있는 퀘스트 데이터는 생성기(`tools/generate_ftbquests.py` + `tools/quest_v1/`)와 그 출력인 이 폴더의 `quests/` 입니다. RS jar에 넣지 않습니다.
언어는 `lang/ko_kr`만 생성하며 `fallback_locale = ko_kr`입니다 (en_us 폴더는 생성 시 삭제).

**진행표(생성 스냅샷):** `docs/Realism Survival — 퀘스트 진행표 v1.md` (`python tools/sync_progression_doc.py`로 현재 생성기 기준 재생성; 손편집 금지)  
**전체 표(생성 스냅샷):** `docs/QUEST_LIST.md` / `.csv` (`python tools/export_quest_list.py`)  
**구현 메모:** `docs/QUEST_IMPL_NOTES.md`  
**생성기:** `tools/generate_ftbquests.py` + `tools/quest_v1/`

## 생성·동기화

```bash
python tools/generate_ftbquests.py
```

`pack/config/ftbquests/quests`에 **JSON5**를 다시 쓰고, `run/config/ftbquests/quests`로 복사합니다.

FTB Quests **26.1** 규칙:
- 챕터/퀘스트 파일은 `*.json5` (`.snbt` 무시)
- **제목·설명**은 `lang/<locale>/` 번역 테이블 (`chapter_group.json5`, `chapter.json5`, `chapters/<file>.json5`)
- **아이콘**은 문자열 ID가 아니라 ItemStack 객체 `{ "id": "minecraft:book", "count": 1 }`
- 로드 위치는 월드가 아니라 **인스턴스 `config/ftbquests/quests`**

생성 후 자동으로 `tools/fix_ftbquests_v26.py`가 아이콘·lang을 맞춥니다.
**`fix_ftbquests_v26.py`만 단독 실행하지 마세요.** (가능하면 `generate_ftbquests.py`만 사용. fix는 이제 lang을 보존하지만, 제목 복구의 정본은 generate입니다.)

**ID 규칙:** FTB는 퀘스트/챕터 ID를 Java `Long.parseLong(hex, 16)`으로 읽습니다. 최상위 비트가 켜진 ID(`8`–`F`로 시작)는 `0`으로 깨져 「이름 없음」이 됩니다. 생성기는 항상 부호 있는 long 범위 ID만 씁니다.

**왕관/도감 완성:** 튜토리얼·번들 왕관은 해당 챕터의 **필수(비선택) 퀘스트 전부**, 도감 완성은 **시작 체크+모든 항목**에 의존합니다.

## 구조 (진행표 v1)

| 그룹 | 역할 |
|------|------|
| Tutorial | 0 시작하기 → I 생존 → II 밭과 부엌 → III 밖으로 → IV 깊은 곳 → V 자동화 |
| Guides | 팁 8개 (보상 없음, 본선 퀘스트 완료 시 열림) |
| Bundles | 생존·부엌·목축·탐험·공방 방 + 인장 5 |
| Collection | 도감 7종(30/60/90/100%) + Perfection(P-01~P-10) |

ID 접두어: T0 / C1~C5 / G / B1~B5 / D / P. 손편집보다 생성기·진행표를 고친 뒤 재생성하는 것을 권장합니다.
