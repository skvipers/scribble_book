package org.skvipers.scribble_book.client;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.skvipers.scribble_book.Config;
import org.skvipers.scribble_book.item.ScribbleBookItem;
import org.skvipers.scribble_book.network.ServerboundStudyItemPacket;

public class SpyglassScanHandler {

    private static int lastSentEntityId = -1;

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide()) return;

        boolean scoping = player.isUsingItem() && player.getUseItem().is(Items.SPYGLASS);
        if (!scoping) {
            lastSentEntityId = -1;
            return;
        }

        InteractionHand spyglassHand = player.getUsedItemHand();
        var bookStack = spyglassHand == InteractionHand.MAIN_HAND
                ? player.getOffhandItem() : player.getMainHandItem();
        if (!(bookStack.getItem() instanceof ScribbleBookItem)) {
            lastSentEntityId = -1;
            return;
        }

        ItemEntity target = ScribbleBookItem.findItemEntityInRay(player, player.level(), Config.spyglassRange);
        if (target == null) {
            lastSentEntityId = -1;
            return;
        }

        if (target.getId() == lastSentEntityId) return;
        lastSentEntityId = target.getId();
        ClientPacketDistributor.sendToServer(new ServerboundStudyItemPacket(target.getId()));
    }
}
