# rs-api-sample

Minimal addon that proves the frozen Realism Survival API (`docs/ADDON_API.md`):

1. Registers a fifth `WaterQuality` (`mineral`) — no new fluid.
2. Bucket + bottle via `GradedWaterItems`.
3. `drink` datapack for the bottle.
4. `PlayerDrinkEvent` bonus when drinking mineral water.
5. Adds the bucket to `#realismsurvival:still_input_buckets`.

## Build / run (from repo root)

Not loaded in normal `runClient`. Opt in only when testing the public API:

```text
gradlew compileAddonExampleJava
gradlew runClient -PsampleAddon=true
```

Creative: buckets/bottles are not auto-listed in RS tabs — use `/give @s rsnapisample:mineral_water_bottle` (or add your own tab). Graded ice of this quality appears under Natural Blocks when the sample is loaded.
