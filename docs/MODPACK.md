# Realism Survival — 모드팩 현황과 설계 기록 (NeoForge 26.1.2)

지금까지 이야기해서 정해 둔 것들을 한곳에 모아 둔 문서다.  
별도로 안 적혀 있으면, Realism Survival(이하 RS) 본체 모드 안에는 **다른 모드를 강제하지 않고**, 있으면 맞춰 주고 없으면 그냥 돌아가는 식으로만 연동한다.

편의 모드들을 한꺼번에 끄려면: `-Ppackqol=false`

> **현재 상태 (2026-10-21)**: 이 저장소는 `runClient`/`runServer` 개발 프로필과 팩 설정·데이터 소스를 포함한다. 정식 배포용 모드팩 manifest/조립물은 아직 고정되지 않았다. WDA는 현재 프로필에 없고 26.1용 의존성으로 보류 중이다. BoP Overworld 바이옴의 기후 분류는 현재 태그에 반영되어 있다. 퀘스트 현재 수와 진행은 `MODPACK_REVIEW_06_PROGRESSION_DENSITY.md`, 순서 평가는 `MODPACK_REVIEW_07_QUEST_FLOW.md`, 실제 구현/보류 판정은 `MODPACK_REVIEW_LOG.md`를 기준으로 한다. 아래의 “예정”, WDA 포함 챕터, 목표 개수 표 등은 별도 표시가 없는 한 **과거 설계 제안**이며 현재 구현 목록으로 읽지 않는다.
>
> **현행 요약 (패치 2–8 반영)**: 퀘스트 844개 = 본편 162(0~V장) · 번들 111 · 도감 561 · Perfection 10 (선택 24) — 정본은 생성기 `tools/quest_v1`, 스냅샷은 `docs/QUEST_LIST.md`. 퀘스트 언어는 `ko_kr`만 제공하고 `fallback_locale = ko_kr` (D-04). P-10 보상은 드래곤 알 + XP 500. 공급소는 구매 = Q Shop, 매입 = RS 매입 계산대(웅크리고 우클릭, `docs/SHOP_SUPPLY_POST.md`). 음식·물·소금·보상 흐름은 `docs/MODPACK_ECONOMY_FLOW.md`, Mixin 정책은 `docs/MIXIN_POLICY.md`, 회귀 확인은 `docs/REGRESSION_CHECKLIST.md`. Oritech는 `-Ppackcontent` 프로필에 포함(베타 빌드), BoP 기후 태그 반영 완료, WDA 보류.

---

## 이 팩이 뭔지

**현실적인 생존 규칙** 위에, **판타지 탐험**을 얹은 RPG 모드팩이다.

역할을 나누면 대략 이렇게다.

| 누가 | 뭘 담당하나 |
|---|---|
| **Realism Survival** | 갈증, 체온, 물 품질(증류·민물·고인물·바닷물), 먹고 마시는 쪽 규칙 |
| **FTB Quests** | “다음에 뭘 하면 되는지” — 챕터별로 퀘스트를 주고 보상 |
| **Towns and Towers** | 탐험할 탑·마을 같은 건물들. When Dungeons Arise는 현재 프로필에 포함하지 않음 |
| **지도·상자·편의 모드** | 플레이를 편하게 — 엔딩 자체는 아님 |

**엔딩 방식:**  
생존 / 음식 / 탐험 / 길들이기 / 산업 다섯 분야를 각각 “이 분야는 여기까지 했다” 수준까지 끝낸 뒤에야 **최종장(Mastery)** 이 열린다.  
최종장에서는 우리가 미리 적어 둔 목록을 거의 다 채워야 한다 — 음식 한 번씩 먹기, 몹 한 번씩 잡기, 구조물·바이옴 다니기 같은 것.  
그걸 다 모으면 팩이 끝난다. **보스 한 마리 잡아서 엔딩**은 쓰지 않는다.  
보스는 중간에 “하고 싶으면 해라” 정도만. **Grimoire of Gaia는 넣지 않는다.**

### 진행을 받쳐 주는 모드

| 역할 | 모드 | 26.1 |
|---|---|---|
| 퀘스트책 | **FTB Quests** + Library (파티용이면 Teams도) | ✅ `26.1.2.9` 근처 |
| 도전과제 화면을 예쁘게 | Advancement Plaques, Better Advancements | ✅ 있으면 좋음 |
| 스킬 트리류 (Pufferfish 등) | — | **1차 출시에서는 안 씀** (퀘스트랑 겹침) |

퀘스트 내용 파일은 **팩 쪽 데이터**(FTB 퀘스트 파일)에 둔다. RS jar 안에 박지 않는다. 나중에 “이 행동이 퀘스트에 안 잡혀서” RS에 아주 작은 감지용 코드가 필요할 수는 있다.

---

## 모드 목록과, 대략 60시간 분량

**예산(목표치):** 하루 2시간씩 하면 한 달 정도 — 본편(튜토리얼 + 번들) 대략 **60시간**. 측정값이 아니다.  
아래 “몇 시간 구간”은 **설계할 때 짐작한 분량**이지, “이 시간이 안 되면 막힌다”는 뜻이 아니다. 실제 시간은 8.5 새 월드 진행에서 본편과 도감 수집을 따로 기록한다.

### A. 이미 개발/실행에 붙어 있는 것

| 구분 | 모드 | 하는 일 |
|---|---|---|
| 본체 | Realism Survival | 물·체온·증류/필터 |
| 음식 | Farmer's Delight, Serene Seasons, Aquaculture, Fish of Thieves | 요리, 계절, 낚시 음식 |
| 전력·가공 | Oritech (+ Athena, GeckoLib) | 초반 발전기·분쇄기부터 전력망 사용, 중후반 배터리·자원 노드, 후반 공방 통합까지 단계적으로 확장 |
| 물 운반 | Large Fluid Tank, Pipez | 저장과 관 연결은 유지하고, Oritech 전력 기계와 조합해 공방·급수 시설로 확장 |
| 보기·편의 | JEI, AppleSkin, Curios, Sodium, Iris | 레시피, 배고픔 표시, 악세서리 칸, 그래픽 |
| 편의 묶음 (`-Ppackqol`) | Jade, **SeasonHUD**, Mouse Tweaks, TrashSlot, Iron Chests, 보이스챗, AmbientSounds, 최적화 모드들 | 정보 표시, 계절 HUD, 인벤·상자 등 |
| (보류) | Jade Addons | NeoForge 26.1에서 `@OnlyIn` 부팅 경고 — upstream 수정 전 제외 |
| 팩 콘텐츠 (`-Ppackcontent`) | FTB Quests(+Library/Teams/**Chunks**/XMod*), Better Advancements, T&T, Lootr, SoL Carrot, BoP+TerraBlender, Waystones(+Shogi, **Teleport Pets**), Sophisticated Backpacks(+Core), Nature's Compass, Artifacts, Guard Villagers, Pet Home, Treeliable, **Oritech(+Athena, GeckoLib)** | 퀘스트·지도·클레임·탐험·바이옴·짐·전력 공방. GameTest에서는 끔 |
| 상점 (`-Pshopstack`) | **Q Shop**, **SG Economy**(인벤 골드 UI), Celesthyd(`libs/`), **Easy NPC** Core+Config UI | 월드젠 공급소+상인, 계절별 품목 순환, 통화 브릿지. `docs/SHOP_SUPPLY_POST.md` |

**HUD 역할 분담:** 미니맵은 우상단(FTB Chunks). 바닐라 상태 효과 아이콘은 **좌상단**. 계절·날짜·시계·기온은 미니맵 아래 생물군계 줄 밑(달력/시계/온도계 게이트). 지도를 끄면 SeasonHUD가 우상단에 계절을 다시 그리고 RS 시계·기온이 그 아래에 붙는다 (`enable_minimap_integration = true`, `enable_show_minimap_hidden = true`).

**한글 로케일:** FTB Chunks와 Oritech를 포함한 팩 모드 누락 `ko_kr`은 리소스팩 `pack/resourcepacks/rs-korean`으로 보완. 게임에서 **리소스 팩 → rs-korean** 활성화. 자세한 건 `docs/KOREAN_LOCALES.md`.

**모드 간 입력 연결:** RS는 바닐라 물 대신 등급 유체를 쓰므로 Oritech의 바닐라 물 레시피만 두면 바이오폴리머·플라스틱 같은 제작 경로가 끊긴다. `rsbridges`가 같은 재료·산출량으로 신선수와 증류수를 받는 대체 레시피를 제공한다. Aquaculture와 Fish of Thieves의 생선은 공용 raw-fish 태그로 Farmer's Delight 요리에 연결되므로 중복 스튜 레시피는 두지 않는다. BoP 기후·해안·고인물 분류는 Realism Survival 태그에서 관리한다. 세부 목록과 갱신 주의점은 `pack/datapacks/rs-item-bridges/INVENTORY.md`에 있다.

\* XMod는 POM이 Essentials/Ranks/Backups 등을 끌어와서 **exclude** 해 둠. **지도·클레임·포스로드는 FTB Chunks** (`pack/config/ftbchunks-world.snbt`). Xaero·OPC는 쓰지 않음.  
\*\* Advancement Plaques/Iceberg는 Iceberg nightconfig 쉐이딩 → FML `IllegalAccessError`라 **제외**.  
\*\*\* GlitchCore·Serene Seasons·TerraBlender·BoP는 Modrinth **version id**로 NeoForge jar를 고정 (version_number만 쓰면 Fabric jar가 잡힐 수 있음). TerraBlender는 `26.1.2.0.2` (0.3은 Neo 26.2 요구).  
\*\*\*\* `runServer` + Sophisticated + Iris: IrisCompat가 서버에서 클라 클래스를 건드려 크래시 → **전용 서버는 `-PclientMods=false`**.

### B. 아직 실행 목록에 안 붙인 것 / 막힌 것

| 모드 | 상태 | 이유 |
|---|---|---|
| When Dungeons Arise | **보류** | NeoForge 26.1 jar 없음 (최신=1.21.1) |
| Comforts | **보류** | `rs-comforts` 연동 전 |
| Dynamic Trees | **보류** | DTBOP 없음 — BoP와 동시 비권장 |
| Advancement Plaques (+ Iceberg) | **보류** | Iceberg nightconfig ↔ FML 충돌 |
| Sophisticated Storage / Patchouli | **여유** | 아직 안 넣음 |

### C. 60시간·탐험용 (실행에 반영된 것 / 남은 주의)

Create나 Gaia 없이도 “할 거리”가 남도록. **아래 “꼭/다음”은 대부분 `-Ppackcontent`로 이미 localRuntime.**

| 급 | 모드 | 실행 | 조심할 점 |
|---|---|---|---|
| 꼭 | BoP + TerraBlender | ✅ | BoP Overworld 바이옴 기후 태그는 RS 데이터에 반영됨. 신규 BoP 버전에서 태그 목록 재확인 |
| 꼭 | Waystones | ✅ | 사람당 워프 ~2. 레시피 너프는 팩 데이터 |
| 꼭 | Sophisticated Backpacks | ✅ | **넣는 칸만**. 자석·자동 줍기 끔(설정) |
| 다음 | Comforts | ❌ | `rs-comforts` 후 |
| 다음 | Nature's Compass | ✅ | |
| 다음 | Artifacts | ✅ | Curios 벨트 칸과 겹침 — 계측 슬롯 정리 권장 |
| 다음 | Guard Villagers | ✅ | BoP 월드에서 마을·경비 스모크 |
| 다음 | Pet Home | ✅ | |
| 나무 | Treeliable | ✅ | FallingTree와 동시 금지 |
| 여유 | Patchouli / Sophisticated Storage | ❌ | |

### 나무·벌목 (Dynamic Trees 대신 쓸 것)

Dynamic Trees가 주는 느낌은 사실 **두 갈래**다.

1. **자라는 모습** — 씨앗에서 굵어지고, 숲이 퍼짐 (DT 본체)  
2. **베는 손맛** — 밑동만 쳐도 나무가 넘어지고, 크기에 따라 시간이 걸림  

BoP용 DT 브리지(DTBOP)가 26.1에 없어서, **BoP를 켠 채 DT만 켜면** 바닐라 나무만 다이나믹하고 BoP 나무는 그대로라 어색하다.  
그래서 1차는 이렇게 가는 걸 추천한다.

| 역할 | 추천 모드 | 26.1 | 팩에서의 위치 |
|---|---|---|---|
| **벌목 (메인)** | **Treeliable** | ✅ | 나무 크기만큼 시간·내구도·배고픔. “현실 생존”에 잘 맞음. Cloth Config 권장 |
| 벌목 (대안) | **FallingTree** | ✅ `26.1.2.4` | 설정이 많음. **Shift-down(한 칸씩 내려옴)** 모드가 치트감이 덜함. 도구 내구도를 원목 수만큼 깎게 켤 것 |
| 벌목 (단순) | TreeTimber | ✅ | 한 방에 쓰러뜨리기. 밸런스 약하면 패스 |
| 자라는 나무 | **Dynamic Trees** | ✅ (β) | **BoP 끄거나**, DTBOP 나올 때만 같이. 지금은 보류가 안전 |
| HT's TreeChop | — | ❌ 26.1 없음 | Treeliable이 사실상 후계(포크). TreeChop 원본은 기다리지 말 것 |

**1차 권장 조합**

- BoP 켠다 → **Treeliable**(또는 FallingTree Shift-down)만 넣는다. DT는 빼거나, 나중에 DTBOP 오면 재검토.  
- BoP 안 켠다 → DT + Treeliable도 가능. 다만 DT 나무가 벌목 모드랑 충돌하는지만 스모크 테스트.  
- **둘 다(Treeliable + FallingTree)는 넣지 말 것.** 한 개만.

설정 메모 (현실 팩 톤):

- 잎 달린 나무만 통째로 인식 (집 기둥 실수 방지 — Treeliable 기본에 가까움)  
- 도끼만, 웅크리기면 일반 파괴  
- 큰 나무일수록 오래 / 내구도·허기 많이  
- 산업·생존 퀘스트에 “원목 N” 제출이 있으므로, 벌목이 너무 사기면 초반 목재 퀘스트가 김이 빠짐 → 위 설정으로 조절

### D. 지금은 안 넣거나 미루는 것

| 모드 | 결정 | 이유 |
|---|---|---|
| Create | 기다림 | 26.1 jar 없음. 물 저장·운반은 LFT+Pipez, 전력 생산·기계 가공은 Oritech으로 채움 |
| Oritech Things | 기본 구성 제외 | 26.1.2 빌드가 알파이며, 고단계 속도·수율 업그레이드와 비행 장비가 팩 밸런스를 크게 바꿈 |
| Refined Storage | 보류 | 산업 후반이 너무 허전할 때만 검토 |
| Dynamic Trees (+ BoP용 브리지) | 보류 (벌목은 Treeliable로 대체) | DTBOP 26.1 없음. BoP와 동시에 켜지 말 것 |
| YUNG 계열 / Battle Towers | 보류 | T&T+WDA만으로도 건물 많음. 더 넣으면 맵이 유적투성이 |
| BoM / Gateways | 보류 | 최종장 **옆가지** 보스용으로만 |
| Gaia | 안 씀 | |
| Domestication Innovation | 안 씀 | → Pet Home |
| Dynamic Lights | 안 씀 | 밤에는 횃불·석탄이 계속 필요하게 |
| 스킬 모드 | 1차에선 안 씀 | 퀘스트·SoL이랑 역할이 겹침 |
| Supplementaries 같은 “이것저것 한 무더기” | jar 없으면 안 씀 | 필요한 것만 골라 넣기 |

### E. 시간 목표 (Sunlit Valley식 나눔)

Society: Sunlit Valley는 **초반 탭만 보고 시작**하고, Community Center·Collection은 나중에 펼친다.  
우리도 그렇게 나눈다.

| 어디까지 | 목표 시간 | 하루 2시간 기준 |
|---|---|---|
| **튜토리얼 + 번들(인장)까지** — “스토리 클리어” | ~**60시간** | 약 1달 |
| **도감(Collection) + 완벽(Perfection)까지** — “진짜 엔딩” | 추가 ~**40~60시간** | 합치면 약 2달 |

예전처럼 도감을 짧게 잡으면 60시간에 엔딩이 난다.  
SV에 가깝게 도감을 키우면, 60시간은 “본편 끝”, 그다음이 수집이다.

---

## Sunlit Valley 퀘스트 실측 → 우리 설계

원본: [Society: Sunlit Valley](https://github.com/Chakyl/society-sunlit-valley) 의 `config/ftbquests/quests/chapters/*.snbt`  
챕터마다 `tasks: [` 개수 ≈ 퀘스트 수. **합계 약 1,140개**, 챕터 파일 29개, 그룹 4개.

### SV 탭 구조 (배울 점)

가이드에서도: **처음엔 Community Center·Collection을 접고**, Welcome / Tutorial / Guides만 보라.

| 그룹 | 챕터 예 | 실측 개수 | 역할 |
|---|---|---|---|
| Welcome | welcome | 13 | 규칙 읽기·체크 |
| **Tutorial** | Getting Started(23), Farm Building(67), Advanced(50), Prismatic(66), Mechanical(42), Mystical(32) | **~280** | 순서 있는 본편 |
| **Community Center** | Pantry·Boiler·Vault·Crafts·Fish Tank·Abandoned Farm | **~30** | 번들 제출 → 큰 해금 |
| **Guides** | Equipment, Tools, Transportation, Villagers… | **~100** | 안 해도 진행되는 설명 |
| **Collection** | Crops(91), Fish(80), Cooking(**214**), Brews(85), Minerals, Butterflies(85), Perfection(22)… | **~700+** | 하나하나 수집 |

난이도 톤: 보스 한 방보다 **제출·제작·먹기·방문이 길게 쌓임**.

### 우리 목표 (개수·난이도 맞춤)

| | Sunlit Valley | Realism Survival |
|---|---|---|
| 총 퀘스트 | ~1,140 | **~1,000~1,100** |
| 본편 (튜토리얼+번들) | ~310 | **~280~320** → **약 60시간 / 1달** |
| 도감+완벽 | ~700+ | **~700+** → 추가 약 1달, **진짜 엔딩** |
| 초반 UX | 큰 탭 접기 | 동일 |

---

## 퀘스트 책 구조 (SV식)

```text
Welcome
├─ Tutorial   … 본편 I~V (처음엔 이것만)
├─ Guides     … 팁 (접어 둬도 됨)
├─ Bundles    … 인장용 번들방 (= Community Center)
└─ Collection … 도감 + Perfection 엔딩 (처음엔 접기)
```

### Welcome (~12)

규칙·물 등급·벌목(Treeliable)·“번들·도감은 나중에” 안내. 대부분 체크. 보상은 식량·횃불 수준.

### Tutorial (~250) — 60시간 본편

| 챕터 | 목표 개수 | SV 대응 | 내용 |
|---|---|---|---|
| **I 생존 기반** | ~25 | Getting Started(23) | 집·물·증류 → **증류 1→5** (아래 상세) |
| **II 밭과 부엌** | ~65 | Farm Building(67) | 계절, FD, 증류수 요리, SoL, 낚시, Comforts |
| **III 밖으로** | ~50 | Advanced(50) | 바이옴, T&T, Lootr, 파티, Waystone#1, 나침반 |
| **IV 깊은 곳** | ~65 | Prismatic(66) | WDA·목록 A/B, 네더, Artifacts, Waystone#2 |
| **V 자동화** | ~45 | Mechanical(42) | Iron Chest, 호퍼 증류, Pipez+LFT, 백팩(저장만) |

### Guides (~100)

물·체온 팁, 장비, 지도/FTB Chunks, 마을, 펫, 파이프 설명. 본편 게이트 아님.

### Bundles (~31) — 인장

| 방 | 개수 | 제출 예 | 보상 |
|---|---|---|---|
| 생존 방 | 6 | 증류 16, 필터+증류기, 계측 3종, 침낭, 기후 | 생존 인장 |
| 부엌 방 | 6 | 사계절 수확, FD 요리 세트, SoL N3, 낚시 음식 | 음식 인장 |
| 탐험 방 | 7 | 목록 A, Lootr 10, 목록 B, 네더 상자 | 탐험 인장 |
| 목축 방 | 6 | 종 6, 우리, 탈것, Pet Home, 꿀 | 길들이기 인장 |
| 공방 방 | 6 | 상자 티어, Pipez, LFT, 자동 증류 | 산업 인장 |

다섯 인장 → Collection의 Perfection 입구.

### Collection (~700) + Perfection (~20)

| 챕터 | 목표 | SV 대응 | 형태 |
|---|---|---|---|
| 작물 | ~90 | Crops(91) | 수확/제출 1종=1퀘스트 |
| 요리 | ~180 | Cooking(214) | **먹기** 1종=1퀘스트 (SoL 목록과 맞춤) |
| 음료 | ~40 | Brews 일부 | 마시기 |
| 물고기 | ~70 | Fish(80) | 낚시·먹기 |
| 몹 | ~80 | 슬라임·생물 분산분 | 처치 1종 |
| 구조·바이옴 | ~50 | — | 방문 |
| 동물·펫 | ~40 | Creatures | 길들임 |
| 광물 (얇게) | ~40 | Minerals/Gems | 캐기 |
| 장인 체크 | ~30 | — | 시설 보유 |
| 유물 (선택) | ~40 | Artifacts/Relics | 획득 |
| **Perfection** | ~20 | Perfection(22) | 도감 클리어 모아 **엔딩** |

보스 한 마리로 Perfection을 건너뛰지 않음.

### 총합

Welcome12 + Tutorial250 + Guides100 + Bundles31 + Collection~700 ≈ **~1,090** (SV와 동급).

---

## Tutorial I — 생존 기반 (상세, 확정에 가까움)

**목표:** 살아남는 기본을 갖추며 **안전한 물(증류수)** 쪽으로 데려가기.  
**왕관:** 증류수 병 **1개** 제출 → **5개**. 초중반 비상분이지 무한 충전이 아니다.

첫 증류수를 얻는 방법은 퀘스트 글에 둘 다 적어 둔다.

- 건물·룻·월드에 있는 물 가마솥(RS)에서 이미 증류수가 나오기도 하고,  
- 아니면 증류기를 만들어서 직접 뽑는다.

### 메인 (순서대로 → 왕관)

작고 가벼운 과제. 전부 왕관으로 이어진다.

| # | ID | 종류 | 할 일 | 작은 보상 | 왜 있나 |
|---|---|---|---|---|---|
| 1 | `surv_e0_handbook` | 읽기 | 퀘스트책 / 생존 안내 열기 | 빵 8 또는 사과 8 | 목표가 “안전한 물”임을 알려 줌 |
| 2 | `surv_e0_shelter` | 감지·제출 | 모닥불 또는 침대 | 횃불 4 등 | 쉴 곳 |
| 3 | `surv_e0_vessel` | 제출 | 유리병 또는 양동이 1 | 유리병 3 | 물을 담을 그릇 |
| 4 | `surv_e0_heat` | 제출 | 석탄 4 또는 숯 8 | 숯 8 | 나중에 증류 연료 |
| 5 | `surv_e0_grades` | 읽기 | 물 등급 설명 (증류/민물/고인/바다) | 민물 병 등 | 바닷물·고인물 함부로 마시지 말라고 |
| 6 | `surv_e0_raw` | 감지 | RS 음료 한 번, 또는 월드 물로 병 채우기 | 꿀·스튜 | 갈증 시스템이 실제로 돌아감을 느끼게 |
| 7 | `surv_e0_kit` | 제출 | 온도계 | Serene Seasons 달력 | HUD·준비 감각 |
| 8 | `surv_e0_still_hint` | 읽기·감지 | 증류/필터 설명 읽기, 또는 필터·증류기 만들기 | 철 2 | 증류로 가는 길 |
| **★** | `surv_e0_crown` | **제출** | **증류수 병 1** | **증류수 병 5** | 극초반 생존 클리어 |

순서: 1→2→3→4→5→6→8→★.  
7번은 2번 이후 언제든. 왕관을 막지는 않는다.  
왕관은 최소한 8번까지(줄이면 5+8) 한 뒤에.

### 옆길 (왕관을 막지 않음)

읽기거나 가볍게 제출. 보상은 조금씩.

| ID | 할 일 | 보상 |
|---|---|---|
| `surv_e0_side_food` | 날음식 8개 제출 | 구운 감자 4 |
| `surv_e0_side_wood` | 원목 16 | 상자 1 |
| `surv_e0_side_cobble` | 조약돌 32 | 돌 16 |
| `surv_e0_side_torch` | 횃불 8개 만들기/놓기 | 횃불 8 |
| `surv_e0_side_map` | 지도 만들기 또는 FTB Chunks 맵 쓰는 법 읽기 | 나침반 |
| `surv_e0_side_fish` | 물고기 1 | 익힌 생선 |
| `surv_e0_side_sleep` | 한 번 자기 | 침대 또는 양모 |
| `surv_e0_side_biome` | “체온 조심” 읽기 | 가죽 |

### 이 단계에서 지킬 것

- 보상은 먹을 것·도구 위주. 끝게임 장비 주지 않기.  
- “큰 보상”은 증류 5병뿐.  
- 파이프·큰 탱크·워프스톤은 여기서 안 연다.  
- 멀티면 제출·마시기는 **각자**.

---

## Tutorial II~V · Bundles · Collection (작성 순서)

위 예산표를 채우는 순서. 예전에 쓰던 “분야×극초반~후반” 표(~130개)는 **폐기**하고, SV식 개수로 다시 깐다.

### 권장 플레이 순서

```text
1) Welcome + Tutorial I (증류 왕관)
2) Tutorial II (부엌·SoL) ‖ Guides 필요한 페이지만
3) Tutorial III → IV (탐험)
4) Tutorial V (파이프·상자)
5) Bundles 다섯 방 → 인장
   ※ 여기까지 ≈ 60시간 / “본편 클리어”
6) Collection 도감들 (아무 순)
7) Perfection → 엔딩
```

### Tutorial II 예산 쪼개기 예 (~65)

| 묶음 | 개수 | 예 |
|---|---|---|
| 밭·씨앗·계절 | 15 | 경작, 계절별 수확, Serene Seasons 달력 |
| FD 주방 | 15 | 도마·칼·냄비·프라이팬, 증류수 요리 |
| SoL 단계 | 12 | 하트 N1~N3 중간 체크 (최종 100%는 요리 도감) |
| 낚시 | 10 | 낚싯대, Aqua/FoT 몇 종 |
| 수면·쉼터 | 8 | 침대, 침낭, 하루 생존 |
| 왕관/중간 보상 | 5 | 백팩(저장만) 등 |

본편 SNBT 초안은 `pack/config/ftbquests/quests/`에 둔다. 생성: `python tools/generate_ftbquests.py` (→ `run/config/ftbquests`로도 복사).  
Collection 전량(~700)과 WDA 연동은 이후 패스에서 품목 표 고정 후 확장한다.

### 난이도 맞추는 법 (SV와 같게)

- 초반: **체크·읽기** 많음, 보상 소량 (Welcome / I)  
- 중반: **만들기·가기·상자** (II~V)  
- 번들: **여러 아이템 한꺼번에 제출** (CC)  
- 후반: **한 품목당 퀘스트 하나**로 물량 (Collection Cooking 214개처럼)  
- 보상으로 생존을 망가뜨리지 않기 (초반 무한 증류·자석 백팩 금지)

---

## 제출·보상 눈금 (SV에 맞춤)

SV 실측 요약 (보상 `count` 중앙값·최대, 번들 제출 형태):

| SV 구간 | 제출(과제) | 보상 느낌 |
|---|---|---|
| Welcome | 대부분 체크만 | 1~8개 소모품 (묘목 2, 비료 8…) |
| Getting Started / Farm Building | 만들기·가지기 1종, 가끔 씨앗 16 | **중앙값 1개** (기계·도구 해금). 가끔 4~16. 드물게 32~64 |
| Community Center 번들 | **서로 다른 품목 3~5종 × 각 1**(또는 소수) | **다음 단계 도구 1개** 또는 소모품 16~24 (스프링클러 1, 비료 20, 항아리 3…) |
| Vault(후반 번들) | 비싼 재화 묶음 | **유니크 1개** (텔레포터, 기계) |
| Collection (Crops/Cooking…) | 품목당 1 | 보상 **거의 없음**(1개·경험치). 수집 자체가 목적 |

원칙: **그 챕터에서 막 가능해진 것 하나**를 주거나, **바로 다음 발걸음에 쓸 재료 한 줌**.  
챕터를 뛰어넘는 장비·무한 자원을 주지 않는다. 그렇다고 빵 한 조각만 주는 허무한 보상도 피한다.

### 공통 규칙

| | 해도 됨 | 하지 말 것 |
|---|---|---|
| 제출량 | 흔한 것 8~32, 중간 것 1~8, 희귀·키 아이템 1 | 초반에 다이아 64, 증류수 64 |
| 보상량 | 소모품 4~24 / 해금 아이템 1 / 재료 8~16 | 한 퀘스트에 풀세트 장비 |
| 왕관·인장 | 그 방의 “상징” 1 + 실용 소량 | 인장만 주고 본편 보상을 다 몰아주기 |
| 도감 | 먹기·잡기·방문 1회 | 도감마다 고급 보상 (SV도 안 함) |
| RS 특수 | 증류 왕관 1→5는 Tutorial I의 **유일한 큰 물 보상** | 초반 무한 증류, 자석 백팩, Waystone 남발 |

### 구간별 제출 · 보상표 (우리 팩)

#### Welcome / Tutorial I

| 퀘스트 종류 | 제출·과제 | 보상 (한 퀘스트당) |
|---|---|---|
| 읽기·체크 | 없음 | 식량 4~8 **또는** 횃불 4~8 |
| 가벼운 제출 | 원목 16, 조약돌 32, 석탄 4~8 | 같은 티어 소모품 4~16 |
| **증류 왕관** | 증류수 병 **1** | 증류수 병 **5** (여기만 물 “두둑”) |

과도: I에서 Iron Chest·Pipez·Waystone.  
과소: 왕관 보상이 병 1→1이거나, 메인 보상이 계속 막대기 1개.

#### Tutorial II (부엌)

| 종류 | 제출·과제 | 보상 |
|---|---|---|
| 도구 해금 | 도마/칼/냄비 **제작 감지**(제출 아님도 OK) | 재료 4~8 **또는** 음식 1~2 |
| 요리 | 지정 식사 **1회 먹기** / 재료 제출은 2~4종×1~4 | 같은 계열 음식 2~4, 뼛가루 8~16 |
| SoL 중간 | 하트 구간 도달 | 씨앗·비료 8~16 (온실급 아님) |
| 챕터 큰 보상 (1회) | 조건 묶음 | **백팩 기본형 1** (업그레이드 OFF) |

과도: II에서 LFT·다이아 상자.  
과소: 냄비 만들고 보상이 막대기.

#### Tutorial III~IV (탐험)

| 종류 | 제출·과제 | 보상 |
|---|---|---|
| 준비 키트 | 증류 4 + 식량 16 + 횃불 32 (**가지고 있기** 확인, 뺏지 않기 가능) | 식량·횃불 보충 8~16 |
| 구조물 | 들어가기 / Lootr **1~3** | 철·화살·경험치. 장비 풀세트 금지 |
| 목록 A 중간 | 사이트 5곳급 | 마법책 저급 **1** 또는 상자 1 |
| Waystone | 챕터당 **재료 세트 1회** | 사람당 워프 **총 2**가 넘지 않게 |
| 선택 보스 | 처치 | 자랑용 1. 인장·엔딩과 무관 |

과도: III에서 Waystone 무한, 네더라이트.  
과소: WDA 클리어하고 보상 빵 1.

#### Tutorial V (산업)

| 종류 | 제출·과제 | 보상 |
|---|---|---|
| 상자 티어 | 구리→철→금 **각 1회 제작/설치** | 다음 티어 재료 4~8 |
| 호퍼 증류 | 동작 1회 감지 | 철 4~8 |
| Pipez / LFT | 설치 1 + 루프 1 | 파이프 8~16, 탱크 부품 1 |
| 챕터 마무리 | 루프 증명 | 다이아 상자 **재료**(상자 완제품 난사 X) |

#### Bundles (인장 방) — SV Pantry/Vault 눈금

방마다 퀘스트 5~7개. **한 번들은 품목 3~5개**, 수량은 아래.

| 방 | 제출 눈금 (예) | 방 클리어 보상 |
|---|---|---|
| 생존 | 증류 16; 필터+증류기 설치; 계측 3종; 침낭; 더위·추위 토큰(또는 방문 감지) | **생존 인장** + 증류 4~8 |
| 부엌 | 계절 작물 4종×1~4; FD 대표 요리 3~4; SoL N3; 낚시 음식 3 | **음식 인장** + 씨앗/비료 16~24 |
| 탐험 | 목록 A 완료; Lootr 10; 목록 B 2~4; 네더 상자 1 | **탐험 인장** + 물약·수리 재료 (장비 풀셋 X) |
| 목축 | 목록 종 6; 우리; 탈것; Pet Home; 꿀 3; 교배 3 | **길들이기 인장** + 사료 16 |
| 공방 | 철·금·다이아 상자; Pipez; LFT; 자동 증류 1회 | **산업 인장** + 파이프/철 16 |

번들 **중간** 퀘스트 보상은 SV처럼: 비료 16~20, 기계 **1**, 병 3 수준.  
방 전체를 깨야 인장 — 중간마다 인장을 쪼개 주지 않음(조각은 OK, 완성은 방 클리어).

과도: 한 번들에 네더라이트 풀세트 요구.  
과소: 인장 방에 원목 4개만 내고 인장.

#### Collection / Perfection

| 종류 | 제출·과제 | 보상 |
|---|---|---|
| 도감 1칸 | 해당 음식 1 먹기 / 몹 1 / 방문 1 | **없거나** XP 소량, 가끔 씨앗 1~4 |
| 도감 챕터 클리어 (90칸 등) | 하위 전부 | 소모품 16 **또는** 장식 1 |
| Perfection | 도감 챕터들 완료 | **엔딩 아이템 1** (칭호·코스메틱). 전투 장비 종결자 아님 |

SV Cooking 214개가 보상을 거의 안 주는 이유를 따른다: 물량은 과제에 있고, 보상 인플레로 해결하지 않는다.

### 한 줄 검산

새 퀘스트를 넣을 때:

1. **제출**이 그 챕터 플레이어가 이미 만질 법한 티어인가? (한 단계 위 재료는 “조금”, 두 단계 위는 금지)  
2. **보상**이 “다음 퀘스트 3~5개를 수월하게” 해 주는가, 아니면 “나머지 챕터를 스킵”하게 만드는가?  
3. 같은 구간 SV면 중앙값이 **아이템 1개 해금**인가, **16개 소모품**인가? 그에 맞춘다.

---

## 모드를 넣을지 말지 (퀘스트 기준으로 다시 본 것)

| 말 | 뜻 |
|---|---|
| **KEEP** | 이미 쓰거나, 꼭 유지 |
| **넣을 예정** | 1차에 넣을 생각 |
| **퀘스트로 잠금** | 모드는 넣되, 레시피·기능은 퀘스트 후에 |
| **보류** | 나중에 |
| **안 씀** | 이번엔 제외 |
| **기다림** | 버전 나올 때까지 |

핵심만 다시:

- 진행의 중심은 **FTB Quests**.  
- 탐험 건물은 **T&T + WDA**, 상자는 **Lootr**, 파티·클레임은 **FTB Teams + FTB Chunks**.  
- SoL·백팩·Waystone·Pipez/LFT는 **퀘스트 지나기 전에는 풀지 않거나 약하게**.  
- Gaia·스킬 모드·Dynamic Lights·(26.1 없는) Create/DI는 빼거나 기다림.

구조물 조합은 이렇게 고정한다.

| 모드 | 역할 |
|---|---|
| Towns and Towers | 눈에 띄는 탑·마을 |
| When Dungeons Arise | 큰 던전 |
| Lootr + FTB Teams/Chunks | 각자 상자 + 파티·클레임 |

---

## 던전을 어떻게 다룰지

기본안. 이 절을 고치려면 “규칙을 바꾼다”고 명시하고 고친다.

- **형태:** MMO 인스턴스 서버가 아니라, 오버월드에 있는 **층이 나뉜 유적** (2~4단계).  
- **입장:** 물·식량이 안정된 **중반**에야 쓸 수 있는 키/입장 아이템. 초반에 뚫고 끝장 장비 줍기 방지.  
- **보스:** 있어도 **선택**. 엔딩의 유일한 조건이 아님.  
- **RPG 깊이:** 장비 단계 + Artifacts 정도. 스킬 트리 풀세트·Gaia는 안 씀.

**상자:** Lootr로 **사람마다 따로**.  
**잡몹:** 월드를 같이 씀. 한 팀이 비우면 다른 팀에 빈 던전이 되는 건 싫다.  
**엔딩 인정:** 사람마다. 한 명이 서버 전체를 대신 클리어하는 구조는 금지.  
선택 보스를 넣더라도 “월드에 한 번만”이 아니라, 못 한 사람은 다시 소환할 수 있게.

**1차에 실제로 깔 것:** FTB (Quests/Teams/Chunks) + Lootr + T&T + WDA.  
YUNG을 잔뜩 얹지 말 것. BoM/Gateways는 최종장 옆가지.  
“파티가 키를 쓰면 근처 파티원만 문이 열린다” 같은 세밀한 UX는, 필요해지면 그때 작은 보조 모드나 커맨드로.

---

## 파티

멀티에서 유적 갈 때는 **파티를 맺는 것**을 전제로 한다. (FTB Teams + FTB Chunks)

- 인원은 밸런스 메모상 **2~4명** 정도가 적당 (설정은 나중에).  
- FTB Teams 파티원이면 FTB Chunks 맵에서 위치를 공유할 수 있다 (팀 Location Visibility). 웨이포인트도 파티에 공유할 수 있다.  
- 웨이스톤은 남의 클레임에서도 우클릭할 수 있다 (`ftbchunks` interact whitelist). 발견한 웨이스톤을 지도 웨이포인트로 자동 찍지는 않는다.  
- 집은 클레임해도 되지만, **던전 구역은 클레임 못하게** — 유적지를 사유화하지 말 것.  
- 계절·시계·기온은 FTB Chunks 미니맵 아래(생물군계 다음)에 붙인다. 지도를 숨기면 SeasonHUD가 우상단으로 돌아온다.  
- 플레이어별 **청크 forceload 한도 16** (`pack/config/ftbchunks-world.snbt` → `force_loading.max_force_loaded_chunks`). `force_load_mode = never`로 오프라인 forceload 끔. Xaero·OPC는 쓰지 않음.

| | 파티일 때 |
|---|---|
| 잡몹 | 같이 싸운다 (월드 공유) |
| 상자 | **각자** Lootr (먼저 연 사람이 독식하지 않음) |
| 입장 키 | 한 번 쓰면 근처에 있는 파티원에게 문이 열림. “중반 준비됨” 같은 조건이 있으면 각자 충족 |
| 보스(선택) | 같이 잡고, **엔딩/인정은 싸운 사람마다**. 빠진 사람은 다시 소환 |
| 갈증·체온 | **각자**. 물은 나눠 마시게 해도 시스템이 대신 채워 주진 않음 |

하지 말 것: 파티 공용 상자 하나, 호스트만 엔딩.

---

## 건물에 몹을 어떻게 둘지

원형은 두 가지. 규칙은 같고(키, 층, Lootr, 파티), 분위기만 다르다.

**A. 탑 (위로 갈수록 어려움)**  
지상 → 1층 잡몹·키 문 → 2층 센 몹·좋은 상자 → (3층) → 꼭대기에서 (선택) 보스 소환.

**B. 지하 (깊이 + 어둠·추위)**  
입구(키) → 갤러리 → 큰 방 → 금고 → 맨 안쪽.  
횃불·증류수 준비가 의미가 있다. (손에 드는 동적 광원 모드는 안 넣음.)

1차에서는 “세상에 레이드 포인트 20개”가 아니라,  
**중반용 탑 느낌 / 후반용 깊은 곳 느낌**이 읽히게 T&T·WDA를 희귀도로 조절한다.  
필드에는 바닐라(+바이옴)만. Gaia 없음.

진행·엔딩에 꼭 필요한 적은 **방 안·제단**에 둔다. 필드에 우연히 스폰되는 걸로 엔딩을 걸지 않는다.

유적에서의 흐름:

```text
파티 맺기 → 지도로 이동(물·식량은 각자) → 키로 문 열기
→ 같이 잡몹 정리 → 각자 상자 → (선택) 보스 → 인정 → 집으로
```

---

## 상태 표시 (모드 결정용)

| 표시 | 뜻 |
|---|---|
| **이미 씀** | 개발 실행(`localRuntime`)에 들어가 있음 |
| **넣을 예정** | 1차에 넣을 생각 (데이터·연동 남았을 수 있음) |
| **검토 중** | 넣고 싶지만 RS 쪽 손질이 먼저 |
| **보류** | 나중 |
| **안 씀** | 이번에 제외 |

### 개별로 적어 둔 결정

**1. Comforts**  
침대 = 집에서 제대로 쉬기(스폰도 갱신). 침낭 = 밖에 쓰러져 응급 수면(스폰 안 옮김, 회복도 약하게).  
이 차이가 RS에 들어가기 전에는 Comforts를 팩에 넣지 않는다.

**2. BoP + TerraBlender**  
넣을 예정. 다만 바이옴마다 갈증·체온·물 품질 태그를 데이터로 맞춘 뒤.

**3. Dynamic Trees / 벌목**  
- **자라는 나무(DT):** BoP 브리지 나오기 전에는 BoP와 같이 안 켬.  
- **베는 손맛:** **Treeliable** (차선 FallingTree). 자세한 표는 위 «나무·벌목».

**4. Artifacts**  
넣을 예정. 지금은 온도계·시계·Serene Seasons 달력이 벨트 칸을 같이 쓰므로, 계측용 칸을 나눈 뒤에 본격적으로.

**5. Guard Villagers**  
마을을 지우지는 않는다. BoP 켠 새 월드에서 마을·경비가 나오는지 확인한 뒤 넣는다.

**6~7. Jade, FTB Chunks**  
Jade는 편의 묶음. 지도·클레임·포스로드는 `-Ppackcontent`의 FTB Chunks.

**8. Waystones**  
넣을 예정. 중반, 사람당 워프 대략 2. 레시피는 팩 데이터로 비싸게. RS 코어가 아님.

**9~11. Mouse Tweaks, TrashSlot, Iron Chests, Voice Chat**  
이미 씀. TrashSlot은 Balm 필요. 보이스챗은 디스코드 있어도 월드 거리감용으로 유지.

**12. AmbientSounds**  
이미 씀 (CreativeCore 필요). BoP 넣은 뒤 특정 바이옴이 너무 조용한지만 보면 됨.

**13. Dynamic Lights**  
안 씀.

**14. Entity Culling / ModernFix / FerriteCore**  
이미 씀 (Sodium/Iris랑 같이 성능).

**15. 출시 전에 한 번 더 볼 것**  
초반 상자가 너무 겹치는지, 호퍼·파이프·증류가 끊긴 아이템이 없는지, Waystone이 탐험을 죽이는지, Comforts·DT+BoP 조합이 어색하지 않은지.

### 이미 RS 연동/실행에 있는 이름

JEI, AppleSkin, Sodium, Iris, Curios, Serene Seasons(+GlitchCore Neo jar), Farmer's Delight, Aquaculture, Fish of Thieves, Large Fluid Tank, Pipez, Cloth Config,  
+ `-Ppackcontent`: FTB Quests/Library/Teams/Chunks/XMod*, Better Advancements, T&T(+Cristel Lib), Lootr, SoL Carrot, BoP+TerraBlender, Waystones+Shogi(+Balm), Sophisticated Core/Backpacks, Nature's Compass, Artifacts, Guard Villagers, Pet Home, Treeliable, Oritech(+Athena, GeckoLib).

스모크 (2026-10-18): `runServer -Ppackcontent -PclientMods=false` → Done; `runClient` 풀스택 → OpenAL/타이틀까지 진입.

### 아이템 브리지 (`rsbridges` / `pack/datapacks/rs-item-bridges`)

모드 안에만 쓰이는 재료(넵튜늄, FoT 열대과일, BoP 갈대·장미석영 등)를 **팩 레시피**로 다른 모드·바닐라에 잇는다.  
태그로 철/다이아를 대체하지 않는다. 목록·조사 표: `pack/datapacks/rs-item-bridges/INVENTORY.md`.  
레시피는 RS jar `data/rsbridges/recipe/*`에도 실려 `mod_loaded`일 때만 등록(JEI에서 바로 보임).

### 편의 모드 켜고 끄기

```text
gradlew runClient                              # packqol + packcontent + medicineAddon 기본 ON
gradlew runClient -Ppackqol=false
gradlew runClient -Ppackcontent=false
gradlew runClient -PmedicineAddon=false
gradlew runServer -PclientMods=false           # 전용 서버: Sodium/Iris 끄기 (Sophisticated+Iris 이슈)
gradlew runGameTestServer                      # packqol/packcontent/medicineAddon 끔
```

---

## 직접 만들어 볼 만한 애드온

RS 코어는 **갈증·체온·물 등급 엔진**으로 두고, 팩 전용 살은 애드온·데이터팩으로 빼는 편이 맞다.  
공개 API는 `docs/ADDON_API.md` / `examples/rs-api-sample/`.

아래는 “남이 만든 모드로 대충 되나?” vs “우리가 짧게 짜는 게 낫나?” 기준이다.

### 바로 만들 가치 큼 (작~중간, 팩에 직결)

| 우선 | 가칭 | 하는 일 | 왜 애드온인가 |
|---|---|---|---|
| **1** | **rs-comforts** | 침대 = 집에서의 회복(갈증·체온 회복 강함, 스폰 갱신). 침낭 = 야외 응급(회복 약함, 스폰 안 옮김, 젖음·추위 페널티 가능) | Comforts를 넣기 **전에** 필요. 코어에 Comforts 하드 의존을 넣기 싫음. `SurvivalApi` / 수면·쉼터 이벤트로 충분 |
| **2** | **rs-bop-climate** (또는 데이터팩만) | BoP 바이옴마다 갈증 배수·기본 물 등급·체온 | 대부분 `biome_thirst` JSON + 바이옴 태그로 가능. 예외만 `TemperatureCalculateEvent` / `ResolveWaterQualityEvent`. **코드보다 데이터팩 먼저** 가도 됨 |
| **3** | **rs-quest-hooks** | FTB가 못 잡는 RS 행동 감지: 증류수 마시기, 필터/증류기 설치, 등급별 물 채우기, “위험 체온 없이 하루” 등 | 퀘스트 카탈로그가 RS 고유 행동에 많이 묶여 있음. 바닐라 advancement만으로는 애매한 것들. 작을수록 좋음 (커스텀 criterion / 간단한 트리거 아이템) |

### 있으면 좋은데, 1차 이후에

| 가칭 | 하는 일 | 비고 |
|---|---|---|
| **rs-medicine** | HP 회복 아이템 + 바닐라 상태이상 연계(몹별 확률 발병) + 전용/만능 치료템 | ✅ `addons/rs-medicine` · `-PmedicineAddon` (기본 ON). 아래 상세 |
| **rs-expedition** | 중반 “원정 키”, 파티가 근처에 있을 때 문 열기, (선택) 제단 재소환 | FTB Teams+데이터팩으로 절반 가능. UX를 예쁘게 하려면 작은 블록·아이템 모드 |
| **rs-chop-strain** | Treeliable/FallingTree로 큰 나무 벨 때 갈증·체온·허기를 RS 쪽으로 더 깎기 | 벌목 모드 설정만으로도 허기는 됨. RS와 **한 줄로 묶고 싶을 때만** |
| **물 등급 콘텐츠 팩** | 광천수·약수 등 추가 등급 + 병/양동이 + drink JSON | 샘플 애드온이 이미 형태를 보여 줌. 팩 전용 등급은 여기로 |
| **rs-guide** | Patchouli/Modonomicon 책: 물 등급, 증류 vs 필터 | 순수 데이터. 코드 거의 불필요 |

### rs-medicine (구현 — `addons/rs-medicine`, modid `rsmedicine`)

자연 회복을 낮춰 둔 뒤 **현장 회복·상태 대응**을 애드온 한 개로 담당한다.  
포켓몬식 “HP템 / 상태별 치료 / 만능” **역할만** 빌리고, 이름·IP는 쓰지 않는다.  
커스텀 탈진·충격·출혈 미터는 **1차에서 안 씀**.  
발병 상태는 전용 `MedicineEffects`(독소·부패·피로·쇠약·한기·충격·화상 아이콘) + 엔진 연동용 바닐라(실명·공중부양) + 불/동상 틱.

로드: `packcontent`와 같이 기본 ON (`runClient`/`runServer`), GameTest OFF. 끄기: `-PmedicineAddon=false`.  
소스셋 `medicineAddon` · `gradlew compileMedicineAddonJava`.

#### 발병 (운 + 몹 종류)

- 몹(또는 엔티티 태그)마다 **걸릴 수 있는 상태 1~2종**만 둔다. 예시는 밸런스용 초안:

| 몹/태그 예 | 연계 상태 (바닐라) |
|---|---|
| 거미·동굴거미·벌레류 | `rsmedicine:toxin` |
| 위더·위더스켈레톤 | `rsmedicine:decay` (희귀·짧게) |
| 좀비·허스크·드라운드 | `fatigue` 또는 `frailty` |
| 팬텀·셜커 | 바닐라 `blindness` / `levitation`(엔진 연동) |
| 블레이즈·마그마큐브 | 불 틱 + `burn` 아이콘 |
| 스트레이·눈골렘 | 동상 틱 + `chill` / 또는 `chill`만 |
| 번개·가디언 | `shock` |
| 스켈레톤·크리퍼 | **추가 상태 없음** (피해만; 좀비 계열은 위 행) |

- **피격 시 발병 확률 ≤ 10%** (몹별 상한. 대부분 4~8%대).
- 확률은 **현재 HP에 반비례**: HP가 높을수록 잘 안 걸림.  
  예: `p = pMax * (1 - currentHp / maxHp)` 를 기본으로 하고, `pMax ≤ 0.10`.  
  (풀피 ≈ 0%, 반피 ≈ `pMax/2`, 빈사 ≈ `pMax`.)
- 이미 같은 효과가 있으면 **무시** (시간 연장·중첩 없음).

#### 지속 시간 (플레잉 우선)

- **상한 8초** (160틱). 어떤 상태든 애드온이 새로 걸 때 이 값을 넘지 않는다.
- 길이는 **완전 랜덤**: 발병할 때마다 `uniform(1초 … 8초)` (틱 단위로 굴림). 상태 종류별 고정 구간은 두지 않는다.
- 불·동상도 애드온이 붙이는 추가분만 위 규칙을 따르고, 바닐라 본래 틱을 임의로 늘리지 않는다.
- 디버프 중에도 **이동·채굴·전투가 완전히 막히지 않게**. (슬로우 II 장시간·실명 장시간 금지 — 어차피 최대 8초.)

#### 아이템

| 역할 | 예 | 효과 |
|---|---|---|
| HP만 | 붕대 → 외상 키트 → 메디킷 | `NaturalRegenHandler` 감속 대상 아님. **상태 안 지움** |
| 상태별 | 해독제, 화상 연고, 온열 팩, 처치제, 안약… | 해당 바닐라 효과(또는 불/동상)만 해제 |
| 만능 | 야전 구급 키트 | 치료 대상 상태 일괄 해제, HP는 안 채움(또는 소량) |
| 고급 | 진료소 키트 | HP + 상태 (레시피 무거움) |

**기본 효과(힐·상태 해제)는 확정.**  
추가로, 회복 아이템·물약 사용 시 **낮은 확률**로 보너스 버프를 한 줄 더 건다 (운은 발병과 대칭).

| 규칙 | 초안 |
|---|---|
| 대상 | rs-medicine 회복/치료템 + **마시는 물약** (`minecraft:potion` / `POTION_CONTENTS`; 투척·잔류·팁드 화살 제외) |
| 확률 | **≤ 10%** (템 등급 높아도 남발하지 않음. 대체로 5% 전후) |
| 보너스 예 | `regeneration` / `resistance` / `absorption`(약) / `speed` 중 랜덤 1종 |
| 보너스 지속 | 디버프와 동일 — **1~8초 완전 랜덤**, 최대 8초 |
| 실패 시 | 아무 추가 없음 (본전 힐/해제는 그대로) |

치료 실패로 본전을 깎지는 않는다.

#### 넣지 않는 것 (이 애드온 1차)

탈진·충격·골절·감염 등 **완전 커스텀 상태 미터**.  
바닐라에 없는 “감전” 전용 효과를 새로 만들기보다, 위처럼 `slowness`/`weakness`로 표현.

#### API·코어 관계

- 애드온 소유 Attachment는 최소화(발병 쿨·최근 롤 정도). 상태는 바닐라 이펙트 그대로.
- HP 힐은 `SurvivalApi.allowUnscaledHeal` (`NaturalRegenHandler.allowNextHeal`) 또는 heal≥4 경로.
- 갈증·체온과 강한 커플링은 하지 않음(물·체온 루프와 역할 분리). 필요하면 나중에 약하게.

### 만들지 않는 편이 나은 것

| 아이디어 | 이유 |
|---|---|
| Dynamic Trees급 나무 성장 엔진 | 범위가 거대. DTBOP 기다리거나 DT 없이 간다 |
| 벌목 모드를 처음부터 자작 | Treeliable/FallingTree가 26.1에 있음 |
| 스킬 트리 / 직업 모드 | 퀘스트·SoL과 겹침. SKIP 유지 |
| Create와 같은 자동화 역할 전체 | 물 저장·운반은 LFT+Pipez, 전력·가공·기계 자동화는 Oritech으로 역할을 나눔 |
| Gaia급 필드 몹 팩 | 의도적으로 제외한 방향 |
| 장시간 전투불능형 상태이상 | 플레잉 훼손. rs-medicine **최대 8초·완전 랜덤**으로 방지 |

### 추천 작업 순서

```text
1) BoP 기후·수역 태그 → ✅ RS 리소스 태그에 반영 (BOP 업데이트 때 바이옴 목록 재검토)
2) rs-comforts → 끝난 뒤 Comforts를 팩에 넣음
3) Treeliable을 팩 목록에 추가 (설정 프리셋)
4) rs-quest-hooks → Survival 극초반·초중반 퀘스트를 FTB에 붙일 때
5) rs-medicine → ✅ 1차 구현 (`addons/rs-medicine`; 아이템·상태 아이콘 CC0/자작, `THIRD_PARTY_ASSETS.md`)
6) (중반 콘텐츠 들어갈 때) rs-expedition
```

애드온도 코어와 같이 **열린 바닐라 물(`Blocks.WATER`)을 남기지 않는다**는 규칙을 지킨다 (`ConvertVanillaWaterEvent` 취소로 변환을 건너뛰지 말 것).


