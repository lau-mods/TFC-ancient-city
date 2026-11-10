package net.claustra01.tfcancientcity.gametest;

import net.claustra01.tfcancientcity.TfcAncientCity;
import net.claustra01.tfcancientcity.registry.ModBlocks;
import net.claustra01.tfcancientcity.registry.ModItems;
import net.claustra01.tfcancientcity.registry.ModStructureProcessors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(TfcAncientCity.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ModRegistrationGameTests {
    @GameTest(template = "empty")
    public static void serverLoadsWithoutOverdims(GameTestHelper helper) {
        require(ModList.get().isLoaded(TfcAncientCity.MOD_ID), "TFC Ancient City was not loaded");
        require(ModList.get().isLoaded("tfc"), "TerraFirmaCraft was not loaded");
        require(!ModList.get().isLoaded("tfcoverdims"), "TFC Overdims must not be required or loaded");
        require(ModBlocks.LOOSE_DEEPSLATE.isBound(), "Loose Deepslate block was not registered");
        require(ModItems.LOOSE_DEEPSLATE.isBound(), "Loose Deepslate item was not registered");
        require(ModItems.DEEPSLATE_BRICK.isBound(), "Deepslate Brick was not registered");
        require(ModStructureProcessors.TFC_BLOCK_REPLACEMENT.isBound(), "replacement processor was not registered");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void metadataAndStableIds(GameTestHelper helper) {
        var modInfo = ModList.get().getModContainerById(TfcAncientCity.MOD_ID)
            .orElseThrow()
            .getModInfo();
        String version = modInfo.getVersion().toString();
        require(version.equals("1.0.0"), "Expected version 1.0.0, got " + version);
        var minecraftDependency = modInfo.getDependencies().stream()
            .filter(dependency -> dependency.getModId().equals("minecraft"))
            .findFirst()
            .orElseThrow();
        var minecraftRestrictions = minecraftDependency.getVersionRange().getRestrictions();
        require(minecraftRestrictions.size() == 1, "Minecraft dependency must have one exact restriction");
        var minecraftRestriction = minecraftRestrictions.getFirst();
        require(minecraftRestriction.getLowerBound().toString().equals("1.21.1")
            && minecraftRestriction.getUpperBound().toString().equals("1.21.1")
            && minecraftRestriction.isLowerBoundInclusive()
            && minecraftRestriction.isUpperBoundInclusive(), "Minecraft dependency must be exactly 1.21.1");
        var tfcDependency = modInfo.getDependencies().stream()
            .filter(dependency -> dependency.getModId().equals("tfc"))
            .findFirst()
            .orElseThrow();
        require(tfcDependency.getType() == IModInfo.DependencyType.REQUIRED, "TFC dependency was not required");
        require(tfcDependency.getOrdering() == IModInfo.Ordering.AFTER, "TFC dependency ordering was not AFTER");
        require(tfcDependency.getSide() == IModInfo.DependencySide.BOTH, "TFC dependency was not declared for both sides");
        require(modInfo.getDependencies().stream().noneMatch(dependency -> dependency.getModId().equals("tfcoverdims")),
            "TFC Overdims was declared as a dependency");
        require(BuiltInRegistries.BLOCK.containsKey(id("rock/loose/deepslate")), "Loose Deepslate block ID is missing");
        require(BuiltInRegistries.ITEM.containsKey(id("rock/loose/deepslate")), "Loose Deepslate item ID is missing");
        require(BuiltInRegistries.ITEM.containsKey(id("brick/deepslate")), "Deepslate Brick ID is missing");
        require(BuiltInRegistries.STRUCTURE_PROCESSOR.containsKey(id("tfc_block_replacement")), "processor ID is missing");
        helper.succeed();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(TfcAncientCity.MOD_ID, path);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new GameTestAssertException(message);
        }
    }

    private ModRegistrationGameTests() {
    }
}
