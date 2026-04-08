package org.skvipers.scribble_book.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import org.skvipers.scribble_book.Config;
import org.skvipers.scribble_book.book.BookData;
import org.skvipers.scribble_book.book.BookEntry;
import org.skvipers.scribble_book.book.BookEntryLoader;
import org.skvipers.scribble_book.book.KnowledgeLevel;
import org.skvipers.scribble_book.event.ScribbleBookStudyEvent;
import org.skvipers.scribble_book.registry.ModDataComponents;
import org.skvipers.scribble_book.registry.ModItems;

public class ScribbleBookItem extends Item {
    public ScribbleBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            net.minecraft.client.Minecraft.getInstance().setScreen(
                    new org.skvipers.scribble_book.client.screen.ScribbleBookScreen(player.getItemInHand(hand)));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) return InteractionResult.PASS;

        Level level = context.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());

        ItemStack bookStack = context.getItemInHand();
        BookData data = bookStack.getOrDefault(ModDataComponents.BOOK_DATA.get(), BookData.EMPTY);

        // Compute default costs based on what's known for the block ID.
        // Listeners may change the entryKey and/or costs in the event.
        KnowledgeLevel preliminaryLevel = data.getLevel(blockId);
        int inkCost   = (preliminaryLevel == null) ? Config.basicInkCost   : Config.deepInkCost;
        int paperCost = (preliminaryLevel == null) ? Config.basicPaperCost : Config.deepPaperCost;

        ScribbleBookStudyEvent studyEvent = new ScribbleBookStudyEvent(
                player, pos, state, blockId, inkCost, paperCost);
        NeoForge.EVENT_BUS.post(studyEvent);
        if (studyEvent.isCanceled()) return InteractionResult.FAIL;

        // Use the key from the event — may have been overridden by a listener
        Identifier entryKey = studyEvent.getEntryKey();
        inkCost   = studyEvent.getInkCost();
        paperCost = studyEvent.getPaperCost();

        // All checks below use entryKey, not blockId
        KnowledgeLevel currentLevel = data.getLevel(entryKey);
        if (currentLevel == KnowledgeLevel.DEEP) {
            player.sendSystemMessage(Component.translatable("scribble_book.already_studied"));
            return InteractionResult.FAIL;
        }

        BookEntry entry = BookEntryLoader.INSTANCE.getEntry(entryKey);
        if (entry == null) {
            player.sendSystemMessage(Component.translatable("scribble_book.no_entry"));
            return InteractionResult.FAIL;
        }

        KnowledgeLevel targetLevel = (currentLevel == null) ? KnowledgeLevel.BASIC : KnowledgeLevel.DEEP;

        if (targetLevel == KnowledgeLevel.DEEP && !entry.hasDeepText()) {
            player.sendSystemMessage(Component.translatable("scribble_book.already_studied"));
            return InteractionResult.FAIL;
        }

        if (!hasResources(player, inkCost, paperCost)) {
            player.sendSystemMessage(Component.translatable("scribble_book.no_resources"));
            return InteractionResult.FAIL;
        }

        consumeResources(player, inkCost, paperCost);
        bookStack.set(ModDataComponents.BOOK_DATA.get(), data.withEntry(entryKey, targetLevel));
        player.sendSystemMessage(Component.translatable("scribble_book.studied",
                Component.translatable(entry.title())));

        return InteractionResult.SUCCESS;
    }

    private boolean hasResources(Player player, int inkCost, int paperCost) {
        return countInkUnits(player) >= inkCost
                && countItem(player, Items.PAPER) >= paperCost;
    }

    private int countInkUnits(Player player) {
        int total = 0;
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(ModItems.INK.get())) {
                total += stack.getMaxDamage() - stack.getDamageValue();
            }
        }
        return total;
    }

    private int countItem(Player player, Item item) {
        int count = 0;
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }

    private void consumeResources(Player player, int inkCost, int paperCost) {
        consumeInkUnits(player, inkCost);
        removeItems(player, Items.PAPER, paperCost);
    }

    private void consumeInkUnits(Player player, int amount) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize() && amount > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.is(ModItems.INK.get())) continue;
            int available = stack.getMaxDamage() - stack.getDamageValue();
            int toConsume = Math.min(available, amount);
            int newDamage = stack.getDamageValue() + toConsume;
            if (newDamage >= stack.getMaxDamage()) {
                inventory.setItem(i, new ItemStack(ModItems.INK_BOTTLE.get()));
            } else {
                stack.setDamageValue(newDamage);
            }
            amount -= toConsume;
        }
    }

    private void removeItems(Player player, Item item, int toRemove) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize() && toRemove > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item)) {
                int removed = Math.min(stack.getCount(), toRemove);
                inventory.removeItem(i, removed);
                toRemove -= removed;
            }
        }
    }
}
