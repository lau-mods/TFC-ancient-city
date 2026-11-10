# TFC Ancient City development guide

This repository targets Minecraft 1.21.1, Java 21, NeoForge 21.1.264, and TerraFirmaCraft 4.2.12 or newer for Minecraft 1.21.1. The mod ID is `tfcancientcity`, the release version is `1.0.0`, and Java code belongs under `net.claustra01.tfcancientcity`.

Keep Ancient City integration narrow. The structure processor changes exactly the 21 block IDs documented in `PROJECT.md`, always uses Blackwood, and runs only for `minecraft:ancient_city/*` templates. Campfires, deepslate and its variants, Sculk, redstone, wool, and soul lighting remain vanilla. Furnace-family NBT is discarded when creating a Firepit; compatible state and other NBT are retained.

Loose Deepslate uses `GroundcoverBlock`, supports counts one through four, drops its represented count, and requires a sturdy support face. Do not add natural generation, Hardened Deepslate, collapse behavior, configuration files, or a dependency on TFC Overdims.

M02 inherits the v0.1.1 Ancient City biome tag and all nine loot data files without a semantic migration. Minecraft 1.21.1 already uses the singular `loot_table` directory, every referenced TFC 4.2.12 item and tag ID remains valid, and generated TFC food receives its normal food component and decay behavior without adding a recipe-only `tfc:copy_food` modifier. Do not add worldgen registries, override Vanilla Ancient City structure data, or alter Sculk and Warden behavior.

M03 overrides only the Vanilla Deepslate and Cobbled Deepslate block loot tables. Connected mining drops Loose Deepslate uniformly from two through four, replacing v0.1.1's one-through-four range, while TFC isolated recovery returns one original block. Add only those two blocks to `tfc:breaks_when_isolated`; do not extend the behavior to ores, Reinforced Deepslate, derived building blocks, or general stone.

M04 registers `tfcancientcity:rock/cobble/deepslate` as a separate TFC-style Cobble block and connects only that block to `tfc:can_landslide`. Keep Vanilla Cobbled Deepslate and all Ancient City Deepslate blocks unchanged. Do not add collapse tags or recipes, Hardened Deepslate, or a custom landslide recipe; TFC's tag-backed landslide fallback supplies the behavior. Preserve the six Deepslate processing recipes, metamorphic rock Knapping tags and GUI material, and the three explicit item size definitions.

Use vanilla and NeoForge registries and data formats before custom mechanisms. Keep common code safe for dedicated servers. Add a GameTest for server-side behavior and an E2E screenshot scenario for client rendering changes. Run these commands from the repository root before handoff:

```text
./gradlew classes
./gradlew build
./gradlew runGameTestServer
```

Treat `PROJECT.md`, `.harness/`, and `.harness-state/` as read-only. Do not edit license files.
