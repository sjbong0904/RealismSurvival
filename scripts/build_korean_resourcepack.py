#!/usr/bin/env python3
"""Build pack/resourcepacks/rs-korean from jar ko_kr + pack/lang-ko overlays."""
from __future__ import annotations

import json
import shutil
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "pack" / "resourcepacks" / "rs-korean"
RUN_OUT = ROOT / "run" / "resourcepacks" / "rs-korean"
CACHE = Path.home() / ".gradle" / "caches" / "modules-2" / "files-2.1"
LIBS = ROOT / "libs"
OVERRIDES = ROOT / "pack" / "lang-ko"

# (jar name needle, namespace)
TARGETS: list[tuple[str, str]] = [
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
    ("oritech-eCP3mBH7", "oritech"),
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


def load_json_from_jar(jar: Path, path: str) -> dict[str, str]:
    with zipfile.ZipFile(jar) as zf:
        if path not in zf.namelist():
            return {}
        try:
            data = json.loads(zf.read(path).decode("utf-8"))
        except json.JSONDecodeError:
            return {}
        return {str(k): str(v) for k, v in data.items()} if isinstance(data, dict) else {}


def load_override(ns: str) -> dict[str, str]:
    path = OVERRIDES / f"{ns}.json"
    if not path.exists():
        return {}
    data = json.loads(path.read_text(encoding="utf-8"))
    return {str(k): str(v) for k, v in data.items()} if isinstance(data, dict) else {}


def write_pack_meta() -> None:
    if OUT.exists():
        shutil.rmtree(OUT)
    OUT.mkdir(parents=True, exist_ok=True)
    meta = {
        "pack": {
            "description": "Realism Survival — Korean locale overlays (FTB + pack mods)",
            "min_format": 84,
            "max_format": 84,
        }
    }
    (OUT / "pack.mcmeta").write_text(json.dumps(meta, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def build_namespace(jar: Path, ns: str) -> tuple[str, int, int]:
    """Returns (status, written_keys, remaining_gaps)."""
    en = load_json_from_jar(jar, f"assets/{ns}/lang/en_us.json")
    if not en:
        return "no-en", 0, 0
    ko_existing = load_json_from_jar(jar, f"assets/{ns}/lang/ko_kr.json")
    override = load_override(ns)
    if not override and not ko_existing:
        return "skip-no-ko", 0, len(en)
    if not override:
        # jar already has ko; nothing for us to ship unless gaps remain
        gap = len(set(en) - set(ko_existing))
        return ("ok-jar" if gap == 0 else "gap-no-overlay"), 0, gap

    merged = dict(ko_existing)
    merged.update(override)
    # Prefer en key order; keep extras from overrides
    ordered = {k: merged[k] for k in en if k in merged}
    for k, v in merged.items():
        if k not in ordered:
            ordered[k] = v

    dest = OUT / "assets" / ns / "lang" / "ko_kr.json"
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_text(json.dumps(ordered, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    gap = len(set(en) - set(ordered))
    return "wrote", len(ordered), gap


def main() -> None:
    write_pack_meta()
    print(f"Building {OUT}")
    wrote_ns = 0
    for needle, ns in TARGETS:
        jar = find_jar(needle)
        if jar is None:
            override = load_override(ns)
            if not override:
                print(f"  SKIP {ns}: jar not found")
                continue
            dest = OUT / "assets" / ns / "lang" / "ko_kr.json"
            dest.parent.mkdir(parents=True, exist_ok=True)
            dest.write_text(json.dumps(override, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
            wrote_ns += 1
            print(f"  WRITE {ns:24} keys={len(override):4} gap=? (jar missing, overlay only)")
            continue
        status, keys, gap = build_namespace(jar, ns)
        if status == "wrote":
            wrote_ns += 1
            print(f"  WRITE {ns:24} keys={keys:4} gap={gap}")
        else:
            print(f"  {status:16} {ns:24} gap={gap}")

    # Mirror into run/resourcepacks for local testing
    if RUN_OUT.exists():
        shutil.rmtree(RUN_OUT)
    shutil.copytree(OUT, RUN_OUT)
    print(f"Copied to {RUN_OUT}")
    print(f"Namespaces written: {wrote_ns}")
    print("Enable resource pack: Options → Resource Packs → rs-korean")


if __name__ == "__main__":
    main()
