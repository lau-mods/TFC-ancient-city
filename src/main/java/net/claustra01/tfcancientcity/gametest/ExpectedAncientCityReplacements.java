package net.claustra01.tfcancientcity.gametest;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

final class ExpectedAncientCityReplacements {
    static final Map<Block, ResourceLocation> BLOCKS = create();

    private static Map<Block, ResourceLocation> create() {
        Map<Block, ResourceLocation> blocks = new LinkedHashMap<>();
        blocks.put(Blocks.DARK_OAK_LOG, tfc("wood/log/blackwood"));
        blocks.put(Blocks.DARK_OAK_PLANKS, tfc("wood/planks/blackwood"));
        blocks.put(Blocks.DARK_OAK_SLAB, tfc("wood/planks/blackwood_slab"));
        blocks.put(Blocks.DARK_OAK_STAIRS, tfc("wood/planks/blackwood_stairs"));
        blocks.put(Blocks.DARK_OAK_FENCE, tfc("wood/fence/blackwood"));
        blocks.put(Blocks.DARK_OAK_FENCE_GATE, tfc("wood/fence_gate/blackwood"));
        blocks.put(Blocks.DARK_OAK_DOOR, tfc("wood/door/blackwood"));
        blocks.put(Blocks.DARK_OAK_TRAPDOOR, tfc("wood/trapdoor/blackwood"));
        blocks.put(Blocks.DARK_OAK_BUTTON, tfc("wood/button/blackwood"));
        blocks.put(Blocks.DARK_OAK_PRESSURE_PLATE, tfc("wood/pressure_plate/blackwood"));
        blocks.put(Blocks.CHEST, tfc("wood/chest/blackwood"));
        blocks.put(Blocks.TRAPPED_CHEST, tfc("wood/trapped_chest/blackwood"));
        blocks.put(Blocks.LECTERN, tfc("wood/lectern/blackwood"));
        blocks.put(Blocks.CANDLE, tfc("candle"));
        blocks.put(Blocks.WHITE_CANDLE, tfc("candle/white"));
        blocks.put(Blocks.TORCH, tfc("torch"));
        blocks.put(Blocks.WALL_TORCH, tfc("wall_torch"));
        blocks.put(Blocks.FURNACE, tfc("firepit"));
        blocks.put(Blocks.SMOKER, tfc("firepit"));
        blocks.put(Blocks.BLAST_FURNACE, tfc("firepit"));
        blocks.put(Blocks.IRON_TRAPDOOR, tfc("metal/trapdoor/wrought_iron"));
        return Collections.unmodifiableMap(blocks);
    }

    private static ResourceLocation tfc(String path) {
        return ResourceLocation.fromNamespaceAndPath("tfc", path);
    }

    private ExpectedAncientCityReplacements() {
    }
}
