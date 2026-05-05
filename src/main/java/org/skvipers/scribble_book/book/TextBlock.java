package org.skvipers.scribble_book.book;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record TextBlock(String text) implements ContentBlock {

    public static final MapCodec<TextBlock> MAP_CODEC = RecordCodecBuilder.mapCodec(i ->
            i.group(com.mojang.serialization.Codec.STRING.fieldOf("text").forGetter(TextBlock::text))
             .apply(i, TextBlock::new));

    @Override
    public String type() { return "text"; }
}
