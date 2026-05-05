package org.skvipers.scribble_book.book;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntityType;

public record KilledEntityCondition(Identifier entity, int count) implements UnlockCondition {

    public static final MapCodec<KilledEntityCondition> MAP_CODEC = RecordCodecBuilder.mapCodec(i ->
            i.group(
                    Identifier.CODEC.fieldOf("entity").forGetter(KilledEntityCondition::entity),
                    Codec.INT.optionalFieldOf("count", 1).forGetter(KilledEntityCondition::count)
            ).apply(i, KilledEntityCondition::new));

    @Override
    public String type() { return "killed_entity"; }

    @Override
    public boolean isMet(BookData bookData, ServerPlayer player) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(entity).orElse(null);
        if (type == null) return false;
        return player.getStats().getValue(Stats.ENTITY_KILLED.get(type)) >= count;
    }
}
