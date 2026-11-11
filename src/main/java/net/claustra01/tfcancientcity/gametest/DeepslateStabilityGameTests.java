package net.claustra01.tfcancientcity.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.claustra01.tfcancientcity.TfcAncientCity;
import net.claustra01.tfcancientcity.accessor.StructureTemplateIdAccessor;
import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.entities.misc.TFCFallingBlockEntity;
import net.dries007.tfc.common.recipes.CollapseRecipe;
import net.dries007.tfc.common.recipes.LandslideRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforgespi.locating.IModFile;

@GameTestHolder(TfcAncientCity.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DeepslateStabilityGameTests {
    private static final List<Block> PROTECTED_DEEPSLATE = List.of(
        Blocks.DEEPSLATE,
        Blocks.COBBLED_DEEPSLATE,
        Blocks.DEEPSLATE_BRICKS,
        Blocks.DEEPSLATE_TILES,
        Blocks.DEEPSLATE_IRON_ORE
    );

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void deepslateNeverCollapsesOrHardens(GameTestHelper helper) {
        ResourceLocation hardened = id("rock/hardened/deepslate");
        require(!BuiltInRegistries.BLOCK.containsKey(hardened), "Hardened Deepslate block was registered");
        require(!BuiltInRegistries.ITEM.containsKey(hardened), "Hardened Deepslate item was registered");
        require(!modResourceExists("data", "tfc", "tags", "block", "can_start_collapse.json"),
            "The mod supplies tfc:can_start_collapse data");
        require(!modResourceExists("data", "tfc", "tags", "block", "can_collapse.json"),
            "The mod supplies tfc:can_collapse data");
        require(!modResourceExists("data", "tfc", "tags", "block", "can_trigger_collapse.json"),
            "The mod supplies tfc:can_trigger_collapse data");
        require(modRecipeTypes().stream().noneMatch("tfc:collapse"::equals), "The mod supplies a TFC collapse recipe");

        int buildingBlocks = 4;
        for (int row = 0; row < 3; row++) {
            for (int index = 0; index < buildingBlocks; index++) {
                BlockPos pos = new BlockPos(2 + index, 4, 2 + row);
                helper.setBlock(pos.below(), index % 2 == 0 ? Blocks.AIR : Blocks.CAVE_AIR);
                helper.setBlock(pos, PROTECTED_DEEPSLATE.get(index));
            }
        }
        for (int index = 0; index < buildingBlocks; index++) {
            BlockPos pos = new BlockPos(2 + index, 4, 3);
            require(!CollapseRecipe.canStartCollapse(helper.getLevel(), helper.absolutePos(pos)), "Deepslate can start collapse at " + pos);
            CollapseRecipe.tryTriggerCollapse(helper.getLevel(), helper.absolutePos(pos));
        }

        BlockPos templatePos = new BlockPos(2, 4, 9);
        for (int index = 0; index < buildingBlocks; index++) {
            helper.setBlock(templatePos.offset(index, 0, 0), PROTECTED_DEEPSLATE.get(index));
        }
        StructureTemplate template = new StructureTemplate();
        template.fillFromWorld(helper.getLevel(), helper.absolutePos(templatePos), new Vec3i(buildingBlocks, 1, 1), false, Blocks.STRUCTURE_VOID);
        ((StructureTemplateIdAccessor)template).tfcAncientCity$setTemplateId(ResourceLocation.withDefaultNamespace("ancient_city/m04_unsupported_deepslate"));
        for (int index = 0; index < buildingBlocks; index++) {
            helper.setBlock(templatePos.offset(index, 0, 0), Blocks.AIR);
            helper.setBlock(templatePos.offset(index, -1, 0), index % 2 == 0 ? Blocks.AIR : Blocks.CAVE_AIR);
        }
        require(template.placeInWorld(helper.getLevel(), helper.absolutePos(templatePos), helper.absolutePos(templatePos),
            new StructurePlaceSettings(), RandomSource.create(41L), 2), "Ancient City fixture did not place");

        helper.runAfterDelay(5, () -> {
            for (int row = 0; row < 3; row++) {
                for (int index = 0; index < buildingBlocks; index++) {
                    BlockPos pos = new BlockPos(2 + index, 4, 2 + row);
                    require(helper.getBlockState(pos).is(PROTECTED_DEEPSLATE.get(index)), "Unsupported Deepslate changed at " + pos);
                }
            }
            for (int index = 0; index < buildingBlocks; index++) {
                BlockPos pos = templatePos.offset(index, 0, 0);
                require(helper.getBlockState(pos).is(PROTECTED_DEEPSLATE.get(index)), "Ancient City Deepslate changed over air at " + pos);
                require(!helper.getBlockState(pos).is(BuiltInRegistries.BLOCK.get(hardened)), "Ancient City Deepslate hardened at " + pos);
            }
            AABB area = new AABB(helper.absolutePos(new BlockPos(1, 2, 1))).expandTowards(7, 5, 10);
            require(helper.getLevel().getEntitiesOfClass(TFCFallingBlockEntity.class, area).isEmpty(), "Deepslate spawned a falling block entity");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void noDeepslateLandslides(GameTestHelper helper) {
        require(!modResourceExists("data", "tfc", "tags", "block", "can_landslide.json"), "The mod supplies tfc:can_landslide data");
        require(modRecipeTypes().stream().noneMatch("tfc:landslide"::equals), "The mod supplies a TFC landslide recipe");
        for (Block block : PROTECTED_DEEPSLATE) {
            require(!block.defaultBlockState().is(TFCTags.Blocks.CAN_LANDSLIDE), block + " was added to can_landslide");
            require(!LandslideRecipe.canLandslide(block.defaultBlockState()), block + " resolved a landslide recipe");
        }

        for (int index = 1; index < PROTECTED_DEEPSLATE.size(); index++) {
            Block block = PROTECTED_DEEPSLATE.get(index);
            BlockPos pos = new BlockPos(2 + (index - 1) * 3, 5, 13);
            helper.setBlock(pos.below(), Blocks.AIR);
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                helper.setBlock(pos.relative(direction), Blocks.AIR);
                helper.setBlock(pos.relative(direction).below(), Blocks.STONE);
            }
            helper.setBlock(pos, block);
            require(helper.getBlockState(pos).is(block), block + " was not placed at " + pos);
            require(!LandslideRecipe.canLandslide(helper.getBlockState(pos)), block + " gained a landslide recipe after placement");
            LandslideRecipe.tryLandslide(helper.getLevel(), helper.absolutePos(pos), block.defaultBlockState());
            require(helper.getBlockState(pos).is(block), block + " moved during landslide check");
            require(helper.getLevel().getEntitiesOfClass(TFCFallingBlockEntity.class,
                new AABB(helper.absolutePos(pos)).inflate(2.0)).isEmpty(), block + " spawned a falling block entity");
        }
        helper.succeed();
    }

    private static Set<String> modRecipeTypes() {
        Path directory = modFile().findResource("data", TfcAncientCity.MOD_ID, "recipe");
        try (var files = Files.walk(directory)) {
            return files.filter(Files::isRegularFile).map(path -> {
                try (Reader reader = Files.newBufferedReader(path)) {
                    return JsonParser.parseReader(reader).getAsJsonObject().get("type").getAsString();
                } catch (IOException exception) {
                    throw new GameTestAssertException("Could not read recipe " + path.getFileName() + ": " + exception.getMessage());
                }
            }).collect(Collectors.toSet());
        } catch (IOException exception) {
            throw new GameTestAssertException("Could not inspect mod recipes: " + exception.getMessage());
        }
    }

    private static boolean modResourceExists(String... path) {
        return Files.isRegularFile(modFile().findResource(path));
    }

    private static JsonObject modJson(String... path) {
        try (Reader reader = Files.newBufferedReader(modFile().findResource(path))) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException exception) {
            throw new GameTestAssertException("Could not read mod JSON: " + exception.getMessage());
        }
    }

    private static IModFile modFile() {
        return ModList.get().getModFileById(TfcAncientCity.MOD_ID).getFile();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(TfcAncientCity.MOD_ID, path);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new GameTestAssertException(message);
        }
    }

    private DeepslateStabilityGameTests() {
    }
}
