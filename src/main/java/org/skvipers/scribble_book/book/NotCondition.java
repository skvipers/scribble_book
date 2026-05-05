package org.skvipers.scribble_book.book;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;

public record NotCondition(UnlockCondition condition) implements UnlockCondition {

    public static final MapCodec<NotCondition> MAP_CODEC = RecordCodecBuilder.mapCodec(i ->
            i.group(
                    UnlockCondition.CODEC.fieldOf("condition").forGetter(NotCondition::condition)
            ).apply(i, NotCondition::new));

    @Override
    public String type() { return "not"; }

    @Override
    public boolean isMet(BookData bookData, ServerPlayer player) {
        return !condition.isMet(bookData, player);
    }
}
