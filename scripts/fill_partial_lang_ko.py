#!/usr/bin/env python3
"""Fill missing ko_kr keys for partial namespaces via Google translate endpoint."""
from __future__ import annotations

import json
import re
import sys
import time
import urllib.parse
import urllib.request
import zipfile
from pathlib import Path

# Unbuffered logs when piped
try:
    sys.stdout.reconfigure(line_buffering=True)  # type: ignore[attr-defined]
except Exception:
    pass

ROOT = Path(__file__).resolve().parents[1]
CACHE = Path.home() / ".gradle" / "caches" / "modules-2" / "files-2.1"
LIBS = ROOT / "libs"
OV = ROOT / "pack" / "lang-ko"

# Only namespaces that still have gaps after existing overlays
PARTIALS = [
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
]

PLACEHOLDER = re.compile(r"(%(?:\d+\$)?[sd]|§.)")


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


def load_jar_lang(jar: Path, ns: str, locale: str) -> dict[str, str]:
    path = f"assets/{ns}/lang/{locale}.json"
    with zipfile.ZipFile(jar) as zf:
        if path not in zf.namelist():
            return {}
        data = json.loads(zf.read(path).decode("utf-8"))
        return {str(k): str(v) for k, v in data.items()} if isinstance(data, dict) else {}


def protect(text: str) -> tuple[str, list[str]]:
    tokens: list[str] = []

    def repl(m: re.Match[str]) -> str:
        tokens.append(m.group(0))
        return f"[[{len(tokens) - 1}]]"

    return PLACEHOLDER.sub(repl, text), tokens


def restore(text: str, tokens: list[str]) -> str:
    for i, tok in enumerate(tokens):
        text = text.replace(f"[[{i}]]", tok)
    return text


def translate(text: str) -> str:
    if not text.strip():
        return text
    if not any("a" <= c.lower() <= "z" for c in text):
        return text
    protected, tokens = protect(text)
    q = urllib.parse.quote(protected)
    scrape = (
        "https://translate.googleapis.com/translate_a/single"
        f"?client=gtx&sl=en&tl=ko&dt=t&q={q}"
    )
    req = urllib.request.Request(scrape, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req, timeout=30) as resp:
        data = json.loads(resp.read().decode("utf-8"))
    parts = []
    for chunk in data[0] or []:
        if chunk and chunk[0]:
            parts.append(chunk[0])
    return restore("".join(parts), tokens)


def fill_ns(needle: str, ns: str) -> int:
    jar = find_jar(needle)
    if jar is None:
        print(f"  SKIP {ns}: no jar")
        return 0
    en = load_jar_lang(jar, ns, "en_us")
    ko = load_jar_lang(jar, ns, "ko_kr")
    ov_path = OV / f"{ns}.json"
    ov = json.loads(ov_path.read_text(encoding="utf-8")) if ov_path.exists() else {}
    merged = dict(ko)
    merged.update(ov)
    missing = [k for k in en if k not in merged or not str(merged[k]).strip()]
    if not missing:
        print(f"  OK   {ns}: already complete")
        return 0
    print(f"  FILL {ns}: {len(missing)} keys...")
    added = 0
    for i, key in enumerate(missing):
        try:
            ov[key] = translate(en[key])
            added += 1
        except Exception as ex:
            print(f"    fail {key}: {ex}")
            ov[key] = en[key]
        if (i + 1) % 25 == 0:
            print(f"    ... {i + 1}/{len(missing)}")
            ov_path.write_text(json.dumps(ov, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
            time.sleep(0.4)
        else:
            time.sleep(0.08)
    ov_path.write_text(json.dumps(ov, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"  DONE {ns}: wrote {added}")
    return added


def main() -> None:
    OV.mkdir(parents=True, exist_ok=True)
    total = 0
    for needle, ns in PARTIALS:
        total += fill_ns(needle, ns)
    print(f"Total new keys: {total}")


if __name__ == "__main__":
    main()
