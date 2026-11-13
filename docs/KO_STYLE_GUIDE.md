# 한글화 스타일 가이드 (Realism Survival)

모드팩 전체(RS 본체, 퀘스트, 서드파티 모드 보완 번역)의 한국어를 같은 목소리로 유지하기 위한 기준입니다.

## 번역 소스와 우선순위

| 계층 | 위치 | 비고 |
|------|------|------|
| 모드 공식 번역 | 각 모드 jar의 `ko_kr.json` | 원칙적으로 수정하지 않음. 용어집 충돌이 심하면 overlay로 덮음 (예: Waystones `전송석`→`웨이스톤`, Guard Villagers `보초병`→`경비병`, FTB Chunks `Saturation`→`채도`) |
| **보완 번역 (정본)** | `pack/lang-ko/<namespace>.json` | 공식 번역에 없는 키를 채움. `scripts/build_korean_resourcepack.py`가 `pack/resourcepacks/rs-korean`으로 합침 |
| RS jar 내 복사본 | `src/main/resources/assets/<namespace>/lang/ko_kr.json` | 정본의 미러. 직접 고치지 말고 정본을 고친 뒤 동기화 |
| RS 본체 | `src/main/resources/assets/realismsurvival/lang/ko_kr.json` | 본체 문구 |
| 퀘스트 | `pack/config/ftbquests/quests/lang/ko_kr/**` | 생성기 `tools/quest_v1/*.py`가 원본 |

## 문체

- 기본은 **합쇼체**(`~합니다`, `~하세요`). 반말(`~해`, `~다`)과 해요체는 섞지 않습니다.
- 메뉴·버튼·설정 이름은 **명사형**(`전체 보기`, `자동 저장`). 문장 끝 마침표 없음.
- 설명·툴팁·메시지는 완결된 문장이며 마침표로 끝냅니다. 원문에 마침표가 없으면 짧은 라벨/툴팁은 생략해도 됩니다.
- 명령형 안내는 `~하세요`. 번역투 `~하는 것을 허용합니다`, `~에 의해 ~됩니다`, `~할 수 있습니다` 남발, `당신의`, `~하기 위해서` 반복은 피하고 자연스러운 주어·능동 표현으로 바꿉니다.
- 조사는 `은(는)`, `이(가)`, `을(를)` 대신 가능하면 **받침과 무관한 문장 구조**로 피합니다. 불가피한 `%s`/`{0}` 뒤에는 `은(는)` 계열을 허용합니다.
- 줄임표는 `…` 또는 `...`로 앞말과 붙여 씁니다 (`검색...`). 구두점 앞 공백 금지.
- 플레이스홀더(`%s`, `%1$s`, `{0}`, `§`, `\n`, `<...>`)는 원문과 **개수·순서 의미**를 맞춥니다.
- 영문 고유명사(FTB Chunks, JEI, Curios, Serene Seasons 등 모드 이름)는 그대로 둡니다. 아이템·블록·몹 이름은 한글화합니다.

## 용어집 (반드시 통일)

| 영어 | 한국어 | 쓰지 않음 |
|------|--------|-----------|
| claim (chunk) | 점유 | 청구, 클레임, 사유화 |
| claim reward (퀘스트 보상 받기) | 수령 / 받기 | 청구 |
| force load | 강제 로드 | 강제 적재, 포스로드 |
| waypoint | 웨이포인트 | 경유지, 길잡이 |
| biome | 생물군계 | 바이옴, 군계 |
| quest | 퀘스트 | 임무 |
| potion | 물약 | 포션 |
| thirst | 갈증 | 목마름 |
| body temperature | 체온 | 신체 온도 |
| Waystone | 웨이스톤 | 전송석, 워프석 |
| Contaminated / stagnant water | 오염된 물 | 고인물(게임 은어 '고인물'과 혼동), 고인 물 |
| Fresh / Distilled / Salt water | 민물 / 증류수 / 바닷물 | |
| pet | 펫 | 애완동물 |
| guard (Guard Villagers) | 경비병 | 가드, 경비원 |
| tier | 단계 | Tier, 티어 |
| config / settings | 설정 | 구성, 환경 설정 |
| cooldown | 재사용 대기시간 | 쿨타임 |
| bookmark | 북마크 | |
| ingredient (JEI) | 재료 | 성분 |
| party / team | 파티 / 팀 | |
| stack | 스택 | |
| hotbar | 핫바 | |
| void (backpack upgrade) | 즉시 삭제 / 버리기 | 보이딩 |
| gold (화폐) | 골드 | |
| border (테두리) | 테두리 | 국경 |
| source block (물) | 물 블록 / 수원 | 원천 |

바닐라 마인크래프트 한국어 용어(예: 약탈자, 소환사, 엔드 시티)가 있으면 그쪽을 따릅니다.

## 검수 기준 (사용자가 읽는 느낌)

1. 영어를 먼저 떠올려야 이해되는 문장이면 다시 쓴다.
2. 같은 개념에 두 가지 이름이 있으면 하나로 통일한다.
3. 오역(의미 반전, 설정 키 이름을 UI 문구로 오해)부터 고친다.
4. 기계 번역투(사물 주어 수동태, `~하는 것을`, `~에 대한`)를 줄인다.
5. 라벨은 짧게, 설명은 한 번에 읽히게. 과도한 직역 대신 의미를 전달한다.
6. 게임 은어가 이상하게 겹치지 않게 한다 (예: `고인물`).

## 도구

- `python tools/audit_ko_quality.py [--detail] [--only <ns>]` — 용어집/번역투/플레이스홀더 기계 점검
- `python scripts/build_korean_resourcepack.py` — rs-korean 재생성
