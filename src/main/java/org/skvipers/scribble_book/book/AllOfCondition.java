package org.skvipers.scribble_book.book;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public record AllOfCondition(List<UnlockCondition> conditions) implements UnlockCondition {

    public static final MapCodec<AllOfCondition> MAP_CODEC = RecordCodecBuilder.mapCodec(i ->
            i.group(
                    UnlockCondition.CODEC.listOf().fieldOf("conditions").forGetter(AllOfCondition::conditions)
            ).apply(i, AllOfCondition::new));

    @Override
    public String type() { return "all_of"; }

    @Override
    public boolean isMet(BookData bookData, ServerPlayer player) {
        return conditions.stream().allMatch(c -> c.isMet(bookData, player));
    }
}
