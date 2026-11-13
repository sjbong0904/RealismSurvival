#!/usr/bin/env python3
"""Verify pack/lang-ko overlays against jar en_us/ko_kr."""
from __future__ import annotations

import json
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CACHE = Path.home() / ".gradle" / "caches" / "modules-2" / "files-2.1"
LIBS = ROOT / "libs"
OV = ROOT / "pack" / "lang-ko"

PAIRS = [
    ("qshop-neoforge-26.1", "qshop"),
    ("sg_economy-26.1.0", "sg_economy"),
    ("balm-26.1", "balm"),
    ("cristel-lib-neoforge-26.1", "cristellib"),
    ("shogi-26.1", "shogi"),
    ("terrablender-26.1.2.0", "terrablender"),
    ("entityculling-1.11", "entityculling"),
    ("large-fluid-tank-26", "fluidtank"),
    ("modernfix-5.27", "modernfix"),
    ("pet_home-1.1", "pet_home"),
    ("treeliable-1.4", "treeliable"),
    ("easy_npc-neoforge-26.1", "easy_npc"),
    ("easy_npc_config_ui-neoforge-26.1", "easy_npc_config_ui"),
    ("ftb-library-neoforge-26.1", "ftblibrary"),
    ("ftb-quests-neoforge-26.1", "ftbquests"),
    ("ftb-teams-neoforge-26.1", "ftbteams"),
    ("ftb-chunks-neoforge-26.1", "ftbchunks"),
    ("aquaculture-2.9", "aquaculture"),
    ("guard-villagers-3.0", "guardvillagers"),
    ("waystones-26.1", "waystones"),
    ("jade-26.1", "jade"),
    ("jei-26.1.2-neoforge-29.47", "jei"),
    ("sophisticated-core-26.1", "sophisticatedcore"),
    ("sophisticated-backpacks-26.1", "sophisticatedbackpacks"),
    ("iron-chests-26.1", "ironchest"),
    ("artifacts-15.1", "artifacts"),
    ("trashslot-26.1", "trashslot"),
    ("simple-voice-chat-neoforge-2.6", "voicechat"),
    ("creativecore-2.14", "creativecore"),
    ("appleskin-neoforge-mc26.1", "appleskin"),
    ("better-advancements-0.6", "betteradvancements"),
    ("seasonhud-26.1", "seasonhud"),
    ("pipez-neoforge-1.2", "pipez"),
    ("biomes-o-plenty-26.1", "biomesoplenty"),
    ("iris-1.11.4", "iris"),
    ("spice-of-life-carrot", "solcarrot"),
    ("oritech-2.0.0-exp12-neoforge-26.1.2", "oritech"),
]


def find_jar(needle: str) -> Path | None:
    best: Path | None = None
    for root in (CACHE, LIBS):
        if not root.exists():
            continue
        for jar in root.rglob("*.jar"):
            name = jar.name
            if needle not in name:
                continue
            if any(x in name for x in ("sources", "javadoc", "-api", "common-api")):
                continue
            if best is None or jar.stat().st_mtime > best.stat().st_mtime:
                best = jar
    return best


def hangul_ratio(s: str) -> float:
    if not s:
        return 0.0
    letters = [c for c in s if c.isalpha() or ("\uac00" <= c <= "\ud7a3")]
    if not letters:
        return 1.0
    hangul = sum(1 for c in letters if "\uac00" <= c <= "\ud7a3")
    return hangul / len(letters)


def main() -> None:
    print(f"{'ns':24} {'ov':>5} {'en':>5} {'jko':>5} {'after':>5} {'gap':>5} {'ko%':>5}")
    need_partial: list[tuple[str, str, int]] = []
    for needle, ns in PAIRS:
        jar = find_jar(needle)
        ov_path = OV / f"{ns}.json"
        ov = json.loads(ov_path.read_text(encoding="utf-8")) if ov_path.exists() else {}
        if jar is None:
            print(f"{ns:24} NOJAR")
            continue
        with zipfile.ZipFile(jar) as zf:
            en_p = f"assets/{ns}/lang/en_us.json"
            ko_p = f"assets/{ns}/lang/ko_kr.json"

            def safe(path: str) -> dict:
                if path not in zf.namelist():
                    return {}
                try:
                    data = json.loads(zf.read(path).decode("utf-8"))
                    return {str(k): str(v) for k, v in data.items()} if isinstance(data, dict) else {}
                except json.JSONDecodeError:
                    return {}

            en = safe(en_p)
            ko = safe(ko_p)
        merged = dict(ko)
        merged.update(ov)
        gap = len(set(en) - set(merged))
        after = len(set(en) & set(merged))
        # overlay quality sample
        ratios = [hangul_ratio(str(v)) for v in list(ov.values())[:40]] if ov else [1.0]
        avg = sum(ratios) / len(ratios)
        flag = ""
        if gap > 0 and after < len(en):
            flag = " PARTIAL"
            need_partial.append((needle, ns, gap))
        if ov and avg < 0.15:
            flag += " LOW_KO"
        print(f"{ns:24} {len(ov):5} {len(en):5} {len(ko):5} {after:5} {gap:5} {avg:5.2f}{flag}")

    print("\nStill need overlays / fill:")
    for needle, ns, gap in need_partial:
        has = (OV / f"{ns}.json").exists()
        print(f"  {ns}: gap={gap} overlay={'yes' if has else 'NO'}")


if __name__ == "__main__":
    main()
