package org.skvipers.scribble_book.registry;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.skvipers.scribble_book.ScribbleBook;
import org.skvipers.scribble_book.item.InkItem;
import org.skvipers.scribble_book.item.ScribbleBookItem;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ScribbleBook.MODID);

    public static final RegistryObject<ScribbleBookItem> SCRIBBLE_BOOK = ITEMS.register(
            "scribble_book", () -> new ScribbleBookItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<InkItem> INK = ITEMS.register(
            "ink", () -> new InkItem(new Item.Properties().durability(100)));

    public static final RegistryObject<Item> INK_BOTTLE = ITEMS.register(
            "ink_bottle", () -> new Item(new Item.Properties()));
}
