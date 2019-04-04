package alexiil.mc.lib.net;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.netty.buffer.ByteBuf;

import net.fabricmc.fabric.api.network.PacketContext;

import alexiil.mc.lib.net.impl.ActiveMinecraftConnection;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/** An active game connection to a single receiver (and with a single sender). */
public abstract class ActiveConnection {

    final ParentNetId rootId;

    /** Map of int -> net_id for reading. */
    final List<TreeNetIdBase> readMapIds = new ArrayList<>();
    final Object2IntMap<NetIdPath> writeMapIds = new Object2IntOpenHashMap<>();

    final Map<NetObjectCache<?>, NetObjectCache<?>.Data> caches = new HashMap<>();

    /** The next ID to use for *writing*. Note that the other side of the connection will tell *us* what ID's to
     * allocate. */
    int nextFreeId = InternalMsgUtil.COUNT_HARDCODED_IDS;

    public ActiveConnection(ParentNetId rootId) {
        this.rootId = rootId;
        for (int i = 0; i < InternalMsgUtil.COUNT_HARDCODED_IDS; i++) {
            readMapIds.add(null);
        }
    }

    /** @return The Fabric API {@link PacketContext} for this connection. Throws an error if this is not a
     *         {@link ActiveMinecraftConnection}. (Although mods will *never* need to worry about that unless they
     *         create their own netty layer for a completely different connection). */
    public abstract PacketContext getMinecraftContext();

    /** @param data
     * @param packetId The packet ID that has been written out to the first int of the given buffer.
     * @param netId The {@link NetIdBase} that is being written out. Will be null if the packet ID is one of the
     *            internal packets.
     * @param priority The priority for the packet. Will be either 0 or a negative number. */
    public abstract void sendPacket(ByteBuf data, int packetId, NetIdBase netId, int priority);

    public void onReceiveRawData(ByteBuf data) throws InvalidInputDataException {
        InternalMsgUtil.onReceive(this, data);
    }

    <T> NetObjectCache<T>.Data getCacheData(NetObjectCache<T> cache) {
        // Nothing we can do about this warning without storing it directly in the cache
        return (NetObjectCache<T>.Data) caches.computeIfAbsent(cache, c -> c.new Data());
    }
}
