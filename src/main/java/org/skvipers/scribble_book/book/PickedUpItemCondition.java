package org.skvipers.scribble_book.book;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.Item;

public record PickedUpItemCondition(Identifier item, int count) implements UnlockCondition {

    public static final MapCodec<PickedUpItemCondition> MAP_CODEC = RecordCodecBuilder.mapCodec(i ->
            i.group(
                    Identifier.CODEC.fieldOf("item").forGetter(PickedUpItemCondition::item),
                    Codec.INT.optionalFieldOf("count", 1).forGetter(PickedUpItemCondition::count)
            ).apply(i, PickedUpItemCondition::new));

    @Override
    public String type() { return "picked_up_item"; }

    @Override
    public boolean isMet(BookData bookData, ServerPlayer player) {
        Item itemType = BuiltInRegistries.ITEM.getOptional(item).orElse(null);
        if (itemType == null) return false;
        return player.getStats().getValue(Stats.PICKED_UP.get(itemType)) >= count;
    }
}
