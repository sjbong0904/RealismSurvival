# -*- coding: utf-8 -*-
"""Build curated Collection quest sample lists from jar lang catalogs."""
from __future__ import annotations

import json
import re
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CAT = ROOT / "tools" / "quest_catalog"
OUT = CAT / "collections.json"

AQUA_KO = {
    "algae": "조류",
    "arapaima": "아라파이마",
    "arrau_turtle": "아라우 거북",
    "atlantic_cod": "대서양 대구",
    "atlantic_halibut": "대서양 넙치",
    "atlantic_herring": "대서양 청어",
    "bayad": "바야드",
    "blackfish": "블랙피시",
    "bluegill": "블루길",
    "boulti": "볼티",
    "brown_shrooma": "갈색 슈루마",
    "brown_trout": "갈색 송어",
    "capitaine": "카피텐",
    "carp": "잉어",
    "catfish": "메기",
    "gar": "가아",
    "goldfish": "금붕어",
    "jellyfish": "해파리",
    "largemouth_bass": "큰입배스",
    "leech": "거머리",
    "minnow": "피라미",
    "muskellunge": "머스키",
    "pacific_halibut": "태평양 넙치",
    "perch": "농어",
    "pink_salmon": "핑크 연어",
    "piranha": "피라냐",
    "pollock": "명태",
    "rainbow_trout": "무지개 송어",
    "red_grouper": "붉바리",
    "red_shrooma": "빨간 슈루마",
    "smallmouth_bass": "작은입배스",
    "starshell_turtle": "별껍질 거북",
    "sushi": "초밥",
    "synodontis": "시노돈티스",
    "tambaqui": "탐바키",
    "tuna": "참치",
    "turtle_soup": "거북 수프",
    "fish_fillet_raw": "날생선 필렛",
    "fish_fillet_cooked": "익힌 생선 필렛",
}


def load_tsv(ns: str) -> list[tuple[str, str, str, str]]:
    path = CAT / f"{ns}.tsv"
    rows = []
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line.strip():
            continue
        parts = line.split("\t")
        while len(parts) < 4:
            parts.append("")
        rows.append((parts[0], parts[1], parts[2], parts[3]))
    return rows


def find_bop_jar() -> Path:
    root = Path.home() / ".gradle" / "caches"
    best = None
    for p in root.rglob("biomes-o-plenty*.jar"):
        if best is None or p.stat().st_size > best.stat().st_size:
            best = p
    if not best:
        raise SystemExit("BoP jar not found")
    return best


def extract_bop_biomes() -> list[tuple[str, str]]:
    jar = find_bop_jar()
    z = zipfile.ZipFile(jar)
    biom = [
        n
        for n in z.namelist()
        if "/worldgen/biome/" in n and n.endswith(".json") and "biomesoplenty" in n
    ]
    ids = []
    for n in biom:
        m = re.search(r"biomesoplenty/worldgen/biome/([a-z0-9_]+)\.json", n)
        if m:
            ids.append("biomesoplenty:" + m.group(1))
    en_name = next(n for n in z.namelist() if n.endswith("lang/en_us.json"))
    en = json.loads(z.read(en_name))
    ko_files = [n for n in z.namelist() if n.endswith("lang/ko_kr.json")]
    ko = json.loads(z.read(ko_files[0])) if ko_files else {}
    out = []
    for bid in sorted(set(ids)):
        key = "biome." + bid.replace(":", ".")
        name = ko.get(key) or en.get(key) or bid.split(":")[1]
        # Hand-fix common English leftovers from incomplete BoP ko_kr
        fixes = {
            "Subtropics": "아열대",
            "subtropics": "아열대",
        }
        name = fixes.get(name, name)
        out.append((bid, name))
    return out


VANILLA_CROPS = [
    ("minecraft:wheat", "밀"),
    ("minecraft:wheat_seeds", "밀 씨앗"),
    ("minecraft:carrot", "당근"),
    ("minecraft:potato", "감자"),
    ("minecraft:baked_potato", "구운 감자"),
    ("minecraft:poisonous_potato", "독이 있는 감자"),
    ("minecraft:beetroot", "비트"),
    ("minecraft:beetroot_seeds", "비트 씨앗"),
    ("minecraft:melon_slice", "수박 조각"),
    ("minecraft:melon", "수박"),
    ("minecraft:pumpkin", "호박"),
    ("minecraft:pumpkin_seeds", "호박 씨앗"),
    ("minecraft:melon_seeds", "수박 씨앗"),
    ("minecraft:sugar_cane", "사탕수수"),
    ("minecraft:cocoa_beans", "코코아 콩"),
    ("minecraft:nether_wart", "네더 와트"),
    ("minecraft:sweet_berries", "달콤한 열매"),
    ("minecraft:glow_berries", "발광 열매"),
    ("minecraft:chorus_fruit", "후렴과"),
    ("minecraft:apple", "사과"),
    ("minecraft:golden_apple", "황금 사과"),
    ("minecraft:enchanted_golden_apple", "마법이 부여된 황금 사과"),
    ("minecraft:brown_mushroom", "갈색 버섯"),
    ("minecraft:red_mushroom", "빨간 버섯"),
    ("minecraft:crimson_fungus", "진홍빛 균"),
    ("minecraft:warped_fungus", "뒤틀린 균"),
    ("minecraft:kelp", "켈프"),
    ("minecraft:dried_kelp", "말린 켈프"),
    ("minecraft:sea_pickle", "불우렁쉥이"),
    ("minecraft:bamboo", "대나무"),
    ("minecraft:cactus", "선인장"),
    ("minecraft:torchflower", "횃불꽃"),
    ("minecraft:torchflower_seeds", "횃불꽃 씨앗"),
    ("minecraft:pitcher_pod", "피처 포드"),
    ("minecraft:pitcher_plant", "피처 플랜트"),
]

VANILLA_FOODS = [
    ("minecraft:bread", "빵"),
    ("minecraft:cookie", "쿠키"),
    ("minecraft:cake", "케이크"),
    ("minecraft:pumpkin_pie", "호박파이"),
    ("minecraft:mushroom_stew", "버섯 스튜"),
    ("minecraft:rabbit_stew", "토끼 스튜"),
    ("minecraft:beetroot_soup", "비트 수프"),
    ("minecraft:suspicious_stew", "수상한 스튜"),
    ("minecraft:cooked_beef", "스테이크"),
    ("minecraft:cooked_porkchop", "익힌 돼지고기"),
    ("minecraft:cooked_chicken", "익힌 닭고기"),
    ("minecraft:cooked_mutton", "익힌 양고기"),
    ("minecraft:cooked_rabbit", "익힌 토끼고기"),
    ("minecraft:cooked_cod", "익힌 대구"),
    ("minecraft:cooked_salmon", "익힌 연어"),
    ("minecraft:beef", "익히지 않은 소고기"),
    ("minecraft:porkchop", "익히지 않은 돼지고기"),
    ("minecraft:chicken", "익히지 않은 닭고기"),
    ("minecraft:mutton", "익히지 않은 양고기"),
    ("minecraft:rabbit", "익히지 않은 토끼고기"),
    ("minecraft:cod", "대구"),
    ("minecraft:salmon", "연어"),
    ("minecraft:tropical_fish", "열대어"),
    ("minecraft:pufferfish", "복어"),
    ("minecraft:rotten_flesh", "썩은 살점"),
    ("minecraft:spider_eye", "거미 눈"),
    ("minecraft:fermented_spider_eye", "발효된 거미 눈"),
    ("minecraft:golden_carrot", "황금 당근"),
    ("minecraft:honey_bottle", "꿀병"),
    ("minecraft:dried_kelp", "말린 켈프"),
    ("minecraft:apple", "사과"),
    ("minecraft:golden_apple", "황금 사과"),
    ("minecraft:enchanted_golden_apple", "마법이 부여된 황금 사과"),
    ("minecraft:carrot", "당근"),
    ("minecraft:potato", "감자"),
    ("minecraft:baked_potato", "구운 감자"),
    ("minecraft:poisonous_potato", "독이 있는 감자"),
    ("minecraft:beetroot", "비트"),
    ("minecraft:melon_slice", "수박 조각"),
    ("minecraft:sweet_berries", "달콤한 열매"),
    ("minecraft:glow_berries", "발광 열매"),
    ("minecraft:chorus_fruit", "후렴과"),
    ("minecraft:tropical_fish", "열대어"),
]

VANILLA_FISH = [
    ("minecraft:cod", "대구"),
    ("minecraft:salmon", "연어"),
    ("minecraft:tropical_fish", "열대어"),
    ("minecraft:pufferfish", "복어"),
]

DRINKS = [
    ("realismsurvival:distilled_water_bottle", "증류수 병"),
    ("realismsurvival:fresh_water_bottle", "민물 병"),
    ("realismsurvival:stagnant_water_bottle", "오염된 물 병"),
    ("realismsurvival:salt_water_bottle", "바닷물 병"),
    ("realismsurvival:distilled_water_bucket", "증류수 양동이"),
    ("realismsurvival:fresh_water_bucket", "민물 양동이"),
    ("realismsurvival:stagnant_water_bucket", "오염된 물 양동이"),
    ("realismsurvival:salt_water_bucket", "바닷물 양동이"),
    ("realismsurvival:distilled_ice_bucket", "증류수 얼음 양동이"),
    ("realismsurvival:fresh_ice_bucket", "민물 얼음 양동이"),
    ("realismsurvival:stagnant_ice_bucket", "오염된 얼음 양동이"),
    ("realismsurvival:snow_bucket", "눈 양동이"),
    ("minecraft:honey_bottle", "꿀병"),
    ("minecraft:milk_bucket", "우유 양동이"),
    ("minecraft:potion", "물약"),
    ("farmersdelight:apple_cider", "사과주"),
    ("farmersdelight:melon_juice", "수박 주스"),
    ("farmersdelight:hot_cocoa", "핫 코코아"),
    ("farmersdelight:milk_bottle", "우유 병"),
    ("farmersdelight:bone_broth", "사골국"),
]

MINERALS = [
    ("minecraft:coal", "석탄"),
    ("minecraft:charcoal", "숯"),
    ("minecraft:raw_iron", "철 원석"),
    ("minecraft:iron_ingot", "철 주괴"),
    ("minecraft:raw_copper", "구리 원석"),
    ("minecraft:copper_ingot", "구리 주괴"),
    ("minecraft:raw_gold", "금 원석"),
    ("minecraft:gold_ingot", "금 주괴"),
    ("minecraft:redstone", "레드스톤 가루"),
    ("minecraft:lapis_lazuli", "청금석"),
    ("minecraft:diamond", "다이아몬드"),
    ("minecraft:emerald", "에메랄드"),
    ("minecraft:amethyst_shard", "자수정 조각"),
    ("minecraft:quartz", "네더 석영"),
    ("minecraft:netherite_scrap", "네더라이트 파편"),
    ("minecraft:netherite_ingot", "네더라이트 주괴"),
    ("minecraft:ancient_debris", "고대 잔해"),
    ("minecraft:obsidian", "흑요석"),
    ("minecraft:crying_obsidian", "우는 흑요석"),
    ("minecraft:glowstone_dust", "발광석 가루"),
    ("minecraft:blaze_rod", "블레이즈 막대"),
    ("minecraft:ghast_tear", "가스트의 눈물"),
    ("minecraft:ender_pearl", "엔더 진주"),
    ("minecraft:echo_shard", "메아리 조각"),
    ("minecraft:prismarine_shard", "프리즈마린 조각"),
    ("minecraft:prismarine_crystals", "프리즈마린 수정"),
    ("minecraft:nether_star", "네더의 별"),
    ("minecraft:salt", "소금"),  # may be RS — keep if exists; else vanilla has none — use realismsurvival
]

# fix salt
MINERALS = [(a, b) for a, b in MINERALS if a != "minecraft:salt"] + [
    ("realismsurvival:salt", "소금"),
    ("realismsurvival:salt_block", "소금 블록"),
    ("realismsurvival:filter_cartridge", "필터 카트리지"),
    ("realismsurvival:thermometer", "온도계"),
    ("realismsurvival:graded_ice", "등급 얼음"),
    ("sereneseasons:calendar", "달력"),
    ("sereneseasons:season_sensor", "계절 센서"),
]

MOBS = [
    ("minecraft:zombie", "좀비"),
    ("minecraft:skeleton", "스켈레톤"),
    ("minecraft:creeper", "크리퍼"),
    ("minecraft:spider", "거미"),
    ("minecraft:enderman", "엔더맨"),
    ("minecraft:witch", "마녀"),
    ("minecraft:drowned", "드라운드"),
    ("minecraft:husk", "허스크"),
    ("minecraft:stray", "스트레이"),
    ("minecraft:phantom", "팬텀"),
    ("minecraft:slime", "슬라임"),
    ("minecraft:magma_cube", "마그마 큐브"),
    ("minecraft:pillager", "약탈자"),
    ("minecraft:vindicator", "변명자"),
    ("minecraft:evoker", "소환사"),
    ("minecraft:ravager", "파괴수"),
    ("minecraft:warden", "워든"),
    ("minecraft:blaze", "블레이즈"),
    ("minecraft:ghast", "가스트"),
    ("minecraft:piglin", "피글린"),
    ("minecraft:piglin_brute", "피글린 야수"),
    ("minecraft:hoglin", "호글린"),
    ("minecraft:zoglin", "조글린"),
    ("minecraft:wither_skeleton", "위더 스켈레톤"),
    ("minecraft:zombified_piglin", "좀비화된 피글린"),
    ("minecraft:shulker", "셜커"),
    ("minecraft:guardian", "가디언"),
    ("minecraft:elder_guardian", "엘더 가디언"),
    ("minecraft:silverfish", "좀벌레"),
    ("minecraft:endermite", "엔더마이트"),
    ("minecraft:cave_spider", "동굴 거미"),
    ("minecraft:breeze", "브리즈"),
    ("minecraft:bogged", "보그드"),
]

# Animal chapter uses products / pet gear (not kill tasks).
ANIMAL_ITEMS = [
    ("minecraft:beef", "소고기"),
    ("minecraft:leather", "가죽"),
    ("minecraft:porkchop", "돼지고기"),
    ("minecraft:mutton", "양고기"),
    ("minecraft:white_wool", "흰 양털"),
    ("minecraft:chicken", "닭고기"),
    ("minecraft:egg", "달걀"),
    ("minecraft:feather", "깃털"),
    ("minecraft:rabbit", "토끼고기"),
    ("minecraft:rabbit_hide", "토끼 가죽"),
    ("minecraft:rabbit_foot", "토끼 발"),
    ("minecraft:honeycomb", "벌집 조각"),
    ("minecraft:honey_bottle", "꿀병"),
    ("minecraft:ink_sac", "먹물 주머니"),
    ("minecraft:glow_ink_sac", "발광 먹물 주머니"),
    ("minecraft:turtle_scute", "거북 등딱지"),
    ("minecraft:goat_horn", "염소 뿔"),
    ("minecraft:saddle", "안장"),
    ("minecraft:name_tag", "이름표"),
    ("minecraft:lead", "끈"),
    ("pet_home:pet_bed_red", "빨간 펫 침대"),
    ("pet_home:collar_tag", "목걸이 태그"),
    ("pet_home:wayward_lantern", "방황하는 랜턴"),
    ("pet_home:feather_on_a_stick", "깃털 낚싯대"),
    ("farmersdelight:horse_feed", "말먹이"),
    ("farmersdelight:dog_food", "개밥"),
]

VANILLA_BIOMES = [
    ("minecraft:plains", "평원"),
    ("minecraft:sunflower_plains", "해바라기 평원"),
    ("minecraft:forest", "숲"),
    ("minecraft:flower_forest", "꽃 숲"),
    ("minecraft:birch_forest", "자작나무 숲"),
    ("minecraft:dark_forest", "어두운 숲"),
    ("minecraft:old_growth_birch_forest", "원시 자작나무 숲"),
    ("minecraft:old_growth_pine_taiga", "원시 소나무 타이가"),
    ("minecraft:old_growth_spruce_taiga", "원시 가문비 타이가"),
    ("minecraft:taiga", "타이가"),
    ("minecraft:snowy_taiga", "눈 덮인 타이가"),
    ("minecraft:snowy_plains", "눈 덮인 평원"),
    ("minecraft:ice_spikes", "얼음 스파이크"),
    ("minecraft:desert", "사막"),
    ("minecraft:savanna", "사바나"),
    ("minecraft:savanna_plateau", "사바나 고원"),
    ("minecraft:badlands", "악지"),
    ("minecraft:wooded_badlands", "나무 있는 악지"),
    ("minecraft:eroded_badlands", "침식된 악지"),
    ("minecraft:jungle", "정글"),
    ("minecraft:sparse_jungle", "드문드문한 정글"),
    ("minecraft:bamboo_jungle", "대나무 정글"),
    ("minecraft:swamp", "늪"),
    ("minecraft:mangrove_swamp", "맹그로브 늪"),
    ("minecraft:mushroom_fields", "버섯 들판"),
    ("minecraft:beach", "해변"),
    ("minecraft:snowy_beach", "눈 덮인 해변"),
    ("minecraft:stony_shore", "돌 해안"),
    ("minecraft:river", "강"),
    ("minecraft:frozen_river", "얼어붙은 강"),
    ("minecraft:ocean", "바다"),
    ("minecraft:warm_ocean", "따뜻한 바다"),
    ("minecraft:lukewarm_ocean", "미지근한 바다"),
    ("minecraft:cold_ocean", "차가운 바다"),
    ("minecraft:frozen_ocean", "얼어붙은 바다"),
    ("minecraft:deep_ocean", "깊은 바다"),
    ("minecraft:deep_cold_ocean", "깊은 차가운 바다"),
    ("minecraft:deep_frozen_ocean", "깊은 얼어붙은 바다"),
    ("minecraft:deep_lukewarm_ocean", "깊은 미지근한 바다"),
    ("minecraft:lush_caves", "무성한 동굴"),
    ("minecraft:dripstone_caves", "점적석 동굴"),
    ("minecraft:deep_dark", "깊고 어두운 곳"),
    ("minecraft:meadow", "목초지"),
    ("minecraft:cherry_grove", "벚나무 숲"),
    ("minecraft:grove", "숲지"),
    ("minecraft:snowy_slopes", "눈 덮인 비탈"),
    ("minecraft:jagged_peaks", "뾰족한 봉우리"),
    ("minecraft:frozen_peaks", "얼어붙은 봉우리"),
    ("minecraft:stony_peaks", "돌 봉우리"),
    ("minecraft:windswept_hills", "바람 휩쓸린 언덕"),
    ("minecraft:windswept_forest", "바람 휩쓸린 숲"),
    ("minecraft:windswept_gravelly_hills", "바람 휩쓸린 자갈 언덕"),
    ("minecraft:windswept_savanna", "바람 휩쓸린 사바나"),
    ("minecraft:nether_wastes", "네더 황무지"),
    ("minecraft:crimson_forest", "진홍빛 숲"),
    ("minecraft:warped_forest", "뒤틀린 숲"),
    ("minecraft:soul_sand_valley", "영혼 모래 골짜기"),
    ("minecraft:basalt_deltas", "현무암 삼각주"),
    ("minecraft:the_end", "엔드"),
    ("minecraft:end_highlands", "엔드 고지"),
    ("minecraft:end_midlands", "엔드 중지"),
    ("minecraft:small_end_islands", "작은 엔드 섬"),
    ("minecraft:end_barrens", "엔드 불모지"),
]


FD_FOOD_SKIP = {
    "diamond_knife",
    "flint_knife",
    "golden_knife",
    "iron_knife",
    "netherite_knife",
    "straw",
    "tree_bark",
    "earthworm",
    "dog_food",
    "horse_feed",
    "brown_mushroom_colony",
    "red_mushroom_colony",
    "pie_crust",
    "wheat_dough",
    "raw_pasta",
    "tomato_sauce",
    "minced_beef",
    "chicken_cuts",
    "bacon",
    "cod_slice",
    "salmon_slice",
    "mutton_chops",
    "ham",
}


def fd_foods() -> list[tuple[str, str]]:
    out = []
    for iid, ko, _en, _p in load_tsv("farmersdelight"):
        item = iid.split(":")[1]
        if item in FD_FOOD_SKIP:
            continue
        if any(
            x in item
            for x in (
                "crate",
                "cabinet",
                "tatami",
                "canvas",
                "basket",
                "rope",
                "safety",
                "compost",
                "rich_soil",
                "stove",
                "cooking_pot",
                "cutting",
                "skillet",
                "sink",
                "straw_bale",
                "sandy",
                "wild_",
                "budding",
                "tomatoes",
                "cabbages",
                "onions",
                "rice_panicles",
                "rice_bag",
                "rice_bale",
                "seeds",
                "knife",
                "block",
            )
        ):
            continue
        # keep edible-ish: soups, cooked, sandwiches, etc. Exclude pure crops (handled elsewhere)
        if item in ("tomato", "cabbage", "onion", "rice", "cabbage_leaf", "rice_panicle", "rotten_tomato"):
            continue
        out.append((iid, ko))
    return out


def fd_crops() -> list[tuple[str, str]]:
    keep = {
        "tomato",
        "cabbage",
        "onion",
        "rice",
        "cabbage_seeds",
        "tomato_seeds",
        "onion_seeds",
        "rice_panicle",
        "cabbage_leaf",
    }
    out = []
    for iid, ko, _en, _p in load_tsv("farmersdelight"):
        item = iid.split(":")[1]
        if item in keep:
            out.append((iid, ko))
    return out


def aqua_fish() -> list[tuple[str, str]]:
    skip = {
        "fishing_line",
        "neptunes_bounty",
        "tin_can",
        "algae",
        "leech",
        "minnow",  # bait-ish but keep minnow? keep as fish-ish
    }
    # actually keep minnow; skip junk
    skip = {"fishing_line", "neptunes_bounty", "tin_can", "algae"}
    out = []
    for iid, ko, en, _p in load_tsv("aquaculture"):
        item = iid.split(":")[1]
        if item in skip or item.endswith("_bucket") or item.endswith("_spawn_egg"):
            continue
        if any(
            x in item
            for x in (
                "rod",
                "knife",
                "hook",
                "bobber",
                "tackle",
                "worm",
                "bone",
                "mount",
                "neptunium",
                "driftwood",
                "box",
                "farm",
                "message",
                "note",
                "scrap",
                "lockbox",
                "treasure",
                "alchemy",
                "arrow",
                "boot",
                "hat",
                "chest",
                "legging",
                "ingot",
                "nugget",
                "block",
                "ore",
                "fillet",
            )
        ):
            continue
        title = AQUA_KO.get(item) or (ko if ko != en else en)
        # prefer our KO map
        title = AQUA_KO.get(item, ko if " " not in ko or any(ord(c) > 127 for c in ko) else AQUA_KO.get(item, en))
        if item in AQUA_KO:
            title = AQUA_KO[item]
        elif any(ord(c) > 127 for c in ko):
            title = ko
        else:
            title = en
        out.append((iid, title))
    # fillets as food-adjacent fish entries
    out.append(("aquaculture:fish_fillet_raw", AQUA_KO["fish_fillet_raw"]))
    out.append(("aquaculture:fish_fillet_cooked", AQUA_KO["fish_fillet_cooked"]))
    return out


def fot_fish() -> list[tuple[str, str]]:
    names = {
        "ancientscale",
        "battlegill",
        "devilfish",
        "islehopper",
        "plentifin",
        "pondie",
        "splashtail",
        "stormfish",
        "wildsplash",
        "wrecker",
    }
    out = []
    for iid, ko, _en, _p in load_tsv("fishofthieves"):
        item = iid.split(":")[1]
        if item in names:
            out.append((iid, ko))
    return out


def artifacts() -> list[tuple[str, str]]:
    out = []
    for iid, ko, _en, _p in load_tsv("artifacts"):
        item = iid.split(":")[1]
        if any(x in item for x in ("mimic", "spawn", "trophy", "creative")):
            continue
        out.append((iid, ko))
    return out


_BOP_PLANT_SKIP = (
    "log",
    "wood",
    "plank",
    "slab",
    "stair",
    "fence",
    "door",
    "trapdoor",
    "button",
    "pressure",
    "sign",
    "boat",
    "chest",
    "leaves",
    "stripped",
    "wall",
    "bricks",
    "tile",
    "mud",
    "dirt",
    "sand",
    "grass",
    "origin",
    "flesh",
    "honeycomb",
    "nest",
    "hive",
    "jar",
    "blood",
    "porphyry",
    "white_sand",
    "black_sand",
    "orange_sand",
    "dried",
)
_BOP_PLANT_SUFFIX = (
    "_sapling",
    "_fungus",
    "_mushroom",
    "_flower",
    "_blossom",
    "_bloom",
    "_petal",
    "_bulb",
    "_reed",
    "_grass",
    "_fern",
    "_bush",
    "_sprout",
    "_root",
    "_vine",
    "_cactus",
    "_lily",
    "_orchid",
    "_tulip",
    "_rose",
    "_daisy",
    "_dandelion",
    "_lavender",
    "_violet",
    "_clover",
    "_toadstool",
)
_BOP_PLANT_EXACT = {
    "rose",
    "violet",
    "lavender",
    "wildflower",
    "orange_cosmos",
    "pink_daffodil",
    "pink_hibiscus",
    "glowflower",
    "wilted_lily",
    "burning_blossom",
    "sprout",
    "bush",
    "clover",
    "huge_clover_petal",
    "toadstool",
    "glowshroom",
    "blue_hydrangea",
    "goldenrod",
    "icy_iris",
    "waterlily",
    "cattail",
    "sea_oats",
    "reed",
    "watergrass",
    "dune_grass",
    "desert_grass",
    "dead_grass",
    "barley",
    "mangrove_root",
}


def _bop_plant_match(item: str) -> bool:
    """Plant-like BoP ids, including potted variants so the crop window can count them."""
    bare = item.removeprefix("potted_")
    if any(x in bare for x in _BOP_PLANT_SKIP):
        return False
    return bare.endswith(_BOP_PLANT_SUFFIX) or bare in _BOP_PLANT_EXACT


def bop_plants() -> list[tuple[str, str]]:
    """Decorative/plant collectibles from BoP. Potted blocks are excluded."""
    out = []
    for iid, ko, _en, pref in load_tsv("biomesoplenty"):
        item = iid.split(":")[1]
        if "potted_" in item:
            continue
        if _bop_plant_match(item):
            out.append((iid, ko))
    return uniq_pairs(out)


def uniq_pairs(pairs: list[tuple[str, str]]) -> list[tuple[str, str]]:
    seen = set()
    out = []
    for a, b in pairs:
        if a in seen:
            continue
        seen.add(a)
        out.append((a, b))
    return out


def bop_crops_window() -> list[tuple[str, str]]:
    """Historical crop slice: the first 40 plant rows, then drop potted blocks.

    Potted rows still consume a slot in that window so quest indices of the
    remaining plants stay put. Later plants are not backfilled.
    """
    window: list[tuple[str, str]] = []
    for iid, ko, _en, pref in load_tsv("biomesoplenty"):
        item = iid.split(":")[1]
        if not _bop_plant_match(item):
            continue
        window.append((iid, ko))
        if len(window) >= 40:
            break
    return [(iid, ko) for iid, ko in window if "potted_" not in iid]


def main() -> None:
    crops = uniq_pairs(VANILLA_CROPS + fd_crops() + bop_crops_window())
    cooking = uniq_pairs(VANILLA_FOODS + fd_foods())
    fish = uniq_pairs(VANILLA_FISH + aqua_fish() + fot_fish())
    drinks = uniq_pairs(DRINKS)
    minerals = uniq_pairs(MINERALS)
    arts = uniq_pairs(artifacts())
    biomes = uniq_pairs(VANILLA_BIOMES + extract_bop_biomes())
    # Fish of Thieves tab opens its island node on this biome. Appended so earlier ids stay put.
    if not any(bid == "fishofthieves:tropical_island" for bid, _name in biomes):
        biomes.append(("fishofthieves:tropical_island", "도적의 섬"))
    mobs = uniq_pairs(MOBS)
    animals = uniq_pairs(ANIMAL_ITEMS)

    data = {
        "crops": crops,
        "cooking": cooking,
        "fish": fish,
        "drinks": drinks,
        "minerals": minerals,
        "artifacts": arts,
        "biomes": biomes,
        "mobs": mobs,
        "animals": animals,
    }
    OUT.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    for k, v in data.items():
        print(f"{k}: {len(v)}")
    print("total entries", sum(len(v) for v in data.values()))
    print("wrote", OUT)


if __name__ == "__main__":
    main()
