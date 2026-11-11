package net.claustra01.tfcancientcity.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.claustra01.tfcancientcity.TfcAncientCity;
import net.claustra01.tfcancientcity.loot.TfcLootReplacementModifier;
import net.dries007.tfc.common.component.food.FoodCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootParams.Builder;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(TfcAncientCity.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AncientCityLootGameTests {
    private static final ResourceKey<LootTable> ANCIENT_CITY = lootTable("minecraft", "chests/ancient_city");
    private static final ResourceKey<LootTable> ICE_BOX = lootTable("minecraft", "chests/ancient_city_ice_box");
    private static final List<String> GEMS = List.of(
        "amethyst", "diamond", "emerald", "lapis_lazuli", "opal", "pyrite", "ruby", "sapphire", "topaz"
    );
    private static final List<String> COAL_LIKE = List.of(
        "minecraft:charcoal", "tfc:ore/bituminous_coal", "tfc:ore/lignite", "tfc:ore/graphite"
    );
    /** Built-in Minecraft packs; the GameTest server also enables the experimental feature packs. */
    private static final Set<String> VANILLA_PACKS = Set.of("vanilla", "bundle", "trade_rebalance");
    private static final Set<String> ENCHANTED_SOURCES = Set.of(
        "minecraft:diamond_hoe", "minecraft:diamond_leggings", "minecraft:iron_leggings"
    );

    @GameTest(template = "empty")
    public static void vanillaLootUnchangedAndModifiersResolve(GameTestHelper helper) {
        ResourceManager resources = helper.getLevel().getServer().getResourceManager();
        for (ResourceKey<LootTable> key : List.of(ANCIENT_CITY, ICE_BOX)) {
            ResourceLocation path = key.location().withPath(name -> "loot_table/" + name + ".json");
            List<Resource> stack = resources.getResourceStack(path);
            require(
                stack.stream().anyMatch(resource -> resource.sourcePackId().equals("vanilla"))
                    && stack.stream().allMatch(resource -> VANILLA_PACKS.contains(resource.sourcePackId())),
                "Ancient City loot table JSON must stay Vanilla: " + key.location() + " from " + stack.stream().map(Resource::sourcePackId).toList()
            );
            require(helper.getLevel().getServer().reloadableRegistries().getLootTable(key) != LootTable.EMPTY, "Loot table failed to load: " + key.location());
        }

        JsonObject global = readResource(resources, ResourceLocation.fromNamespaceAndPath("neoforge", "loot_modifiers/global_loot_modifiers.json"));
        Set<String> entries = new HashSet<>();
        global.getAsJsonArray("entries").forEach(entry -> entries.add(entry.getAsString()));
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess());
        for (String table : List.of("ancient_city", "ancient_city_ice_box")) {
            require(entries.contains("tfcancientcity:chests/" + table), "Global loot modifier list misses " + table);
            JsonObject modifier = readResource(resources, ResourceLocation.fromNamespaceAndPath(TfcAncientCity.MOD_ID, "loot_modifiers/chests/" + table + ".json"));
            require(modifier.get("type").getAsString().equals("tfcancientcity:tfc_loot_replacement"), "Unexpected modifier type for " + table);
            require(modifier.get("profile").getAsString().equals("lategame"), "Ancient City must use the lategame profile: " + table);
            require(
                TfcLootReplacementModifier.CODEC.codec().parse(ops, modifier).isSuccess(),
                "Loot modifier does not decode: " + table
            );
        }

        for (String id : allTargets()) {
            require(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(id)), "Unknown replacement item " + id);
        }
        helper.succeed();
    }

    /** Compares the Vanilla roll ({@code getRandomItemsRaw} skips GLMs) with the GLM result of the same seed. */
    @SuppressWarnings("deprecation")
    @GameTest(template = "empty", timeoutTicks = 600)
    public static void fixedSeedsReplaceOnlyRolledStacks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        LootParams params = new Builder(level)
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)))
            .create(LootContextParamSets.CHEST);
        Set<String> expected = new HashSet<>();
        Set<String> seen = new HashSet<>();
        int unfinished = 0;
        int finished = 0;
        int enchantedFinished = 0;
        boolean validFood = false;

        for (ResourceKey<LootTable> key : List.of(ANCIENT_CITY, ICE_BOX)) {
            LootTable table = level.getServer().reloadableRegistries().getLootTable(key);
            for (long seed = 1L; seed <= 4096L; seed++) {
                List<ItemStack> raw = new ArrayList<>();
                table.getRandomItemsRaw(
                    new LootContext.Builder(params).withOptionalRandomSeed(seed).create(Optional.empty()),
                    LootTable.createStackSplitter(level, raw::add)
                );
                List<ItemStack> modified = table.getRandomItems(params, seed);
                int index = 0;

                for (ItemStack source : raw) {
                    String sourceId = id(source);
                    Set<String> allowed = allowedOutputs(sourceId);
                    ItemStack first = modified.get(index);
                    if (allowed.isEmpty()) {
                        require(ItemStack.matches(source, first), "Unmapped Vanilla reward changed: " + sourceId + " -> " + id(first));
                        index++;
                        continue;
                    }

                    String outputId = id(first);
                    expected.addAll(allowed);
                    seen.add(outputId);
                    require(allowed.contains(outputId), sourceId + " must not become " + outputId);
                    if (outputId.matches("tfc:metal/(.*_head|.*_blade|unfinished_.*)/.*")) {
                        unfinished++;
                        require(first.getTagEnchantments().isEmpty() && !first.has(DataComponents.STORED_ENCHANTMENTS), "Unfinished parts must not be enchanted: " + outputId);
                    } else if (outputId.startsWith("tfc:metal/") && !outputId.contains("horse_armor")) {
                        finished++;
                        if (!first.getTagEnchantments().isEmpty()) {
                            require(ENCHANTED_SOURCES.contains(sourceId) && !source.getTagEnchantments().isEmpty(), "Unexpected enchantment on " + outputId);
                            enchantedFinished++;
                        }
                    }

                    require(!first.has(DataComponents.DAMAGE) || first.getDamageValue() == 0, "Replacement copied source damage: " + outputId);
                    if (FoodCapability.has(first)) {
                        validFood |= !FoodCapability.isRotten(first);
                    }

                    int total = 0;
                    while (total < source.getCount()) {
                        require(modified.get(index).is(first.getItem()), "Split stacks keep one replacement item: " + sourceId);
                        total += modified.get(index++).getCount();
                    }

                    require(total == source.getCount(), "Replacement keeps the rolled count: " + sourceId);
                }

                require(index == modified.size(), "Replacement adds no stacks: " + key.location());
            }
        }

        require(seen.containsAll(expected), "Fixed seeds must exercise every replacement: missing " + difference(expected, seen));
        int equipment = unfinished + finished;
        require(
            equipment > 0 && unfinished * 100 / equipment >= 58 && unfinished * 100 / equipment <= 75,
            "Unfinished to finished equipment ratio differs from 2/3: " + unfinished + "/" + equipment
        );
        require(enchantedFinished > 0, "Finished replacements of enchanted rewards were never re-enchanted");
        require(validFood, "TFC food replacements did not receive fresh food data");

        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, Blocks.CHEST);
        ChestBlockEntity chest = (ChestBlockEntity)helper.getBlockEntity(pos);
        for (ResourceKey<LootTable> key : List.of(ANCIENT_CITY, ICE_BOX)) {
            for (long seed : new long[] {17L, 42L, 256L, 8191L}) {
                chest.clearContent();
                chest.setLootTable(key, seed);
                chest.unpackLootTable(null);
                require(!chest.isEmpty(), "Opening generated chest fills inventory: " + key.location());
                for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                    String id = id(chest.getItem(slot));
                    require(allowedOutputs(id).isEmpty(), "Chest inventory contains a replaceable Vanilla reward: " + id);
                }
            }
        }

        helper.succeed();
    }

    private static Set<String> allowedOutputs(String source) {
        Set<String> values = new HashSet<>();
        switch (source) {
            case "minecraft:amethyst_shard" -> GEMS.forEach(gem -> values.add("tfc:gem/" + gem));
            case "minecraft:coal" -> values.addAll(COAL_LIKE);
            case "minecraft:diamond_hoe" -> List.of("hoe_head", "scythe_blade", "hoe", "scythe").forEach(shape -> values.add(steel(shape)));
            case "minecraft:diamond_leggings", "minecraft:iron_leggings" -> List.of("unfinished_greaves", "greaves").forEach(shape -> values.add(steel(shape)));
            case "minecraft:diamond_horse_armor" -> values.add(steel("horse_armor"));
            case "minecraft:baked_potato" -> values.add("tfc:food/baked_potato");
            default -> {
            }
        }
        return values;
    }

    private static Set<String> allTargets() {
        Set<String> targets = new HashSet<>();
        for (String source : List.of(
            "minecraft:amethyst_shard", "minecraft:coal", "minecraft:diamond_hoe", "minecraft:diamond_leggings",
            "minecraft:diamond_horse_armor", "minecraft:baked_potato"
        )) {
            targets.addAll(allowedOutputs(source));
        }
        return targets;
    }

    private static String steel(String shape) {
        return "tfc:metal/" + shape + "/steel";
    }

    private static Set<String> difference(Set<String> expected, Set<String> seen) {
        Set<String> missing = new HashSet<>(expected);
        missing.removeAll(seen);
        return missing;
    }

    private static String id(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private static JsonObject readResource(ResourceManager resources, ResourceLocation path) {
        try (Reader reader = resources.getResourceOrThrow(path).openAsReader()) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException exception) {
            throw new GameTestAssertException("Could not read " + path + ": " + exception.getMessage());
        }
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void replacedChestsKeepLootAcrossNbtRoundTrip(GameTestHelper helper) {
        BlockPos chestPos = new BlockPos(2, 2, 2);
        BlockPos trappedPos = new BlockPos(5, 2, 2);
        helper.setBlock(chestPos, Blocks.CHEST);
        helper.setBlock(trappedPos, Blocks.TRAPPED_CHEST);
        setLootTable(helper, chestPos, 103L);
        setLootTable(helper, trappedPos, 211L);
        StructureTemplate template = helper.getLevel().getStructureManager().getOrCreate(minecraft("ancient_city/loot_round_trip"));
        template.fillFromWorld(helper.getLevel(), helper.absolutePos(chestPos), new BlockPos(4, 1, 1), false, Blocks.AIR);
        helper.setBlock(chestPos, Blocks.AIR);
        helper.setBlock(trappedPos, Blocks.AIR);
        require(
            template.placeInWorld(
                helper.getLevel(), helper.absolutePos(chestPos), helper.absolutePos(chestPos), new StructurePlaceSettings(), RandomSource.create(29L), 2
            ),
            "Loot chest template did not place"
        );
        requireBlock(helper, chestPos, tfc("wood/chest/blackwood"));
        requireBlock(helper, trappedPos, tfc("wood/trapped_chest/blackwood"));

        for (BlockPos pos : List.of(chestPos, trappedPos)) {
            RandomizableContainerBlockEntity placed = (RandomizableContainerBlockEntity)helper.getBlockEntity(pos);
            require(ANCIENT_CITY.equals(placed.getLootTable()), "Replacement lost its loot table at " + pos);
            require(placed.getLootTableSeed() != 0L, "Replacement lost its loot seed at " + pos);
            CompoundTag saved = placed.saveWithFullMetadata(helper.getLevel().registryAccess());
            BlockState state = helper.getBlockState(pos);
            helper.setBlock(pos, Blocks.AIR);
            helper.setBlock(pos, state);
            RandomizableContainerBlockEntity reloaded = (RandomizableContainerBlockEntity)helper.getBlockEntity(pos);
            reloaded.loadWithComponents(saved, helper.getLevel().registryAccess());
            require(ANCIENT_CITY.equals(reloaded.getLootTable()), "NBT round-trip lost its loot table at " + pos);
            require(reloaded.getLootTableSeed() != 0L, "NBT round-trip lost its loot seed at " + pos);
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            require(reloaded.createMenu(0, player.getInventory(), player) != null, "Replacement did not create its chest menu at " + pos);
            require(countItems(reloaded) > 0, "Replacement generated an empty inventory at " + pos);
            player.discard();
            require(reloaded.getLootTable() == null, "Loot table was not consumed after opening at " + pos);
        }

        helper.succeed();
    }

    private static void setLootTable(GameTestHelper helper, BlockPos pos, long seed) {
        RandomizableContainerBlockEntity container = (RandomizableContainerBlockEntity)helper.getBlockEntity(pos);
        container.setLootTable(ANCIENT_CITY);
        container.setLootTableSeed(seed);
    }

    private static int countItems(Container container) {
        int count = 0;

        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            count += container.getItem(slot).getCount();
        }

        return count;
    }

    private static void requireBlock(GameTestHelper helper, BlockPos pos, ResourceLocation expected) {
        ResourceLocation actual = BuiltInRegistries.BLOCK.getKey(helper.getBlockState(pos).getBlock());
        require(actual.equals(expected), "Expected " + expected + " at " + pos + ", got " + actual);
    }

    private static ResourceKey<LootTable> lootTable(String namespace, String path) {
        return ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(namespace, path));
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

    private AncientCityLootGameTests() {
    }
}
