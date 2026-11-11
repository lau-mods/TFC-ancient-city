package net.claustra01.tfcancientcity.registry;

import net.dries007.tfc.common.TFCCreativeTabs;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/** Adds this mod's items to the TFC creative tabs that list the equivalent TFC rock items. */
public final class ModCreativeTabs {
    public static void addToTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(TFCCreativeTabs.TFC_NATURAL_BLOCKS.tab().getKey())) {
            event.accept(ModItems.LOOSE_DEEPSLATE);
        } else if (event.getTabKey().equals(TFCCreativeTabs.TFC_METALS_INGREDIENTS.tab().getKey())) {
            event.accept(ModItems.DEEPSLATE_BRICK);
        }
    }

    private ModCreativeTabs() {
    }
}
