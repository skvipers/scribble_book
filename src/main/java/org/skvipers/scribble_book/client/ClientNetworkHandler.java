package org.skvipers.scribble_book.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.skvipers.scribble_book.book.BookData;
import org.skvipers.scribble_book.client.screen.ScribbleBookScreen;
import org.skvipers.scribble_book.item.ScribbleBookItem;
import org.skvipers.scribble_book.network.ClientboundOpenBookPacket;
import org.skvipers.scribble_book.registry.ModDataComponents;

public class ClientNetworkHandler {

    public static void handleOpenBook(ClientboundOpenBookPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            ItemStack bookStack = mc.player.getItemInHand(packet.hand());
            if (!(bookStack.getItem() instanceof ScribbleBookItem)) return;
            BookData data = bookStack.getOrDefault(ModDataComponents.BOOK_DATA.get(), BookData.EMPTY);
            bookStack.set(ModDataComponents.BOOK_DATA.get(), data.withUnlocked(packet.unlockedEntries()));
            mc.setScreen(new ScribbleBookScreen(bookStack));
        });
    }
}
