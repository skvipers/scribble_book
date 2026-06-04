package org.skvipers.scribble_book.book;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.List;

public record ItemsBlock(List<Identifier> items, boolean background, String align, int gap) implements ContentBlock {

    public static final MapCodec<ItemsBlock> MAP_CODEC = RecordCodecBuilder.mapCodec(i ->
            i.group(
                    Identifier.CODEC.listOf().fieldOf("items").forGetter(ItemsBlock::items),
                    Codec.BOOL.optionalFieldOf("background", true).forGetter(ItemsBlock::background),
                    Codec.STRING.optionalFieldOf("align", "center").forGetter(ItemsBlock::align),
                    Codec.INT.optionalFieldOf("gap", 2).forGetter(ItemsBlock::gap)
            ).apply(i, ItemsBlock::new));

    @Override
    public String type() { return "items"; }
}
