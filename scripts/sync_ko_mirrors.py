#!/usr/bin/env python3
"""Mirror the canonical Korean overlays (pack/lang-ko/<ns>.json) into the RS jar copies.

Why: the same third-party keys used to exist twice with different translations
(src/main/resources/assets/<ns>/lang/ko_kr.json and pack/lang-ko/<ns>.json). The overlay is now the
single source of truth; this script copies it over the jar copy so both layers always agree.

Rules
- Only namespaces that already have a jar copy are touched (``realismsurvival`` and ``minecraft`` are skipped).
- Keys that exist in the jar copy but not in the overlay are reported (the overlay must be a superset).
- Aquaculture keys that live inside realismsurvival/lang/ko_kr.json are refreshed from aquaculture.json.

Usage: python scripts/sync_ko_mirrors.py [--check]
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OVERLAY_DIR = ROOT / "pack" / "lang-ko"
ASSETS = ROOT / "src" / "main" / "resources" / "assets"
SKIP = {"realismsurvival", "minecraft"}


def load(path: Path) -> dict[str, str]:
    return json.loads(path.read_text(encoding="utf-8"))


def dump(path: Path, data: dict[str, str]) -> None:
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--check", action="store_true", help="report only, write nothing")
    args = ap.parse_args()
    dirty = 0

    for ns_dir in sorted(p for p in ASSETS.iterdir() if p.is_dir() and p.name not in SKIP):
        mirror = ns_dir / "lang" / "ko_kr.json"
        overlay = OVERLAY_DIR / f"{ns_dir.name}.json"
        if not mirror.exists():
            continue
        if not overlay.exists():
            print(f"[warn] {ns_dir.name}: jar copy without overlay")
            continue
        cur, src = load(mirror), load(overlay)
        extra = sorted(set(cur) - set(src))
        changed = sum(1 for k in src if cur.get(k) != src[k]) + len(extra)
        if extra:
            print(f"[warn] {ns_dir.name}: {len(extra)} keys only in jar copy (overlay not a superset): {extra[:3]}")
        if changed:
            dirty += 1
            print(f"{ns_dir.name}: {changed} entries differ")
            if not args.check:
                dump(mirror, {**src, **{k: cur[k] for k in extra}})

    core = ASSETS / "realismsurvival" / "lang" / "ko_kr.json"
    aq = OVERLAY_DIR / "aquaculture.json"
    if core.exists() and aq.exists():
        cur, src = load(core), load(aq)
        changed = {k: src[k] for k in cur if k in src and cur[k] != src[k]}
        if changed:
            dirty += 1
            print(f"realismsurvival (aquaculture keys): {len(changed)} entries differ")
            if not args.check:
                cur.update(changed)
                dump(core, cur)

    print("in sync" if not dirty else ("out of sync" if args.check else "synced"))
    return 1 if (dirty and args.check) else 0


if __name__ == "__main__":
    sys.exit(main())
