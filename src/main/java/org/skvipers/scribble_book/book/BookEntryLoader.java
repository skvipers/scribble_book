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

public class BookEntryLoader extends SimplePreparableReloadListener<Map<ResourceLocation, BookEntry>> {
    public static final BookEntryLoader INSTANCE = new BookEntryLoader();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final FileToIdConverter CONVERTER = FileToIdConverter.json("scribble_book/blocks");

    private Map<ResourceLocation, BookEntry> entries = Map.of();

    private BookEntryLoader() {}

    @Override
    protected Map<ResourceLocation, BookEntry> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, BookEntry> result = new HashMap<>();
        for (var e : CONVERTER.listMatchingResources(manager).entrySet()) {
            ResourceLocation fileKey = e.getKey();
            String path = fileKey.getPath();
            // strip "scribble_book/blocks/" prefix and ".json" suffix
            ResourceLocation id = new ResourceLocation(fileKey.getNamespace(),
                    path.substring("scribble_book/blocks/".length(), path.length() - ".json".length()));
            try (var reader = new BufferedReader(
                    new InputStreamReader(e.getValue().open(), StandardCharsets.UTF_8))) {
                JsonElement json = GsonHelper.parse(reader);
                BookEntry.CODEC.parse(JsonOps.INSTANCE, json)
                        .resultOrPartial(err -> LOGGER.error("Failed to parse block entry {}: {}", id, err))
                        .ifPresent(entry -> result.put(id, entry));
            } catch (Exception ex) {
                LOGGER.error("Failed to load block entry {}: {}", id, ex.getMessage());
            }
        }
        return Collections.unmodifiableMap(result);
    }

    @Override
    protected void apply(Map<ResourceLocation, BookEntry> prepared,
                         ResourceManager manager, ProfilerFiller profiler) {
        entries = prepared;
        LOGGER.info("Loaded {} scribble book entries", entries.size());
    }

    public BookEntry getEntry(ResourceLocation blockId) { return entries.get(blockId); }
    public Map<ResourceLocation, BookEntry> getAllEntries() { return entries; }
}
