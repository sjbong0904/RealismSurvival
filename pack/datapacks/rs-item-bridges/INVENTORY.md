# rs-item-bridges — 막다른 아이템 전수조사 & 브리지

정책: **팩 데이터팩 레시피**(방식 1). 철/다이아 통짜 태그 대체는 하지 않는다.  
런타임: 동일 JSON이 RS jar `data/rsbridges/recipe/`에도 들어가 `neoforge:mod_loaded`로 켜진다.  
이 폴더는 팩 배포용 복사본(독립 데이터팩으로 빼기 쉬움).

## 조사 요약 (모드 jar 기준)

| 모드 | 자체 쓰임새 | 크로스모드 공백 (브리지 대상) |
|---|---|---|
| Aquaculture | 넵튜늄 풀세트·생선 필렛·통조림 등 **내부** 레시피 충실 | 넵튜늄이 Waystones/Pipez/상자/나침반/RS와 무관. algae·leech·worm·driftwood·특수 훅 잉여 |
| Fish of Thieves | 코코넛 목재·염료·뼈가루 일부. 생선은 공용 `c:foods/raw_fish` 태그로 FD 생선 스튜에 사용 가능 | 과일 일부와 포터리 샤드 과잉 |
| Biomes O' Plenty | 목재·장미석영 블록·일부 염료 | reed/cattail/glowshroom/barnacles/spanish_moss/toadstool이 장식·염료에 그침. 바이옴 기후·물 등급은 RS 태그에서 보강 |
| Farmer's Delight | 자체 루프 탄탄. 공용 raw-fish 태그로 양 낚시 모드의 생선을 스튜 재료로 수용 | FoT 전용 스튜 레시피는 중복 연결이므로 추가하지 않음 |
| Waystones / Pipez / Iron Chests / Nature's Compass | 바닐라·네더라이트 재료 | 팩 말기 희귀금속(넵튜늄)·BoP 장미석영 경로 없음 |

제외(브리지 안 함): 양동이·스폰알·로드 캐스트 모델·장식용 사플링 전부·이미 Aqua 내부로 닫힌 생선 태그 요리.

## 브리지 레시피 목록 (`rsbridges:`)

### Aquaculture
| 레시피 | 입력 → 출력 |
|---|---|
넵튜늄 레시피는 **기존(금·다이아·철 기반) 레시피의 상위호환**이다. 기존 레시피는 지우지 않는다(`C1-10` 정수 필터, `C3-01` 나침반, `C3-05` 웨이스톤, `C3-07` 귀환 두루마리가 `C4-10` 넵튜늄 획득보다 앞서기 때문).

| 레시피 | 입력 → 출력 (기존 대비) |
|---|---|
| `neptunium_warp_stone` | 자수정 4·진주 4·넵튜늄 잉곳 1 → 워프스톤 **×2** (기존: 금괴 1 → ×1) |
| `neptunium_return_scrolls` | 넵튜늄 조각 2·먹물·종이 3 → 귀환 두루마리 **×6** (기존 ×3) |
| `neptunium_pipez_advanced_upgrade` | 넵튜늄 잉곳 **2**·레드스톤 블록 4·향상 업그레이드 → `pipez:advanced_upgrade` (기존: 다이아 4) |
| `neptunium_diamond_chest` | 금 상자 + 유리 7 + 넵튜늄 잉곳 **1** → 다이아 상자 (기존: 유리 6 + 다이아 2) |
| `neptunium_natures_compass` | 묘목 4 + 넵튜늄 조각 4 → Nature's Compass (**바닐라 나침반 불필요**) |
| `neptunium_water_filter` | 넵튜늄 조각 4 + 자갈·숯·돌 → RS 정수 필터 **×2** (기존 ×1, 철 4) |
| `algae_to_green_dye` / `algae_organic_compost` | 조류 → 염료 / FD 퇴비 |
| `leech_to_spider_eye` / `worm_to_bone_meal` | 거머리·지렁이 변환 |
| `driftwood_campfire` | 유목 → 모닥불 |
| `aqua_salted_fish_fillet` | 날생선 필렛 + 소금 → 익힌 필렛 (연료 없이, `salted_cod`와 같은 방식) |
| `vanilla_rod_to_iron_rod` | 바닐라 낚싯대 + 철괴 1 → Aquaculture 철 낚싯대 (`crafting_transmute`: 마법 부여·내구도 유지) |
| `recycle_*_hook` | 흑요석/석영/발광석 훅 제련 회수 |

### Fish of Thieves
| 레시피 | 입력 → 출력 |
|---|---|
| `fot_fruit_salad` / `fot_mango_juice` | 공용 과일·베리 태그와 망고 → FD 과일샐러드 / 멜론주스 |
| `fot_pineapple_to_sugar` | 반쪽 파인애플 ×2 → 설탕 |
| `fot_*_dye` | 석류·바나나 → 염료 |
| `fot_salted_*` (10종) | FoT 생선 + 소금 → 해당 익힌 생선 (연료 없이) |
| `fot_fish_bone_bonemeal` | FoT 생선뼈 → 뼛가루 ×3 |
| `fot_*_sherd_to_brick` | 과잉 포터리 샤드 제련 → 벽돌 |

### Farmer's Delight
| 레시피 | 입력 → 출력 |
|---|---|
| `fd_flint_to_iron_knife` | 부싯돌 칼 + 철괴 1 → 철 칼 (`crafting_transmute`) |

### Biomes O' Plenty
| 레시피 | 입력 → 출력 |
|---|---|
| `bop_reed_to_paper` / `bop_cattail_to_paper` | 갈대·부들 → 종이 |
| `bop_glowshroom_to_glowstone` | 발광버섯 → 발광석 가루 |
| `bop_rose_quartz_to_amethyst` | 장미석영 조각 → 자수정 조각 |
| `bop_rose_quartz_warp_stone` | 장미석영 → 워프스톤 |
| `bop_barnacles_prismarine` | 따개비 → 프리즈머린 조각 |
| `bop_toadstool_brown_mushroom` / `bop_spanish_moss_string` | 변환 |

### Oritech 및 등급 물

Oritech에서 `minecraft:water`를 요구하는 유체 레시피 25종(기본 13종, 외부 모드 호환 12종)에 신선수·증류수 등급 물을 받는 대체 레시피 50개를 추가했다. Oritech과 Realism Survival 조건을 기본으로 두고, 외부 호환 레시피에는 원본의 FTBMaterials·EnderIO 조건도 보존한다. 입력량·재료·출력·작업 시간은 원본과 같아서 바닐라 물이 없는 RS 월드에서도 기존 처리 경로가 이어진다.

부산물이나 제작법을 RS 전용 아이템으로 바꾸지는 않았다. 각 모드의 재료 체계를 유지하면서 유체 입력 경계에서 연결한다.

## 후속 후보 (아직 안 함)
- BOP 업데이트로 추가되는 바이옴의 기후·수역 태그 분류 재검토
- LFT `crafting_grade_up` 커스텀 타입에 넵튜늄 티어
- Guard Villagers / Pet Home 드롭 (아이템 풀이 작음)
- Sophisticated Backpacks 업그레이드에 넵튜늄 (자석 등은 팩에서 끄는 전제라 보류)
