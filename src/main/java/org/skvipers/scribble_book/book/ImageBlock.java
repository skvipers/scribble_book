package org.skvipers.scribble_book.book;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

public record ImageBlock(Identifier texture, int width, int height) implements ContentBlock {

    public static final MapCodec<ImageBlock> MAP_CODEC = RecordCodecBuilder.mapCodec(i ->
            i.group(
                    Identifier.CODEC.fieldOf("texture").forGetter(ImageBlock::texture),
                    Codec.INT.optionalFieldOf("width", 0).forGetter(ImageBlock::width),
                    Codec.INT.optionalFieldOf("height", 0).forGetter(ImageBlock::height)
            ).apply(i, ImageBlock::new));

    @Override
    public String type() { return "image"; }
}
