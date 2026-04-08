package org.skvipers.scribble_book.event;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.skvipers.scribble_book.ScribbleBook;
import org.skvipers.scribble_book.registry.ModItems;

public class SoulboundHandler {
    private static final String NBT_KEY = "scribble_book.soulbound_book";

    public static final ResourceKey<Enchantment> SOULBOUND = ResourceKey.create(
            Registries.ENCHANTMENT,
            Identifier.fromNamespaceAndPath(ScribbleBook.MODID, "soulbound"));

    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        var drops = event.getDrops();
        drops.removeIf(drop -> {
            ItemStack stack = drop.getItem();
            if (!stack.is(ModItems.SCRIBBLE_BOOK.get())) return false;

            var registry = player.level().registryAccess().lookup(Registries.ENCHANTMENT);
            if (registry.isEmpty()) return false;

            var holder = registry.get().get(SOULBOUND);
            if (holder.isEmpty()) return false;

            if (EnchantmentHelper.getItemEnchantmentLevel(holder.get(), stack) < 1) return false;

            // Сохраняем книгу в persistentData игрока
            var ops = player.registryAccess().createSerializationContext(NbtOps.INSTANCE);
            ItemStack.CODEC.encodeStart(ops, stack).ifSuccess(tag ->
                    player.getPersistentData().put(NBT_KEY, tag));
            return true;
        });
    }

    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;

        Player original = event.getOriginal();
        Player newPlayer = (Player) event.getEntity();

        Tag tag = original.getPersistentData().get(NBT_KEY);
        if (tag == null) return;

        var ops = newPlayer.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        ItemStack.CODEC.parse(ops, tag).ifSuccess(stack -> {
            if (!stack.isEmpty()) {
                newPlayer.getInventory().add(stack);
            }
        });

        original.getPersistentData().remove(NBT_KEY);
    }
}
