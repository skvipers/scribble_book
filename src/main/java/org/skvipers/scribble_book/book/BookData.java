package org.skvipers.scribble_book.book;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public record BookData(Map<ResourceLocation, KnowledgeLevel> entries) {
    public static final BookData EMPTY = new BookData(Map.of());
    private static final String NBT_KEY = "BookData";

    public static BookData fromStack(ItemStack stack) {
        if (!stack.hasTag()) return EMPTY;
        CompoundTag tag = stack.getTag();
        if (!tag.contains(NBT_KEY, 10)) return EMPTY;
        return fromNbt(tag.getCompound(NBT_KEY));
    }

    public static void toStack(ItemStack stack, BookData data) {
        stack.getOrCreateTag().put(NBT_KEY, data.toNbt());
    }

    private static BookData fromNbt(CompoundTag tag) {
        Map<ResourceLocation, KnowledgeLevel> map = new HashMap<>();
        for (String key : tag.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id == null) continue;
            String levelStr = tag.getString(key);
            try {
                map.put(id, KnowledgeLevel.valueOf(levelStr.toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }
        return new BookData(Collections.unmodifiableMap(map));
    }

    private CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        for (Map.Entry<ResourceLocation, KnowledgeLevel> e : entries.entrySet()) {
            tag.putString(e.getKey().toString(), e.getValue().getSerializedName());
        }
        return tag;
    }

    public KnowledgeLevel getLevel(ResourceLocation id) {
        return entries.get(id);
    }

    public boolean hasEntry(ResourceLocation id) {
        return entries.containsKey(id);
    }

    public BookData withEntry(ResourceLocation id, KnowledgeLevel level) {
        Map<ResourceLocation, KnowledgeLevel> updated = new HashMap<>(entries);
        updated.put(id, level);
        return new BookData(Collections.unmodifiableMap(updated));
    }
}
