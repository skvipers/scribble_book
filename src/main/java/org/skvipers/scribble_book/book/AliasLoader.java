package org.skvipers.scribble_book.book;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
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

/**
 * Loads block and entity alias mappings from data/[namespace]/scribble_book/aliases/*.json.
 * Each file is a flat JSON object: { "namespace:variant": "namespace:canonical" }.
 * All files are merged into one map. Used to redirect variant IDs to a shared entry —
 * e.g. all shulker box colors → shulker_box, all boat wood types → oak_boat.
 */
public class AliasLoader extends SimplePreparableReloadListener<Map<Identifier, Identifier>> {
    public static final AliasLoader INSTANCE = new AliasLoader();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final FileToIdConverter CONVERTER = FileToIdConverter.json("scribble_book/aliases");

    private Map<Identifier, Identifier> aliases = Map.of();

    private AliasLoader() {}

    @Override
    protected Map<Identifier, Identifier> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, Identifier> merged = new HashMap<>();
        for (var entry : CONVERTER.listMatchingResources(manager).entrySet()) {
            try (var reader = new BufferedReader(
                    new InputStreamReader(entry.getValue().open(), StandardCharsets.UTF_8))) {
                JsonObject json = GsonHelper.parse(reader);
                for (var e : json.entrySet()) {
                    merged.put(Identifier.parse(e.getKey()),
                               Identifier.parse(e.getValue().getAsString()));
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load alias file {}: {}", entry.getKey(), e.getMessage());
            }
        }
        return merged;
    }

    @Override
    protected void apply(Map<Identifier, Identifier> prepared,
                         ResourceManager manager, ProfilerFiller profiler) {
        aliases = Map.copyOf(prepared);
        LOGGER.info("Loaded {} aliases", aliases.size());
    }

    /** Returns the canonical entry key for the given ID, or the ID itself if no alias exists. */
    public Identifier resolve(Identifier id) {
        return aliases.getOrDefault(id, id);
    }
}
