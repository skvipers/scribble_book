package org.skvipers.scribble_book.book;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BookCategoryLoader extends SimpleJsonResourceReloadListener<BookCategory> {
    public static final BookCategoryLoader INSTANCE = new BookCategoryLoader();
    private static final Logger LOGGER = LogUtils.getLogger();

    private Map<Identifier, BookCategory> categories = Map.of();
    private List<Map.Entry<Identifier, BookCategory>> sorted = List.of();

    private BookCategoryLoader() {
        super(BookCategory.CODEC, FileToIdConverter.json("scribble_book/categories"));
    }

    @Override
    protected void apply(Map<Identifier, BookCategory> objects, ResourceManager manager, ProfilerFiller profiler) {
        categories = Collections.unmodifiableMap(new HashMap<>(objects));
        sorted = categories.entrySet().stream()
                .sorted(Comparator.comparingInt(e -> e.getValue().sortOrder()))
                .toList();
        LOGGER.info("Loaded {} scribble book categories", categories.size());
    }

    public Map<Identifier, BookCategory> getAllCategories() {
        return categories;
    }

    public List<Map.Entry<Identifier, BookCategory>> getSorted() {
        return sorted;
    }
}
