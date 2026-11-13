# -*- coding: utf-8 -*-
"""Keep the pack datapack copy of rsbridges recipes identical to the JAR source.

Canonical: src/main/resources/data/rsbridges/recipe/
Copy:      pack/datapacks/rs-item-bridges/data/rsbridges/recipe/

Usage:
    python tools/sync_bridge_recipes.py          # copy canonical -> pack (adds/updates/removes)
    python tools/sync_bridge_recipes.py --check  # exit 1 on any drift (CI / pre-release)
"""
from __future__ import annotations

import hashlib
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src" / "main" / "resources" / "data" / "rsbridges" / "recipe"
DST = ROOT / "pack" / "datapacks" / "rs-item-bridges" / "data" / "rsbridges" / "recipe"


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def index(root: Path) -> dict[str, str]:
    if not root.is_dir():
        return {}
    return {p.relative_to(root).as_posix(): digest(p) for p in sorted(root.rglob("*.json"))}


def main() -> int:
    check = "--check" in sys.argv[1:]
    src = index(SRC)
    dst = index(DST)
    if not src:
        print(f"missing canonical recipes: {SRC}")
        return 1

    missing = sorted(set(src) - set(dst))
    extra = sorted(set(dst) - set(src))
    changed = sorted(k for k in set(src) & set(dst) if src[k] != dst[k])

    if check:
        for name in missing:
            print(f"missing in pack: {name}")
        for name in extra:
            print(f"extra in pack:   {name}")
        for name in changed:
            print(f"content differs: {name}")
        ok = not (missing or extra or changed)
        print(f"{'OK' if ok else 'DRIFT'}: {len(src)} canonical, {len(dst)} pack")
        return 0 if ok else 1

    for name in missing + changed:
        target = DST / name
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(SRC / name, target)
    for name in extra:
        (DST / name).unlink()
    print(
        f"synced {len(src)} recipes "
        f"(+{len(missing)} added, ~{len(changed)} updated, -{len(extra)} removed)"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
