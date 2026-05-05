package org.skvipers.scribble_book.book;

import com.mojang.serialization.Codec;
import net.minecraft.server.level.ServerPlayer;

public sealed interface UnlockCondition permits StudiedCondition, AdvancementCondition, AllOfCondition {

    String type();

    boolean isMet(BookData bookData, ServerPlayer player);

    Codec<UnlockCondition> CODEC = Codec.STRING.dispatch(
            "type",
            UnlockCondition::type,
            key -> switch (key) {
                case "studied"     -> StudiedCondition.MAP_CODEC;
                case "advancement" -> AdvancementCondition.MAP_CODEC;
                case "all_of"      -> AllOfCondition.MAP_CODEC;
                default -> throw new IllegalArgumentException("Unknown unlock condition type: " + key);
            });
}
