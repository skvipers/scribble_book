package org.skvipers.scribble_book.book;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EntityEntryLoader extends SimpleJsonResourceReloadListener<BookEntry> {
    public static final EntityEntryLoader INSTANCE = new EntityEntryLoader();
    private static final Logger LOGGER = LogUtils.getLogger();

    private Map<Identifier, BookEntry> entries = Map.of();

    private EntityEntryLoader() {
        super(BookEntry.CODEC, FileToIdConverter.json("scribble_book/entities"));
    }

    @Override
    protected void apply(Map<Identifier, BookEntry> objects, ResourceManager manager, ProfilerFiller profiler) {
        entries = Collections.unmodifiableMap(new HashMap<>(objects));
        LOGGER.info("Loaded {} scribble book entity entries", entries.size());
    }

    public BookEntry getEntry(Identifier entityTypeId) {
        return entries.get(entityTypeId);
    }

    public Map<Identifier, BookEntry> getAllEntries() {
        return entries;
    }
}
