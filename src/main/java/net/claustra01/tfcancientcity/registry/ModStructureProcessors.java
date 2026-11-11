package net.claustra01.tfcancientcity.registry;

import net.claustra01.tfcancientcity.TfcAncientCity;
import net.claustra01.tfcancientcity.world.TfcBlockReplacementProcessor;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModStructureProcessors {
    public static final DeferredRegister<StructureProcessorType<?>> STRUCTURE_PROCESSORS = DeferredRegister.create(
        Registries.STRUCTURE_PROCESSOR,
        TfcAncientCity.MOD_ID
    );

    public static final DeferredHolder<StructureProcessorType<?>, StructureProcessorType<TfcBlockReplacementProcessor>> TFC_BLOCK_REPLACEMENT =
        STRUCTURE_PROCESSORS.register("tfc_block_replacement", () -> () -> TfcBlockReplacementProcessor.CODEC);

    private ModStructureProcessors() {
    }
}
