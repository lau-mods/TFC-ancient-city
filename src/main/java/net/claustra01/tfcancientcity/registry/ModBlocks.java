package net.claustra01.tfcancientcity.registry;

import net.claustra01.tfcancientcity.TfcAncientCity;
import net.claustra01.tfcancientcity.block.LooseDeepslateBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TfcAncientCity.MOD_ID);

    public static final DeferredBlock<LooseDeepslateBlock> LOOSE_DEEPSLATE = BLOCKS.register(
        "rock/loose/deepslate",
        () -> new LooseDeepslateBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.DEEPSLATE)
            .strength(0.1F)
            .sound(SoundType.DEEPSLATE)
            .noCollission())
    );

    private ModBlocks() {
    }
}
