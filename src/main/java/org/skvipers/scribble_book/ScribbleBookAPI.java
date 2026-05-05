package org.skvipers.scribble_book;

import com.mojang.serialization.MapCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.skvipers.scribble_book.book.AdvancementCondition;
import org.skvipers.scribble_book.book.AllOfCondition;
import org.skvipers.scribble_book.book.AnyOfCondition;
import org.skvipers.scribble_book.book.BookData;
import org.skvipers.scribble_book.book.KilledEntityCondition;
import org.skvipers.scribble_book.book.NotCondition;
import org.skvipers.scribble_book.book.PickedUpItemCondition;
import org.skvipers.scribble_book.book.ScoreboardCondition;
import org.skvipers.scribble_book.book.StudiedCondition;
import org.skvipers.scribble_book.book.StudiedCountCondition;
import org.skvipers.scribble_book.book.UnlockCondition;
import org.skvipers.scribble_book.item.ScribbleBookItem;
import org.skvipers.scribble_book.registry.ModDataComponents;

public final class ScribbleBookAPI {

    private ScribbleBookAPI() {}

    /**
     * Register a custom unlock condition type for use in datapacks.
     * Call this during mod initialization, before datapacks are loaded.
     *
     * Example:
     * <pre>{@code
     * ScribbleBookAPI.registerConditionType("mymod:kills", MyKillsCondition.MAP_CODEC);
     * }</pre>
     */
    public static void registerConditionType(String type, MapCodec<? extends UnlockCondition> codec) {
        UnlockCondition.REGISTRY.put(type, codec);
    }

    /**
     * Re-evaluate unlock conditions for a specific book stack held by a player.
     * Call this after any external event that may satisfy an unlock condition.
     */
    public static void reEvaluateBookUnlocks(ServerPlayer player, ItemStack bookStack) {
        if (!(bookStack.getItem() instanceof ScribbleBookItem)) return;
        BookData data = bookStack.getOrDefault(ModDataComponents.BOOK_DATA.get(), BookData.EMPTY);
        BookData evaluated = ScribbleBookItem.reEvaluateUnlocks(player, data);
        bookStack.set(ModDataComponents.BOOK_DATA.get(), evaluated);
    }

    /**
     * Re-evaluate unlock conditions for all Scribble Books in a player's inventory.
     * Call this after any external event that may satisfy an unlock condition.
     */
    public static void reEvaluateAllBooks(ServerPlayer player) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) reEvaluateBookUnlocks(player, stack);
        }
    }

    static void registerBuiltins() {
        registerConditionType("studied",       StudiedCondition.MAP_CODEC);
        registerConditionType("studied_count", StudiedCountCondition.MAP_CODEC);
        registerConditionType("advancement",   AdvancementCondition.MAP_CODEC);
        registerConditionType("killed_entity", KilledEntityCondition.MAP_CODEC);
        registerConditionType("picked_up_item",PickedUpItemCondition.MAP_CODEC);
        registerConditionType("scoreboard",    ScoreboardCondition.MAP_CODEC);
        registerConditionType("all_of",        AllOfCondition.MAP_CODEC);
        registerConditionType("any_of",        AnyOfCondition.MAP_CODEC);
        registerConditionType("not",           NotCondition.MAP_CODEC);
    }
}
