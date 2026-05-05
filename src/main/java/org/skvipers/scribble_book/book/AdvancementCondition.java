package org.skvipers.scribble_book.book;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public record AdvancementCondition(Identifier advancement) implements UnlockCondition {

    public static final MapCodec<AdvancementCondition> MAP_CODEC = RecordCodecBuilder.mapCodec(i ->
            i.group(
                    Identifier.CODEC.fieldOf("advancement").forGetter(AdvancementCondition::advancement)
            ).apply(i, AdvancementCondition::new));

    @Override
    public String type() { return "advancement"; }

    @Override
    public boolean isMet(BookData bookData, ServerPlayer player) {
        var server = player.level().getServer();
        if (server == null) return false;
        AdvancementHolder holder = server.getAdvancements().get(advancement);
        if (holder == null) return false;
        return player.getAdvancements().getOrStartProgress(holder).isDone();
    }
}
