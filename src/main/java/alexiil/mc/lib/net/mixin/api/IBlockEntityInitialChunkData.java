package alexiil.mc.lib.net.mixin.api;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.chunk.WorldChunk;

/** This should be implemented by {@link BlockEntity}s that wish to send their own initial chunk data packet to players
 * individually, rather than using {@link BlockEntity#toInitialChunkDataTag()}. */
public interface IBlockEntityInitialChunkData {
    void sendInitialChunkData(ServerPlayerEntity to, WorldChunk chunk);
}
