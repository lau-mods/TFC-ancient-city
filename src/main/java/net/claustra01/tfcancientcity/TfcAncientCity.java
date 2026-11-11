package net.claustra01.tfcancientcity;

import net.claustra01.tfcancientcity.loot.TfcLootReplacementModifier;
import net.claustra01.tfcancientcity.registry.ModBlocks;
import net.claustra01.tfcancientcity.registry.ModCreativeTabs;
import net.claustra01.tfcancientcity.registry.ModItems;
import net.claustra01.tfcancientcity.registry.ModStructureProcessors;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(TfcAncientCity.MOD_ID)
public final class TfcAncientCity {
    public static final String MOD_ID = "tfcancientcity";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TfcAncientCity(IEventBus modEventBus) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModStructureProcessors.STRUCTURE_PROCESSORS.register(modEventBus);
        TfcLootReplacementModifier.SERIALIZERS.register(modEventBus);
        modEventBus.addListener(ModCreativeTabs::addToTabs);
    }
}
