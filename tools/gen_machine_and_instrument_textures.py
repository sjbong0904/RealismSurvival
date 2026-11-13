"""Generate 16x16 machine / instrument PNGs from vanilla textures (MIT/vanilla assets)."""
from __future__ import annotations

from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
VANILLA_ITEM = ROOT / "build/tmp/mc-ice/assets/minecraft/textures/item"
VANILLA_BLOCK = ROOT / "build/tmp/mc-ice/assets/minecraft/textures/block"
OUT_ITEM = ROOT / "src/main/resources/assets/realismsurvival/textures/item"
OUT_BLOCK = ROOT / "src/main/resources/assets/realismsurvival/textures/block"
OUT_HUD = ROOT / "src/main/resources/assets/realismsurvival/textures/gui/sprites/hud"


def load(folder: Path, name: str) -> np.ndarray:
    return np.array(Image.open(folder / name).convert("RGBA"), dtype=np.float32)


def save(folder: Path, name: str, a: np.ndarray) -> None:
    folder.mkdir(parents=True, exist_ok=True)
    img = np.clip(a, 0, 255).astype(np.uint8)
    Image.fromarray(img, "RGBA").save(folder / name)
    print("wrote", folder / name)


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


def alpha_over(dst: np.ndarray, src: np.ndarray) -> np.ndarray:
    out = dst.copy()
    sa = src[..., 3] / 255.0
    m = sa > 0.01
    for i in range(3):
        out[..., i][m] = out[..., i][m] * (1 - sa[m]) + src[..., i][m] * sa[m]
    out[..., 3][m] = np.maximum(out[..., 3][m], src[..., 3][m])
    return out


def draw_rect(a: np.ndarray, x0: int, y0: int, x1: int, y1: int, rgba: tuple[int, int, int, int]) -> None:
    a[y0:y1, x0:x1] = rgba


def px(a: np.ndarray, x: int, y: int, rgba: tuple[int, int, int, int]) -> None:
    if 0 <= x < 16 and 0 <= y < 16:
        a[y, x] = rgba


def make_thermometer() -> None:
    """Classic glass stem thermometer: brass tip, tube with scale, red mercury bulb."""
    t = np.zeros((16, 16, 4), dtype=np.float32)
    glass_hi = (220, 235, 245, 255)
    glass_mid = (160, 190, 210, 255)
    glass_lo = (90, 120, 145, 255)
    mercury = (200, 36, 32, 255)
    mercury_hi = (240, 70, 55, 255)
    mercury_lo = (140, 20, 18, 255)
    brass = (196, 150, 58, 255)
    brass_dk = (140, 100, 36, 255)
    brass_hi = (230, 190, 90, 255)
    scale = (55, 60, 70, 220)

    # Brass tip / hanging eye
    px(t, 7, 0, brass_hi)
    px(t, 8, 0, brass)
    for x in (6, 7, 8, 9):
        px(t, x, 1, brass if x in (7, 8) else brass_dk)
    px(t, 7, 2, brass)
    px(t, 8, 2, brass_dk)

    # Glass tube (vertical)
    for y in range(3, 11):
        px(t, 6, y, glass_lo)
        px(t, 7, y, glass_hi)
        px(t, 8, y, glass_mid)
        px(t, 9, y, glass_lo)

    # Mercury column inside tube (reads mid-high)
    for y in range(5, 11):
        px(t, 7, y, mercury_hi if y % 2 == 0 else mercury)
        px(t, 8, y, mercury if y % 2 == 0 else mercury_lo)

    # Scale ticks on left of tube
    for y in (3, 5, 7, 9):
        px(t, 5, y, scale)

    # Round mercury bulb
    bulb = [
        (6, 11), (7, 11), (8, 11), (9, 11),
        (5, 12), (6, 12), (7, 12), (8, 12), (9, 12), (10, 12),
        (5, 13), (6, 13), (7, 13), (8, 13), (9, 13), (10, 13),
        (6, 14), (7, 14), (8, 14), (9, 14),
        (7, 15), (8, 15),
    ]
    for x, y in bulb:
        # Highlight upper-left of bulb
        if (x, y) in ((6, 12), (7, 12), (6, 13)):
            px(t, x, y, mercury_hi)
        elif (x, y) in ((9, 13), (10, 13), (9, 14), (8, 15)):
            px(t, x, y, mercury_lo)
        else:
            px(t, x, y, mercury)
    # Glass rim around bulb
    for x, y, c in (
        (5, 11, glass_mid), (10, 11, glass_lo),
        (4, 12, glass_lo), (11, 12, glass_lo),
        (4, 13, glass_mid), (11, 13, glass_lo),
        (5, 14, glass_mid), (10, 14, glass_lo),
        (6, 15, glass_lo), (9, 15, glass_lo),
    ):
        px(t, x, y, c)
    # Specular glint on glass tube
    px(t, 7, 4, (245, 250, 255, 255))
    save(OUT_ITEM, "thermometer.png", t)


def make_filter_side(active: bool) -> np.ndarray:
    iron = load(VANILLA_BLOCK, "iron_block.png")
    copper = shift(load(VANILLA_BLOCK, "copper_block.png"), dr=-10, dg=20, db=40, scale=0.9)
    side = iron.copy()
    # Frame
    side = shift(side, dr=-15, dg=-10, db=5, scale=0.92)
    # Copper grate inset
    for y in range(3, 13):
        for x in range(3, 13):
            if (x + y) % 2 == 0:
                side[y, x] = copper[y, x]
            else:
                side[y, x, :3] = np.clip(copper[y, x, :3] * 0.65, 0, 255)
                side[y, x, 3] = 255
    # Water/filter accent
    accent = (70, 170, 220, 255) if not active else (90, 220, 255, 255)
    for y in range(5, 11):
        for x in range(5, 11):
            if (x + y) % 3 == 0:
                side[y, x] = accent if active else (55, 130, 170, 255)
    if active:
        # Glow rim
        for y in range(2, 14):
            side[y, 2, :3] = np.clip(side[y, 2, :3] * 0.5 + np.array([40, 160, 220]), 0, 255)
            side[y, 13, :3] = np.clip(side[y, 13, :3] * 0.5 + np.array([40, 160, 220]), 0, 255)
    return side


def make_filter_top() -> np.ndarray:
    iron = shift(load(VANILLA_BLOCK, "iron_block.png"), dr=-20, dg=-5, db=15, scale=0.9)
    top = iron.copy()
    # Hopper-like inlet
    for y in range(4, 12):
        for x in range(4, 12):
            dist = abs(x - 7.5) + abs(y - 7.5)
            if dist < 5:
                top[y, x, :3] = np.clip(top[y, x, :3] * 0.55 + np.array([30, 90, 130]), 0, 255)
    draw_rect(top, 6, 6, 10, 10, (40, 80, 110, 255))
    return top


def make_still_side(lit: bool) -> np.ndarray:
    copper = shift(load(VANILLA_BLOCK, "copper_block.png"), dr=10, dg=-5, db=-20, scale=0.95)
    furnace = load(VANILLA_BLOCK, "furnace_side.png")
    side = alpha_over(shift(furnace, dr=30, dg=10, db=-15, scale=0.85), tint_luminance(copper, (180, 110, 70)))
    # Window
    for y in range(5, 11):
        for x in range(5, 11):
            if lit:
                side[y, x] = (255, 180, 60, 255) if (x + y) % 2 == 0 else (220, 120, 30, 255)
            else:
                side[y, x] = (40, 50, 70, 255)
    # Condenser coil hint
    for x in range(3, 13):
        side[12, x, :3] = np.clip(side[12, x, :3] * 0.7 + np.array([60, 140, 180]), 0, 255)
    return side


def make_still_front(lit: bool) -> np.ndarray:
    front = make_still_side(lit)
    # Door seam
    for y in range(2, 14):
        front[y, 7, :3] = np.clip(front[y, 7, :3] * 0.6, 0, 255)
    if lit:
        for y in range(6, 10):
            for x in range(6, 10):
                front[y, x, :3] = np.clip(front[y, x, :3] * 0.4 + np.array([255, 200, 80]), 0, 255)
    return front


def make_still_top() -> np.ndarray:
    copper = shift(load(VANILLA_BLOCK, "copper_block.png"), dr=5, dg=-10, db=-15, scale=0.9)
    top = copper.copy()
    # Chimney / spout
    draw_rect(top, 6, 2, 10, 6, (120, 130, 140, 255))
    draw_rect(top, 7, 1, 9, 3, (90, 100, 110, 255))
    return top


def make_temp_half() -> None:
    full = load(OUT_HUD, "temp_full.png")
    half = full.copy()
    # Keep left 5 px of the 9x9 icon (same idea as thirst_half).
    half[:, 5:, 3] = 0
    save(OUT_HUD, "temp_half.png", half)


def main() -> None:
    if not VANILLA_BLOCK.is_dir() or not VANILLA_ITEM.is_dir():
        raise SystemExit("Vanilla textures missing — run createMinecraftArtifacts / compile once first.")
    make_thermometer()
    make_temp_half()

    save(OUT_BLOCK, "water_filter_side.png", make_filter_side(False))
    save(OUT_BLOCK, "water_filter_side_active.png", make_filter_side(True))
    save(OUT_BLOCK, "water_filter_top.png", make_filter_top())
    # Item uses side idle
    save(OUT_ITEM, "water_filter.png", make_filter_side(False))

    save(OUT_BLOCK, "water_still_side.png", make_still_side(False))
    save(OUT_BLOCK, "water_still_front.png", make_still_front(False))
    save(OUT_BLOCK, "water_still_front_on.png", make_still_front(True))
    save(OUT_BLOCK, "water_still_top.png", make_still_top())
    save(OUT_ITEM, "water_still.png", make_still_front(False))


if __name__ == "__main__":
    main()
