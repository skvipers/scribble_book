package org.skvipers.scribble_book.book;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Optional;

public record BookEntry(
        String title,
        List<EntrySection> sections,
        boolean sneakOnly,
        boolean countable,
        boolean alwaysVisible,
        Optional<UnlockCondition> unlock
) {

    private static final Codec<BookEntry> NEW_CODEC = RecordCodecBuilder.create(i ->
            i.group(
                    Codec.STRING.fieldOf("title").forGetter(BookEntry::title),
                    EntrySection.CODEC.listOf().fieldOf("sections").forGetter(BookEntry::sections),
                    Codec.BOOL.optionalFieldOf("sneak_only", true).forGetter(BookEntry::sneakOnly),
                    Codec.BOOL.optionalFieldOf("countable", true).forGetter(BookEntry::countable),
                    Codec.BOOL.optionalFieldOf("always_visible", false).forGetter(BookEntry::alwaysVisible),
                    UnlockCondition.CODEC.optionalFieldOf("unlock").forGetter(BookEntry::unlock)
            ).apply(i, BookEntry::new));

    private static final Codec<BookEntry> LEGACY_CODEC = RecordCodecBuilder.create(i ->
            i.group(
                    Codec.STRING.fieldOf("title").forGetter(BookEntry::title),
                    Codec.STRING.fieldOf("basic").forGetter(e -> e.getTextFor(KnowledgeLevel.BASIC)),
                    Codec.STRING.optionalFieldOf("deep", "").forGetter(e -> e.getTextFor(KnowledgeLevel.DEEP)),
                    Codec.BOOL.optionalFieldOf("sneak_only", true).forGetter(BookEntry::sneakOnly)
            ).apply(i, BookEntry::fromLegacy));

    public static final Codec<BookEntry> CODEC = Codec.withAlternative(NEW_CODEC, LEGACY_CODEC);

    private static BookEntry fromLegacy(String title, String basic, String deep, boolean sneakOnly) {
        List<EntrySection> sections = deep.isBlank()
                ? List.of(new EntrySection(KnowledgeLevel.BASIC, List.of(new TextBlock(basic))))
                : List.of(
                        new EntrySection(KnowledgeLevel.BASIC, List.of(new TextBlock(basic))),
                        new EntrySection(KnowledgeLevel.DEEP,  List.of(new TextBlock(deep))));
        return new BookEntry(title, sections, sneakOnly, true, false, Optional.empty());
    }

    public List<ContentBlock> getBlocks(KnowledgeLevel level) {
        for (EntrySection s : sections) {
            if (s.level() == level) return s.blocks();
        }
        return List.of();
    }

    private String getTextFor(KnowledgeLevel level) {
        List<ContentBlock> blocks = getBlocks(level);
        if (blocks.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (ContentBlock b : blocks) {
            if (b instanceof TextBlock t) sb.append(t.text());
        }
        return sb.toString();
    }

    public boolean hasDeepSection() {
        return sections.stream().anyMatch(s -> s.level() == KnowledgeLevel.DEEP && !s.blocks().isEmpty());
    }
}
