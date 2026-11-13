# Realism Survival — 퀘스트 진행표 v1

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
| 튜토리얼 | 0·I~V | **162** | 0=15, I=25(본선 25+곁가지 0), II=34, III=32, IV=34, V=22 |
| 번들 | 5방 | **111** | 방마다 입장+꾸러미×4+인장 |
| 도감 | 7종 | **561** | 시작+항목+30/60/90/100% |
| 완벽 | P-01~P-10 | **10** | 도감 탭 안 |

총합 약 **857**개 (`docs/QUEST_LIST.md`와 동일).

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

## 0장 시작하기 (15개)

화면·땅·지도를 익히고 갈증 문제를 던져 I장으로 넘긴다.

| ID | 퀘스트 | 과제 | 선행 | 배우는 것 | 보상 |
| --- | --- | --- | --- | --- | --- |
| T0-01 | 퀘스트북 펼치기 | 체크마크 | 없음 | 장 구성, 선행 조건, 보상 받는 법을 익히세요 | minecraft:bread ×4 |
| T0-02 | 화면 살펴보기 | 체크마크 | T0-01 | 배고픔(AppleSkin), 갈증·체온 아이콘, 계절 HUD(SeasonHUD)의 위치를 확인하세요 | — |
| T0-03 | 첫 나무 | 발전과제: realismsurvival:any_log | T0-02 | 스폰 지점의 나무면 종류는 상관없습니다 | minecraft:stick ×8 |
| T0-04 | 작업대 | 보유: minecraft:crafting_table ×1 | T0-03 | 기본 제작에 쓰는 작업대입니다 | minecraft:charcoal ×4 |
| T0-05 | 돌 도구 | 보유: minecraft:stone_pickaxe ×1 · 보유: minecraft:stone_axe ×1 | T0-04 | 돌 곡괭이와 돌 도끼를 만드세요 | minecraft:torch ×8 |
| T0-06 | 첫 밤 | 발전과제: minecraft:adventure/sleep_in_bed | T0-05 | 색깔에 관계없이 침대에서 한 번 자면 완료됩니다 | minecraft:bread ×4 |
| T0-07 | 청크 점유하기 | 체크마크 | T0-04 | FTB Chunks 맵을 열고 청크 하나를 점유해 보세요 | — |
| T0-08 | 파티 (선택) | 체크마크 | T0-07 | FTB Teams 파티나 팀 화면을 열어 보세요 | — |
| T0-09 | 지도와 웨이포인트 | 체크마크 | T0-06 | FTB Chunks 미니맵이나 월드맵에 집·물·광산처럼 다시 올 곳을 웨이포인트로 하나 표시해 보세요 | — |
| T0-10 | 레시피 찾기 | 체크마크 | T0-04 | JEI에서 아이템의 제작법과 사용처를 찾아보고, Jade 오버레이로 블록 정보를 확인하세요 | — |
| T0-11 | 짐 정리 | 보유: minecraft:chest ×1 | T0-04 | 상자를 만들고 Mouse Tweaks와 TrashSlot으로 보관함과 인벤토리를 정리해 보세요 | — |
| T0-12 | 도전과제 화면 | 체크마크 | T0-06 | Better Advancements로 도전과제 화면을 열어 보세요 | — |
| T0-13 | 물 마셔 보기 | 체크마크 | T0-06 | 갈증이 생깁니다 | minecraft:glass_bottle ×3 |
| T0-14 | 말하기 (선택) | 체크마크 | T0-02 | Simple Voice Chat으로 말해 보세요 | — |
| T0-15 | 다음 여정을 향해 | 체크마크 | T0-05, T0-06, T0-09, T0-13 | 0장을 마칩니다 | minecraft:bread ×16, minecraft:torch ×32, minecraft:charcoal ×16 |

## I 생존 (본선 25 + 곁가지 0)

증류·정수·소금·체온·회복을 한 번씩 손에 쥐게 한다.

| ID | 퀘스트 | 과제 | 선행 | 배우는 것 | 보상 |
| --- | --- | --- | --- | --- | --- |
| C1-01 | 갈증의 시작 | 체크마크 | 없음 | 갈증 수치가 시간에 따라 줄어듭니다 | — |
| C1-02 | 물에도 등급이 있다 | 체크마크 | C1-01 | JEI에서 증류수·민물·오염된 물·바닷물 병과 양동이를 확인해 보세요 | — |
| C1-03 | 오염된 물 한 양동이 | 보유: realismsurvival:stagnant_water_bucket ×1 | C1-02 | 월드에 고여 있는 물은 대부분 오염된 물입니다 | minecraft:iron_ingot ×3 |
| C1-04 | 바닷물 | 보유: realismsurvival:salt_water_bucket ×1 | C1-03 | 바닷물은 마시는 물이 아니라 소금의 재료입니다 | — |
| C1-05 | 민물 한 병 | 보유: realismsurvival:fresh_water_bottle ×1 | C1-03 | 민물은 마실 수 있으며, 갈증을 20 회복합니다 | minecraft:glass_bottle ×3 |
| C1-06 | 민물 마시기 | 제출: realismsurvival:fresh_water_bottle ×1 | C1-05 | 민물 병 한 개를 제출하면 완료됩니다 | minecraft:glass_bottle ×4 |
| C1-07 | 불씨 | 보유: minecraft:furnace ×1 | C1-02 | 증류기를 돌릴 화로를 준비하세요 | minecraft:coal ×8 |
| C1-08 | 증류기 | 보유: realismsurvival:water_still ×1 | C1-07, C1-03 | 증류기로 물을 끓여 증류수로 만드세요 | — |
| C1-09 | 첫 증류수 | 제출: realismsurvival:distilled_water_bottle ×3 | C1-08 | 증류수 병 세 개를 제출하세요 | minecraft:glass_bottle ×6 |
| C1-10 | 정수 필터 | 보유: realismsurvival:water_filter ×1 | C1-05 | 정수 필터로 물을 거르면 민물 병을 얻습니다 | — |
| C1-11 | 필터 카트리지 | 보유: realismsurvival:filter_cartridge ×2 | C1-10 | 필터 카트리지는 사용할수록 닳습니다 | realismsurvival:filter_cartridge ×1 |
| C1-12 | 필터로 정수하기 | 제출: realismsurvival:fresh_water_bottle ×3 | C1-11 | 정수 필터로 만든 민물 병 세 개를 제출하세요 | minecraft:glass_bottle ×6 |
| C1-13 | 물 비축 | 보유: realismsurvival:distilled_water_bottle ×10 | C1-09, C1-12 | 탐험을 떠나기 전에 마실 물을 챙기세요 | minecraft:glass_bottle ×8 |
| C1-14 | 바닷물에서 소금 얻기 | 제출: realismsurvival:salt ×4 | C1-04, C1-08 | 소금 네 개를 제출하세요 | minecraft:bucket ×2 |
| C1-15 | 소금 보관하기 | 보유: realismsurvival:salt_block ×1 | C1-14 | 소금 블록은 소금을 보관하거나 건축 재료로 쓸 수 있습니다 | realismsurvival:salt ×2 |
| C1-16 | 온도계 | 보유: realismsurvival:thermometer ×1 | C1-07 | 온도계로 주변 기온과 자신의 체온을 확인하세요 | — |
| C1-17 | 추위 | 생물군계: minecraft:snowy_plains | C1-16 | 눈 덮인 평원에 들어가면 완료됩니다 | — |
| C1-18 | 더위 | 생물군계: minecraft:desert | C1-16 | 사막에 들어가면 완료됩니다 | — |
| C1-19a | 민물 얼음 | 제출: realismsurvival:fresh_ice_bucket ×1 | C1-18 | 민물 얼음 양동이를 제출하세요 | minecraft:bucket ×1 |
| C1-19b | 눈 양동이 | 제출: realismsurvival:snow_bucket ×1 | C1-18 | 눈 양동이를 제출하세요 | minecraft:bucket ×1 |
| C1-19 | 얼음과 눈 | 체크마크 | C1-19a, C1-19b — 이 중 1개만 완료하면 됨 | 민물 얼음 양동이 또는 눈 양동이 중 하나를 제출하면 완료됩니다 | — |
| C1-20 | 온열 팩 | 보유: rsmedicine:warm_pack ×1 | C1-17 | 온열 팩은 몸을 데우고 한기와 동상을 완화하는 휴대용 아이템입니다 | rsmedicine:warm_pack ×1 |
| C1-21 | 붕대 | 제출: rsmedicine:bandage ×1 | C1-01 | 붕대 한 개를 제출하세요 | rsmedicine:bandage ×2 |
| C1-22 | 외상 키트 | 보유: rsmedicine:trauma_kit ×1 | C1-21 | 외상 키트는 붕대보다 더 많은 체력을 회복합니다 | — |
| C1-23 | 생존 장 마무리 | 보유: realismsurvival:distilled_water_bottle ×5 · 보유: rsmedicine:bandage ×3 · 보유: rsmedicine:warm_pack ×1 | C1-13, C1-20, C1-22 | I장을 마칩니다 | realismsurvival:filter_cartridge ×6, minecraft:glass_bottle ×16, minecraft:bread ×16, 기타 보상 |


## II 밭과 부엌 (본선 34 + 곁가지 0)

계절 → 밭 → 부엌 → 골고루 먹기. 목축·낚시·백팩을 곁에 붙인다.  
C2-05는 작물 네 갈래(C2-05a~d) 중 **2개만** 끝내면 통과한다.

| ID | 퀘스트 | 과제 | 선행 | 배우는 것 | 보상 |
| --- | --- | --- | --- | --- | --- |
| C2-01 | 계절 살펴보기 | 체크마크 | 없음 | SeasonHUD(또는 화면의 계절 표시)에서 지금 계절을 확인하세요 | minecraft:wheat_seeds ×8 |
| C2-02 | 작물과 계절 | 체크마크 | C2-01 | 작물마다 잘 자라는 계절이 다릅니다 | — |
| C2-03 | 첫 밭 | 보유: minecraft:wooden_hoe ×1 | C2-01 | 땅을 갈고 물 옆에 경작지를 만드세요 | minecraft:bone_meal ×8 |
| C2-04 | 첫 씨앗 심기 | 발전과제: minecraft:husbandry/plant_seed | C2-03 | 계절을 기다리지 않아도 자랄 수 있는 작물을 골라 씨앗을 심으세요 | minecraft:wheat_seeds ×8 |
| C4-09 | 첫 다이아몬드 | 보유: minecraft:diamond ×1 | C2-04 | 다이아몬드 한 개를 보유하면 완료됩니다 | waystones:return_scroll ×2 |
| C2-05a | 새 작물: 양배추 | 제출: farmersdelight:cabbage ×1 | C2-04 | 이 분기에서 지정한 작물을 제출하세요 | farmersdelight:cabbage_seeds ×4 |
| C2-05b | 새 작물: 토마토 | 제출: farmersdelight:tomato ×1 | C2-04 | 이 분기에서 지정한 작물을 제출하세요 | farmersdelight:tomato_seeds ×4 |
| C2-05c | 새 작물: 양파 | 제출: farmersdelight:onion ×1 | C2-04 | 이 분기에서 지정한 작물을 제출하세요 | farmersdelight:onion ×2 |
| C2-05d | 새 작물: 쌀 | 제출: farmersdelight:rice ×1 | C2-04 | 이 분기에서 지정한 작물을 제출하세요 | farmersdelight:rice ×2 |
| C2-05 | 새 작물 | 체크마크 | C2-05a, C2-05b, C2-05c, C2-05d — 이 중 2개만 완료하면 됨 | 양배추·토마토·양파·쌀 가운데 서로 다른 두 종류를 제출하면 완료됩니다 | farmersdelight:cabbage_seeds ×4, farmersdelight:tomato_seeds ×4 |
| C2-06 | 도마와 칼 | 보유: farmersdelight:cutting_board ×1 · 보유: farmersdelight:flint_knife ×1 | C2-04 | 도마와 부싯돌 칼을 준비하세요 | — |
| C2-07 | 스토브 | 보유: farmersdelight:stove ×1 | C2-06 | 스토브는 냄비 요리에 필요한 열원입니다 | minecraft:coal ×8 |
| C2-08 | 냄비 | 보유: farmersdelight:cooking_pot ×1 | C2-07 | 냄비를 스토브 위에 올려 수프·스튜를 끓입니다 | — |
| C2-09 | 프라이팬 (선택) | 보유: farmersdelight:skillet ×1 | C2-07 | 냄비와 함께 쓰는 선택 조리 도구입니다 | — |
| C2-10 | 첫 수프 | 제출: farmersdelight:vegetable_soup ×1 | C2-08, C2-05 | 도마로 손질해 냄비에서 끓인 채소 수프를 제출합니다 | minecraft:bread ×8, minecraft:bowl ×2 |
| C2-11 | 음식 책 | 보유: solcarrot:food_book ×1 | C2-10 | 먹은 음식은 Spice of Life 음식 책에 기록됩니다 | — |
| C2-12 | 골고루 먹기 | 체크마크 | C2-11 | 음식 책에 서로 다른 음식 10종을 기록한 뒤 체크하세요 | minecraft:bowl ×8 |
| C2-13 | 함께 나누는 잔치 | 보유: farmersdelight:roast_chicken_block ×1 | C2-12 | 여럿이 나눠 먹는 잔치 블록 1종을 준비하세요 | minecraft:cooked_chicken ×4 |
| C2-14 | 바구니와 로프 | 보유: farmersdelight:basket ×1 | C2-03 | 바구니와 로프는 농장에서 수확물과 재료를 다루기 편하게 해 줍니다 | farmersdelight:rope ×4 |
| C2-15 | 울타리 | 발전과제: realismsurvival:any_wooden_fence | C2-03 | 나무 종류와 관계없이 나무 울타리 16개를 준비하세요 | minecraft:wheat ×16 |
| C2-16 | 우유와 달걀 (선택) | 보유: minecraft:milk_bucket ×1 · 보유: minecraft:egg ×1 | C2-15 | 우유와 달걀은 요리에 쓸 수 있습니다 | — |
| C2-17 | 가죽과 양모 (선택) | 보유: minecraft:leather ×4 · 발전과제: realismsurvival:any_wool | C2-15 | 가죽 네 개와 아무 색 양모 네 개를 준비하세요 | — |
| C2-18 | 낚싯대 | 보유: minecraft:fishing_rod ×1 | C2-03 | 막대와 실로 만드는 바닐라 낚싯대면 충분합니다 | — |
| C2-19 | 지렁이 | 보유: aquaculture:worm ×8 | C2-18 | 지렁이는 낚시 미끼입니다 | aquaculture:worm_farm ×1 |
| C2-20 | 첫 물고기 | 제출: aquaculture:bluegill ×1 | C2-19 | 블루길 한 마리를 제출하세요 | minecraft:bread ×8 |
| C2-21 | 태클 박스 | 보유: aquaculture:tackle_box ×1 | C2-20 | 낚시 도구와 미끼를 보관하는 태클 박스입니다 | — |
| C2-22 | 생선 손질 | 제출: aquaculture:fish_fillet_raw ×1 | C2-20 | 손질한 날생선 필렛을 제출하세요 | minecraft:bowl ×4 |
| C2-23 | 익힌 필렛 | 제출: aquaculture:fish_fillet_cooked ×1 | C2-22 | 익힌 생선 필렛을 제출하세요 | minecraft:iron_nugget ×8 |
| C2-24 | 열대의 맛 | 보유: fishofthieves:mango ×1 · 보유: fishofthieves:pineapple ×1 | C2-10 | 망고와 파인애플을 하나씩 제출하세요 | — |
| C2-25 | 백팩 | 보유: sophisticatedbackpacks:backpack ×1 | C2-14 | 백팩으로 소지품 공간을 늘리세요 | — |
| C2-26 | 수납 공간 늘리기 | 보유: sophisticatedbackpacks:upgrade_base ×1 | C2-25 | 업그레이드 베이스를 사용해 백팩 확장 방식을 익혀 보세요 | minecraft:leather ×4 |
| C2-28 | 첫 전력원 (선택) | 보유: oritech:basic_generator ×1 | C2-04 | Oritech의 기본 발전기는 석탄 같은 일반 화로 연료로 전력을 만듭니다 | — |
| C2-29 | 전동 가공대 (선택) | 보유: oritech:powered_furnace ×1 | C2-28 | 전력으로 작동하는 화로를 준비하세요 | — |
| C2-27 | 부엌 장 마무리 | 체크마크 | C2-13, C2-26 | II장을 마칩니다 | farmersdelight:cabbage_seeds ×16, farmersdelight:tomato_seeds ×16, minecraft:coal ×32, minecraft:bowl ×16, 기타 보상 |


## III 밖으로 (32개)

생물군계·거점·펫·구조물·마을·공급소·의료. 워프스톤은 IV로 미루고 예고만.

| ID | 퀘스트 | 과제 | 선행 | 배우는 것 | 보상 |
| --- | --- | --- | --- | --- | --- |
| C3-01 | 자연의 나침반 | 보유: naturescompass:naturescompass ×1 | 없음 | 원하는 생물군계의 방향과 거리를 알려 주는 나침반입니다 | — |
| C3-02 | 새로운 생물군계 탐험 | 생물군계: biomesoplenty:lavender_field · 생물군계: biomesoplenty:bayou · 생물군계: biomesoplenty:jade_cliffs | C3-01 | Biomes O' Plenty에서 서로 다른 생물군계 세 곳을 방문하세요 | minecraft:torch ×16 |
| C3-03 | 채집 | 보유: biomesoplenty:rose_quartz_chunk ×1 · 보유: biomesoplenty:reed ×1 · 보유: biomesoplenty:cattail ×1 | C3-02 | 이 채집물로는 종이와 워프스톤 재료를 만들 수 있습니다 | — |
| C3-04 | 웨이포인트 표시 | 체크마크 | C3-02 | FTB Chunks 미니맵이나 월드맵에 웨이포인트 세 곳을 표시하세요 | — |
| C3-05 | 웨이스톤으로 이동하기 | 보유: waystones:andesite_waystone ×1 | C3-04 | 웨이스톤을 사용하면 활성화한 거점 사이를 빠르게 오갈 수 있습니다 | — |
| C3-06 | 첫 거점 | 체크마크 | C3-05 | 웨이스톤을 설치하고 활성화하세요 | — |
| C3-07 | 귀환 두루마리 | 보유: waystones:return_scroll ×1 | C3-06 | 한 번 사용하면 귀환 지점으로 돌아갈 수 있습니다 | — |
| C3-08 | 두 번째 거점 | 체크마크 | C3-07 | 첫 거점과 다른 위치에 두 번째 웨이스톤을 설치하고 활성화하세요 | waystones:return_scroll ×2, 기타 보상 |
| C3-09 | 워프스톤 예고 | 체크마크 | C3-08 | 워프스톤은 자수정 4·엔더 진주 4·금괴 1로 만듭니다 | — |
| C3-10 | 길들이기 | 발전과제: minecraft:husbandry/tame_an_animal | C2-15 | 늑대 또는 고양이를 길들이면 완료됩니다 | minecraft:bone ×8 |
| C3-11 | 펫 침대 | 보유: pet_home:pet_bed_red ×1 | C3-10 | Pet Home을 사용하면 펫을 맡아 두고 필요할 때 다시 부를 수 있습니다 | — |
| C3-12 | 함께 이동 | 체크마크 | C3-11, C3-06 | 웨이스톤으로 이동해 펫도 함께 따라오는지 확인하세요(Waystones Teleport Pets) | — |
| C3-13 | 첫 구조물 탐험 | 체크마크 | C3-04 | Towns and Towers 구조물 한 곳을 방문한 뒤 체크하세요 | — |
| C3-14 | 탑 | 체크마크 | C3-13 | 탑 한 곳을 방문한 뒤 체크하세요 | — |
| C3-15 | 전초와 폐허 | 체크마크 | C3-13 | 전초기지와 폐허를 각각 한 곳씩 방문한 뒤 체크하세요 | — |
| C3-16 | 전리품 상자 | 발전과제: lootr:1chest | C3-13 | Lootr 상자를 하나 여세요 | minecraft:gold_ingot ×3 |
| C3-17 | Lootr 상자 열 개 열기 | 발전과제: lootr:10loot | C3-16 | 구조물을 탐험하며 Lootr 상자를 열 개 여세요 | minecraft:gold_ingot ×5 |
| C3-18 | 주민 거래 | 발전과제: minecraft:adventure/trade | C3-13 | 주민과 한 번 거래하면 자동으로 완료됩니다 | — |
| C3-19 | 공급소 (선택) | 체크마크 | C3-18 | 분야와 관계없이 공급소 가판대 한 곳을 찾아보세요 | — |
| C3-20 | 상인 (선택) | 체크마크 | C3-19 | 가판대의 상인을 우클릭하면 상자 상점이 열립니다 | — |
| C3-21 | 골드 (선택) | 체크마크 | C3-20 | 인벤토리의 골드가 상점에서 사용하는 화폐입니다 | realismsurvival:filter_cartridge ×1, 기타 보상 |
| C3-22 | 첫 판매 (선택) | 체크마크 | C3-21 | 상자에서 매입을 고르고, 뼈·실·썩은 살점 중 하나를 가운데 칸에 올려 보세요 | minecraft:torch ×8, 기타 보상 |
| C3-29 | 계절별 상품 확인 (선택) | 체크마크 | C3-22, C2-02 | SeasonHUD나 달력으로 현재 계절을 확인한 뒤, 앞서 방문한 것과 같은 분야의 상점을 다시 여세요 | minecraft:bone_meal ×4 |
| C3-30 | 전동 분쇄기 (선택) | 보유: oritech:pulverizer ×1 | C2-29, C3-03 | 전동 분쇄기로 금속 원료를 가공하는 공정을 살펴보세요 | — |
| C3-31 | 전력 관 연결 (선택) | 보유: oritech:energy_pipe ×6 | C3-30 | Oritech 전력 관 여섯 개를 준비하세요 | — |
| C3-32 | 첫 광석 가공 (선택) | 보유: oritech:iron_dust ×1 | C3-31 | 원철 한 개를 Oritech 분쇄기에 넣고 철 가루를 얻으세요 | — |
| C3-23 | 해독제 | 보유: rsmedicine:antidote ×1 | C3-13 | 독과 독소 계열 상태이상을 치료합니다 | — |
| C3-24 | 화상 연고 | 보유: rsmedicine:burn_salve ×1 | C3-23 | 화상과 몸에 붙은 불을 치료합니다 | — |
| C3-25 | 자극제 | 보유: rsmedicine:stimulant ×1 | C3-23 | 쇠약·한기·충격을 풀어 줍니다 | — |
| C3-26 | 안약 | 보유: rsmedicine:eye_drops ×1 | C3-23 | 실명·어둠·멀미·공중 부양 계열을 치료합니다 | — |
| C3-27 | 야전 구급 키트 | 보유: rsmedicine:field_kit ×1 | C3-24, C3-25, C3-26 | 상태이상을 한꺼번에 치료합니다 | rsmedicine:bandage ×4 |
| C3-28 | 탐험 장 마무리 | 체크마크 | C3-08, C3-12, C3-17, C3-27 | III장을 마칩니다 | waystones:return_scroll ×8, rsmedicine:bandage ×8, minecraft:torch ×32, 기타 보상 |

## IV 깊은 곳 (34개)

유물·광물·넵튜늄·워프스톤·극한 원정·상위 의료.

| ID | 퀘스트 | 과제 | 선행 | 배우는 것 | 보상 |
| --- | --- | --- | --- | --- | --- |
| C4-01 | 장신구 슬롯 | 체크마크 | 없음 | Curios 장신구 슬롯(허리띠·목걸이·반지 등)을 열어 보세요 | — |
| C4-02 | 첫 유물 | 발전과제: realismsurvival:any_artifact | C4-01 | 유물 한 종류를 보유하면 완료됩니다 | — |
| C4-03 | 장착 | 체크마크 | C4-02 | Curios 장신구 슬롯에 유물을 장착한 뒤 체크하세요 | — |
| C4-04 | 우산 | 보유: artifacts:umbrella ×1 | C4-03 | 우산을 직접 구해 쓰임을 익히세요 | — |
| C4-05 | 유물 다섯 | 체크마크 | C4-03 | 서로 다른 유물 5종을 모은 뒤 체크하세요 | minecraft:gold_ingot ×5 |
| C4-06 | 깊은 구조물 | 체크마크 | C4-01 | 앞서 방문한 곳과 다른 구조물 세 곳을 추가로 탐험하세요 | minecraft:torch ×16 |
| C4-07 | 광맥 | 제출: minecraft:raw_copper ×1 · 제출: minecraft:raw_iron ×1 · 제출: minecraft:raw_gold ×1 | C4-01 | 구리 원석·철 원석·금 원석을 각각 한 개씩 제출하세요 | minecraft:coal ×16 |
| C4-08 | 철 도구 | 보유: minecraft:iron_pickaxe ×1 | C4-07 | 철 곡괭이를 제작하세요 | — |
| C4-10-a | 바다 낚시 · 대서양 대구 | 제출: aquaculture:atlantic_cod ×1 | C2-21, C4-01 | 대서양 대구 한 마리를 제출하세요 | — |
| C4-10-b | 바다 낚시 · 대서양 넙치 | 제출: aquaculture:atlantic_halibut ×1 | C2-21, C4-01 | 대서양 넙치 한 마리를 제출하세요 | — |
| C4-10-c | 바다 낚시 · 붉바리 | 제출: aquaculture:red_grouper ×1 | C2-21, C4-01 | 붉바리 한 마리를 제출하세요 | — |
| C4-10-d | 바다 낚시 · 참치 | 제출: aquaculture:tuna ×1 | C2-21, C4-01 | 참치 한 마리를 제출하세요 | — |
| C4-10-e | 바다 낚시 · 카피텐 | 제출: aquaculture:capitaine ×1 | C2-21, C4-01 | 카피텐 한 마리를 제출하세요 | — |
| C4-10 | 바다 낚시 | 체크마크 | C4-10-a, C4-10-b, C4-10-c, C4-10-d, C4-10-e — 이 중 2개만 완료하면 됨 | 대서양 어종 다섯 가지 가운데 서로 다른 두 종류를 제출하면 열립니다 | minecraft:cooked_cod ×8, aquaculture:neptunium_ingot ×1 |
| C4-11 | 넵튜늄 (선택) | 보유: aquaculture:neptunium_ingot ×1 | C4-10 | 넵튜늄 잉곳 한 개를 보유하면 완료됩니다 | — |
| C4-12 | 넵튜늄 도구 (선택) | 보유: aquaculture:neptunium_pickaxe ×1 | C4-11 | 넵튜늄 도구 한 개를 보유하면 완료됩니다 | — |
| C4-13 | 넵튜늄 갑옷 (선택) | 보유: aquaculture:neptunium_helmet ×1 | C4-11 | 넵튜늄 갑옷 한 부위를 보유하면 완료됩니다 | — |
| C4-15 | 워프스톤 | 보유: waystones:warp_stone ×1 | C3-09, C3-03 | 워프스톤은 충전해 사용하는 이동 수단입니다 | — |
| C4-16 | 워프 두루마리 | 보유: waystones:warp_scroll ×1 | C4-15 | 워프 두루마리를 사용하면 등록한 거점 사이를 이동합니다 | — |
| C4-17 | 귀속 두루마리 | 보유: waystones:bound_scroll ×1 | C4-15 | 귀속 두루마리로 현재 위치를 거점으로 지정하세요 | — |
| C4-18 | 설원 탐험 | 생물군계: minecraft:snowy_taiga | C1-20, C4-01 | 눈 덮인 타이가에 들어가면 완료됩니다 | rsmedicine:warm_pack ×2 |
| C4-25 | 한대 기후 상점 (선택) | 체크마크 | C4-18, C3-20 | 눈 덮인 지역에서 공급소 상점을 찾아 여세요 | minecraft:charcoal ×4, 기타 보상 |
| C4-19 | 악지 탐험 | 생물군계: minecraft:badlands | C4-01 | 악지에 들어가면 완료됩니다 | realismsurvival:distilled_water_bottle ×6 |
| C4-26 | 건조 기후 상점 (선택) | 체크마크 | C4-19, C3-20 | 건조 지역의 공급소 상점을 찾아 여세요 | minecraft:cactus ×4, 기타 보상 |
| C4-20 | 위험한 밤 | 처치: minecraft:zombie ×20 | C3-27 | 좀비 스무 마리를 처치하세요 | rsmedicine:bandage ×6 |
| C4-21a | 낚시 심화 · 조류 | 제출: aquaculture:algae ×1 | C4-10 | 조류를 제출하세요 | minecraft:bone_meal ×4 |
| C4-21b | 낚시 심화 · 거머리 | 제출: aquaculture:leech ×1 | C4-10 | 거머리를 제출하세요 | minecraft:string ×4 |
| C4-21c | 낚시 심화 · 유목 | 제출: aquaculture:driftwood ×1 | C4-10 | 유목을 제출하세요 | minecraft:stick ×8 |
| C4-21d | 낚시 심화 · 특수 훅 | 발전과제: realismsurvival:special_fish_hook | C4-10 | 철 훅을 제외한 특수 훅을 한 번 얻으면 자동으로 완료됩니다 | — |
| C4-21 | 낚시 심화 | 체크마크 | C4-21a, C4-21b, C4-21c, C4-21d — 이 중 2개만 완료하면 됨 | 조류·거머리·유목·특수 훅 가운데 서로 다른 두 종류를 제출하면 열립니다 | — |
| C4-22 | 메디킷 | 보유: rsmedicine:medkit ×1 | C3-27 | 체력을 크게 회복합니다 | — |
| C4-23 | 진료소 키트 | 보유: rsmedicine:clinic_kit ×1 | C4-22 | 체력과 상태를 함께 치료합니다 | — |
| C4-24 | 심층 탐험 마무리 | 체크마크 | C4-05, C4-09, C4-16, C4-18, C4-23 | IV장을 마칩니다 | waystones:return_scroll ×8, minecraft:gold_ingot ×8, 레벨 10, 기타 보상 |
| C4-27 | 전력 저장 장치 (선택) | 보유: oritech:basic_battery ×1 | C4-24 | 기본 배터리는 강철·일렉트럼과 플라스틱 판이 필요합니다 | — |

## V 자동화 (22개)

상자·관·탱크·물 라인. Pipez는 **개선 업그레이드까지**만 본선. 시설 라인은 체크마크.

| ID | 퀘스트 | 과제 | 선행 | 배우는 것 | 보상 |
| --- | --- | --- | --- | --- | --- |
| C5-01 | 구리 상자 업그레이드 | 보유: ironchest:copper_chest ×1 | 없음 | Iron Chests 모드의 기본 상자입니다 | — |
| C5-02 | 상자 업그레이드 | 보유: ironchest:wood_to_copper_chest_upgrade ×1 | C5-01 | 상자 업그레이드로 보관 용량을 단계별로 늘려 보세요 | — |
| C5-03 | 철 상자 | 보유: ironchest:iron_chest ×1 | C5-02 | 철 상자는 구리 상자보다 더 많은 아이템을 보관합니다 | — |
| C5-04 | 금 상자 | 보유: ironchest:gold_chest ×1 | C5-03 | 금 상자를 보유하세요 | minecraft:gold_ingot ×4 |
| C5-05 | 렌치 | 보유: pipez:wrench ×1 | C5-01 | 렌치로 파이프의 연결 방향을 조정하세요 | minecraft:copper_ingot ×8 |
| C5-06 | 아이템 파이프 | 보유: pipez:item_pipe ×8 | C5-05 | 아이템 파이프 여덟 개를 보유하세요 | pipez:item_pipe ×8 |
| C5-07 | 유체 파이프 | 보유: pipez:fluid_pipe ×8 | C5-06 | 유체 파이프로 물을 탱크나 기계에 옮기세요 | — |
| C5-08 | 속도·필터 업그레이드 (선택) | 보유: pipez:improved_upgrade ×1 | C5-06 | 관을 더 빠르게 쓰는 선택 장비입니다 | — |
| C5-09 | 구리 탱크 | 보유: fluidtank:tank_copper ×1 | C5-07 | 물을 많이 담아 두는 구리 탱크입니다 | — |
| C5-10 | 철 탱크 | 보유: fluidtank:tank_iron ×1 | C5-09 | 철 탱크는 구리 탱크보다 더 많은 물을 저장합니다 | — |
| C5-11 | 저장조 | 보유: fluidtank:reservoir_iron ×1 | C5-10 | 저장조는 중·후반에 대량의 물을 보관하는 시설입니다 | — |
| C5-12 | 증류 라인 | 보유: realismsurvival:distilled_water_bottle ×16 | C5-07, C5-09 | 증류수 병 16개를 보유하면 완료됩니다 | — |
| C5-13 | 정수 라인 | 보유: realismsurvival:fresh_water_bottle ×16 | C5-12 | 민물 병 16개를 보유하면 완료됩니다 | realismsurvival:filter_cartridge ×4 |
| C5-14 | 급수 시설 | 보유: realismsurvival:distilled_water_bucket ×4 | C5-11, C5-13 | 증류수 양동이 4개를 보유하면 완료됩니다 | — |
| C5-15 | 소금 공정 | 보유: realismsurvival:salt ×32 | C5-12, C1-14 | 소금 32개를 보유하면 완료됩니다 | realismsurvival:salt_block ×4 |
| C5-16 | 부엌 연결 | 체크마크 | C5-06, C2-27 | 상자와 파이프를 연결해 부엌에 필요한 재료를 공급하세요 | — |
| C5-17 | 수확물 모으기 | 체크마크 | C5-06, C2-05 | 수확물이 상자에 자동으로 모이도록 연결하세요 | — |
| C5-18 | 백팩 확장 | 보유: sophisticatedbackpacks:iron_backpack ×1 | C2-26 | 철 백팩을 보유하면 완료됩니다 | — |
| C5-19 | 자동화 장 마무리 | 체크마크 | C5-14, C5-16, C5-17, C5-18 | V장·튜토리얼을 마칩니다 | minecraft:gold_block ×2, realismsurvival:filter_cartridge ×8, 레벨 15 |
| C5-20 | 전력 공방 연결 (선택) | 체크마크 | C3-31, C3-32, C5-14, C5-16, C5-17 | 초중반에 준비한 Oritech 발전기·전력 관·분쇄기를 급수·부엌·수확물 보관 시설과 연결해 보세요 | — |
| C5-21 | 심부 자원 탐지기 (선택) | 보유: oritech:resource_node_resonator ×1 | C4-24 | 자원 노드 공명기는 전력을 충전해 주변에 로드된 청크의 심부 자원 노드를 찾습니다 | — |
| C5-22 | 베드락 채굴기 (선택) | 보유: oritech:bedrock_extractor ×1 | C5-21 | 베드락 채굴기는 자원 노드에서 원료를 지속적으로 채취하는 고급 설비입니다 | — |

## 번들: 5개 방

방마다 **입장 퀘스트 → 꾸러미 4개(품목+완료) → 인장**.  
소모품·재료는 제출(소비), 도구·장비·블록은 보유. 상세 품목은 `docs/QUEST_LIST.md` 번들 절.

### 생존 방 (22퀘)

**열리는 시점:** `C1-23` 완료 후

**꾸러미:**
- 물의 네 얼굴
- 차가움과 따뜻함
- 거름과 소금
- 응급 상자

**인장 보상:** realismsurvival:filter_cartridge ×4, minecraft:gold_ingot ×2, 경험치 40

### 부엌 방 (23퀘)

**열리는 시점:** `C2-27` 완료 후

**꾸러미:**
- 텃밭의 네 가지
- 손에 쥔 도구
- 한 상 차림
- 열대의 과일

**인장 보상:** farmersdelight:cabbage_seeds ×8, farmersdelight:tomato_seeds ×8, farmersdelight:onion ×8, minecraft:gold_ingot ×2, 경험치 40

### 목축 방 (22퀘)

**열리는 시점:** `C2-27` 완료 후

**꾸러미:**
- 외양간의 산물
- 벌통
- 한 식구
- 달리는 친구

**인장 보상:** minecraft:saddle ×1, minecraft:name_tag ×1, minecraft:gold_ingot ×2, 경험치 40

### 탐험 방 (22퀘)

**열리는 시점:** `C3-28` 완료 후

**꾸러미:**
- 생물군계의 선물
- 구조물 도장
- 열어 본 상자
- 길잡이의 도구

**인장 보상:** waystones:return_scroll ×4, minecraft:gold_ingot ×2, 경험치 40

### 공방 방 (22퀘)

**열리는 시점:** `C5-14` 완료 후

**꾸러미:**
- 큰 상자
- 관의 도구
- 물 저장
- 넵튜늄 가공

**인장 보상:** fluidtank:tank_gold ×1, minecraft:gold_ingot ×4, 경험치 60

## 도감 7종

항목 하나 = 퀘스트 하나. 대부분 보유·방문·처치. 전 항목 목록은 `docs/QUEST_LIST.md`.

| 도감 | 열리는 시점 | 인정 방식 | 현재 퀘스트 수(시작+항목+단계) |
| --- | --- | --- | --- |
| 작물 | C2-05 | 보유 | 75 |
| 음식·음료 | C2-12 | 보유 | 135 |
| 물고기 | C2-20 | 보유 | 57 |
| 생물군계 | C3-02 | 생물군계 방문 | 137 |
| 생물 | C3-28 | 처치 / 제출 | 65 |
| 광물 | C4-09 | 보유 | 39 |
| 유물 | C4-05 | 보유 | 53 |

도감마다 **30% / 60% / 90% / 100%** 단계 보상이 있다. 희귀 계열은 도감 한 곳에만 둔다. 네더라이트·네더의 별은 광물, 토템은 생물, 겉날개는 생물군계, 마법 황금 사과는 음식. 100%가 도감 인장 역할(P-02 선행).

## 완벽 (Perfection) (10개)

| ID | 퀘스트 | 과제 | 선행 | 배우는 것 | 보상 |
| --- | --- | --- | --- | --- | --- |
| P-01 | 첫 번째 문 | 체크마크 | B1-seal, B2-seal, B3-seal, B4-seal, B5-seal | 번들 인장 다섯 개를 모두 모은 뒤 체크하세요 | — |
| P-02 | 두 번째 문 | 체크마크 | D-crops-m100, D-food-m100, D-fish-m100, D-bio-m100, D-crea-m100, D-ore-m100, D-art-m100 | 도감 100% 인장 일곱 개를 모두 모은 뒤 체크하세요 | — |
| P-03 | 완벽의 장 | 체크마크 | P-01, P-02 | 다섯 인장과 일곱 도감을 모았습니다 | 경험치 50 |
| P-04 | 생존의 시험 | 통계 minecraft:time_since_death ≥192000 | C4-24 | 마지막으로 죽은 뒤 게임 시간으로 8일을 버티세요 | minecraft:gold_ingot ×8 |
| P-05 | 식탁의 시험 | 체크마크 | P-03 | 음식 책에 서로 다른 음식 50종이 기록되었는지 확인한 뒤 직접 체크하세요 | minecraft:gold_ingot ×8 |
| P-06 | 길의 시험 | 통계 minecraft:walk_one_cm ≥2000000 | P-03 | 도보로 20km를 걸으세요 | minecraft:gold_ingot ×8 |
| P-07 | 가축의 시험 | 통계 minecraft:animals_bred ≥20 | P-03 | 동물을 20번 번식시키세요 | minecraft:gold_ingot ×8 |
| P-08 | 공방의 시험 | 체크마크 | P-03 | 아래 세 설비 조건을 모두 갖춘 뒤 직접 체크하세요 | minecraft:gold_ingot ×8 |
| P-09 | 한 해 | 통계 minecraft:play_time ≥2304000 | C4-24 | 게임 시간으로 96일을 보내세요 | minecraft:gold_ingot ×8 |
| P-10 | 완벽 | 체크마크 | P-04, P-05, P-06, P-07, P-08, P-09 | 완벽한 생존자가 되었습니다 | minecraft:dragon_egg ×1, 경험치 500 |

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
