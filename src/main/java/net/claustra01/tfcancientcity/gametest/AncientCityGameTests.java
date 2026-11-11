package net.claustra01.tfcancientcity.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.claustra01.tfcancientcity.TfcAncientCity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.warden.AngerLevel;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameRules.BooleanValue;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.entity.SculkSensorBlockEntity;
import net.minecraft.world.level.block.entity.SculkShriekerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SculkSensorPhase;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEvent.Context;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.NeoForgeRegistries.Keys;

@GameTestHolder(TfcAncientCity.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AncientCityGameTests {
    private static final Set<ResourceLocation> ANCIENT_CITY_BIOMES = Set.of(
        tfc("burren_plateau"),
        tfc("cenote_highlands"),
        tfc("cenote_plateau"),
        tfc("doline_highlands"),
        tfc("doline_plateau"),
        tfc("extreme_doline_mountains"),
        tfc("extreme_doline_plateau"),
        tfc("glacially_carved_mountains"),
        tfc("glacially_carved_oceanic_mountains"),
        tfc("glaciated_mountains"),
        tfc("glaciated_oceanic_mountains"),
        tfc("highlands"),
        tfc("ice_sheet_mountains"),
        tfc("ice_sheet_mountains_edge"),
        tfc("ice_sheet_oceanic_mountains"),
        tfc("ice_sheet_oceanic_mountains_edge"),
        tfc("mountain_lake"),
        tfc("mountains"),
        tfc("oceanic_mountain_lake"),
        tfc("oceanic_mountains"),
        tfc("old_mountain_lake"),
        tfc("old_mountains"),
        tfc("plateau"),
        tfc("plateau_lake"),
        tfc("plateau_wide"),
        tfc("rocky_plateau"),
        tfc("shilin_highlands"),
        tfc("shilin_plateau"),
        tfc("tower_karst_highlands"),
        tfc("volcanic_mountain_lake"),
        tfc("volcanic_mountains"),
        tfc("volcanic_oceanic_mountain_lake"),
        tfc("volcanic_oceanic_mountains")
    );

    @GameTest(template = "empty")
    public static void vanillaAncientCitySettingsAndThirtyThreeBiomes(GameTestHelper helper) {
        JsonObject biomeJson = readJson("/data/minecraft/tags/worldgen/biome/has_structure/ancient_city.json").getAsJsonObject();
        require(biomeJson.get("replace").getAsBoolean(), "Ancient City biome JSON must replace the Vanilla tag");
        Set<ResourceLocation> jsonBiomes = biomeJson.getAsJsonArray("values")
            .asList()
            .stream()
            .map(value -> ResourceLocation.parse(value.getAsString()))
            .collect(Collectors.toSet());
        require(jsonBiomes.equals(ANCIENT_CITY_BIOMES), "Ancient City biome JSON mismatch: " + jsonBiomes);
        Registry<Biome> biomeRegistry = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        TagKey<Biome> biomeTag = TagKey.create(Registries.BIOME, minecraft("has_structure/ancient_city"));
        Set<ResourceLocation> actualBiomes = biomeRegistry.getTag(biomeTag).orElseThrow()
            .stream()
            .map(holder -> holder.unwrapKey().orElseThrow().location())
            .collect(Collectors.toSet());
        require(actualBiomes.equals(ANCIENT_CITY_BIOMES), "Ancient City biome tag mismatch: " + actualBiomes);

        for (ResourceLocation biome : ANCIENT_CITY_BIOMES) {
            require(biomeRegistry.containsKey(biome), "Missing TFC biome " + biome);
        }

        Registry<Structure> structureRegistry = helper.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE);
        Structure structure = structureRegistry.get(minecraft("ancient_city"));
        require(structure instanceof JigsawStructure, "minecraft:ancient_city is not a jigsaw structure");
        JsonObject structureJson = encode(Structure.DIRECT_CODEC, structure, helper).getAsJsonObject();
        require(structureJson.get("biomes").getAsString().equals("#minecraft:has_structure/ancient_city"), "Ancient City biome source changed");
        require(structureJson.get("size").getAsInt() == 7, "Ancient City jigsaw depth changed");
        require(structureJson.getAsJsonObject("start_height").get("absolute").getAsInt() == -27, "Ancient City height changed");
        require(structureJson.get("max_distance_from_center").getAsInt() == 116, "Ancient City range changed");
        require(structureJson.get("start_pool").getAsString().equals("minecraft:ancient_city/city_center"), "Ancient City start pool changed");
        require(structureJson.get("start_jigsaw_name").getAsString().equals("minecraft:city_anchor"), "Ancient City anchor changed");
        require(structureJson.get("step").getAsString().equals("underground_decoration"), "Ancient City generation step changed");
        require(structureJson.get("terrain_adaptation").getAsString().equals("beard_box"), "Ancient City terrain adaptation changed");
        Registry<StructureSet> setRegistry = helper.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE_SET);
        StructureSet structureSet = setRegistry.get(minecraft("ancient_cities"));
        require(structureSet != null && structureSet.structures().size() == 1, "Ancient City structure set changed");
        require(structureSet.placement() instanceof RandomSpreadStructurePlacement, "Ancient City placement type changed");
        RandomSpreadStructurePlacement placement = (RandomSpreadStructurePlacement)structureSet.placement();
        JsonObject placementJson = encode(StructureSet.DIRECT_CODEC, structureSet, helper).getAsJsonObject().getAsJsonObject("placement");
        require(
            placement.spacing() == 24 && placement.separation() == 8 && placementJson.get("salt").getAsInt() == 20083232,
            "Ancient City spacing, separation, or salt changed"
        );
        Registry<StructureTemplatePool> poolRegistry = helper.getLevel().registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);
        StructureTemplatePool startPool = poolRegistry.get(minecraft("ancient_city/city_center"));
        require(startPool != null && startPool.size() == 3, "Ancient City center pool changed");
        require(startPool.getFallback().unwrapKey().orElseThrow().location().equals(minecraft("empty")), "Ancient City fallback pool changed");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "ancient_city_templates", timeoutTicks = 400)
    public static void representativeTemplateKeepsVanillaLayout(GameTestHelper helper) {
        String[] paths = new String[]{"city_center/city_center_1", "city_center/walls/bottom_1", "structures/camp_2"};
        Set<ResourceLocation> baselineBlocks = new HashSet<>();
        int replacementCount = 0;
        Registry<Block> blocksRegistry = helper.getLevel().registryAccess().registryOrThrow(Registries.BLOCK);

        for (int templateIndex = 0; templateIndex < paths.length; templateIndex++) {
            String path = paths[templateIndex];
            ResourceLocation templateId = minecraft("ancient_city/" + path);
            StructureTemplate template = helper.getLevel().getStructureManager().getOrCreate(templateId);
            CompoundTag vanilla = template.save(new CompoundTag());
            ListTag palette = vanilla.getList("palette", 10);
            ListTag blocks = vanilla.getList("blocks", 10);
            ListTag dimensions = vanilla.getList("size", 3);
            Vec3i size = new Vec3i(dimensions.getInt(0), dimensions.getInt(1), dimensions.getInt(2));
            BlockPos origin = new BlockPos(2 + templateIndex * 50, 2, 2);
            require(template.getBoundingBox(new StructurePlaceSettings(), helper.absolutePos(origin)).getXSpan() == size.getX()
                && template.getBoundingBox(new StructurePlaceSettings(), helper.absolutePos(origin)).getYSpan() == size.getY()
                && template.getBoundingBox(new StructurePlaceSettings(), helper.absolutePos(origin)).getZSpan() == size.getZ(),
                "Placed template dimensions differ from Vanilla: " + path);
            require(
                template.placeInWorld(
                    helper.getLevel(), helper.absolutePos(origin), helper.absolutePos(origin), new StructurePlaceSettings(), RandomSource.create(187L), 2
                ),
                "Vanilla Ancient City template did not place: " + path
            );

            for (int index = 0; index < blocks.size(); index++) {
                CompoundTag blockTag = blocks.getCompound(index);
                BlockState source = NbtUtils.readBlockState(blocksRegistry.asLookup(), palette.getCompound(blockTag.getInt("state")));
                if (!source.is(Blocks.STRUCTURE_VOID)) {
                    ListTag position = blockTag.getList("pos", 3);
                    BlockPos relative = origin.offset(position.getInt(0), position.getInt(1), position.getInt(2));
                    ResourceLocation sourceId = BuiltInRegistries.BLOCK.getKey(source.getBlock());
                    baselineBlocks.add(sourceId);
                    ResourceLocation expected = ExpectedAncientCityReplacements.BLOCKS.getOrDefault(source.getBlock(), sourceId);
                    BlockState placed = helper.getBlockState(relative);
                    ResourceLocation actual = BuiltInRegistries.BLOCK.getKey(placed.getBlock());
                    require(actual.equals(expected), "Unexpected block in " + path + " at " + relative + ": " + actual);
                    if (expected.equals(sourceId)) {
                        require(placed.equals(source), "Vanilla block state changed in " + path + " at " + relative);
                    }
                    if (!expected.equals(sourceId)) {
                        replacementCount++;
                    }
                }
            }
        }

        require(replacementCount > 0, "Vanilla templates did not exercise an Ancient City replacement");
        require(
            baselineBlocks.contains(minecraft("sculk_sensor"))
                && baselineBlocks.contains(minecraft("gray_wool"))
                && baselineBlocks.contains(minecraft("redstone_wire"))
                && baselineBlocks.contains(minecraft("dark_oak_planks")),
            "Vanilla templates did not cover the center circuit, Sculk sensor, wool, and building blocks"
        );
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void sculkAndWardenRemainVanilla(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Difficulty previousDifficulty = level.getDifficulty();
        boolean previousSpawning = level.getGameRules().getBoolean(GameRules.RULE_DO_WARDEN_SPAWNING);
        level.getServer().setDifficulty(Difficulty.NORMAL, true);
        ((BooleanValue)level.getGameRules().getRule(GameRules.RULE_DO_WARDEN_SPAWNING)).set(true, level.getServer());
        ServerPlayer player = new ServerPlayer(
            level.getServer(), level, new GameProfile(UUID.randomUUID(), "ancient_city_test"), ClientInformation.createDefault()
        );
        Runnable cleanup = () -> {
            player.discard();
            level.getServer().setDifficulty(previousDifficulty, true);
            ((BooleanValue)level.getGameRules().getRule(GameRules.RULE_DO_WARDEN_SPAWNING)).set(previousSpawning, level.getServer());
        };
        BlockPos sensorPos = new BlockPos(2, 2, 2);
        helper.setBlock(sensorPos, Blocks.SCULK_SENSOR);
        BlockPos eventPos = helper.absolutePos(sensorPos.offset(1, 0, 0));
        level.gameEvent(GameEvent.STEP, eventPos, Context.of(player));
        helper.runAfterDelay(
            5L,
            () -> {
                try {
                    BlockState activeSensor = helper.getBlockState(sensorPos);
                    SculkSensorBlockEntity sensor = (SculkSensorBlockEntity)helper.getBlockEntity(sensorPos);
                    require(activeSensor.getValue(SculkSensorBlock.PHASE) == SculkSensorPhase.ACTIVE, "Sculk Sensor did not detect the Vanilla step event");
                    require(activeSensor.getValue(SculkSensorBlock.POWER) == 14, "Sculk Sensor redstone strength changed");
                    require(sensor.getLastVibrationFrequency() > 0, "Sculk Sensor did not record the vibration frequency");
                    BlockPos shriekerPos = new BlockPos(18, 2, 18);

                    for (int x = -8; x <= 8; x++) {
                        for (int z = -8; z <= 8; z++) {
                            helper.setBlock(shriekerPos.offset(x, -1, z), Blocks.STONE);
                        }
                    }

                    helper.setBlock(shriekerPos, Blocks.SCULK_SHRIEKER.defaultBlockState().setValue(SculkShriekerBlock.CAN_SUMMON, true));
                    player.setPos(Vec3.atCenterOf(helper.absolutePos(shriekerPos.above())));
                    player.getWardenSpawnTracker().orElseThrow().setWarningLevel(3);
                    SculkShriekerBlockEntity shrieker = (SculkShriekerBlockEntity)helper.getBlockEntity(shriekerPos);
                    shrieker.tryShriek(level, player);
                    require(helper.getBlockState(shriekerPos).getValue(SculkShriekerBlock.SHRIEKING), "Shrieker did not enter its Vanilla shrieking state");
                    require(
                        player.getWardenSpawnTracker().orElseThrow().getWarningLevel() == 4, "Shrieker did not raise the player warning level"
                    );
                    helper.runAfterDelay(
                        110L,
                        () -> {
                            BlockPos absoluteShrieker = helper.absolutePos(shriekerPos);
                            AABB search = new AABB(absoluteShrieker).inflate(30.0);
                            Warden warden = null;

                            try {
                                warden = level.getEntitiesOfClass(Warden.class, search)
                                    .stream()
                                    .findFirst()
                                    .orElseThrow(() -> new GameTestAssertException("A level-four Vanilla Shrieker did not summon a Warden"));
                                require(warden.getType() == EntityType.WARDEN, "Shrieker summoned a non-Vanilla Warden");
                                require(warden.getAttributeValue(Attributes.MAX_HEALTH) == 500.0, "Warden health changed");
                                require(warden.getAttributeValue(Attributes.ATTACK_DAMAGE) == 30.0, "Warden attack damage changed");
                                require(Math.abs(warden.getAttributeValue(Attributes.MOVEMENT_SPEED) - 0.3) < 1.0E-6, "Warden movement speed changed");
                                require(warden.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) == 1.0, "Warden knockback resistance changed");
                                require(warden.getAttributeValue(Attributes.ATTACK_KNOCKBACK) == 1.5, "Warden attack knockback changed");
                                require(warden.getBrain().isActive(Activity.EMERGE) || warden.getPose() == Pose.EMERGING, "Warden did not begin with Vanilla emerge AI");
                                warden.increaseAngerAt(player, 150, true);
                                require(warden.getAngerLevel() == AngerLevel.ANGRY, "Warden did not become angry at its target");
                                require(warden.getEntityAngryAt().filter(target -> target == player).isPresent(), "Warden anger management did not retain its target");
                                warden.setAttackTarget(player);
                                require(
                                    warden.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).filter(target -> target == player).isPresent(),
                                    "Warden did not track an attack target"
                                );
                                helper.succeed();
                            } finally {
                                if (warden != null) {
                                    warden.discard();
                                }

                                cleanup.run();
                            }
                        }
                    );
                } catch (RuntimeException exception) {
                    cleanup.run();
                    throw exception;
                }
            }
        );
    }

    @GameTest(template = "empty")
    public static void registersNoWorldgenOrNaturalLooseDeepslate(GameTestHelper helper) {
        assertNamespaceEmpty(helper, Registries.CONFIGURED_FEATURE);
        assertNamespaceEmpty(helper, Registries.PLACED_FEATURE);
        assertNamespaceEmpty(helper, Registries.BIOME);
        assertNamespaceEmpty(helper, Registries.STRUCTURE);
        assertNamespaceEmpty(helper, Registries.STRUCTURE_SET);
        assertNamespaceEmpty(helper, Registries.TEMPLATE_POOL);
        assertNamespaceEmpty(helper, Registries.PROCESSOR_LIST);
        assertNamespaceEmpty(helper, Keys.BIOME_MODIFIERS);
        Registry<Biome> biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);

        for (Reference<Biome> biome : biomes.holders().toList()) {
            for (HolderSet<PlacedFeature> step : biome.value().getGenerationSettings().features()) {
                for (Holder<PlacedFeature> feature : step) {
                    String definition = encode(ConfiguredFeature.DIRECT_CODEC, feature.value().feature().value(), helper).toString();
                    require(
                        !definition.contains("tfcancientcity:rock/loose/deepslate"),
                        "Loose Deepslate is naturally placed in " + biome.unwrapKey().orElseThrow().location()
                    );
                }
            }
        }

        Set<ResourceLocation> modBlocks = BuiltInRegistries.BLOCK.keySet().stream()
            .filter(id -> id.getNamespace().equals("tfcancientcity"))
            .collect(Collectors.toSet());
        require(modBlocks.equals(Set.of(ResourceLocation.fromNamespaceAndPath("tfcancientcity", "rock/loose/deepslate"))),
            "Unexpected mod blocks: " + modBlocks);
        helper.succeed();
    }

    private static <T> void assertNamespaceEmpty(GameTestHelper helper, ResourceKey<Registry<T>> registryKey) {
        Registry<T> registry = helper.getLevel().registryAccess().registryOrThrow(registryKey);
        Set<ResourceLocation> entries = registry.keySet().stream().filter(id -> id.getNamespace().equals("tfcancientcity")).collect(Collectors.toSet());
        require(entries.isEmpty(), "Unexpected " + registryKey.location() + " entries: " + entries);
    }

    private static <T> JsonElement encode(Codec<T> codec, T value, GameTestHelper helper) {
        return codec.encodeStart(RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess()), value).getOrThrow();
    }

    private static JsonElement readJson(String path) {
        try (InputStream stream = AncientCityGameTests.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new GameTestAssertException("Missing resource " + path);
            }
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new GameTestAssertException("Could not read " + path + ": " + exception.getMessage());
        }
    }

    private static ResourceLocation minecraft(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }

    private static ResourceLocation tfc(String path) {
        return ResourceLocation.fromNamespaceAndPath("tfc", path);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new GameTestAssertException(message);
        }
    }

    private AncientCityGameTests() {
    }
}
