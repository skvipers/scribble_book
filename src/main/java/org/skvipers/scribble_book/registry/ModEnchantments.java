package org.skvipers.scribble_book.registry;

import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.skvipers.scribble_book.ScribbleBook;
import org.skvipers.scribble_book.item.SoulboundEnchantment;

public class ModEnchantments {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, ScribbleBook.MODID);

    public static final RegistryObject<Enchantment> SOULBOUND =
            ENCHANTMENTS.register("soulbound", SoulboundEnchantment::new);
}
