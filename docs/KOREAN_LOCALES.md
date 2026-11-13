# 팩 모드 한글 로케일

스캔 결과 미니맵/월드맵·상점·퀘스트 UI 등 여러 모드에 `ko_kr`이 없거나 불완전했다.  
`pack/lang-ko/`에 네임스페이스별 오버레이를 두고, 리소스팩으로 합친다.

## 사용법

1. `python scripts/build_korean_resourcepack.py` — `pack/resourcepacks/rs-korean` 생성 (`run/resourcepacks`에도 복사)
2. 게임 **설정 → 리소스 팩 → rs-korean** 활성화 (맨 위로)
3. 언어를 **한국어(대한민국)** 로 둔다

배포 시에는 `pack/resourcepacks/rs-korean`을 인스턴스 `resourcepacks`에 넣고 기본 활성화하면 된다.

### 기본 적용 안내 (패치 6.3)

배포 플랫폼이 미정(D-13)이라 "기본 활성화"는 플랫폼 결정 후 manifest에서 처리한다. 그 전까지:

1. **인스턴스 기본값**: 배포 zip에 `options.txt`를 넣을 수 있으면 `lang:ko_kr`과 `resourcePacks:["vanilla","file/rs-korean"]`을 미리 넣는다 (기존 사용자 `options.txt`는 덮어쓰지 않는다).
2. **안내 경로**: README 설치 절차와 퀘스트 `T0-01`/`T0-02` 설명에 "언어 한국어 + 리소스 팩 rs-korean 맨 위" 단계를 둔다.
3. **검수**: 리소스팩 활성화 + 게임 언어 `ko_kr` 상태에서 퀘스트북·JEI·Jade·FTB Chunks 맵·상점 화면을 함께 본다 (`docs/REGRESSION_CHECKLIST.md` 8.7).

퀘스트 문구는 리소스팩이 아니라 FTB Quests `lang/ko_kr`(정본)에서 나오며, 다른 언어 클라이언트도 `fallback_locale = ko_kr`로 한국어가 보인다 (D-04).

## 커버리지 점검 (패치 6.2)

```powershell
.\gradlew writeRuntimeJarList                  # 배포 프로필 runtimeClasspath → build/reports/runtime-jars.txt
python tools/scan_missing_ko_kr.py --fail-on-missing
```

- 입력은 실제 프로필 jar 목록(+ `libs/*.jar`). 목록이 없거나 jar가 사라졌으면 **종료 코드 2**로 즉시 실패한다.
- RS 자체(`realismsurvival`, `rsmedicine`)의 en_us 키가 ko_kr에 없으면 항상 **종료 코드 1**.
- `--fail-on-missing`이면 타 모드 게임플레이 키 누락도 실패. 보고서: `build/reports/locale/`.
- 이 점검은 **키 완전성**만 본다. 번역이 자연스러운지는 아래 검토 상태로 따로 관리한다.

| 검토 상태 | 대상 |
|-----------|------|
| 키 완전 (자동) | 2026-10-21 스캔: 179 jar, 누락 0 (aquaculture jar ko_kr 손상 → 오버레이로 보완) |
| 자연스러움 검토 | 2026-10-22: `docs/KO_STYLE_GUIDE.md` 기준으로 RS 본체·rs-medicine·퀘스트 lang·`pack/lang-ko` 전수 검수. 정본은 overlay. jar 복사본은 `scripts/sync_ko_mirrors.py`로 맞춤. 공식 번역과 용어가 충돌하면 overlay가 덮음 (웨이스톤, 경비병, FTB Chunks 채도). |

## 유지보수

| 경로 | 역할 |
|------|------|
| `pack/lang-ko/<namespace>.json` | 손/자동 번역 소스 (품질 수정은 여기). 정본 |
| `src/main/resources/assets/<ns>/lang/ko_kr.json` | overlay 미러. 직접 고치지 말고 `scripts/sync_ko_mirrors.py` |
| `docs/KO_STYLE_GUIDE.md` | 문체·용어집 |
| `tools/audit_ko_quality.py` | 용어집/번역투/플레이스홀더 기계 점검 |
| `tools/scan_missing_ko_kr.py` | 배포 프로필 jar `en_us` vs `ko_kr` 커버리지 (위 6.2) |
| `scripts/scan_mod_locales.py` | (구) 모드 jar `en_us` vs `ko_kr` 스캔 |
| `scripts/verify_lang_ko.py` | 오버레이 커버리지 확인 |
| `scripts/fill_partial_lang_ko.py` | jar에 일부만 있는 모드의 빈 키 채우기 |
| `scripts/sync_ko_mirrors.py` | overlay → jar 복사본 동기화 |
| `scripts/build_korean_resourcepack.py` | jar 기존 KO + 오버레이 → 리소스팩 |

FTB Chunks 공식 `ko_kr`은 일부만 있어, 빠진 키와 오역(채도 등)은 `pack/lang-ko/ftbchunks.json`이 덮어씁니다. 구 Xaero/OPC 오버레이는 리소스팩 빌드 대상에서 뺐습니다.
