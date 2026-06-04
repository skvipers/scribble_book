package org.skvipers.scribble_book.book;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.List;

public record RecipeBlock(List<String> grid, Identifier output) implements ContentBlock {

    public static final MapCodec<RecipeBlock> MAP_CODEC = RecordCodecBuilder.mapCodec(i ->
            i.group(
                    Codec.STRING.listOf().fieldOf("grid").forGetter(RecipeBlock::grid),
                    Identifier.CODEC.fieldOf("output").forGetter(RecipeBlock::output)
            ).apply(i, RecipeBlock::new));

    @Override
    public String type() { return "recipe"; }
}
