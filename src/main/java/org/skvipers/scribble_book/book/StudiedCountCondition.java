package org.skvipers.scribble_book.book;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public record StudiedCountCondition(int count, Optional<String> namespace) implements UnlockCondition {

    public static final MapCodec<StudiedCountCondition> MAP_CODEC = RecordCodecBuilder.mapCodec(i ->
            i.group(
                    Codec.INT.fieldOf("count").forGetter(StudiedCountCondition::count),
                    Codec.STRING.optionalFieldOf("namespace").forGetter(StudiedCountCondition::namespace)
            ).apply(i, StudiedCountCondition::new));

    @Override
    public String type() { return "studied_count"; }

    @Override
    public boolean isMet(BookData bookData, ServerPlayer player) {
        long studied = bookData.entries().keySet().stream()
                .filter(id -> namespace.isEmpty() || id.getNamespace().equals(namespace.get()))
                .count();
        return studied >= count;
    }
}
