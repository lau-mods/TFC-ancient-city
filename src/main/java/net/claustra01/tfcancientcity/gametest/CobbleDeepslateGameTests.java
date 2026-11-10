package net.claustra01.tfcancientcity.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.claustra01.tfcancientcity.TfcAncientCity;
import net.claustra01.tfcancientcity.accessor.StructureTemplateIdAccessor;
import net.claustra01.tfcancientcity.registry.ModBlocks;
import net.claustra01.tfcancientcity.registry.ModItems;
import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.component.size.ItemSizeDefinition;
import net.dries007.tfc.common.component.size.ItemSizeManager;
import net.dries007.tfc.common.component.size.Size;
import net.dries007.tfc.common.component.size.Weight;
import net.dries007.tfc.common.recipes.KnappingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforgespi.locating.IModFile;

@GameTestHolder(TfcAncientCity.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CobbleDeepslateGameTests {
    @GameTest(template = "empty")
    public static void cobbleRegistrationDropsAndAncientCityIndependence(GameTestHelper helper) {
        Block cobble = ModBlocks.COBBLE_DEEPSLATE.get();
        Item cobbleItem = ModItems.COBBLE_DEEPSLATE.get();
        require(BuiltInRegistries.BLOCK.get(id("rock/cobble/deepslate")) == cobble, "Cobble Deepslate block ID is missing");
        require(BuiltInRegistries.ITEM.get(id("rock/cobble/deepslate")) == cobbleItem, "Cobble Deepslate item ID is missing");
        require(cobbleItem instanceof BlockItem blockItem && blockItem.getBlock() == cobble, "Cobble Deepslate BlockItem points at the wrong block");
        require(cobble != Blocks.COBBLED_DEEPSLATE, "Cobble Deepslate replaced Vanilla Cobbled Deepslate");
        require(cobble.defaultBlockState().is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE), "Cobble Deepslate is not pickaxe-mineable");

        LootParams params = new LootParams.Builder(helper.getLevel())
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 2, 1))))
            .withParameter(LootContextParams.BLOCK_STATE, cobble.defaultBlockState())
            .withParameter(LootContextParams.TOOL, new ItemStack(Items.NETHERITE_PICKAXE))
            .create(LootContextParamSets.BLOCK);
        LootTable table = helper.getLevel().getServer().reloadableRegistries().getLootTable(cobble.getLootTable());
        List<ItemStack> drops = table.getRandomItems(params, 41L);
        require(drops.size() == 1 && drops.getFirst().is(cobbleItem) && drops.getFirst().getCount() == 1,
            "Cobble Deepslate did not drop exactly one of itself");

        BlockPos pos = new BlockPos(3, 2, 3);
        helper.setBlock(pos, Blocks.COBBLED_DEEPSLATE);
        StructureTemplate template = new StructureTemplate();
        template.fillFromWorld(helper.getLevel(), helper.absolutePos(pos), new Vec3i(1, 1, 1), false, Blocks.STRUCTURE_VOID);
        ((StructureTemplateIdAccessor)template).tfcAncientCity$setTemplateId(ResourceLocation.withDefaultNamespace("ancient_city/m04_cobble_independence"));
        helper.setBlock(pos, Blocks.AIR);
        require(template.placeInWorld(
            helper.getLevel(), helper.absolutePos(pos), helper.absolutePos(pos), new StructurePlaceSettings(), RandomSource.create(41L), 2
        ), "Ancient City fixture did not place");
        require(helper.getBlockState(pos).is(Blocks.COBBLED_DEEPSLATE), "Ancient City Vanilla Cobbled Deepslate was replaced");
        require(!helper.getBlockState(pos).is(cobble), "Ancient City unexpectedly gained the new Cobble Deepslate");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cobbleAndLooseRecipes(GameTestHelper helper) {
        Item loose = ModItems.LOOSE_DEEPSLATE.get();
        CraftingInput looseSquare = CraftingInput.of(2, 2, List.of(
            new ItemStack(loose), new ItemStack(loose), new ItemStack(loose), new ItemStack(loose)
        ));
        assertCraft(helper, "cobble_deepslate", looseSquare, ModItems.COBBLE_DEEPSLATE.get(), 1);
        List<RecipeHolder<CraftingRecipe>> matching = helper.getLevel().getRecipeManager()
            .getRecipesFor(RecipeType.CRAFTING, looseSquare, helper.getLevel());
        require(matching.stream().map(recipe -> recipe.value().assemble(looseSquare, helper.getLevel().registryAccess()))
            .noneMatch(result -> result.is(Blocks.COBBLED_DEEPSLATE.asItem())),
            "Loose Deepslate still crafts Vanilla Cobbled Deepslate");

        CraftingInput newCobble = CraftingInput.of(1, 1, List.of(new ItemStack(ModItems.COBBLE_DEEPSLATE.get())));
        assertCraft(helper, "loose_deepslate_from_cobble", newCobble, loose, 4);
        CraftingInput vanillaCobble = CraftingInput.of(1, 1, List.of(new ItemStack(Blocks.COBBLED_DEEPSLATE)));
        assertCraft(helper, "loose_deepslate_from_vanilla_cobble", vanillaCobble, loose, 4);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void brickRecipesDamageTools(GameTestHelper helper) {
        Item chisel = item("tfc", "metal/chisel/copper");
        Item hammer = item("tfc", "metal/hammer/copper");
        Item mortar = item("tfc", "mortar");

        CraftingInput chiselInput = CraftingInput.of(2, 1, List.of(
            new ItemStack(ModItems.LOOSE_DEEPSLATE.get()), new ItemStack(chisel)
        ));
        CraftingRecipe chiselRecipe = assertCraft(helper, "deepslate_brick", chiselInput, ModItems.DEEPSLATE_BRICK.get(), 1);
        assertDamagedRemainder(chiselRecipe, chiselInput, chisel, 1, "Chisel");

        CraftingInput bricksInput = CraftingInput.of(3, 3, List.of(
            new ItemStack(ModItems.DEEPSLATE_BRICK.get()), new ItemStack(mortar), new ItemStack(ModItems.DEEPSLATE_BRICK.get()),
            new ItemStack(mortar), new ItemStack(ModItems.DEEPSLATE_BRICK.get()), new ItemStack(mortar),
            new ItemStack(ModItems.DEEPSLATE_BRICK.get()), new ItemStack(mortar), new ItemStack(ModItems.DEEPSLATE_BRICK.get())
        ));
        CraftingRecipe bricksRecipe = assertCraft(helper, "deepslate_bricks", bricksInput, Blocks.DEEPSLATE_BRICKS.asItem(), 4);
        NonNullList<ItemStack> bricksRemainders = bricksRecipe.getRemainingItems(bricksInput);
        require(bricksRemainders.stream().allMatch(ItemStack::isEmpty), "Brick or Mortar was retained instead of consumed");
        require(bricksRecipe.getIngredients().stream().filter(ingredient -> ingredient.test(new ItemStack(ModItems.DEEPSLATE_BRICK.get()))).count() == 5,
            "Deepslate Bricks recipe must consume five brick materials");
        require(bricksRecipe.getIngredients().stream().filter(ingredient -> ingredient.test(new ItemStack(mortar))).count() == 4,
            "Deepslate Bricks recipe must consume four Mortar");

        CraftingInput hammerInput = CraftingInput.of(2, 1, List.of(
            new ItemStack(Blocks.DEEPSLATE_BRICKS), new ItemStack(hammer)
        ));
        CraftingRecipe hammerRecipe = assertCraft(helper, "cracked_deepslate_bricks", hammerInput, Blocks.CRACKED_DEEPSLATE_BRICKS.asItem(), 1);
        assertDamagedRemainder(hammerRecipe, hammerInput, hammer, 1, "Hammer");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void knappingAndItemSizes(GameTestHelper helper) {
        ItemStack loose = new ItemStack(ModItems.LOOSE_DEEPSLATE.get());
        require(loose.is(TFCTags.Items.ROCK_KNAPPING), "Loose Deepslate is absent from tfc:rock_knapping");
        var metamorphic = net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.ITEM, ResourceLocation.fromNamespaceAndPath("tfc", "stones/loose/metamorphic")
        );
        require(loose.is(metamorphic), "Loose Deepslate is absent from tfc:stones/loose/metamorphic");

        RecipeHolder<?> holder = helper.getLevel().getRecipeManager().byKey(
            ResourceLocation.fromNamespaceAndPath("tfc", "knapping/stone/axe_head/metamorphic")
        ).orElseThrow(() -> new GameTestAssertException("TFC metamorphic axe head Knapping recipe is missing"));
        require(holder.value() instanceof KnappingRecipe, "TFC metamorphic axe head is not a Knapping recipe");
        KnappingRecipe recipe = (KnappingRecipe)holder.value();
        require(recipe.matchesItem(loose), "Loose Deepslate does not satisfy the metamorphic Knapping recipe");
        ItemStack result = recipe.assemble();
        require(BuiltInRegistries.ITEM.getKey(result.getItem()).equals(ResourceLocation.fromNamespaceAndPath("tfc", "stone/axe_head/metamorphic")),
            "Metamorphic Knapping produced the wrong tool head");

        assertSize(new ItemStack(ModItems.LOOSE_DEEPSLATE.get()), Size.SMALL, Weight.LIGHT);
        assertSize(new ItemStack(ModItems.COBBLE_DEEPSLATE.get()), Size.SMALL, Weight.LIGHT);
        assertSize(new ItemStack(ModItems.DEEPSLATE_BRICK.get()), Size.VERY_SMALL, Weight.VERY_LIGHT);
        require(Files.isRegularFile(modFile().findResource("assets", "tfc", "textures", "gui", "knapping", "rock", "loose", "deepslate.png")),
            "Loose Deepslate Knapping GUI texture is missing from TFC's resolved asset namespace");

        assertTagJson("rock_knapping.json", "data", "tfc", "tags", "item", "rock_knapping.json");
        assertTagJson("metamorphic.json", "data", "tfc", "tags", "item", "stones", "loose", "metamorphic.json");
        helper.succeed();
    }

    private static CraftingRecipe assertCraft(
        GameTestHelper helper,
        String recipePath,
        CraftingInput input,
        Item expectedItem,
        int expectedCount
    ) {
        RecipeManager manager = helper.getLevel().getRecipeManager();
        RecipeHolder<?> holder = manager.byKey(id(recipePath))
            .orElseThrow(() -> new GameTestAssertException("Missing recipe " + recipePath));
        require(holder.value() instanceof CraftingRecipe, recipePath + " is not a crafting recipe");
        CraftingRecipe recipe = (CraftingRecipe)holder.value();
        require(recipe.matches(input, helper.getLevel()), recipePath + " does not match its specified input");
        ItemStack result = recipe.assemble(input, helper.getLevel().registryAccess());
        require(result.is(expectedItem) && result.getCount() == expectedCount,
            recipePath + " produced " + result + " instead of " + expectedCount + " " + expectedItem);
        return recipe;
    }

    private static void assertDamagedRemainder(
        CraftingRecipe recipe,
        CraftingInput input,
        Item tool,
        int toolSlot,
        String toolName
    ) {
        NonNullList<ItemStack> remainders = recipe.getRemainingItems(input);
        ItemStack remainder = remainders.get(toolSlot);
        require(remainder.is(tool), toolName + " was consumed instead of retained");
        require(remainder.getDamageValue() == 1, toolName + " durability changed by " + remainder.getDamageValue() + " instead of one");
        for (int slot = 0; slot < remainders.size(); slot++) {
            if (slot != toolSlot) {
                require(remainders.get(slot).isEmpty(), "Crafting material in slot " + slot + " was not consumed");
            }
        }
    }

    private static void assertSize(ItemStack stack, Size expectedSize, Weight expectedWeight) {
        require(ItemSizeManager.get(stack).getSize(stack) == expectedSize, stack + " has the wrong size");
        require(ItemSizeManager.get(stack).getWeight(stack) == expectedWeight, stack + " has the wrong weight");
        long definitions = ItemSizeManager.MANAGER.getElements().values().stream()
            .map(ItemSizeDefinition.class::cast)
            .filter(definition -> definition.ingredient().test(stack))
            .count();
        require(definitions == 1, stack + " has " + definitions + " explicit size definitions instead of one");
    }

    private static void assertTagJson(String name, String... path) {
        JsonObject json;
        try (Reader reader = Files.newBufferedReader(modFile().findResource(path))) {
            json = JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException exception) {
            throw new GameTestAssertException("Could not read " + name + ": " + exception.getMessage());
        }
        require(!json.get("replace").getAsBoolean(), name + " must preserve TFC values");
        Set<String> values = json.getAsJsonArray("values").asList().stream().map(value -> value.getAsString()).collect(Collectors.toSet());
        require(values.equals(Set.of("tfcancientcity:rock/loose/deepslate")), name + " contains unexpected additions: " + values);
    }

    private static Item item(String namespace, String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, path);
        require(BuiltInRegistries.ITEM.containsKey(id), "Missing required item " + id);
        return BuiltInRegistries.ITEM.get(id);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(TfcAncientCity.MOD_ID, path);
    }

    private static IModFile modFile() {
        return ModList.get().getModFileById(TfcAncientCity.MOD_ID).getFile();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new GameTestAssertException(message);
        }
    }

    private CobbleDeepslateGameTests() {
    }
}
