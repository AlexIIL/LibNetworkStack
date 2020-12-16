package alexiil.mc.lib.net.impl;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.network.ServerPlayerEntity;

/** This should be implemented by {@link BlockEntity}s that wish to send their own initial data packet to players
 * individually, rather than using {@link BlockEntity#toInitialChunkDataTag()} or
 * {@link BlockEntity#toUpdatePacket()}. */
public interface BlockEntityInitialData {
    void sendInitialData(ServerPlayerEntity to);
}
