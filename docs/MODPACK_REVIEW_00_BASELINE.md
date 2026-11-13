# 전수 검토 00 — 기준선·구성 재현성

검토일: 2026-10-21  
검토 범위: 프로젝트 파일과 문서에 드러난 개발·팩 구성의 기준선. 기능 품질, 밸런스, 실제 플레이는 이번 회차에서 판정하지 않는다.

전체 회차 현황 및 수정 방향 논의 상태: [전수 검토 진행 기록](MODPACK_REVIEW_LOG.md).

## 검토 결론

현재 코드베이스에서 **개발 런타임의 기본 조합은 상당 부분 재현 가능**하지만, **검토 기준으로 삼을 정식 배포 팩은 고정되어 있지 않다**. 따라서 후속 검토에서 “모드팩 전체”라고 할 때 기준 조합을 먼저 선언해야 한다. 이번 검토의 임시 기준은 `runClient` 기본 구성으로 두고, 실제 배포 구성이 정해지면 교체하거나 별도 기준선으로 추가하는 것을 권한다.

## 확인된 기준선

| 항목 | 현재 확인값 | 근거 |
|---|---|---|
| Minecraft | 26.1.2 | `gradle.properties`의 `minecraft_version` |
| NeoForge | 26.1.2.122 | `gradle.properties`의 `neo_version` |
| Java 컴파일 도구체인 | Java 25 지정 | `build.gradle`의 `java.toolchain.languageVersion` |
| 현재 셸의 Java/Gradle 실행기 | Java 21.0.5 / Gradle Wrapper 9.2.1 | 이번 검토 환경의 `java -version`, `gradlew --version`. Java 25는 프로젝트 도구체인으로 지정되며 Gradle 캐시에 배포 아카이브가 있으나, 실제 빌드에서 선택되는지는 이번에 검증하지 않음 |
| RS 본체 | modid `realismsurvival`, 버전 0.1.0 | `gradle.properties`, 생성 대상 `neoforge.mods.toml` |
| 의약품 애드온 | `addons/rs-medicine`, modid `rsmedicine`, 별도 소스셋·런타임 토글 | `build.gradle`, 애드온 메타데이터 |
| 개발 런타임 그룹 | `clientMods`, Curios, Farmer's Delight, fluid stack, QoL, pack content, medicine addon, shop stack 등이 Gradle 속성에 따라 켜짐. 기본 `runClient`는 대부분 켜짐, GameTest 실행은 다수 그룹을 끔 | `build.gradle`, `gradle.properties` |
| 팩 데이터 위치 | 설정·퀘스트·상점·브리지 데이터는 `pack/` 아래, 일부 브리지 레시피는 RS JAR 소스 `src/main/resources/data/rsbridges/recipe/`에도 있음 | 저장소 파일 구조, `docs/MODPACK.md`, `pack/datapacks/rs-item-bridges/INVENTORY.md` |
| 한국어 리소스팩 | `pack/resourcepacks/rs-korean`; 생성 스크립트가 `run/resourcepacks`에도 복사 | `scripts/build_korean_resourcepack.py`, `docs/KOREAN_LOCALES.md` |
| 로컬 런타임 모드 JAR | Q Shop, SG Economy, Easy NPC, Oritech 및 라이브러리 등이 `libs/`에 고정 파일로 존재 | `libs/`, `build.gradle` |
| 현재 빌드 산출물 | `build/libs/realismsurvival-0.1.0.jar` 확인 | 검토 시점 디렉터리 내용 |

`run/mods/`는 비어 있다. 이 개발 프로젝트는 Gradle `localRuntime` 클래스패스로 모드를 불러오므로 이 폴더를 설치형 모드팩의 실제 모드 목록으로 볼 수 없다. `run/`에는 설정·월드·로그 등 로컬 실행 상태도 있으므로, 그것만으로 배포 설정이나 새 설치 기본값을 확정할 수 없다.

## 기준선을 막는 불일치와 공백

### P1 — 정식 배포 조합과 산출 절차가 정의되지 않음

`build.gradle`은 RS 모드 JAR을 빌드하고 여러 외부 모드를 `localRuntime`으로 개발 실행에 추가한다. `pack/`의 설정·데이터팩·리소스팩과 외부 모드 JAR을 하나의 배포 가능한 인스턴스/팩으로 묶는 작업은 확인하지 못했다. `build/libs`에도 RS 본체 JAR만 있다.

영향: 후속 검토의 기준이 개발용 기본 실행인지, 실제 플레이어가 설치할 배포본인지 불명확하다. 현재는 두 구성의 모드 목록·설정 기본값·리소스 적용 상태가 동일하다고 단정할 수 없다.

### P1 — 문서 안에서 모드 포함 여부가 서로 다름

- **When Dungeons Arise (WDA):** `build.gradle` / `gradle.properties`는 26.1 런타임 구성에서 제외한다고 명시하며, 실제 소스·퀘스트 데이터에도 WDA 모드 ID가 없다. `docs/MODPACK.md`의 후반 퀘스트·탐험 계획은 이를 포함하므로 현재 구현과 혼동되지 않게 계획으로 표시할 필요가 있다.
- **Biomes O' Plenty (BoP):** `climate_arid/cold/ocean/temperate/tropical.json` 태그와 `salt_water.json`, `stagnant_water.json`에 일부 BoP 바이옴 ID가 등록되어 있다. 갈증 데이터는 기후 태그를 참조한다. 따라서 BoP 연동 자체는 존재한다. 다만 전체 바이옴을 모두 분류했는지는 확인하지 않았다. `docs/MODPACK.md`의 “데이터팩 아직” 설명은 구현과 맞지 않고, 후반의 “태그 반영”은 적용 여부에 대해서는 맞다.

영향: WDA는 현재 개발 기준선에서 제외된 구성으로 취급하고, 관련 챕터/탐험은 계획으로 분리한다. BoP 연결은 구현된 항목이 있으나 전수 커버리지는 02/09 관점에서 확인한다.

### P2 — 버전 고정 방식이 여러 형태로 섞여 있음

모드 버전은 일반 문자열 버전, Modrinth version ID, CurseForge 프로젝트/파일 번호, `libs/`의 직접 포함 JAR 파일명으로 관리된다. 특히 로컬 JAR은 파일명만으로 원 출처·체크섬·라이선스·업데이트 절차를 한눈에 확인하기 어렵다. 세부 공급망 검토는 13회차 범위지만, 기준선 목록에는 각 식별자를 보존해야 한다.

### P2 — JAR과 팩 데이터의 책임 경계 확인 필요

아이템 브리지 레시피 82개는 `src/main/resources/data/rsbridges/recipe/`와 `pack/datapacks/rs-item-bridges/data/rsbridges/recipe/`에 같은 파일명·내용으로 있음을 해시로 확인했다. 문서는 두 경로를 모두 설명한다. 현재 동기화되어 있지만 수정 시 정본과 동기화 절차를 명시할 필요가 있다. 설정·퀘스트는 `pack/`에서 생성·복사 도구를 거쳐 `run/config`로 들어가는 별도 흐름이 있다.

영향: 중복 자체가 즉시 결함이라는 뜻은 아니지만, 어느 파일이 정본인지 기준을 정하지 않으면 서로 다른 내용으로 배포될 위험이 있다.

## 기준선 확정을 위한 권장 진행 방향

후속 관점 검토는 아래처럼 두 기준을 분리해 기록하는 편이 안전하다.

1. **개발 기준선:** 이번 검토에서는 `runClient`의 기본 Gradle 프로필을 임시 기준선으로 사용한다. `runServer -PclientMods=false`와 GameTest 프로필은 별도 환경으로 표시하며, GameTest 구성으로 전체 팩 모드 목록을 대표하지 않는다.
2. **배포 기준선:** 정식 배포용 프로필/산출 절차가 아직 없는 것으로 보이므로, 배포본 검토는 “미확정”으로 표시한다. 새 배포 인스턴스에서 실제 파일과 기본 설정을 확보한 뒤 별도의 고정 기준을 만든다.
3. **모드 포함 여부 정리:** WDA는 현재 개발 기준선에서 제외되고 문서 후반부 계획에만 남아 있음을 확인했다. BoP 기후·수역 태그는 일부 구현되며 전수 커버리지는 02/09 관점에서 확인한다. 설계 문서 표현 갱신은 13번 회차로 넘긴다.
4. **정본 경로 기록:** 퀘스트, 설정, 브리지 레시피, 한국어 오버레이마다 편집 원본과 런타임 복사/패키징 경로를 기록한다.

## 이번 회차 상태와 다음 논의

- 완료: 저장소 문서, Gradle 속성/의존성 선언, 주요 구성 디렉터리, JAR 산출물 위치를 확인했다.
- 미확인: 의존성 해석을 통한 전체 모드 ID/버전 덤프, 새 월드에서 데이터팩 활성 여부, Java 25 도구체인의 실제 선택 여부, 정식 배포 인스턴스, BoP 바이옴 분류 전수 커버리지.
- 실행하지 않은 항목: 빌드·GameTest·클라이언트/서버 실행. 이번 요청은 검토 기준선 확인이며 새 검증 실행은 포함하지 않았다.
- 관점 밖 대기 항목: 로그의 리소스/모델 경고는 UX·자산 검토 회차에서 따로 분류한다.

**현재 진행 방향:** 사용자가 순차 검토를 계속하라고 지시했으므로 개발 `runClient` 기본 구성을 임시 기준선으로 삼아 01번부터 진행한다. 정식 배포 조합은 프로필/실제 인스턴스가 특정되기 전까지 미확정으로 두고 13번에서 다시 다룬다.
