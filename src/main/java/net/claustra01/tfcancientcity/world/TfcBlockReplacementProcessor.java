package net.claustra01.tfcancientcity.world;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import net.claustra01.tfcancientcity.TfcAncientCity;
import net.claustra01.tfcancientcity.registry.ModStructureProcessors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public final class TfcBlockReplacementProcessor extends StructureProcessor {
    public static final TfcBlockReplacementProcessor INSTANCE = new TfcBlockReplacementProcessor(
        BuiltInRegistries.BLOCK::getOptional,
        id -> TfcAncientCity.LOGGER.warn("Missing target block '{}'; leaving the vanilla block unchanged", id)
    );
    public static final MapCodec<TfcBlockReplacementProcessor> CODEC = MapCodec.unit(INSTANCE);

    private static final ResourceLocation TFC_FIREPIT = tfc("firepit");
    private static final Map<Block, Replacement> REPLACEMENTS = Map.ofEntries(
        replacement(Blocks.DARK_OAK_LOG, "wood/log/blackwood"),
        replacement(Blocks.DARK_OAK_PLANKS, "wood/planks/blackwood"),
        replacement(Blocks.DARK_OAK_SLAB, "wood/planks/blackwood_slab"),
        replacement(Blocks.DARK_OAK_STAIRS, "wood/planks/blackwood_stairs"),
        replacement(Blocks.DARK_OAK_FENCE, "wood/fence/blackwood"),
        replacement(Blocks.DARK_OAK_FENCE_GATE, "wood/fence_gate/blackwood"),
        replacement(Blocks.DARK_OAK_DOOR, "wood/door/blackwood"),
        replacement(Blocks.DARK_OAK_TRAPDOOR, "wood/trapdoor/blackwood"),
        replacement(Blocks.DARK_OAK_BUTTON, "wood/button/blackwood"),
        replacement(Blocks.DARK_OAK_PRESSURE_PLATE, "wood/pressure_plate/blackwood"),
        replacement(Blocks.CHEST, "wood/chest/blackwood"),
        replacement(Blocks.TRAPPED_CHEST, "wood/trapped_chest/blackwood"),
        replacement(Blocks.LECTERN, "wood/lectern/blackwood"),
        replacement(Blocks.CANDLE, "candle"),
        replacement(Blocks.WHITE_CANDLE, "candle/white"),
        replacement(Blocks.TORCH, "torch"),
        replacement(Blocks.WALL_TORCH, "wall_torch"),
        replacementWithoutNbt(Blocks.FURNACE, "firepit"),
        replacementWithoutNbt(Blocks.SMOKER, "firepit"),
        replacementWithoutNbt(Blocks.BLAST_FURNACE, "firepit"),
        replacement(Blocks.IRON_TRAPDOOR, "metal/trapdoor/wrought_iron")
    );

    private final Function<ResourceLocation, Optional<Block>> blockResolver;
    private final Consumer<ResourceLocation> missingBlockWarning;
    private final Set<ResourceLocation> warnedMissingBlocks = ConcurrentHashMap.newKeySet();

    private TfcBlockReplacementProcessor(
        Function<ResourceLocation, Optional<Block>> blockResolver,
        Consumer<ResourceLocation> missingBlockWarning
    ) {
        this.blockResolver = blockResolver;
        this.missingBlockWarning = missingBlockWarning;
    }

    public static TfcBlockReplacementProcessor createForTesting(
        Function<ResourceLocation, Optional<Block>> blockResolver,
        Consumer<ResourceLocation> missingBlockWarning
    ) {
        return new TfcBlockReplacementProcessor(blockResolver, missingBlockWarning);
    }

    @Override
    public List<StructureTemplate.StructureBlockInfo> finalizeProcessing(
        ServerLevelAccessor level,
        BlockPos offset,
        BlockPos pos,
        List<StructureTemplate.StructureBlockInfo> originalBlockInfos,
        List<StructureTemplate.StructureBlockInfo> processedBlockInfos,
        StructurePlaceSettings settings
    ) {
        List<StructureTemplate.StructureBlockInfo> replacedBlockInfos = new ArrayList<>(processedBlockInfos.size());
        for (StructureTemplate.StructureBlockInfo blockInfo : processedBlockInfos) {
            Replacement replacement = REPLACEMENTS.get(blockInfo.state().getBlock());
            if (replacement == null) {
                replacedBlockInfos.add(blockInfo);
                continue;
            }

            Optional<Block> targetBlock = blockResolver.apply(replacement.target());
            if (targetBlock.isEmpty()) {
                if (warnedMissingBlocks.add(replacement.target())) {
                    missingBlockWarning.accept(replacement.target());
                }
                replacedBlockInfos.add(blockInfo);
                continue;
            }

            BlockState replacementState = copyPropertiesByName(blockInfo.state(), targetBlock.get().defaultBlockState());
            if (replacement.target().equals(TFC_FIREPIT)) {
                replacementState = replacementState.setValue(BlockStateProperties.HORIZONTAL_AXIS,
                    blockInfo.state().getValue(BlockStateProperties.HORIZONTAL_FACING).getAxis());
            }
            replacedBlockInfos.add(new StructureTemplate.StructureBlockInfo(
                blockInfo.pos(),
                replacementState,
                replacement.clearNbt() ? null : blockInfo.nbt()
            ));
        }
        return replacedBlockInfos;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return ModStructureProcessors.TFC_BLOCK_REPLACEMENT.get();
    }

    private static ResourceLocation tfc(String path) {
        return ResourceLocation.fromNamespaceAndPath("tfc", path);
    }

    private static Map.Entry<Block, Replacement> replacement(Block source, String targetPath) {
        return Map.entry(source, new Replacement(tfc(targetPath), false));
    }

    private static Map.Entry<Block, Replacement> replacementWithoutNbt(Block source, String targetPath) {
        return Map.entry(source, new Replacement(tfc(targetPath), true));
    }

    private static BlockState copyPropertiesByName(BlockState source, BlockState target) {
        StateDefinition<Block, BlockState> targetDefinition = target.getBlock().getStateDefinition();
        for (Property<?> sourceProperty : source.getProperties()) {
            Property<?> targetProperty = targetDefinition.getProperty(sourceProperty.getName());
            if (targetProperty != null) {
                target = copyProperty(source, sourceProperty, target, targetProperty);
            }
        }
        return target;
    }

    private static <T extends Comparable<T>> BlockState copyProperty(
        BlockState source,
        Property<T> sourceProperty,
        BlockState target,
        Property<?> targetProperty
    ) {
        T value = source.getValue(sourceProperty);
        Optional<?> targetValue = targetProperty.getValue(value.toString());
        if (targetValue.isPresent()) {
            return setProperty(target, targetProperty, targetValue.get());
        }
        return target;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BlockState setProperty(BlockState state, Property property, Object value) {
        return state.setValue(property, (Comparable)value);
    }

    private record Replacement(ResourceLocation target, boolean clearNbt) {
    }
}
