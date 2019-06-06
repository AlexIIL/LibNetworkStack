package alexiil.mc.lib.net;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

public final class NetObjectCache<T> {

    public interface IEntrySerialiser<T> {
        void write(T obj, ActiveConnection connection, NetByteBuf buffer);

        T read(ActiveConnection connection, NetByteBuf buffer) throws InvalidInputDataException;
    }

    final class Data {
        // TODO: Entry removal!
        final Int2ObjectMap<T> idToObj = new Int2ObjectOpenHashMap<>();
        final Object2IntMap<T> objToId = new Object2IntLinkedOpenCustomHashMap<>(equality);

        public Data() {
            objToId.defaultReturnValue(-1);
        }
    }

    private final Hash.Strategy<T> equality;
    private final IEntrySerialiser<T> serialiser;
    private final ParentNetId netIdParent;
    private final NetIdData netIdPutCacheEntry;
    private final NetIdData netIdRemoveCacheEntry;

    public NetObjectCache(ParentNetId parent, Hash.Strategy<T> equality, IEntrySerialiser<T> serialiser) {
        this.equality = equality;
        this.serialiser = serialiser;
        this.netIdParent = parent;
        this.netIdPutCacheEntry = netIdParent.idData("put").setReceiver(this::receivePutCacheEntry);
        this.netIdRemoveCacheEntry = netIdParent.idData("remove").setReceiver(this::receiveRemoveCacheEntry);
    }

    private void receivePutCacheEntry(NetByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException {
        int id = buffer.readInt();
        T obj = serialiser.read(ctx.getConnection(), buffer);
        getData(ctx.getConnection()).idToObj.put(id, obj);
    }

    private void receiveRemoveCacheEntry(NetByteBuf buffer, IMsgReadCtx ctx) {

    }

    private NetObjectCache<T>.Data getData(ActiveConnection connection) {
        return connection.getCacheData(this);
    }

    public int getId(ActiveConnection connection, T obj) {
        Data data = getData(connection);
        int id = data.objToId.getInt(obj);
        if (id < 0) {
            id = data.objToId.size();
            data.objToId.put(obj, id);
            final int i = id;
            netIdPutCacheEntry.send(connection, (buffer, ctx) -> {
                buffer.writeInt(i);
                serialiser.write(obj, connection, buffer);
            });
        }
        return id;
    }

    @Nullable
    public T getObj(ActiveConnection connection, int id) {
        return getData(connection).idToObj.get(id);
    }
}
