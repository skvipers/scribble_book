package org.skvipers.scribble_book.book;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

public record ItemBlock(Identifier item) implements ContentBlock {

    public static final MapCodec<ItemBlock> MAP_CODEC = RecordCodecBuilder.mapCodec(i ->
            i.group(Identifier.CODEC.fieldOf("item").forGetter(ItemBlock::item))
             .apply(i, ItemBlock::new));

    @Override
    public String type() { return "item"; }
}
