package org.skvipers.scribble_book.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import org.skvipers.scribble_book.ScribbleBook;

import java.util.HashSet;
import java.util.Set;

public record ClientboundOpenBookPacket(InteractionHand hand, Set<Identifier> unlockedEntries)
        implements CustomPacketPayload {

    public static final Type<ClientboundOpenBookPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ScribbleBook.MODID, "open_book"));

    public static final StreamCodec<FriendlyByteBuf, ClientboundOpenBookPacket> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT.map(i -> InteractionHand.values()[i], InteractionHand::ordinal),
                    ClientboundOpenBookPacket::hand,
                    ByteBufCodecs.collection(HashSet::new, Identifier.STREAM_CODEC),
                    ClientboundOpenBookPacket::unlockedEntries,
                    ClientboundOpenBookPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
