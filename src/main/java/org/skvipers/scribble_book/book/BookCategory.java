package org.skvipers.scribble_book.book;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.List;

public record BookCategory(String title, Identifier icon, int sortOrder, List<Identifier> entries) {

    public static final Codec<BookCategory> CODEC = RecordCodecBuilder.create(i ->
            i.group(
                    Codec.STRING.fieldOf("title").forGetter(BookCategory::title),
                    Identifier.CODEC.fieldOf("icon").forGetter(BookCategory::icon),
                    Codec.INT.optionalFieldOf("sort_order", 0).forGetter(BookCategory::sortOrder),
                    Identifier.CODEC.listOf().optionalFieldOf("entries", List.of()).forGetter(BookCategory::entries)
            ).apply(i, BookCategory::new));
}
