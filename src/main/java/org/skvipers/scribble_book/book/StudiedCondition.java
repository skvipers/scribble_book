package org.skvipers.scribble_book.book;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public record StudiedCondition(Identifier entry, KnowledgeLevel level) implements UnlockCondition {

    public static final MapCodec<StudiedCondition> MAP_CODEC = RecordCodecBuilder.mapCodec(i ->
            i.group(
                    Identifier.CODEC.fieldOf("entry").forGetter(StudiedCondition::entry),
                    KnowledgeLevel.CODEC.optionalFieldOf("level", KnowledgeLevel.BASIC).forGetter(StudiedCondition::level)
            ).apply(i, StudiedCondition::new));

    @Override
    public String type() { return "studied"; }

    @Override
    public boolean isMet(BookData bookData, ServerPlayer player) {
        KnowledgeLevel actual = bookData.getLevel(entry);
        return switch (level) {
            case BASIC -> actual != null;
            case DEEP  -> actual == KnowledgeLevel.DEEP;
        };
    }
}
