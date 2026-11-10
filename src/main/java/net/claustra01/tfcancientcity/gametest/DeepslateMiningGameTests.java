package net.claustra01.tfcancientcity.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.claustra01.tfcancientcity.TfcAncientCity;
import net.claustra01.tfcancientcity.registry.ModItems;
import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.util.loot.TFCLoot;
import net.dries007.tfc.util.tracker.WorldTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforgespi.locating.IModFile;

@GameTestHolder(TfcAncientCity.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DeepslateMiningGameTests {
    private static final List<Block> TARGET_BLOCKS = List.of(Blocks.DEEPSLATE, Blocks.COBBLED_DEEPSLATE);

    @GameTest(template = "empty")
    public static void connectedDeepslateDropsTwoToFourLooseRocks(GameTestHelper helper) {
        for (Block block : TARGET_BLOCKS) {
            Set<Integer> observedCounts = new java.util.HashSet<>();
            for (long seed = 1; seed <= 32; seed++) {
                List<ItemStack> drops = drops(helper, block, false, seed);
                require(drops.size() == 1 && drops.getFirst().is(ModItems.LOOSE_DEEPSLATE.get()),
                    block + " did not generate only Loose Deepslate for seed " + seed);
                int count = drops.getFirst().getCount();
                require(count >= 2 && count <= 4, block + " generated " + count + " loose rocks for seed " + seed);
                observedCounts.add(count);
            }
            require(observedCounts.equals(Set.of(2, 3, 4)), block + " fixed seeds did not exercise exactly counts 2, 3, and 4");
        }

        FakePlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setGameMode(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.NETHERITE_PICKAXE));
        for (int index = 0; index < TARGET_BLOCKS.size(); index++) {
            Block block = TARGET_BLOCKS.get(index);
            BlockPos targetPos = helper.absolutePos(new BlockPos(3 + index * 8, 2, 3));
            helper.getLevel().setBlockAndUpdate(targetPos.east(), block.defaultBlockState());
            for (int attempt = 0; attempt < 8; attempt++) {
                helper.getLevel().setBlockAndUpdate(targetPos, block.defaultBlockState());
                require(player.gameMode.destroyBlock(targetPos), "survival player could not mine " + block);
                List<ItemEntity> entities = itemEntities(helper.getLevel(), targetPos);
                int count = entities.stream().mapToInt(entity -> entity.getItem().getCount()).sum();
                require(!entities.isEmpty() && entities.stream().allMatch(entity -> entity.getItem().is(ModItems.LOOSE_DEEPSLATE.get())),
                    "survival mining " + block + " produced an item other than Loose Deepslate");
                require(count >= 2 && count <= 4, "survival mining " + block + " produced " + count + " loose rocks");
                entities.forEach(ItemEntity::discard);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void isolatedDeepslateReturnsOriginalBlocks(GameTestHelper helper) {
        JsonObject tag = modJson("data", "tfc", "tags", "block", "breaks_when_isolated.json");
        require(!tag.get("replace").getAsBoolean(), "breaks_when_isolated must preserve TFC's existing values");
        Set<String> tagValues = tag.getAsJsonArray("values").asList().stream().map(value -> value.getAsString()).collect(Collectors.toSet());
        require(tagValues.equals(Set.of("minecraft:deepslate", "minecraft:cobbled_deepslate")),
            "the mod's breaks_when_isolated additions were not exactly the two target blocks");

        for (Block block : TARGET_BLOCKS) {
            require(block.defaultBlockState().is(TFCTags.Blocks.BREAKS_WHEN_ISOLATED), block + " is absent from tfc:breaks_when_isolated");
            for (long seed = 1; seed <= 8; seed++) {
                List<ItemStack> drops = drops(helper, block, true, seed);
                require(drops.size() == 1 && drops.getFirst().is(block.asItem()) && drops.getFirst().getCount() == 1,
                    "isolated loot did not return one original " + block);
            }
        }

        ServerLevel level = helper.getLevel();
        WorldTracker tracker = WorldTracker.get(level);
        for (int index = 0; index < TARGET_BLOCKS.size(); index++) {
            BlockPos pos = helper.absolutePos(new BlockPos(3 + index * 8, 3, 12));
            clearNeighbors(level, pos);
            level.setBlockAndUpdate(pos, TARGET_BLOCKS.get(index).defaultBlockState());
            level.setBlockAndUpdate(pos.east(), TARGET_BLOCKS.get(index).defaultBlockState());
            tracker.addIsolatedPos(pos);
        }
        tracker.tick();
        for (int index = 0; index < TARGET_BLOCKS.size(); index++) {
            BlockPos pos = helper.absolutePos(new BlockPos(3 + index * 8, 3, 12));
            require(level.getBlockState(pos).is(TARGET_BLOCKS.get(index)), "WorldTracker removed connected " + TARGET_BLOCKS.get(index));
            level.removeBlock(pos.east(), false);
            tracker.addIsolatedPos(pos);
        }
        tracker.tick();
        for (int index = 0; index < TARGET_BLOCKS.size(); index++) {
            Block block = TARGET_BLOCKS.get(index);
            BlockPos pos = helper.absolutePos(new BlockPos(3 + index * 8, 3, 12));
            require(level.isEmptyBlock(pos), "WorldTracker did not remove isolated " + block);
            List<ItemEntity> entities = itemEntities(level, pos);
            require(entities.size() == 1 && entities.getFirst().getItem().is(block.asItem()) && entities.getFirst().getItem().getCount() == 1,
                "WorldTracker did not recover one original " + block);
            entities.forEach(ItemEntity::discard);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void derivedDeepslateAndGeneralStoneRemainUnchanged(GameTestHelper helper) {
        Set<String> overrides = modBlockLootOverrides();
        require(overrides.equals(Set.of("deepslate.json", "cobbled_deepslate.json")), "unexpected block loot overrides: " + overrides);

        Map<Block, Item> expectedDrops = new LinkedHashMap<>();
        expectedDrops.put(Blocks.DEEPSLATE_DIAMOND_ORE, Items.DIAMOND);
        expectedDrops.put(Blocks.REINFORCED_DEEPSLATE, Items.AIR);
        expectedDrops.put(Blocks.COBBLED_DEEPSLATE_STAIRS, Blocks.COBBLED_DEEPSLATE_STAIRS.asItem());
        expectedDrops.put(Blocks.DEEPSLATE_BRICKS, Blocks.DEEPSLATE_BRICKS.asItem());
        expectedDrops.put(Blocks.DEEPSLATE_TILES, Blocks.DEEPSLATE_TILES.asItem());
        expectedDrops.put(Blocks.STONE, Blocks.COBBLESTONE.asItem());

        FakePlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setGameMode(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.NETHERITE_PICKAXE));
        int index = 0;
        for (Map.Entry<Block, Item> entry : expectedDrops.entrySet()) {
            Block block = entry.getKey();
            Item expected = entry.getValue();
            require(!block.defaultBlockState().is(TFCTags.Blocks.BREAKS_WHEN_ISOLATED), block + " unexpectedly breaks when isolated");
            assertExpectedDrops(drops(helper, block, false, 17L), expected, "loot generation for " + block);

            BlockPos pos = helper.absolutePos(new BlockPos(3 + index * 5, 3, 22));
            helper.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
            require(player.gameMode.destroyBlock(pos), "survival player could not mine protected block " + block);
            List<ItemEntity> entities = itemEntities(helper.getLevel(), pos);
            assertExpectedDrops(entities.stream().map(ItemEntity::getItem).toList(), expected, "survival mining for " + block);
            entities.forEach(ItemEntity::discard);
            index++;
        }
        helper.succeed();
    }

    private static List<ItemStack> drops(GameTestHelper helper, Block block, boolean isolated, long seed) {
        LootParams.Builder params = new LootParams.Builder(helper.getLevel())
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 2, 1))))
            .withParameter(LootContextParams.BLOCK_STATE, block.defaultBlockState())
            .withParameter(LootContextParams.TOOL, new ItemStack(Items.NETHERITE_PICKAXE));
        if (isolated) {
            params.withParameter(TFCLoot.ISOLATED, true);
        }
        LootTable table = helper.getLevel().getServer().reloadableRegistries().getLootTable(block.getLootTable());
        return table.getRandomItems(params.create(LootContextParamSets.BLOCK), seed);
    }

    private static void assertExpectedDrops(List<ItemStack> drops, Item expected, String action) {
        if (expected == Items.AIR) {
            require(drops.isEmpty(), action + " should not drop an item");
        } else {
            require(drops.size() == 1 && drops.getFirst().is(expected) && drops.getFirst().getCount() == 1,
                action + " did not return exactly one " + expected);
        }
    }

    private static Set<String> modBlockLootOverrides() {
        Path directory = modFile().findResource("data", "minecraft", "loot_table", "blocks");
        try (var files = Files.walk(directory)) {
            return files.filter(Files::isRegularFile)
                .map(directory::relativize)
                .map(Path::toString)
                .map(path -> path.replace('\\', '/'))
                .collect(Collectors.toSet());
        } catch (IOException exception) {
            throw new GameTestAssertException("could not inspect the mod's block loot tables: " + exception.getMessage());
        }
    }

    private static JsonObject modJson(String... path) {
        try (Reader reader = Files.newBufferedReader(modFile().findResource(path))) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException exception) {
            throw new GameTestAssertException("could not read mod JSON: " + exception.getMessage());
        }
    }

    private static IModFile modFile() {
        return ModList.get().getModFileById(TfcAncientCity.MOD_ID).getFile();
    }

    private static List<ItemEntity> itemEntities(ServerLevel level, BlockPos pos) {
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1.0));
    }

    private static void clearNeighbors(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            level.removeBlock(pos.relative(direction), false);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new GameTestAssertException(message);
        }
    }

    private DeepslateMiningGameTests() {
    }
}
