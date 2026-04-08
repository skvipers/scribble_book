package org.skvipers.scribble_book.book;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public record BookData(Map<Identifier, KnowledgeLevel> entries) {
    public static final BookData EMPTY = new BookData(Map.of());

    public static final Codec<BookData> CODEC = Codec.unboundedMap(Identifier.CODEC, KnowledgeLevel.CODEC)
            .xmap(BookData::new, BookData::entries);

    public KnowledgeLevel getLevel(Identifier id) {
        return entries.get(id);
    }

    public boolean hasEntry(Identifier id) {
        return entries.containsKey(id);
    }

    public BookData withEntry(Identifier id, KnowledgeLevel level) {
        Map<Identifier, KnowledgeLevel> updated = new HashMap<>(entries);
        updated.put(id, level);
        return new BookData(Collections.unmodifiableMap(updated));
    }
}
