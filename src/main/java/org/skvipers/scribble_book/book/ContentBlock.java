package org.skvipers.scribble_book.book;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import java.util.Map;

public sealed interface ContentBlock permits TextBlock, ItemBlock, ImageBlock {

    String type();

    Map<String, MapCodec<? extends ContentBlock>> TYPES = Map.of(
            "text",  TextBlock.MAP_CODEC,
            "item",  ItemBlock.MAP_CODEC,
            "image", ImageBlock.MAP_CODEC
    );

    Codec<ContentBlock> CODEC = Codec.STRING
            .dispatch("type", ContentBlock::type, key -> {
                MapCodec<? extends ContentBlock> codec = TYPES.get(key);
                if (codec == null) throw new IllegalArgumentException("Unknown ContentBlock type: " + key);
                return codec;
            });
}
