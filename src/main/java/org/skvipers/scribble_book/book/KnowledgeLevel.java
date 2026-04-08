package org.skvipers.scribble_book.book;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum KnowledgeLevel implements StringRepresentable {
    BASIC("basic"),
    DEEP("deep");

    private final String name;

    KnowledgeLevel(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public static final Codec<KnowledgeLevel> CODEC = StringRepresentable.fromEnum(KnowledgeLevel::values);
}
