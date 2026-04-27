package org.skvipers.scribble_book;

import com.mojang.logging.LogUtils;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.skvipers.scribble_book.book.AliasLoader;
import org.skvipers.scribble_book.book.BookEntryLoader;
import org.skvipers.scribble_book.book.EntityEntryLoader;
import org.skvipers.scribble_book.command.ScribbleBookCommand;
import org.skvipers.scribble_book.event.SoulboundHandler;
import org.skvipers.scribble_book.item.ScribbleBookItem;
import org.skvipers.scribble_book.registry.ModEnchantments;
import org.skvipers.scribble_book.registry.ModItems;
import org.slf4j.Logger;

@Mod(ScribbleBook.MODID)
public class ScribbleBook {
    public static final String MODID = "scribble_book";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ScribbleBook() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.ITEMS.register(modEventBus);
        ModEnchantments.ENCHANTMENTS.register(modEventBus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        modEventBus.addListener(this::onBuildCreativeTab);
        MinecraftForge.EVENT_BUS.addListener(this::onAddReloadListeners);
        MinecraftForge.EVENT_BUS.addListener(SoulboundHandler::onLivingDrops);
        MinecraftForge.EVENT_BUS.addListener(SoulboundHandler::onPlayerClone);
        MinecraftForge.EVENT_BUS.addListener(ScribbleBookItem::onEntityInteract);
        MinecraftForge.EVENT_BUS.addListener(ScribbleBookCommand::register);
    }

    private void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.INK_BOTTLE.get());
            event.accept(ModItems.INK.get());
            event.accept(ModItems.SCRIBBLE_BOOK.get());
        }
    }

    private void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(BookEntryLoader.INSTANCE);
        event.addListener(EntityEntryLoader.INSTANCE);
        event.addListener(AliasLoader.INSTANCE);
    }
}
