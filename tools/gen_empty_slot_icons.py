"""Generate monochrome empty-slot sprites from item pixel art (vanilla empty-slot style)."""
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SRC_MC = ROOT / "build/tmp/mc-ice/assets/minecraft/textures/item"
SRC_MOD = ROOT / "src/main/resources/assets/realismsurvival/textures/item"
OUT = ROOT / "src/main/resources/assets/realismsurvival/textures/gui/sprites/container/slot"


def to_empty_slot_icon(img: Image.Image) -> Image.Image:
    img = img.convert("RGBA")
    if img.size != (16, 16):
        img = img.resize((16, 16), Image.NEAREST)
    out = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x in range(16):
        for y in range(16):
            r, g, b, a = img.getpixel((x, y))
            if a < 16:
                continue
            lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
            v = int(55 + (1.0 - lum) * 35)
            aa = min(255, max(180, a))
            out.putpixel((x, y), (v, v, v, aa))
    return out


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    jobs = {
        "water_bucket": SRC_MC / "water_bucket.png",
        "bucket": SRC_MC / "bucket.png",
        "glass_bottle": SRC_MC / "glass_bottle.png",
        "coal": SRC_MC / "coal.png",
        "salt": SRC_MOD / "salt.png",
        "water_bottle": SRC_MC / "potion.png" if (SRC_MC / "potion.png").exists() else SRC_MC / "glass_bottle.png",
    }
    for name, path in jobs.items():
        icon = to_empty_slot_icon(Image.open(path))
        dest = OUT / f"{name}.png"
        icon.save(dest)
        print(f"wrote {dest}")


if __name__ == "__main__":
    main()
