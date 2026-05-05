package org.skvipers.scribble_book.book;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface UnlockCondition {

    String type();

    boolean isMet(BookData bookData, ServerPlayer player);

    Map<String, MapCodec<? extends UnlockCondition>> REGISTRY = new ConcurrentHashMap<>();

    Codec<UnlockCondition> CODEC = Codec.STRING.dispatch(
            "type",
            UnlockCondition::type,
            key -> {
                MapCodec<? extends UnlockCondition> codec = REGISTRY.get(key);
                if (codec == null) throw new IllegalArgumentException("Unknown unlock condition type: " + key);
                return codec;
            });
}
