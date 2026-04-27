package org.skvipers.scribble_book.event;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import org.skvipers.scribble_book.registry.ModEnchantments;
import org.skvipers.scribble_book.registry.ModItems;

public class SoulboundHandler {
    private static final String NBT_KEY = "scribble_book.soulbound_book";

    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        event.getDrops().removeIf(drop -> {
            ItemStack stack = drop.getItem();
            if (!stack.is(ModItems.SCRIBBLE_BOOK.get())) return false;
            if (EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.SOULBOUND.get(), stack) < 1) return false;

            CompoundTag saved = new CompoundTag();
            stack.save(saved);
            player.getPersistentData().put(NBT_KEY, saved);
            return true;
        });
    }

    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;

        Player original  = event.getOriginal();
        Player newPlayer = event.getEntity();

        if (!original.getPersistentData().contains(NBT_KEY, 10)) return;

        ItemStack stack = ItemStack.of(original.getPersistentData().getCompound(NBT_KEY));
        if (!stack.isEmpty()) newPlayer.getInventory().add(stack);

        original.getPersistentData().remove(NBT_KEY);
    }
}
