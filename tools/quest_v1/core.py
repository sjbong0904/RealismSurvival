# -*- coding: utf-8 -*-
"""FTB Quests 26.1 helpers for progression table v1."""
from __future__ import annotations

import hashlib
import json
import re
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "pack" / "config" / "ftbquests" / "quests"
RUN = ROOT / "run" / "config" / "ftbquests" / "quests"

G_TUTORIAL = None  # set after hid
G_BUNDLES = None
G_DAILY = None
G_COLLECTION = None


def hid(key: str) -> str:
    digest = hashlib.sha1(key.encode("utf-8")).digest()
    n = int.from_bytes(digest[:8], "big") & 0x7FFFFFFFFFFFFFFF
    return f"{n:016X}"


G_TUTORIAL = hid("group:tutorial")
G_BUNDLES = hid("group:bundles")
G_COLLECTION = hid("group:collection")
G_DAILY = hid("group:daily")


_JOSA_PAIRS = {
    "을": ("을", "를"),
    "이": ("이", "가"),
    "은": ("은", "는"),
    "과": ("과", "와"),
}
# Digits read in Korean: 0 영, 1 일, 3 삼, 6 육, 7 칠, 8 팔 end in a final consonant.
_DIGIT_BATCHIM = set("013678")
_LATIN_BATCHIM = set("lmnr")


def _has_batchim(word: str) -> bool | None:
    text = re.sub(r"\s*[\(\[（「『][^)\]）」』]*[)\]）」』]\s*$", "", word.strip())
    for ch in reversed(text):
        code = ord(ch)
        if 0xAC00 <= code <= 0xD7A3:
            return (code - 0xAC00) % 28 != 0
        if ch.isdigit():
            return ch in _DIGIT_BATCHIM
        if ch.isascii() and ch.isalpha():
            return ch.lower() in _LATIN_BATCHIM
        if ch.isspace() or ch in "'’·-_.":
            continue
        return None
    return None


def josa(word: str, particle: str) -> str:
    """`word` + the particle form that matches its final consonant (을/를, 이/가, 은/는, 과/와)."""
    with_batchim, without = _JOSA_PAIRS[particle]
    batchim = _has_batchim(word)
    if batchim is None:
        return f"{word}{with_batchim}({without})"
    return f"{word}{with_batchim if batchim else without}"


CHECK_HINT = "직접 체크하세요."


def esc(s: str) -> str:
    return s.replace("\\", "\\\\").replace('"', '\\"')


def snbt_str(s: str) -> str:
    return f'"{esc(s)}"'


def snbt_desc(lines: list[str]) -> str:
    if len(lines) == 1:
        return f"description: [{snbt_str(lines[0])}]"
    inner = "\n\t\t\t".join(snbt_str(x) for x in lines)
    return f"description: [\n\t\t\t{inner}\n\t\t]"


def item_stack(item: str, count: int = 1) -> str:
    return f'{{ id: {snbt_str(item)} count: {count} }}'


def item_task(key: str, item: str, count: int = 1, consume: bool | None = None) -> str:
    tid = hid(f"task:{key}")
    lines = [f"id: {snbt_str(tid)}", f"item: {item_stack(item, 1)}", 'type: "item"']
    if count != 1:
        lines.insert(1, f"count: {count}L")
    if consume is True:
        lines.append("consume_items: true")
    return "{\n\t\t\t\t" + "\n\t\t\t\t".join(lines) + "\n\t\t\t}"


def check_task(key: str) -> str:
    return (
        "{\n\t\t\t\t"
        + f'id: {snbt_str(hid(f"task:{key}"))}\n\t\t\t\t'
        + 'type: "checkmark"\n\t\t\t}'
    )


def adv_task(key: str, advancement: str, criterion: str = "") -> str:
    return (
        "{\n\t\t\t\t"
        + f'id: {snbt_str(hid(f"task:{key}"))}\n\t\t\t\t'
        + f"advancement: {snbt_str(advancement)}\n\t\t\t\t"
        + f"criterion: {snbt_str(criterion)}\n\t\t\t\t"
        + 'type: "advancement"\n\t\t\t}'
    )


def biome_task(key: str, biome: str) -> str:
    return (
        "{\n\t\t\t\t"
        + f'id: {snbt_str(hid(f"task:{key}"))}\n\t\t\t\t'
        + f"biome: {snbt_str(biome)}\n\t\t\t\t"
        + 'type: "biome"\n\t\t\t}'
    )


def stat_task(key: str, stat: str, value: int) -> str:
    return (
        "{\n\t\t\t\t"
        + f'id: {snbt_str(hid(f"task:{key}"))}\n\t\t\t\t'
        + f"stat: {snbt_str(stat)}\n\t\t\t\t"
        + f"value: {int(value)}\n\t\t\t\t"
        + 'type: "stat"\n\t\t\t}'
    )


def kill_task(key: str, entity: str, value: int = 1) -> str:
    return (
        "{\n\t\t\t\t"
        + f'id: {snbt_str(hid(f"task:{key}"))}\n\t\t\t\t'
        + f"value: {int(value)}L\n\t\t\t\t"
        + f"entity: {snbt_str(entity)}\n\t\t\t\t"
        + 'type: "kill"\n\t\t\t}'
    )


def item_reward(key: str, item: str, count: int = 1) -> str:
    rid = hid(f"reward:item:{key}")
    lines = [f"id: {snbt_str(rid)}", f"item: {item_stack(item, 1)}", 'type: "item"']
    if count != 1:
        lines.insert(1, f"count: {count}")
    return "{\n\t\t\t\t" + "\n\t\t\t\t".join(lines) + "\n\t\t\t}"


def xp_levels_reward(key: str, levels: int) -> str:
    return (
        "{\n\t\t\t\t"
        + f'id: {snbt_str(hid(f"reward:xplvl:{key}"))}\n\t\t\t\t'
        + f"xp_levels: {int(levels)}\n\t\t\t\t"
        + 'type: "xp_levels"\n\t\t\t}'
    )


def xp_reward(key: str, xp: int = 25) -> str:
    return (
        "{\n\t\t\t\t"
        + f'id: {snbt_str(hid(f"reward:xp:{key}"))}\n\t\t\t\t'
        + f"xp: {xp}\n\t\t\t\t"
        + 'type: "xp"\n\t\t\t}'
    )


def coin_reward(key: str, amount: int) -> str:
    """FTB command reward → /realism coins give (SG Economy / Q Shop gold)."""
    return (
        "{\n\t\t\t\t"
        + f'id: {snbt_str(hid(f"reward:coin:{key}"))}\n\t\t\t\t'
        + f'command: {snbt_str(f"/realism coins give @p {int(amount)}")}\n\t\t\t\t'
        + "permission_level: 2\n\t\t\t\t"
        + "silent: true\n\t\t\t\t"
        + 'type: "command"\n\t\t\t}'
    )


def daily_coin_reward(key: str, amount: int) -> str:
    """Daily board payout; capped by Overworld day via /realism daily claim."""
    return (
        "{\n\t\t\t\t"
        + f'id: {snbt_str(hid(f"reward:daily:{key}"))}\n\t\t\t\t'
        + f'command: {snbt_str(f"/realism daily claim {int(amount)}")}\n\t\t\t\t'
        + "permission_level: 2\n\t\t\t\t"
        + "silent: true\n\t\t\t\t"
        + 'type: "command"\n\t\t\t}'
    )


class Quest:
    def __init__(
        self,
        qid: str,
        title: str,
        x: float,
        y: float,
        tasks: list[str],
        rewards: list[str] | None = None,
        deps: list[str] | None = None,
        desc: list[str] | None = None,
        size: float = 1.0,
        shape: str | None = None,
        optional: bool = False,
        consume: bool | None = None,
        min_required_dependencies: int | None = None,
        hide_dependency_lines: bool = False,
        can_repeat: bool = False,
        repeat_cooldown: int = 0,
    ):
        self.qid = qid
        self.id = hid(f"quest:{qid}")
        self.title = title
        self.x = x
        self.y = y
        self.tasks = tasks
        self.rewards = rewards or []
        self.deps = deps or []
        self.desc = desc or []
        self.size = size
        self.shape = shape
        self.optional = optional
        self.consume = consume
        self.min_required_dependencies = min_required_dependencies
        self.hide_dependency_lines = hide_dependency_lines
        self.can_repeat = can_repeat
        self.repeat_cooldown = repeat_cooldown

    def render(self, id_map: dict[str, str]) -> str:
        parts = [f"id: {snbt_str(self.id)}", f"title: {snbt_str(self.title)}"]
        if self.desc:
            parts.append(snbt_desc(self.desc))
        if self.deps:
            dep_ids = ", ".join(snbt_str(id_map[d]) for d in self.deps if d in id_map)
            if dep_ids:
                parts.append(f"dependencies: [{dep_ids}]")
        if self.min_required_dependencies is not None:
            parts.append(f"min_required_dependencies: {int(self.min_required_dependencies)}")
        if self.hide_dependency_lines:
            parts.append("hide_dependency_lines: true")
        if self.optional:
            parts.append("optional: true")
        if self.can_repeat:
            parts.append("can_repeat: true")
            if self.repeat_cooldown > 0:
                parts.append(f"repeat_cooldown: {int(self.repeat_cooldown)}")
        if self.consume is True:
            parts.append("consume_items: true")
        if self.shape:
            parts.append(f"shape: {snbt_str(self.shape)}")
        if self.size != 1.0:
            parts.append(f"size: {self.size}d")
        parts.append(f"x: {self.x}d")
        parts.append(f"y: {self.y}d")
        if len(self.tasks) == 1:
            parts.append(f"tasks: [{self.tasks[0]}]")
        else:
            parts.append(
                "tasks: [\n\t\t\t" + ",\n\t\t\t".join(self.tasks) + "\n\t\t]"
            )
        if self.rewards:
            if len(self.rewards) == 1:
                parts.append(f"rewards: [{self.rewards[0]}]")
            else:
                parts.append(
                    "rewards: [\n\t\t\t" + ",\n\t\t\t".join(self.rewards) + "\n\t\t]"
                )
        return "{\n\t\t\t" + "\n\t\t\t".join(parts) + "\n\t\t}"


def layout_quests_grid(
    quests: list[Quest],
    *,
    step_x: float = 2.0,
    step_y: float = 1.75,
) -> list[Quest]:
    if not quests:
        return quests
    by_qid = {q.qid: q for q in quests}
    qids = set(by_qid)
    layers: dict[str, int] = {}

    def depth(qid: str, stack: set[str] | None = None) -> int:
        if qid in layers:
            return layers[qid]
        stack = stack or set()
        if qid in stack:
            layers[qid] = 0
            return 0
        stack.add(qid)
        q = by_qid[qid]
        deps = [d for d in q.deps if d in qids]
        layers[qid] = 0 if not deps else 1 + max(depth(d, stack) for d in deps)
        stack.discard(qid)
        return layers[qid]

    for q in quests:
        depth(q.qid)
    buckets: dict[int, list[Quest]] = {}
    for q in quests:
        buckets.setdefault(layers[q.qid], []).append(q)
    # One depth can be a tall wall (bundle items, opened chapter branches).
    # Wrap so a column stays inside a single quest-screen height.
    gap = 0.6
    max_col_height = 7.2
    x_cursor = 0.0
    prev_half = 0.0
    first_col = True

    def chunks_for(rows: list[Quest]) -> list[list[Quest]]:
        chunks: list[list[Quest]] = []
        current: list[Quest] = []
        current_h = 0.0
        for q in rows:
            size = max(q.size, 1.0)
            add = size if not current else size + gap
            if current and current_h + add > max_col_height:
                chunks.append(current)
                current = [q]
                current_h = size
            else:
                current.append(q)
                current_h += add
        if current:
            chunks.append(current)
        return chunks

    for _layer, rows in sorted(buckets.items()):
        for chunk in chunks_for(rows):
            half = max(max(q.size, 1.0) for q in chunk) / 2
            if not first_col:
                x_cursor += prev_half + half + gap
            first_col = False
            prev_half = half
            sizes = [max(q.size, 1.0) for q in chunk]
            total = sum(sizes) + gap * (len(sizes) - 1)
            y_cursor = -total / 2
            for q, size in zip(chunk, sizes):
                q.x = round(x_cursor, 2)
                q.y = round(y_cursor + size / 2, 2)
                y_cursor += size + gap
    return quests


def write_chapter(
    filename: str,
    group: str,
    title: str,
    icon: str,
    order: int,
    quests: list[Quest],
    *,
    auto_layout: bool = True,
    extra_ids: dict[str, str] | None = None,
    default_shape: str = "circle",
):
    if auto_layout:
        quests = layout_quests_grid(quests)
    id_map = {q.qid: q.id for q in quests}
    if extra_ids:
        id_map.update(extra_ids)
    body = ",\n\t\t".join(q.render(id_map) for q in quests)
    text = f"""{{
\tdefault_hide_dependency_lines: false
\tdefault_quest_shape: {snbt_str(default_shape)}
\tfilename: {snbt_str(filename)}
\tgroup: {snbt_str(group)}
\ticon: {item_stack(icon)}
\tid: {snbt_str(hid(f"chapter:{filename}"))}
\torder_index: {order}
\tquest_links: [ ]
\tquests: [
\t\t{body}
\t]
\ttitle: {snbt_str(title)}
}}
"""
    path = OUT / "chapters" / f"{filename}.snbt"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")
    return path


def write_data_and_groups():
    OUT.mkdir(parents=True, exist_ok=True)
    chapters_dir = OUT / "chapters"
    if chapters_dir.exists():
        shutil.rmtree(chapters_dir)
    chapters_dir.mkdir(parents=True, exist_ok=True)
    (OUT / "chapter_groups.snbt").write_text(
        f"""{{
\tchapter_groups: [
\t\t{{ id: {snbt_str(G_TUTORIAL)} title: {snbt_str("Tutorial")} }}
\t\t{{ id: {snbt_str(G_BUNDLES)} title: {snbt_str("Bundles")} }}
\t\t{{ id: {snbt_str(G_COLLECTION)} title: {snbt_str("Collection")} }}
\t\t{{ id: {snbt_str(G_DAILY)} title: {snbt_str("Daily")} }}
\t]
}}
""",
        encoding="utf-8",
    )
    (OUT / "data.snbt").write_text(
        f"""{{
\tdefault_autoclaim_rewards: "disabled"
\tdefault_consume_items: false
\tdefault_quest_disable_jei: false
\tdefault_quest_shape: "circle"
\tdefault_reward_team: false
\tdetection_delay: 20
\tdisable_gui: false
\tdrop_book_on_death: false
\tdrop_loot_crates: false
\temergency_items_cooldown: 300
\tgrid_scale: 1.0d
\thide_excluded_quests: false
\ticon: {{ id: "realismsurvival:distilled_water_bottle" count: 1 }}
\tlock_message: ""
\tpause_game: false
\tprogression_mode: "linear"
\tshow_lock_icons: true
\ttitle: "&bRealism Survival"
\tversion: 1
}}
""",
        encoding="utf-8",
    )


def snbt_to_json5(text: str) -> str:
    text = re.sub(r"(?<![A-Za-z0-9_])(-?\d+(?:\.\d+)?)[dLb]\b", r"\1", text)
    text = re.sub(r'(")[ \t]+([A-Za-z_][A-Za-z0-9_]*)\s*:', r"\1, \2:", text)
    text = re.sub(
        r"(true|false|\d+(?:\.\d+)?)[ \t]+([A-Za-z_][A-Za-z0-9_]*)\s*:",
        r"\1, \2:",
        text,
    )
    text = re.sub(r"([{\[,]\s*)([A-Za-z_][A-Za-z0-9_]*)\s*:", r'\1"\2":', text)
    text = re.sub(r"(?m)^(\s*)([A-Za-z_][A-Za-z0-9_]*)\s*:", r'\1"\2":', text)
    lines = text.splitlines()
    out: list[str] = []
    for i, line in enumerate(lines):
        stripped = line.rstrip()
        nxt = ""
        for j in range(i + 1, len(lines)):
            if lines[j].strip():
                nxt = lines[j].strip()
                break
        needs_comma = (
            bool(stripped)
            and not stripped.endswith(("{", "[", ","))
            and bool(nxt)
            and not nxt.startswith(("}", "]"))
        )
        out.append(stripped + ("," if needs_comma else ""))
    return "\n".join(out) + "\n"


def convert_snbt_tree_to_json5(root: Path) -> None:
    for snbt_path in sorted(root.rglob("*.snbt")):
        body = snbt_path.read_text(encoding="utf-8")
        snbt_path.with_suffix(".json5").write_text(snbt_to_json5(body), encoding="utf-8")
        snbt_path.unlink()


def sync_to_run():
    if RUN.exists():
        shutil.rmtree(RUN)
    shutil.copytree(OUT, RUN)


def q_check(
    qid: str,
    title: str,
    deps: list[str] | None = None,
    rewards: list[str] | None = None,
    desc: list[str] | None = None,
    optional: bool = False,
    size: float = 1.0,
    shape: str | None = None,
    min_required_dependencies: int | None = None,
    hide_dependency_lines: bool = False,
) -> Quest:
    desc = list(desc or [])
    if not any("체크" in line for line in desc):
        desc.append(CHECK_HINT)
    return Quest(
        qid,
        title,
        0,
        0,
        [check_task(qid)],
        rewards,
        deps,
        desc,
        size=size,
        shape=shape,
        optional=optional,
        min_required_dependencies=min_required_dependencies,
        hide_dependency_lines=hide_dependency_lines,
    )


def q_item(
    qid: str,
    title: str,
    item: str,
    count: int = 1,
    deps: list[str] | None = None,
    rewards: list[str] | None = None,
    desc: list[str] | None = None,
    consume: bool = False,
    optional: bool = False,
    size: float = 1.0,
    shape: str | None = None,
) -> Quest:
    return Quest(
        qid,
        title,
        0,
        0,
        [item_task(qid, item, count, consume=True if consume else None)],
        rewards,
        deps,
        desc,
        size=size,
        shape=shape,
        optional=optional,
        consume=True if consume else None,
    )


def q_items(
    qid: str,
    title: str,
    items: list[tuple[str, int]],
    deps: list[str] | None = None,
    rewards: list[str] | None = None,
    desc: list[str] | None = None,
    consume: bool = False,
    optional: bool = False,
    size: float = 1.0,
    shape: str | None = None,
) -> Quest:
    tasks = [
        item_task(f"{qid}_{i}", it, ct, consume=True if consume else None)
        for i, (it, ct) in enumerate(items)
    ]
    return Quest(
        qid,
        title,
        0,
        0,
        tasks,
        rewards,
        deps,
        desc,
        size=size,
        shape=shape,
        optional=optional,
        consume=True if consume else None,
    )


def gate_extra(*qids: str) -> dict[str, str]:
    return {qid: hid(f"quest:{qid}") for qid in qids}
