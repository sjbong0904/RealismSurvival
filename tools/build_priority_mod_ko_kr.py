# -*- coding: utf-8 -*-
"""Merge Korean overlays for waystones, pet_home, ftblibrary, ftbquests."""
from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MISSING = ROOT / "tools" / "missing_ko_kr"
OUT = ROOT / "src" / "main" / "resources" / "assets"

MC_COLORS = {
    "White": "하양",
    "Gold": "금색",
    "Light Purple": "연보라",
    "Aqua": "하늘색",
    "Yellow": "노랑",
    "Green": "초록",
    "Red": "빨강",
    "Gray": "회색",
    "Light Gray": "밝은 회색",
    "Dark Gray": "짙은 회색",
    "Dark Aqua": "짙은 하늘색",
    "Dark Purple": "짙은 보라",
    "Blue": "파랑",
    "Dark Blue": "짙은 파랑",
    "Dark Green": "짙은 초록",
    "Dark Red": "짙은 빨강",
    "Black": "검정",
    "Cyan": "청록",
    "Brown": "갈색",
    "Lime": "연두",
    "Magenta": "자홍",
    "Orange": "주황",
    "Pink": "분홍",
    "Purple": "보라",
    "Light Blue": "하늘빛",
}


def merge_write(modid: str, translations: dict[str, str]) -> int:
    missing_path = MISSING / f"{modid}.json"
    if not missing_path.is_file():
        raise SystemExit(f"Missing dump: {missing_path}")
    missing = json.loads(missing_path.read_text(encoding="utf-8"))
    out_path = OUT / modid / "lang" / "ko_kr.json"
    out_path.parent.mkdir(parents=True, exist_ok=True)
    existing: dict[str, str] = {}
    if out_path.is_file():
        existing = json.loads(out_path.read_text(encoding="utf-8"))
    written = 0
    for key, val in translations.items():
        if key not in missing:
            continue
        if existing.get(key) != val:
            existing[key] = val
            written += 1
    out_path.write_text(json.dumps(existing, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return written


def waystones_translations(missing: dict[str, str]) -> dict[str, str]:
    ko: dict[str, str] = {}

    color_group = {
        "white": "하양",
        "gold": "금색",
        "light_purple": "연보라",
        "aqua": "하늘색",
        "yellow": "노랑",
        "green": "초록",
        "red": "빨강",
        "gray": "회색",
        "dark_gray": "짙은 회색",
        "dark_aqua": "짙은 하늘색",
        "dark_purple": "짙은 보라",
        "blue": "파랑",
        "dark_blue": "짙은 파랑",
        "dark_green": "짙은 초록",
        "dark_red": "짙은 빨강",
        "black": "검정",
    }
    for cid, label in color_group.items():
        ko[f"color.waystones.group.{cid}"] = label

    groups = {
        "waystones.groups.community_hubs": "커뮤니티 거점",
        "waystones.groups.dimension": "차원",
        "waystones.groups.dungeons": "던전",
        "waystones.groups.favorites": "즐겨찾기",
        "waystones.groups.global": "글로벌 전송석",
        "waystones.groups.overworld": "오버월드",
        "waystones.groups.players": "플레이어",
        "waystones.groups.player_homes": "플레이어 집",
        "waystones.groups.resource_sites": "자원 지점",
        "waystones.groups.teams": "팀",
        "waystones.groups.villages": "마을",
        "waystones.groups.dimension.minecraft.overworld": "오버월드",
        "waystones.groups.dimension.minecraft.the_nether": "네더",
        "waystones.groups.dimension.minecraft.the_end": "엔드",
    }
    ko.update(groups)

    block_item = {
        "block.waystones.andesite_waystone": "전송석",
        "block.waystones.mossy_andesite_waystone": "이끼 낀 전송석",
        "block.waystones.sandstone_waystone": "모래 전송석",
        "block.waystones.blackstone_waystone": "흑암 전송석",
        "block.waystones.deepslate_waystone": "심층암 전송석",
        "block.waystones.end_stone_waystone": "엔더 전송석",
        "block.waystones.red_nether_bricks_waystone": "네더 전송석",
        "block.waystones.purpur_waystone": "퍼퍼 전송석",
        "block.waystones.prismarine_waystone": "프리즈마린 전송석",
        "block.waystones.mud_bricks_waystone": "진흙 전송석",
        "block.waystones.copper_sharestone": "구리 공유석",
        "block.waystones.prismarine_sharestone": "프리즈마린 공유석",
        "block.waystones.gold_sharestone": "금 공유석",
        "block.waystones.diamond_sharestone": "다이아몬드 공유석",
        "block.waystones.amethyst_sharestone": "자수정 공유석",
        "block.waystones.lapis_sharestone": "청금석 공유석",
        "block.waystones.emerald_sharestone": "에메랄드 공유석",
        "block.waystones.redstone_sharestone": "레드스톤 공유석",
        "block.waystones.ruined_sharestone": "무너진 공유석",
        "block.waystones.warp_plate": "전송판",
        "block.waystones.warp_portal": "전송 포털",
        "block.waystones.fleeting_memorial": "덧없는 비석",
        "block.waystones.portstone": "사출석",
        "block.waystones.copper_portstone": "구리 사출석",
        "block.waystones.prismarine_portstone": "프리즈마린 사출석",
        "block.waystones.gold_portstone": "금 사출석",
        "block.waystones.diamond_portstone": "다이아몬드 사출석",
        "block.waystones.amethyst_portstone": "자수정 사출석",
        "block.waystones.lapis_portstone": "청금석 사출석",
        "block.waystones.emerald_portstone": "에메랄드 사출석",
        "block.waystones.redstone_portstone": "레드스톤 사출석",
        "item.waystones.portal_scroll": "포털 주문서",
        "item.waystones.copper_warp_stone": "구리 전송석",
        "item.waystones.prismarine_warp_stone": "프리즈마린 전송석",
        "item.waystones.gold_warp_stone": "금 전송석",
        "item.waystones.diamond_warp_stone": "다이아몬드 전송석",
        "item.waystones.amethyst_warp_stone": "자수정 전송석",
        "item.waystones.lapis_warp_stone": "청금석 전송석",
        "item.waystones.emerald_warp_stone": "에메랄드 전송석",
        "item.waystones.redstone_warp_stone": "레드스톤 전송석",
        "item.waystones.blank_scroll": "미연결 주문서",
        "item.waystones.dormant_shard": "잠든 파편",
        "item.waystones.crumbling_attuned_shard": "부서지는 감응 파편",
        "item.waystones.twinbound_feather": "쌍결 깃털",
        "item.waystones.epitaph": "비문",
    }
    ko.update(block_item)

    containers = {
        "container.waystones.fleeting_memorial": "덧없는 비석",
        "container.waystones.personal_waystone_settings": "%s",
        "container.waystones.waystone_modifiers": "전송 수정자",
        "container.waystones.manage_waystones": "전송석 관리",
        "container.waystones.manage_groups": "그룹 관리",
        "container.waystones.edit_group": "그룹 편집",
        "container.waystones.waystone_admin_selection": "<%s>의 어느 전송석으로 이동할지 선택:",
        "container.waystones.waystone": "전송석",
        "container.waystones.unscoped_portstone": "사출석",
        "container.waystones.copper_portstone": "구리 사출석",
        "container.waystones.prismarine_portstone": "프리즈마린 사출석",
        "container.waystones.gold_portstone": "금 사출석",
        "container.waystones.diamond_portstone": "다이아몬드 사출석",
        "container.waystones.amethyst_portstone": "자수정 사출석",
        "container.waystones.lapis_portstone": "청금석 사출석",
        "container.waystones.emerald_portstone": "에메랄드 사출석",
        "container.waystones.redstone_portstone": "레드스톤 사출석",
        "container.waystones.sharestone": "공유석",
    }
    ko.update(containers)

    gui = {
        "gui.waystones.manage_waystones.drag_to_reorder": "드래그하여 순서 변경. 더블 클릭하면 맨 위로, Shift+더블 클릭하면 맨 아래로 이동합니다.",
        "gui.waystones.manage_waystones.hide_waystone": "전송석 숨기기",
        "gui.waystones.manage_waystones.show_waystone": "전송석 표시",
        "gui.waystones.manage_groups.no_groups": "사용 가능한 그룹이 없습니다.",
        "gui.waystones.manage_groups.create": "그룹 만들기",
        "gui.waystones.manage_groups.hide_group": "내장 그룹 숨기기",
        "gui.waystones.manage_groups.show_group": "내장 그룹 표시",
        "gui.waystones.manage_groups.save": "저장",
        "gui.waystones.manage_groups.unnamed_group": "이름 없는 그룹",
        "gui.waystones.waystone_selection.manage": "관리",
        "gui.waystones.waystone_selection.back": "뒤로",
        "gui.waystones.waystone_selection.edit_personal_settings": "개인 설정",
        "gui.waystones.waystone_selection.sort": "정렬: %s",
        "gui.waystones.waystone_selection.sort.manual": "수동",
        "gui.waystones.waystone_selection.sort.name": "이름순",
        "gui.waystones.waystone_selection.sort.distance": "거리순",
        "gui.waystones.waystone_selection.no_sharestones_available": "전송할 수 있는 공유석이 없습니다.",
        "gui.waystones.waystone_selection.xp_requirement": "경험치 비용: %d",
        "gui.waystones.waystone_selection.item_requirement": "아이템 비용: %d %s",
        "gui.waystones.waystone_selection.epitaph_requirement": "%s",
        "gui.waystones.waystone_selection.twinbound_feather_requirement": "%s",
        "gui.waystones.waystone_selection.no_xp_requirement": "경험치 비용: 무료",
        "gui.waystones.waystone_selection.twinbound_feather": "%s",
        "gui.waystones.waystone_settings.modifiers_active": "수정자 %d개 활성",
        "gui.waystones.waystone_settings.no_modifiers_active": "활성 수정자 없음",
        "gui.waystones.waystone_settings.personal_settings": "개인 설정",
        "gui.waystones.waystone_settings.save": "저장",
        "gui.waystones.waystone_settings.close": "닫기",
        "gui.waystones.waystone_settings.manage_modifiers": "수정자 관리",
        "gui.waystones.personal_waystone_settings.configure_waystone": "전송석 편집",
        "gui.waystones.personal_waystone_settings.no_alias": "별칭 없음",
        "gui.waystones.personal_waystone_settings.favorite": "즐겨찾기",
        "gui.waystones.personal_waystone_settings.no_group": "그룹 없음",
        "gui.waystones.personal_waystone_settings.no_groups_defined": "정의된 그룹 없음",
        "gui.waystones.personal_waystone_settings.save": "저장",
        "gui.waystones.waystone_settings.visibility.activation": "활성화 시 표시",
        "gui.waystones.waystone_settings.visibility.global": "모두에게 표시",
        "gui.waystones.waystone_settings.visibility.shard_only": "감응 파편으로만 전송",
        "gui.waystones.waystone_settings.visibility.sharestones": "같은 공유석에 표시",
        "gui.waystones.waystone_settings.visibility.team": "팀에게만 표시",
        "gui.waystones.group_settings.color": "라벨 색: %s",
        "gui.waystones.group_settings.icon": "아이콘: %s",
        "gui.waystones.waystone_selection.return_to_portal": "포털로 돌아가기",
    }
    ko.update(gui)

    chat = {
        "chat.waystones.cannot_break_waystone": "이 전송석을 부술 권한이 없습니다.",
        "chat.waystones.not_allowed_to_edit": "이 전송석을 편집할 권한이 없습니다.",
        "chat.waystones.only_owner_can_edit": "소유자만 이 전송석을 편집할 수 있습니다.",
        "chat.waystones.warp_portal_no_space": "여기에 전송 포털을 열 공간이 없습니다.",
        "chat.waystones.warp_portal_attunement_lost": "목적지가 파괴되어 포털이 닫혔습니다.",
        "chat.waystones.not_enough_xp": "전송할 경험치가 부족합니다.",
        "chat.waystones.requirements_not_met": "전송 요구 조건을 충족하지 못했습니다.",
        "chat.waystones.requirements_not_met.specific": "실패 — %s",
        "chat.waystones.teleport_no_longer_valid": "전송을 완료할 수 없습니다.",
        "chat.waystones.source_item_missing": "전송에 필요한 아이템을 들고 있지 않습니다.",
        "chat.waystones.source_waystone_out_of_range": "전송석에서 너무 멀리 이동했습니다.",
        "chat.waystones.teleport_failed": "전송에 실패했습니다.",
        "chat.waystones.destination_out_of_bounds": "목적지가 월드 경계 밖입니다.",
        "chat.waystones.destination_chunk_load_failed": "목적지 청크를 불러올 수 없습니다.",
    }
    ko.update(chat)

    tooltips = {
        "tooltip.waystones.ruined_sharestone": "(( 이 공유석은 모드에서 제거되었습니다. ))",
        "tooltip.waystones.copper_sharestone": "다른 구리 공유석으로 전송",
        "tooltip.waystones.prismarine_sharestone": "다른 프리즈마린 공유석으로 전송",
        "tooltip.waystones.gold_sharestone": "다른 금 공유석으로 전송",
        "tooltip.waystones.diamond_sharestone": "다른 다이아몬드 공유석으로 전송",
        "tooltip.waystones.amethyst_sharestone": "다른 자수정 공유석으로 전송",
        "tooltip.waystones.lapis_sharestone": "다른 청금석 공유석으로 전송",
        "tooltip.waystones.emerald_sharestone": "다른 에메랄드 공유석으로 전송",
        "tooltip.waystones.redstone_sharestone": "다른 레드스톤 공유석으로 전송",
        "tooltip.waystones.attuned_shard.attunement_crumbling": "다음 전송 시 소모됩니다.",
        "tooltip.waystones.dormant_shard.plug_into_warp_plate": "전송판에 파편을 넣어 감응시키세요.",
        "tooltip.waystones.edit_restricted": "이 전송석은 편집할 수 없습니다.",
        "tooltip.waystones.blank_scroll": "전송석에 사용해 연결하세요.",
    }
    ko.update(tooltips)

    misc = {
        "stat.waystones.waystone_activated": "활성화한 전송석",
        "waystones.untitled_waystone": "이름 없는 전송석",
        "waystones.map.untitled_waystone": "이름 없는 전송석",
        "config.jade.plugin_waystones.waystone": "전송석",
    }
    ko.update(misc)

    commands = {
        "commands.waystones.count": "%s은(는) 전송석 %d개를 활성화했고 그중 %d개를 소유합니다",
        "commands.waystones.list.header": "%s의 전송석 (x, y, z) (소유만):",
        "commands.waystones.list.entry.owned": "-     소유 %s (%s): %s",
        "commands.waystones.list.entry.activated": "- 활성 %s (%s): %s",
        "commands.waystones.list.footer": "소유 전송석 %d개",
        "commands.waystones.list.all.header": "%s의 전송석 (x, y, z):",
        "commands.waystones.list.all.footer": "전체 %d개, 소유 %d개",
        "commands.waystones.list.coordinates": "%d, %d, %d",
        "commands.waystones.list.in_dimension": "%s에 있음",
        "commands.waystones.list.at_distance": "%d블록 거리",
        "commands.waystones.activate.success.single": "%s에게 전송석 %s 활성화",
        "commands.waystones.activate.success.multiple": "%d명에게 전송석 %s 활성화",
        "commands.waystones.forget.success.single": "%s의 전송석 %s 비활성화",
        "commands.waystones.forget.success.multiple": "%d명의 전송석 %s 비활성화",
        "commands.waystones.forget.all.success.single": "%s의 모든 전송석 비활성화",
        "commands.waystones.forget.all.success.multiple": "%d명의 모든 전송석 비활성화",
        "commands.waystones.cooldown.reset.all.success.single": "%s의 모든 재사용 대기 초기화",
        "commands.waystones.cooldown.reset.all.success.multiple": "%d명의 모든 재사용 대기 초기화",
        "commands.waystones.cooldown.reset.invalid_identifier": "잘못된 식별자: %s",
        "commands.waystones.cooldown.reset.success.single": "%s의 재사용 대기 %s 초기화",
        "commands.waystones.cooldown.reset.success.multiple": "%d명의 재사용 대기 %s 초기화",
        "commands.waystones.twinbound.success": "%s와(과) %s의 쌍결 깃털 연결",
        "commands.waystones.twinbound.already_linked": "%s와(과) %s는 이미 쌍결 깃털이 연결되어 있습니다",
        "commands.waystones.twinbound.same_player": "서로 다른 두 플레이어를 선택하세요",
        "commands.waystones.twinbound.no_space": "%s에게 쌍결 깃털을 넣을 빈 인벤토리 칸이 필요합니다",
        "commands.waystones.waystone_not_found": "이 위치에 전송석이 없습니다",
    }
    ko.update(commands)

    config = {
        "waystones.configuration.rules": "규칙",
        "waystones.configuration.rules.allowEveryoneToManageGlobalWaystones": "모두 글로벌 전송석 관리 허용",
        "waystones.configuration.rules.allowEveryoneToManageGlobalWaystones.tooltip": "true면 모든 플레이어가 글로벌 전송석을 관리할 수 있습니다.",
        "waystones.configuration.rules.defaultVisibility": "기본 공개 범위",
        "waystones.configuration.rules.defaultVisibility.tooltip": "새로 설치·발견한 전송석의 기본 공개 범위를 \"TEAM\" 또는 \"GLOBAL\"로 설정합니다.",
        "waystones.configuration.rules.enableXpCosts": "경험치 비용 사용",
        "waystones.configuration.rules.enableXpCosts.tooltip": "false면 모든 경험치 비용을 끕니다. 세부 설정은 warpRequirements를 참고하세요.",
        "waystones.configuration.rules.enableModifiers": "수정자 사용",
        "waystones.configuration.rules.enableModifiers.tooltip": "true면 전송 시 상태 효과를 주는 수정자 아이템을 사용합니다.",
        "waystones.configuration.rules.enableDurability": "내구도 비용 사용",
        "waystones.configuration.rules.enableDurability.tooltip": "false면 모든 내구도 비용을 끕니다. 세부 설정은 warpRequirements를 참고하세요.",
        "waystones.configuration.rules.enableCooldowns": "재사용 대기 사용",
        "waystones.configuration.rules.enableCooldowns.tooltip": "false면 모든 재사용 대기를 끕니다. 세부 설정은 warpRequirements를 참고하세요.",
        "waystones.configuration.rules.warpSettings": "전송 설정",
        "waystones.configuration.rules.warpSettings.tooltip": "warpRequirements 설정에서 쓰는 변수 목록입니다.",
        "waystones.configuration.rules.warpRequirements": "전송 요구 조건",
        "waystones.configuration.rules.warpRequirements.tooltip": "Shogi 형식의 전송 요구 조건 목록입니다.",
        "waystones.configuration.rules.transportPets": "애완동물 동반 전송",
        "waystones.configuration.rules.transportPets.tooltip": "ENABLED: 근처 애완동물이 함께 전송. SAME_DIMENSION: 같은 차원일 때만. DISABLED: 끔.",
        "waystones.configuration.rules.transportLeashed": "끈 묶인 몹 동반 전송",
        "waystones.configuration.rules.transportLeashed.tooltip": "ENABLED: 끈 묶인 몹이 함께 전송. SAME_DIMENSION: 같은 차원일 때만. DISABLED: 끔.",
        "waystones.configuration.rules.entityDenyList": "전송 금지 개체 목록",
        "waystones.configuration.rules.entityDenyList.tooltip": "애완·끈·전송판으로 전송할 수 없는 개체 목록입니다.",
        "waystones.configuration.inventoryButton.inventoryButton.tooltip": "NONE: 버튼 없음. NEAREST: 가장 가까운 전송석으로. ANY: 선택 메뉴. 특정 이름: 해당 전송석으로.",
        "waystones.configuration.inventoryButton.inventoryButtonX": "전송 버튼 X 위치",
        "waystones.configuration.inventoryButton.inventoryButtonX.tooltip": "인벤토리에서 전송 버튼의 X 좌표입니다.",
        "waystones.configuration.inventoryButton.inventoryButtonY": "전송 버튼 Y 위치",
        "waystones.configuration.inventoryButton.inventoryButtonY.tooltip": "인벤토리에서 전송 버튼의 Y 좌표입니다.",
        "waystones.configuration.inventoryButton.creativeInventoryButtonX": "크리에이티브 전송 버튼 X",
        "waystones.configuration.inventoryButton.creativeInventoryButtonX.tooltip": "크리에이티브 메뉴에서 전송 버튼 X 좌표입니다.",
        "waystones.configuration.inventoryButton.creativeInventoryButtonY": "크리에이티브 전송 버튼 Y",
        "waystones.configuration.inventoryButton.creativeInventoryButtonY.tooltip": "크리에이티브 메뉴에서 전송 버튼 Y 좌표입니다.",
        "waystones.configuration.worldGen.wildWaystoneStyle": "야생 전송석 스타일",
        "waystones.configuration.worldGen.wildWaystoneStyle.tooltip": "DEFAULT: 기본 텍스처만. MOSSY/SANDY: 모두 해당 변형. BIOME: 바이옴에 따라 스타일 결정.",
        "waystones.configuration.worldGen.chunksBetweenWildWaystones": "야생 전송석 청크 간격",
        "waystones.configuration.worldGen.chunksBetweenWildWaystones.tooltip": "야생 전송석 생성 간격(청크). 0이면 생성 안 함.",
        "waystones.configuration.worldGen.wildWaystonesDimensionAllowList": "차원 허용 목록",
        "waystones.configuration.worldGen.wildWaystonesDimensionAllowList.tooltip": "야생 전송석 생성이 허용되는 차원. 비우면 deny 목록을 제외한 차원에서 `has_structure/waystone` 태그 바이옴에 생성됩니다.",
        "waystones.configuration.worldGen.wildWaystonesDimensionDenyList": "차원 차단 목록",
        "waystones.configuration.worldGen.wildWaystonesDimensionDenyList.tooltip": "allow 목록이 비었을 때, 태그에 있어도 생성하지 않을 차원 목록입니다.",
        "waystones.configuration.worldGen.nameGenerationMode.tooltip": "PRESET_FIRST: preset 이름 우선. PRESET_ONLY: preset만. MIXED: preset과 무작위 혼합.",
        "waystones.configuration.worldGen.nameGenerationTemplate": "이름 생성 템플릿",
        "waystones.configuration.worldGen.nameGenerationTemplate.tooltip": "새 이름 생성 템플릿. {Biome}, {MrPork} 자리표시자를 지원합니다.",
        "waystones.configuration.worldGen.nameGenerationPresets": "이름 생성 preset",
        "waystones.configuration.worldGen.nameGenerationPresets.tooltip": "PRESET 모드에서 사용할 이름 목록입니다.",
        "waystones.configuration.worldGen.spawnInVillages.tooltip": "REGULAR: 일부 마을. FREQUENT: 대부분 마을. DISABLED: 마을 생성 끔.",
        "waystones.configuration.client.disableTextGlow.tooltip": "true면 전송석 글자가 항상 최대 밝기로 그려지지 않습니다.",
        "waystones.configuration.journeyMap": "JourneyMap 연동",
        "waystones.configuration.journeyMap.enabled": "JourneyMap 지원",
        "waystones.configuration.journeyMap.enabled.tooltip": "true면 활성화한 전송석마다 JourneyMap 웨이포인트를 만듭니다.",
        "waystones.configuration.journeyMap.preferJourneyMapIntegrationMod": "'JourneyMap Integration' 모드 우선",
        "waystones.configuration.journeyMap.preferJourneyMapIntegrationMod.tooltip": "true면 'JourneyMap Integration' 모드가 없을 때만 웨이포인트를 만듭니다.",
        "waystones.configuration.dynmap": "Dynmap 연동",
        "waystones.configuration.dynmap.enabled": "Dynmap 지원",
        "waystones.configuration.dynmap.enabled.tooltip": "true면 Dynmap에 전송석·공유석 마커를 추가합니다.",
        "waystones.configuration.blueMap": "BlueMap 연동",
        "waystones.configuration.blueMap.enabled": "BlueMap 지원",
        "waystones.configuration.blueMap.enabled.tooltip": "Waystones BlueMap 연동 로드 여부",
        "waystones.configuration.blueMap.includeWaystones": "전송석 포함",
        "waystones.configuration.blueMap.includeWaystones.tooltip": "true면 BlueMap에 전송석 마커 표시",
        "waystones.configuration.blueMap.includeSharestones": "공유석 포함",
        "waystones.configuration.blueMap.includeSharestones.tooltip": "true면 BlueMap에 공유석 마커 표시",
        "waystones.configuration.blueMap.includeUndiscoveredWaystones": "미발견 전송석 포함",
        "waystones.configuration.blueMap.includeUndiscoveredWaystones.tooltip": "true면 미발견 전송석도 BlueMap 마커로 표시",
    }
    ko.update(config)

    for key in missing:
        if key not in ko:
            raise KeyError(f"waystones missing translation: {key}")
    return ko


def pet_bed_name(en: str) -> str | None:
    m = re.match(r"^(?P<c>.+) Pet Bed$", en)
    if not m:
        return None
    c = m.group("c")
    ko = MC_COLORS.get(c)
    if ko:
        return f"{ko}색 펫 침대"
    return None


def pet_home_translations(missing: dict[str, str]) -> dict[str, str]:
    ko: dict[str, str] = {}

    for key, en in missing.items():
        if key.startswith(("block.pet_home.pet_bed_", "item.pet_home.pet_bed_")):
            bed = pet_bed_name(en)
            if bed:
                ko[key] = bed
                continue
        if key in ("block.pet_home.drum", "item.pet_home.drum"):
            ko[key] = "명령 북"
            continue
        if key in ("block.pet_home.wayward_lantern", "item.pet_home.wayward_lantern"):
            ko[key] = "방황 등불"
            continue

    enchants = {
        "amphibious": ("양서", "육지·물 모두에서 펫이 마르거나 익사하지 않으며, 물에서 더 빠르게 헤엄칩니다. 떠오르지 않고 빠르게 수영합니다."),
        "blazing_protection": ("타오르는 보호", "레벨당 타오르는 보호 막대 2개가 공격자를 밀치고 불태웁니다."),
        "blight_curse": ("고사의 저주", "펫 주변 식물이 시들고 죽습니다."),
        "bubbling": ("거품", "공격 시 대상을 거대한 거품에 가두어 위로 띄웁니다."),
        "chain_lightning": ("연쇄 번개", "공격할 때마다 번개가 튕기며, 레벨이 높을수록 더 많은 몹을 맞춥니다."),
        "chaos": ("혼돈의 정신", "공격받으면 공격자에게 혼돈을 걸어 10블록 안 다른 몬스터를 공격하게 합니다."),
        "deflection": ("반사", "유령 방패가 투사체 공격을 막아줍니다."),
        "defusal": ("해제", "폭발이 지형·다른 몹에 피해를 주지 않게 하며, 레벨이 높을수록 범위가 넓어집니다."),
        "fireproof": ("방화", "불·용암 피해에 면역입니다."),
        "frost_fang": ("서리 송곳니", "공격이 잠시 둔화시키고 냉기 피해를 줍니다."),
        "gluttonous": ("폭식", "식단 제한 없이 어떤 음식이든 먹을 수 있습니다."),
        "healing_aura": ("치유 오라", "가끔 주인과 주변 펫을 치유하며, 레벨이 높을수록 강합니다."),
        "health_boost": ("추가 체력", "레벨당 최대 체력 +10"),
        "health_siphon": ("체력 흡수", "펫이 받는 피해가 주인에게 전달됩니다."),
        "herding": ("몰이", "야생 동물이 따라오며, 레벨이 높을수록 더 많이 따릅니다."),
        "immaturity_curse": ("미성숙의 저주", "항상 아기처럼 보이고 행동하며 공격력이 줄어듭니다."),
        "immunity_frame": ("면역 프레임", "레벨당 공격 후 추가로 면역인 시간 +1초"),
        "infamy_curse": ("악명의 저주", "근처 몬스터가 자연스럽게 펫을 적대합니다."),
        "insight": ("통찰", "어두운 곳에서 주변 적대 몹을 강조 표시합니다."),
        "intimidation": ("위협", "무서운 모습으로 적을 쫓으며, 레벨이 범위·빈도를 결정합니다."),
        "linked_inventory": ("연결 인벤토리", "아이템을 줍어 주인 인벤토리로 순간이동시킵니다."),
        "magnetic": ("자석", "유령 자석이 대상 몹을 펫 쪽으로 끌어당깁니다."),
        "night_vision": ("야간 투시", "펫이 가까이 있으면 야간 투시 효과를 줍니다."),
        "ore_scenting": ("광석 후각", "광석을 찾아 주인에게 알리며, 레벨이 거리·개수·빈도를 결정합니다."),
        "paralysis": ("마비", "공격받은 적을 잠시 움직이·점프·공격하지 못하게 합니다."),
        "poison_resistance": ("독 저항", "독 효과에 면역입니다."),
        "psychic_wall": ("사이킥 벽", "전투 중 에너지 벽을 만들어 엄폐하며, 레벨이 크기·지속을 결정합니다."),
        "rejuvenation": ("재생", "부상 시 경험치 구슬을 흡수해 스스로 치유할 수 있습니다."),
        "shadow_hands": ("그림자 손", "어둠의 손으로 공격하며, 레벨이 속도·손 개수를 높입니다."),
        "share": ("분담", "펫이 공격받으면 근처 적대 몹과 피해를 나눕니다."),
        "sonic_boom": ("소닉 붐", "적을 넉백시키는 소닉 붐을 냅니다."),
        "speedster": ("질주", "이동 속도가 빨라집니다."),
        "tethered_teleport": ("연결 순간이동", "차원을 넘어 주인과 함께 순간이동합니다."),
        "total_recall": ("전원 소환", "체력 2 미만이면 소환 구슬로 보호되며 주인이 풀어줄 때까지 안전합니다."),
        "tough": ("강인", "방어력과 넉백 저항이 증가합니다."),
        "vampire": ("흡혈귀", "가한 피해만큼 회복하며, 레벨이 회복 비율을 높입니다."),
        "violent": ("난폭", "펫이 공격한 몬스터에 디버프를 겁니다."),
        "void_cloud": ("공허 구름", "낭떠러지·공허 낙하 시 구름이 들어 올려 주인에게 돌려보냅니다."),
        "warping_bite": ("요동 물기", "대상 몹을 무작위로 멀리 순간이동시킵니다."),
        "xp_transfer": ("경험치 전달", "경험치를 모아 주인에게 전달합니다."),
    }
    for slug, (name, desc) in enchants.items():
        ko[f"enchantment.pet_home.{slug}"] = name
        ko[f"enchantment.pet_home.{slug}.desc"] = desc

    static = {
        "effect.pet_home.drunk": "혼돈",
        "entity.minecraft.killer_bunny": "살인 토끼",
        "entity.minecraft.villager.pet_home.animal_tamer": "조련사",
        "entity.pet_home.chain_lightning": "번개",
        "entity.pet_home.feather": "깃털",
        "entity.pet_home.following_jukebox": "떠다니는 주크박스",
        "entity.pet_home.psychic_wall": "사이킥 벽",
        "entity.pet_home.recall_ball": "소환 구슬",
        "item.pet_home.collar_tag": "목줄 태그",
        "item.pet_home.deed_of_ownership": "펫 방출",
        "item.pet_home.deed_of_ownership.desc": "펫을 우클릭하면 방출할 수 있습니다",
        "item.pet_home.deflection_shield": "반사 방패 모형",
        "item.pet_home.feather_on_a_stick": "깃털 낚싯대",
        "item.pet_home.magnet": "자석 모형",
        "item.pet_home.net": "포획구",
        "item.pet_home.net_has_item": "포획구 (사용 중)",
        "item.pet_home.net_launcher": "생물 포획 발사기",
        "item.pet_home.pet_compass": "펫 나침반",
        "item.pet_home.rotten_apple": "썩은 사과",
        "item.pet_home.sinister_carrot": "음흉한 당근",
        "itemGroup.pet_home": "Pet Home | 아이템",
        "config.jade.plugin_pet_home.collar_tag": "목줄 설명",
        "message.pet_home.command_0": "%s이(가) 배회 중입니다",
        "message.pet_home.command_1": "%s이(가) 대기 중입니다",
        "message.pet_home.command_2": "%s이(가) 따라오는 중입니다",
        "message.pet_home.drum_command_0": "%s에게 배회 명령",
        "message.pet_home.drum_command_1": "%s에게 대기 명령",
        "message.pet_home.drum_command_2": "%s에게 따라오기 명령",
        "message.pet_home.enchantments": "인챈트:",
        "message.pet_home.goodbye": "%s은(는) 다시 나타나지 않습니다. 안녕…",
        "message.pet_home.pet_compass.button_recall": "소환",
        "message.pet_home.pet_compass.button_tp": "펫에게 이동",
        "message.pet_home.pet_compass.dead_wait_respawn": "이 펫은 죽었으며 펫 침대에서 리스폰을 기다립니다",
        "message.pet_home.pet_compass.dim_overworld": "오버월드",
        "message.pet_home.pet_compass.dim_the_end": "엔드",
        "message.pet_home.pet_compass.dim_the_nether": "네더",
        "message.pet_home.pet_compass.disabled": "설정에서 펫 나침반이 비활성화되어 있습니다",
        "message.pet_home.pet_compass.distance": "거리: %s m",
        "message.pet_home.pet_compass.empty": "추적 가능한 펫이 없습니다",
        "message.pet_home.pet_compass.last_known_pos": "마지막 위치: ",
        "message.pet_home.pet_compass.not_loaded_confirm": "펫이 로드되지 않았습니다. 마지막 위치로 순간이동합니다",
        "message.pet_home.pet_compass.position": "좌표: ",
        "message.pet_home.pet_compass.recall_failed": "펫을 찾을 수 없습니다. 나중에 다시 시도하세요",
        "message.pet_home.pet_compass.recall_started": "펫을 찾는 중입니다. 잠시 기다려 주세요…",
        "message.pet_home.pet_compass.recalled": "%s을(를) 소환했습니다",
        "message.pet_home.pet_compass.status_dead": "상태: 사망, 펫 침대 리스폰 대기",
        "message.pet_home.pet_compass.status_loaded": "상태: 로드됨",
        "message.pet_home.pet_compass.status_not_loaded": "상태: 로드 안 됨",
        "message.pet_home.pet_compass.status_wrong_dim": "상태: 다른 차원",
        "message.pet_home.pet_compass.teleported": "펫에게 순간이동했습니다",
        "message.pet_home.pet_compass.title": "펫 나침반",
        "message.pet_home.remove_respawn": "%s의 리스폰 침대를 제거했습니다",
        "message.pet_home.respawn": "%s이(가) 침대에서 리스폰했습니다",
        "message.pet_home.set_owner": "%s이(가) %s의 주인이 되었습니다",
        "message.pet_home.wayward_lantern_return": "%s이(가) 근처 방황 등불로 돌아왔습니다",
        "notif.friendlyfire.protected": "%s은(는) 보호 중입니다. 우회하려면 웅크리세요.",
        "text.pet_home.capturing": "포획 중",
        "text.pet_home.has_petbed_at_pos": "펫 침대 위치 ( %s )",
        "text.pet_home.health": "체력",
        "text.pet_home.net_launcher_default_only_tamable": "기본적으로 길들일 수 있는 몹만 포획할 수 있습니다(설정에서 변경 가능).",
        "text.pet_home.net_launcher_tip": "[Shift+우클릭: 포획/방출 모드 전환]",
        "text.pet_home.network_failed": "네트워크 오류, %s",
        "text.pet_home.no_net_entity": "방출할 생물이 없습니다",
        "text.pet_home.releasing": "방출",
        "tooltips.pet_home.drum.desc": "[우클릭: 32블록 안 펫 명령 순환, 레드스톤으로도 작동]",
        "tooltips.pet_home.pet_compass.desc": "우클릭으로 펫 목록 열기",
        "tooltips.pet_home.substitute_collar.desc": "[펫 우클릭: 목줄 착용]",
        "tooltips.pet_home.substitute_feather.desc": "[낚싯대 착지점으로 펫 이동, 펫 침대 배치 등]",
        "tooltips.pet_home.substitute_pet_bed.desc": "[펫을 침대에 두면, 죽은 뒤 다음 날 리스폰]",
        "tooltips.pet_home.substitute_rotten_apple.desc": "[말에게 먹이면 좀비 말로 변함]",
        "tooltips.pet_home.substitute_sinister_carrot.desc": "[토끼→살인 토끼, 좀비 말→스켈레톤 말]",
        "tooltips.pet_home.wayward_lantern.desc": "[근처에 두면 멀리 언로드된 펫이 등불로 순간이동]",
    }
    ko.update(static)

    cfg = {
        "pet_home.configuration.animal_tamer_villager": "조련사",
        "pet_home.configuration.title": "Pet Home 설정",
        "pet_home.configuration.section.pet.home.common.toml": "Pet Home 공통 설정",
        "pet_home.configuration.section.pet.home.common.toml.title": "Pet Home 공통 설정",
        "pet_home.configuration.displayHitWarning": "보호 경고 표시",
        "pet_home.configuration.displayHitWarning.tooltip": "보호 경고 메시지 표시 여부",
        "pet_home.configuration.petCompassEnable": "펫 나침반",
        "pet_home.configuration.petCompassEnable.tooltip": "펫 나침반 시스템 사용",
        "pet_home.configuration.petInfoOverlay": "펫 정보 패널",
        "pet_home.configuration.petInfoOverlay.tooltip": "내 펫을 볼 때 화면 상단에 정보 패널 표시",
        "pet_home.configuration.petInfoOverlayIgnoreJade": "Jade 있어도 표시",
        "pet_home.configuration.petInfoOverlayIgnoreJade.tooltip": "Jade 설치 시 기본 숨김 — 켜면 Jade와 함께 표시",
        "pet_home.configuration.petInfoOverlayRequireShift": "웅크림 필요",
        "pet_home.configuration.petInfoOverlayRequireShift.tooltip": "웅크린 채 펫을 볼 때만 패널 표시",
        "pet_home.configuration.pet_bed_respawns": "펫 침대 리스폰",
        "pet_home.configuration.pet_bed_respawns.tooltip": "펫이 죽으면 다음 날 침대에서 리스폰",
        "pet_home.configuration.trinary_command_system": "3단계 명령",
        "pet_home.configuration.trinary_command_system.tooltip": "배회/따라오기/대기 명령 사용",
        "pet_home.configuration.teleportPetToPlayer": "펫 소환",
        "pet_home.configuration.teleportPetToPlayer.tooltip": "펫을 플레이어에게 순간이동(소환) 허용",
        "pet_home.configuration.teleportPlayerToPet": "플레이어→펫 순간이동",
        "pet_home.configuration.teleportPlayerToPet.tooltip": "플레이어가 펫에게 순간이동 허용",
        "pet_home.configuration.protectPetsFromOwner": "주인이 펫 공격 불가",
        "pet_home.configuration.protectPetsFromOwner.tooltip": "주인이 자신의 펫을 해칠 수 없음",
        "pet_home.configuration.protectPetsFromPets": "펫 간 피해 없음",
        "pet_home.configuration.protectPetsFromPets.tooltip": "펫끼리 서로 피해를 주지 않음",
        "pet_home.configuration.protectTeamMembers": "팀원 간 피해 없음",
        "pet_home.configuration.protectTeamMembers.tooltip": "같은 팀 생물끼리 피해 없음",
        "pet_home.configuration.protectChildren": "어린 생물 보호",
        "pet_home.configuration.protectChildren.tooltip": "어린 생물 보호 여부",
        "pet_home.configuration.reflectDamage": "피해 반사",
        "pet_home.configuration.reflectDamage.tooltip": "생물 공격 시 주인도 피해(반사)",
        "pet_home.configuration.respectTeamRules": "팀 규칙 따름",
        "pet_home.configuration.respectTeamRules.tooltip": "팀 설정에 따라 아군 공격 허용/차단",
        "pet_home.configuration.rotten_apple": "사과→썩은 사과",
        "pet_home.configuration.rotten_apple.tooltip": "사과가 사라질 때 썩은 사과로 변함",
        "pet_home.configuration.mobcatcherOnlyTamableAnimal": "길들일 수 있는 몹만 포획",
        "pet_home.configuration.mobcatcherOnlyTamableAnimal.tooltip": "끄면 모든 몹 포획 가능",
        "pet_home.configuration.mobcatcherBlacklist": "포획 금지 목록",
        "pet_home.configuration.mobcatcherBlacklist.button": "편집…",
        "pet_home.configuration.mobcatcherBlacklist.tooltip": "포획할 수 없는 몹",
        "pet_home.configuration.can_hurt_pet_item": "펫 공격 아이템",
        "pet_home.configuration.can_hurt_pet_item.button": "편집…",
        "pet_home.configuration.can_hurt_pet_item.tooltip": "Shift 없이 펫을 해칠 수 있는 아이템",
        "pet_home.configuration.can_hurt_all": "전체 보호 우회 아이템",
        "pet_home.configuration.can_hurt_all.button": "편집…",
        "pet_home.configuration.can_hurt_all.tooltip": "Shift 없이 보호를 무시하고 해칠 수 있는 아이템",
        "pet_home.configuration.no_protection_entity": "보호 제외 생물",
        "pet_home.configuration.no_protection_entity.button": "편집…",
        "pet_home.configuration.no_protection_entity.tooltip": "보호하지 않을 생물(예: 피글린)",
        "pet_home.configuration.player_cant_hurt_entity": "플레이어 공격 면역",
        "pet_home.configuration.player_cant_hurt_entity.button": "편집…",
        "pet_home.configuration.player_cant_hurt_entity.tooltip": "플레이어가 공격할 수 없는 생물",
        "pet_home.configuration.other_should_protect_entity": "모든 공격 면역",
        "pet_home.configuration.other_should_protect_entity.button": "편집…",
        "pet_home.configuration.other_should_protect_entity.tooltip": "모든 공격에 면역인 개체",
        "pet_home.configuration.rabbits_scare_ravagers": "토끼가 파괴수 겁줌",
        "pet_home.configuration.rabbits_scare_ravagers.tooltip": "예전처럼 토끼가 파괴수를 겁줄지 여부",
        "pet_home.configuration.tameable_axolotls": "아홀로틀 길들이기",
        "pet_home.configuration.tameable_axolotls.tooltip": "열대어로 길들일 수 있게 함",
        "pet_home.configuration.tameable_fox": "여우 길들이기",
        "pet_home.configuration.tameable_fox.tooltip": "달콤한 열매로 길들일 수 있게 함",
        "pet_home.configuration.tameable_frog": "개구리 길들이기",
        "pet_home.configuration.tameable_frog.tooltip": "거미 눈으로 길들일 수 있게 함",
        "pet_home.configuration.tameable_horse": "말 길들이기",
        "pet_home.configuration.tameable_horse.tooltip": "말을 완전히 길들일 수 있게 함",
        "pet_home.configuration.tameable_rabbit": "토끼 길들이기",
        "pet_home.configuration.tameable_rabbit.tooltip": "당근으로 길들일 수 있게 함",
        "pet_home.configuration.petstore_village_weight": "마을 조련사 건물 가중치",
        "pet_home.configuration.petstore_village_weight.tooltip": "마을에 조련사 건물 생성 가중치(월드 재접속 필요)",
    }
    ko.update(cfg)

    loot = {
        "pet_home.configuration.blazing_protection_loot_chance": "타오르는 보호 마법책 확률",
        "pet_home.configuration.blazing_protection_loot_chance.tooltip": "네더 요새 상자",
        "pet_home.configuration.bubbling_loot_chance": "거품 마법책 확률",
        "pet_home.configuration.bubbling_loot_chance.tooltip": "보물 상자",
        "pet_home.configuration.ore_scenting_loot_chance": "광석 후각 마법책 확률",
        "pet_home.configuration.ore_scenting_loot_chance.tooltip": "폐광 수레 상자",
        "pet_home.configuration.paralysis_loot_chance": "마비 마법책 확률",
        "pet_home.configuration.paralysis_loot_chance.tooltip": "폐광·사막 사원·낚시 전리품",
        "pet_home.configuration.share_loot_chance": "분담 마법책 확률",
        "pet_home.configuration.share_loot_chance.tooltip": "엔드 도시 상자",
        "pet_home.configuration.sinister_carrot_loot_chance": "음흉한 당근 확률",
        "pet_home.configuration.sinister_carrot_loot_chance.tooltip": "상자",
        "pet_home.configuration.sonic_boom_loot_chance": "소닉 붐 마법책 확률",
        "pet_home.configuration.sonic_boom_loot_chance.tooltip": "고대 도시 상자",
        "pet_home.configuration.tough_loot_chance": "강인 마법책 확률",
        "pet_home.configuration.tough_loot_chance.tooltip": "폐광 상자",
        "pet_home.configuration.vampirism_loot_chance": "흡혈 마법책 확률",
        "pet_home.configuration.vampirism_loot_chance.tooltip": "삼림 대저택 상자",
    }
    ko.update(loot)

    sounds = {
        "pet_home.sound.subtitle.blazing_protection": "타오르는 막대 사라짐",
        "pet_home.sound.subtitle.chain_lightning": "번개 타격",
        "pet_home.sound.subtitle.collar_tag": "목줄 태그 장착",
        "pet_home.sound.subtitle.drum": "명령 북 울림",
        "pet_home.sound.subtitle.giant_bubble_inflate": "거대 거품 부풂",
        "pet_home.sound.subtitle.giant_bubble_pop": "거대 거품 터짐",
        "pet_home.sound.subtitle.magnet_loop": "자석 윙윙",
        "pet_home.sound.subtitle.pet_bed_uses": "펫 침대 조정",
        "pet_home.sound.subtitle.psychic_wall": "사이킥 벽 웅웅",
        "pet_home.sound.subtitle.psychic_wall_deflect": "사이킥 벽 반사",
    }
    ko.update(sounds)

    for key in missing:
        if key not in ko:
            raise KeyError(f"pet_home missing translation: {key}")
    return ko


FTBLIBRARY_KO: dict[str, str] = {
    "delete_item": "%s을(를) 삭제하시겠습니까?",
    "lat_support": "이 모드가 마음에 드시나요? 클릭하여 제작을 후원하세요!",
    "ftblibrary.select_image.scanning": "이미지 검색 중… 잠시 걸릴 수 있습니다.",
    "ftblibrary.select_image.rescan": "이미지 다시 검색",
    "ftblibrary.select_image.all_images": "모든 이미지",
    "ftblibrary.unsaved_changes": "저장하지 않은 변경을 버리시겠습니까?",
    "sidebar_button.ftblibrary.toggle.gamemode": "게임 모드 전환",
    "sidebar_button.ftblibrary.toggle.gamemode.tooltip": "서바이벌과 크리에이티브 모드를 전환합니다",
    "sidebar_button.ftblibrary.toggle.rain": "비 전환",
    "sidebar_button.ftblibrary.toggle.rain.tooltip": "맑음과 비를 전환합니다",
    "sidebar_button.ftblibrary.toggle.day": "낮으로 설정",
    "sidebar_button.ftblibrary.toggle.day.tooltip": "낮 시간으로 설정",
    "sidebar_button.ftblibrary.toggle.night": "밤으로 설정",
    "sidebar_button.ftblibrary.toggle.night.tooltip": "밤 시간으로 설정",
    "sidebar_button.ftblibrary.config": "FTB Library 클라이언트 설정",
    "ftblibrary.mb": "%s의 %d mB",
    "ftblibrary.empty": "비어 있음",
    "ftblibrary.read_only": "읽기 전용",
    "ftblibrary.client_settings": "클라이언트 설정",
    "ftblibrary.client_settings.tooltips": "툴팁",
    "ftblibrary.client_settings.tooltips.item_modname": "아이템 선택 GUI에 모드 이름 표시",
    "ftblibrary.client_settings.tooltips.fluid_modname": "유체 선택 GUI에 모드 이름 표시",
    "ftblibrary.client_settings.tooltips.image_modname": "이미지 선택 GUI에 모드 이름 표시",
    "ftblibrary.client_settings.tooltips.entity_modname": "개체 선택 GUI에 모드 이름 표시",
    "ftblibrary.client_settings.sidebar": "사이드바 버튼",
    "ftblibrary.client_settings.sidebar.enabled": "사이드바 버튼 사용",
    "ftblibrary.client_settings.sidebar.position": "사이드바 버튼 위치",
    "ftblibrary.palette.chat": "채팅 색",
    "ftblibrary.palette.dye": "염료 색",
    "ftblibrary.palette.nord": "Nord 테마",
    "ftblibrary.palette.reds": "빨강 계열",
    "ftblibrary.palette.greens": "초록 계열",
    "ftblibrary.palette.blues": "파랑 계열",
    "ftblibrary.palette.recent": "최근 색",
    "ftblibrary.panel.position.top_left": "왼쪽 위",
    "ftblibrary.panel.position.top": "위",
    "ftblibrary.panel.position.top_right": "오른쪽 위",
    "ftblibrary.panel.position.left": "왼쪽",
    "ftblibrary.panel.position.right": "오른쪽",
    "ftblibrary.panel.position.bottom_left": "왼쪽 아래",
    "ftblibrary.panel.position.bottom": "아래",
    "ftblibrary.panel.position.bottom_right": "오른쪽 아래",
    "ftblibrary.server_settings": "서버 설정",
    "dimension.minecraft.overworld": "오버월드",
    "dimension.minecraft.the_nether": "네더",
    "dimension.minecraft.the_end": "엔드",
    "dimension.hyperbox.hyperbox": "Hyperbox",
    "dimension.ae2.spatial_storage": "AE2 공간 저장소",
}

FTBQUESTS_KO = {"ftbquests": "FTB 퀘스트"}


def main() -> None:
    ws_missing = json.loads((MISSING / "waystones.json").read_text(encoding="utf-8"))
    ph_missing = json.loads((MISSING / "pet_home.json").read_text(encoding="utf-8"))
    counts = {
        "waystones": merge_write("waystones", waystones_translations(ws_missing)),
        "pet_home": merge_write("pet_home", pet_home_translations(ph_missing)),
        "ftblibrary": merge_write("ftblibrary", FTBLIBRARY_KO),
        "ftbquests": merge_write("ftbquests", FTBQUESTS_KO),
    }
    for modid, n in counts.items():
        print(f"{modid}: {n}")


if __name__ == "__main__":
    main()
