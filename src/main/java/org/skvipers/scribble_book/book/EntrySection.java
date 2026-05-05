package org.skvipers.scribble_book.book;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record EntrySection(KnowledgeLevel level, List<ContentBlock> blocks) {

    public static final Codec<EntrySection> CODEC = RecordCodecBuilder.create(i ->
            i.group(
                    KnowledgeLevel.CODEC.fieldOf("level").forGetter(EntrySection::level),
                    ContentBlock.CODEC.listOf().fieldOf("blocks").forGetter(EntrySection::blocks)
            ).apply(i, EntrySection::new));
}
