"""Rewrite graded water bucket item defs: untinted bucket + tinted liquid overlay."""
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
    "model": "realismsurvival:item/graded_water_bucket",
    "tints": [
      {{
        "type": "minecraft:constant",
        "value": -1
      }},
      {{
        "type": "minecraft:constant",
        "value": {color}
      }}
    ]
  }}
}}
"""

for name, color in COLORS.items():
    path = ROOT / f"{name}_water_bucket.json"
    path.write_text(TEMPLATE.format(color=color), encoding="utf-8")
    print("wrote", path.name, color)
