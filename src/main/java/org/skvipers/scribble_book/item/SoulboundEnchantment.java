package org.skvipers.scribble_book.item;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import org.skvipers.scribble_book.registry.ModItems;

public class SoulboundEnchantment extends Enchantment {
    public SoulboundEnchantment() {
        super(Rarity.RARE, EnchantmentCategory.VANISHABLE, new EquipmentSlot[0]);
    }

    @Override
    public int getMaxLevel() { return 1; }

    @Override
    public boolean isTreasureOnly() { return true; }

    @Override
    public boolean isTradeable() { return false; }

    @Override
    public boolean isDiscoverable() { return false; }

    @Override
    public boolean canEnchant(ItemStack stack) {
        return stack.is(ModItems.SCRIBBLE_BOOK.get());
    }
}
