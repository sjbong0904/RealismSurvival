"""One-shot balance retune for drink/*.json (realistic thirst / mild temp)."""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DRINK = ROOT / "src/main/resources/data/realismsurvival/realismsurvival/drink"

# 0–100 thirst; temperate drain ≈ 0.05/s (~3/min). Temps are mild toward-normal pulls.
UPDATES: dict[str, tuple[float, float]] = {
    # Tooltip icons: points = round(|thirst|/5); full droplets = points/2 → 40→4, 20→2.
    "distilled_water_bottle.json": (40.0, -0.4),
    "fresh_water_bottle.json": (20.0, -0.25),
    "stagnant_water_bottle.json": (20.0, -0.2),  # same thirst as fresh; sickness is the cost
    "salt_water_bottle.json": (-14.0, -0.1),
    "potion.json": (8.0, -0.25),
    "raw_water.json": (6.0, -0.2),
    "milk_bucket.json": (12.0, -0.15),
    "honey_bottle.json": (6.0, 0.1),
    "soups_stews.json": (12.0, 0.35),
    "juicy_food.json": (6.0, -0.15),
    "fresh_produce.json": (3.0, 0.0),
    "golden_carrot.json": (4.0, 0.0),
    "golden_treats.json": (8.0, 0.0),
    "dry_starch.json": (-3.0, 0.0),
    "baked_potato.json": (-2.0, 0.2),
    "pumpkin_pie.json": (-2.0, 0.1),
    "raw_protein.json": (-2.0, 0.0),
    "raw_fish.json": (-1.0, -0.05),
    "cooked_protein.json": (-3.5, 0.2),
    "hazardous_food.json": (-5.0, 0.0),
    "dried_kelp.json": (-6.0, 0.0),
    "distilled_ice_bucket.json": (40.0, -0.9),
    "fresh_ice_bucket.json": (20.0, -0.9),
    "stagnant_ice_bucket.json": (20.0, -0.9),
    "fd_drinks.json": (14.0, 0.05),
    "fd_soups.json": (12.0, 0.35),
    "fd_salads_fruit.json": (7.0, -0.2),
    "fd_meals.json": (3.0, 0.15),
    "fd_desserts.json": (2.0, 0.05),
    "fd_dry.json": (-3.0, 0.0),
}


def main() -> None:
    for name, (thirst, temp) in UPDATES.items():
        path = DRINK / name
        data = json.loads(path.read_text(encoding="utf-8"))
        data["thirst_restore"] = thirst
        data["temperature_delta"] = temp
        path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        print(f"updated {name}: thirst={thirst} temp={temp}")
    print("done", len(UPDATES))


if __name__ == "__main__":
    main()
