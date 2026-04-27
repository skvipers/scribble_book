package org.skvipers.scribble_book.book;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
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
import java.util.HashMap;
import java.util.Map;

public class AliasLoader extends SimplePreparableReloadListener<Map<ResourceLocation, ResourceLocation>> {
    public static final AliasLoader INSTANCE = new AliasLoader();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final FileToIdConverter CONVERTER = FileToIdConverter.json("scribble_book/aliases");

    private Map<ResourceLocation, ResourceLocation> aliases = Map.of();

    private AliasLoader() {}

    @Override
    protected Map<ResourceLocation, ResourceLocation> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, ResourceLocation> merged = new HashMap<>();
        for (var entry : CONVERTER.listMatchingResources(manager).entrySet()) {
            try (var reader = new BufferedReader(
                    new InputStreamReader(entry.getValue().open(), StandardCharsets.UTF_8))) {
                JsonObject json = GsonHelper.parse(reader);
                for (var e : json.entrySet()) {
                    ResourceLocation from = ResourceLocation.tryParse(e.getKey());
                    ResourceLocation to   = ResourceLocation.tryParse(e.getValue().getAsString());
                    if (from != null && to != null) merged.put(from, to);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load alias file {}: {}", entry.getKey(), e.getMessage());
            }
        }
        return merged;
    }

    @Override
    protected void apply(Map<ResourceLocation, ResourceLocation> prepared,
                         ResourceManager manager, ProfilerFiller profiler) {
        aliases = Map.copyOf(prepared);
        LOGGER.info("Loaded {} aliases", aliases.size());
    }

    public ResourceLocation resolve(ResourceLocation id) {
        return aliases.getOrDefault(id, id);
    }
}
