package net.claustra01.tfcancientcity.registry;

import net.claustra01.tfcancientcity.TfcAncientCity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TfcAncientCity.MOD_ID);

    public static final DeferredItem<BlockItem> LOOSE_DEEPSLATE = ITEMS.register(
        "rock/loose/deepslate",
        () -> new BlockItem(ModBlocks.LOOSE_DEEPSLATE.get(), new Item.Properties())
    );
    public static final DeferredItem<Item> DEEPSLATE_BRICK = ITEMS.registerSimpleItem("brick/deepslate");

    private ModItems() {
    }
}
