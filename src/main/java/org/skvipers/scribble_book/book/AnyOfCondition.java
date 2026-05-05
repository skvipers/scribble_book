package org.skvipers.scribble_book.book;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public record AnyOfCondition(List<UnlockCondition> conditions) implements UnlockCondition {

    public static final MapCodec<AnyOfCondition> MAP_CODEC = RecordCodecBuilder.mapCodec(i ->
            i.group(
                    UnlockCondition.CODEC.listOf().fieldOf("conditions").forGetter(AnyOfCondition::conditions)
            ).apply(i, AnyOfCondition::new));

    @Override
    public String type() { return "any_of"; }

    @Override
    public boolean isMet(BookData bookData, ServerPlayer player) {
        return conditions.stream().anyMatch(c -> c.isMet(bookData, player));
    }
}
