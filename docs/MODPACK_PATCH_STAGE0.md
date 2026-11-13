# 패치 0단계 — 지원 프로필·정본 지도·결정표

- 작성일: 2026-10-21
- 근거: `MODPACK_PATCH_PLAN.md` 0.1–0.3, 검토 00/01/04/10/11/13
- 상태: **문서 고정. 배포 manifest·식별자 이관·상점 계산대는 미구현.**

이 문서는 이후 패치의 기준선이다. 구현 전에 바뀌면 여기를 먼저 고친다.

---

## 0.1 지원 프로필 기준선

| 프로필 | 용도 | 확정 여부 | 조합 출처 |
|---|---|---|---|
| **개발 `runClient`** | 일상 개발·정적 검토·1단계 이후 코드 검증의 기본 | **확정 (임시 플레이 기준)** | `gradle.properties` + `build.gradle` `localRuntime`. 기본으로 packcontent / packqol / medicineAddon / shopstack / fluidstack / fishingmods / sereneseasons / farmersdelight / jei 등 대부분 켜짐 |
| **개발 `runServer`** | 전용 서버 기동·클라이언트 전용 모드 제외 확인 | 확정 (개발용) | `-PclientMods=false` 등 서버 적합 플래그. 실측은 8.1/14에서 |
| **GameTest** | 자동 회귀 | 확정 (좁은 조합) | 다수 콘텐츠 그룹 끔. **팩 전체 대표가 아님** |
| **정식 배포 팩** | 플레이어 설치본 | **미확정** | 후보: Modrinth pack 또는 packwiz. 실설치·manifest 재현 전까지 “배포 완료”로 쓰지 않음 |

### 개발 프로필 고정값 (2026-10-21)

| 항목 | 값 |
|---|---|
| Minecraft | 26.1.2 |
| NeoForge | 26.1.2.122 |
| Java toolchain | 25 (`build.gradle`) |
| RS | `realismsurvival` 0.1.0 |
| 의약품 애드온 | `rsmedicine` — 소스셋 `medicineAddon`, 기본 런타임 포함 |
| WDA | **제외** (26.1 미지원). 문서 후반 챕터는 계획만 |
| Create | **제외** (`-Pcreate` 시 실패하도록 가드) |
| BoP | 포함 (버전 ID `biomes_o_plenty_version`) |
| Oritech | `libs`/Modrinth ID로 포함 (exp 빌드) |
| 상점 스택 | Q Shop + SG Economy + Easy NPC (`shopstack`) |

`run/mods/`는 비어 있어도 정상이다. 모드는 Gradle `localRuntime`으로 로드된다.

### 배포 프로필 (미확정 체크리스트)

- [ ] 플랫폼 선택 (Modrinth / packwiz / 기타)
- [ ] 필수·선택·클라이언트 전용 모드 목록 + 파일 ID/해시
- [ ] 서버팩 vs 클라이언트팩 차이
- [ ] `pack/config`, datapack, `rs-korean` 기본 활성화 여부
- [ ] 업데이트·기존 월드 정책

---

## 0.2 설정·레시피·퀘스트 정본 지도

| 영역 | 편집 정본 | 런타임/산출 | 비고 |
|---|---|---|---|
| RS 코드·기본 데이터팩 | `src/main/java`, `src/main/resources/data/` | JAR `realismsurvival-*.jar` | 음료·작물·바이옴 태그 등 |
| 브리지 레시피 82개 | **정본: `src/main/resources/data/rsbridges/recipe/`** | 배포 복사: `pack/datapacks/rs-item-bridges/...` | 동일 내용 유지. 수정 후 복사·해시 검증 절차를 3.3/6.8에서 자동화 |
| FTB 퀘스트 | **생성기 `tools/quest_v1/`** → `pack/config/ftbquests/` | `run/config` 복사 또는 배포 시 `config/` | `QUEST_LIST.md/.csv`는 스냅샷. 정본 아님 |
| 퀘스트 한국어 | `pack/config/ftbquests/quests/lang/ko_kr/` | 게임 `ko_kr` | **한국어 정본** |
| 퀘스트 en_us | `.../lang/en_us/` | 정책은 0.3 | 현재 한국어 혼입 있음 → 6.1에서 정리 |
| 팩 설정 | `pack/config/**` | 배포 `config/`, 개발 `run/config` | |
| 한국어 오버레이 소스 | `pack/lang-ko/` | `scripts/build_korean_resourcepack.py` → `pack/resourcepacks/rs-korean` (+ `run/resourcepacks`) | |
| 상점 시즌 카탈로그 | `pack/config/qshop/` 및 RS 상점 코드/데이터 | 시즌 재생성 | |
| RS 서버 설정 | 코드 기본값 `RealismSurvivalConfig` | `<world>/serverconfig/realismsurvival-server.toml` | |
| 문서 스냅샷 | `docs/QUEST_LIST.*`, 진행표 등 | — | “생성 시점” 표기. 구현 완료로 보지 않음 |

### 브리지 레시피 동기화 규칙 (3.3에서 스크립트화)

1. JAR 경로를 정본으로 편집한다.
2. `pack/datapacks/rs-item-bridges`에 동일 파일명으로 복사한다.
3. 파일 수·내용 일치 검사(`python tools/sync_bridge_recipes.py --check`)를 배포 전 단계에 둔다. 현재 79개 (없는 Aquaculture 훅 재활용 3개 삭제).
4. Oritech 물 브리지는 원본 고체 재료·수량·시간·출력을 보존한다.

---

## 0.3 패치 결정표

채택안은 검토서·패치 계획의 기본안이다. **사용자 번복 전까지 구현 기준**으로 쓴다. 배포·공개 ID만 미확정으로 남긴다.

| ID | 안건 | 채택 | 후속 패치 |
|---|---|---|---|
| D-01 | `rs-medicine` 배포 단위 | **팩 내부 전용** (독립 Maven/Modrinth JAR 없음). 문서·빌드에 “팩 번들”로 명시 | 7.2 |
| D-02 | 공개 API 범위 | 0.x: facade 유지. 안정 DTO 정리 후 외부 노출. **지금 타입 대이동 금지** | 7.1 |
| D-03 | `com.example` / Maven group | **공개 배포 직전 한 번에** 이관. 패치 중도 변경 금지 | 6.7 |
| D-04 | 영어 퀘스트 | **한국어 우선 팩**. `ko_kr` 정본. `en_us`는 실제 영어 제공 또는 “미지원+fallback” 중 하나를 6.1에서 확정·정리 | 6.1 |
| D-05 | 상점 매입 범위 | **1차: 카탈로그/태그 기반 허용 품목** + 기후·계절·분야 배율. **2차: 계산대 GUI** (임의 전 아이템 아님). 크리에이티브·명령·고유 NBT는 제외 규칙 필수 | 5.1–5.4 |
| D-06 | 화폐 정본 | 당분간 SG↔Q Shop 브리지 유지. 단일 정본 선택은 5.5에서 동기화 검증 후 | 5.5 |
| D-07 | 월드 수원 직접 음용 | **긴급 경로 유지, 병 대비 저효율**. 등급 수원은 병 회복량의 일부만 적용. 품질 효과는 유지. 병·필터·증류의 효율/휴대 이점 보존 | **1.3** |
| D-08 | 첫 넵튜늄 | **필수 워프/C4용 첫 잉곳 보장 경로** (해양 퀘스트 보상 우선). 장비·Pipez·상자 등 장기 소비처 희귀성 유지 | 3.1, 4.1 |
| D-09 | 다이아 `C4-09` ID | **저장 안정성 위해 ID 유지**. 문서/생성기에 실제 II장 위치 표기 | 4.1 |
| D-10 | Serene Seasons | 모드 존재가 아니라 **레벨 계절 스냅샷 성공**으로 외부 규칙 사용 | 2.1 |
| D-11 | 열·냉 거리/차폐 (1.5) | **1.1–1.4 이후 후순위**. 측정·HUD 기대치 후 모델링 | 1.5 보류 |
| D-12 | 성능 SavedData/통화 폴링 | **증거 없이 저장 모델 변경 금지**. 측정 후 7.4–7.5 | 7.4–7.5 |
| D-13 | 배포 플랫폼 | **미확정** (후보 Modrinth pack / packwiz) | 6.8 |

---

## 진행 기록 (0단계)

```text
패치 ID: 0.1–0.3 / 기준·정본·결정표 / P0
수정 파일: docs/MODPACK_PATCH_STAGE0.md, docs/MODPACK_PATCH_PLAN.md, docs/MODPACK_REVIEW_LOG.md
기대되는 플레이어 동작: 없음 (문서만)
자동/정적 검증: 문서 링크·표 자체 일관성
필수 실플레이: 없음
세이브/API/배포 호환: 없음
결과: 구현(문서)
관련: 1.1–1.4 착수 가능
```

---

## 진행 기록 (1.1–1.4)

```text
패치 ID: 1.1–1.4 / 생존 수치·설정 정합 / P1
수정 파일:
  RealismSurvivalConfig.java — 초당 피해/침수 회복 주석·키, worldDrinkBottleFraction, 설정 경고
  SurvivalTickHandler.java — 침수·탈수 피해 × elapsed seconds
  SurvivalEffectsHandler.java — 동상/열사병 피해 × seconds, 임계값 정렬, 효과 지속 clamp
  NaturalRegenHandler.java — 비활성 갈증/체온은 게이트에서 제외
  DrinkWaterHandler.java — 등급 수원 = 병 회복 × fraction (기본 0.25)
  raw_water.json — 미등급 fallback 6 → 4
  RealismSurvival.java — ModConfigEvent 검증 훅
  lang en_us/ko_kr — 새 설정 키
기대되는 플레이어 동작:
  tickInterval만 바꿔도 초당 갈증 회복/피해율이 유지됨
  강물 맨손 음용은 병보다 약함(민물 ≈ +5), 품질 효과는 그대로
  thirst/temperature 끄면 예전 수치로 자연회복이 막히지 않음
자동/정적 검증: compileJava 통과
필수 실플레이: 간격 1/20/200, 수원 음용 vs 병, 토글 후 자연회복 (8.2)
세이브/API/배포 호환: 서버 config에 새 키 추가(기본값 호환). 기존 TOML 유지 가능
결과: 구현 / 실플레이 검증 대기
관련: 다음 2.1 Serene Seasons 스냅샷, 또는 3.1 넵튜늄 보장 설계 병렬
```


---

## 진행 기록 (2–8단계, 2026-10-21)

```text
패치 ID: 2.1–2.5 / 계절·농사·Mixin 정책 / P1
수정 파일:
  SereneSeasonsBridge.java, SeasonCalendar.java — 레벨 계절 조회 성공(SsSnapshot)일 때만 SS 규칙, 실패 시 경고 1회 + RS 달력
  CropGrowthHandler.java, RealismSurvivalConfig.java — 배율 곱 후 cropGrowthMultiplierMax(기본 1.75, 1.0–2.0)로 상한
  data/sereneseasons/tags/block/{spring,summer,autumn}_crops.json — 작물 적용표
  crop_light/sun_crops.json — #minecraft:crops가 로드 시점에 비어 있어 바닐라 작물 명시 목록으로 교체
  docs/MIXIN_POLICY.md (2.5), docs/MODPACK_ECONOMY_FLOW.md (2.4)
기대되는 플레이어 동작: SS 조회 실패 시에도 작물 규칙이 한 기준으로 동작. 최적 조건 성장 ≤ 1.75배. 빛 규칙이 실제 적용
자동/정적 검증: compileJava, GameTest 19/19, 로드 오류 0
필수 실플레이: REGRESSION_CHECKLIST 8.3
세이브/API/배포 호환: 새 config 키(기본값 호환)
결과: 구현 / 실플레이 검증 대기
```

```text
패치 ID: 3.1–3.4 / 자원 경제·필수 관문 / P1
수정 파일:
  tools/quest_v1/tutorial.py — C4-10n 보상 넵튜늄 잉곳 1 (첫 워프석 보장, D-08)
  AbstractFurnaceBlockEntityMixin.java — 바닷물 양동이 증발: 빈 양동이 반환(연료 칸→입력 칸), 채움량 비례 소금
  recipe/filter_cartridge_from_salt.json — 결과 2개
  tools/sync_bridge_recipes.py — 브리지 레시피 단일 정본 복사/검사 (--check)
  rsbridges recycle_{quartz,obsidian,glowstone}_hook 삭제 — Aquaculture 2.9.2에 없는 아이템 (82 → 79)
  docs/MODPACK_ECONOMY_FLOW.md (3.2, 3.4)
기대되는 플레이어 동작: 낚시 운 없이 첫 워프. 양동이 소실·부분 양동이 소금 복제 없음
자동/정적 검증: compileJava, sync --check OK 79/79
필수 실플레이: 8.4, 8.5
세이브/API/배포 호환: 없음 (퀘스트 ID 유지)
결과: 구현 / 실플레이 검증 대기
```

```text
패치 ID: 4.1–4.6 / 진행표·감지·한국어 / P1–P2
수정 파일:
  tools/quest_v1/{core,collections,perfection,tutorial}.py — josa() 받침 조사, 과제 설명=실제 감지, 보상 재정리, P-10 드래곤 알+XP 500
  tools/{generate_ftbquests,fix_ftbquests_v26,export_quest_list}.py — ko_kr 정본, fallback_locale=ko_kr, en_us 퀘스트 언어 제거
  advancement/special_fish_hook.json — 없는 훅 6종 제거 (C4-21d 완료 불가였음)
  advancement/any_artifact.json — mod_loaded 조건
  산출물: pack/config/ftbquests, docs/QUEST_LIST.*, 진행표 v1 (844개)
기대되는 플레이어 동작: 설명대로 완료, 한국어 조사 자연스러움, 완료 불가 과제 없음
자동/정적 검증: 재생성 diff, GameTest 로그 parse 오류 0
필수 실플레이: 8.5
세이브/API/배포 호환: 퀘스트 ID 유지(D-09). en_us 퀘스트 텍스트 제거 → 영어 클라이언트는 한국어 표시
결과: 구현 / 실플레이 검증 대기
```

```text
패치 ID: 5.1–5.6 / 상점 가격·매입 계산대·화폐 / P2
수정 파일:
  compat/shop/SupplyPricePolicy, SupplyCounterLedger, SupplyCounterQuote, SupplyCounterTrade (신규)
  world/inventory/SupplyCounterMenu, client/SupplyCounterScreen (신규), registry/ModMenus, RealismSurvivalClient
  SeasonalSupplyShop — buybackRates, 계산대 켜짐 시 Q Shop SELL 제외
  SgQShopCurrencyBridge — deposit, 동기화 카운터 / ShopMods — SOURCE_SELL_COUNTER
  SupplyShopAccess — 웅크리고 우클릭 = 계산대, /execute … qshop open supply_post* 차단(관리자 예외)
  tags/item/supply_unsellable.json, lang en_us/ko_kr, config supplyCounterEnabled
  docs/SHOP_SUPPLY_POST.md
기대되는 플레이어 동작: 매입 전 단가·총액·거절 사유 확인 후 확정, 서버 재견적, 실패 시 롤백
자동/정적 검증: compileJava
필수 실플레이: 8.6 (UI 배치, 다인 동시 거래, 재접속 잔액)
세이브/API/배포 호환: 플레이어 persistent data 키 realismsurvival_supply_counter 추가
결과: 구현 / 실플레이 검증 대기
```

```text
패치 ID: 6.1–6.8 / 한국어·문서·배포 / P2
수정 파일:
  build.gradle — writeRuntimeJarList, GameTest에서 Serene Seasons 제외
  tools/scan_missing_ko_kr.py — --jar-list, --fail-on-missing (누락 0)
  docs: MODPACK, MOD_CATALOG, SHOP_SUPPLY_POST, KOREAN_LOCALES, README, ftbquests README, 역사 문서 배너
  docs/MODPACK_RELEASE_PREP.md — 6.7 식별자·라이선스, 6.8 manifest 명세 (플랫폼 미정 D-13 → 문서만)
  biome 태그 7개 — BoP 항목 required:false (BoP 없는 환경에서 레지스트리 오류)
기대되는 플레이어 동작: README·T0-02 안내로 한국어 설정
자동/정적 검증: 로케일 스캔 0 누락
필수 실플레이: 8.1, 8.7
세이브/API/배포 호환: 6.7 이관·6.8 manifest는 공개 직전
결과: 6.1–6.6 구현, 6.7–6.8 문서(보류)
```

```text
패치 ID: 7.1–7.5 / API·애드온·성능 / P2
수정 파일:
  addons/rs-medicine neoforge.mods.toml — 팩 내부 전용 명시, neoforge [26.1.2.122,), realismsurvival [0.1.0,0.2.0)
  WaterWorldHandler.pendingChunks, FreshPoolDecayData.trackedCount, SgQShopCurrencyBridge 카운터, /realism stats
  docs/MODPACK_RELEASE_PREP.md (7.1 DTO 계획), docs/REGRESSION_CHECKLIST.md (7.3)
기대되는 플레이어 동작: 없음 (관리자 측정 명령)
자동/정적 검증: compileJava
필수 실플레이: 8.7 측정 → 임계 초과 시에만 7.4/7.5 설계 (D-12)
세이브/API/배포 호환: API 변경 없음 (D-02)
결과: 7.2 구현, 7.1·7.3 문서, 7.4–7.5 측정 도구만
```

```text
패치 ID: 8.1–8.7 / 통합 회귀
수정 파일: docs/REGRESSION_CHECKLIST.md
자동/정적 검증: compile, GameTest 19/19, 브리지 79/79, ko_kr 누락 0
필수 실플레이: 체크리스트 전체
결과: 자동 선행 확인 통과 / 수동 회귀 대기
```