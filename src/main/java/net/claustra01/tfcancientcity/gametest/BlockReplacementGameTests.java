package net.claustra01.tfcancientcity.gametest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import net.claustra01.tfcancientcity.TfcAncientCity;
import net.claustra01.tfcancientcity.world.TfcBlockReplacementProcessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(TfcAncientCity.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BlockReplacementGameTests {
    @GameTest(template = "empty")
    public static void replacesAllTwentyOneBlocks(GameTestHelper helper) {
        Map<Block, ResourceLocation> expected = new LinkedHashMap<>();
        expected.put(Blocks.DARK_OAK_LOG, tfc("wood/log/blackwood"));
        expected.put(Blocks.DARK_OAK_PLANKS, tfc("wood/planks/blackwood"));
        expected.put(Blocks.DARK_OAK_SLAB, tfc("wood/planks/blackwood_slab"));
        expected.put(Blocks.DARK_OAK_STAIRS, tfc("wood/planks/blackwood_stairs"));
        expected.put(Blocks.DARK_OAK_FENCE, tfc("wood/fence/blackwood"));
        expected.put(Blocks.DARK_OAK_FENCE_GATE, tfc("wood/fence_gate/blackwood"));
        expected.put(Blocks.DARK_OAK_DOOR, tfc("wood/door/blackwood"));
        expected.put(Blocks.DARK_OAK_TRAPDOOR, tfc("wood/trapdoor/blackwood"));
        expected.put(Blocks.DARK_OAK_BUTTON, tfc("wood/button/blackwood"));
        expected.put(Blocks.DARK_OAK_PRESSURE_PLATE, tfc("wood/pressure_plate/blackwood"));
        expected.put(Blocks.CHEST, tfc("wood/chest/blackwood"));
        expected.put(Blocks.TRAPPED_CHEST, tfc("wood/trapped_chest/blackwood"));
        expected.put(Blocks.LECTERN, tfc("wood/lectern/blackwood"));
        expected.put(Blocks.CANDLE, tfc("candle"));
        expected.put(Blocks.WHITE_CANDLE, tfc("candle/white"));
        expected.put(Blocks.TORCH, tfc("torch"));
        expected.put(Blocks.WALL_TORCH, tfc("wall_torch"));
        expected.put(Blocks.FURNACE, tfc("firepit"));
        expected.put(Blocks.SMOKER, tfc("firepit"));
        expected.put(Blocks.BLAST_FURNACE, tfc("firepit"));
        expected.put(Blocks.IRON_TRAPDOOR, tfc("metal/trapdoor/wrought_iron"));

        int index = 0;
        for (Map.Entry<Block, ResourceLocation> entry : expected.entrySet()) {
            BlockPos pos = new BlockPos(++index, 2, 2);
            helper.setBlock(pos.below(), Blocks.STONE);
            helper.setBlock(pos.relative(Direction.NORTH), Blocks.STONE);
            helper.setBlock(pos.relative(Direction.SOUTH), Blocks.STONE);
            helper.setBlock(pos, entry.getKey().defaultBlockState());
            if (entry.getKey() == Blocks.DARK_OAK_DOOR) {
                helper.setBlock(pos.above(), Blocks.DARK_OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
            }
        }
        placeCaptured(helper, "ancient_city/all_replacements", new BlockPos(1, 2, 2), new Vec3i(expected.size(), 2, 1));
        index = 0;
        for (Map.Entry<Block, ResourceLocation> entry : expected.entrySet()) {
            ResourceLocation actual = BuiltInRegistries.BLOCK.getKey(helper.getBlockState(new BlockPos(++index, 2, 2)).getBlock());
            require(actual.equals(entry.getValue()), "Expected " + entry.getValue() + " for " + entry.getKey() + ", got " + actual);
        }
        require(expected.size() == 21, "The replacement fixture must contain exactly 21 entries");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void preservesUnlistedAncientCityBlocks(GameTestHelper helper) {
        Block[] protectedBlocks = {
            Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE,
            Blocks.DEEPSLATE, Blocks.COBBLED_DEEPSLATE, Blocks.COBBLED_DEEPSLATE_STAIRS,
            Blocks.COBBLED_DEEPSLATE_SLAB, Blocks.COBBLED_DEEPSLATE_WALL, Blocks.POLISHED_DEEPSLATE,
            Blocks.POLISHED_DEEPSLATE_STAIRS, Blocks.POLISHED_DEEPSLATE_SLAB, Blocks.POLISHED_DEEPSLATE_WALL,
            Blocks.DEEPSLATE_BRICKS, Blocks.DEEPSLATE_BRICK_STAIRS, Blocks.DEEPSLATE_BRICK_SLAB,
            Blocks.DEEPSLATE_BRICK_WALL, Blocks.CRACKED_DEEPSLATE_BRICKS, Blocks.DEEPSLATE_TILES,
            Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB, Blocks.DEEPSLATE_TILE_WALL,
            Blocks.CRACKED_DEEPSLATE_TILES, Blocks.CHISELED_DEEPSLATE, Blocks.REINFORCED_DEEPSLATE,
            Blocks.SCULK, Blocks.SCULK_CATALYST, Blocks.SCULK_SENSOR, Blocks.SCULK_SHRIEKER, Blocks.SCULK_VEIN,
            Blocks.REDSTONE_WIRE, Blocks.REDSTONE_LAMP, Blocks.REPEATER, Blocks.COMPARATOR,
            Blocks.WHITE_WOOL, Blocks.BLUE_WOOL, Blocks.SOUL_TORCH, Blocks.SOUL_WALL_TORCH, Blocks.SOUL_LANTERN
        };
        int index = 0;
        for (Block block : protectedBlocks) {
            BlockPos pos = new BlockPos(++index, 2, 2);
            helper.setBlock(pos.below(), Blocks.STONE);
            helper.setBlock(pos.relative(Direction.NORTH), Blocks.STONE);
            helper.setBlock(pos.relative(Direction.SOUTH), Blocks.STONE);
            BlockState state = block.defaultBlockState();
            if (block == Blocks.SCULK_VEIN) {
                state = state.setValue(BlockStateProperties.NORTH, true);
            }
            helper.setBlock(pos, state);
        }
        placeCaptured(helper, "ancient_city/protected_blocks", new BlockPos(1, 2, 2), new Vec3i(protectedBlocks.length, 1, 1));
        index = 0;
        for (Block block : protectedBlocks) {
            BlockPos pos = new BlockPos(++index, 2, 2);
            require(helper.getBlockState(pos).is(block), "Unexpected replacement of " + BuiltInRegistries.BLOCK.getKey(block));
            if (block == Blocks.CAMPFIRE || block == Blocks.SOUL_CAMPFIRE) {
                require(helper.getBlockState(pos).getValue(CampfireBlock.LIT), "campfire lost its lit state");
                require(helper.getBlockEntity(pos).getType() == BlockEntityType.CAMPFIRE, "campfire lost its block entity");
            } else if (block == Blocks.SCULK_SENSOR) {
                require(helper.getBlockEntity(pos).getType() == BlockEntityType.SCULK_SENSOR, "sensor lost its block entity");
            } else if (block == Blocks.SCULK_SHRIEKER) {
                require(helper.getBlockEntity(pos).getType() == BlockEntityType.SCULK_SHRIEKER, "shrieker lost its block entity");
            } else if (block == Blocks.SCULK_CATALYST) {
                require(helper.getBlockEntity(pos).getType() == BlockEntityType.SCULK_CATALYST, "catalyst lost its block entity");
            } else if (block == Blocks.COMPARATOR) {
                require(helper.getBlockEntity(pos).getType() == BlockEntityType.COMPARATOR, "comparator lost its block entity");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void preservesPropertiesAndCompatibleNbt(GameTestHelper helper) {
        BlockState stairs = Blocks.DARK_OAK_STAIRS.defaultBlockState()
            .setValue(StairBlock.FACING, Direction.WEST)
            .setValue(StairBlock.HALF, net.minecraft.world.level.block.state.properties.Half.TOP);
        BlockState slab = Blocks.DARK_OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
        BlockState door = Blocks.DARK_OAK_DOOR.defaultBlockState()
            .setValue(DoorBlock.FACING, Direction.SOUTH)
            .setValue(DoorBlock.OPEN, true);
        BlockState[] states = new BlockState[18];
        states[0] = stairs;
        states[1] = slab;
        states[2] = door;
        states[3] = Blocks.CHEST.defaultBlockState();
        states[4] = Blocks.CANDLE.defaultBlockState().setValue(BlockStateProperties.CANDLES, 3).setValue(BlockStateProperties.LIT, true);
        states[5] = Blocks.WHITE_CANDLE.defaultBlockState().setValue(BlockStateProperties.CANDLES, 3).setValue(BlockStateProperties.LIT, true);
        int index = 6;
        for (Block furnaceBlock : List.of(Blocks.FURNACE, Blocks.SMOKER, Blocks.BLAST_FURNACE)) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                states[index++] = furnaceBlock.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, direction);
            }
        }
        for (int i = 0; i < states.length; i++) {
            BlockPos pos = new BlockPos(i + 1, 2, 2);
            helper.setBlock(pos.below(), Blocks.STONE);
            helper.setBlock(pos, states[i]);
            require(helper.getBlockState(pos).is(states[i].getBlock()), "source block could not be prepared at " + i);
            if (i == 2) {
                helper.setBlock(pos.above(), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
            }
        }
        var chestEntity = (RandomizableContainerBlockEntity)helper.getBlockEntity(new BlockPos(4, 2, 2));
        ResourceKey<LootTable> lootTable = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.withDefaultNamespace("chests/ancient_city"));
        chestEntity.setLootTable(lootTable);
        chestEntity.setLootTableSeed(42L);
        require(chestEntity.saveWithFullMetadata(helper.getLevel().registryAccess()).getLong("LootTableSeed") == 42L,
            "source chest did not serialize its loot seed");
        for (int i = 6; i < states.length; i++) {
            CompoundTag furnaceNbt = new CompoundTag();
            furnaceNbt.putInt("BurnTime", 100);
            helper.getBlockEntity(new BlockPos(i + 1, 2, 2)).loadWithComponents(furnaceNbt, helper.getLevel().registryAccess());
        }
        StructureTemplate stateTemplate = capture(helper, "ancient_city/states_and_nbt", new BlockPos(1, 2, 2), new Vec3i(states.length, 2, 1));
        require(stateTemplate.filterBlocks(helper.absolutePos(new BlockPos(1, 2, 2)), new StructurePlaceSettings(), Blocks.CHEST)
            .getFirst().nbt().getLong("LootTableSeed") == 42L, "template did not capture the chest loot seed");
        placeCaptured(helper, "ancient_city/states_and_nbt", new BlockPos(1, 2, 2), new Vec3i(states.length, 2, 1));
        require(helper.getBlockState(new BlockPos(1, 2, 2)).getValue(StairBlock.FACING) == Direction.WEST, "stair facing was lost");
        require(helper.getBlockState(new BlockPos(2, 2, 2)).getValue(SlabBlock.TYPE) == SlabType.TOP, "slab type was lost");
        BlockState replacedDoor = helper.getBlockState(new BlockPos(3, 2, 2));
        require(replacedDoor.getValue(DoorBlock.FACING) == Direction.SOUTH && replacedDoor.getValue(DoorBlock.OPEN), "door state was lost");
        var placedChest = (RandomizableContainerBlockEntity)helper.getBlockEntity(new BlockPos(4, 2, 2));
        require(lootTable.equals(placedChest.getLootTable()), "chest loot table was lost");
        // StructureTemplate replaces container loot seeds with a placement seed before loading the block entity.
        require(placedChest.getLootTableSeed() != 0L, "chest loot seed was lost");
        for (int i = 4; i <= 5; i++) {
            require(helper.getBlockState(new BlockPos(i + 1, 2, 2)).getValue(BlockStateProperties.CANDLES) == 3,
                "candle count was lost");
            require(helper.getBlockState(new BlockPos(i + 1, 2, 2)).getValue(BlockStateProperties.LIT), "candle light was lost");
        }
        for (int i = 6; i < states.length; i++) {
            BlockPos pos = new BlockPos(i + 1, 2, 2);
            require(helper.getBlockState(pos).getValue(BlockStateProperties.HORIZONTAL_AXIS)
                == states[i].getValue(BlockStateProperties.HORIZONTAL_FACING).getAxis(), "firepit axis was lost");
            require(!helper.getBlockEntity(pos).saveWithFullMetadata(helper.getLevel().registryAccess()).contains("BurnTime"),
                "furnace NBT remained in the firepit");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void limitsScopeAndWarnsOnceForMissingTargets(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 2);
        helper.setBlock(pos.below(), Blocks.STONE);
        helper.setBlock(pos, Blocks.DARK_OAK_PLANKS);
        StructureTemplate ancientTemplate = capture(helper, "ancient_city/scope", pos, new Vec3i(1, 1, 1));
        helper.setBlock(pos, Blocks.AIR);
        StructurePlaceSettings reusedSettings = new StructurePlaceSettings();
        ancientTemplate.placeInWorld(helper.getLevel(), helper.absolutePos(pos), helper.absolutePos(pos), reusedSettings, RandomSource.create(1L), 2);
        ancientTemplate.placeInWorld(helper.getLevel(), helper.absolutePos(pos), helper.absolutePos(pos), reusedSettings, RandomSource.create(2L), 2);
        require(helper.getBlockState(pos).is(BuiltInRegistries.BLOCK.get(tfc("wood/planks/blackwood"))), "Ancient City placement was not replaced");
        long processorCount = reusedSettings.getProcessors().stream().filter(processor -> processor == TfcBlockReplacementProcessor.INSTANCE).count();
        require(processorCount == 1L, "the Ancient City processor was added more than once");

        helper.setBlock(pos, Blocks.DARK_OAK_PLANKS);
        StructureTemplate otherTemplate = capture(helper, "village/scope", pos, new Vec3i(1, 1, 1));
        helper.setBlock(pos, Blocks.AIR);
        StructurePlaceSettings otherSettings = new StructurePlaceSettings();
        otherTemplate.placeInWorld(helper.getLevel(), helper.absolutePos(pos), helper.absolutePos(pos), otherSettings, RandomSource.create(3L), 2);
        require(helper.getBlockState(pos).is(Blocks.DARK_OAK_PLANKS), "non-Ancient City template was replaced");
        require(otherSettings.getProcessors().isEmpty(), "a non-Ancient City template received the replacement processor");

        AtomicInteger warnings = new AtomicInteger();
        TfcBlockReplacementProcessor missingProcessor = TfcBlockReplacementProcessor.createForTesting(id -> Optional.empty(), id -> warnings.incrementAndGet());
        List<StructureTemplate.StructureBlockInfo> source = List.of(
            new StructureTemplate.StructureBlockInfo(BlockPos.ZERO, Blocks.FURNACE.defaultBlockState(), null),
            new StructureTemplate.StructureBlockInfo(new BlockPos(1, 0, 0), Blocks.FURNACE.defaultBlockState(), null)
        );
        List<StructureTemplate.StructureBlockInfo> result = missingProcessor.finalizeProcessing(
            helper.getLevel(), BlockPos.ZERO, BlockPos.ZERO, source, source, new StructurePlaceSettings()
        );
        require(result.get(0).state().is(Blocks.FURNACE) && result.get(1).state().is(Blocks.FURNACE), "missing targets did not retain source blocks");
        require(warnings.get() == 1, "a missing target warning was emitted " + warnings.get() + " times");
        helper.succeed();
    }

    private static StructureTemplate capture(GameTestHelper helper, String path, BlockPos pos, Vec3i size) {
        StructureTemplate template = helper.getLevel().getStructureManager().getOrCreate(ResourceLocation.withDefaultNamespace(path));
        template.fillFromWorld(helper.getLevel(), helper.absolutePos(pos), size, false, Blocks.AIR);
        return template;
    }

    private static void placeCaptured(GameTestHelper helper, String path, BlockPos pos, Vec3i size) {
        StructureTemplate template = capture(helper, path, pos, size);
        for (int x = 0; x < size.getX(); x++) {
            helper.setBlock(pos.offset(x, 0, 0), Blocks.AIR);
            if (size.getY() > 1) {
                helper.setBlock(pos.offset(x, 1, 0), Blocks.AIR);
            }
        }
        require(template.placeInWorld(helper.getLevel(), helper.absolutePos(pos), helper.absolutePos(pos),
            new StructurePlaceSettings(), RandomSource.create(1L), 2), "template placement failed");
    }

    private static ResourceLocation tfc(String path) {
        return ResourceLocation.fromNamespaceAndPath("tfc", path);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new GameTestAssertException(message);
        }
    }

    private BlockReplacementGameTests() {
    }
}
