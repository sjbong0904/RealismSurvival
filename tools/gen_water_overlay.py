"""Build a grayscale liquid-only overlay from vanilla water_bucket vs empty bucket."""
from PIL import Image
import os
from pathlib import Path

tmp = os.environ["TEMP"]
water = Image.open(Path(tmp) / "mc_water_bucket.png").convert("RGBA")
bucket = Image.open(Path(tmp) / "mc_bucket.png").convert("RGBA")

w, h = water.size
out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
wp, bp, op = water.load(), bucket.load(), out.load()

for y in range(h):
    for x in range(w):
        wr, wg, wb, wa = wp[x, y]
        if wa < 200:
            continue
        # Keep only clearly blue liquid pixels from the filled bucket texture.
        if wb > wr + 20 and wb > wg + 10:
            # Near-white grayscale so constant tint multiplies cleanly.
            op[x, y] = (220, 220, 220, 255)

out_dir = Path(__file__).resolve().parents[1] / "src/main/resources/assets/realismsurvival/textures/item"
out_dir.mkdir(parents=True, exist_ok=True)
path = out_dir / "water_bucket_overlay.png"
out.save(path)
count = sum(1 for y in range(h) for x in range(w) if op[x, y][3] > 0)
print(f"saved {path} opaque={count}")
