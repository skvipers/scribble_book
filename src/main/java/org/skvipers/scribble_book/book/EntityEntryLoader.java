package org.skvipers.scribble_book.book;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EntityEntryLoader extends SimplePreparableReloadListener<Map<ResourceLocation, BookEntry>> {
    public static final EntityEntryLoader INSTANCE = new EntityEntryLoader();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final FileToIdConverter CONVERTER = FileToIdConverter.json("scribble_book/entities");

    private Map<ResourceLocation, BookEntry> entries = Map.of();

    private EntityEntryLoader() {}

    @Override
    protected Map<ResourceLocation, BookEntry> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, BookEntry> result = new HashMap<>();
        for (var e : CONVERTER.listMatchingResources(manager).entrySet()) {
            ResourceLocation fileKey = e.getKey();
            String path = fileKey.getPath();
            ResourceLocation id = new ResourceLocation(fileKey.getNamespace(),
                    path.substring("scribble_book/entities/".length(), path.length() - ".json".length()));
            try (var reader = new BufferedReader(
                    new InputStreamReader(e.getValue().open(), StandardCharsets.UTF_8))) {
                JsonElement json = GsonHelper.parse(reader);
                BookEntry.CODEC.parse(JsonOps.INSTANCE, json)
                        .resultOrPartial(err -> LOGGER.error("Failed to parse entity entry {}: {}", id, err))
                        .ifPresent(entry -> result.put(id, entry));
            } catch (Exception ex) {
                LOGGER.error("Failed to load entity entry {}: {}", id, ex.getMessage());
            }
        }
        return Collections.unmodifiableMap(result);
    }

    @Override
    protected void apply(Map<ResourceLocation, BookEntry> prepared,
                         ResourceManager manager, ProfilerFiller profiler) {
        entries = prepared;
        LOGGER.info("Loaded {} scribble book entity entries", entries.size());
    }

    public BookEntry getEntry(ResourceLocation entityTypeId) { return entries.get(entityTypeId); }
    public Map<ResourceLocation, BookEntry> getAllEntries() { return entries; }
}
