# Realism Survival

NeoForge **26.1.2**용 생존 모드/모드팩 프로젝트입니다. 갈증, 체온, 물 품질을 데이터팩으로 조정하며 Sodium/Iris 환경에서도 HUD가 동작하도록 설계했습니다. 음식 툴팁의 허기·포만도 아이콘에는 클라이언트에 [AppleSkin](https://modrinth.com/mod/appleskin)이 필요합니다. RS는 갈증 방울과 체온 정보를 표시합니다.

| 항목 | 값 |
|---|---|
| 모드 ID | `realismsurvival` |
| Java 패키지 | `com.example.realismsurvival` |
| 기술 명세 | [REALISM_SURVIVAL_SPEC.md](REALISM_SURVIVAL_SPEC.md) |
| 모드팩 구성 | [docs/MODPACK.md](docs/MODPACK.md) |
| 모드 설명집 | [docs/MOD_CATALOG.md](docs/MOD_CATALOG.md) |
| 퀘스트 전체 표 (생성 스냅샷) | [docs/QUEST_LIST.md](docs/QUEST_LIST.md) |
| 공급소 상점·매입 계산대 | [docs/SHOP_SUPPLY_POST.md](docs/SHOP_SUPPLY_POST.md) |
| 음식·물·자원·보상 흐름 | [docs/MODPACK_ECONOMY_FLOW.md](docs/MODPACK_ECONOMY_FLOW.md) |
| Mixin 정책 | [docs/MIXIN_POLICY.md](docs/MIXIN_POLICY.md) |
| 회귀 확인표 | [docs/REGRESSION_CHECKLIST.md](docs/REGRESSION_CHECKLIST.md) |
| 패치 결정·진행 기록 | [docs/MODPACK_PATCH_STAGE0.md](docs/MODPACK_PATCH_STAGE0.md) |
| 공개 배포 준비 (식별자·manifest·API) | [docs/MODPACK_RELEASE_PREP.md](docs/MODPACK_RELEASE_PREP.md) |

## 모드팩 설치 시 한국어

게임 언어를 **한국어(대한민국)** 로 두고, 리소스 팩에서 **rs-korean**을 맨 위로 활성화합니다 ([docs/KOREAN_LOCALES.md](docs/KOREAN_LOCALES.md)). 퀘스트북은 언어와 관계없이 한국어로 표시됩니다.

## 공급소 이용

상인 **우클릭** = 구매(Q Shop), **웅크리고 우클릭** = 매입 계산대. 서버가 견적을 계산하고 확정 시 다시 검증합니다.

## 실행

```powershell
.\gradlew runClient    # 게임 실행 (localRuntime Sodium/Iris 포함)
.\gradlew runServer    # 전용 서버 실행
.\gradlew build
```

## 운영자 디버그 명령

```text
/realism get
/realism set thirst <0..100>
/realism set temperature <30..42>
```

체온 HUD는 체력 위에 온도 범위를, 갈증 HUD는 허기 위에 물방울을 표시합니다. 체온은 주변 온도와 이동·수영·달리기 활동에 따라 변하고, 찬물에 오래 잠기면 체온이 내려갑니다. 물에 잠기면 물 등급에 따라 갈증이 회복되며, 잠수 중에는 더 빠르게 회복됩니다. 갈증 부족·저체온·고체온에는 단계별 약화 효과가 적용되고 극단적인 상태는 피해를 줍니다. 자연 회복은 갈증과 체온 상태에 따라 제한됩니다.

음식·음료의 갈증 및 체온 변화는 `data/.../drink/*.json` 데이터로 조정합니다. 월드에서 뜬 물은 민물·고인물·바닷물 등급을 가지며, 끓여 증류하거나 물통·유리병으로 옮길 수 있습니다. 자세한 수치와 동작은 기술 명세를 참고하세요.

## 개발 참고

매핑 이름은 Mojang 공식 이름을 사용합니다. [NeoForm Mojang 매핑 설명](https://github.com/NeoForged/NeoForm/blob/main/Mojang.md), [NeoForge 문서](https://docs.neoforged.net/).
