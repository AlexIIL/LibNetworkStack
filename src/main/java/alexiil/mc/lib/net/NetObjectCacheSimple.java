/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import alexiil.mc.lib.net.NetObjectCache.IEntrySerialiser;

import it.unimi.dsi.fastutil.Hash.Strategy;

public class NetObjectCacheSimple<T> extends NetObjectCacheBase<T> {

    public NetObjectCacheSimple(Strategy<T> equality, IEntrySerialiser<T> serialiser) {
        super(equality, serialiser);
    }

    public NetObjectCacheSimple(Map<String, T> map, Function<T, String> reverse) {
        super(Util.identityHashStrategy(), new IEntrySerialiser<T>() {
            @Override
            public void write(T obj, ActiveConnection connection, NetByteBuf buffer) {
                buffer.writeString(reverse.apply(obj));
            }

            @Override
            public T read(ActiveConnection connection, NetByteBuf buffer) throws InvalidInputDataException {
                String str = buffer.readString();
                T obj = map.get(str);
                if (obj == null) {
                    throw new InvalidInputDataException("Unknown object '" + str + "'");
                }
                return obj;
            }
        });
    }

    public static <T> NetObjectCacheSimple<T> createMappedIdentifier(
        Map<Identifier, T> map, Function<T, Identifier> reverse
    ) {
        return new NetObjectCacheSimple<>(Util.identityHashStrategy(), new IEntrySerialiser<T>() {
            @Override
            public void write(T obj, ActiveConnection connection, NetByteBuf buffer) {
                buffer.writeIdentifier(reverse.apply(obj));
            }

            @Override
            public T read(ActiveConnection connection, NetByteBuf buffer) throws InvalidInputDataException {
                Identifier id = buffer.readIdentifierSafe();
                T obj = map.get(id);
                if (obj == null) {
                    throw new InvalidInputDataException("Unknown object '" + id + "'");
                }
                return obj;
            }
        });
    }

    public static <T> NetObjectCacheSimple<T> createPartiallyMappedIdentifier(
        Map<Identifier, T> map, Function<T, Identifier> reverse
    ) {
        return new NetObjectCacheSimple<>(Util.identityHashStrategy(), new IEntrySerialiser<T>() {
            @Override
            public void write(T obj, ActiveConnection connection, NetByteBuf buffer) {
                buffer.writeIdentifier(reverse.apply(obj));
            }

            @Override
            public T read(ActiveConnection connection, NetByteBuf buffer) throws InvalidInputDataException {
                Identifier id = buffer.readIdentifierSafe();
                T obj = map.get(id);
                if (obj == null) {
                    throw new InvalidInputDataException("Unknown object '" + id + "'");
                }
                return obj;
            }
        });
    }

    public static NetObjectCacheSimple<Identifier> createIdentifierSet(Set<Identifier> set) {
        return new NetObjectCacheSimple<>(Util.identityHashStrategy(), new IEntrySerialiser<Identifier>() {
            @Override
            public void write(Identifier obj, ActiveConnection connection, NetByteBuf buffer) {
                buffer.writeIdentifier(obj);
            }

            @Override
            public Identifier read(ActiveConnection connection, NetByteBuf buffer) throws InvalidInputDataException {
                Identifier id = buffer.readIdentifierSafe();
                if (!set.contains(id)) {
                    throw new InvalidInputDataException("Unknown object '" + id + "'");
                }
                return id;
            }
        });
    }

    public void write(T obj, NetByteBuf buffer, IMsgWriteCtx ctx) {
        NetObjectCacheBase<T>.Data cacheData = ctx.getConnection().getCacheData(this);
        int id = cacheData.objToId.getInt(obj);
        if (id == -1) {
            buffer.writeBoolean(true);
            int newId = cacheData.objToId.size();
            cacheData.objToId.put(obj, newId);
            buffer.writeVarUnsignedInt(newId);
            serialiser.write(obj, ctx.getConnection(), buffer);
        } else {
            buffer.writeBoolean(false);
            buffer.writeVarUnsignedInt(id);
        }
    }

    public T read(NetByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException {
        NetObjectCacheBase<T>.Data cacheData = ctx.getConnection().getCacheData(this);
        boolean isNew = buffer.readBoolean();
        int id = buffer.readVarUnsignedInt();
        if (isNew) {
            T obj = serialiser.read(ctx.getConnection(), buffer);
            cacheData.idToObj.put(id, obj);
            return obj;
        } else {
            T obj = cacheData.idToObj.get(id);
            if (obj == null) {
                throw new InvalidInputDataException("Unknown ID " + id);
            }
            return obj;
        }
    }
}
