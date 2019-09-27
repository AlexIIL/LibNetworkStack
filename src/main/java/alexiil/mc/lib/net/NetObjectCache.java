/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.util.Identifier;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

public final class NetObjectCache<T> {

    static final boolean DEBUG = LibNetworkStack.DEBUG || Boolean.getBoolean("libnetworkstack.cache.debug");

    public interface IEntrySerialiser<T> {
        void write(T obj, ActiveConnection connection, NetByteBuf buffer);

        T read(ActiveConnection connection, NetByteBuf buffer) throws InvalidInputDataException;
    }

    final class Data {
        // TODO: Entry removal!
        final Int2ObjectMap<T> idToObj = new Int2ObjectOpenHashMap<>();
        final Object2IntMap<T> objToId = new Object2IntLinkedOpenCustomHashMap<>(equality);

        Data(ActiveConnection connection) {
            if (DEBUG) {
                LibNetworkStack.LOGGER.info("[cache] " + connection + " " + netIdParent + " Created a new cache.");
            }
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

    /** @see NetIdBase#notBuffered() */
    public void notBuffered() {
        netIdPutCacheEntry.notBuffered();
        netIdRemoveCacheEntry.notBuffered();
    }

    public static <T> NetObjectCache<T> createMappedIdentifier(ParentNetId parent, Function<T, Identifier> nameGetter,
        Function<Identifier, T> objectGetter) {
        return new NetObjectCache<>(parent, new Hash.Strategy<T>() {
            @Override
            public int hashCode(T o) {
                return o == null ? 0 : nameGetter.apply(o).hashCode();
            }

            @Override
            public boolean equals(T a, T b) {
                if (a == null || b == null) {
                    return a == b;
                }
                return Objects.equals(nameGetter.apply(a), nameGetter.apply(b));
            }
        }, new IEntrySerialiser<T>() {
            @Override
            public void write(T obj, ActiveConnection connection, NetByteBuf buffer) {
                buffer.writeIdentifier(nameGetter.apply(obj));
            }

            @Override
            public T read(ActiveConnection connection, NetByteBuf buffer) throws InvalidInputDataException {
                Identifier id = buffer.readIdentifierSafe();
                if (id == null) {
                    return null;
                }
                return objectGetter.apply(id);
            }
        });
    }

    public static <T> NetObjectCache<T> createMappedIdentifier(ParentNetId parent, Function<T, Identifier> nameGetter,
        Map<Identifier, T> map) {
        return createMappedIdentifier(parent, nameGetter, map::get);
    }

    private void receivePutCacheEntry(NetByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException {
        int id = buffer.readInt();
        T obj = serialiser.read(ctx.getConnection(), buffer);
        if (DEBUG) {
            LibNetworkStack.LOGGER.info(
                "[cache] " + ctx.getConnection() + " " + netIdParent + " Read new ID " + id + " for object " + obj
            );
        }
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
            if (DEBUG) {
                LibNetworkStack.LOGGER.info(
                    "[cache] " + connection + " " + netIdParent + " Sending new ID " + i + " for object " + obj
                );
            }
            netIdPutCacheEntry.send(connection, (buffer, ctx) -> {
                buffer.writeInt(i);
                serialiser.write(obj, connection, buffer);
            });
        }
        return id;
    }

    @Nullable
    public T getObj(ActiveConnection connection, int id) {
        NetObjectCache<T>.Data data = getData(connection);
        if (DEBUG && !data.idToObj.containsKey(id)) {
            LibNetworkStack.LOGGER.info("[cache] " + connection + " " + netIdParent + " Unknown ID " + id + "!");
        }
        return data.idToObj.get(id);
    }
}
