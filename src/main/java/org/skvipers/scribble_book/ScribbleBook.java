package org.skvipers.scribble_book;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.skvipers.scribble_book.book.BookEntryLoader;
import org.skvipers.scribble_book.event.SoulboundHandler;
import org.skvipers.scribble_book.registry.ModDataComponents;
import org.skvipers.scribble_book.registry.ModItems;
import org.slf4j.Logger;

@Mod(ScribbleBook.MODID)
public class ScribbleBook {
    public static final String MODID = "scribble_book";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ScribbleBook(IEventBus modEventBus, ModContainer modContainer) {
        ModItems.ITEMS.register(modEventBus);
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        modEventBus.addListener(this::onBuildCreativeTab);
        NeoForge.EVENT_BUS.addListener(this::onAddReloadListeners);
        NeoForge.EVENT_BUS.addListener(SoulboundHandler::onLivingDrops);
        NeoForge.EVENT_BUS.addListener(SoulboundHandler::onPlayerClone);
    }

    private void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.INK_BOTTLE);
            event.accept(ModItems.INK);
            event.accept(ModItems.SCRIBBLE_BOOK);
        }
    }

    private void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath(MODID, "book_entries"), BookEntryLoader.INSTANCE);
    }
}
