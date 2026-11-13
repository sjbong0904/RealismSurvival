"""Build 16x16 item PNGs from vanilla textures with light recolors."""
from __future__ import annotations

from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
VANILLA = ROOT / "build/tmp/mc-ice/assets/minecraft/textures/item"
OUT = ROOT / "src/main/resources/assets/realismsurvival/textures/item"


def load(name: str) -> np.ndarray:
    return np.array(Image.open(VANILLA / name).convert("RGBA"), dtype=np.float32)


def save(name: str, a: np.ndarray) -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    img = np.clip(a, 0, 255).astype(np.uint8)
    Image.fromarray(img, "RGBA").save(OUT / name)
    print("wrote", OUT / name)


def tint_luminance(a: np.ndarray, rgb: tuple[int, int, int]) -> np.ndarray:
    out = a.copy()
    m = out[..., 3] > 0
    lum = (0.299 * out[..., 0] + 0.587 * out[..., 1] + 0.114 * out[..., 2]) / 255.0
    for i, c in enumerate(rgb):
        out[..., i][m] = np.clip(lum[m] * c, 0, 255)
    return out


def shift(a: np.ndarray, dr: float = 0, dg: float = 0, db: float = 0, scale: float = 1.0) -> np.ndarray:
    out = a.copy()
    m = out[..., 3] > 0
    out[..., 0][m] = np.clip(out[..., 0][m] * scale + dr, 0, 255)
    out[..., 1][m] = np.clip(out[..., 1][m] * scale + dg, 0, 255)
    out[..., 2][m] = np.clip(out[..., 2][m] * scale + db, 0, 255)
    return out


def desaturate(a: np.ndarray, amount: float = 0.7) -> np.ndarray:
    out = a.copy()
    m = out[..., 3] > 0
    gray = 0.299 * out[..., 0] + 0.587 * out[..., 1] + 0.114 * out[..., 2]
    for i in range(3):
        out[..., i][m] = out[..., i][m] * (1 - amount) + gray[m] * amount
    return out


def alpha_over(dst: np.ndarray, src: np.ndarray) -> np.ndarray:
    out = dst.copy()
    sa = src[..., 3] / 255.0
    m = sa > 0.01
    for i in range(3):
        out[..., i][m] = out[..., i][m] * (1 - sa[m]) + src[..., i][m] * sa[m]
    out[..., 3][m] = np.maximum(out[..., 3][m], src[..., 3][m])
    return out


def make_thermometer() -> None:
    bottle = shift(load("glass_bottle.png"), dr=-8, dg=0, db=14, scale=0.96)
    mercury = tint_luminance(load("potion_overlay.png"), (205, 45, 38))
    mercury[..., 3] *= 0.95
    thermo = alpha_over(bottle, mercury)
    iron = load("iron_ingot.png")
    for y in range(2, 4):
        for x in range(6, 10):
            if thermo[y, x, 3] > 0:
                thermo[y, x, :3] = iron[8, 8, :3] * 0.8 + np.array([35, 35, 40])
                thermo[y, x, 3] = 255
    save("thermometer.png", thermo)


def make_salt() -> None:
    salt = desaturate(load("sugar.png"), amount=0.88)
    salt = shift(salt, dr=10, dg=12, db=22, scale=1.0)
    m = salt[..., 3] > 0
    salt[..., :3][m] = np.clip(salt[..., :3][m] * 1.14 + 14, 0, 255)
    save("salt.png", salt)


def make_ice_bucket_textures() -> None:
    powder = load("powder_snow_bucket.png")
    bucket = load("bucket.png")
    overlay = np.zeros_like(powder)
    for y in range(16):
        for x in range(16):
            pa = powder[y, x]
            ba = bucket[y, x]
            if pa[3] < 8:
                continue
            if ba[3] > 180 and abs(pa[:3] - ba[:3]).sum() < 55:
                continue
            lum = 0.299 * pa[0] + 0.587 * pa[1] + 0.114 * pa[2]
            if lum < 70:
                continue
            # White multiply mask so item tints (WaterColors) read clearly.
            overlay[y, x] = (255, 255, 255, min(255, pa[3]))
    save("ice_bucket_overlay.png", overlay)
    save("ice_bucket.png", shift(powder, dr=-12, dg=6, db=28, scale=0.96))


def main() -> None:
    if not VANILLA.is_dir():
        raise SystemExit(f"Vanilla textures missing: {VANILLA}")
    make_thermometer()
    make_salt()
    make_ice_bucket_textures()


if __name__ == "__main__":
    main()
