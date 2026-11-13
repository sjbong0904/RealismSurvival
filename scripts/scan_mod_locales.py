#!/usr/bin/env python3
"""Scan pack-relevant mod jars for en_us vs ko_kr lang coverage."""
from __future__ import annotations

import json
import re
import zipfile
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PROPS = ROOT / "gradle.properties"
CACHE = Path.home() / ".gradle" / "caches" / "modules-2" / "files-2.1"
LIBS = ROOT / "libs"

# jar name substrings / exact files used by build.gradle localRuntime
WANTED = [
    "sodium-mc26.1.2",
    "iris-1.11.4",
    "curios-neoforge-15.0.0",
    "appleskin-neoforge-mc26.1",
    "jei-26.1.2-neoforge-29.47",
    "glitchcore-26.1.2.0.2",
    "serene-seasons-26.1.2.0.7",
    "farmers-delight-26-neo-ver",
    "cloth-config-26.1",
    "aquaculture-2.9",
    "fish-of-thieves-26.1",
    "scalable-cats-force",
    "large-fluid-tank-26",
    "pipez-neoforge-1.2",
    "jade-26.1",
    "mouse-tweaks-26.1",
    "balm-26.1",
    "trashslot-26.1",
    "iron-chests-26.1",
    "simple-voice-chat-neoforge-2.6",
    "creativecore-2.14",
    "ambientsounds-6.3",
    "entityculling-1.11",
    "modernfix-5.27",
    "ferrite-core-9.0",
    "seasonhud-26.1",
    "shogi-26.1",
    "ftb-library-neoforge-26.1",
    "ftb-teams-neoforge-26.1",
    "ftb-quests-neoforge-26.1",
    "ftb-chunks-neoforge-26.1",
    "ftb-xmod-compat-neoforge-26.1",
    "better-advancements-0.6",
    "cristel-lib-neoforge-26.1",
    "towns-and-towers-1.13",
    "lootr-1.23",
    "spice-of-life-carrot",
    "terrablender-26.1.2.0",
    "biomes-o-plenty-26.1",
    "waystones-26.1",
    "waystones-teleport-pets-1.2",
    "sophisticated-core-26.1",
    "sophisticated-backpacks-26.1",
    "natures-compass-26.1",
    "artifacts-15.1",
    "guard-villagers-3.0",
    "pet_home-1.1",
    "treeliable-1.4",
    "qshop-neoforge-26.1",
    "sg_economy-26.1",
    "celesthyd-26.1",
    "easy_npc-neoforge-26.1",
    "easy_npc_config_ui-neoforge-26.1",
]


def find_jars() -> list[Path]:
    found: dict[str, Path] = {}
    roots = [CACHE, LIBS]
    for root in roots:
        if not root.exists():
            continue
        for jar in root.rglob("*.jar"):
            name = jar.name
            if any(x in name for x in ("sources", "javadoc", "-api-", "common-api", "neoforge-api")):
                continue
            for needle in WANTED:
                if needle in name:
                    # prefer exact version match / newer path
                    prev = found.get(needle)
                    if prev is None or jar.stat().st_mtime > prev.stat().st_mtime:
                        found[needle] = jar
                    break
    return sorted(found.values(), key=lambda p: p.name.lower())


LANG_RE = re.compile(r"^assets/([^/]+)/lang/(en_us|ko_kr)\.(json|lang)$", re.I)


def scan_jar(path: Path) -> dict:
    namespaces: dict[str, dict] = defaultdict(lambda: {"en": None, "ko": None, "en_keys": set(), "ko_keys": set()})
    try:
        with zipfile.ZipFile(path) as zf:
            for info in zf.infolist():
                m = LANG_RE.match(info.filename.replace("\\", "/"))
                if not m:
                    continue
                ns, locale, ext = m.group(1), m.group(2).lower(), m.group(3).lower()
                bucket = namespaces[ns]
                data = zf.read(info).decode("utf-8", errors="replace")
                keys: set[str] = set()
                if ext == "json":
                    try:
                        obj = json.loads(data)
                        if isinstance(obj, dict):
                            keys = {str(k) for k in obj.keys()}
                    except json.JSONDecodeError:
                        keys = set()
                else:
                    for line in data.splitlines():
                        line = line.strip()
                        if not line or line.startswith("#") or "=" not in line:
                            continue
                        keys.add(line.split("=", 1)[0].strip())
                if locale == "en_us":
                    bucket["en"] = info.filename
                    bucket["en_keys"] |= keys
                else:
                    bucket["ko"] = info.filename
                    bucket["ko_keys"] |= keys
    except zipfile.BadZipFile:
        return {"jar": path.name, "error": "bad zip"}
    rows = []
    for ns, b in sorted(namespaces.items()):
        en_n = len(b["en_keys"])
        ko_n = len(b["ko_keys"])
        missing = sorted(b["en_keys"] - b["ko_keys"]) if b["en"] else []
        rows.append(
            {
                "ns": ns,
                "en": bool(b["en"]),
                "ko": bool(b["ko"]),
                "en_n": en_n,
                "ko_n": ko_n,
                "missing_n": len(missing),
                "missing_sample": missing[:12],
                "missing_all": missing,
            }
        )
    return {"jar": path.name, "path": str(path), "namespaces": rows}


def main() -> None:
    jars = find_jars()
    print(f"Scanning {len(jars)} jars...\n")
    missing_mods = []
    partial_mods = []
    ok_mods = []
    no_lang = []
    details = []
    for jar in jars:
        info = scan_jar(jar)
        details.append(info)
        if "error" in info:
            print(f"ERR  {info['jar']}: {info['error']}")
            continue
        if not info["namespaces"]:
            no_lang.append(info["jar"])
            continue
        for ns in info["namespaces"]:
            label = f"{info['jar']} :: {ns['ns']}"
            if ns["en"] and not ns["ko"]:
                missing_mods.append((label, ns["en_n"], ns["missing_all"]))
            elif ns["en"] and ns["missing_n"] > 0:
                # ignore tiny gaps under 5% for "ok-ish", still report
                ratio = ns["missing_n"] / max(ns["en_n"], 1)
                partial_mods.append((label, ns["en_n"], ns["ko_n"], ns["missing_n"], ratio, ns["missing_sample"], ns["missing_all"]))
            elif ns["en"]:
                ok_mods.append(label)
            elif ns["ko"] and not ns["en"]:
                ok_mods.append(label + " (ko only)")

    print("=== NO ko_kr (has en_us) ===")
    for label, en_n, _ in missing_mods:
        print(f"  MISSING  {label}  (en={en_n})")
    print(f"\n=== PARTIAL ko_kr (missing keys) ===")
    for label, en_n, ko_n, miss_n, ratio, sample, _ in sorted(partial_mods, key=lambda x: -x[4]):
        if miss_n == 0:
            continue
        print(f"  PARTIAL  {label}  en={en_n} ko={ko_n} missing={miss_n} ({ratio:.0%})")
        if sample:
            print(f"           e.g. {', '.join(sample[:6])}")
    print(f"\n=== OK (full ko_kr or no gap) ===")
    for label in ok_mods:
        print(f"  OK       {label}")
    print(f"\n=== NO lang files ===")
    for jar in no_lang:
        print(f"  NOLANG   {jar}")

    out = ROOT / "build" / "locale_scan.json"
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(details, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"\nWrote {out}")


if __name__ == "__main__":
    main()
