"""Compare en_us vs ko_kr across the deployment profile's mod jars + RS overlays.

Input is the real runtime jar list of the profile:
    ./gradlew writeRuntimeJarList          # -> build/reports/runtime-jars.txt
    python tools/scan_missing_ko_kr.py [--jar-list FILE] [--fail-on-missing]

Reports go to build/reports/locale/. Key completeness only: whether a translation reads naturally is
tracked separately (docs/KOREAN_LOCALES.md review status).
Exit codes: 0 ok, 1 gaps with --fail-on-missing (or RS's own ko_kr gaps, always), 2 bad/missing input.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
import zipfile
from collections import defaultdict
from pathlib import Path

RS_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_JAR_LIST = RS_ROOT / "build" / "reports" / "runtime-jars.txt"
REPORT_DIR = RS_ROOT / "build" / "reports" / "locale"
OUT = REPORT_DIR / "missing-ko-kr.json"
SUMMARY = REPORT_DIR / "missing-ko-kr-summary.txt"
FULL_DIR = REPORT_DIR / "missing-ko-kr-full"

SKIP_JAR = re.compile(
    r"(lwjgl|netty|guava|gson|commons-|log4j|slf4j|asm-|fastutil|brigadier|"
    r"datafixer|authlib|icu4j|joml|jna|oshi|neoforge-|minecraft-patched|"
    r"loader-|DevLaunch|JarJar|mergetool|srgutils|typetools|failureaccess|"
    r"listenablefuture|error_prone|j2objc|jspecify|jsr305|annotations-|"
    r"azure-|msal|json5|jopt|jorbis|jtracy|terminalconsole|earlydisplay|"
    r"blocklist|patchy|text2speech|maven-artifact|plexus|core-3\.8|toml-3|"
    r"bus-|accesstransformer|at-parser|java-objc|sponge-mixin|mezz_config|"
    r"logging-|authlib|sodium|iris|modernfix|ferrite|entityculling|"
    r"scalable-cats)",
    re.I,
)

# Gameplay-facing key prefixes (skip raw config dumps / debug)
KEEP_PREFIX = (
    "item.",
    "block.",
    "entity.",
    "biome.",
    "fluid.",
    "fluid_type.",
    "container.",
    "gui.",
    "menu.",
    "screen.",
    "advancement.",
    "effect.",
    "enchantment.",
    "trim_pattern.",
    "trim_material.",
    "stat.",
    "subtitles.",
    "death.",
    "commands.",
    "argument.",
    "key.",
    "category.",
    "tabs.",
    "itemGroup.",
    "itemgroup.",
    "creative_tab.",
    "tooltip.",
    "jade.",
    "config.",
    "text.",
    "message.",
    "chat.",
    "emotes.",
    "jei.",
    "emi.",
    "modmenu.",
    "ftbquests.",
    "quest.",
    "season.",
    "sereneseasons.",
    "biomesoplenty.",
    "farmersdelight.",
    "aquaculture.",
    "fishofthieves.",
    "waystones.",
    "artifacts.",
    "curios.",
    "sophisticated",
    "pipez.",
    "fluidtank.",
    "lootr.",
    "guardvillagers.",
    "pet_home.",
    "treeliable.",
    "ironchest.",
    "ironchests.",
    "solcarrot.",
    "naturescompass.",
    "ftbchunks.",
    "voicechat.",
    "trashslot.",
    "balm.",
    "shogi.",
    "seasonhud.",
    "appleskin.",
    "realismsurvival.",
    "rsmedicine.",
)


def load_json_bytes(data: bytes) -> dict[str, str] | None:
    try:
        text = data.decode("utf-8-sig")
        obj = json.loads(text)
        if isinstance(obj, dict):
            return {str(k): str(v) for k, v in obj.items() if isinstance(v, (str, int, float))}
    except Exception:
        return None
    return None


def keep_key(key: str) -> bool:
    k = key.lower()
    return any(k.startswith(p.lower()) or p.lower() in k for p in KEEP_PREFIX)


def merge_lang(dst: dict[str, str], src: dict[str, str] | None) -> None:
    if not src:
        return
    dst.update(src)


def read_jar_list(path: Path) -> list[str]:
    if not path.is_file():
        print(f"ERROR: jar list not found: {path}\n  run: ./gradlew writeRuntimeJarList", file=sys.stderr)
        raise SystemExit(2)
    jars = [l.strip() for l in path.read_text(encoding="utf-8").splitlines() if l.strip().endswith(".jar")]
    jars += [str(p) for p in sorted((RS_ROOT / "libs").glob("*.jar"))]
    missing = [j for j in jars if not Path(j).is_file()]
    if missing:
        print(f"ERROR: {len(missing)} listed jars do not exist (stale list?), e.g. {missing[0]}", file=sys.stderr)
        raise SystemExit(2)
    if not jars:
        print(f"ERROR: jar list is empty: {path}", file=sys.stderr)
        raise SystemExit(2)
    return list(dict.fromkeys(jars))


def own_lang_gaps() -> dict[str, list[str]]:
    """RS-owned namespaces: every en_us key must exist in ko_kr (ko_kr is canonical, D-04)."""
    roots = {
        "realismsurvival": RS_ROOT / "src" / "main" / "resources" / "assets" / "realismsurvival" / "lang",
        "rsmedicine": RS_ROOT / "addons" / "rs-medicine" / "src" / "main" / "resources" / "assets" / "rsmedicine" / "lang",
    }
    gaps: dict[str, list[str]] = {}
    for ns, root in roots.items():
        en_path, ko_path = root / "en_us.json", root / "ko_kr.json"
        if not en_path.exists():
            continue
        en = load_json_bytes(en_path.read_bytes()) or {}
        ko = load_json_bytes(ko_path.read_bytes()) if ko_path.exists() else {}
        missing = sorted(k for k in en if k not in (ko or {}))
        if missing:
            gaps[ns] = missing
    return gaps


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--jar-list", type=Path, default=DEFAULT_JAR_LIST)
    parser.add_argument("--fail-on-missing", action="store_true", help="exit 1 if any third-party gameplay key lacks ko_kr")
    args = parser.parse_args()
    jars = read_jar_list(args.jar_list)
    REPORT_DIR.mkdir(parents=True, exist_ok=True)
    # namespace -> {en, ko_from_jars, ko_broken}
    en: dict[str, dict[str, str]] = defaultdict(dict)
    ko_jar: dict[str, dict[str, str]] = defaultdict(dict)
    broken_ko: dict[str, list[str]] = defaultdict(list)
    jar_for_ns: dict[str, str] = {}

    for jp in jars:
        name = Path(jp).name
        if SKIP_JAR.search(name):
            continue
        try:
            zf = zipfile.ZipFile(jp)
        except Exception:
            continue
        for info in zf.infolist():
            m = re.match(r"assets/([^/]+)/lang/(en_us|ko_kr)\.json$", info.filename)
            if not m:
                continue
            ns, lang = m.group(1), m.group(2)
            jar_for_ns.setdefault(ns, name)
            raw = zf.read(info)
            parsed = load_json_bytes(raw)
            if parsed is None:
                if lang == "ko_kr":
                    broken_ko[ns].append(f"{name}:{info.filename}")
                continue
            if lang == "en_us":
                merge_lang(en[ns], parsed)
            else:
                merge_lang(ko_jar[ns], parsed)

    # Overlays in RS source
    ko_overlay: dict[str, dict[str, str]] = defaultdict(dict)
    for path in (RS_ROOT / "src" / "main" / "resources" / "assets").glob("*/lang/ko_kr.json"):
        ns = path.parent.parent.name
        parsed = load_json_bytes(path.read_bytes())
        merge_lang(ko_overlay[ns], parsed)
    # realismsurvival hosts foreign keys (aquaculture workaround)
    rs_ko = ko_overlay.get("realismsurvival", {})
    for key, val in rs_ko.items():
        # item.aquaculture.x -> aquaculture namespace conceptually
        parts = key.split(".")
        if len(parts) >= 2 and parts[1] not in ("realismsurvival",):
            # keys like item.aquaculture.foo → ns aquaculture
            if len(parts) >= 3:
                maybe_ns = parts[1]
                if maybe_ns in en or maybe_ns in ko_jar or maybe_ns in broken_ko:
                    ko_overlay[maybe_ns][key] = val

    # also medicine addon
    med = RS_ROOT / "addons" / "rs-medicine" / "src" / "main" / "resources" / "assets" / "rsmedicine" / "lang" / "ko_kr.json"
    if med.exists():
        merge_lang(ko_overlay["rsmedicine"], load_json_bytes(med.read_bytes()))

    report = []
    total_missing = 0
    for ns in sorted(set(en) | set(ko_jar) | set(broken_ko) | set(ko_overlay)):
        if ns in ("minecraft", "neoforge"):
            continue
        en_keys = {k: v for k, v in en.get(ns, {}).items() if keep_key(k)}
        if not en_keys and ns not in broken_ko:
            # no en_us gameplay keys — skip
            continue
        have = {}
        merge_lang(have, ko_jar.get(ns))
        merge_lang(have, ko_overlay.get(ns))
        # foreign keys hosted in realismsurvival with full key path
        for k, v in rs_ko.items():
            if k in en_keys or (len(k.split(".")) >= 2 and k.split(".")[1] == ns):
                have[k] = v

        missing = {k: en_keys[k] for k in en_keys if k not in have}
        # Also: has en but no ko file at all
        no_ko_file = ns not in ko_jar and ns not in ko_overlay and ns not in broken_ko
        if broken_ko.get(ns):
            # treat all en keys not in overlay as missing
            pass
        if not missing and not broken_ko.get(ns):
            continue
        total_missing += len(missing)
        report.append(
            {
                "namespace": ns,
                "jar": jar_for_ns.get(ns, "?"),
                "en_keys": len(en_keys),
                "ko_jar_keys": len(ko_jar.get(ns, {})),
                "ko_overlay_keys": len(ko_overlay.get(ns, {})),
                "missing_count": len(missing),
                "broken_ko": broken_ko.get(ns, []),
                "no_ko_file": no_ko_file and not have,
                "missing_sample": dict(list(missing.items())[:15]),
                "missing": missing,
            }
        )

    report.sort(key=lambda r: (-r["missing_count"], r["namespace"]))
    OUT.write_text(json.dumps([{k: v for k, v in r.items() if k != "missing"} | {"missing_count": r["missing_count"]} for r in report], indent=2, ensure_ascii=False), encoding="utf-8")
    # write full missing separately per ns for apply step
    full_dir = FULL_DIR
    full_dir.mkdir(parents=True, exist_ok=True)
    lines = [f"namespaces_with_gaps={len(report)} total_missing_gameplay_keys={total_missing}", ""]
    for r in report:
        (full_dir / f"{r['namespace']}.json").write_text(
            json.dumps(r["missing"], indent=2, ensure_ascii=False) + "\n",
            encoding="utf-8",
        )
        flag = ""
        if r["broken_ko"]:
            flag = " [BROKEN ko_kr]"
        elif r["no_ko_file"]:
            flag = " [NO ko_kr]"
        lines.append(
            f"{r['namespace']:28} missing={r['missing_count']:5} en={r['en_keys']:5} "
            f"ko_jar={r['ko_jar_keys']:5} overlay={r['ko_overlay_keys']:5}  {r['jar']}{flag}"
        )
    own = own_lang_gaps()
    lines.append("")
    lines.append(f"jars_scanned={len(jars)} jar_list={args.jar_list}")
    for ns, keys in own.items():
        lines.append(f"OWN {ns}: {len(keys)} en_us keys missing in ko_kr, e.g. {keys[:5]}")
    SUMMARY.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(SUMMARY.read_text(encoding="utf-8"))
    if own:
        print("FAIL: RS-owned ko_kr is incomplete", file=sys.stderr)
        raise SystemExit(1)
    if args.fail_on_missing and total_missing:
        print(f"FAIL: {total_missing} gameplay keys without ko_kr (see {SUMMARY})", file=sys.stderr)
        raise SystemExit(1)


if __name__ == "__main__":
    main()
