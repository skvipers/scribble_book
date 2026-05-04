package org.skvipers.scribble_book.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.skvipers.scribble_book.Config;
import org.skvipers.scribble_book.book.AliasLoader;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerPlayer;
import org.skvipers.scribble_book.book.BookData;
import org.skvipers.scribble_book.book.BookEntry;
import org.skvipers.scribble_book.book.BookEntryLoader;
import org.skvipers.scribble_book.book.EntityEntryLoader;
import org.skvipers.scribble_book.book.ItemEntryLoader;
import org.skvipers.scribble_book.book.KnowledgeLevel;
import org.skvipers.scribble_book.event.ScribbleBookEntityStudyEvent;
import org.skvipers.scribble_book.event.ScribbleBookItemStudyEvent;
import org.skvipers.scribble_book.event.ScribbleBookStudyEvent;
import org.skvipers.scribble_book.registry.ModDataComponents;
import org.skvipers.scribble_book.registry.ModItems;

public class ScribbleBookItem extends Item {
    public ScribbleBookItem(Properties properties) {
        super(properties);
    }

    private static final double GROUND_STUDY_RANGE = 4.5;

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack other = hand == InteractionHand.MAIN_HAND
                ? player.getOffhandItem() : player.getMainHandItem();
        boolean hasSpyglass = other.is(Items.SPYGLASS);
        double range = hasSpyglass ? Config.spyglassRange : GROUND_STUDY_RANGE;
        boolean canStudy = hasSpyglass || !Config.requireSpyglass;

        if (level.isClientSide()) {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult)
                return InteractionResult.PASS;
            if (canStudy && findItemEntityInRay(player, level, range) != null)
                return InteractionResult.CONSUME;
            mc.setScreen(new org.skvipers.scribble_book.client.screen.ScribbleBookScreen(
                    player.getItemInHand(hand)));
            return InteractionResult.SUCCESS;
        }

        if (canStudy) {
            ItemEntity target = findItemEntityInRay(player, level, range);
            if (target != null)
                return doItemStudy(player, target, player.getItemInHand(hand));
        }
        return InteractionResult.SUCCESS;
    }

    public static ItemEntity findItemEntityInRay(Player player, Level level, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(range));
        List<ItemEntity> candidates = level.getEntitiesOfClass(
                ItemEntity.class, new AABB(eye, end).inflate(0.5), e -> !e.isRemoved());
        ItemEntity nearest = null;
        double nearestSq = Double.MAX_VALUE;
        for (ItemEntity candidate : candidates) {
            var hit = candidate.getBoundingBox().inflate(0.2).clip(eye, end);
            if (hit.isPresent()) {
                double d = eye.distanceToSqr(hit.get());
                if (d < nearestSq) { nearestSq = d; nearest = candidate; }
            }
        }
        return nearest;
    }

    public static InteractionResult doItemStudy(Player player, ItemEntity itemEntity, ItemStack bookStack) {
        return doItemStudy(player, itemEntity, bookStack, false);
    }

    public static InteractionResult doItemStudy(Player player, ItemEntity itemEntity, ItemStack bookStack, boolean silent) {
        Identifier itemId = AliasLoader.INSTANCE.resolve(
                BuiltInRegistries.ITEM.getKey(itemEntity.getItem().getItem()));
        BookEntry entry = ItemEntryLoader.INSTANCE.getEntry(itemId);
        if (entry == null) {
            if (!silent) player.sendSystemMessage(Component.translatable("scribble_book.no_entry"));
            return InteractionResult.CONSUME;
        }
        studyItemEntity(player, itemEntity, bookStack, itemId, entry, silent);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        Level level = context.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Identifier blockId = AliasLoader.INSTANCE.resolve(
                BuiltInRegistries.BLOCK.getKey(state.getBlock()));

        BookEntry blockEntry = BookEntryLoader.INSTANCE.getEntry(blockId);
        if (blockEntry != null) {
            if (blockEntry.sneakOnly() && !player.isShiftKeyDown()) return InteractionResult.PASS;
            return studyBlock(player, state, pos, blockId, context.getItemInHand(), blockEntry);
        }

        // No block entry — check for item entity near the clicked position
        ItemStack bookStack = context.getItemInHand();
        ItemStack other = context.getHand() == InteractionHand.MAIN_HAND
                ? player.getOffhandItem() : player.getMainHandItem();
        boolean hasSpyglass = other.is(Items.SPYGLASS);
        if (hasSpyglass || !Config.requireSpyglass) {
            double range = hasSpyglass ? Config.spyglassRange : GROUND_STUDY_RANGE;
            ItemEntity nearby = findItemEntityInRay(player, level, range);
            if (nearby != null)
                return doItemStudy(player, nearby, bookStack);
        }

        return InteractionResult.PASS;
    }

    private InteractionResult studyBlock(Player player, BlockState state, BlockPos pos,
                                         Identifier blockId, ItemStack bookStack, BookEntry entry) {
        BookData data = bookStack.getOrDefault(ModDataComponents.BOOK_DATA.get(), BookData.EMPTY);
        KnowledgeLevel preliminaryLevel = data.getLevel(blockId);
        int inkCost   = (preliminaryLevel == null) ? Config.basicInkCost   : Config.deepInkCost;
        int paperCost = (preliminaryLevel == null) ? Config.basicPaperCost : Config.deepPaperCost;

        ScribbleBookStudyEvent studyEvent = new ScribbleBookStudyEvent(
                player, pos, state, blockId, inkCost, paperCost);
        NeoForge.EVENT_BUS.post(studyEvent);
        if (studyEvent.isCanceled()) return InteractionResult.FAIL;

        return performStudy(player, bookStack, studyEvent.getEntryKey(),
                BookEntryLoader.INSTANCE.getEntry(studyEvent.getEntryKey()),
                studyEvent.getInkCost(), studyEvent.getPaperCost(), data);
    }

    // Called from ScribbleBook event subscriber — not an @Override
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        ItemStack bookStack = event.getItemStack();
        if (!(bookStack.getItem() instanceof ScribbleBookItem)) return;

        Player player = event.getEntity();
        Entity target = event.getTarget();
        Level level   = target.level();
        if (level.isClientSide()) return;

        if (target instanceof ItemEntity itemEntity) {
            handleItemEntityInteract(event, player, itemEntity, bookStack);
            return;
        }

        Identifier entityId = AliasLoader.INSTANCE.resolve(
                BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()));

        BookEntry entry = EntityEntryLoader.INSTANCE.getEntry(entityId);
        if (entry == null) return;

        if (entry.sneakOnly() && !player.isShiftKeyDown()) return;

        // Block vanilla interaction when book takes over
        event.setCanceled(true);

        BookData data = bookStack.getOrDefault(ModDataComponents.BOOK_DATA.get(), BookData.EMPTY);
        KnowledgeLevel preliminaryLevel = data.getLevel(entityId);
        int inkCost   = (preliminaryLevel == null) ? Config.basicInkCost   : Config.deepInkCost;
        int paperCost = (preliminaryLevel == null) ? Config.basicPaperCost : Config.deepPaperCost;

        ScribbleBookEntityStudyEvent studyEvent = new ScribbleBookEntityStudyEvent(
                player, target, entityId, inkCost, paperCost);
        NeoForge.EVENT_BUS.post(studyEvent);
        if (studyEvent.isCanceled()) return;

        Identifier finalKey = studyEvent.getEntryKey();
        BookEntry finalEntry = EntityEntryLoader.INSTANCE.getEntry(finalKey);
        performStudy(player, bookStack, finalKey, finalEntry,
                studyEvent.getInkCost(), studyEvent.getPaperCost(), data);
    }

    private static void handleItemEntityInteract(PlayerInteractEvent.EntityInteract event,
                                                  Player player, ItemEntity itemEntity,
                                                  ItemStack bookStack) {
        if (Config.requireSpyglass) {
            event.setCanceled(true);
            player.sendSystemMessage(Component.translatable("scribble_book.use_spyglass"));
            return;
        }
        Identifier itemId = AliasLoader.INSTANCE.resolve(
                BuiltInRegistries.ITEM.getKey(itemEntity.getItem().getItem()));
        BookEntry entry = ItemEntryLoader.INSTANCE.getEntry(itemId);
        if (entry == null) return;
        if (entry.sneakOnly() && !player.isShiftKeyDown()) return;
        event.setCanceled(true);
        studyItemEntity(player, itemEntity, bookStack, itemId, entry, false);
    }

    private static void studyItemEntity(Player player, ItemEntity itemEntity,
                                         ItemStack bookStack, Identifier itemId, BookEntry entry, boolean silent) {
        BookData data = bookStack.getOrDefault(ModDataComponents.BOOK_DATA.get(), BookData.EMPTY);
        KnowledgeLevel preliminaryLevel = data.getLevel(itemId);
        int inkCost   = (preliminaryLevel == null) ? Config.basicInkCost   : Config.deepInkCost;
        int paperCost = (preliminaryLevel == null) ? Config.basicPaperCost : Config.deepPaperCost;
        ScribbleBookItemStudyEvent studyEvent = new ScribbleBookItemStudyEvent(
                player, itemEntity, itemId, inkCost, paperCost);
        NeoForge.EVENT_BUS.post(studyEvent);
        if (studyEvent.isCanceled()) return;
        Identifier finalKey = studyEvent.getEntryKey();
        BookEntry finalEntry = ItemEntryLoader.INSTANCE.getEntry(finalKey);
        performStudy(player, bookStack, finalKey, finalEntry,
                studyEvent.getInkCost(), studyEvent.getPaperCost(), data, silent);
    }

    private static InteractionResult performStudy(Player player, ItemStack bookStack,
                                                   Identifier entryKey, BookEntry entry,
                                                   int inkCost, int paperCost, BookData data) {
        return performStudy(player, bookStack, entryKey, entry, inkCost, paperCost, data, false);
    }

    private static InteractionResult performStudy(Player player, ItemStack bookStack,
                                                   Identifier entryKey, BookEntry entry,
                                                   int inkCost, int paperCost, BookData data, boolean silent) {
        if (entry == null) {
            if (!silent) player.sendSystemMessage(Component.translatable("scribble_book.no_entry"));
            return InteractionResult.FAIL;
        }

        KnowledgeLevel currentLevel = data.getLevel(entryKey);
        if (currentLevel == KnowledgeLevel.DEEP) {
            if (!silent) player.sendSystemMessage(Component.translatable("scribble_book.already_studied"));
            return InteractionResult.FAIL;
        }

        KnowledgeLevel targetLevel = (currentLevel == null) ? KnowledgeLevel.BASIC : KnowledgeLevel.DEEP;

        if (targetLevel == KnowledgeLevel.DEEP && !entry.hasDeepText()) {
            if (!silent) player.sendSystemMessage(Component.translatable("scribble_book.already_studied"));
            return InteractionResult.FAIL;
        }

        if (!hasResources(player, inkCost, paperCost)) {
            if (!silent) player.sendSystemMessage(Component.translatable("scribble_book.no_resources"));
            return InteractionResult.FAIL;
        }

        consumeResources(player, inkCost, paperCost);
        BookData newData = data.withEntry(entryKey, targetLevel);
        bookStack.set(ModDataComponents.BOOK_DATA.get(), newData);
        player.sendSystemMessage(Component.translatable("scribble_book.studied",
                Component.translatable(entry.title())));

        if (player instanceof ServerPlayer sp) {
            var server = sp.level().getServer();
            int count = newData.entries().size();

            if (count >= 10) {
                AdvancementHolder h = server.getAdvancements().get(
                        Identifier.fromNamespaceAndPath("scribble_book", "thirst_for_knowledge"));
                if (h != null) sp.getAdvancements().award(h, "study_10");
            }

            if (entryKey.equals(Identifier.fromNamespaceAndPath("minecraft", "creeper"))
                    && targetLevel == KnowledgeLevel.DEEP) {
                AdvancementHolder h = server.getAdvancements().get(
                        Identifier.fromNamespaceAndPath("scribble_book", "explosive_story"));
                if (h != null) sp.getAdvancements().award(h, "study_creeper");
            }

            if (entryKey.equals(Identifier.fromNamespaceAndPath("minecraft", "warden"))
                    && targetLevel == KnowledgeLevel.DEEP) {
                AdvancementHolder h = server.getAdvancements().get(
                        Identifier.fromNamespaceAndPath("scribble_book", "dont_yell"));
                if (h != null) sp.getAdvancements().award(h, "study_guardian");
            }

            boolean allStudied = java.util.stream.Stream.concat(
                    java.util.stream.Stream.concat(
                            BookEntryLoader.INSTANCE.getAllEntries().entrySet().stream(),
                            EntityEntryLoader.INSTANCE.getAllEntries().entrySet().stream()
                    ),
                    ItemEntryLoader.INSTANCE.getAllEntries().entrySet().stream()
            ).allMatch(e -> {
                KnowledgeLevel lvl = newData.getLevel(e.getKey());
                return e.getValue().hasDeepText() ? lvl == KnowledgeLevel.DEEP : lvl != null;
            });
            if (allStudied) {
                AdvancementHolder h = server.getAdvancements().get(
                        Identifier.fromNamespaceAndPath("scribble_book", "forty_two"));
                if (h != null) sp.getAdvancements().award(h, "answer");
            }
        }

        return InteractionResult.SUCCESS;
    }

    private static boolean hasResources(Player player, int inkCost, int paperCost) {
        return countInkUnits(player) >= inkCost
                && countItem(player, Items.PAPER) >= paperCost;
    }

    private static int countInkUnits(Player player) {
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

    private static int countItem(Player player, Item item) {
        int count = 0;
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }

    private static void consumeResources(Player player, int inkCost, int paperCost) {
        consumeInkUnits(player, inkCost);
        removeItems(player, Items.PAPER, paperCost);
    }

    private static void consumeInkUnits(Player player, int amount) {
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

    private static void removeItems(Player player, Item item, int toRemove) {
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
