# TFC Ancient City development guide

This repository targets Minecraft 1.21.1, Java 21, NeoForge 21.1.264, and TerraFirmaCraft 4.2.12 or newer for Minecraft 1.21.1. The mod ID is `tfcancientcity`, the release version is `1.0.0`, and Java code belongs under `net.claustra01.tfcancientcity`.

Keep Ancient City integration narrow. The structure processor changes exactly the 21 block IDs documented in `PROJECT.md`, always uses Blackwood, and runs only for `minecraft:ancient_city/*` templates. Campfires, deepslate and its variants, Sculk, redstone, wool, and soul lighting remain vanilla. Furnace-family NBT is discarded when creating a Firepit; compatible state and other NBT are retained.

Loose Deepslate uses `GroundcoverBlock`, supports counts one through four, drops its represented count, and requires a sturdy support face. Do not add natural generation, Hardened Deepslate, collapse behavior, configuration files, or a dependency on TFC Overdims.

M02 inherits the v0.1.1 Ancient City biome tag. Do not add worldgen registries, override Vanilla Ancient City structure data, or alter Sculk and Warden behavior.

Ancient City chest loot is converted after rolling, not by overriding loot JSON. Do not ship `data/minecraft/loot_table/chests/ancient_city*.json`, shared replacement tables, or extra pools; Vanilla (including experimental feature packs) keeps every pool, roll, weight, condition, empty entry, and count. The Global Loot Modifier `tfcancientcity:tfc_loot_replacement` (`net.claustra01.tfcancientcity.loot`) is listed in `neoforge:loot_modifiers/global_loot_modifiers.json` as `tfcancientcity:chests/ancient_city` and `tfcancientcity:chests/ancient_city_ice_box`, each gated by `neoforge:loot_table_id` and using the `lategame` profile. Paths that skip GLMs, such as `LootTable#getRandomItemsRaw`, stay unconverted. `TfcLootRules` is shared in content with TFC Overdims and applies, per rolled stack, one replacement choice for the whole count, splitting at the target's max stack size:

| Source | Output (equal chance within each set) |
| --- | --- |
| diamond, emerald, lapis lazuli, amethyst shard | `tfc:gem/<amethyst, diamond, emerald, lapis_lazuli, opal, pyrite, ruby, sapphire, topaz>` |
| iron, gold, copper ingot / block | `tfc:metal/ingot/<metal>` / `tfc:metal/double_ingot/<metal>`, lategame metals `steel`, `black_steel` |
| iron nugget / gold nugget | `tfc:ore/small_<hematite, magnetite, limonite>` / `tfc:ore/small_native_gold` |
| coal, charcoal | `minecraft:charcoal`, `tfc:ore/bituminous_coal`, `tfc:ore/lignite`, `tfc:ore/graphite` |
| crying obsidian, quartz | `tfc:powder/flux`, glass, glass pane, `tfc:kaolin_clay`, `tfc:powder/wood_ash`, `tfc:powder/saltpeter`, `tfc:powder/sylvite` |
| 22 Vanilla foods, 4 seeds, chain | fixed same-state TFC food, `tfc:seeds/*`, `tfc:metal/chain/wrought_iron` |
| Vanilla tools, armor, shears, shield, horse armor | `tfc:metal/<shape>/steel`; families with parts give parts 2/3 and finished items 1/3 |

Equipment shapes are pickaxe: pickaxe_head, propick_head / pickaxe, propick; axe: axe_head, saw_blade / axe, saw; hoe: hoe_head, scythe_blade / hoe, scythe; sword: sword_blade, javelin_head, mace_head / sword, javelin, mace; shovel: shovel_head / shovel; helmet, chestplate, boots: unfinished_<part> / <part>; leggings: unfinished_greaves / greaves; shears, shield, horse_armor: finished only. Outputs are fresh stacks: source damage is not copied, parts are never enchanted, and TFC food gets its default creation and decay data. A stack keeps its original item when its target is missing, equals the source, or the stack carries components a fresh stack lacks, except damage and enchantments whose source function is declared. The `ancient_city` modifier declares `minecraft:enchant_with_levels` with `#minecraft:on_random_loot` for `diamond_hoe` and `diamond_leggings` (uniform 30–50) and `iron_leggings` (uniform 20–39); it re-runs only on finished replacements of enchanted sources. In Ancient City this converts amethyst shards, coal, the diamond hoe, both leggings, diamond horse armor, and the Ice Box baked potato; every other reward, including golden carrots, glow berries, enchanted books, and smithing templates, is unchanged.

M03 overrides only the Vanilla Deepslate and Cobbled Deepslate block loot tables. Connected mining drops Loose Deepslate uniformly from two through four, replacing v0.1.1's one-through-four range, while TFC isolated recovery returns one original block. Add only those two blocks to `tfc:breaks_when_isolated`; do not extend the behavior to ores, Reinforced Deepslate, derived building blocks, or general stone.

M04 registers `tfcancientcity:rock/cobble/deepslate` as a separate TFC-style Cobble block and connects only that block to `tfc:can_landslide`. Keep Vanilla Cobbled Deepslate and all Ancient City Deepslate blocks unchanged. Do not add collapse tags or recipes, Hardened Deepslate, or a custom landslide recipe; TFC's tag-backed landslide fallback supplies the behavior. Preserve the six Deepslate processing recipes, metamorphic rock Knapping tags and GUI material, and the three explicit item size definitions.

Use vanilla and NeoForge registries and data formats before custom mechanisms. Keep common code safe for dedicated servers. Add a GameTest for server-side behavior and an E2E screenshot scenario for client rendering changes. Run these commands from the repository root before handoff:

```text
./gradlew classes
./gradlew build
./gradlew runGameTestServer
```

Treat `PROJECT.md`, `.harness/`, and `.harness-state/` as read-only. Do not edit license files.
