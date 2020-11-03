/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.fabricmc.fabric.api.network.PacketContext;

import alexiil.mc.lib.net.impl.ActiveMinecraftConnection;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/** An active game connection to a single receiver (and with a single sender). */
public abstract class ActiveConnection {

    final ParentNetId rootId;

    /** Map of int -> net_id for reading. */
    final List<TreeNetIdBase> readMapIds = new ArrayList<>();
    final Object2IntMap<NetIdPath> writeMapIds = new Object2IntOpenHashMap<>();

    final Map<NetObjectCacheBase<?>, NetObjectCacheBase<?>.Data> caches = new HashMap<>();

    final Int2ObjectMap<StringTraceSegment> receivedTraceStringSegments;
    final Int2ObjectMap<SingleTraceLine> receivedTraceLines;
    final Int2ObjectMap<MultiTraceLines> receivedJoinedTraces;

    final StringTraceSegment rootTraceSegment;

    int allocatedStringSegemnts;
    int allocatedTraceSegemnts;
    int allocatedJoinedTraces;

    /** The next ID to use for *writing*. Note that the other side of the connection will tell *us* what ID's to
     * allocate. */
    int nextFreeId = InternalMsgUtil.COUNT_HARDCODED_IDS;

    boolean sendTypes = LibNetworkStack.CONFIG_RECORD_TYPES;

    /** As stacktraces can leak (potentially) information about the server modset (maybe?) and it's quite expensive this
     * values should always be AND'd with {@link LibNetworkStack#CONFIG_RECORD_STACKTRACES} to ensure that both sides
     * are okay sending information. */
    boolean sendStacktraces = false;

    int lastReceivedTypesCount;
    NetByteBuf lastReceivedTypes;
    MultiTraceLines lastReceivedStacktrace;

    public ActiveConnection(ParentNetId rootId) {
        this.rootId = rootId;
        for (int i = 0; i < InternalMsgUtil.COUNT_HARDCODED_IDS; i++) {
            readMapIds.add(null);
        }

        if (LibNetworkStack.CONFIG_RECORD_STACKTRACES) {
            receivedTraceStringSegments = new Int2ObjectOpenHashMap<>();
            receivedTraceLines = new Int2ObjectOpenHashMap<>();
            receivedJoinedTraces = new Int2ObjectOpenHashMap<>();
            rootTraceSegment = new StringTraceSegment(0, null, null, null);
        } else {
            receivedTraceStringSegments = null;
            receivedTraceLines = null;
            receivedJoinedTraces = null;
            rootTraceSegment = null;
        }
    }

    public final void postConstruct() {
        if (LibNetworkStack.CONFIG_RECORD_TYPES) {
            NetByteBuf data = NetByteBuf.buffer(1);
            data.writeVarInt(InternalMsgUtil.ID_INTERNAL_REQUEST_DEBUG_TYPES);
            sendPacket(data, InternalMsgUtil.ID_INTERNAL_REQUEST_DEBUG_TYPES, null, 0);
        }
        if (LibNetworkStack.CONFIG_RECORD_STACKTRACES) {
            NetByteBuf data = NetByteBuf.buffer(1);
            data.writeVarInt(InternalMsgUtil.ID_INTERNAL_REQUEST_STACKTRACES);
            sendPacket(data, InternalMsgUtil.ID_INTERNAL_REQUEST_STACKTRACES, null, 0);
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
    protected abstract void sendPacket(NetByteBuf data, int packetId, @Nullable NetIdBase netId, int priority);

    public void onReceiveRawData(NetByteBuf data) throws InvalidInputDataException {
        InternalMsgUtil.onReceive(this, data);
    }

    <T> NetObjectCacheBase<T>.Data getCacheData(NetObjectCacheBase<T> cache) {
        // Nothing we can do about this warning without storing it directly in the cache
        return (NetObjectCacheBase<T>.Data) caches.computeIfAbsent(cache, c -> c.new Data(this));
    }

    enum StringTraceSeparator {
        DOT('.'),
        SLASH('/'),
        DOLLAR('$');

        final char separator;

        StringTraceSeparator(char separator) {
            this.separator = separator;
        }

        @Nullable
        static StringTraceSeparator from(char c) {
            switch (c) {
                case '.':
                    return DOT;
                case '/':
                    return SLASH;
                default:
                    return null;
            }
        }
    }

    static final class StringTraceSegment {
        final int id;
        final StringTraceSegment parent;
        final StringTraceSeparator separator;
        final String str;

        final Map<String, StringTraceSegment> dotChildren = new HashMap<>();
        final Map<String, StringTraceSegment> slashChildren = new HashMap<>();
        final Map<String, StringTraceSegment> dollarChildren = new HashMap<>();
        final Int2ObjectMap<SingleTraceLine> lineChildren = new Int2ObjectOpenHashMap<>();

        public StringTraceSegment(int id, StringTraceSegment parent, StringTraceSeparator separator, String str) {
            this.id = id;
            this.separator = separator;
            this.str = str;
            this.parent = parent == null ? null : parent.separator == null ? null : parent;
            if (parent != null) {
                parent.getCharChild(separator).put(str, this);
            }
        }

        Map<String, StringTraceSegment> getCharChild(StringTraceSeparator sep) {
            switch (sep) {
                case DOT:
                    return dotChildren;
                case SLASH:
                    return slashChildren;
                case DOLLAR:
                    return dollarChildren;
                default:
                    throw new IllegalArgumentException("Unknown StringTraceSeparator " + sep);
            }
        }

        @Override
        public String toString() {
            if (parent == null) {
                return str;
            }
            return parent.toString() + separator.separator + str;
        }
    }

    static final class SingleTraceLine {
        final int id;
        final StringTraceSegment str;
        final int lineNumber;

        final Map<MultiTraceLines, MultiTraceLines> children = new HashMap<>();

        public SingleTraceLine(int id, StringTraceSegment str, int lineNumber) {
            this.id = id;
            this.str = str;
            this.lineNumber = lineNumber;

            str.lineChildren.put(lineNumber, this);
        }

        @Override
        public String toString() {
            return str + "():" + lineNumber;
        }
    }

    static final class MultiTraceLines {
        final int id;
        final MultiTraceLines parent;
        final SingleTraceLine line;

        public MultiTraceLines(int id, MultiTraceLines parent, SingleTraceLine line) {
            this.id = id;
            this.parent = parent;
            this.line = line;

            line.children.put(parent, this);
        }

        @Override
        public String toString() {
            if (parent == null) {
                return line.toString();
            } else {
                return line.toString() + "\n at " + parent.toString();
            }
        }
    }
}
