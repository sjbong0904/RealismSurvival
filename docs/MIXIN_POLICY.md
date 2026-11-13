# 코어 Mixin 정책 (패치 2.5)

지원 버전: **Minecraft 26.1.2 / NeoForge 26.1.2.122** (`gradle.properties`, 0.1 프로필). 이 조합 외의 버전은 지원하지 않는다.
버전을 올릴 때는 아래 표의 **필수** 항목을 전부 클라이언트(runClient)와 전용 서버(runServer)에서 다시 확인한다.

## 설정 파일과 실패 정책

| 설정 | `required` | `defaultRequire` | 실패 시 |
|------|-----------|------------------|---------|
| `realismsurvival.mixins.json` | true | 1 | **게임 시작 실패** (의도). 물 변환 불변식이 깨진 채 월드를 여는 것보다 낫다 |
| `realismsurvival.sodium.mixins.json` | false | 0 | Sodium 미설치/구조 변경 시 조용히 건너뜀 → 등급 경계 이음새·평탄 틴트(시각 문제만) |
| `realismsurvival.fot.mixins.json` | false | 0 | Fish of Thieves 미설치/구조 변경 시 건너뜀 → FoT 경로에서 바닐라 물 양동이가 남을 수 있음 (`VanillaWaterConverter` 후처리로 완화) |

원칙: 핵심 물 변환 Mixin을 **optional로 내리지 않는다**. 대상 메서드가 바뀌면 시작 실패로 즉시 드러나게 두고, 지원 버전 회귀 항목으로 고친다.

## 필수 Mixin (realismsurvival.mixins.json)

등급 = 실패하면 무엇이 깨지는가. **A** = "바닐라 열린 물 없음" 불변식 또는 저장 데이터, **B** = 게임플레이 규칙, **C** = 편의/표시.

| Mixin | 목적 | 실패 영향 | 등급 |
|-------|------|-----------|------|
| `ChunkStatusTasksMixin` | 월드젠 FEATURES 직후 ProtoChunk의 바닐라 물 → 등급수 | 새 청크에 바닐라 물 잔존, 로드 시 대량 변환 부하 | A |
| `IceBlockMeltMixin` | 얼음 녹음 → 등급수 (같은 호출 스택) | 녹은 자리에 `Blocks.WATER` | A |
| `WaterFluidMixin` | 다른 물 계열 수원이 흐르는 바닐라 물을 밀어낼 수 있게 | 등급수/바닐라 물 경계에서 흐름 정지 | A |
| `FlowingFluidMixin` | 등급수·바닐라·물에 잠긴 칸을 하나의 수면으로 흐름 계산 | 산호 옆 흐름 리셋, 벽처럼 동작 | A |
| `SimpleWaterloggedBlockMixin` | 등급수 양동이로 물에 잠김 허용 (산호 제외) | 등급수 양동이로 물에 잠김 불가 | B |
| `CoralSkipWaterFluidTickMixin` | 물에 잠긴 산호의 `Fluids.WATER` 틱이 바닐라 물을 퍼뜨리지 않게 | 산호 주변 바닐라 물 확산 | A |
| `BiomeFreezeMixin` | 추운 바이옴에서 등급수도 얼게 | 등급수 바다/호수가 겨울에 안 얼음 | B |
| `ServerLevelFreezeMixin` | 강수 결빙 → 등급 얼음(QUALITY 유지) | 결빙 시 등급 손실 | B |
| `SnowAndFreezeFeatureMixin` | 월드젠 결빙 → 등급 얼음 | 월드젠 얼음 등급 손실 | B |
| `BiomePrecipitationMixin` | 자체 달력 강수 정책 (SS 활성 시 건너뜀) | SS 없을 때 계절 강수 없음 | B |
| `BiomeHasPrecipitationRecursionMixin` | SS 클라이언트 `hasPrecipitation` 재귀 차단 | SS 설치 클라이언트 StackOverflow | A |
| `FarmlandBlockMixin` | 농지 수분 판정에 등급수 반영 | 등급수 옆 농지 건조 | B |
| `LayeredCauldronBlockMixin` / `CauldronBlockMixin` | 가마솥에 QUALITY, 비/점적 → 등급수 | 가마솥 물 등급 없음 | B |
| `SpongeBlockMixin` / `WetSpongeBlockMixin` / `ItemNameMixin` | 스펀지가 흡수한 물 등급 유지·표시 | 스펀지 짜면 등급 손실 | B |
| `AbstractFurnaceBlockEntityMixin` | 젖은 스펀지 → 등급수 양동이; 바닷물 양동이 증발 시 빈 양동이 반환·채움량 비례 소금 | 양동이 소실, 부분 양동이로 소금 복제 | B |
| `PowderSnowBlockMixin` | 가루눈 → 모드 눈 양동이 | 바닐라 가루눈 양동이 | C |
| `BucketableMixin` / `AxolotlFeedMixin` / `NautilusFeedMixin` | 물고기 포획·먹이 후 등급수 양동이 | 바닐라 물 양동이 생성 | A |
| `DriedGhastBlockMixin` | 마른 가스트가 등급수로 수화 | 수화 불가 | C |
| `CoralBlockMixin` / `BaseCoralPlantTypeBlockMixin` / `CoralPlantBlockMixin` / `CoralFanBlockMixin` / `CoralWallFanBlockMixin` | 산호는 바닷물에서만 생존 | 산호 생존 규칙 무시 | B |
| `SupplyPostFireMixin` | 공급소 나무 가판대 불 번짐 방지 | 가판대 소실 | C |
| (client) `FluidStateModelSetMixin` / `FluidRendererMixin` / `BlockFluidFaceMixin` | 등급 경계 렌더 이음새 제거, 바닷물 틴트 | 경계에 수직 이음새 | C |

## 회귀 확인 (지원 버전에서, 클라이언트 + 전용 서버)

1. 새 월드: 바다·강·늪 주변 청크에 `minecraft:water` 열린 칸 0 (F3 대상 블록 / 디버그 스캔).
2. 얼음 깨기·녹이기, 가루눈, 스펀지 흡수→화로, 물고기 양동이 포획·먹이 → 모두 등급수.
3. 산호 옆 흐르는 등급수: 흐름 유지, 바닐라 물 확산 없음.
4. 겨울(SS 있음/없음): 등급수 수면 결빙, 결빙 얼음 QUALITY 유지.
5. 바닷물 양동이 화로 증발: 소금 2 + 빈 양동이(연료 칸 비면 연료 칸, 아니면 입력 칸). 1/3 양동이 → 소금 1.
6. SS 설치 클라이언트에서 비/눈 바이옴 진입 시 크래시 없음.

자동화 테스트 도입은 7.3 결정에 따라 이 단계에서 하지 않는다. 위 항목은 `docs/REGRESSION_CHECKLIST.md`에 포함한다.
