package net.claustra01.tfcancientcity.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import net.claustra01.tfcancientcity.TfcAncientCity;
import net.dries007.tfc.common.component.food.FoodCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
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
    private static final List<String> LOOT_DATA = List.of(
        "data/minecraft/loot_table/chests/ancient_city.json",
        "data/minecraft/loot_table/chests/ancient_city_ice_box.json",
        "data/tfcancientcity/loot_table/shared/coal_like.json",
        "data/tfcancientcity/loot_table/shared/gems.json",
        "data/tfcancientcity/loot_table/shared/produce.json",
        "data/tfcancientcity/loot_table/shared/equipment/lategame/greaves.json",
        "data/tfcancientcity/loot_table/shared/equipment/lategame/hoe.json",
        "data/tfcancientcity/loot_table/shared/equipment/lategame/horse_armor.json",
        "data/tfcancientcity/tags/item/loot/produce.json"
    );
    private static final ResourceKey<LootTable> ANCIENT_CITY = lootTable("minecraft", "chests/ancient_city");
    private static final ResourceKey<LootTable> ICE_BOX = lootTable("minecraft", "chests/ancient_city_ice_box");

    @GameTest(template = "empty")
    public static void lootDataMatchesFixedReferenceAndResolves(GameTestHelper helper) {
        for (String path : LOOT_DATA) {
            JsonElement actual = readJson("/" + path);
            JsonElement reference = readJson("/tfcancientcity/gametest/reference/" + path);
            require(actual.equals(reference), "Loot data differs recursively from v0.1.1: " + path);
            validateReferences(actual, helper);
        }

        JsonObject ancientCity = readJson("/data/minecraft/loot_table/chests/ancient_city.json").getAsJsonObject();
        JsonArray pools = ancientCity.getAsJsonArray("pools");
        require(pools.size() == 2, "Ancient City pool count changed");
        JsonObject primary = pools.get(0).getAsJsonObject();
        require(primary.getAsJsonObject("rolls").get("min").getAsInt() == 5, "Ancient City minimum rolls changed");
        require(primary.getAsJsonObject("rolls").get("max").getAsInt() == 10, "Ancient City maximum rolls changed");
        require(primary.getAsJsonArray("entries").size() == 28, "Ancient City primary entry count changed");
        require(pools.get(1).getAsJsonObject().get("rolls").getAsInt() == 1, "Ancient City rare pool rolls changed");
        JsonArray rareEntries = pools.get(1).getAsJsonObject().getAsJsonArray("entries");
        require(entryWeight(rareEntries, "minecraft:empty") == 75, "Ancient City empty weight changed");
        require(entryWeight(rareEntries, "minecraft:ward_armor_trim_smithing_template") == 4, "Ward trim weight changed");
        require(entryWeight(rareEntries, "minecraft:silence_armor_trim_smithing_template") == 1, "Silence trim weight changed");
        JsonObject iceBox = readJson("/data/minecraft/loot_table/chests/ancient_city_ice_box.json").getAsJsonObject();
        JsonObject icePool = iceBox.getAsJsonArray("pools").get(0).getAsJsonObject();
        require(icePool.getAsJsonObject("rolls").get("min").getAsInt() == 4, "Ice Box minimum rolls changed");
        require(icePool.getAsJsonObject("rolls").get("max").getAsInt() == 10, "Ice Box maximum rolls changed");
        require(icePool.getAsJsonArray("entries").size() == 5, "Ice Box entry count changed");
        require(sharedEntryCount("gems") == 9, "Gem entry count changed");
        require(sharedEntryCount("coal_like") == 4, "Coal-like entry count changed");
        require(
            readJson("/data/tfcancientcity/tags/item/loot/produce.json").getAsJsonObject().getAsJsonArray("values").size() == 39,
            "Produce tag entry count changed"
        );

        for (String id : List.of(
            "tfcancientcity:shared/coal_like",
            "tfcancientcity:shared/gems",
            "tfcancientcity:shared/produce",
            "tfcancientcity:shared/equipment/lategame/greaves",
            "tfcancientcity:shared/equipment/lategame/hoe",
            "tfcancientcity:shared/equipment/lategame/horse_armor",
            "minecraft:chests/ancient_city",
            "minecraft:chests/ancient_city_ice_box"
        )) {
            LootTable table = helper.getLevel()
                .getServer()
                .reloadableRegistries()
                .getLootTable(ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.parse(id)));
            require(table != LootTable.EMPTY, "Loot table failed to load: " + id);
        }

        TagKey<Item> produceTag = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("tfcancientcity", "loot/produce"));
        int loadedProduceCount = BuiltInRegistries.ITEM.getTag(produceTag).orElseThrow().size();
        require(loadedProduceCount == 39, "Loaded produce tag contains " + loadedProduceCount + " entries");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void fixedSeedsPreserveAncientCityLoot(GameTestHelper helper) {
        LootTable ancientCity = helper.getLevel().getServer().reloadableRegistries().getLootTable(ANCIENT_CITY);
        LootTable iceBox = helper.getLevel().getServer().reloadableRegistries().getLootTable(ICE_BOX);
        LootParams params = new Builder(helper.getLevel())
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)))
            .create(LootContextParamSets.CHEST);
        Set<ResourceLocation> allowedNormal = referenceItems("data/minecraft/loot_table/chests/ancient_city.json");
        Set<ResourceLocation> allowedIce = referenceItems("data/minecraft/loot_table/chests/ancient_city_ice_box.json");
        allowedNormal.add(minecraft("enchanted_book"));
        Set<ResourceLocation> normalItems = new HashSet<>();
        Set<ResourceLocation> iceItems = new HashSet<>();
        boolean validFood = false;
        int ward = 0;
        int silence = 0;
        int packedIce = 0;
        int snowball = 0;

        for (long seed = 0L; seed < 4096L; seed++) {
            for (ItemStack stack : ancientCity.getRandomItems(params, seed)) {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                require(allowedNormal.contains(id), "Unexpected Ancient City reward " + id);
                normalItems.add(id);
                if (id.equals(minecraft("ward_armor_trim_smithing_template"))) {
                    ward++;
                }

                if (id.equals(minecraft("silence_armor_trim_smithing_template"))) {
                    silence++;
                }

                if (FoodCapability.has(stack)) {
                    validFood |= !FoodCapability.isRotten(stack);
                }
            }

            for (ItemStack stack : iceBox.getRandomItems(params, seed)) {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                require(allowedIce.contains(id), "Unexpected Ice Box reward " + id);
                iceItems.add(id);
                if (id.equals(minecraft("packed_ice"))) {
                    packedIce++;
                }

                if (id.equals(minecraft("snowball"))) {
                    snowball++;
                }

                if (FoodCapability.has(stack)) {
                    validFood |= !FoodCapability.isRotten(stack);
                }
            }
        }

        require(ward >= 140 && ward <= 270, "Ward trim frequency differs from the reference 4/80 weight: " + ward);
        require(silence >= 25 && silence <= 85, "Silence trim frequency differs from the reference 1/80 weight: " + silence);
        require(
            packedIce > 0 && snowball > 0 && packedIce * 100 / snowball >= 45 && packedIce * 100 / snowball <= 55,
            "Ice Box packed ice to snowball frequency differs from the reference 2:4 weight"
        );
        require(normalItems.contains(minecraft("echo_shard")), "Echo Shards disappeared from Ancient City loot");
        require(normalItems.stream().anyMatch(id -> id.getPath().startsWith("music_disc_")), "Music discs disappeared from Ancient City loot");
        require(normalItems.contains(minecraft("ward_armor_trim_smithing_template")), "Ward trim disappeared from Ancient City loot");
        require(normalItems.contains(minecraft("silence_armor_trim_smithing_template")), "Silence trim disappeared from Ancient City loot");
        require(normalItems.contains(minecraft("enchanted_book")), "Enchanted books disappeared from Ancient City loot");

        for (String path : List.of(
            "metal/unfinished_greaves/steel",
            "metal/greaves/steel",
            "metal/hoe_head/steel",
            "metal/scythe_blade/steel",
            "metal/hoe/steel",
            "metal/scythe/steel",
            "metal/horse_armor/steel"
        )) {
            require(normalItems.contains(tfc(path)), "Missing specific TFC steel reward " + path);
        }

        require(
            normalItems.stream().anyMatch(id -> id.getNamespace().equals("tfc") && id.getPath().startsWith("gem/")),
            "TFC gems disappeared from Ancient City loot"
        );

        for (ResourceLocation coal : List.of(minecraft("charcoal"), tfc("ore/bituminous_coal"), tfc("ore/lignite"), tfc("ore/graphite"))) {
            require(normalItems.contains(coal), "Missing coal-like reward " + coal);
        }

        require(iceItems.contains(minecraft("packed_ice")) && iceItems.contains(minecraft("snowball")), "Ice Box Vanilla rewards changed");
        require(
            validFood && iceItems.stream().anyMatch(id -> id.getNamespace().equals("tfc") && id.getPath().startsWith("food/")),
            "TFC food rewards did not preserve their food data"
        );
        helper.succeed();
    }

    private static Set<ResourceLocation> referenceItems(String path) {
        Set<ResourceLocation> items = new HashSet<>();

        for (JsonElement pool : readJson("/tfcancientcity/gametest/reference/" + path).getAsJsonObject().getAsJsonArray("pools")) {
            for (JsonElement entry : pool.getAsJsonObject().getAsJsonArray("entries")) {
                JsonObject object = entry.getAsJsonObject();
                String type = object.get("type").getAsString();
                if (type.equals("minecraft:item")) {
                    items.add(ResourceLocation.parse(object.get("name").getAsString()));
                } else if (type.equals("minecraft:loot_table")) {
                    ResourceLocation id = ResourceLocation.parse(object.get("value").getAsString());
                    items.addAll(referenceItems("data/" + id.getNamespace() + "/loot_table/" + id.getPath() + ".json"));
                } else if (type.equals("minecraft:tag")) {
                    ResourceLocation id = ResourceLocation.parse(object.get("name").getAsString());

                    for (JsonElement value : readJson("/tfcancientcity/gametest/reference/data/" + id.getNamespace() + "/tags/item/" + id.getPath() + ".json")
                        .getAsJsonObject()
                        .getAsJsonArray("values")) {
                        items.add(ResourceLocation.parse(value.getAsString()));
                    }
                }
            }
        }

        return items;
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

    private static void validateReferences(JsonElement element, GameTestHelper helper) {
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                validateReferences(child, helper);
            }
        } else if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("type") && object.get("type").isJsonPrimitive()) {
                String type = object.get("type").getAsString();
                if (type.equals("minecraft:item") && object.has("name")) {
                    ResourceLocation item = ResourceLocation.parse(object.get("name").getAsString());
                    require(BuiltInRegistries.ITEM.containsKey(item), "Unknown loot item " + item);
                } else if (type.equals("minecraft:loot_table") && object.has("value")) {
                    ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.parse(object.get("value").getAsString()));
                    require(helper.getLevel().getServer().reloadableRegistries().getLootTable(key) != LootTable.EMPTY, "Unknown nested loot table " + key.location());
                } else if (type.equals("minecraft:tag") && object.has("name")) {
                    TagKey<Item> tag = TagKey.create(Registries.ITEM, ResourceLocation.parse(object.get("name").getAsString()));
                    require(BuiltInRegistries.ITEM.getTag(tag).isPresent(), "Unknown loot item tag " + tag.location());
                }
            }

            for (Entry<String, JsonElement> entry : object.entrySet()) {
                validateReferences(entry.getValue(), helper);
            }
        }
    }

    private static int entryWeight(JsonArray entries, String name) {
        for (JsonElement element : entries) {
            JsonObject entry = element.getAsJsonObject();
            String entryName = entry.has("name") ? entry.get("name").getAsString() : entry.get("type").getAsString();
            if (entryName.equals(name)) {
                return entry.has("weight") ? entry.get("weight").getAsInt() : 1;
            }
        }

        throw new GameTestAssertException("Missing loot entry " + name);
    }

    private static int sharedEntryCount(String name) {
        return readJson("/data/tfcancientcity/loot_table/shared/" + name + ".json")
            .getAsJsonObject()
            .getAsJsonArray("pools")
            .get(0)
            .getAsJsonObject()
            .getAsJsonArray("entries")
            .size();
    }

    private static JsonElement readJson(String path) {
        try (InputStream stream = AncientCityLootGameTests.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new GameTestAssertException("Missing resource " + path);
            }
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new GameTestAssertException("Could not read " + path + ": " + exception.getMessage());
        }
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
