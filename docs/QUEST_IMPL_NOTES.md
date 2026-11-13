# 퀘스트 구현 메모 (진행표 v1 · Phase 0)

> **역사 기록 (패치 6.6)**: Phase 0 구현 당시 메모. 현재 상태는 생성기 `tools/quest_v1/`와 스냅샷 `docs/QUEST_LIST.md`를 본다.

진행표: `docs/Realism Survival — 퀘스트 진행표 v1.md`  
생성: `python tools/generate_ftbquests.py`

## 사전 확인 결과

| # | 항목 | 결과 | 퀘스트 반영 |
|---|---|---|---|
| 0.1 | 소금 | 증류기에 **바닷물 양동이**(`STILL_SALT_SOURCE_BUCKETS`)를 넣으면 증류수와 함께 **소금** 부산물 | C1-14: 소금 4 보유. C5-15: 바닷물→소금 라인 체크마크 |
| 0.2 | 음식 책 | `solcarrot:food_book` (SoL Carrot). 제작·획득 아이템 | C2-11: 아이템 보유(소비 없음) |
| 0.3 | 넵튜늄 | `aquaculture:neptunium_ingot` 및 도구·갑옷. 워프스톤·귀환 두루마리·정수 필터·다이아 상자 등의 **상위호환 레시피** 재료(필수 아님) | C4-10의 어종 5종 중 2종 제출 → 보상 잉곳 1개. C4-11은 선택 과제. 워프스톤은 기본 레시피(자수정 4·진주 4·금괴 1)로 만들 수 있다 |
| 0.4 | 의약품 | 해독제=독/독소 · 화상연고=불/화상 · 온열팩=동상·한기(+슬로우) · 자극제=쇠약·슬로우 · 안약=실명·어둠·멀미·공중부양 · 야전 구급 키트=상태 일괄 · 진료소 키트=체력+상태 | C3-23~26·C1-20 설명에 반영 |
| 0.5 | 물·갈증 | 증류수 병 +40 · 민물/고인물 병 +20 · 바닷물 병 −14. 정수 필터 출력은 **민물 병** | C1-06~12 설명 |
| 0.6 | 한 해 | Serene Seasons: `sub_season_duration=8` × 12 = **96일**/년 (`run/config/sereneseasons/seasons.toml`) | P-09 N=96 (플레이테스트로 조정 가능) |
| 0.7 | Lootr | 팩에서 쓰던 ID: `lootr:1chest`, `lootr:10loot`. 번들 4단계는 1 / 10 / (25loot 또는 체크) / 트로피·100 계열을 쓰되, 없는 ID는 체크마크로 대체 | 탐험 번들 |
| 0.8 | 공급소 | 약 1/40 청크 월드젠. 못 찾으면 지도·탐험으로 공급소 상인 찾기 안내 | C3-19~20 |
| 0.9 | ID 표 | 과제 아이템은 아래 표 + `tools/quest_catalog/*.tsv` | 생성기 상수 |
| 0.10 | 스키마 | `hid` / item·check·biome·adv·kill / `consume_items` / `optional` / `min_required_dependencies` / 챕터 간 deps는 `extra_ids` | `tools/quest_v1/core.py` |

## 핵심 아이템 ID (생성기용)

| 표시 | registry id |
|---|---|
| 증류기 / 정수 필터 / 카트리지 | `realismsurvival:water_still` / `water_filter` / `filter_cartridge` |
| 물병 4종 | `distilled_water_bottle` · `fresh_water_bottle` · `stagnant_water_bottle` · `salt_water_bottle` |
| 양동이 | `*_water_bucket` · `snow_bucket` · `*_ice_bucket` |
| 온도계·소금 | `thermometer` · `salt` · `salt_block` |
| 의학 | `rsmedicine:bandage` · `trauma_kit` · `medkit` · `antidote` · `burn_salve` · `warm_pack` · `stimulant` · `eye_drops` · `field_kit` · `clinic_kit` |
| FD 주방 | `farmersdelight:cutting_board` · `flint_knife` · `stove` · `cooking_pot` · `skillet` |
| FD 작물 | `cabbage` · `tomato` · `onion` · `rice` |
| SoL | `solcarrot:food_book` |
| 낚시 | `aquaculture:iron_fishing_rod` · `worm` · `worm_farm` · `tackle_box` · `fish_fillet_cooked` · `tin_can` 계열 |
| 이동 | `waystones:andesite_waystone` · `warp_stone` · `return_scroll` · `warp_scroll` · `bound_scroll` |
| 나침반 | `naturescompass:naturescompass` |
| 상자·관·탱크 | `ironchest:*_chest` · `pipez:item_pipe` · `fluid_pipe` · `wrench` · `improved_upgrade` · `fluidtank:tank_copper` 등 |
| Oritech | `basic_generator` · `powered_furnace` · `pulverizer` · `energy_pipe` · `iron_dust` · `basic_battery` · `resource_node_resonator` · `bedrock_extractor` |
| 백팩 | `sophisticatedbackpacks:backpack` · `upgrade_base` |
| 펫 | `pet_home:pet_bed_red` |
| 유물 예 | `artifacts:umbrella` |

## 체크마크 대체 (1차) / 훅 후보 (이후)

| 퀘스트 | 1차 | 이후 |
|---|---|---|
| T0-06 첫 밤 | **발전과제** `minecraft:adventure/sleep_in_bed` (아무 색 침대) | — |
| T0-09 지도 | 빈 지도 보유 | FTB Chunks 웨이포인트 커스텀 훅 |
| T0-02·10·12 HUD/JEI/도전과제 UI | 체크마크 (UI 오픈은 FTB가 못 잡음) | `rs-quest-hooks` CustomTask |
| T0-07 청크 클레임 | 체크마크 | FTB Chunks 연동 훅 |
| C1-12 필터 통과 물 | 민물 병 3 보유 + 설명 | 필터 출력 감지 |
| C1-17·18 추위·더위 | 체크마크 | 바이옴·체온 트리거 |
| C3-12 펫 동행 | 체크마크 | 워프+펫 이벤트 |
| C3-18 주민 거래 | 발전과제 `minecraft:adventure/trade`로 자동 감지 | 완료. 생성 원본은 `tools/quest_v1/tutorial.py` |
| C5-12~17 시설 라인 | 체크마크 | 블록 배치 감지 |
| P-04 생존 8일 · P-06 걷기 20km · P-07 번식 20 · P-09 96일 | 통계 `time_since_death` / `walk_one_cm` / `animals_bred` / `play_time` | — |
| P-05 음식 50 · P-08 세 시설 | 체크마크. 음식 종류 수와 동시 가동은 통계로 못 잡음 | CustomTask |

### UI·HUD 행동 감지 (질문에 대한 답)

FTB Quests 기본 과제로는 **화면/HUD를 열었다**는 것을 거의 잡지 못한다.

| 할 수 있는 것 | 과제 유형 |
|---|---|
| 아이템 보유·제작·소비 | `item` |
| 자기 / 특정 발전과제 | `advancement` |
| 바이옴·구조물·처치·바닐라 통계 | `biome` / `structure` / `kill` / `stat` |
| 특정 블록·엔티티를 **바라보기** | `observation` |
| 모드가 알려 주는 커스텀 조건 | `custom` (+ `rs-quest-hooks` 같은 코드) |

SeasonHUD·AppleSkin·JEI·FTB Chunks 맵을 “열었다”는 신호는 각 모드가 이벤트를 안 주면 **체크마크 또는 우회 과제**(달력/온도계/지도 아이템 보유)로만 가능하다. 진짜 행동 감지가 필요하면 나중에 작은 훅 모드를 붙인다.

### 붕대(C1-21) 선행

진행표에는 `T0-05`만 있어 돌 도구만 끝나면 I장 붕대가 켜질 수 있었다.  
**정상 아님.** `C1-01`(I장 입장) 이후로 고쳤다.

## 진행 데이터

퀘스트 파일 ID는 `quest:<문자열 ID>`의 SHA1이다. 생성기를 다시 돌려도 같은 문자열 ID는 같은 16진 ID를 유지한다. 월드의 완료 기록은 그 16진 ID에 붙어 있으므로, 목록에서 빠진 ID만 사라진다.

작물 도감은 `potted_` 열세 칸(`D-crops-070`~`082`)만 제거했다. `D-crops-000`~`069`와 단계 퀘스트 ID는 그대로다. 그 화분 퀘스트를 이미 완료한 기록은 퀘스트가 없어져 남지 않는다.

`C1-19`, `C4-02`는 과제 종류가 바뀌어 해당 퀘스트의 과제 진행만 다시 센다. 퀘스트 ID 자체는 같다.

## 보상 정책 (1차)

- 칭호·기념 블록·서버 공지 → **미구현**. 인장/완벽은 골드 대신 소모품·재료·경험치로 대체.
- Pipez 본선은 `improved_upgrade`까지. 고급·궁극은 유도하지 않음.
- 백팩 자석·자동줍기 보상 없음.

## 진행 흐름 조정 (2026-10)

- `C4-09` 다이아몬드 보유 과제를 II장 초반으로 옮겼다. 퀘스트 ID는 유지해 광물 도감 입장 및 IV장 마무리의 선행 연결을 보존한다.
- IV장 마무리와 금 상자 과제의 다이아몬드 보상을 금 주괴로 바꿨다. 이 시점에 이미 다이아몬드를 가진 플레이어에게 중복 보상을 주지 않도록 했다.
- `C2-04`는 밀 수확 대신 `minecraft:husbandry/plant_seed` 발전과제로 첫 파종을 감지한다. 계절에 따라 밀을 기다리는 관문을 없애고, 작물·요리는 이후 과제에서 다룬다.
- `C2-20`은 블루길 한 마리 제출로 첫 낚시를 소개한다. II장 마무리는 생선 제출에 더는 묶이지 않는다.
- `C4-10`은 어종 5종 가운데 서로 다른 2종만 제출하면 된다. 넵튜늄(`C4-11`)은 이전에 워프스톤 필수 재료로 적혀 있었으나, Waystones 기본 워프스톤 레시피는 금괴를 쓰고 넵튜늄은 대체·상위호환 경로임을 jar 확인으로 바로잡았다. `C4-11`은 선택 과제로 바꾸고 `C4-15`의 선행에서 뺐다.
- `P-04` 생존 시험은 마지막 사망 후 24일에서 8일로 줄였다. 생존 시험이 끝부분 진행을 오래 막지 않도록 조정했다.
- `P-09`의 `play_time`은 월드 생성 이후 누적 통계다. Perfection이 열리기 전에 96일에 도달했다면 퀘스트가 열릴 때 즉시 완료될 수 있음을 설명에 명시한다.
- 기존 월드에서는 `C2-04`, `C2-20`, `C4-10`, `P-04`의 과제 조건이 바뀌었다. 퀘스트 ID와 완료 이력은 유지되지만, 해당 퀘스트가 아직 미완료라면 새 과제 조건으로 다시 진행해야 한다.
- Oritech 배터리는 플라스틱 판 가공이 선행되어야 하므로 III장의 전력 관·분쇄기 실사용과 분리해 IV장 이후 선택 목표로 뒀다. C3-32는 기계 배치 자체를 감지하지 못하는 한계를 보완하려 철 가루 산출물을 확인한다. 실제 전력 전달·시설 연결은 자동 감지되지 않아 C5-20은 설명과 체크마크로 안내한다.
- 자원 노드 공명기와 베드락 채굴기는 고급 금속·부품 및 전력망이 필요한 후반 선택 갈래다. 기본 자동화의 필수 관문으로 삼지 않는다.
