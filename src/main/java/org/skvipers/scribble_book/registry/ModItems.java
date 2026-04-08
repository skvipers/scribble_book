package org.skvipers.scribble_book.registry;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.skvipers.scribble_book.ScribbleBook;
import org.skvipers.scribble_book.item.InkItem;
import org.skvipers.scribble_book.item.ScribbleBookItem;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ScribbleBook.MODID);

    public static final DeferredItem<ScribbleBookItem> SCRIBBLE_BOOK = ITEMS.registerItem(
            "scribble_book", ScribbleBookItem::new, props -> props.stacksTo(1));

    public static final DeferredItem<InkItem> INK = ITEMS.registerItem(
            "ink", InkItem::new, props -> props.durability(100));

    public static final DeferredItem<Item> INK_BOTTLE = ITEMS.registerSimpleItem("ink_bottle");
}
