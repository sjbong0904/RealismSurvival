# Vendored runtime jars (shop stack)

Used by `-Pshopstack` / default `runClient` for the seasonal supply post.

| File | Source |
|------|--------|
| `qshop-neoforge-26.1.2-1.8.4.jar` | CurseForge Q Shop file `9060615` |
| `sg_economy-26.1.0.jar` | CurseForge SG-Economy API file `8573142` |
| `celesthyd-26.1.0.jar` | SG Economy dependency (SirGrantd) |
| `easy_npc-neoforge-26.1.2-7.16.0.jar` | Modrinth Easy NPC Core `kE7XEpsf` |
| `easy_npc_config_ui-neoforge-26.1.2-7.16.0.jar` | Modrinth Easy NPC Config UI `oO5EAPIF` |

These are **not** published inside the Realism Survival mod jar. Re-download if versions change (see `gradle.properties` comments / `docs/SHOP_SUPPLY_POST.md`).

## Oritech industrial progression

| File | Source |
|------|--------|
| `oritech-2.0.0-exp12-neoforge-26.1.2.jar` | Modrinth Oritech `eCP3mBH7` (beta) |
| `athena-4.7.3-neoforge-26.1.2.jar` | Modrinth Athena `8KRMFzZ7` (required) |
| `geckolib-5.5.2-neoforge-26.1.2.jar` | Modrinth GeckoLib `xfVfPcoC` (required by project metadata) |

Loaded only by the pack content runtime configuration; these files are not published inside the Realism Survival mod jar. Oritech Things is intentionally not included.

## Not in the repository

These jars are third-party files and are **not committed** (`.gitignore`: `libs/*.jar`). Download each file from the source listed above into `libs/` before `runClient`. Without them, `build` and `runGameTestServer` still work; `runClient` just starts without the Oritech and supply-shop stacks (or pass `-Ppackcontent=false -Pshopstack=false`).
