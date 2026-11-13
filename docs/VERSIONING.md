# 버전 관리와 배포

## 모드 JAR 버전 규칙 (SemVer)

| 구분 | 적용 시점 |
|---|---|
| **메이저** | `api.**` 제거/이름 변경/동작 호환성 파괴, 데이터팩 스키마 파괴, 애드온을 깨는 태그 의미 변경 |
| **마이너** | 새 API 타입/메서드, 선택 가능한 태그·이벤트, 기본값이 있는 데이터팩 필드 추가 |
| **패치** | 버그 수정, 밸런스 변경, 고정 API 범위를 바꾸지 않는 콘텐츠 변경 |

기존 JAR에 포함된 필터·증류기, 작물 조명, Curios 슬롯 데이터팩은 포함된 동안 마이너 버전에서 변경될 수 있다. 이들은 고정 API가 아니다. 가능하면 태그와 데이터팩을 통해 확장한다.

## 현재 Maven 좌표

| 항목 | 값 | 비고 |
|---|---|---|
| `group` | `com.example.realismsurvival` | 공개 출시 전 자리표시자 |
| `archivesName` / artifact | `realismsurvival` | `mod_id` |
| `version` | `gradle.properties`의 `mod_version` | 현재는 `0.1.0`부터 시작 (0.x에서는 API 범위가 조정될 수 있음) |

로컬 Maven 저장소에 게시:

```text
gradlew publishToMavenLocal
```

산출 좌표: `com.example.realismsurvival:realismsurvival:<version>`.

애드온 Gradle 의존성 예시:

```gradle
dependencies {
    compileOnly "com.example.realismsurvival:realismsurvival:${rs_version}"
    localRuntime "com.example.realismsurvival:realismsurvival:${rs_version}"
}
```

## 첫 공개 배포 전

1. 최종 Maven group과 Modrinth 슬러그를 정하고 `com.example`을 교체한다.
2. 릴리스 버전으로 Git 태그를 만든다.
3. JAR와 소스를 게시하고 애드온 제작자에게 [애드온 API](ADDON_API.md)를 안내한다.
4. 해당 태그에서 GameTest가 통과하는지 확인한다.
