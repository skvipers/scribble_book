package org.skvipers.scribble_book.book;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record BookEntry(String title, String basicText, String deepText) {
    public static final Codec<BookEntry> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("title").forGetter(BookEntry::title),
                    Codec.STRING.fieldOf("basic").forGetter(BookEntry::basicText),
                    Codec.STRING.optionalFieldOf("deep", "").forGetter(BookEntry::deepText)
            ).apply(instance, BookEntry::new));

    public boolean hasDeepText() {
        return deepText != null && !deepText.isBlank();
    }
}
