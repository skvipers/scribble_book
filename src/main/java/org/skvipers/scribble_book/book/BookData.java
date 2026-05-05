package org.skvipers.scribble_book.book;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record BookData(Map<Identifier, KnowledgeLevel> entries, Set<Identifier> unlockedEntries) {

    public static final BookData EMPTY = new BookData(Map.of(), Set.of());

    private static final Codec<BookData> NEW_CODEC = RecordCodecBuilder.create(i ->
            i.group(
                    Codec.unboundedMap(Identifier.CODEC, KnowledgeLevel.CODEC)
                            .fieldOf("entries").forGetter(BookData::entries),
                    Identifier.CODEC.listOf()
                            .xmap(l -> (Set<Identifier>) new HashSet<>(l), l -> List.copyOf(l))
                            .optionalFieldOf("unlocked", Set.of())
                            .forGetter(BookData::unlockedEntries)
            ).apply(i, BookData::new));

    private static final Codec<BookData> LEGACY_CODEC = Codec.unboundedMap(Identifier.CODEC, KnowledgeLevel.CODEC)
            .xmap(m -> new BookData(m, Set.of()), BookData::entries);

    public static final Codec<BookData> CODEC = Codec.withAlternative(NEW_CODEC, LEGACY_CODEC);

    public KnowledgeLevel getLevel(Identifier id) {
        return entries.get(id);
    }

    public boolean hasEntry(Identifier id) {
        return entries.containsKey(id);
    }

    public boolean isUnlocked(Identifier id) {
        return unlockedEntries.contains(id);
    }

    public BookData withEntry(Identifier id, KnowledgeLevel level) {
        Map<Identifier, KnowledgeLevel> updated = new HashMap<>(entries);
        updated.put(id, level);
        return new BookData(Collections.unmodifiableMap(updated), unlockedEntries);
    }

    public BookData withUnlocked(Set<Identifier> unlocked) {
        return new BookData(entries, Collections.unmodifiableSet(new HashSet<>(unlocked)));
    }
}
