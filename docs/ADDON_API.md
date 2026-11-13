# Realism Survival 애드온 API (고정 지원 범위)

Realism Survival은 갈증·체온 기반 엔진과 `WaterQuality` 식별자, 애드온 확장 지점을 제공한다. 필터/증류기 확장, 모드팩 레시피 연결, 추가 계기류처럼 깊은 콘텐츠는 애드온에서 구현한다.

이 문서는 애드온이 의존할 수 있는 계약을 설명한다. 세부 설계는 [기술 명세 §15](../REALISM_SURVIVAL_SPEC.md)를 참고한다.

## 의존 방식

| 항목 | 규칙 |
|---|---|
| Maven/Modrinth JAR | 같은 Minecraft·NeoForge 버전의 공개 JAR를 `compileOnly`로 참조한다. |
| Java 패키지 | `com.example.realismsurvival.api.**`만 사용한다. |

공개 출시 전까지 Maven group은 `com.example.realismsurvival`이다. 변경 계획은 [버전 관리 문서](VERSIONING.md)를 참고한다.

## 공개 Java API

| 타입 | 역할 |
|---|---|
| `SurvivalApi` | 갈증·체온 조회/설정/증가, `applyDrink`, 비례 회복 허용, 장비 및 환경 확인 |
| `SurvivalDataAccess` | 데이터팩 조회 (`drink` / `biomeThirst` / `insulation`) |
| `WaterApi` | 물 품질 판정, 유체·아이템 조회, 바닐라 물 변환 보조 |
| `WaterQuality` 및 레지스트리 키 | 공유 `graded_water` 유체와 품질 ID로 등급 확장 등록 |
| `GradedWaterItems` | 새 유체 쌍 없이 양동이/병 `Item.Properties` 및 팩토리 제공 |
| `EquipmentAccess` / `CuriosAccess` | 장착 아이템 열거 |
| `EnvironmentContext` | 평가 훅용 환경 스냅샷 |
| `api.event.*` | `PlayerDrinkEvent`, `ThirstDrainEvent`, `TemperatureCalculateEvent`, `SurvivalEvaluateEvent`, 대피/젖음/호흡, 물 판정·변환 이벤트 등 |
| `ModBlockTags` / `ModItemTags` / `ModFluidTags` / `ModBiomeTags` / `ModDamageTypes` | 태그 키 상수 |

### 임시 지원 예외

용기 생성은 `GradedWaterItems`로만 수행한다. 애드온에서 `registry.*`, `water.BucketFill`, `water.WaterQualityStates`를 직접 가져오지 않는다.

`SurvivalApi.getData`가 반환하는 `component.SurvivalData`는 API 패키지로 이동하기 전까지 공개 타입으로 취급한다.

## 데이터팩·태그 인터페이스

| 경로/태그 | 용도 |
|---|---|
| `data/<ns>/realismsurvival/drink/*.json` | 아이템 사용 시 갈증·체온 변화 |
| `data/<ns>/realismsurvival/biome_thirst/*.json` | 바이옴별 갈증 소모 배율 |
| `data/<ns>/realismsurvival/insulation/*.json` | 장비 단열값 |
| `#realismsurvival:heat_sources` / `cold_sources` | 주변 온도에 영향을 주는 블록 |
| `#realismsurvival:drinkable_fluids` 및 등급 유체 태그 | 월드 유체 마시기 |
| `#realismsurvival:salt_water` / `stagnant_water` | 바이옴 기본 물 등급 지정 |
| `#realismsurvival:filter_input_buckets` / `filter_cartridges` / `still_input_buckets` / `still_salt_source_buckets` | 기계 입력 판별 |
| `#realismsurvival:cooking_water_buckets` | 조리용 물 (기본값은 증류수) |

## 새 WaterQuality 추가 체크리스트

기술 명세 §15.3.1의 세부 절차와 호환성 조건을 따른다. 애드온은 기존 품질 ID나 공유 유체를 임의로 바꾸지 말고, 고유한 품질 ID를 등록한 뒤 해당 등급의 병·양동이 표현과 판정 규칙을 연결해야 한다.

1. 모드 ID를 네임스페이스로 사용하는 고유한 품질 ID를 정한다.
2. `WaterQuality`를 등록하고 병·양동이 아이템을 `GradedWaterItems`로 만든다.
3. 해당 액체를 월드에서 마실 수 있도록 필요한 유체·아이템 태그를 등록한다.
4. 바이옴 기본 등급, 물 변환, 음식 조리, 기계 입력에서 새 등급이 어떻게 처리되는지 확인한다.
5. 저장·동기화·클라이언트 툴팁에서 품질 ID가 안정적으로 유지되는지 검증한다.

API 타입이나 데이터팩 형식, 기존 태그 의미를 깨는 변경은 메이저 버전 변경 대상이다. 신규 선택 기능이나 기본값이 있는 필드는 마이너, 호환되는 버그 수정과 밸런스 변경은 패치 변경을 따른다.
