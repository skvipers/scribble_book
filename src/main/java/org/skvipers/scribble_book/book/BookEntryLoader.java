package org.skvipers.scribble_book.book;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BookEntryLoader extends SimpleJsonResourceReloadListener {
    public static final BookEntryLoader INSTANCE = new BookEntryLoader();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().create();

    private Map<ResourceLocation, BookEntry> entries = Map.of();

    private BookEntryLoader() {
        super(GSON, "scribble_book/blocks");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, BookEntry> result = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> e : objects.entrySet()) {
            BookEntry.CODEC.parse(JsonOps.INSTANCE, e.getValue())
                    .resultOrPartial(err -> LOGGER.error("Failed to parse block entry {}: {}", e.getKey(), err))
                    .ifPresent(entry -> result.put(e.getKey(), entry));
        }
        entries = Collections.unmodifiableMap(result);
        LOGGER.info("Loaded {} scribble book entries", entries.size());
    }

    public BookEntry getEntry(ResourceLocation blockId) {
        return entries.get(blockId);
    }

    public Map<ResourceLocation, BookEntry> getAllEntries() {
        return entries;
    }
}
