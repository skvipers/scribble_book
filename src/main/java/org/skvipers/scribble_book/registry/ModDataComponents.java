package org.skvipers.scribble_book.registry;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.skvipers.scribble_book.ScribbleBook;
import org.skvipers.scribble_book.book.BookData;

public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, ScribbleBook.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BookData>> BOOK_DATA =
            DATA_COMPONENTS.register("book_data", () -> DataComponentType.<BookData>builder()
                    .persistent(BookData.CODEC)
                    .build());
}
