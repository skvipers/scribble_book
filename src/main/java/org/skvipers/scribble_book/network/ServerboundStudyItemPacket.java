package org.skvipers.scribble_book.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.skvipers.scribble_book.Config;
import org.skvipers.scribble_book.ScribbleBook;
import org.skvipers.scribble_book.item.ScribbleBookItem;

public record ServerboundStudyItemPacket(int entityId) implements CustomPacketPayload {

    public static final Type<ServerboundStudyItemPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ScribbleBook.MODID, "study_item"));

    public static final StreamCodec<FriendlyByteBuf, ServerboundStudyItemPacket> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, ServerboundStudyItemPacket::entityId,
                    ServerboundStudyItemPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ServerboundStudyItemPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            Entity entity = player.level().getEntity(packet.entityId());
            if (!(entity instanceof ItemEntity itemEntity)) return;

            double maxRange = Config.spyglassRange + 16.0;
            if (player.distanceToSqr(itemEntity) > maxRange * maxRange) return;

            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand  = player.getOffhandItem();
            ItemStack bookStack = mainHand.getItem() instanceof ScribbleBookItem ? mainHand
                    : offHand.getItem() instanceof ScribbleBookItem ? offHand : null;
            if (bookStack == null) return;

            ScribbleBookItem.doItemStudy(player, itemEntity, bookStack, true);
        });
    }
}
