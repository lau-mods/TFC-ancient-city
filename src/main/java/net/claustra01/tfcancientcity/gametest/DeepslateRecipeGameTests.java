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
import net.claustra01.tfcancientcity.registry.ModItems;
import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.component.size.ItemSizeDefinition;
import net.dries007.tfc.common.component.size.ItemSizeManager;
import net.dries007.tfc.common.component.size.Size;
import net.dries007.tfc.common.component.size.Weight;
import net.dries007.tfc.common.player.ChiselMode;
import net.dries007.tfc.common.recipes.ChiselRecipe;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforgespi.locating.IModFile;

@GameTestHolder(TfcAncientCity.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DeepslateRecipeGameTests {
    /** TFC rock type to Vanilla Deepslate block: Cobble, Smooth, Bricks and the Tile variant of Bricks. */
    private static final List<String> SHAPED_FAMILIES = List.of("cobbled_deepslate", "polished_deepslate", "deepslate_brick", "deepslate_tile");
    private static final List<String> DISABLED_CRAFTING = List.of(
        "deepslate", "polished_deepslate", "deepslate_bricks", "chiseled_deepslate", "cracked_deepslate_bricks", "cracked_deepslate_tiles"
    );

    @GameTest(template = "empty")
    public static void vanillaCobbleIsTheDeepslateCobble(GameTestHelper helper) {
        ResourceLocation removed = id("rock/cobble/deepslate");
        require(!BuiltInRegistries.BLOCK.containsKey(removed), "A separate Cobble Deepslate block is still registered");
        require(!BuiltInRegistries.ITEM.containsKey(removed), "A separate Cobble Deepslate item is still registered");
        require(!Blocks.COBBLED_DEEPSLATE.defaultBlockState().is(TFCTags.Blocks.CAN_LANDSLIDE), "Vanilla Cobbled Deepslate was added to can_landslide");

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
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cobbleAndLooseRecipes(GameTestHelper helper) {
        Item loose = ModItems.LOOSE_DEEPSLATE.get();
        CraftingInput looseSquare = CraftingInput.of(2, 2, List.of(
            new ItemStack(loose), new ItemStack(loose), new ItemStack(loose), new ItemStack(loose)
        ));
        assertCraft(helper, "cobbled_deepslate", looseSquare, Blocks.COBBLED_DEEPSLATE.asItem(), 1);
        assertOnlyResult(helper, looseSquare, Blocks.COBBLED_DEEPSLATE.asItem(), 1);
        assertCraft(helper, "loose_deepslate", single(Blocks.COBBLED_DEEPSLATE), loose, 4);
        assertCraft(helper, "loose_deepslate_from_slabs", single(Blocks.COBBLED_DEEPSLATE_SLAB), loose, 2);
        assertCraft(helper, "loose_deepslate_from_stairs", single(Blocks.COBBLED_DEEPSLATE_STAIRS), loose, 3);
        assertCraft(helper, "loose_deepslate_from_walls", single(Blocks.COBBLED_DEEPSLATE_WALL), loose, 4);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vanillaBypassRecipesDisabledAndShapesFollowTfc(GameTestHelper helper) {
        RecipeManager manager = helper.getLevel().getRecipeManager();
        for (String name : DISABLED_CRAFTING) {
            require(manager.byKey(ResourceLocation.withDefaultNamespace(name)).isEmpty(), "Vanilla recipe minecraft:" + name + " is still enabled");
        }
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            ResourceLocation recipeId = holder.id();
            ItemStack result = holder.value().getResultItem(helper.getLevel().registryAccess());
            require(!result.is(Items.DEEPSLATE), "Raw Deepslate is craftable through " + recipeId);
            require(!(holder.value().getType() == RecipeType.STONECUTTING && BuiltInRegistries.ITEM.getKey(result.getItem()).getPath().contains("deepslate")),
                "Deepslate stonecutting recipe remains: " + recipeId);
            require(!(holder.value().getType() == RecipeType.SMELTING && (result.is(Items.CRACKED_DEEPSLATE_BRICKS) || result.is(Items.CRACKED_DEEPSLATE_TILES))),
                "Cracked Deepslate is still smeltable through " + recipeId);
        }
        CraftingInput cobbleSquare = CraftingInput.of(2, 2, List.of(
            new ItemStack(Blocks.COBBLED_DEEPSLATE), new ItemStack(Blocks.COBBLED_DEEPSLATE),
            new ItemStack(Blocks.COBBLED_DEEPSLATE), new ItemStack(Blocks.COBBLED_DEEPSLATE)
        ));
        require(manager.getRecipeFor(RecipeType.CRAFTING, cobbleSquare, helper.getLevel()).isEmpty(), "Cobbled Deepslate still crafts Polished Deepslate");

        for (String family : SHAPED_FAMILIES) {
            Item base = BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(baseBlock(family)));
            ItemStack b = new ItemStack(base);
            assertOnlyResult(helper, CraftingInput.of(3, 3, List.of(
                b.copy(), ItemStack.EMPTY, ItemStack.EMPTY, b.copy(), b.copy(), ItemStack.EMPTY, b.copy(), b.copy(), b.copy()
            )), item(family + "_stairs"), 8);
            assertOnlyResult(helper, CraftingInput.of(3, 1, List.of(b.copy(), b.copy(), b.copy())), item(family + "_slab"), 6);
            assertOnlyResult(helper, CraftingInput.of(3, 2, List.of(b.copy(), b.copy(), b.copy(), b.copy(), b.copy(), b.copy())), item(family + "_wall"), 6);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void chiselShapesDeepslateInWorld(GameTestHelper helper) {
        assertChisel(Blocks.DEEPSLATE.defaultBlockState(), ChiselMode.SMOOTH.get(), Items.POLISHED_DEEPSLATE, null);
        assertChisel(Blocks.DEEPSLATE_BRICKS.defaultBlockState(), ChiselMode.SMOOTH.get(), Items.CHISELED_DEEPSLATE, null);
        for (String family : SHAPED_FAMILIES) {
            BlockState base = BuiltInRegistries.BLOCK.get(ResourceLocation.withDefaultNamespace(baseBlock(family))).defaultBlockState();
            assertChisel(base, ChiselMode.SLAB.get(), item(family + "_slab"), item(family + "_slab"));
            assertChisel(base, ChiselMode.STAIR.get(), item(family + "_stairs"), null);
        }
        require(ChiselRecipe.getRecipe(Blocks.COBBLED_DEEPSLATE.defaultBlockState(), ChiselMode.SMOOTH.get()) == null,
            "Cobbled Deepslate must not chisel into Polished Deepslate");
        require(ChiselRecipe.getRecipe(Blocks.REINFORCED_DEEPSLATE.defaultBlockState(), ChiselMode.SMOOTH.get()) == null,
            "Reinforced Deepslate gained a chisel recipe");
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

        for (Object[] toolRecipe : new Object[][] {
            {"cracked_deepslate_bricks", Blocks.DEEPSLATE_BRICKS, hammer, Items.CRACKED_DEEPSLATE_BRICKS, "Hammer"},
            {"cracked_deepslate_tiles", Blocks.DEEPSLATE_TILES, hammer, Items.CRACKED_DEEPSLATE_TILES, "Hammer"},
            {"polished_deepslate", Blocks.DEEPSLATE, chisel, Items.POLISHED_DEEPSLATE, "Chisel"},
            {"chiseled_deepslate", Blocks.DEEPSLATE_BRICKS, chisel, Items.CHISELED_DEEPSLATE, "Chisel"}
        }) {
            CraftingInput input = CraftingInput.of(2, 1, List.of(new ItemStack((Block)toolRecipe[1]), new ItemStack((Item)toolRecipe[2])));
            CraftingRecipe recipe = assertCraft(helper, (String)toolRecipe[0], input, (Item)toolRecipe[3], 1);
            assertDamagedRemainder(recipe, input, (Item)toolRecipe[2], 1, (String)toolRecipe[4]);
        }
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

    private static void assertOnlyResult(GameTestHelper helper, CraftingInput input, Item expectedItem, int expectedCount) {
        List<RecipeHolder<CraftingRecipe>> matching = helper.getLevel().getRecipeManager()
            .getRecipesFor(RecipeType.CRAFTING, input, helper.getLevel());
        require(matching.size() == 1, "Expected one recipe for " + expectedItem + ", found " + matching.stream().map(RecipeHolder::id).toList());
        ItemStack result = matching.getFirst().value().assemble(input, helper.getLevel().registryAccess());
        require(result.is(expectedItem) && result.getCount() == expectedCount,
            matching.getFirst().id() + " produced " + result + " instead of " + expectedCount + " " + expectedItem);
    }

    private static void assertChisel(BlockState input, ChiselMode mode, Item expectedBlock, Item expectedExtra) {
        ChiselRecipe recipe = ChiselRecipe.getRecipe(input, mode);
        require(recipe != null, "Missing chisel recipe for " + input + " in " + mode);
        ItemStack result = recipe.getResultItem(null);
        require(result.is(expectedBlock), "Chiseling " + input + " produced " + result);
        ItemStack extra = recipe.getItemOutput(ItemStack.EMPTY);
        require(expectedExtra == null ? extra.isEmpty() : extra.is(expectedExtra) && extra.getCount() == 1,
            "Chiseling " + input + " returned an unexpected extra item " + extra);
    }

    private static CraftingInput single(Block block) {
        return CraftingInput.of(1, 1, List.of(new ItemStack(block)));
    }

    private static String baseBlock(String family) {
        return switch (family) {
            case "deepslate_brick" -> "deepslate_bricks";
            case "deepslate_tile" -> "deepslate_tiles";
            default -> family;
        };
    }

    private static Item item(String vanillaPath) {
        return item("minecraft", vanillaPath);
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

    private DeepslateRecipeGameTests() {
    }
}
