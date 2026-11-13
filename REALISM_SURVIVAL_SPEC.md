# Realism Survival — 기술 디자인 명세서 (Technical Design Specification)

| 항목 | 값 |
|---|---|
| 문서 버전 | 1.41 (2026-10-16) — 베이스 코어 계약 보완(음용 취소·HUD 토글·체온 blend·GameTest) |
| 대상 플랫폼 | Minecraft **26.1.2** / NeoForge **26.1.2.122** / Java **25** toolchain (Gradle 9.2.1, ModDevGradle 2.0.148) |
| 모드 ID | `realismsurvival` |
| 최상위 패키지 | `com.example.realismsurvival` |
| 역할 | **갈증·체온 베이스 엔진** — 심화 콘텐츠·팩 브릿지는 애드온 |
| 작성 | 메인 소프트웨어 아키텍트 |
| 독자 | 세부 기능을 구현할 주니어 개발자 / 애드온 작성자 |
| Java 소스 | **82**개 (`src/main/java`, 2026-10-15 기준) |

> 이 문서의 모든 API 이름·시그니처는 **실제 26.1 디컴파일 소스와 NeoForge 26.1.0.19-beta 소스 jar에서 직접 확인**한 것이다.
> 1.21.x 시절 튜토리얼·블로그의 API 이름과 다른 부분이 많으므로(§2), 외부 자료보다 이 문서를 우선한다.
> **방향:** 코어 = 갈증·체온 베이스 + WaterQuality 정체성 (§1·§15.1). 심화(기계·작물·악세서리·팩 브릿지) = 애드온.

---

## 0. 목차

1. 개요와 설계 원칙 (**코어 API 방향**)
2. **26.1 API 변경 사항 (반드시 숙지)**
3. 저장소·패키지 구조
4. 빌드 스크립트 (build.gradle / gradle.properties)
5. 데이터 모델: `SurvivalData`
6. Data Attachment: `SurvivalAttachments`
7. 데이터팩(Data Pack) 구조와 리소스 리로더
8. HUD 렌더링: `RealismSurvivalHUD`
9. 게임플레이 루프 (서버 로직)
10. 클라이언트 / 서버 물리적 분리 수칙
11. 구현 티켓 (작업 지시서)
12. 검증 절차와 현재 검증 상태
13. 밸런싱 기본값 테이블
14. 코딩 컨벤션
15. **코어 API 범위·레이어·로드맵**

---

## 1. 개요와 설계 원칙

### 1.1 모드 정체성

**Realism Survival**의 공식 방향은 **갈증·체온 베이스 엔진 + 애드온 계약**이다.

* 코어는 **체온/갈증 관련 베이스만** 제공한다 — 상태·평가·패널티·HUD·데이터팩 로더·공개 API/이벤트/태그.
* **심화**(정수 기계, 작물 생태, 악세서리 콘텐츠, 팩 전용 브릿지, 추가 생존 미터 등)는 **애드온이 붙인다**. 코어에 심화 콘텐츠를 계속 쌓지 않는다.
* 단독으로도 갈증/체온이 동작하는 **플레이 가능 베이스**. 애드온은 태그·데이터팩·이벤트·Attachment로만 확장한다.
* “만능 콘텐츠 팩”이 아니다.

### 1.2 코어 축 (계약) — 베이스만

| 축 | 베이스(코어) | 심화(애드온) |
|---|---|---|
| **갈증** | 0~100, 바이옴·행동 소모, `drink`/`biome_thirst` 회복, 탈수 패널티, HUD 물방울 | 정수·증류 라인 확장, FD/커스텀 음식 일괄 매핑, 질병·오염 등 2차 미터 |
| **체온** | 30.0~42.0 °C, ambient °C/h(현실×½×1.5)+활동열(걷기/수영/질주)·냉수 −4.5°C/h·단열, 저/고체온 패널티, HUD | 쉼터/기후 모드, 장비 세트 효과, 전용 열원 기계 |
| **물 정체성** | `WaterQuality` + 기본 4등급·worldgen 변환 (무엇을 마셨는가) | 필터/증류기·소금 UI, 추가 등급·유체 콘텐츠 |

작물 일조(§9.5)·Curios 슬롯(§9.11)·필터/증류기는 **심화/주변** — 제거해도 갈증·체온 베이스 계약은 성립해야 한다. 신규 심화는 코어 jar가 아니라 애드온으로 추가한다.

### 1.3 설계 원칙 (모든 티켓에 적용)

| # | 원칙 | 구현 수단 |
|---|---|---|
| P1 | **서버 권위(Server authority)** | 수치 변경은 논리 서버에서만. 클라이언트는 동기화된 사본을 읽어 그리기만 한다. |
| P2 | **데이터 주도(Data-driven)** | 바이옴별 갈증 소모량, 작물 성장 보정, 아이템별 회복량은 코드에 두지 않고 JSON 데이터팩 + Codec으로 로드한다. |
| P3 | **불변 값 객체(Immutable value object)** | `SurvivalData`는 `record`. 변경은 새 인스턴스 생성 → `setData`. |
| P4 | **물리적 사이드 분리** | `net.minecraft.client.*`는 `client` 패키지와 `RealismSurvivalClient`에서만 import 한다. |
| P5 | **렌더 파이프라인 중립** | HUD는 `GuiGraphicsExtractor` + 바닐라 `RenderPipelines.GUI_TEXTURED`만 사용 → Sodium/Iris 호환. |
| P6 | **매직 넘버 금지** | 엔진 상수(아이콘 크기 등)는 `static final`로 이름을 붙이고, 밸런싱 숫자는 데이터팩/서버 config로. |
| P7 | **애드온 우선 확장점** | 신규 연동은 가능하면 **태그 / 데이터팩 / NeoForge 이벤트**로. 애드온이 코어를 리플렉션·믹스인하지 않아도 되게. |

---

## 2. 26.1 API 변경 사항 (반드시 숙지)

아래 표의 "이전 이름"으로 코드를 쓰면 **컴파일되지 않는다.**

| 영역 | 1.21.x 이름 (사용 금지) | **26.1 실제 이름** | 비고 |
|---|---|---|---|
| 리소스 ID | `ResourceLocation` | `net.minecraft.resources.Identifier` | `Identifier.withDefaultNamespace("hud/food_full")`, `Identifier.fromNamespaceAndPath(ns, path)`, `Identifier.parse("a:b")` |
| GUI 드로잉 객체 | `GuiGraphics` | `net.minecraft.client.gui.GuiGraphicsExtractor` | 즉시 그리지 않고 `GuiRenderState`에 요소를 "추출(extract)"해 넣는 구조 |
| 스프라이트 블릿 | `guiGraphics.blitSprite(Identifier, x, y, w, h)` | `graphics.blitSprite(RenderPipeline, Identifier, x, y, w, h)` | **첫 인자로 `RenderPipelines.GUI_TEXTURED` 필수** |
| HUD 오버레이 등록 | `RegisterGuiOverlaysEvent` / `IGuiOverlay` | `net.neoforged.neoforge.client.event.RegisterGuiLayersEvent` / `net.neoforged.neoforge.client.gui.GuiLayer` | `GuiLayer#render(GuiGraphicsExtractor, DeltaTracker)` |
| 바닐라 레이어 ID | `VanillaGuiOverlay.FOOD_LEVEL` | `net.neoforged.neoforge.client.gui.VanillaGuiLayers.FOOD_LEVEL` | `Identifier` 상수 |
| HUD 우측 열 높이 | (없음) | `Gui.rightHeight` / `Gui.leftHeight` (public int, NeoForge 패치) | 매 프레임 39로 리셋, 각 레이어가 10씩 증가 |
| 아이템 홀더 | `ItemStack#getItemHolder()` | `ItemStack#typeHolder()` | `Holder<Item>` 반환 |
| 이벤트 버스 지정 | `@EventBusSubscriber(bus = Bus.MOD)` | `@EventBusSubscriber(modid = ..., value = Dist.CLIENT)` | **bus 파라미터 없음.** 이벤트 타입(`IModBusEvent` 여부)으로 자동 판별 |
| 데미지 | `player.hurt(src, amt)` | `player.hurtServer(ServerLevel, DamageSource, float)` | 서버 전용 시그니처 |
| 서버 리로드 리스너 | `AddReloadListenerEvent` | `net.neoforged.neoforge.event.AddServerReloadListenersEvent` | `addListener(Identifier key, PreparableReloadListener)` + `getRegistryAccess()` |
| JSON 리로더 생성자 | `SimpleJsonResourceReloadListener(Gson, String dir)` | `SimpleJsonResourceReloadListener(HolderLookup.Provider, Codec<T>, ResourceKey<? extends Registry<T>>)` 또는 `(Codec<T>, FileToIdConverter)` | Gson 기반 생성자 삭제 |
| 월드 시각 | `Level#getDayTime()` | `Level#getOverworldClockTime()` | 체온 주야 항(§9.3)에서 사용. `% 24000`으로 하루 위상 |
| 명령 권한 | `source.hasPermission(2)` | `Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)` | 26.1 `PermissionCheck` API |

그 외 변경 없이 유지되는 것: `AttachmentType`, `DeferredRegister`, `NeoForgeRegistries.Keys.ATTACHMENT_TYPES`, `PlayerTickEvent.Post`, `LivingEntityUseItemEvent.Finish`, `ModConfigSpec`, `RecordCodecBuilder`, `StreamCodec.composite`, `ByteBufCodecs.FLOAT`.

---

## 3. 저장소·패키지 구조

```
RealismSurvival/
├─ REALISM_SURVIVAL_SPEC.md
├─ build.gradle                            ← Modrinth + Illusive Soulworks; Sodium/Iris/Curios localRuntime (§4)
├─ gradle.properties                       ← sodium_ / iris_ / curios_version
├─ src/main/templates/META-INF/neoforge.mods.toml  ← mixins + curios optional
└─ src/main/
   ├─ java/com/example/realismsurvival/
   │  ├─ RealismSurvival.java / RealismSurvivalClient.java / RealismSurvivalConfig.java
   │  ├─ component/     SurvivalData, SurvivalAttachments
   │  ├─ client/        HUD·틴트·GradedFluidModels·WaterFluidTint·GradedWaterTint
   │  ├─ command/       RealismCommands (T-01)
   │  ├─ registry/      ModFluidTypes, ModFluids, ModBlocks(GradedLiquidBlock), ModItems
   │  ├─ water/         WaterQuality, 변환/수분/색/병·인챈트 헬퍼, WaterSurfaceCompat
   │  ├─ environment/   TemperatureEvaluator (T-05; insulation T-06)
   │  ├─ data/          biome_thirst / crop_* / drink / insulation 로더 + STREAM_CODEC
   │  ├─ network/       SyncSurvivalDataPayload, SurvivalDataSync (T-09)
   │  ├─ gametest/      SurvivalGameTests (T-11)
   │  ├─ event/         tick·작물·패널티·회복·스택·물 채우기/월드/디스펜서·병 조합·리스폰
   │  └─ mixin/         FarmlandBlock · FluidRenderer · FluidStateModelSet · BlockFluidFace
   │     └─ sodium/     SodiumDefaultFluidRendererMixin (soft)
   └─ resources/
      ├─ assets/realismsurvival/  lang, HUD 텍스처, 물 블록/아이템 모델
      ├─ data/minecraft/tags/     fluid/water, enchantment 테이블 등
      ├─ data/curios/tags/item/   슬롯별 빈 태그 (§9.11)
      ├─ data/realismsurvival/curios/{slots,entities}/  ← Curios 슬롯 등록
      └─ data/realismsurvival/realismsurvival/         ← ★ 네임스페이스 이중 (§7.2)
         ├─ biome_thirst / crop_growth / crop_light / drink
         └─ (enchantment JSON은 data/realismsurvival/enchantment/)
```

패키지 배치 규칙:

* `component` — 플레이어에 붙는 데이터와 그 등록. 사이드 중립.
* `data` — 데이터팩 스키마(record+Codec), 로더, 조회. **서버 전용으로 취급** (§7.6).
* `environment` — 환경→스탯 변환 (체온 목표 등). 서버 전용.
* `command` — Brigadier 명령. 서버 전용.
* `event` — 게임 버스 이벤트 핸들러. 서버 로직. 메서드 첫 줄에서 `ServerPlayer` 캐스팅으로 사이드를 걸러낸다.
* `client` — 클라이언트 전용. `RealismSurvivalClient`를 통해서만 참조된다. `ClientSurvivalData`·`DrinkTooltipHandler` 포함.
* `network` — 페이로드 정의·등록·서버 전송. 클라 핸들러는 `RealismSurvivalClient`에만 둔다.
* `mixin` — 농지·유체 렌더(바닐라/Sodium). 클라 전용 엔트리는 `realismsurvival.mixins.json`의 `client` 배열.

---

## 4. 빌드 스크립트

### 4.1 gradle.properties (변경 사항)

```properties
mod_id=realismsurvival
mod_name=Realism Survival
mod_version=0.1.0
mod_group_id=com.example.realismsurvival

sodium_version=mc26.1.1-0.8.9-neoforge
iris_version=1.10.9+26.1-neoforge
curios_version=15.0.0+26.1.2
```

### 4.2 build.gradle (변경 사항)

```groovy
repositories {
    exclusiveContent {
        forRepository { maven { name = "Modrinth"; url = "https://api.modrinth.com/maven" } }
        filter { includeGroup "maven.modrinth" }
    }
    maven { name = "Illusive Soulworks"; url = "https://maven.theillusivec4.top/" }
}

dependencies {
    if (project.findProperty('clientMods') != 'false') {
        localRuntime "maven.modrinth:sodium:${sodium_version}"
        localRuntime "maven.modrinth:iris:${iris_version}"
    }
    if (project.findProperty('curios') != 'false') {
        localRuntime "top.theillusivec4.curios:curios-neoforge:${curios_version}"
    }
}
```

### 4.3 왜 이 버전·이 구성인가 (검증 근거)

| 질문 | 답 |
|---|---|
| 왜 Sodium 0.8.9 / Iris 1.10.9인가? | Modrinth API 조회 결과, **Minecraft "26.1"을 메타데이터에 포함하는 NeoForge 빌드는 이 쌍뿐**이다. Iris 1.11.x(26.1)는 Sodium `mc26.1.2-0.9.2`를 **required** 의존으로 요구하는데, 그 Sodium은 26.1.2 전용이다. MDK의 `minecraft_version_range=[26.1]`과 맞추려면 0.8.9+1.10.9를 써야 한다. |
| 왜 `runtimeOnly`가 아니라 `localRuntime`인가? | MDK가 정의한 `localRuntime`은 `runtimeClasspath`를 확장하므로 **런타임 전용 의존성으로 동작**하되, `runtimeOnly`와 달리 이 모드를 의존하는 다른 프로젝트에 **퍼블리시되지 않는다.** 개발용 모드에 적합한 "runtimeOnly의 안전한 변형"이다. |
| 클라이언트 런에만 넣을 수 없나? | ModDevGradle은 26.1부터 런별 `additionalRuntimeClasspath`를 제거했다 (시도 시 "there is no additional classpath anymore for Minecraft 26.1" 오류). 대신 Sodium(내부 JarJar jar)과 Iris 모두 `@Mod(dist = Dist.CLIENT)`로 선언되어 있어 `runServer`에 함께 올라가도 모드 클래스가 인스턴스화되지 않음을 바이트코드로 확인했다. 실제 `runServer` 부팅 성공(§12). |
| 끄려면? | `gradlew runClient -PclientMods=false` |

### 4.4 런 설정

| 태스크 | 용도 |
|---|---|
| `gradlew runClient` | 클라이언트 + Sodium + Iris. 셰이더 환경에서 HUD 검증. |
| `gradlew runServer` | 전용 서버. `run/eula.txt`에 `eula=true` 필요(이미 생성됨). 데이터팩 로드 로그 확인. |
| `gradlew runData` | 데이터 생성기 (향후 태그/언어 파일 생성 시). |
| `gradlew build` | 컴파일 + `build/libs/realismsurvival-<ver>.jar` |

---

## 5. 데이터 모델: `SurvivalData`

파일: `component/SurvivalData.java`

```java
public record SurvivalData(float thirst, float temperature)
```

### 5.1 상수

| 상수 | 값 | 의미 |
|---|---|---|
| `THIRST_MIN / MAX / DEFAULT` | 0 / 100 / 100 | 갈증 포인트 범위, 시작값 |
| `THIRST_DISPLAY_POINTS` | 20 | HUD 환산 스케일 (아이콘 10개 × 2) |
| `TEMPERATURE_MIN / MAX / DEFAULT` | 30.0 / 42.0 / 36.5 | 체온 °C |
| `TEMPERATURE_HYPOTHERMIA` | 35.0 | 미만이면 저체온 |
| `TEMPERATURE_HYPERTHERMIA` | 38.5 | 초과면 고체온 |
| `DEFAULT` | `(100, 36.5)` | 신규 플레이어 / 손상 데이터 복구값 |

### 5.2 불변식(Invariants)

* **컴팩트 생성자가 두 필드를 무조건 clamp 한다.** 어떤 경로(디스크, 네트워크, 로직)로 생성되어도 범위 밖 값은 존재할 수 없다.
* `CODEC`은 `Codec.floatRange`로 범위를 **디코드 단계에서 재검증**한다 → 손상된 세이브는 clamp가 아니라 **에러 로그**를 남긴다(디버깅 가시성).
* 두 필드는 `optionalFieldOf(..., DEFAULT)` → 세이브에 필드가 없어도(구버전 호환) 기본값으로 로드된다.

### 5.3 직렬화 3종

| 필드 | 타입 | 용도 |
|---|---|---|
| `CODEC` | `Codec<SurvivalData>` | 범용 (명령어 출력, 테스트) |
| `MAP_CODEC` | `MapCodec<SurvivalData>` | `AttachmentType.Builder#serialize(MapCodec)` 요구 타입 |
| `STREAM_CODEC` | `StreamCodec<ByteBuf, SurvivalData>` | 서버→클라 동기화. `StreamCodec.composite(ByteBufCodecs.FLOAT, ..., ByteBufCodecs.FLOAT, ..., SurvivalData::new)`. 8바이트. |

### 5.4 파생 메서드

| 메서드 | 규칙 |
|---|---|
| `withThirst(f)`, `withTemperature(f)` | 새 인스턴스 반환 |
| `addThirst(delta)`, `addTemperature(delta)` | 음수 허용. 결과는 생성자에서 clamp |
| `thirstPoints()` | `clamp(ceil(thirst / 100 × 20), 0, 20)`. **ceil**인 이유: 0이 아닌 한 반 칸은 보이게 하여 "아직 조금 남았다"를 표현 |
| `isDehydrated()` | `thirst <= 0` |
| `isHypothermic()` / `isHyperthermic()` | 위 임계값 비교 |

> **확장 시 규칙**: 필드를 추가하면 (1) 상수, (2) `CODEC`과 `MAP_CODEC` 양쪽에 `optionalFieldOf`, (3) `STREAM_CODEC`, (4) 컴팩트 생성자 clamp, (5) `with*` 헬퍼를 **모두** 추가한다. 하나라도 빠지면 세이브 호환 또는 동기화가 깨진다.

---

## 6. Data Attachment: `SurvivalAttachments`

파일: `component/SurvivalAttachments.java`

```java
DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
        DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MODID);

DeferredHolder<AttachmentType<?>, AttachmentType<SurvivalData>> SURVIVAL_DATA =
        ATTACHMENT_TYPES.register("survival_data", () -> AttachmentType.builder(() -> SurvivalData.DEFAULT)
                .serialize(SurvivalData.MAP_CODEC)                       // ① 디스크 저장
                .copyOnDeath()                                           // ② 사망·엔드 귀환 시 복사
                .sync((holder, to) -> holder == to, SurvivalData.STREAM_CODEC) // ③ 소유자에게만 동기화
                .build());
```

### 6.1 생명주기

| 단계 | 동작 |
|---|---|
| 최초 접근 | `player.getData(SURVIVAL_DATA)` 시 값이 없으면 `DEFAULT`를 **생성·저장·동기화**한다 (NeoForge 기본 동작). |
| 저장 | 플레이어 NBT의 `neoforge:attachments` 하위에 `realismsurvival:survival_data = {thirst, temperature}`. |
| 사망 | `copyOnDeath()` → 리스폰 엔티티로 그대로 복사. **게임플레이상 리셋(예: 리스폰 시 갈증 60)이 필요하면 `PlayerEvent.PlayerRespawnEvent`에서 명시적으로 `set` 한다.** 어태치먼트 설정으로 해결하지 않는다. |
| 동기화 | 논리 서버에서 `setData`가 호출될 때마다 NeoForge가 `STREAM_CODEC`으로 **소유 플레이어에게만** 전송. `holder == to` 조건 때문에 타인의 수치는 전송되지 않는다. 초기 접속·차원 이동 시에도 자동 초기 동기화. |
| 서버 측 변경 → HUD 반영 | 별도 패킷 코드 **불필요**. 이것이 record + Attachment sync 조합을 택한 이유다. |

### 6.2 접근 규칙

```java
SurvivalData d = SurvivalAttachments.get(player);              // 양쪽 사이드 OK (클라=동기화 사본)
SurvivalAttachments.set(player, d.addThirst(-0.5f));           // 논리 서버 전용. 클라에서 호출 시 IllegalStateException
```

* `set`은 `player.level().isClientSide()`를 검사해 **개발 중 사이드 누수를 즉시 터뜨린다.** 제거하지 말 것.
* `get` 후 `set` 사이에 다른 핸들러가 끼어들 수 없도록, 한 이벤트 핸들러 내에서 **읽기 1회 → 계산 → 쓰기 1회**로 구성한다.
* 변경이 없으면 `set`을 호출하지 않는다 (`if (!next.equals(current))`). 불필요한 패킷/디스크 dirty 방지.

---

## 7. 데이터팩 구조와 리소스 리로더

### 7.1 전체 흐름

```
서버 리소스 리로드(월드 로드, /reload)
  └─ AddServerReloadListenersEvent (게임 버스)         SurvivalReloadListeners.onAddReloadListeners
       ├─ addListener(realismsurvival:biome_thirst, SurvivalJsonLoader<BiomeThirstProfile>)
       ├─ addListener(realismsurvival:crop_growth,  SurvivalJsonLoader<CropGrowthModifier>)
       ├─ addListener(realismsurvival:crop_light,   SurvivalJsonLoader<CropLightProfile>)
       └─ addListener(realismsurvival:drink,        SurvivalJsonLoader<DrinkDefinition>)
            │
            ├─ prepare()  [워커 스레드] : 디렉터리 스캔 → Codec(RegistryOps) 파싱 → Map<Identifier,T>
            └─ apply()    [서버 스레드] : Map.copyOf → SurvivalDataManager.publish*()  (volatile 교체)
                                                     │
                                       SurvivalTickHandler / CropGrowthHandler 조회
```

### 7.2 디렉터리 규칙 (왜 네임스페이스가 두 번인가)

`SurvivalJsonLoader`는 `SimpleJsonResourceReloadListener(HolderLookup.Provider, Codec, ResourceKey)` 생성자를 사용한다. 이 생성자는 `FileToIdConverter.registry(key)` → `Registries.elementsDirPath(key)` → NeoForge `CommonHooks.prefixNamespace(id)` 규칙으로 디렉터리를 만든다:

* key 네임스페이스가 `minecraft`면 `path`만, 아니면 `<namespace>/<path>`.
* 우리 pseudo key `realismsurvival:biome_thirst` → 디렉터리 `realismsurvival/biome_thirst`.

따라서 **어떤 데이터팩 네임스페이스에서든** 아래 경로가 스캔된다.

```
data/<pack_namespace>/realismsurvival/biome_thirst/<entry>.json   → id = <pack_namespace>:<entry>
data/<pack_namespace>/realismsurvival/crop_growth/<entry>.json
data/<pack_namespace>/realismsurvival/crop_light/<entry>.json
data/<pack_namespace>/realismsurvival/drink/<entry>.json
```

모드 내장 기본값은 `pack_namespace = realismsurvival`이므로 경로가 `data/realismsurvival/realismsurvival/...`가 된다. **오타가 아니다.** 서드파티 팩은 `data/mypack/realismsurvival/drink/cola.json`처럼 자기 네임스페이스로 추가·덮어쓰기(같은 id) 할 수 있다.

이 생성자를 택한 이유:
1. `RegistryOps`가 만들어져 `HolderSet<Biome>` 등 레지스트리 참조를 **태그 포함**으로 디코드할 수 있다.
2. NeoForge `neoforge:conditions`가 자동 적용된다 (`scanDirectory` 패치).
3. 파싱이 워커 스레드, 적용이 서버 스레드로 분리되는 바닐라 수명주기를 그대로 탄다.

### 7.3 HolderSet JSON 문법 (★ 가장 흔한 실수)

모든 `biomes` / `items` / `crops` 필드는 `RegistryCodecs.homogeneousList(...)` 코덱이다. 허용되는 세 가지 형태:

| 형태 | 예 | 비고 |
|---|---|---|
| 태그 단일 문자열 | `"biomes": "#minecraft:is_nether"` | `#` 접두사 필수 |
| ID 리스트 | `"items": ["minecraft:potion", "minecraft:milk_bucket"]` | **리스트 안에 `#태그`를 넣으면 파싱 실패** (실측: `Not a valid resource location: #minecraft:is_badlands`) |
| NeoForge 복합 | `"biomes": {"type": "neoforge:or", "values": [["minecraft:desert"], "#minecraft:is_badlands"]}` | `values`의 각 원소는 다시 위 세 형태 중 하나. `neoforge:and`, `neoforge:not`(`{"type":"neoforge:not","value":...}`), `neoforge:any` 도 사용 가능 |

26.1 바닐라에 존재하는 바이옴 태그(검증됨): `is_badlands, is_beach, is_deep_ocean, is_end, is_forest, is_hill, is_jungle, is_mountain, is_nether, is_ocean, is_overworld, is_river, is_savanna, is_taiga`. **없는 태그를 참조하면 해당 파일 전체가 로드 실패**하므로 새 태그는 먼저 `data/<ns>/tags/worldgen/biome/*.json`으로 정의한다.

### 7.4 스키마

#### `realismsurvival/biome_thirst/*.json` → `BiomeThirstProfile`

| 필드 | 타입 | 필수 | 범위/기본 | 의미 |
|---|---|---|---|---|
| `biomes` | HolderSet\<Biome\> | ✔ | — | 적용 바이옴 |
| `thirst_per_second` | float | ✔ | 0~100 | 가만히 있을 때 초당 갈증 소모 포인트 |
| `sprint_multiplier` | float | ✘ | 0~10, 기본 1.5 | 달리기 중 배수 |
| `priority` | int | ✘ | 기본 0 | 충돌 시 높은 값 우선 |

#### `realismsurvival/crop_growth/*.json` → `CropGrowthModifier`

| 필드 | 타입 | 필수 | 범위/기본 | 의미 |
|---|---|---|---|---|
| `biomes` | HolderSet\<Biome\> | ✔ | — | 적용 바이옴 |
| `crops` | HolderSet\<Block\> | ✘ | 생략 = 모든 작물 | 적용 작물 블록 |
| `growth_multiplier` | float | ✔ | 0~10 | 1.0=바닐라, 0.5=절반 확률, 0=성장 정지 |
| `priority` | int | ✘ | 기본 0 | |

바이옴 배수와 아래 `crop_light` 배수는 **곱셈**으로 합친다 (`CropGrowthHandler`).

#### `realismsurvival/crop_light/*.json` → `CropLightProfile`

| 필드 | 타입 | 필수 | 범위/기본 | 의미 |
|---|---|---|---|---|
| `crops` | HolderSet\<Block\> | ✔ | — | 적용 작물 블록 |
| `metric` | string | ✔ | `sun` / `sky` / `block` / `max` | 측정 채널 (§9.5) |
| `bands` | list\`{min,max,multiplier}\` | ✔ | light 0~15, mult 0~10 | 포함 구간별 성장 배수. **미매칭 광량 → 0** |
| `priority` | int | ✘ | 기본 0 | |

#### `realismsurvival/drink/*.json` → `DrinkDefinition`

| 필드 | 타입 | 필수 | 범위/기본 | 의미 |
|---|---|---|---|---|
| `items` | HolderSet\<Item\> | ✔ | — | 적용 아이템 |
| `thirst_restore` | float | ✔ | -100~100 | 사용 완료 시 갈증 변화 (음수 = 짠 음식) |
| `temperature_delta` | float | ✘ | -5~5, 기본 0 | 정상(36.5°C)으로의 조절 강도 (+ = 추울 때 온열, − = 더울 때 냉각; 정상 너머로 밀지 않음) |
| `priority` | int | ✘ | 기본 0 | |

> 포션 내용물(물병 vs 실제 포션) 구분이 필요해지면 `DrinkDefinition`에 `Optional<DataComponentPredicate>` 필드를 **optionalFieldOf로 추가**한다. 기존 JSON은 그대로 유효해야 한다.

### 7.5 충돌 해결 규칙 (`SurvivalDataManager.pickBest`)

1. `matches(...)`가 참인 엔트리만 남긴다.
2. `priority` 내림차순.
3. 동률이면 엔트리 `Identifier` 오름차순 (결정론 보장 — 파일 시스템 순서에 의존하지 않음).
4. 아무것도 없으면 `Optional.empty()` → 호출자가 config 폴백(`fallbackThirstPerSecond`) 또는 "효과 없음"으로 처리.

내장 기본값은 `overworld_default.json`을 `priority: -100`으로 두어 **어떤 서드파티 엔트리에도 밀리게** 설계되어 있다.

### 7.6 사이드 규칙과 클라 동기화 (T-09 ✅)

`SurvivalDataManager`는 **논리 서버에서만 채워진다.** 원격 서버에 접속한 클라이언트에서는 모든 맵이 비어 있다. 클라는 아래 경로로 동일 테이블을 받는다:

1. `OnDatapackSyncEvent`(게임 버스) → `SurvivalDataSync`가 `event.getRelevantPlayers()`에게 `SyncSurvivalDataPayload` 전송 (`PacketDistributor.sendToPlayer`).
2. `RegisterPayloadHandlersEvent`에서 `registrar.playToClient(TYPE, STREAM_CODEC)` (핸들러 없음).
3. `RegisterClientPayloadHandlersEvent`(`RealismSurvivalClient`)에서 `ClientSurvivalData.apply(payload)`.
4. 각 데이터 record에 `STREAM_CODEC` (`ByteBufCodecs.holderSet` / `map` / `optional`).
5. 툴팁·클라 API는 **`ClientSurvivalData`만** 조회. `SurvivalDataManager`를 클라에서 쓰지 않는다.

수락: 전용 서버 접속 후 포션 호버 → `drink/potion.json` 값(**갈증: +8**), 증류수병 → **+40** (물방울 4).

### 7.7 식용 아이템 스탯 툴팁 (AppleSkin + 본 모드)

**허기·포화는 AppleSkin에 맡긴다.** 본 모드는 갈증·체온·등급 물 효과만 그린다.

| 줄 | 담당 | 표기 |
|---|---|---|
| Hunger + Saturation | **AppleSkin** (클라 필수 의존) | AppleSkin 기본 아이콘/HUD |
| Thirst | `ThirstStatsClientTooltip` | HUD와 같은 물방울 (restore/5 → 0–20 포인트) |
| Body temp | `DrinkTooltipHandler` | **숫자** `Body temp: ±N °C` |
| Effects | 본 모드 | `PotionContents.addPotionTooltip` (포션 내용물 중복 없음) |

- **의존성**: `neoforge.mods.toml` → `appleskin` `type=required`, `side=CLIENT`, `ordering=AFTER`. 개발: `localRuntime` `squeek.appleskin:appleskin-neoforge:mc26.1-3.0.9` (maven.ryanliptak.com).
- **등급 양동이**: 병 drink JSON × `1` (모금 = 병 1병분; 양동이 fill 3 = 병 3개). 서버 `applyBucketSip`과 동일.
- drink JSON이 없는 식용: AppleSkin 허기/포화만 (+소비 효과). 스플래시/링거링·월드 물(빈손)은 대상 아님.

수락: AppleSkin 없이 클라 기동 시 의존성 오류; 익힌 소고기 → AppleSkin 허기줄 + 갈증 물방울 + Temp; 증류 양동이 fill 비례 물방울.

---

## 8. HUD 렌더링: `RealismSurvivalHUD`

파일: `client/RealismSurvivalHUD.java` — `implements net.neoforged.neoforge.client.gui.GuiLayer`

### 8.1 등록

```java
// RealismSurvivalClient (모드 버스, Dist.CLIENT)
@SubscribeEvent static void onRegisterGuiLayers(RegisterGuiLayersEvent e) { RealismSurvivalHUD.register(e); }

// RealismSurvivalHUD
event.registerAbove(VanillaGuiLayers.FOOD_LEVEL, LAYER_ID /* realismsurvival:thirst_level */, new RealismSurvivalHUD());
```

`registerAbove(FOOD_LEVEL, ...)`는 **FOOD_LEVEL 직후에 실행**됨을 뜻한다(z-order상 위, 실행 순서상 다음). 이 순서가 §8.2의 좌표 계산 전제다.

### 8.2 레이아웃 수학 (바닐라 `Gui.extractFood`와 1:1 대응)

NeoForge가 패치한 바닐라 `Gui`의 우측 열 규약:

```
extractRenderState(): leftHeight = 39; rightHeight = 39;          // 매 프레임 리셋
FOOD_LEVEL  : y = guiHeight - rightHeight; 그린다; rightHeight += 10;
(VEHICLE_HEALTH, AIR_LEVEL 도 동일 패턴)
```

따라서 우리 레이어가 FOOD_LEVEL 직후에 실행되면:

| 변수 | 식 | 설명 |
|---|---|---|
| `xRight` | `guiWidth / 2 + 91` | 핫바 절반 폭(182/2). 바닐라 배고픔과 동일 앵커 |
| `y` | `guiHeight - gui.rightHeight` | FOOD가 이미 `+10` 했으므로 **배고픔 줄보다 정확히 10px 위** |
| `x(i)` | `xRight - i * 8 - 9` | i = 0..9, 오른쪽→왼쪽, 8px 간격, 9px 아이콘 |
| 종료 | `gui.rightHeight += 10` | 다음 레이어(공기 방울 등)가 우리 위로 쌓이게 예약 |

채움 규칙 (`points = data.thirstPoints()`, 0..20):

```
threshold = i * 2 + 1
EMPTY 항상 → threshold <  points ? FULL
          → threshold == points ? HALF
```

| thirst | points = ceil(thirst/5) | 결과 |
|---|---|---|
| 100 | 20 | i=0..9 모두 `2i+1 < 20` → **FULL ×10** |
| 47.5 | 10 | i=0..4 (1,3,5,7,9 < 10) FULL, i=5.. (11..) EMPTY → **FULL ×5, EMPTY ×5** |
| 42.5 | 9 | i=0..3 FULL, i=4 (`9 == 9`) **HALF**, i=5.. EMPTY |
| 0.1 | 1 | i=0 (`1 == 1`) HALF, 나머지 EMPTY → "거의 바닥"이 반 칸으로 보임 |
| 0 | 0 | 전부 EMPTY |

HALF는 points가 **홀수**일 때만 나타난다 — 바닐라 배고픔 게이지와 동일한 특성이다.

탈것(말 등) 탑승 중에는 바닐라가 FOOD_LEVEL을 그리지 않으므로 `rightHeight`가 39에 머물고, 갈증 줄이 배고픔 자리를 차지한 뒤 탈것 체력이 그 위에 그려진다. 겹침은 없다. 이 동작은 **의도된 사양**이다.

### 8.3 표시 조건 (`cameraPlayer`)

바닐라 서바이벌 레이어의 `survivalVisible` 조건을 그대로 복제한다. 이벤트로 등록한 레이어에는 바닐라 조건이 자동 적용되지 않으므로 **직접 검사해야 한다.**

```
!options.hideGui && gameMode != null && gameMode.canHurtPlayer() && getCameraEntity() instanceof Player
```

### 8.4 Sodium / Iris 호환 규칙

| 금지 | 허용 |
|---|---|
| `RenderSystem.*` 상태 변경, 직접 GL 호출 | `graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ...)` |
| 커스텀 `RenderPipeline` 생성 | 바닐라 `RenderPipelines.GUI_TEXTURED` / `GUI` |
| `Tesselator`/`BufferBuilder` 직접 사용 | `graphics.fill(...)`, `graphics.text(...)` |
| `pose()` push 후 pop 누락 | `pose().pushMatrix()` … `popMatrix()` 쌍 |

이유: 26.1의 GUI는 "추출(extract) → `GuiRenderState` → `GuiRenderer`가 일괄 렌더" 구조다. Sodium/Iris는 `GuiRenderer` 아래 단계에서 개입하므로, 우리가 `GuiRenderState`에 바닐라 요소만 넣는 한 두 모드와 충돌할 지점이 없다. 레이어 코드가 예외를 던지면 게임 전체가 크래시하므로 `render()` 안에서는 null 가능 값(`gameMode`, `player`)을 반드시 가드한다.

### 8.5 HUD 레이아웃 — **구현 완료** (T-07 / T-12 / T-13)

바닐라 방어구 아이콘 줄(`ARMOR_LEVEL`)은 **제거**한다. 확보한 좌측 한 줄은 체온에 쓰고, 방어력은 HP 하트 **테두리**로 표현한다.

```
왼쪽 (leftHeight)                 오른쪽 (rightHeight)
  [체온 연속 스펙트럼 바 81×9]       [공기 / 탈것 등]
  [HP 하트 + 재료색 테두리]          [갈증 10칸]
  ──────────────────────────────── 핫바 ────────────────────────────────
                                  [허기]
```

| 레이어 | 등록 | 높이 예약 | 상태 |
|---|---|---|---|
| 갈증 | `registerAbove(FOOD_LEVEL, …)` | `rightHeight += 10` | **구현됨** |
| 방어 테두리 | `wrapLayer(PLAYER_HEALTH, …)` + `replaceLayer(ARMOR_LEVEL, no-op)` | `leftHeight` 변경 없음 | **구현됨** (재료 그룹은 코드 상수; `armor_tint` 데이터팩은 후속) |
| 체온 | `registerAbove(ARMOR_LEVEL, …)` 후 `leftHeight += 10` | HP 위 한 줄 | **구현됨** |
| 계측 (날짜/시간/기온) | `registerAboveAll(instruments, …)` | 좌하단 고정 좌표 (left/rightHeight 미사용) | **구현됨** — 손 또는 `#curios:belt`에 Serene Seasons 달력·시계·온도계 |

서버 `SurvivalData`가 sync되면 갈증·체온 HUD가 같은 틱에 반영된다. 체온 수치는 §9.3 환경 공식으로 갱신된다.

#### 8.5.1 계측 HUD (`InstrumentsHudLayer`)

화면 **왼쪽 하단** 세로 스택 (margin 8px, bottom-anchor). 줄마다 장비가 있을 때만 그린다 (손 메인/오프 또는 Curios belt). 화면이 낮아도 `y = max(2, …)`로 하단부터 쌓는다.

| 줄 | 트리거 | 표시 |
|---|---|---|
| 날짜 | `sereneseasons:calendar` 게이트 | `(이모지) Y년 M월 D일 (계절)` — 계절색. `SeasonCalendar` 365일/4계절 (SS 있으면 SS 권위). RS 전용 달력 아이템 없음 |
| 시간 | 바닐라 `minecraft:clock` | `HH:MM` — **주황** 고정 (`dayTime % 24000`, 0=06:00) |
| 기온 | `thermometer` | ambient °C — **온도에 따른 글자색** (`TemperatureEvaluator.computeAmbient`) |

Curios `#curios:belt` 슬롯 **size=3** — 시계·Serene Seasons 달력·온도계 동시 장착 가능. 날짜와 시계는 `dayTime`을 공유하지만 **표시 축이 직교**한다.

### 8.6 방어력 = HP 테두리 — **구현 완료** (T-12)

**의도:** 바닐라 방어구 게이지를 없애고, 하트의 **container(테두리)** 만 재료색으로 덮는다. HP fill·독/위더/동상 등 상태색은 바닐라가 그린 그대로 두고, HP가 깎여도 **빈 칸에 은색(재료색) 윤곽이 남는다.**

#### 8.6.1 칸 단위 (0.5칸 = half 테두리)

`armor = player.getArmorValue()` (바닐라 포인트, 보통 0~20). 허기/갈증과 **동일 수학**.

```
칸 i = 0..9 (왼쪽 → 오른쪽),  threshold = i * 2 + 1
armor >  threshold  → 해당 칸에 재료색 테두리 FULL (전체 윤곽)
armor == threshold  → 해당 칸에 재료색 테두리 HALF (왼쪽 절반만)
armor <  threshold  → 오버레이 없음 (바닐라 검정 테두리만)
```

| 방어력 | 결과 예 |
|---|---|
| 7 | FULL×3 + HALF×1 + 나머지 없음 |
| 1 | 맨 왼쪽 칸 HALF만 |
| 0 | 테두리 오버레이 전부 없음 |

HALF는 **테두리를 반만 그리는 것**이 확정이다 (올림/내림·별도 점 표시 금지).

#### 8.6.2 테두리 색 = 우세 재료 (합산)

장비 슬롯 `HEAD / CHEST / LEGS / FEET`만 대상. 각 조각의 **armor 속성 포인트**만 합산 (Protection 인챈트 제외).

1. 조각을 **틴트 그룹**(leather / gold / chain / iron / diamond / netherite / turtle / other)에 매핑.
2. 그룹별 방어 포인트 합산.
3. 합이 최대인 그룹의 색으로 테두리를 틴트.
4. **동점** 시 티어: `netherite > diamond > iron > chain > gold > leather > turtle > other`.

예시 (합의 검증):

| 착용 | 합산 | 테두리 색 |
|---|---|---|
| 가죽 흉갑(3) + 다이아 레깅스(6) + 다이아 헬멧(3) | 가죽 3, 다이아 9 | **다이아** |
| 다이아 헬멧(3) + 철 부츠(2) + 철 흉갑(6) | 다이아 3, 철 8 | **철** |

그룹→색 매핑은 현재 `ArmorHeartOverlay.TintGroup`에 **하드코딩**되어 있다. 데이터팩 `realismsurvival/armor_tint/*.json`으로 옮기는 것은 후속(T-12 잔여).

```json
{
  "items": ["minecraft:diamond_helmet", "minecraft:diamond_chestplate", "minecraft:diamond_leggings", "minecraft:diamond_boots"],
  "group": "diamond",
  "color": "#4AEDD9",
  "priority": 50
}
```

렌더: 속이 투명한 `armor_container` / `armor_container_half`(바닐라 9×9 윤곽) + `blitSprite(..., ARGB tint)`. 재료마다 PNG를 복제하지 않는다. HALF는 **왼쪽 절반**만 그린다.

#### 8.6.3 구현 요약

1. `replaceLayer(ARMOR_LEVEL, …)`로 바닐라 방어 줄 no-op.
2. `wrapLayer(PLAYER_HEALTH, old -> { old.render; ArmorHeartOverlay.render })`.
3. HP와 같은 `(x, y)`에 테두리만 blit. 속이 투명해 fill/독·위더 색이 비친다.
4. Sodium/Iris: §8.4 준수.

### 8.7 체온 마름모 10칸 — **구현 완료** (T-13)

HP **위** 좌측 한 줄. 허기/갈증과 같은 **10칸 마름모**(9×9, stride 8). 왼쪽→오른쪽 채움, 칸마다 파랑→빨강 틴트. **36.5°C = 정확히 5칸**(points 10/20).

```
30°C        36.5°C(정상)         42°C
[◆◆◆◆◆|◇◇◇◇◇]  ← 정상: 왼쪽 5칸 점등
 파랑 ───────── 중간 ───────── 빨강
```

- 앵커: `xLeft = guiWidth/2 - 91`, `y = guiHeight - leftHeight`, 그린 뒤 `leftHeight += 10`.
- points: `SurvivalData.temperaturePoints()` — 정상 이하는 0..10, 초과는 10..20.
- 스프라이트: `temp_empty` / `temp_full` / `temp_half` (마름모; half는 왼쪽 채움).

### 8.8 갈증 커스텀 아트 — **구현 완료** (T-07)

스프라이트:

```java
SPRITE_FULL  = RealismSurvival.id("hud/thirst_full")
SPRITE_HALF  = RealismSurvival.id("hud/thirst_half")
SPRITE_EMPTY = RealismSurvival.id("hud/thirst_empty")
```

- 세 스프라이트 모두 검은 외곽선과 불투명한 빈칸 배경을 공유한다.
- 우측→좌측으로 감소하는 HUD 방향에 맞춰 `thirst_half.png`는 **오른쪽 절반**이 차 있다.
- 채움색은 물방울의 청색 계열이며 별도 런타임 틴트를 적용하지 않는다.

---

## 9. 게임플레이 루프 (서버 로직)

파일: `event/SurvivalTickHandler.java` — `@EventBusSubscriber(modid = MODID)` (게임 버스, 양쪽 사이드에서 호출되지만 첫 줄 캐스팅으로 서버만 통과)

### 9.1 주기 평가 (`PlayerTickEvent.Post`)

```
if !(entity instanceof ServerPlayer) return
if tickCount % TICK_INTERVAL != 0 return
if creativeBypass && (creative || spectator) return
seconds = TICK_INTERVAL / 20
biome   = level.getBiome(player.blockPosition())
current = get(player); next = current
if thirstEnabled:      next = evaluateThirst(...)
if temperatureEnabled: next = evaluateTemperature(...)
if next != current: set(player, next)
```

### 9.2 갈증 (`evaluateThirst`) — 구현 완료

```
drain = biomeThirst(biome).map(p -> p.drainFor(seconds, sprinting))
                          .orElse(fallbackThirstPerSecond * seconds)
if data.isHyperthermic():
    drain *= hyperthermiaThirstMultiplier          // 기본 1.5
next  = data.addThirst(-drain)
# 수영/잠수 갈증 회복 (비 단독 제외; 생존 평가 틱당 절대값)
if player.isInWater():
    restore = distilled→+0.2 / fresh→+0.05 / stagnant·salt·unknown→0   // 평가 1회당
    if environment.submerged():
        restore *= swimThirstDiveMultiplier                  // 기본 1.5
    next = next.addThirst(restore)
if next.isDehydrated() && dehydrationDamage > 0:
    player.hurtServer(level, level.damageSources().source(ModDamageTypes.DEHYDRATION), dehydrationDamage)
```

등급은 발 위치 fluid의 `WaterQuality`(없으면 바이옴 분류). `tickInterval` 기본 20이면 대략 초당 증류 +0.2 / 민물 +0.05. 품질 디버프는 의도적 음용에만 적용.

T-10: `data/realismsurvival/damage_type/dehydration.json` + `death.attack.dehydration` 언어 키. 방어구·넉백 무시 태그 포함.

### 9.3 체온 (`TemperatureEvaluator`) — **구현 완료** (T-05)

파일: `environment/TemperatureEvaluator.java`. `SurvivalTickHandler`가 매 평가 주기마다 호출한다.

```
blend     = ClimateTemperature.blendNearby(pos, biomeTempBlendRadius)  // 기본 r=16, 4칸 스텝, 거리가중
base      = blend.vanillaBase                             // 주변 바이옴 baseTemperature 평균 (경계 급변 완화)
t         = base - max(0, y - seaLevel) / 600             // 바닐라 고도 강하 (ClimateTemperature)
climateC  = -5 + t × 25                                   // °C: 0→−5, 0.15→−1.25, 0.8→15, 2.0→45
ambient   = climateC
ambient  += dayNightTerm(level.getOverworldClockTime())   // 정오 0, 자정 -6°C (base≥1.5면 -12°C), 코사인
ambient  += isInWaterOrRain|powderSnow ? wetnessOffset : 0  // config 기본 -8
ambient  += headInWater ? submergedOffset : 0               // config 기본 -4 (잠수)
ambient  += isInLava ? +18 : (isOnFire ? +10 : 0)
ambient  += min(heatSourceMaxBonus, Σ nearbyHeatWeights)  // r=heatScanRadius, 용암/모닥불/화로/횃불 가중
ambient  += -min(coldSourceMaxPenalty, Σ nearbyColdWeights) // r=coldScanRadius, 얼음/눈/근처 물
ambient  += blend.seasonOffsetC                           // 동일 가중으로 season 오프셋도 블렌드
insulation = SurvivalDataManager.wornInsulation(player)   // T-06; 이벤트에서 덮어쓰기 가능
# 체온 = 환경 °C/h + 활동 대사열(가산)
if player.isInWater():
    env = -4.5 * (1 - insulation)                         // 냉수 침수 (기온 밴드 무시; ½-real × 1.5)
else:
    env = airBandRate(ambient)                            // 아래 표 (½-real × 1.5, 구간 보간)
    env *= (ambient ≥ 40 ? 1 - insulation*0.5 : 1 - insulation)  // env==0(쾌적)이면 그대로 0
activity = activityRate(resolveActivity(player), bodyTemp)  // |activity| ≤ 0.05 < 최소 환경 밴드 0.1125
rate = env + activity
next = temperature + rate * temperatureRateMultiplier * (seconds / 3600)
```

기온 밴드 → **환경** 변화율 (°C/h = 현실 midpoint의 **½ × 1.5**; 쾌적대 18–25는 **0** — 회복은 온열원·식음료·걷기 수렴):

| ambient (°C) | env (°C/h) |
|---|---|
| ≤ −10 | −1.6875 |
| −10 … 0 | −1.6875 … −0.5625 |
| 0 … 10 | −0.5625 |
| 10 … 15 | −0.1125 |
| 15 … 18 | −0.1125 … 0 |
| 18 … 25 | 0 |
| 25 … 32 | 0 … +0.1125 |
| 32 … 35 | +0.1125 |
| 35 … 40 | +0.1125 … +0.5625 |
| 40 … 45 | +0.5625 … +2.25 |
| ≥ 45 | +2.25 |

활동 항(|Δ| ≤ 0.05로 환경보다 약함; 우선순위 sprint &gt; swim &gt; walk &gt; idle — **동시에 하나만**):

| activity | °C/h |
|---|---|
| IDLE | 0 |
| WALK | `clamp((36.5 − body) × 0.05, ±0.05)` — 정상 체온 수렴 |
| SWIM | +0.04 |
| SPRINT | +0.05 |

#### 9.3.1 환경 × 활동 조합표 (`rate = env + activity`)

전제: `insulation = 0`, `temperatureRateMultiplier = 1.0`. 구간 보간이 있는 밴드는 **대표 ambient**의 env를 씀.  
WALK는 체온에 따라 부호·크기가 바뀌므로 열을 **캡 극단 3열**로 펼침 (정상 36.5에서 WALK=0 → IDLE과 동일).

| 환경 (대표 ambient) | env | + IDLE | + WALK (body 34 → +0.05) | + WALK (body 36.5 → 0) | + WALK (body 39 → −0.05) | + SWIM (+0.04) | + SPRINT (+0.05) |
|---|---:|---:|---:|---:|---:|---:|---:|
| 한파 ≤−10 (−10) | −1.688 | −1.688 | −1.638 | −1.688 | −1.738 | −1.648 | −1.638 |
| 동결 −10…0 (−5) | −1.125 | −1.125 | −1.075 | −1.125 | −1.175 | −1.085 | −1.075 |
| 추움 0…10 (5) | −0.562 | −0.562 | −0.512 | −0.562 | −0.612 | −0.522 | −0.512 |
| 쌀쌀 10…15 (12) | −0.113 | −0.113 | −0.063 | −0.113 | −0.163 | −0.073 | −0.063 |
| 서늘 15…18 (16.5) | −0.056 | −0.056 | −0.006 | −0.056 | −0.106 | −0.016 | −0.006 |
| 쾌적 18…25 (20) | 0 | 0 | +0.050 | 0 | −0.050 | +0.040 | +0.050 |
| 따뜻 25…32 (28.5) | +0.056 | +0.056 | +0.106 | +0.056 | +0.006 | +0.096 | +0.106 |
| 더움 32…35 (33) | +0.113 | +0.113 | +0.163 | +0.113 | +0.063 | +0.153 | +0.163 |
| 폭염 35…40 (37.5) | +0.338 | +0.338 | +0.388 | +0.338 | +0.288 | +0.378 | +0.388 |
| 극고온 40…45 (42.5) | +1.406 | +1.406 | +1.456 | +1.406 | +1.356 | +1.446 | +1.456 |
| 사막 ≥45 (45) | +2.250 | +2.250 | +2.300 | +2.250 | +2.200 | +2.290 | +2.300 |
| **냉수 침수** (`isInWater`) | −4.500 | −4.500 | −4.450 | −4.500 | −4.550 | −4.460 | −4.450 |

해상도 노트:

- 표 값은 **수식 조합** (`env + activity`). 실제 `resolveActivity`에서는 지상에서 SWIM이 나오지 않고, 물속 이동은 보통 SWIM(또는 SPRINT)이 WALK보다 우선한다.
- 단열 &gt; 0이면 음수 env는 ×(1−insulation), 고온(ambient≥40) env는 ×(1−insulation×0.5). 활동 항은 단열의 영향을 받지 않는다.
- 1시간 체온 변화 ≈ 표의 `rate` (°C). 예: 쾌적+질주 → +0.05°C/h, 침수+수영 → −4.46°C/h.

계측 HUD (§8): 온도계/시계/Serene Seasons 달력 손·Curios(belt) 시 좌하단 세로 스택에 기온·시간·날짜(`SeasonCalendar`). 온도계는 **환경 ambient**(seasonTerm 포함).

열원 가중(반경 내 합산, 상한 `heatSourceMaxBonus` 기본 12): 용암 8, 모닥불(LIT) 6, 불 5, 용암/마그마·점화 화로류 4, 횃불·랜턴 1.

냉원 가중(상한 `coldSourceMaxPenalty` 기본 10, ambient에 음수로 적용): 푸른 얼음 7, 단단한 얼음 6, 얼음/`graded_ice`/서리 얼음 5, 가루눈 4, 눈 블록 3, 눈 레이어 1, 열린 물 1.5(이미 물에 잠긴 동안은 물 셀 스킵 — 젖음 항과 이중 계산 방지). `#cold_sources` 태그 폴백 +2.

상태 패널티는 §9.6 (`SurvivalEffectsHandler`). 고체온→갈증 ×`hyperthermiaThirstMultiplier`는 §9.2.

### 9.4 음료 (`PlayerDrinkEvent` / use-tick) — 구현 완료

아이템 음용은 **마지막 use-tick**(`LivingEntityUseItemEvent.Tick`, duration==1)에서 `PlayerDrinkEvent`를 띄운 뒤 적용한다. 취소 시 use를 중단해 **스택을 소모하지 않는다.**  
`Finish`는 틱에서 이미 적용된 경우 재적용하지 않으며, 폴백 경로에서 취소되면 result 스택을 복구한다.  
월드 물·양동이 한 모금도 `applyDrink == false`면 사운드/쿨다운/채움 감소를 하지 않는다.

```
if !(entity instanceof ServerPlayer) return
drink(event.getItem().typeHolder()).ifPresent(d ->
    set(player, get(player).addThirst(d.thirstRestore()).addTemperature(d.temperatureDelta())))
```

물 직접 마시기 (T-04 ✅): `DrinkWaterHandler` — 빈손·Shift + `RightClickBlock`/`RightClickItem`, `ClipContext.Fluid.SOURCE_ONLY`. 갈증은 `drink/raw_water.json`(+10). 등급/바이옴 품질은 `PlayerDrinkEvent`로 §9.9 효과 적용. 쿨다운 20틱.

### 9.6 상태 패널티 (`SurvivalEffectsHandler`) — **구현 완료** (T-08)

매 생존 평가 직후 `SurvivalEffectsHandler.apply(player, level, data)` 호출. 효과는 `penaltyEffectDurationTicks`(기본 60)로 짧게 걸고 **조건이 유지되는 동안 재적용**한다. 회복되면 갱신이 멈춰 자연 만료.

| 조건 (기본 임계값, 전부 config) | 효과 |
|---|---|
| thirst ≤ 20 (그리고 > 0) | `WEAKNESS` I |
| thirst = 0 | `realismsurvival:dehydration` 피해 (`dehydrationDamage`, §9.2) |
| temp &lt; 35 | `SLOWNESS` I |
| temp &lt; 33 | + `MINING_FATIGUE` I |
| temp ≤ 31 | `SLOWNESS` II + `MINING_FATIGUE` II + `freeze` 피해 |
| temp &gt; 38.5 | `HUNGER` I + 갈증 ×1.5 |
| temp &gt; 39.5 | + `WEAKNESS` I |
| temp ≥ 41 | + `onFire` 피해 |

`penaltiesEnabled=false`면 디버프·극단 피해만 끄고, 갈증/체온 수치 자체는 계속 갱신된다.

### 9.7 자연 HP 회복 (`NaturalRegenHandler`) — **구현 완료**

허기 고갈·탈수·극단 체온 **피해는 그대로** 두고, 바닐라 음식/포화도 기반 **자연 회복만** 늦춘다. 이후 추가할 회복 아이템은 감속 대상이 아니다.

`LivingHealEvent`에서 `ServerPlayer` 치유를 가로채며:

| 판정 | 동작 |
|---|---|
| `REGENERATION` 효과 보유 | 통과 (금사과·재생 포션·향후 재생형 아이템) |
| heal 양 ≥ 4 | 통과 (`Instant Health` 및 대부분 전용 회복) |
| `NaturalRegenHandler.allowNextHeal(runnable)` | 통과 (커스텀 아이템용 API) |
| 그 외 (허기 `FoodData` 자연 회복) | 갈증 &lt; `naturalRegenMinThirst`(기본 40) 또는 저/고체온 구간이면 **취소**. 아니면 양 × `naturalRegenMultiplier`(기본 **0.25**) |

포화도 소모·허기 소모는 바닐라가 heal **이전**에 처리하므로, 배율만 낮추면 “식량은 쓰이는데 HP는 거의 안 찬다”가 되어 회복 아이템 의존도가 생긴다.

### 9.8 동일 컴포넌트 스택 (`ItemComponentModifiers`) — **구현 완료**

인벤토리 슬롯을 덜 쓰도록, 내용물/마법이 **같은** 아이템만 겹치도록 최대 스택을 올린다 (`ModifyDefaultComponentsEvent` → `DataComponents.MAX_STACK_SIZE`).

| 아이템 | 바닐라 | 모드 | 합쳐지는 조건 |
|---|---|---|---|
| `minecraft:potion` | 1 | **16** | 동일 `POTION_CONTENTS` (물병↔물병 OK, 물병↔신속 불가) |
| `minecraft:water_bucket` / `milk_bucket` | 1 | **1 (유지)** | 채워진 양동이는 바닐라처럼 스택 불가 |
| graded `*_water_bucket` / `*_ice_bucket` / `snow_bucket` | — | **1** | 동일 (빈 `minecraft:bucket`만 16) |
| graded `*_water_bottle` | — | **16** | 동일 등급 병 |
| `minecraft:enchanted_book` | 1 | **64** | 동일 `STORED_ENCHANTMENTS` (날카로움 III끼리 OK, 보호 III와는 불가) |
| `minecraft:honey_bottle` | 16 | 유지 | — |
| `minecraft:glass_bottle` / 빈 양동이 | 64 / 16 | 유지 | — |

투척/잔류 포션·생물 양동이는 손대지 않는다.  
**밸런스:** 예전처럼 물/우유 양동이를 16스택으로 올리면 증류·운반이 과도해져, 채워진 양동이는 전부 스택 1로 고정.

### 9.9 물 품질 1차 (`WaterQuality` 레지스트리) — **구현 완료**

물 등급은 **enum이 아니라** NeoForge 커스텀 레지스트리 `realismsurvival:water_quality` (`api.water.WaterQuality` / `ModWaterQualities`)다. 코어는 distilled / fresh / stagnant / salt 4종을 등록한다. **월드 유체는 공유 한 쌍** `graded_water` / `flowing_graded_water`이며, 등급은 `FluidState`·liquid block의 `QUALITY` 정수(0–15)로 구분한다. 애드온은 동일 레지스트리에 등급·bucket·bottle만 추가하면 된다(등급마다 fluid를 새로 등록하지 않음; 데이터팩만으로는 불가).

**불변 조건 — 열린 바닐라 물 금지:** 어떤 경로로도 월드에 **열린** `minecraft:water` / `Fluids.WATER` 액체 칸(`Blocks.WATER`)이 남아서는 안 된다. 월드젠·양동이·디스펜서·얼음 융해/파괴·동결 후처리 모두 graded water(+QUALITY)로 끝나야 한다. (워터로그는 엔진상 `Fluids.WATER` 고정 — 예외이며, 등급은 바이옴으로만 해석.)

월드에서 뜬 물은 바이옴으로 등급이 갈린다 (`WaterApi.resolveFromBiome` + `ResolveWaterQualityEvent`). **증류수만 안전 식수.** 민물은 응급용(약한 패널티), 오염된 물·바닷물은 사실상 비식용.

| 등급 | 획득 | 병 갈증 | 마실 때 효과 |
|---|---|---|---|
| **증류** `distilled` | 증류기만 생산 (화로 증류 경로 삭제) | **+40** (툴팁 물방울 4), 체온 **−0.4** | 디버프 없음 — 유일한 안전·고효율 식수 |
| **민물** `fresh` | **강** (`#minecraft:is_river`) · 양동이/필터 등 | **+20** (툴팁 물방울 2), 체온 **−0.25** | 멀미 5초 + 허기 I 8초 (흔하지만 약함 → 증류 유도) |
| **오염된 물** `stagnant` | 늪·맹그로브 태그 + **강이 아닌 내륙 담수**(연못·호수·함수층 등, 월드 변환 시) | **+20** (민물과 동일), 체온 **−0.2** | 멀미 15초 + 독 I 6초 + 허기 I 14초 |
| **바닷물** `salt` | 바다·해변 (`#realismsurvival:salt_water` = ocean·deep_ocean·beach + `stony_shore`) | **−14**, 체온 **−0.1** | 느림 I 12초 + 허기 I 15초 + 멀미 8초 |

**양동이 음용** (`GradedWaterBucketItem`):

| 조작 | 결과 |
|---|---|
| 블록 우클릭 | fill=3 설치 / fill&lt;3 내용 폐기→빈 양동이 |
| **허공에서 길게 우클릭** | 마시기 (병과 동일 drink duration) |
| 한 모금 | fill −1 (1→빈 양동이). 갈증·체온·상태이상 = 병 값 × **1** (모금 = 병 1병분). 가득 찬 양동이 3모금 ≈ 병 3개 |

부분 채움 표시: 아이템 **내구도형 게이지**(fill&lt;3만 표시). `채움: n/3` 툴팁 문구 없음. **플레이 확인 OK** — 별도 물 높이 3단 메시 아트는 하지 않음(게이지로 충분).

**아이템:** `*_water_bucket` / 얼음·눈 양동이 = 스택 **1** (바닐라 채움 양동이); `*_water_bottle` = 스택 **16** (`registry/ModItems`, `GradedWaterItems`).  
**유체**: `realismsurvival:graded_water` + `flowing_graded_water` (한 쌍). 레거시 블록 id(`distilled_water` 등)는 로드·`VanillaWaterConverter`가 canonical `graded_water`+QUALITY로 이전.  
`#minecraft:water` 태그 + `isWaterLike`. 무한 수원은 **같은 QUALITY** 원천 이웃 2+일 때만 (`GradedFlowingFluid#getNewLiquid`). **증류(`distilled`)는 무한 수원 불가**(증류기·루트 산물).  
바닐라 패리티: `supports_lily_pad` / `supports_frogspawn` / `bubble_column_can_occupy`에 graded 원천 포함, `supports_lily_pad` 블록에 `graded_ice`, 폭발 저항 100, `WaterFluid`와 동일 앰비언트/점적/소화 `entityInside`.  
**농지 수분** (`WaterHydration` + `FarmlandBlockMixin`):  
- 등급 유체: 증류·민물·**고인 물** 수분 가능. **바닷물만** 수분 불가 (`WaterHydration` 판정; `FluidType.canHydrate`는 스펀지/콘크리트용으로 true).  
- 워터로그/잔존 바닐라 물: **유체 칸 바이옴**으로 판정 (바다 해초 = 오염원, 강·연못 = 수분).  
- **혼재 규칙** (config `saltContaminatesIrrigation`, 기본 `false`):  
  - `false` = **수분 우선** — 범위 안 증류/민물/고인물이 있으면 수분 (바닷물이 옆에도 무시).  
  - `true` = **바닷물 오염** — 범위 안 바닷물(또는 바다/해변 바이옴 바닐라 물)이 있으면 수분 실패.  
- **기수 성장 패널티** (config `saltCoastGrowthMultiplier`, 기본 `0.75`): 수분 물과 바닷물이 **동시에** 범위에 있으면 농지는 촉촉(기본 혼재 규칙)하되 `CropGrowEvent` 성장 배수에 이 값을 곱함. `1.0`이면 패널티 없음.  
- 비는 바닐라대로 별도 수분. NeoForge water ticket 유지.  

**월드 변환 등급** (`VanillaWaterConverter.classifyNaturalWater`): salt → salt, 늪 태그 → stagnant, `#minecraft:is_river` → fresh, 그 외 바이옴이 fresh로 해석되면 **stagnant**(연못·호수). 이미 `WATER_CONVERTED`인 청크는 재분류하지 않음.

**월드 변환** (`VanillaWaterConverter` / `WaterApi` / `WATER_CONVERTED`):

| 경로 | 시점 | 역할 |
|---|---|---|
| **Primary** | 월드젠 `ChunkStatusTasks#initializeLight` (FEATURES·구조물 이후, 라이팅 이전) | ProtoChunk에서 `minecraft:water` → 등급 유체 (`getNoiseBiome` + `setBlockState` flags 0), 끝에서 `WATER_CONVERTED` |
| **Legacy** | 청크 로드 → 틱 지연 (`WaterWorldHandler`) | 미변환 청크만 1회 마이그레이션 후 스킵 |
| **Runtime** | 양동이 설치 · NeighborNotify · 얼음 파괴 · **디스펜서** (`WaterDispenseBehaviors`) | 월드젠 밖 바닐라 물 안전망 (`tryConvert`) |

블록 치환 직전: `ResolveWaterQualityEvent` → `ConvertVanillaWaterEvent`(취소 시 바닐라 유지 / 등급 변경).  
워터로그 유체 타입은 바닐라 고정 → 변환 대신 농지 판정으로 처리.  
양동이/채우기/조합은 기존과 동일. **정수 필터·증류기·화로 역할 조정**(증류수는 증류기만, 화로는 바닷물→소금)은 §15.5. 가마솥·희석·CA 확산은 더 뒤.

**등급 간 배치·경계** (동일 Fluid + QUALITY):  
- **양동이** `GradedWaterBucketItem` — `Fluid.ANY`로 조준. 흐름/원천 칸에 부으면 **공유 graded-water 원천 + 그 양동이 QUALITY**.  
- **퍼짐** — `Fluid.isSame`이 등급과 무관하게 true → 바닐라처럼 한 표면으로 흐름. QUALITY는 `getNewLiquid`이 가장 센 이웃에서 전파.  
  - 바닐라 물 흐름 칸만 이종 graded **원천**이 옆으로 교체 (`WaterFluidMixin` / `canDisplaceFlowingWaterLike`).  
  - 양동이 배치 직후 `FlowingFluid#tick`으로 즉시 spread.  
- **뜨기/마시기** — `WaterApi.fromFluidState` (QUALITY). 바닐라 물이면 바이옴.  
- **색** `WaterFluidTint` — QUALITY 이웃 Euclidean 공간평균(반경 ~10, 흐름 자유 / 원천 고가중). **Sodium**: `BlendedColorProvider` + 물-like에 smooth tint 강제.  
  - 바닐라 물·워터로그와의 옆면: `FluidRendererMixin` / `SodiumDefaultFluidRendererMixin` / `GradedLiquidBlock`  
- 워터로그는 바닐라 물 유지; 옆면 컬링·틴트로 이질감 완화.

**소형 민물 수면 부패** (`FreshWaterStagnator` · `FreshPoolDecayData` — **구현**):

| 항목 | 규칙 |
|---|---|
| **대상** | **민물(`fresh`) 원천**만. 증류·고인·바다는 부패 대상 아님 (증류는 이미 무한 수원 불가). |
| **판정** | 동일 QUALITY 민물 **원천**이 유체면으로 연결된 집합 크기(칸 수). 흐름 칸은 크기에 넣지 않음. |
| **임계값** | 연결 원천 **&lt; 64** (`freshPoolStagnateThreshold`) → 유예 후 **고인물(`stagnant`)**. **≥ 64** → 부패 없음. |
| **유예** | `grace = size × freshPoolGraceTicksPerSource` (기본 **24000틱/칸** = 칸당 1 마인크래프트 일). 풀이 클수록 더 오래 민물 유지. 시계는 칸별 first-seen 중 **최소값**(차원 SavedData). |
| **의도** | 작은 웅덩이/2×2는 방치 시 오염. 중형 이상 저수지·강은 민물 유지. 인공 플래그 불필요. |
| **config** | `freshPoolStagnationEnabled` · `freshPoolStagnateThreshold` · `freshPoolGraceTicksPerSource` · `freshPoolScanIntervalTicks` · `freshPoolChunksPerScan`. |
| **성능** | 간헐 스캔(플레이어 근처 청크 + 추적 칸) + BFS 상한(threshold). |
| **되돌림** | 부패 후 연결을 ≥64로 키워도 **자동 민물 복구 없음**. 복구는 양동이/필터·후속 현장 정화. |

**후속 콘텐츠 (부패 이후):** 작은 연못 **수질 관리용**으로 웅덩이에 직접 배치하는 필터/정화 도구(월드 고인→민물 유지·전환). 기존 슬롯 필터(고인 양동이→민물 병)와 역할 분리. 증류 대체 금지·유지비(연료/소모/주기) 필수. 상세는 부패 구현 후 별도 설계.

**미패치·설계 보류** (합의 전 코드 금지):

| 후보 | 요지 | 미결 / 우려 |
|---|---|---|
| 인접 중화·오염 | 맞닿은 등급이 섞여 민물화·오염 | **유체면이 직접 연결된 칸만** 허용해야 함. 틴트용 반경 샘플처럼 고체 한 칸 너머·한 칸 떨어진 물까지 전파되면 안 됨 |

인접 중화는 우선순위 밖. 소형 수면 부패·현장 정화와 별도로 합의 후에만 착수.

**등급 얼음** (`graded_ice` + `QUALITY`, §9.9 연장) — **구현**:

| 경로 | 동작 |
|---|---|
| 월드젠 / 청크 변환 | 바닐라 `minecraft:ice` → `graded_ice` + 바이옴 등급(`classifyNaturalWater`) |
| `freeze_top_layer` / `tickPrecipitation` | 동결 시 graded ice (유체 QUALITY 우선, 없으면 바이옴) |
| `Biome#shouldFreeze` | `Fluids.WATER`뿐 아니라 shared graded water도 동결 가능 |
| 융해·파괴(실크 없이 물 생성) | 동일 QUALITY의 graded water 수원 |
| packed_ice / blue_ice / frosted_ice | **1차 비대상** (바닐라 유지) |

**얼음 양동이** (담수만: distilled / fresh / stagnant — **salt 없음**):

| 항목 | 내용 |
|---|---|
| 조합 | `graded_ice`(QUALITY, 실크터치 시 복사) + 빈 양동이 → 해당 등급 얼음 양동이 (`ice_to_ice_bucket`) |
| 사용 | 블록: 같은 QUALITY 얼음 설치 · 허공 홀드: 마시기 → 빈 양동이 |
| drink | 병보다 낮은 갈증 + `temperature_delta` **−2°C** + 해당 `WaterQuality` 디버프 |

**스펀지 · 산호 · 콘크리트 가루** (`canHydrate` + 블록별 규칙) — **동결**:

| 대상 | 규칙 |
|---|---|
| **젖은 스펀지** | 바닐라 `minecraft:wet_sponge` **공유 블록** + `QUALITY`(얼음과 동일 패턴). 마른 스펀지는 하나. |
| **흡수 등급** | 흡수한 칸의 QUALITY를 집계 → **다수결**, 동률이면 `resolvePriority` 높은 쪽(salt > stagnant > fresh > distilled). |
| **드롭·스택** | 파괴 시 `copy_state`로 QUALITY 유지. `BLOCK_STATE` 성분이 다르면 스택 분리. 성분 없는 레거시/가디언 드롭 = **salt**. |
| **화로** | 젖은 스펀지 + 빈 양동이(연료 슬롯) → **기억한 QUALITY의 graded 물 양동이**(바닐라 `water_bucket` 아님). |
| **산호** (블록·식물·부채) | **바닷물(salt)만** 생존. 워터로그는 바이옴 등급으로 판정(강 워터로그 = 사망). |
| **콘크리트 가루** | 아무 graded / `#minecraft:water`로 경화 (`FluidType.canHydrate(true)`). |
| **농지** | 기존 `WaterHydration` 유지(바닷물 수분 불가). `canHydrate(true)`여도 농지 믹스인이 가로챔. |

### 9.10 병 보온 / 냉각 인챈트 — **구현 완료**

유리병(및 등급 물병)에만 붙는 상호 배타 인챈트. 인챈트 테이블·모루에서 부여.

| 인챈트 | 효과 | 시각 |
|---|---|---|
| `realismsurvival:warming` (보온) | 마셔서 체온 **+0.75°C** (정상 수렴) | 액체 틴트가 붉은 쪽으로 치우침 |
| `realismsurvival:cooling` (냉각) | 마셔서 체온 **−0.75°C** (정상 수렴) | 액체 틴트가 푸른 쪽으로 치우침 |

- `exclusive_set`: `#realismsurvival:exclusive_set/bottle_temperature` (둘 중 하나만)
- `supported_items`: `#realismsurvival:enchantable/temperature_bottle` (유리병 + 등급 물병)
- 인챈트 테이블 비용: `min_cost` 1 / `max_cost` 41 (책장 0에서도 후보가 나오도록 aqua_affinity와 동일 대역)
- 빈 유리병에 건 뒤 물을 뜨거나(양동이+병 조합) 하면 인챈트가 **채워진 병으로 복사**
- 바닐라 글린트 색 커스텀 API는 26.1에 없어, 액체색 시프트로 보온/냉각을 구분 (글린트 PNG 에셋은 `textures/misc/enchanted_glint_{warming,cooling}.png`로 준비만 해 둠)
- 병/양동이 툴팁 효과 줄은 바닐라 포션 형식 (`PotionContents.addPotionTooltip`, 예: `멀미 (0:15)`)

### 9.11 Curios 악세서리 슬롯 — **슬롯 + 계측 벨트 아이템**

코어 계약(갈증·체온) 밖이다. 시중 악세서리 모드와 맞추기 위해 [Curios API](https://www.curseforge.com/minecraft/mc-mods/curios)에 슬롯을 선등록한다. Curios 없으면 본 모드만으로도 기동 (`type=optional`) — 계측 HUD는 **손 소지**만으로도 동작.

| 슬롯 ID | 표시 (ko) | 데이터 |
|---|---|---|
| `earrings` | 귀걸이 | `data/realismsurvival/curios/slots/earrings.json` |
| `glasses` | 안경 | `…/glasses.json` |
| `necklace` | 목걸이 | `…/necklace.json` (Curios 프리셋과 병합) |
| `bracelet` | 팔찌 | `…/bracelet.json` |
| `gloves` | 장갑 | `…/gloves.json` |
| `belt` | 벨트 | `…/belt.json` |

- 플레이어 부여: `data/realismsurvival/curios/entities/player.json`
- `#curios:belt`: `thermometer`, `sereneseasons:calendar` (optional), `minecraft:clock` (§8.5.1)
- 로컬라이즈: `curios.identifier.<id>` (`en_us` / `ko_kr`)
- 개발: `localRuntime`으로 Curios `15.0.0+26.1.2` (`-Pcurios=false`로 제외 가능)

계절: `SeasonCalendar` 365일/4계절 + `climate_*` 바이옴 태그 × `SeasonClimate` (기온·강수·작물). Serene Seasons soft-compat(리플렉션) — 있으면 계절 권위·작물 시즌은 SS, precip mixin 비활성.

### 9.5 작물 성장 · 일조량 — **구현 완료** (T-03)

`CropGrowthHandler`가 `CropGrowEvent.Pre`에서 배수를 곱해 적용한다.

```
combined = biome × light × season × rainMoisture × saltCoast(기본 1.0; 담수+바닷물 공존 시 config)
≤0 → DO_NOT_GROW
(0,1) → 확률적으로 DO_NOT_GROW
1 → DEFAULT (바닐라)
>1 → (mult−1) 확률로 GROW 강제
```

**일조 메트릭 (`CropLightProfile.LightMetric`)**

| metric | 값 | 용도 |
|---|---|---|
| `sun` | `skyLight − skyDarken` (0 하한) | 햇빛이 필요한 작물. 지하 횃불 농장·밤에는 성장 둔화 |
| `sky` | 스카이 라이트만 (시간 무시) | 그늘 작물. 열린 하늘(높은 sky)을 싫어함 |
| `block` | 블록 라이트만 | (확장용) |
| `max` | `getRawBrightness` | 바닐라에 가까운 합성 광량 |

**내장 분류**

| 프로필 | 작물 | metric | 요지 |
|---|---|---|---|
| `sun_crops` | `#minecraft:crops` + 스위트 베리 | `sun` | 0–5 정지 · 6–8 ×0.35 · 9–11 ×0.7 · 12–13 ×1 · 14–15 ×1.3 |
| `shade_crops` | 버섯·동굴덩굴·코코아 | `sky` | 0–7 ×1.25 · 8–11 ×0.55 · 12–15 정지 |

네더 사마귀 등은 프로필 밖 → 일조 보정 없음 (바이옴 보정만).

---

## 10. 클라이언트 / 서버 물리적 분리 수칙

| 규칙 | 근거 |
|---|---|
| `@Mod(MODID)` 공용 클래스에서는 `net.minecraft.client.*`, `com.mojang.blaze3d.*`를 **import조차 하지 않는다.** | 전용 서버 클래스로딩 시 `NoClassDefFoundError` |
| 클라 전용 코드는 `client` 패키지 + `@Mod(value = MODID, dist = Dist.CLIENT)` 클래스에서만 참조 | FML이 서버에서 그 `@Mod` 클래스를 아예 로드하지 않음 |
| **`@OnlyIn` / `@OnlyIns`를 모드 코드에 쓰지 않는다** (아래 §10.1) | NeoForge 26.1+ 런타임 스트리핑 없음 → 로드 경고 |
| 게임 버스 핸들러는 첫 줄에서 `instanceof ServerPlayer` / `instanceof ServerLevel`로 분기 | `PlayerTickEvent`는 양쪽에서 발화 |
| 수치 변경(`setData`)은 서버만. `SurvivalAttachments.set`의 사이드 체크 유지 | P1 |
| 데이터팩 맵(`SurvivalDataManager`)은 서버 전용. 클라가 필요하면 §7.6 페이로드 | 싱글플레이(통합 서버)에서는 우연히 동작하므로 **반드시 전용 서버로 테스트** |
| `Minecraft.getInstance()`는 `client` 패키지 밖에서 호출 금지 | |

### 10.1 `@OnlyIn` 금지 (재발 방지)

NeoForge 26.1에서는 `@OnlyIn(Dist.CLIENT)`의 **런타임 멤버 스트리핑이 없다.** 모드 jar에 어노테이션이 하나라도 있으면 구동 시 경고가 뜬다:

> The mod realismsurvival uses the @OnlyIn annotation; the runtime member-stripping behaviour of this annotation is no longer present…

| 하지 말 것 | 대신 |
|---|---|
| `client` 패키지 클래스에 `@OnlyIn(Dist.CLIENT)` (바닐라 `ClientTooltipComponent` 등을 따라 붙이는 것 포함) | 패키지 위치 + `RealismSurvivalClient` / `@EventBusSubscriber(... value = Dist.CLIENT)` 로만 격리 |
| 공용(`event`, `water`, …) 클래스에 `@OnlyIn`으로 클라 전용 메서드 표시 | 클라 코드를 `client`로 옮기거나, 서버 핸들러는 `ServerPlayer` 캐스팅으로 분기 |
| `import net.neoforged.api.distmarker.OnlyIn` | 사용처 삭제. `Dist`는 `@Mod(dist=…)` / `@EventBusSubscriber(value=…)` 에만 |

검증: 워크스페이스 `src/**/*.java`에서 `OnlyIn` 검색 시 **히트 0**. `runClient` 부팅 시 “모드 구동중 경고”에 `@OnlyIn` 문구 없음.

---

## 11. 구현 티켓 (작업 지시서)

각 티켓은 **독립적으로 PR 가능**해야 하며, 완료 조건(AC)을 모두 만족해야 한다. 순서는 의존성 순.

### 11.0 티켓 스코어보드 (2026-10-15)

| 상태 | 티켓 |
|---|---|
| ✅ 완료 | T-01, T-02, T-03, T-04, T-05, T-06, T-07, T-08, T-09, T-10, T-11, T-13 |
| ✅* 핵심 완료·잔여 | T-12 (`armor_tint` 데이터팩 미도입, 재료 그룹은 코드 상수) |
| ✅ 완료 | **W-01 ~ W-05** (§15.5) |
| ❌ 미구현 (선택) | **W-04** GameTest (§15.5.1) |
| 티켓 밖 완료 | §9.9 물 품질, §9.10 병 인챈트, §9.11 Curios 슬롯(아이템 없음), §15.4-1 이벤트·태그 |

| ID | 제목 | 파일 | 완료 조건(AC) |
|---|---|---|---|
| **T-01** ✅ | 디버그 명령 `/realism get|set thirst|temperature <value>` | `command/RealismCommands.java` | GAMEMASTERS. `set` 후 HUD 즉시 갱신. `get`은 JSON + ambient °C |
| **T-02** ✅ | 리스폰 정책 | `event/SurvivalLifecycleHandler.java` | `respawnResetOnDeath`(기본 true)면 사망 리스폰 시 thirst=60·temp=36.5. `false`면 copyOnDeath 유지. 엔드 귀환(`isEndConquered`)은 항상 유지 |
| **T-03** ✅ | 작물 성장·일조 | `CropGrowthHandler`, `crop_growth/*`, `crop_light/*` | §9.5. 바이옴×일조 배수. 햇볕 작물은 지하 횃불만으로 풀성장 불가 |
| **T-04** ✅ | 물 블록 직접 마시기 | `event/DrinkWaterHandler.java`, `drink/raw_water.json` | 빈손 Shift+우클릭 원천 → +10, 쿨다운 20틱, `GENERIC_DRINK`. 등급·바이옴 품질 효과 적용 |
| **T-05** ✅ | 체온 °C/h 구간표 | `environment/TemperatureEvaluator.java` | §9.3. 쾌적대 유지·한파/폭염/냉수 침수에서 체온 드리프트 |
| **T-06** ✅ | 방어구 단열 데이터팩 | `data/InsulationDefinition.java`, `insulation/leather.json`·`diamond.json` | 가죽 조각당 0.1(풀셋 0.4), 다이아 0.025(풀셋 0.1). `SurvivalDataManager.wornInsulation` → evaluator → `TemperatureCalculateEvent` |
| **T-07** ✅ | 갈증 커스텀 HUD 스프라이트 | `client/RealismSurvivalHUD`, PNG 3장 | §8.8. right-fill half + 검정 외곽선 |
| **T-12** ✅* | HP 재료색 테두리 방어력 + `ARMOR_LEVEL` 제거 | `client/ArmorHeartOverlay.java`, 테두리 PNG | §8.6. *재료 그룹은 코드 상수; `armor_tint` JSON은 잔여 |
| **T-13** ✅ | 체온 스펙트럼 HUD (HP 위) | `client/TemperatureHudLayer.java`, `temp_empty/full` | §8.7. 30~42°C → 0~79px 연속 채움 |
| **T-08** ✅ | 패널티 효과 | `event/SurvivalEffectsHandler.java` | §9.6. 단계별 디버프 + freeze/onFire. 임계값·피해량 config |
| **T-09** ✅ | 데이터팩 → 클라 동기화 | `network/SyncSurvivalDataPayload`, `SurvivalDataSync`, `client/ClientSurvivalData`, `DrinkTooltipHandler` | §7.6. 전용 서버 클라에서 포션 툴팁 **갈증: +8**, 증류수병 **+40** (물방울 4) |
| **T-10** ✅ | 전용 데미지 타입 | `api/ModDamageTypes`, `damage_type/dehydration.json`, lang | 사망 메시지 "X은(는) 탈수로 사망했습니다" |
| **T-11** ✅ | GameTest | `gametest/SurvivalGameTests.java`, `test_instance/*.json` | thirst_drain · drink_restore · respawn_reset · drink_cancel · temperature_converge · water_quality_resolve · water_quality_state · penalty_hypothermia · **cauldron_quality_io** · **bucket_fish_capture** · **axolotl_feed_leftover** · **dispenser_water_bottle** · **distilled_brewing** · **legacy_water_item_rewrite** · **distilled_no_infinite_source** · **graded_water_vanilla_tags** · **fresh_pool_stagnation**. `gradlew runGameTestServer` (Curios·FD는 GameTest 시 자동 제외) |
| **W-01** ✅ | 정수 필터 (슬롯 기계) | §15.5.1 | 고인 양동이+유리병+카트리지→민물 병, 카트리지 내구 소모, 10초/병 |
| **W-02** ✅ | 증류기 + 소금 | §15.5.1 | 연료+물양동이+빈용기→증류(+소금), 20초/병 |
| **W-03** ✅ | 화로: 증류 제거·바다→소금×2 | §15.5.1 | 증류 제련 삭제, 바다 양동이→소금 2(양동 소모) |
| **W-05** ✅ | 양동이 채움 1/3·2/3·가득 | §15.5.1 | fill + 병1~3↔양동이 역조합(동종·비넘침) |

공통 AC:
* `gradlew build` 경고 없이 성공.
* 신규 밸런싱 숫자는 데이터팩 또는 config에만 존재.
* 신규 JSON은 §7.3 문법 준수, 전용 서버 로그에 `Loaded N '<type>' data entries`로 개수 확인.
* 사이드 규칙 §10 위반 없음 (전용 서버 `runServer` 부팅 확인).

---

## 12. 검증 절차와 현재 검증 상태

### 12.1 현재 검증된 것

| 검증 | 결과 | 시점 |
|---|---|---|
| `gradlew build` / `compileJava` | **성공** (Java 25, **82** Java 소스) | 2026-10-15 |
| T-09 클라 동기화 | `SyncSurvivalDataPayload` + `ClientSurvivalData` + drink 툴팁 | 2026-10-15 |
| T-10 / T-04 / T-02 | dehydration 데미지 · 월드 물 마시기 · 리스폰 리셋 | 2026-10-15 |
| T-11 GameTest | `runGameTestServer` — 12 required RS tests (+ cauldron/fish/axolotl/dispenser bottle) | 2026-10-17 |
| Modrinth Sodium/Iris | `mc26.1.1-0.8.9-neoforge` / `1.10.9+26.1-neoforge` 해석 성공 | 2026-10-14 |
| Curios `localRuntime` | `curios-neoforge:15.0.0+26.1.2` 해석 성공 | 2026-10-15 |
| `gradlew runServer` | 전용 서버 부팅 성공. Realism Survival + Sodium + Iris 공존 | 2026-10-14 |
| 어태치먼트 / 데이터팩 | `survival_data` 등록; thirst 3 / crop_growth 2 / crop_light 2 / drink 20 | 2026-10-14 |
| `runClient` + HUD | 갈증·방어 테두리·체온 바 스크린샷 검증 (Sodium/Iris) | 2026-10-14 |
| 체온↔환경 (T-05) | `/realism get` ambient, 사막·설원·모닥불 수동 확인 | 2026-10-14 |
| 물 품질 (§9.9) | 월드 변환·뜨기·농지 수분·색 블렌드·Sodium 단층 수정 반복 검증 | 2026-10-15 |
| 물 품질 재검증 | 공유 fluid+QUALITY, 강=민/내륙 담수=고인, 고인 농지 수분, 양동이 fill 게이지 — 플레이 확인 OK | 2026-10-17 |
| Curios 슬롯 (§9.11) | 데이터팩·optional dep·빈 태그 등록 (아이템 없음 → 슬롯 비어 있음) | 2026-10-15 |

### 12.4 미구현·후속 백로그 (코어 API 우선)

갈증·체온 **코어 계약**을 먼저 닫고, 콘텐츠는 뒤로 민다 (§1·§15).

| 우선 | 항목 | 레이어 | 비고 |
|---|---|---|---|
| ✅ | 코어 이벤트 + 열원/음용 태그 | 코어 API | §15.4-1 완료 |
| ✅ | **T-06** 단열 데이터팩 | 코어 | leather/diamond 내장 |
| ✅ | **T-09** 데이터팩→클라 동기화 | 코어 | 툴팁·`ClientSurvivalData` |
| ✅ | **T-10** 탈수 데미지 타입 | 코어 | `dehydration` + ko 사망 메시지 |
| ✅ | **T-04** 월드 물 마시기 | 코어+물 | `raw_water` + 품질 효과 |
| ✅ | **T-02** 리스폰 정책 | 코어 | `respawnResetOnDeath` config |
| ✅ | **T-11** GameTest | 코어 | thirst/drink/respawn + cancel/temp/water/penalty |
| ✅ | **W-01** 정수 필터 (슬롯) | 물 콘텐츠 | §15.5 — 고인 양동이→민물 병; `filter_cartridge` 필수·내구 |
| ✅ | **W-02** 증류기 + 소금 | 물 콘텐츠 | §15.5 — 증류 유일 경로 |
| ✅ | **W-03** 화로 역할 조정 | 물 콘텐츠 | 증류수 제련 삭제, 바다→소금 유지 |
| ✅ | **W-05** 양동이 채움 단계 | 물 콘텐츠 | 1/3·2/3·가득 + 병 조합 |
| — | `api` 패키지 정리 / 향후 api jar | 구조 | §15.2 |
| ✅ | 가마솥 QUALITY·점적·월드젠 증류 | 물 콘텐츠 | 담기/회수 동일 등급; 점적=원천(미등급→민물); 구조물 가마솥·자연 루트 물=증류(`trial_chambers/water`·`chamber` 포함). 레거시 `water_bucket`/물 포션은 인벤·증류기에서 graded로 재작성. **희석·CA는 후속** |
| — | 가마솥 희석·현장 CA 정화 | 물 콘텐츠 | QUALITY I/O 이후 |
| ✅ | 소형 민물 수면 부패 (연결 &lt; 64, 유예∝크기) | 물 콘텐츠 | `FreshWaterStagnator` · GameTest `fresh_pool_stagnation` |
| — | 웅덩이 배치형 현장 정화 (소형 연못 수질) | 물 콘텐츠 | 부패 이후 후속. 슬롯 필터와 역할 분리 |
| ⏳ | Create soft-compat (유체·자동화 연동만) | 물 콘텐츠 | Create **26.1 jar 대기**. 연동 표면은 준비됨(아래 FluidHandler). 레시피 변경 없음 |
| ✅ | Large Fluid Tank + Pipez localRuntime (Create 대체) | 물 콘텐츠 | `fluidtank`/`pipez` optional. QUALITY 보존은 인게임 검증 |
| ✅ | 애드온 API 동결 문서 + 샘플 | 코어 API | `docs/ADDON_API.md` · `GradedWaterItems` · `examples/rs-api-sample` |
| ✅ | 필터/증류기 FluidHandler (트랙 B 유체 주입) | 물 콘텐츠 | `Capabilities.Fluid.BLOCK` + QUALITY 성분. GameTest `machine_fluid_input`. 호퍼 트랙과 병행 |
| — | 등급 인접 중화·오염 | 물 콘텐츠 | §9.9 보류 — 합의 전 구현 금지 |
| — | Curios 악세서리 **아이템** | 콘텐츠 | §9.11 |
| — | T-12 `armor_tint` JSON | HUD 잔여 | 선택 |

### 12.2 수동 검증 체크리스트 (`gradlew runClient`)

1. 메인 메뉴 도달 → 로그에 `Iris`/`Sodium` 초기화, `[RealismSurvival] Client setup complete`.
2. 새 월드(서바이벌) 생성 → 우측 갈증 FULL, 좌측 HP 위 체온 바가 중간(≈36.5°C) 근처.
3. `/realism set temperature 30` → 바 왼쪽(파랑) + 저체온 디버프(느림·채굴피로)·동상 피해. `42` → 빨강 + 허기/나약 + 화상 피해.
4. `/realism set thirst 15` → Weakness. `0` → 탈수 피해.
5. 사막 정오(`/tp`) 2~3분 → 체온 상승·갈증 감소. 설원/물 → 하강. 모닥불 → 상승.
6. 물병(`/give @s potion`) 마시기 → 갈증 6칸(30pt) 회복.
7. `/kill @s` → 리스폰 후 갈증≈60·체온≈36.5(`respawnResetOnDeath=true`). config false면 유지. 월드 재입장 → 디스크 유지.
8. Iris 셰이더 + `F1`/크리에이티브에서 HUD 숨김 정상.
9. `/reload` → `Loaded N ... data entries` 재출력.

### 12.3 로그 필터 (PowerShell)

```powershell
rg -n "\[RealismSurvival\]|SimpleJsonResourceReloadListener.*realismsurvival|ERROR" run\logs\latest.log
```

---

## 13. 밸런싱 기본값 테이블 (내장 데이터팩)

| 파일 | 대상 | 값 | priority |
|---|---|---|---|
| `biome_thirst/overworld_default.json` | `#minecraft:is_overworld` | 0.05/s, 달리기 ×1.5 (≈ 33분에 100→0) | -100 |
| `biome_thirst/arid.json` | 사막 ∪ `#is_badlands` ∪ `#is_savanna` | 0.15/s, 달리기 ×2.0 (≈ 11분) | 10 |
| `biome_thirst/nether.json` | `#minecraft:is_nether` | 0.30/s, 달리기 ×2.0 (≈ 5.5분) | 20 |
| `crop_growth/arid_penalty.json` | 사막 ∪ `#is_badlands`, 모든 작물 | ×0.5 | 10 |
| `crop_growth/jungle_bonus.json` | `#is_jungle`, 밀/당근/감자/비트/멜론·호박 줄기 | ×1.35 | 10 |
| `crop_light/sun_crops.json` | `#minecraft:crops` + 스위트 베리 | `sun` 밴드 (§9.5) | 0 |
| `crop_light/shade_crops.json` | 버섯·동굴덩굴·코코아 | `sky` 밴드 (§9.5) | 0 |
| `drink/potion.json` | `minecraft:potion` (물병 제외, 코드에서 민물 처리) | +8 | 0 |
| `drink/distilled_water_bottle.json` | 증류수 병 | **+40** (물방울 4, 효과 없음) | 10 |
| `drink/fresh_water_bottle.json` | 민물 병 | **+20** (물방울 2; +멀미 5초 + 허기 8초) | 10 |
| `drink/stagnant_water_bottle.json` | 오염된 물 병 | **+20** (민물과 동일 갈증; 멀미+독+허기) | 10 |
| `drink/salt_water_bottle.json` | 바닷물 병 | **−14** (느림I+허기I+멀미) | 10 |
| `drink/milk_bucket.json` | 우유 | +12, 체온 **−0.15** | 0 |
| `drink/honey_bottle.json` | 꿀병 | +6, 체온 **+0.1** | 0 |
| `drink/soups_stews.json` | 머쉬룸/비트/토끼/수상한 스튜 | +12, 체온 **+0.35** | 0 |
| `drink/golden_treats.json` | 금사과 / 마법이 부여된 금사과 | +8 | 0 |
| `drink/juicy_food.json` | 수박·베리·사과·후렴과 | +6, 체온 **−0.15** | 0 |
| `drink/golden_carrot.json` | 황금 당근 | +4 | 0 |
| `drink/fresh_produce.json` | 당근·비트·감자 | +3 | 0 |
| `drink/baked_potato.json` | 구운 감자 | **−2**, 체온 **+0.2** | 0 |
| `drink/pumpkin_pie.json` | 호박 파이 | **−2**, 체온 **+0.1** | 0 |
| `drink/dry_starch.json` | 빵·쿠키 | **−3** | 0 |
| `drink/raw_protein.json` | 생고기 | **−2** | 0 |
| `drink/raw_fish.json` | 생선(생) | **−1**, 체온 **−0.05** | 0 |
| `drink/cooked_protein.json` | 익힌 고기·생선 | **−3.5**, 체온 **+0.2** | 0 |
| `drink/hazardous_food.json` | 썩은살·거미눈·복어·독감자 | **−5** | 0 |
| `drink/dried_kelp.json` | 말린 켈프 | **−6** | 0 |
| `drink/raw_water.json` | 월드 물 원천 마시기 (items=`air`, id 조회로 사용) | **+6** | 0 |
| `drink/*_ice_bucket.json` | 얼음 양동이 (병과 동일 갈증 스케일) | 증류 +40 / 민물·오염 +20, 체온 **−0.9** | 10 |
| `insulation/leather.json` | 가죽 투구·흉갑·레깅스·부츠 | 조각당 **0.1** (풀셋 0.4) | 0 |
| `insulation/diamond.json` | 다이아 투구·흉갑·레깅스·부츠 | 조각당 **0.025** (풀셋 0.1) | 0 |

바닐라 `Foods` 식용 아이템은 위 표로 전수 커버. 체온은 뜨겁거나 차가운 음식이 분명할 때만 부여하고, 나머지는 갈증만. 케이크(블록)는 `LivingEntityUseItemEvent` 밖이라 미포함. 서드파티는 같은 `drink/` 경로로 추가.

#### Farmer's Delight (`mod_loaded` · `drink/fd_*.json`, priority 5)

컨셉별 분리. 바닐라 밴드에 맞춤(수프=+12/+0.35, 익힌 단백질=−3.5/+0.2, 건조 전분=−3 등). `dog_food`·블록 피스트는 제외.

| 파일 | 컨셉 | 갈증 | 체온 |
|---|---|---|---|
| `fd_hot_drinks` | 핫 코코아·사과 사이다 | +14 | **+0.35** |
| `fd_cold_drinks` | 수박 주스 | +16 | **−0.35** |
| `fd_milk_bottle` | 우유병 | +10 | **−0.15** |
| `fd_soups` | 스튜·수프·육수 (볶음밥 제외) | +12 | **+0.35** |
| `fd_warm_meals` | 밥·파스타·만두·라тату이 등 촉촉한 온기 식사 | +6 | **+0.25** |
| `fd_hot_platters` | 구이·로스트·바비큐·프라이드 에그 | **−2** | **+0.25** |
| `fd_sandwiches` | 샌드위치·랩 | +2 | 0 |
| `fd_sushi` | 초밥·김밥 롤 | +4 | **−0.1** |
| `fd_salads` | 과일·믹스·글림 샐러드 | +8 | **−0.2** |
| `fd_nether_salad` | 네더 샐러드 (온기·저수분) | +3 | **+0.1** |
| `fd_popsicle` | 수박 아이스 | +10 | **−0.5** |
| `fd_produce` | 토마토·양배추·양파·호박 조각 | +3 | 0 |
| `fd_custard` | 글로우베리 커스터드·베리 치즈케이크 | +5 | **−0.1** |
| `fd_desserts` | 파이·케이크 조각 | **−2** | **+0.1** |
| `fd_cookies` | 쿠키 | **−3** | 0 |
| `fd_dry` | 반죽·생면·파이크러스트·생쌀 | **−3** | 0 |
| `fd_sauce` | 토마토 소스 (액체) | +5 | 0 |
| `fd_raw_protein` | 생 고기 컷·햄 | **−2** | 0 |
| `fd_raw_fish` | 생 생선 슬라이스 | **−1** | **−0.05** |
| `fd_cooked_protein` | 익힌 컷·패티·훈제햄·익힌 생선 | **−3.5** | **+0.2** |

#### Aquaculture 2 (`mod_loaded` · `drink/aqua_*.json`, priority 5)

| 파일 | 컨셉 | 갈증 | 체온 |
|---|---|---|---|
| `aqua_raw_fish` | 생 생선·생 필렛 (금붕어 제외) | **−1** | **−0.05** |
| `aqua_cooked_protein` | 익힌 생선 필렛 | **−3.5** | **+0.2** |
| `aqua_sushi` | 초밥 | +4 | **−0.1** |
| `aqua_soup` | 거북이 수프 | +12 | **+0.35** |
| `aqua_algae` | 조류 | **−6** | 0 |

한글(우회): 업스트림 `aquaculture:lang/ko_kr.json`이 JSON 파싱 오류로 **네임스페이스 전체 스킵**되므로, Aqua 번역 키를 본 모드 [`assets/realismsurvival/lang/ko_kr.json`](src/main/resources/assets/realismsurvival/lang/ko_kr.json)에 실어 로드한다(동일 경로 오버레이는 깨진 파일이 스택을 끊으면 무효). 업스트림 수정 시 이전 가능.

#### Fish of Thieves (`mod_loaded` · `drink/fot_*.json`, priority 5)

| 파일 | 컨셉 | 갈증 | 체온 |
|---|---|---|---|
| `fot_raw_fish` | 생 thieves fish 10종 | **−1** | **−0.05** |
| `fot_cooked_protein` | 익힌 thieves fish 10종 | **−3.5** | **+0.2** |
| `fot_juicy` | 파인애플·망고·코코넛·가디언 프루트 | +6 | **−0.15** |
| `fot_produce` | 바나나·석류·덜 익은 망고 | +3 | 0 |
| `fot_worms` | 지렁이·구더기·거머리 | **−5** | 0 |

서버 config (`<world>/serverconfig/realismsurvival-server.toml`) 기본값:

| 키 | 기본 | 역할 |
|---|---|---|
| `thirstEnabled` / `temperatureEnabled` | true | 마스터 스위치 |
| `tickInterval` | 20 | 평가 주기(틱) |
| `fallbackThirstPerSecond` | 0.05 | 바이옴 미매칭 시 갈증 |
| `dehydrationDamage` | 1.0 | 탈수 피해 |
| `hyperthermiaThirstMultiplier` | 1.5 | 고체온 시 갈증 배수 |
| `temperatureRateMultiplier` | 1.0 | °C/h 구간표 전역 배율 (쾌적대 0은 유지) |
| `temperatureConvergenceRate` / `temperatureAmbientFactor` | (deprecated) | 무시 — 구 서버 config 호환용 |
| `temperatureWetnessOffset` | -8.0 | 물/비 ambient 보정 |
| `biomeTempBlendRadius` | 16 | 바이옴 기온·계절 오프셋 수평 블렌드 반경(0=발밑만, 4칸 스텝) |
| `heatScanRadius` / `heatSourceMaxBonus` | 4 / 12 | 열원 스캔 |
| `coldScanRadius` / `coldSourceMaxPenalty` | 4 / 10 | 냉원(얼음·눈·근처 물) 스캔 |
| `temperatureSubmergedOffset` | −4 | 머리 수중 추가 냉각 |
| `swimThirstDiveMultiplier` | 1.5 | 잠수 시 침수 갈증 회복 배수 (증류 +0.2 / 민물 +0.05 / 그 외 0, 평가 틱당) |
| `swimThirstRestorePerSecond` | (deprecated) | 무시 — 구 서버 config 호환용 |
| `creativeBypass` | true | 크리에이티브/관전 스킵 |
| `respawnResetOnDeath` | true | 사망 리스폰 시 갈증/체온 리셋 (false=copyOnDeath 유지) |
| `respawnThirst` / `respawnTemperature` | 60 / 36.5 | 사망 리스폰 목표값 |
| `saltContaminatesIrrigation` | false | `false`=증류/민물/고인물 수분, `true`=바닷물이 범위 안이면 수분 실패 (§9.9) |
| `saltCoastGrowthMultiplier` | 0.75 | 수분 물+바닷물 공존 시 작물 성장 배수 (농지 건조와 별개, §9.9) |
| `penaltiesEnabled` | true | 디버프·극단 피해 마스터 |
| `penaltyEffectDurationTicks` | 60 | 디버프 갱신 지속시간 |
| `thirstDebuffThreshold` | 20 | 이하 시 Weakness |
| `hypothermiaThreshold` / `Moderate` / `Severe` | 35 / 33 / 31 | 저체온 단계 |
| `hyperthermiaThreshold` / `Moderate` / `Severe` | 38.5 / 39.5 / 41 | 고체온 단계 |
| `freezeDamage` / `heatDamage` | 1.0 / 1.0 | 극단 피해(하프하트) |
| `slowNaturalRegen` | true | 자연 HP 회복 감속 스위치 |
| `naturalRegenMultiplier` | 0.25 | 자연 회복량 배율 (0=완전 차단) |
| `naturalRegenMinThirst` | 40 | 이 갈증 미만이면 자연 회복 불가 |

---

## 14. 코딩 컨벤션

* **Java 25 문법 적극 사용**: `record`, 패턴 매칭 `instanceof`(`if (!(e.getEntity() instanceof ServerPlayer player)) return;`), `switch` 패턴, `var`(지역 변수, 타입이 우변에서 명백할 때만).
* 클래스는 가능하면 `final`, 유틸 클래스는 private 생성자.
* 상수는 `static final` + 의미 있는 이름. 숫자 리터럴이 두 번 이상 등장하면 상수화.
* 이벤트 핸들러는 `static`, 클래스에 `@EventBusSubscriber(modid = RealismSurvival.MODID[, value = Dist.CLIENT])`. `bus =` 파라미터는 존재하지 않는다.
* 로그 접두사 `[RealismSurvival]`, 로거는 `RealismSurvival.LOGGER` 하나만 사용.
* Javadoc에 "왜"를 쓴다. "무엇"은 코드가 말한다.
* 새 데이터 타입 추가 절차: record+`CODEC` → `SurvivalDataManager`에 volatile 맵 + `publish*` + 조회 메서드 → `SurvivalReloadListeners`에 `addListener` 1줄 → 내장 JSON 1개 이상 → 이 문서 §7.4/§13 갱신.
* **코어 API 변경**은 애드온 호환을 깨지 않도록: 공개 타입은 `api` 패키지(예정)에만 두고, 구현 세부(`event` 내부 헬퍼 등)는 숨긴다. 이벤트·태그는 추가만 하고 기존 시그니처는 가능하면 유지(P7).

---

## 15. 코어 API 범위·레이어·로드맵

### 15.1 레이어 구분 — 베이스(코어) vs 심화(애드온)

| 레이어 | 포함 | 비포함 |
|---|---|---|
| **베이스 계약 (코어)** | `SurvivalData`, 갈증/체온 평가·패널티·sync·HUD·config, `drink`/`biome_thirst`/`insulation` 로더, `SurvivalApi`·이벤트·태그 | 악세서리 아이템, 추가 생존 미터 본체, JEI/FD/FTB Chunks 브릿지 |
| **베이스 인프라 (코어)** | `WaterQuality` + 기본 4등급(§9.9), worldgen 변환, 등급 fluid 렌더, 소형 민물 수면 부패 | 가마솥 희석; 인접 중화 보류; 현장 정화는 후속 |
| **심화 (애드온 책임)** | 필터·증류·소금 라인 확장, 작물 생태, Curios 장비·효과, 쉼터/시즌/질병 등 2차 시스템, 팩 브릿지 | 코어에 신규 심화 콘텐츠를 계속 추가하지 않음 |
| **레거시 동봉 (단일 jar, 성장 금지)** | 현재 jar에 이미 있는 필터·증류기·작물 일조·Curios **슬롯만**·스택 크기 | 신규 기능은 여기가 아니라 애드온으로. 분리 트리거(아래) 시 애드온 jar로 이전 후보 |

#### 의도적 결정 — 베이스만 코어, 심화는 애드온

“API 모드” = **갈증·체온 베이스 엔진 + WaterQuality 정체성 + 확장 훅**. 빈 인터페이스 jar도 아니고, 만능 콘텐츠 팩도 아니다 (§1.1).

- **코어에 고정 (베이스):** `SurvivalData` / tick·evaluate / penalties / sync / HUD / `SurvivalApi`·이벤트·태그 / `drink`·`biome_thirst`·`insulation` 로더 / `WaterQuality` + distilled·fresh·stagnant·salt.
- **애드온이 담당 (심화):** 정수·증류 콘텐츠 확장, 작물·악세서리·기후/쉼터 모드, FD·JEI 등 팩 브릿지, 커스텀 미터. 훅·태그·데이터팩만으로 붙인다 (§15.3.0).
- **하지 않음:** 베이스 엔진·4등급·로더를 전부 애드온으로 빼는 전면 분리(순수 API jar).
- **레거시 동봉:** 필터/증류기·작물·Curios 슬롯은 당장 jar에서 제거하지 않되, **코어에 심화를 더 쌓지 않는다.** 인식 API(태그·이벤트)는 유지해 애드온이 대체·확장할 수 있게 한다.
- **멀티모듈 트리거:** (1) 베이스만 쓰는 애드온/팩 수요 (2) 레거시 동봉이 연동 비용이 됨 (3) API 전용 의존 요구 — 이때 `…-content`로 심화만 분리. **베이스(엔진+물 4등급)는 코어에 남긴다.**

### 15.2 패키지 목표 (점진)

현재는 단일 모듈에 구현이 섞여 있다. jar를 쪼개지 않은 채 목표 배치:

```
com.example.realismsurvival.api/     ← 공개: SurvivalData 조회, 이벤트, 태그 상수, 음용/체온 헬퍼
com.example.realismsurvival.impl/   ← 내부: tick 핸들러, 로더, mixin (애드온이 import하지 않음)
com.example.realismsurvival.client/ ← HUD (기존)
```

1차로 **`api` 파사드·이벤트·레지스트리**를 공개한다. 패키지/`impl` 정리와 jar 분리는 §15.1 트리거 이후에만 검토한다.

**애드온 계약 문서:** [`docs/ADDON_API.md`](docs/ADDON_API.md) (동결 표면) · [`docs/VERSIONING.md`](docs/VERSIONING.md) (SemVer) · 샘플 [`examples/rs-api-sample/`](examples/rs-api-sample/) (기본 off, `-PsampleAddon=true`로만 로드).

### 15.3 공개 확장점 (목표 목록)

| 확장점 | 형태 | 용도 |
|---|---|---|
| `#realismsurvival:heat_sources` | 블록 태그 | 체온 열원. 애드온 난로·기계 |
| `#realismsurvival:drinkable_fluids` (등급별 하위 태그) | 유체 태그 | 월드 마시기·판정 |
| `#realismsurvival:salt_water` / `stagnant_water` | 바이옴 태그 (`api.ModBiomeTags`) | 기본 등급 매핑; 애드온 바이옴 추가 |
| `#realismsurvival:purifiers` / filter fuel tags | 아이템·블록 태그 | 정수 필터 소모품·허용 입력 (W-01) |
| `WaterQuality` 레지스트리 | `realismsurvival:water_quality` | 애드온 새 물 등급 (fluid/item 동시 등록) |
| `WaterApi` | 공개 헬퍼 | resolve / convert / fluid·item 역조회 / interactable source |
| `SurvivalApi` | 공개 헬퍼 | thirst/temp get·set·add, `applyDrink` |
| `SurvivalDataAccess` | 공개 헬퍼 | `drink` / `biomeThirst` / `insulation` 조회 |
| `#filter_input_buckets` / `#still_input_buckets` / `#still_salt_source_buckets` | 아이템 태그 (`api.ModItemTags`) | 필터·증류기 입력 인식 |
| `#drinkable_fluids` (+ 등급 fluid 태그) | 유체 태그 | 월드 마시기·뜨기 |
| `PlayerDrinkEvent` | NeoForge 이벤트 (취소·수정 가능) | thirst delta / effects 개입 |
| `ThirstDrainEvent` | NeoForge 이벤트 (취소·drain 수정) | 갈증 소모·악세서리·기후 배수 |
| `TemperatureCalculateEvent` | NeoForge 이벤트 | ambient / insulation / 환경 분해(고도·주야·젖음·열원·냉원·계절) |
| `CollectEquippedItemsEvent` | NeoForge 이벤트 | Curios 외 악세서리 인벤을 단열 합산에 포함 |
| `ResolveWaterQualityEvent` | NeoForge 이벤트 | 바이옴→등급 덮어쓰기 |
| `ConvertVanillaWaterEvent` | NeoForge 이벤트 (취소 가능) | 바닐라 물 치환 스킵·등급 변경 |
| `EquipmentAccess` / `SurvivalApi.collectEquipped` | 공개 헬퍼 | 갑옷+Curios+이벤트 장비 목록 |
| `drink` / `biome_thirst` / (T-06) `insulation` JSON | 데이터팩 | 밸런스·신규 아이템 연동 (insulation은 Curios 포함) |
| 바닐라 레시피 JSON | 데이터팩 | 애드온 제작법 (코어 개입 불필요) |
| `SurvivalAttachments.get/set` | 기존 (가급적 `SurvivalApi` 경유) | 애드온이 수치 읽기/쓰기 (서버만 set) |

### 15.3.0 범용 생존 애드온 매트릭스 (심화 창구)

코어는 **갈증·체온 베이스 엔진 + 훅**만 키운다. 아래는 **심화 애드온**이 콘텐츠 없이도(또는 자기 콘텐츠와 함께) 이벤트·태그·데이터팩으로 붙는 규격이다.

> **무체감 원칙:** 연동 창구(이벤트·빈 태그·`EnvironmentProbe`)를 열어도 **리스너/데이터팩이 없으면** 베이스 체감이 창구 추가 전과 동일해야 한다. 예외: 코어가 직접 채운 `#heat_sources` / `#cold_sources`와 하드코드 열·냉 가중, 수영/잠수 갈증 회복은 코어 콘텐츠다. 쉼터·애드온 전용 젖음 배수 등은 구독할 때만 추가 개입.

| 애드온 종류 | 붙이는 규격 |
|---|---|
| 악세서리 장착 | Curios 슬롯 태그 `#curios:*` (코어가 슬롯 선등록) |
| 악세서리 단열 | `insulation/*.json` (Curios 자동 스캔) 또는 `TemperatureCalculateEvent#addInsulation` |
| 악세서리 갈증·스태미나성 배수 | `ThirstDrainEvent` · `SurvivalEvaluateEvent.Pre/Post` |
| 비-Curios 악세서리 | `CollectEquippedItemsEvent#add` |
| 기후·계절·날씨·차원 | `EnvironmentContext` + `TemperatureCalculateEvent#addAmbient` · `biome_thirst` · 차원은 `environment.dimension()` |
| 텐트·지붕·차량 쉼터 | `ShelterCalculateEvent` · `#shelter_blocks` |
| 비·습도·수영 | `WetnessCalculateEvent` (코어 젖음/잠수 오프셋·수영 갈증은 evaluator/tick이 직접 처리) |
| 수중·유독 대기·호흡기 | `BreathCalculateEvent` · `#submerging_fluids` · `#breathable_fluids` |
| 열원 / 냉원 | `#heat_sources` / `#cold_sources` (코어 ambient 스캔) |
| 침대·휴식·스파 | `SurvivalRestEvent` (기상 시) · `#comfort_blocks` |
| 음료 | `drink/*.json` + `PlayerDrinkEvent` |
| 음식·약품·버프 아이템 | `SurvivalItemFinishEvent` (사용 완료 후; drink 적용 여부 포함) |
| 물 등급·바이옴 | §15.3.1 |
| 커스텀 미터(정신·방사능·질병 등) | `SurvivalEvaluateEvent`에서 `SurvivalApi`로 병행 갱신 (별도 Attachment는 애드온 소유) |
| 임의 수치 조작 | `SurvivalApi` · `SurvivalApi.probeEnvironment` |

**평가 창 파이프라인:** `EnvironmentProbe` → shelter/wet/breath 이벤트 → `SurvivalEvaluateEvent.Pre` → thirst(수영 회복 포함) → temperature → write → `SurvivalEvaluateEvent.Post`.  
체온 ambient는 플레이어 접촉(비·물·잠수·화염)과 열/냉 블록 스캔을 코어에서 계산한다. probe의 쉼터/젖음 배수 필드는 ambient에 넣지 않으며, 애드온은 `TemperatureCalculateEvent`로 추가 개입한다.

### 15.3.2 계획 모드팩 스택 (NeoForge 26.1) — 소프트 호환

목표: 아래와 **같이 써도 로드·기본 생존 체감이 깨지지 않는다.**  
JEI 플러그인·FD `drink`/`crop_season` datapack(`neoforge:mod_loaded`)·Serene Seasons 리플렉션은 soft-compat로 동작한다.

| # | 모드 | modId | NeoForge 26.1 | 본 모드와의 관계 |
|---|---|---|---|---|
| 1 | JEI | `jei` | ✅ | optional. `compat/jei` — 작업대 JSON + CustomRecipe(양동이↔병·얼음 양동이) 표시 |
| 2 | Cloth Config API (+이를 쓰는 모드들) | `cloth_config` | ✅ | optional. 라이브러리 — 충돌 없음. 본 모드는 아직 Cloth UI 미사용(NeoForge config) |
| 3 | Architectury API (+이를 쓰는 모드들) | `architectury` | ✅ | optional. 라이브러리 — 공존 OK. 본 모드는 Architectury 미의존 |
| 4 | AppleSkin | `appleskin` | ✅ | **required (CLIENT)**. 허기/포만 아이콘 |
| 5 | Sodium | `sodium` | ✅ | optional. 등급 물 fluid mixin은 Sodium 있을 때만 |
| 6 | Curios API (+악세서리 모드들) | `curios` | ✅ | optional. 슬롯 datapack + insulation 스캔(매칭 없으면 무체감) |
| 7 | FTB Chunks | `ftbchunks` | ✅ (26.1.2.x) | optional. 미니맵·월드맵·클레임·포스로드. 생존 HUD와 독립 (`-Ppackcontent`) |
| 8 | Iris Shaders | `iris` | ✅ | optional. Sodium과 버전 쌍 주의(§4) |
| 9 | Farmer's Delight | `farmersdelight` | ✅ (비공식 Neo 26.1.2 포트, CurseMaven) | optional. `drink/fd_*.json`·`crop_season/fd_*.json` (`mod_loaded`) |
| 10 | SuperMartijn642's Core Lib (+이를 쓰는 모드들) | `supermartijn642corelib` | ✅ | optional. 라이브러리 — 공존 OK |
| 11 | Serene Seasons | `sereneseasons` | ✅ (Neo 26.1.2+) | optional soft-compat. 있으면 계절 권위·작물 시즌 위임; 없으면 자체 365/4 |
| 12 | Aquaculture 2 | `aquaculture` | ✅ (Neo 26.1.x) | optional soft-compat. `runClient` localRuntime (끄기: `-Pfishingmods=false`) |
| 13 | Fish of Thieves | `fishofthieves` | ✅ (Neo 26.1.2) | optional soft-compat. Cloth Config 필요. 동일 `-Pfishingmods=false` |
| 14 | Create | `create` | ❌ **26.1 미출시** (Modrinth 최신 = 1.21.1) | optional **soft-compat · 연동만**. Fluid/Item cap 준비됨. Create 레시피 변경 없음. `localRuntime`는 26.1 jar 나오면 `-Pcreate=true` |
| 14b | Large Fluid Tank | `fluidtank` | ✅ (Neo 26.1.2) | Create 탱크 **대체 저장**. Scalable Cat's Force(Scala 로더) 필요. `runClient` localRuntime (끄기: `-Pfluidstack=false`) |
| 14c | Pipez | `pipez` | ✅ (Neo 26.1.2) | Create 파이프 **대체 수송**. 동일 `-Pfluidstack=false` |
| 15 | Jade | `jade` | ✅ | **ADOPT** QoL. `-Ppackqol` |
| 16 | *(지도는 #7 FTB Chunks)* | — | — | Xaero Minimap/World Map·OPC **제거**. 클레임·맵은 FTB Chunks |
| 17 | Mouse Tweaks / TrashSlot | `mousetweaks` / `trashslot` | ✅ | **ADOPT** (TrashSlot→Balm) |
| 18 | Iron Chests | `ironchest` | ✅ | **ADOPT** |
| 19 | Simple Voice Chat | `voicechat` | ✅ | **ADOPT** |
| 20 | AmbientSounds 6 + CreativeCore | `ambientsounds` / `creativecore` | ✅ | **ADOPT**. BoP는 카테고리 사운드 수준(전용 애드온 불필요) |
| 21 | Entity Culling / ModernFix / FerriteCore | `entityculling` / `modernfix` / `ferritecore` | ✅ | **ADOPT** 성능 |
| 22 | Comforts | `comforts` | ✅ jar | **ADD** — 침대≠침낭 RS 훅 후. [`docs/MODPACK.md`](docs/MODPACK.md) |
| 23 | BoP + TerraBlender | `biomesoplenty` / `terrablender` | ✅ jar | **ADD** — `biome_thirst`/체온 datapack 후 (~60h 분량) |
| 24 | Dynamic Trees | `dynamictrees` | ✅ jar (β) | **HOLD** — DTBOP **26.1 미출시** → BoP와 동시 도입 보류 |
| 25 | Artifacts | `artifacts` | ✅ jar | **ADD** — Curios 계측 슬롯 분리 후 중후반 |
| 26 | Guard Villagers | `guardvillagers` | ✅ jar | **ADD** — BoP 마을 스모크 테스트 후 |
| 27 | Waystones | `waystones` | ✅ jar | **ADD · QUEST-GATED** — 중반·인당 ~2 (Balm+Shogi) |
| 28 | Soph. Backpacks | `sophisticatedbackpacks` | ✅ jar | **ADD · QUEST-GATED** — 저장만, 자석 등 OFF |
| 29 | Nature's Compass | `naturescompass` | ✅ jar | **ADD · QUEST-GATED** |
| 30 | Pet Home | `pethome` *(confirm modId)* | ✅ jar | **ADD** — Taming 챕터 (DI 대체) |
| — | Dynamic Lights | — | — | **SKIP** (석탄/횃불 압박 유지) |

팩 모드 목록·약 60시간 분량·퀘스트 설계: [`docs/MODPACK.md`](docs/MODPACK.md).  
**팩 장르(고정):** 현실주의적 판타지 RPG — RS=물리 규칙, **FTB Quests**=분야별 진행 후 **Mastery 최종장에서 합류**. 엔딩=목록화된 전 음식·전 몹 등 수집형 완료(보스 단독 엔딩 아님). Gaia 제외. 상세 [`docs/MODPACK.md`](docs/MODPACK.md) «Mastery · 극후반».

**라이브러리 계열(2·3·10):** 본 코어가 직접 호출하지 않아도, 팩에 들어와도 로드만 되면 된다. 그 위에 얹힌 “유명 모드”는 각자 NeoForge 26.1 jar를 쓰면 된다.  
**지도·클레임:** `ftbchunks`를 `ModCompat`에서 감지(선택). 팩 기본 forceload 한도 16·오프라인 로드 off — `pack/config/ftbchunks-world.snbt`.  
**Serene Seasons:** 리플렉션 브리지(`compat/sereneseasons`). `runClient`에 GlitchCore + Serene Seasons `localRuntime` (끄기: `-Psereneseasons=false`).  
**Cloth Config:** FoT 필수 의존으로 `runClient`에 `localRuntime` (`cloth-config` Modrinth). 끄기는 `-Pfishingmods=false`와 함께.  
**낚시 모드:** Aquaculture + Fish of Thieves는 soft-compat. `drink/aqua_*`·`drink/fot_*`로 식용 갈증·체온 매핑.  
**하지 않음 — 물 등급별 낚시/서식 분리:** QUALITY(증류·민물·고인·바다)로 loot·자연 스폰 종을 가르지 않는다. 경우의 수(바닐라+Aqua+FoT×등급)가 과도하고, 두 모드는 바이옴 룰만 쓴다. graded water는 `#minecraft:water`로 물리 낚시/스폰만 가능하게 두고 끝낸다.

**물 저장·자동화 투트랙 (Create 연동 방침):**

| 트랙 | 수급 | 저장 | 정화·증류 | 필수? |
|---|---|---|---|---|
| **A. 호퍼/바닐라** | 양동이 수동·간단 운반 | (선택) Large Fluid Tank 등 | 호퍼 → 필터/증류기 슬롯 | **기본 완결** — Create 없이 초중반 가능 |
| **B. Pipez + LFT** | 월드/탱크 → 파이프 | **Large Fluid Tank** (`fluidtank`) | Pipez → `Capabilities.Fluid.BLOCK` | **26.1 Create 대체** (`-Pfluidstack`) |
| **B′. Create** (대기) | 펌프·파이프·호스 | Create 탱크 | 벨트·펀널 / 스파우트·드레인 | 26.1 jar 후 추가 |

- 공통: 탱크·파이프 **안**의 유체는 월드 원천 부패 대상이 아님(부패는 민물 **원천 블록**만).  
- Create / LFT / Pipez는 **연동만** — graded 물 FluidType/QUALITY가 탱크·파이프·머신 FluidHandler에 통하게.  
- **하지 않음:** 외부 모드 레시피 난이도 완화·너프, Create/LFT/Pipez를 초중반 필수처럼 유도.  
- 레시피 완화는 원하면 팩메이커 **optional datapack**으로만 (코어 범위 밖).

**조리·제작에 쓰는 물 양동이 (현실성 우선):** 민물/고인물/바닷물로 요리하고 완성품에 패널티를 얹지 않는다. **증류수 양동이만** 조리/관련 제작에 쓴다.
- 태그 `#realismsurvival:cooking_water_buckets` = `distilled_water_bucket`
- `#c:buckets/water`에도 증류수 추가 (컨벤션 브리지; 바닐라 `water_bucket` 항목은 NeoForge 기본 유지)
- 오버라이드: `farmersdelight:cooking_pot`, `aquaculture:turtle_soup` → `#realismsurvival:cooking_water_buckets`

`ModCompat.logDetected()`가 common setup에서 위 modId yes/no를 남긴다. `incompatible` 강제는 하지 않는다.

### 15.3.1 애드온 계약 — 형식만 맞으면 코어가 인식

원칙: **하드코딩된 아이템 ID 비교를 피하고**, 레지스트리·태그·데이터팩·이벤트로만 연동한다.

#### 새 물 등급

1. **fluid/block을 새로 만들지 않음** — 코어 공유 `graded_water` + `QUALITY` id(레지스트리 순번, 0–15) 사용  
2. bucket · bottle을 **애드온 모드에서** 등록  
3. `DeferredRegister.create(WaterQuality.REGISTRY_KEY, modid)` + `WaterQuality.builder()` (biome tag · hydrate flags · tint · drink effects · resolvePriority · bottle/bucket 홀더). `sourceFluid`/`flowingFluid` 빌더 호출은 no-op(호환용)  
4. 양동이는 `BUCKET_FILL` + 가급적 `GradedWaterBucketItem`(공유 fluid + quality supplier). 병도 `GradedWaterBottleItem` 권장  
5. `drink/<bottle>.json`으로 병 갈증·체온 (월드 한 모금도 등급 병이 있으면 그 값을 우선)  
6. 바이옴 매핑: 등급의 `biomeTag` 또는 `#salt_water` / `#stagnant_water`에 바이옴 추가. 예외만 `ResolveWaterQualityEvent`  
7. 등급 판별은 fluid 태그가 아니라 `WaterApi.fromFluidState` / `isQuality`. 디스펜서: `WaterQuality` bucket은 공통 setup에서 **자동 등록**  
8. `QUALITY` id가 15를 넘지 않게 등록 순서·개수를 유지 (초과 시 fresh로 클램프)

#### 필터 · 증류기

| 태그 | 의미 | 코어 기본값 |
|---|---|---|
| `#realismsurvival:filter_input_buckets` | 필터 입력 → **코어 민물 병** 출력 | stagnant bucket |
| `#realismsurvival:still_input_buckets` | 증류기 입력 → **코어 증류수** 출력 | fresh + salt buckets |
| `#realismsurvival:still_salt_source_buckets` | 증류 시 소금 부산물 | salt bucket |

애드온 양동이를 해당 태그에 넣으면 기계가 인식한다(입력 아이템은 `WaterQuality`에 bucket으로 연결되어 fill이 동작해야 함).

#### 기타 커스텀

| 하고 싶은 것 | 규격 |
|---|---|
| 새 바이옴 갈증 | `biome_thirst/*.json` (태그·ID HolderSet) |
| 새 음료 아이템 | `drink/*.json` + (선택) `PlayerDrinkEvent` |
| 단열 장비 | `insulation/*.json` |
| 열원 블록 | `#realismsurvival:heat_sources` |
| 제작법 | 바닐라/데이터팩 레시피 (병↔양동이는 레지스트리 등급이면 코어 커스텀 레시피도 동작) |
| 체온·갈증 개입 | `SurvivalApi` / `TemperatureCalculateEvent` / `PlayerDrinkEvent` |

### 15.4 로드맵 순서 (코어 먼저)

1. ✅ **이벤트 훅 + 열원/음용 태그** — `api.event.PlayerDrinkEvent` / `TemperatureCalculateEvent`; `#realismsurvival:heat_sources`, `#realismsurvival:drinkable_fluids` (+ 등급별 fluid 태그)
2. ✅ **T-06 단열 데이터팩** — `insulation/*.json`, 착용 방어구 합산 → °C/h 변화율 완화
3. ✅ **T-09 클라 동기화** — `SyncSurvivalDataPayload` / `ClientSurvivalData` / drink 툴팁
4. ✅ **T-10 / T-04 / T-02** — dehydration · 월드 물 마시기 · 리스폰 리셋
5. ✅ **T-11 GameTest** — thirst_drain / drink_restore / respawn_reset / drink_cancel / temperature_converge / water_quality_resolve / water_quality_state / penalty_hypothermia
6. **§15.5 물 콘텐츠 트랙** — 간이 필터 → 증류기 → 화로 역할 조정 (아래)

§15.4 코어 로드맵(1–5) 완료. 진행 중: §15.5.

### 15.5 물 콘텐츠 로드맵 — 정수 필터 · 증류기

코어 built-in 등급은 **WaterQuality 4종** + **공유 Fluid 1쌍**. 코어 콘텐츠는 유체를 늘리지 않고 **장치로 QUALITY를 바꾼다.** (애드온 등급 추가는 §15.3.1.)

#### 설계 계약 (합의)

| 규칙 | 내용 |
|---|---|
| 식수 계층 | 증류 = 안전 식수 · 민물 = 응급 · 고인/바다 = 사실상 비식용 (§9.9) |
| 고인 → 민물 | **정수 필터 블록**(슬롯 기계)만. 증류기로 직행 불가. 월드 반경 정화 없음 |
| 민물 → 증류 | **증류기**만 |
| 바다 → 증류 | **증류기만** + 부산물 **소금**. 필터로는 민물화하지 않음 (1차) |
| 바다 → 소금만 | **화로**에 바닷물 양동이 → **소금** (증류수 없음). 세부 §15.5.1 W-03 |
| 고인 → 증류 | **불가** (필터로 민물화한 뒤 증류) |
| 화로 → 증류수 | **불가**. `distilled_water_bucket_from_smelting` **삭제됨**(W-03). 바닐라 기타 제련 레시피는 그대로 |

루프 요약:

```
고인물 양동이 ──필터──► 민물 병 (×fill) ──역조합──► 민물 양동이 ──증류기──► 증류수
바닷물 양동이 + (빈 양동이|유리병) + 연료 ──증류기──► 증류수(같은 용기) + 소금
바닷물 양동이 ──화로──────────────────► 소금×2 (양동이 소모)
민물 양동이 + (빈 양동이|유리병) + 연료 ──증류기──► 증류수(같은 용기)
```


#### 티켓 순서

| ID | 제목 | 선행 |
|---|---|---|
| **W-01** ✅ | 정수 필터 블록 | — |
| **W-02** ✅ | 증류기 + 소금 아이템 | UI 패턴 공유 |
| **W-03** ✅ | 화로 역할 조정 | salt 아이템 후 |
| **W-04** ❌ | GameTest (선택) | W-01·W-02 — 미착수 |
| **W-05** ✅ | 양동이 채움 1/3·2/3·가득 | W-01~W-03 |
| **W-06** | (후속) 양동이 월드 설치·장치 IO | W-05 이후, 현재 안 함 |

---

### 15.5.1 세부 결정 동결 (구현 착수 기준)

숫자·슬롯·입출력은 아래를 **기본값으로 동결**한다. 바꾸면 이 절과 AC를 같이 수정한다.

#### 공통

| 항목 | 결정 |
|---|---|
| 새 Fluid | **추가하지 않음** (4등급 유지) |
| 기계 UI | 화로형 Container + BlockEntity. **슬롯 역할은 장치별 상이**(필터≠증류기) |
| 진행 단위 | **1병 = 양동이 1/3** (W-05 전에도 내부적으로 동일: 가득 양동이 = 3사이클) |
| 호퍼 | **지원** (`WorldlyContainer` + `Capabilities.Item.BLOCK`/`WorldlyContainerWrapper`). **위**: 원료·빈 용기(필터는 병). **옆**: 연료+원료·빈 용기(필터는 입력 전부). **아래**: 결과·소금 + 소모 후 빈 양동이(원료칸에 남으면 추출해야 재투입 가능). 물 양동이 슬롯은 호퍼 병합 방지로 **항상 1개** |
| 파괴 | 화로와 동일: 블록 아이템 + **내용물 전부 드롭**. 진행도·잔여 연료는 소실 OK |
| 텍스처 | **커스텀 블록/GUI 텍스처는 후속 아트 패스**. 1차는 플레이스홀더(단색·임시 모델)로 기능 우선. lit/active용 **별도 텍스처 슬롯은 미리 등록** |
| config | `filterProcessTicks`, `stillProcessTicks` (서버 config). 기본값은 아래 |

#### W-01 정수 필터

| 항목 | 결정 |
|---|---|
| 형태 | 배치 블록 `water_filter` + BE + Menu |
| 슬롯 | (0) 입력 양동이 · (1) 유리병 · (2) 결과 민물 병 · (3) 빈 양동이 출력 |
| 입력 허용 | `stagnant_water_bucket` **만** |
| 입력 거부 | fresh / salt / distilled 양동이, 병, 기타 |
| 1사이클 | 유리병 1 소비 → 민물 병 1 산출 · 입력 양동이 내부 fill −1 (W-05 전이면 카운터 3→2→1→0) |
| fill 0 | 입력 슬롯 비우고 **빈 양동이**를 슬롯 (3)으로 |
| 연료 | **없음** (증류기와 차별) |
| 시간 | 기본 **200틱(10초)/병** (`filterProcessTicks`) |
| 경도 | **맨손 즉시 수준** — `destroyTime` ≈ 나무/흙대 (예: **0.5**), `requiresCorrectToolForDrops` = **false**. 맨손으로도 금방 깨지고 드롭 |
| GUI 진행 | 컨테이너에 **정수 진행 화살표/바** (화로 cook progress와 동일 계열). 클라 동기화 필수 |
| 외부 동작 표시 | 처리 중 블록스테이트 **`filtering=true`**(또는 `active`). 외형으로 “돌아가는 중”이 보이게 — 1차는 면/오버레이 플레이스홀더, 아트 패스에서 물·필터 움직임 텍스처로 교체 |
| 제작 | 아래 **제작 레시피 (동결)** — `data/realismsurvival/recipe/water_filter.json` |
| 월드 정화 | **없음** |

#### W-02 증류기

| 항목 | 결정 |
|---|---|
| 형태 | 배치 블록 `water_still` + BE + Menu |
| UI 계약 | **열원 + 물 양동이 + (빈 양동이 \| 유리병) → 증류수(같은 용기) + 부산물(있으면)** |
| 슬롯 | (0) **연료(열원)** · (1) **물 양동이(원료)** · (2) **빈 용기**(빈 양동이 또는 유리병) · (3) **증류 결과** · (4) **부산물(소금)** |
| 원료 허용 | `fresh_water_bucket` · `salt_water_bucket` (fill 1..3). **병 원료 불가** |
| 원료 거부 | 고인·증류 양동이, 아무 물병, 빈 양동이, 기타 |
| 빈 용기 | `bucket`(빈) → 결과 = 증류수 **양동이** · `glass_bottle` → 결과 = 증류수 **병** |
| 1사이클 (병) | 유리병 1 소비 · 원료 fill −1 · 증류수 병 1 → (3) · 바닷물이면 소금 +1 → (4) · 원료 fill 0이면 (1)을 **빈 양동이**로 |
| 1사이클 (양동이) | 빈 양동이 1 소비 · 원료 **가득(fill=3)만** 허용 · 원료 제거 후 (1)에 빈 양동이 · 증류수 양동이(fill=3) → (3) · 바닷물이면 소금 **+3** → (4) |
| 부분 원료→양동이 | 원료 fill가 3 미만 + 빈 양동이 = **진행 불가** (병으로만 빼기) |
| 연료 | 바닐라 **화로 연료** (`FuelValues` / burn time). 필터와 차별점 |
| lit | **필수**. `lit=true`일 때 별도 모델/텍스처(화로 `furnace`/`furnace_on` 패턴). 증류기 전용 텍스처는 아트 패스에서 제작 — 1차는 플레이스홀더 on/off 쌍 |
| GUI 진행 | cook/fuel 게이지(화로형). 클라 동기화 |
| 시간 | 기본 **400틱(20초)/병-사이클**. 양동이 출력 = **3사이클**(1200틱) 또는 동일 총량으로 한 번에 처리(구현 편의, AC는 총 틱·입출력만 일치) |
| 소금 아이템 | `realismsurvival:salt`, 스택 64, 식용 아님(1차). **제작 레시피 없음**(부산물만) |
| 경도 | **화로급** — `destroyTime` **3.5**, `explosionResistance` 화로와 동급, `requiresCorrectToolForDrops` = **true**(돌 곡괭이 이상). 맨손으로는 사실상 안 깨짐 |
| 파괴 | 내용물 **인벤 드롭** (화로와 동일) |
| 제작 | 아래 **제작 레시피 (동결)** — `data/realismsurvival/recipe/water_still.json` |
| 유일 경로 | 월드/화로로 증류수 **생성 불가** (W-03) |

#### 제작 레시피 (동결) — W-01 / W-02

진행 톤: **필터 = 철 직후 초중반**, **증류기 = 화로+가마솥+구리 확보 후 중반**. 증류기가 확실히 비싸서 “안전 식수” 게이트가 되도록.

**정수 필터** `water_filter` — `minecraft:crafting_shaped`

| | 1 | 2 | 3 |
|---|---|---|---|
| A | `iron_ingot` | `gravel` | `iron_ingot` |
| B | `gravel` | `charcoal` | `gravel` |
| C | `iron_ingot` | `stone` | `iron_ingot` |

- 재료: 철 잉곳 **4** · 자갈 **3** · 숯 **1** · 돌 **1**
- 의도: 철 프레임 + 자갈 여재 + 숯 흡착. 구리/유리 불필요 → 초중반
- JSON 키: `pattern` `["IGI","GCG","ISI"]`, `key` I/G/C/S → 위 아이템, `result` `realismsurvival:water_filter` ×1

**증류기** `water_still` — `minecraft:crafting_shaped`

| | 1 | 2 | 3 |
|---|---|---|---|
| A | `copper_ingot` | `glass` | `copper_ingot` |
| B | `iron_ingot` | `cauldron` | `iron_ingot` |
| C | `iron_ingot` | `furnace` | `iron_ingot` |

- 재료: 구리 잉곳 **2** · 유리 **1** · 철 잉곳 **4** · 가마솥 **1** · 화로 **1**
- 의도: 구리·유리 = 응축부, 가마솥 = 보일러, 화로 = 열원(블록 소모 1회 제작비). 필터보다 한 단계 위
- JSON 키: `pattern` `["CGC","IUI","IFI"]`, `key` C/G/I/U/F → 위 아이템, `result` `realismsurvival:water_still` ×1

| 규칙 | 결정 |
|---|---|
| 해금 | 바닐라 제작대. 전용 발전 과제 **1차 없음**(원하면 후속) |
| 재사용/분해 | 없음. 회수는 채굴 드롭만 — 필터=맨손 OK, 증류기=돌곡괭이+ |
| salt | 제작 불가. 증류기·화로(W-03) 부산물만 |
| 아트 패스 | 블록 on/off 텍스처. GUI는 바닐라 `furnace.png` + `container/slot` 합성. **출력 2칸은 가로 동일 크기**(105/125, y=35). 빈 슬롯 배경은 대표 아이템 단색 실루엣(`getNoItemIcon` / `container/slot/*`, 병은 바닐라 `container/slot/potion`). Curios는 API 기본 아이콘 |

#### W-03 화로

| 항목 | 결정 |
|---|---|
| 삭제 | `distilled_water_bucket_from_smelting.json` (및 undistilled→증류 제련 전부) |
| 유지·추가 | `salt_water_bucket` smelting → **`salt` ×2** (양동이 소모 = “빠른 증발, 물 버림”). 증류기 경로가 양동이 보존+증류수라 화로가 손해가 되도록 |
| 민물·고인 양동이 | 화로 입력 **아님** |
| 문구 | `bucket_use` / lang: 증류는 증류기, 바다 소금만 화로 |

#### W-04 GameTest (선택, 권장)

| 테스트 | AC |
|---|---|
| `filter_stagnant_to_fresh` | 필터 BE에 고인 양동이+병 → 민물 병 생성 |
| `still_rejects_stagnant` | 고인 입력 시 진행도 증가 없음 |
| `still_salt_byproduct` | 바닷물 양동이 증류 → 증류 양동이 + salt |
| `furnace_no_distilled` | (가능하면) 증류 smelting 레시피 부재 확인 |

#### W-05 양동이 채움 · 병↔양동이 조합

| 항목 | 결정 |
|---|---|
| 표현 | `DataComponent` `realismsurvival:bucket_fill` : int **1..3** (3=가득). 등급은 기존 아이템 타입 |
| 획득 | 빈 양동이로 수원 퍼올림 → **항상 3** |
| 필터/증류기 | fill n이면 최대 n병-사이클. 사이클마다 fill−1 |

**양동이 → 병 (기존 방향, shapeless):**

| 조합 | 결과 |
|---|---|
| 양동이(fill=n, 등급 Q) + 유리병 1 | 물병(Q) 1 + 양동이(n−1) 또는 빈 양동이(n=1) |

**병 → 양동이 (역조합, shapeless) — 1·2·3병 모두 허용:**

| 조합 | 결과 |
|---|---|
| 빈 양동이 + 동일 등급 물병 k(1..3) | 양동이(fill=k, 같은 등급) |
| 양동이(fill=n, 등급 Q) + 동일 등급 물병 k | 양동이(fill=n+k), **단 n+k ≤ 3** |

| 거부 | 조건 |
|---|---|
| 등급 불일치 | 양동이 등급 ≠ 병 등급 (예: 민물 양동이 + 바닷물 병) |
| 넘침 | n+k > 3 |
| 증류/혼입 | 서로 다른 등급 병을 한 번에 섞기 |
| 바닐라 물병 | `minecraft:potion` 물병은 본 모드 등급 양동이와 **역조합 불가**(등급 병만) |

구현: 단일 shapeless로 k를 가변 처리하거나, k=1·2·3용 레시피를 등급별로 두어도 됨. AC는 위 표.

**부분 채움 UX (fill 1~2):**

| 행동 | 결과 |
|---|---|
| 바닥 쏟기 | **물 미설치**, 빈 양동이만 (내용 폐기). **fill=3만** 원천 설치 |
| 수원 재퍼올림 | 부분량 **폐기→빈 양동이** 후, **재클릭**으로만 가득 |
| 허공 홀드 마시기 | fill −1, 효과 × 1 (병 1병분; §9.9) |
| 부분 양동이끼리 합치기 / 월드에서 일부만 뜨기 | **불가** (조합대·기계 fill만) |
| 표시 | 내구도형 채움 게이지 (가득이면 숨김). 툴팁 `bucket_use`·`ice/snow_bucket_use`는 **fill=3 → `.full.*` / fill&lt;3 → `.partial.*`** (블록 동작이 다름) |

#### W-06 (후속, 지금 안 함)

양동이 월드 설치 블록 + 필터/증류기 면 입출력. C안 연계용. 착수 금지 until W-05 완료·별도 합의.

---

#### 의도적으로 뒤로 미룸 (현재 트랙에서 **하지 않음**)

| 후순위 | 이유 |
|---|---|
| **대량 유입·CA 희석 중화** | 틱·밸런스 리스크. 장치는 슬롯 기계만 |
| 물속 반경 필터, 바닷물 필터 담수화, 중화조, 다블록 응축 증류 | W-01~05 이후 |
| 민물 y/바이옴별 추가 Fluid | 농도/효과로만 검토 |
| 양동이 월드 설치 (W-06) | 후속 |

**현재 중화 = 장치만:** 필터 · 증류기 · 화로(바다→소금).

#### 구현 시 패키지 가이드

- `block/` · `blockentity/` · `world/inventory/`(Menu) · `registry/` · `item/`(`SaltItem`)
- **새 Fluid 금지**
- 밸런스 틱 수: `RealismSurvivalConfig` 서버 항목

이 순서를 어기고 가마솥·CA·Curios 아이템·W-06을 먼저 넣지 않는다.
