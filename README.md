# TFC Ancient City

TFC Ancient City integrates Minecraft's Ancient City structures with TerraFirmaCraft while preserving the Deep Dark, Sculk, Warden, deepslate construction, redstone, and campfire behavior.

Version 1.0.0 targets Minecraft 1.21.1 with Java 21 and NeoForge 21.1.264. TerraFirmaCraft 4.2.12 or a compatible newer 1.21.1 release is required; TFC Overdims is not required.

M01 provides:

- a structure processor that replaces exactly 21 Ancient City blocks with fixed TFC Blackwood, candle, torch, Firepit, and wrought-iron equivalents;
- scope protection so other structures and unlisted Ancient City blocks remain unchanged;
- `tfcancientcity:rock/loose/deepslate` as a placeable, stackable Groundcover block and BlockItem with counts one through four;
- `tfcancientcity:brick/deepslate` as the existing standalone material item.

Build and verify from the project root:

```text
./gradlew classes
./gradlew build
./gradlew runGameTestServer
```
