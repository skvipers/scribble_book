package org.skvipers.scribble_book;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.skvipers.scribble_book.client.ClientNetworkHandler;
import org.skvipers.scribble_book.client.SpyglassScanHandler;
import org.skvipers.scribble_book.network.ClientboundOpenBookPacket;
import org.skvipers.scribble_book.network.ServerboundStudyItemPacket;
import org.skvipers.scribble_book.book.AliasLoader;
import org.skvipers.scribble_book.book.BookCategoryLoader;
import org.skvipers.scribble_book.book.CustomEntryLoader;
import org.skvipers.scribble_book.command.ScribbleBookCommand;
import org.skvipers.scribble_book.book.BookEntryLoader;
import org.skvipers.scribble_book.book.EntityEntryLoader;
import org.skvipers.scribble_book.book.ItemEntryLoader;
import org.skvipers.scribble_book.item.ScribbleBookItem;
import org.skvipers.scribble_book.event.SoulboundHandler;
import org.skvipers.scribble_book.registry.ModDataComponents;
import org.skvipers.scribble_book.registry.ModItems;
import org.slf4j.Logger;

@Mod(ScribbleBook.MODID)
public class ScribbleBook {
    public static final String MODID = "scribble_book";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ScribbleBook(IEventBus modEventBus, ModContainer modContainer) {
        ScribbleBookAPI.registerBuiltins();
        ModItems.ITEMS.register(modEventBus);
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        modEventBus.addListener(this::onBuildCreativeTab);
        modEventBus.addListener(this::onRegisterPayloads);
        if (FMLEnvironment.getDist().isClient()) {
            NeoForge.EVENT_BUS.addListener(SpyglassScanHandler::onPlayerTick);
        }
        NeoForge.EVENT_BUS.addListener(this::onAddReloadListeners);
        NeoForge.EVENT_BUS.addListener(SoulboundHandler::onLivingDrops);
        NeoForge.EVENT_BUS.addListener(SoulboundHandler::onPlayerClone);
        NeoForge.EVENT_BUS.addListener(ScribbleBookItem::onEntityInteract);
        NeoForge.EVENT_BUS.addListener(ScribbleBookCommand::register);
        NeoForge.EVENT_BUS.addListener(ScribbleBook::onEntityKilled);
        NeoForge.EVENT_BUS.addListener(ScribbleBook::onPlayerLogin);
    }

    private void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.INK_BOTTLE);
            event.accept(ModItems.INK);
            event.accept(ModItems.SCRIBBLE_BOOK);
        }
    }

    private void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        var reg = event.registrar("1");
        reg.playToServer(ServerboundStudyItemPacket.TYPE, ServerboundStudyItemPacket.CODEC, ServerboundStudyItemPacket::handle);
        reg.playToClient(ClientboundOpenBookPacket.TYPE, ClientboundOpenBookPacket.CODEC,
                FMLEnvironment.getDist().isClient() ? ClientNetworkHandler::handleOpenBook : (p, c) -> {});
    }

    private static void onEntityKilled(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer sp) {
            ScribbleBookAPI.reEvaluateAllBooks(sp);
        }
    }

    private static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            ScribbleBookAPI.reEvaluateAllBooks(sp);
        }
    }

    private void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath(MODID, "book_entries"), BookEntryLoader.INSTANCE);
        event.addListener(Identifier.fromNamespaceAndPath(MODID, "entity_entries"), EntityEntryLoader.INSTANCE);
        event.addListener(Identifier.fromNamespaceAndPath(MODID, "item_entries"), ItemEntryLoader.INSTANCE);
        event.addListener(Identifier.fromNamespaceAndPath(MODID, "categories"), BookCategoryLoader.INSTANCE);
        event.addListener(Identifier.fromNamespaceAndPath(MODID, "custom_entries"), CustomEntryLoader.INSTANCE);
        event.addListener(Identifier.fromNamespaceAndPath(MODID, "aliases"), AliasLoader.INSTANCE);
    }
}
