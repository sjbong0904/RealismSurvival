"""Scan runtime mod jars for vanilla open-water placement signals."""
from __future__ import annotations

import json
import re
import tempfile
import zipfile
from pathlib import Path

DUMP = Path(tempfile.gettempdir()) / "dumpjars.clean.txt"
OUT = Path(tempfile.gettempdir()) / "water-mod-audit.json"

SKIP = re.compile(
    r"(lwjgl|netty|guava|gson|commons-|log4j|slf4j|asm-|fastutil|brigadier|"
    r"datafixer|authlib|icu4j|joml|jna|oshi|neoforge-|minecraft-patched|"
    r"loader-|DevLaunch|JarJar|mergetool|srgutils|typetools|failureaccess|"
    r"listenablefuture|error_prone|j2objc|jspecify|jsr305|annotations-|"
    r"azure-|msal|json5|jopt|jorbis|jtracy|terminalconsole|earlydisplay|"
    r"blocklist|patchy|text2speech|maven-artifact|plexus|core-3\.8|toml-3|"
    r"bus-|accesstransformer|at-parser|java-objc|sponge-mixin|mezz_config|"
    r"jei-|jade-|appleskin|cloth-config|mouse-tweaks|modernfix|ferrite|"
    r"entityculling|ftb-chunks|voice-chat|seasonhud|curios-|better-advancements|"
    r"ftb-|shogi-|balm-|trashslot|creativecore|ambientsounds|sodium|iris|"
    r"scalable-cats|sg-economy|logging-|authlib|patchy)",
    re.I,
)

SEARCH = [
    ("WATER_BUCKET", b"WATER_BUCKET"),
    ("minecraft:water", b"minecraft:water"),
    ("meltsInto", b"meltsInto"),
    ("placeLiquid", b"placeLiquid"),
    ("emptyContents", b"emptyContents"),
    ("IceBlock", b"world/level/block/IceBlock"),
    ("BucketItem", b"world/item/BucketItem"),
    ("LiquidBlock", b"LiquidBlock"),
    ("Fluids", b"world/level/material/Fluids"),
    ("Blocks", b"world/level/block/Blocks"),
    ("LakeFeature", b"LakeFeature"),
    ("water_lake", b"water_lake"),
    ("spring_water", b"spring_water"),
]


def main() -> None:
    raw = DUMP.read_text(encoding="utf-8", errors="ignore").splitlines()
    jars = [l.strip() for l in raw if l.strip().lower().endswith(".jar") and Path(l.strip()).exists()]
    results = []
    for jp in jars:
        name = Path(jp).name
        if SKIP.search(name):
            continue
        try:
            zf = zipfile.ZipFile(jp)
        except Exception:
            continue
        hits: dict[str, list[str]] = {k: [] for k, _ in SEARCH}
        for info in zf.infolist():
            fn = info.filename
            if not (
                fn.endswith(".class")
                or fn.endswith(".json")
                or fn.endswith(".nbt")
                or fn.endswith(".mcfunction")
            ):
                continue
            try:
                data = zf.read(info)
            except Exception:
                continue
            for key, needle in SEARCH:
                if needle in data and len(hits[key]) < 10:
                    hits[key].append(fn)

        water_json: list[str] = []
        for info in zf.infolist():
            if not info.filename.endswith(".json"):
                continue
            try:
                txt = zf.read(info).decode("utf-8", errors="ignore")
            except Exception:
                continue
            if "minecraft:water" in txt and len(water_json) < 16:
                water_json.append(info.filename)

        # Require a strong signal or datapack water
        strong = any(
            hits[k]
            for k in (
                "WATER_BUCKET",
                "minecraft:water",
                "meltsInto",
                "placeLiquid",
                "emptyContents",
                "IceBlock",
                "LakeFeature",
                "water_lake",
                "spring_water",
            )
        )
        if not strong and not water_json:
            continue

        code_hits = {k: v for k, v in hits.items() if v and k not in ("Blocks", "Fluids", "LiquidBlock")}
        # Keep Blocks/Fluids only when strong water signals exist
        if strong:
            for k in ("Blocks", "Fluids", "LiquidBlock", "BucketItem"):
                if hits[k]:
                    code_hits[k] = hits[k]

        results.append(
            {
                "jar": name,
                "path": jp,
                "code_hits": code_hits,
                "datapack_water_json": water_json,
            }
        )

    def score(r: dict) -> int:
        s = 0
        ch = r["code_hits"]
        if "WATER_BUCKET" in ch:
            s += 5
        if "minecraft:water" in ch:
            s += 5
        if "meltsInto" in ch:
            s += 4
        if "placeLiquid" in ch or "emptyContents" in ch:
            s += 4
        if "IceBlock" in ch:
            s += 3
        if r["datapack_water_json"]:
            s += 8
        if "LakeFeature" in ch or "water_lake" in ch or "spring_water" in ch:
            s += 5
        return -s

    results.sort(key=score)
    OUT.write_text(json.dumps(results, indent=2), encoding="utf-8")
    print(f"Audited content jars with hits: {len(results)}")
    for r in results:
        print("==", r["jar"], "==")
        for k, v in r["code_hits"].items():
            print(f"  CODE {k}: {len(v)} e.g. {v[0]}")
        if r["datapack_water_json"]:
            print(f"  DATA minecraft:water x{len(r['datapack_water_json'])}:")
            for p in r["datapack_water_json"][:8]:
                print("   -", p)


if __name__ == "__main__":
    main()
