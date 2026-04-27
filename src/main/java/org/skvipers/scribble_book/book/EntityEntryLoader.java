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

public class EntityEntryLoader extends SimpleJsonResourceReloadListener {
    public static final EntityEntryLoader INSTANCE = new EntityEntryLoader();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().create();

    private Map<ResourceLocation, BookEntry> entries = Map.of();

    private EntityEntryLoader() {
        super(GSON, "scribble_book/entities");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, BookEntry> result = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> e : objects.entrySet()) {
            BookEntry.CODEC.parse(JsonOps.INSTANCE, e.getValue())
                    .resultOrPartial(err -> LOGGER.error("Failed to parse entity entry {}: {}", e.getKey(), err))
                    .ifPresent(entry -> result.put(e.getKey(), entry));
        }
        entries = Collections.unmodifiableMap(result);
        LOGGER.info("Loaded {} scribble book entity entries", entries.size());
    }

    public BookEntry getEntry(ResourceLocation entityTypeId) {
        return entries.get(entityTypeId);
    }

    public Map<ResourceLocation, BookEntry> getAllEntries() {
        return entries;
    }
}
