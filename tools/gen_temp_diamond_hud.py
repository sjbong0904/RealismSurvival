"""Generate 9x9 diamond (rhombus) HUD sprites for body temperature."""
from __future__ import annotations

from pathlib import Path

import numpy as np
from PIL import Image

OUT = Path(__file__).resolve().parents[1] / "src/main/resources/assets/realismsurvival/textures/gui/sprites/hud"


def diamond_mask(size: int = 9) -> np.ndarray:
    """Boolean mask for a centered diamond including a 1px outline ring."""
    c = (size - 1) / 2.0
    yy, xx = np.mgrid[0:size, 0:size]
    # Manhattan diamond; radius so tips touch edges
    return (np.abs(xx - c) + np.abs(yy - c)) <= c + 0.01


def make_sprites() -> None:
    size = 9
    filled = diamond_mask(size)
    # Outline = filled XOR eroded
    eroded = np.zeros_like(filled)
    for y in range(size):
        for x in range(size):
            if not filled[y, x]:
                continue
            # interior if all 4-neighbors filled
            if (
                y > 0
                and y < size - 1
                and x > 0
                and x < size - 1
                and filled[y - 1, x]
                and filled[y + 1, x]
                and filled[y, x - 1]
                and filled[y, x + 1]
            ):
                eroded[y, x] = True
    rim = filled & ~eroded

    empty = np.zeros((size, size, 4), dtype=np.uint8)
    # Dark fill + black outline (matches thirst empty style)
    empty[filled] = (40, 40, 45, 255)
    empty[rim] = (10, 10, 12, 255)

    full = np.zeros((size, size, 4), dtype=np.uint8)
    # Light body for runtime tint (blue→red); dark outline
    full[eroded] = (230, 230, 235, 255)
    full[rim] = (15, 15, 18, 255)

    mid = size // 2
    # HUD (LTR fill): bright left / dark right
    half_ltr = empty.copy()
    for y in range(size):
        for x in range(mid):
            if eroded[y, x]:
                half_ltr[y, x] = (230, 230, 235, 255)
            elif rim[y, x] and x < mid:
                half_ltr[y, x] = (15, 15, 18, 255)

    # Tooltip (match thirst/hunger): dark left / bright right
    half_rtl = empty.copy()
    for y in range(size):
        for x in range(mid, size):
            if eroded[y, x]:
                half_rtl[y, x] = (230, 230, 235, 255)
            elif rim[y, x] and x >= mid:
                half_rtl[y, x] = (15, 15, 18, 255)

    OUT.mkdir(parents=True, exist_ok=True)
    for name, arr in (
        ("temp_empty.png", empty),
        ("temp_full.png", full),
        ("temp_half.png", half_ltr),
        ("temp_half_tooltip.png", half_rtl),
    ):
        Image.fromarray(arr, "RGBA").save(OUT / name)
        print("wrote", OUT / name)


if __name__ == "__main__":
    make_sprites()
