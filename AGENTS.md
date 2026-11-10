# TFC Ancient City development guide

This repository targets Minecraft 1.21.1, Java 21, NeoForge 21.1.264, and TerraFirmaCraft 4.2.12 or newer for Minecraft 1.21.1. The mod ID is `tfcancientcity`, the release version is `1.0.0`, and Java code belongs under `net.claustra01.tfcancientcity`.

Keep Ancient City integration narrow. The structure processor changes exactly the 21 block IDs documented in `PROJECT.md`, always uses Blackwood, and runs only for `minecraft:ancient_city/*` templates. Campfires, deepslate and its variants, Sculk, redstone, wool, and soul lighting remain vanilla. Furnace-family NBT is discarded when creating a Firepit; compatible state and other NBT are retained.

Loose Deepslate uses `GroundcoverBlock`, supports counts one through four, drops its represented count, and requires a sturdy support face. Do not add natural generation, Hardened Deepslate, collapse behavior, configuration files, or a dependency on TFC Overdims.

M02 inherits the v0.1.1 Ancient City biome tag and all nine loot data files without a semantic migration. Minecraft 1.21.1 already uses the singular `loot_table` directory, every referenced TFC 4.2.12 item and tag ID remains valid, and generated TFC food receives its normal food component and decay behavior without adding a recipe-only `tfc:copy_food` modifier. Do not add worldgen registries, override Vanilla Ancient City structure data, or alter Sculk and Warden behavior.

Use vanilla and NeoForge registries and data formats before custom mechanisms. Keep common code safe for dedicated servers. Add a GameTest for server-side behavior and an E2E screenshot scenario for client rendering changes. Run these commands from the repository root before handoff:

```text
./gradlew classes
./gradlew build
./gradlew runGameTestServer
```

Treat `PROJECT.md`, `.harness/`, and `.harness-state/` as read-only. Do not edit license files.
