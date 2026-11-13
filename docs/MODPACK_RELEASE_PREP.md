# 공개 배포 준비 (패치 6.7 · 6.8 · 7.1)

이 문서는 **공개 배포 직전에 한 번에 처리할 작업**을 모은다. 지금은 코드·식별자를 바꾸지 않는다 (D-02, D-03, D-13).
배포 플랫폼이 정해지면 6.8 → 6.7 → 7.1 순서로 진행하고, 끝나면 `REGRESSION_CHECKLIST.md` 8.1을 다시 본다.

## 6.7 외부 의존성 · 라이선스 · 식별자

### 의존 모드 목록 (입력 = 배포 프로필의 실제 JAR)

1. `.\gradlew writeRuntimeJarList` → `build/reports/runtime-jars.txt` (현재 개발 프로필 179개).
2. 배포 manifest(6.8)가 생기면 그 목록이 정본이고, `runtime-jars.txt`는 개발 프로필 비교용으로만 쓴다.
3. 모드마다 다음 열을 `MOD_CATALOG.md`에 채운다:

| 열 | 내용 |
|----|------|
| 버전·파일 | 정확한 파일명 또는 플랫폼 버전 ID |
| 역할 | 양쪽 / 서버 전용 / 클라이언트 전용 |
| 라이선스 | SPDX 또는 원문 링크 |
| 재배포 | manifest 참조만 허용 / 팩에 JAR 포함 허용 / 금지 |
| 필수 여부 | 필수 / 선택(soft-compat) / 개발 전용 |

재배포 금지 모드는 manifest 참조로만 넣고 서버팩에도 JAR를 담지 않는다.

### 커스텀 자산 attribution

- 정본: `docs/THIRD_PARTY_ASSETS.md` (현재 `filter_cartridge.png`, CC0).
- 배포 전 `src/main/resources/assets/**/textures`와 애드온 텍스처 중 자작이 아닌 파일을 모두 이 표에 올린다. 출처를 모르는 텍스처는 교체한다.
- 배포 산출물(모드 JAR, 팩 zip)에 `THIRD_PARTY_ASSETS.md` 또는 동일 내용의 `CREDITS`를 넣는다.

### 식별자 이관 (D-03: 한 번에)

| 항목 | 현재 | 바꿀 곳 |
|------|------|---------|
| Java 패키지 | `com.example.realismsurvival` | 전체 소스, mixin json `package`, `rs-medicine` import |
| Maven group | `com.example.realismsurvival` (`gradle.properties` `mod_group_id`) | gradle, `VERSIONING.md`, `ADDON_API.md` 예제 |
| Modrinth/플랫폼 slug | 미정 | manifest, README |
| `mod_id` | `realismsurvival` | **유지** (세이브·데이터팩·퀘스트 ID 호환) |

이관 패치는 이름 변경만 담고 동작 변경을 섞지 않는다. 이관 후 자동 선행 확인 전체 + 기존 월드 로드를 확인한다.

## 6.8 모드팩 배포 manifest (플랫폼 미정, D-13)

후보: **Modrinth `.mrpack`** 또는 **packwiz** (packwiz는 `.mrpack` 내보내기 가능). 둘 중 하나로 정하면 아래 규칙을 그대로 쓴다.

### 조립 입력

| 입력 | 위치 | 비고 |
|------|------|------|
| 코어 JAR | `.\gradlew build` → `build/libs/realismsurvival-<ver>.jar` | 팩에 직접 포함 |
| `rs-medicine` | `addons/rs-medicine` 빌드 산출물 | 팩 내부 전용(D-01), 별도 공개 안 함 |
| 외부 모드 | manifest 참조 (해시 고정) | 6.7 재배포 열 기준 |
| config | `pack/config` | FTB Quests 포함 |
| 데이터팩 | `pack/datapacks/rs-item-bridges` | `sync_bridge_recipes.py --check` 통과 후에만 |
| 리소스팩 | 한국어 리소스팩 | 6.3 — 플랫폼이 기본 활성화를 지원하면 `options.txt` 기본값으로, 아니면 README·T0-02 안내 |

### 클라이언트팩 / 서버팩 차이

- 서버팩: 클라이언트 전용 모드(Sodium, 지도 클라이언트 등)와 리소스팩 제외. `pack/config` 중 클라이언트 설정 파일 제외.
- 클라이언트팩: 전체 + 기본 `options.txt`(언어 `ko_kr`, 리소스팩 순서).
- 두 팩 모두 같은 코어 JAR·데이터팩·서버 config를 쓴다.

### 업데이트 · 기존 월드 정책

- 퀘스트 ID·`mod_id`·SavedData 키는 마이너 업데이트에서 바꾸지 않는다 (`VERSIONING.md`).
- 업데이트 노트에 config 기본값 변경(예: `worldDrinkBottleFraction`, `cropGrowthMultiplierMax`, `supplyCounterEnabled`)을 적고, 기존 서버 TOML은 덮어쓰지 않는다.
- 데이터팩·퀘스트 갱신은 덮어쓰기. 플레이어 진행도(FTB 팀 데이터)는 건드리지 않는다.

### 완료 기준

클린 인스턴스에 manifest만으로 설치 → `REGRESSION_CHECKLIST.md` 8.1 통과 → 서버팩으로 전용 서버 기동 + 접속.

## 7.1 공개 API 계약 (D-02: 0.x 동안 facade 유지)

현재 `api` 패키지에서 구현 타입이 새는 지점:

| 지점 | 문제 | 계획 |
|------|------|------|
| `SurvivalApi.getData` / `setData` | `component.SurvivalData`(내부 attachment record) 노출 | `api.SurvivalSnapshot(thirst, temperature)` DTO 추가, 기존 두 메서드는 `@Deprecated(forRemoval)` 후 1.0에서 제거 |
| `SurvivalDataAccess` | `BiomeThirstProfile`·`DrinkDefinition`·`InsulationDefinition`(데이터 로더 record) 반환 | 읽기 전용 DTO 또는 필요한 값만 반환하는 메서드로 축소 |
| `EnvironmentContext` | 이미 `api.environment` | 유지, 필드 추가는 마이너 |
| 이벤트 (`api.event.*`) | 안정 | 유지. `ConvertVanillaWaterEvent` 취소는 "열린 바닐라 물 금지" 불변식 문서화 |

순서:

1. DTO와 새 메서드 추가 (마이너) → `ADDON_API.md`와 `rs-medicine`을 새 계약으로 옮긴다.
2. 옛 메서드에 deprecation + 전환 버전 표기.
3. 식별자 이관(6.7)과 같은 릴리스에서 공개한다. 외부 이용자가 아직 없으므로 deprecation 기간은 0.x 마이너 1회로 충분하다.

현재 저장소 안에서 `getData`/`setData`를 쓰는 코드는 없고, 문서(`ADDON_API.md`)만 언급한다.
