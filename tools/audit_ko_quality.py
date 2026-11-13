#!/usr/bin/env python3
"""Mechanical Korean-locale audit (placeholders, leftovers, tone, glossary).

Compares assets/<ns>/lang/en_us.json vs ko_kr.json for RS sources and pack/lang-ko overlays
(en side comes from the built rs-korean pack + jar extracts when available).

Usage:
    python tools/audit_ko_quality.py            # summary
    python tools/audit_ko_quality.py --detail   # per-key findings
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.stdout.reconfigure(encoding="utf-8")

PH = re.compile(r"%(?:\d+\$)?[sdfx%]|\{\d*\}|\$\{[^}]+\}")
LATIN_WORD = re.compile(r"[A-Za-z]{3,}")
HANGUL = re.compile(r"[가-힣]")

# Formatting tokens / known brand words that are fine to leave in Latin script
ALLOWED_LATIN = {
    "ftb", "chunks", "jei", "rs", "pvp", "hud", "fps", "gui", "npc", "id", "uuid", "url", "tps",
    "jade", "ftb", "teams", "quests", "waystones", "curios", "minecraft", "neoforge", "mod",
    "oritech", "ae", "rf", "fe", "xp", "hp", "tnt", "ok", "wiki", "http", "https", "www",
    "shift", "ctrl", "alt", "tab", "space", "lan", "gb", "mb", "kb", "api", "json", "snbt",
    "sodium", "iris", "lootr", "seasonhud", "appleskin", "qshop", "sg", "economy", "easy",
}

# Mixed registers: formal (합니다/습니다) vs plain (한다/이다) vs polite (해요)
RE_FORMAL = re.compile(r"(합니다|입니다|습니다|ㅂ니다|세요|십시오|하세요)[\.\!\s]*$")
RE_PLAIN = re.compile(r"(한다|된다|있다|없다|이다|않는다|준다|난다)[\.\!\s]*$")
RE_HAEYO = re.compile(r"(해요|예요|에요|이에요|돼요|있어요|없어요)[\.\!\s]*$")

# Awkward / machine-translation smell patterns
SMELLS = [
    (re.compile(r"\b하는 것입니다\b"), "…하는 것입니다 (번역투)"),
    (re.compile(r"에 의해"), "'에 의해' (번역투 수동)"),
    (re.compile(r"것을 허용"), "'것을 허용' (번역투)"),
    (re.compile(r"하게 됩니다"), "'하게 됩니다' (번역투)"),
    (re.compile(r"\b당신\b"), "'당신' (2인칭 번역투)"),
    (re.compile(r"청구"), "'청구' (OPC 직역: claim)"),
    (re.compile(r"열린 파티"), "'열린 파티' (Open Parties 직역)"),
    (re.compile(r"[\u3131-\u3163]"), "자모 단독 문자"),
    (re.compile(r"  +"), "연속 공백"),
    (re.compile(r"\s+[\.,!?]"), "구두점 앞 공백"),
    (re.compile(r"\(s\)|\(es\)"), "영어 복수 표기 잔재"),
    (re.compile(r"…\.|\.\.\.\."), "말줄임표 중복"),
]

# Terms the pack should use consistently
GLOSSARY = {
    "점유": ["청구", "클레임"],
    "강제 로드": ["강제 로딩", "강제로딩", "포스로드"],
    "웨이포인트": ["경유지", "길잡이 표시"],
    "생물군계": ["바이옴"],
    "퀘스트": ["임무"],
    "물약": ["포션"],
    "체온": ["온도 체력"],
    "갈증": ["목마름"],
}


def load(path: Path) -> dict[str, str]:
    try:
        d = json.loads(path.read_text(encoding="utf-8-sig"))
    except Exception:
        return {}
    return {str(k): str(v) for k, v in d.items() if isinstance(v, (str, int, float))}


def ph_sig(s: str) -> list[str]:
    return sorted(PH.findall(s))


def audit_pair(label: str, en: dict[str, str], ko: dict[str, str], findings: dict[str, list[tuple[str, str, str]]]):
    for k, kv in ko.items():
        ev = en.get(k)
        if ev is not None and ph_sig(ev) != ph_sig(kv):
            findings["placeholder"].append((label, k, f"EN={ph_sig(ev)} KO={ph_sig(kv)} :: {kv[:80]}"))
        if ev is not None and kv.strip() == ev.strip() and LATIN_WORD.search(ev):
            words = {w.lower() for w in LATIN_WORD.findall(ev)}
            if not words <= ALLOWED_LATIN:
                findings["untranslated"].append((label, k, kv[:80]))
        elif not HANGUL.search(kv) and LATIN_WORD.search(kv):
            words = {w.lower() for w in LATIN_WORD.findall(kv)}
            if not words <= ALLOWED_LATIN:
                findings["no_hangul"].append((label, k, kv[:80]))
        for rx, msg in SMELLS:
            if rx.search(kv):
                findings["smell"].append((label, k, f"{msg} :: {kv[:90]}"))
        for good, bads in GLOSSARY.items():
            for bad in bads:
                if bad in kv:
                    findings["glossary"].append((label, k, f"'{bad}' → '{good}' :: {kv[:80]}"))
    for k in en:
        if k not in ko:
            findings["missing"].append((label, k, en[k][:60]))


def register_stats(label: str, ko: dict[str, str]) -> dict[str, int]:
    c = {"formal": 0, "plain": 0, "haeyo": 0}
    for v in ko.values():
        for line in v.split("\n"):
            line = line.strip()
            if RE_FORMAL.search(line):
                c["formal"] += 1
            elif RE_PLAIN.search(line):
                c["plain"] += 1
            elif RE_HAEYO.search(line):
                c["haeyo"] += 1
    return c


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--detail", action="store_true")
    ap.add_argument("--only", default="")
    args = ap.parse_args()

    findings: dict[str, list[tuple[str, str, str]]] = defaultdict(list)
    regs: dict[str, dict[str, int]] = {}

    sources: list[tuple[str, Path, Path | None]] = []
    for p in sorted(ROOT.glob("src/main/resources/assets/*/lang/ko_kr.json")):
        sources.append((f"RS:{p.parts[-3]}", p, p.with_name("en_us.json")))
    for p in sorted(ROOT.glob("addons/*/src/main/resources/assets/*/lang/ko_kr.json")):
        sources.append((f"ADDON:{p.parts[-3]}", p, p.with_name("en_us.json")))
    for p in sorted(ROOT.glob("pack/lang-ko/*.json")):
        ns = p.stem
        if ns.startswith("xaero") or ns == "openpartiesandclaims":
            continue
        en_p = ROOT / "build" / "locale_extract" / f"{ns}_en.json"
        sources.append((f"OVERLAY:{ns}", p, en_p if en_p.exists() else None))

    for label, ko_p, en_p in sources:
        if args.only and args.only not in label:
            continue
        ko = load(ko_p)
        en = load(en_p) if en_p and en_p.exists() else {}
        audit_pair(label, en, ko, findings)
        regs[label] = register_stats(label, ko)

    print("== findings ==")
    for kind, items in sorted(findings.items()):
        print(f"{kind:14} {len(items)}")
    print("\n== register mix (formal/plain/haeyo) ==")
    for label, c in regs.items():
        tot = sum(c.values())
        if tot >= 8:
            print(f"{label:34} formal={c['formal']:4} plain={c['plain']:4} haeyo={c['haeyo']:4}")
    if args.detail:
        for kind, items in sorted(findings.items()):
            print(f"\n--- {kind} ---")
            for label, k, msg in items:
                print(f"[{label}] {k}: {msg}")


if __name__ == "__main__":
    main()
