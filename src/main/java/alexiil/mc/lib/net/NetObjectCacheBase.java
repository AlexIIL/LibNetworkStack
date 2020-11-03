/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net;

import alexiil.mc.lib.net.NetObjectCache.IEntrySerialiser;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.Hash.Strategy;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

abstract class NetObjectCacheBase<T> {

    class Data {
        // TODO: Entry removal!
        final Int2ObjectMap<T> idToObj = new Int2ObjectOpenHashMap<>();
        final Object2IntMap<T> objToId = new Object2IntLinkedOpenCustomHashMap<>(equality);

        Data(ActiveConnection connection) {
            objToId.defaultReturnValue(-1);
        }
    }

    final Hash.Strategy<T> equality;
    final IEntrySerialiser<T> serialiser;

    public NetObjectCacheBase(Strategy<T> equality, IEntrySerialiser<T> serialiser) {
        this.equality = equality;
        this.serialiser = serialiser;
    }

    Data newData(ActiveConnection connection) {
        return new Data(connection);
    }

    Data getData(ActiveConnection connection) {
        return connection.getCacheData(this);
    }
}
