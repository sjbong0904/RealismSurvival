# rs-medicine

Realism Survival용 모드팩 의약품 애드온입니다. 야전에서 체력을 회복하고, 피해를 받을 때 짧은 상태이상을 부여하며, 개별 또는 일괄 치료 아이템을 제공합니다.

설계 설명은 [모드팩 설계 노트](../../docs/MODPACK.md)의 `rs-medicine` 항목을 참고하세요.

## 실행

모드는 항상 컴파일됩니다. `runClient`와 `runServer`에서는 로드하고 GameTest에서는 제외합니다.

```text
gradlew compileMedicineAddonJava
gradlew runClient
gradlew runClient -PmedicineAddon=false
```

## 아이템

| 아이템 | 효과 |
|---|---|
| 붕대 / 외상 키트 / 메디킷 | 체력 4 / 8 / 12 회복 (비례 회복 미적용) |
| 해독제 | 독 제거 |
| 화상 연고 | 불 제거 |
| 온열 팩 | 동상 및 둔화 제거 |
| 자극제 | 둔화 및 나약함 제거 |
| 안약 | 실명 / 어둠 / 멀미 / 공중부양 제거 |
| 야전 구급 키트 | 추적 중인 모든 상태이상 제거 (체력 회복 없음) |
| 진료소 키트 | 체력 10 회복 및 전체 치료 |

의약품을 사용하거나 마실 수 있는 물약을 마시면 5% 확률로 짧은 추가 효과(재생/저항/흡수/신속, 1–8초)가 생깁니다.

## 상태이상

대응하도록 지정된 몹 또는 번개로 피해를 입으면 상태이상이 생길 수 있습니다. 확률은 `pMax * (1 - 현재체력/최대체력)`이며 `pMax`는 최대 0.10입니다. 지속 시간은 1–8초입니다. 같은 효과가 이미 있으면 다시 부여하지 않습니다. 스켈레톤과 크리퍼는 추가 상태이상을 부여하지 않습니다.

## 텍스처 및 라이선스

- 아이템은 [Medicine Pack 16x16](https://opengameart.org/content/medicine-pack-16x16) (Kipperfalcon), [Pixel Potion Set](https://opengameart.org/content/pixel-potion-set-16x16) (yafarida)의 CC0 조각과 자체 아이콘을 사용합니다. 자세한 내용은 [THIRD_PARTY_ASSETS.md](THIRD_PARTY_ASSETS.md)를 참고하세요.
- `textures/mob_effect/*.png`는 등록된 `MedicineEffects`(독소, 부패, 피로, 허약, 한기, 충격, 화상)용입니다. 실명·공중부양 아이콘은 엔진 연동 때문에 바닐라 아이콘을 사용합니다.
