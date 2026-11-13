from pathlib import Path

COLORS = {
    "distilled": 0xB8E8FF,
    "fresh": 0x385DC6,
    "stagnant": 0x6B7A32,
    "salt": 0x1F6B8C,
}

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources/assets/realismsurvival/items"
TEMPLATE = """{{
  "model": {{
    "type": "minecraft:model",
    "model": "realismsurvival:item/graded_water_bottle",
    "tints": [
      {{
        "type": "realismsurvival:graded_water",
        "base": {color}
      }}
    ]
  }}
}}
"""

for name, color in COLORS.items():
    (ROOT / f"{name}_water_bottle.json").write_text(TEMPLATE.format(color=color), encoding="utf-8")
    print("wrote", name, color)
