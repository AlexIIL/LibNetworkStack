/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net;

import java.nio.charset.StandardCharsets;

import io.netty.buffer.Unpooled;

import alexiil.mc.lib.net.CheckingNetByteBuf.InvalidNetTypeException;
import alexiil.mc.lib.net.CheckingNetByteBuf.NetMethod;

public class InternalMsgUtil {

    private static final boolean DEBUG = LibNetworkStack.DEBUG;

    /** The ID to allocate a new packet. */
    public static final int ID_INTERNAL_ALLOCATE_STATIC = 0;

    /** The ID to inform the other side of a connection about the bandwidth we want to send/receive. */
    public static final int ID_INTERNAL_NEW_BANDWIDTH = 1;

    // What debug data?
    // What do we want to write out?
    // for example:

    // PACKET READ FAILURE
    // read:
    // .block_entity
    /// BLOCK_POS: [ 56, 62, -1467 ]
    // .libmultipart:container
    // .container
    /// BYTE: 2
    // (error thrown here)
    /// .

    // or

    // PACKET READ FAILURE
    // Header
    // .block_entity
    /// BLOCK_POS: [ 56, 61, 41 ]
    // .buildcraft:engine
    // .engine
    // .render_data
    // Payload
    /// BOOLEAN false
    /// BOOLEAN true
    /// DOUBLE 0.65
    /// ENUM 2
    // (error thrown here)

    // In other words write out the data types and ID path so we can see where stuff is going wrong
    // (in addition so we can see what data is in the packet).

    // Is that it? well.... what else could we write out that might be useful?
    // erm, one thing you thought about was writing the stacktrace of the errored packet.
    // but that's difficult to do nicely, as you'd usually need to get the server's log file.
    // As we're already killing the connection it seems reasonable to write out the entire stacktrace
    // HOWEVER
    // how would the sender send the stacktrace *before* it gets disconnected?
    // as it would normally work like this:

    // 1: sender sends a bad packet
    // 2: receiver throws an exception on read
    // 3: receiver sends "ID_INTERNAL_READ_FAILURE" for that bad packet
    // 4: receiver logs packet debug data.
    // 5: receiver disconnects
    // 6: sender logs packet debug data.

    // Where debug data is just the list of types.
    // However the write stacktrace would be hard to send ahead of time, as the sender only knows there's a problem
    // AFTER the receiver has already disconnected. Alternatively we could:

    // 1, 2, 3, 4 same
    // 5: receiver marks itself as INVALID and starts a ~4s timer until auto-disconnect
    // 6: sender logs packet debug data
    // 7: sender sends stacktrace of when the packet was written
    // 8: sender disconnects
    // 9: receiver logs stacktrace of writer
    // 10: receiver disconnects

    // However the receiver might exit after (5) if it takes more than 4 seconds to receive the debug info
    // (or if debugging is disabled server-side)

    // /** The ID to inform the sender that the given number of packets have been read successfully, and so the sender
    // can
    // * drop any debug information they had about those packets. */
    // public static final int ID_INTERNAL_READ_SUCCESS = 3;

    // /** The ID to inform the sender that something went badly wrong when reading one of it's packets, and both sides
    // * should print debug information about that packet. */
    // public static final int ID_INTERNAL_READ_FAILURE = 4;

    /** Debug data with type information for the next packet. */
    public static final int ID_INTERNAL_DEBUG_TYPES = 5;

    // /** Debug data with the stacktrace for the writer. */
    // public static final int ID_INTERNAL_DEBUG_STACKTRACE = 6;

    /** Sent by a receiver who wishes to receive {@link #ID_INTERNAL_DEBUG_TYPES}. */
    public static final int ID_INTERNAL_REQUEST_DEBUG_TYPES = 7;

    // /** Sent by a receiver who wishes to receive {@link #ID_INTERNAL_DEBUG_STACKTRACE}. The sender should reply with
    // * {@link #ID_INTERNAL_DEBUG_STACKTRACE_ENABLED} if it is enabled, along with the first index that can be
    // * requested. */
    // public static final int ID_INTERNAL_REQUEST_STACKTRACES = 8;

    // /** Reply for {@link #ID_INTERNAL_REQUEST_STACKTRACES}, which also includes the index of the first packet that
    // can
    // * have it's stacktrace requested. */
    // public static final int ID_INTERNAL_DEBUG_STACKTRACE_ENABLED = 9;

    // /** The ID for a packet that lib network stack has split up into smaller parts that need to be re-assembled. */
    // private static final int ID_INTERNAL_SPLIT_PACKET = 9;

    public static final int COUNT_HARDCODED_IDS = 9;

    /** @param buffer All of the data for a single packet. It must be complete! */
    public static void onReceive(ActiveConnection connection, NetByteBuf buffer) throws InvalidInputDataException {
        int id = buffer.readVarUnsignedInt();
        switch (id) {
            case ID_INTERNAL_ALLOCATE_STATIC: {
                int parent = buffer.readVarUnsignedInt();
                int newId = buffer.readVarUnsignedInt();
                int flags = buffer.readInt();
                final boolean isParent = ((flags & NetIdBase.FLAG_IS_PARENT) != 0);
                int len;
                if (isParent) {
                    len = TreeNetIdBase.PARENT_LENGTH;
                } else {
                    if ((flags & NetIdBase.PACKET_SIZE_FLAG) == NetIdBase.FLAG_FIXED_SIZE) {
                        len = buffer.readUnsignedMedium();
                    } else {
                        len = TreeNetIdBase.DYNAMIC_LENGTH;
                    }
                }
                int textLen = buffer.readUnsignedByte();
                byte[] textData = new byte[textLen];
                buffer.readBytes(textData);
                String str = new String(textData, StandardCharsets.UTF_8);

                final ParentNetIdBase p;
                if (parent == 0) {
                    p = connection.rootId;
                } else {
                    TreeNetIdBase cId = connection.readMapIds.get(parent);
                    if (!(cId instanceof ParentNetIdBase)) {
                        throw new InvalidInputDataException("Not a valid parent: " + cId.fullName);
                    }
                    p = (ParentNetIdBase) cId;
                }

                TreeNetIdBase childId;
                if (p instanceof ResolvedParentNetId) {
                    childId = resolveChild((ResolvedParentNetId<?, ?>) p, str);
                } else if (p instanceof ParentDynamicNetId<?, ?>) {
                    childId = resolveChild((ParentDynamicNetId<?, ?>) p, str);
                } else if (p instanceof ResolvedDynamicNetId) {
                    childId = resolveChild((ResolvedDynamicNetId<?>) p, str);
                } else {
                    childId = p.getChild(str);
                }
                if (childId == null) {
                    throw new InvalidInputDataException("Unknown child " + str + " of parent " + p.fullName);
                }
                if (DEBUG) {
                    LibNetworkStack.LOGGER.info(
                        (connection + " Received new id " + newId + " as " + childId + " '")
                            + ("(flags " + Integer.toBinaryString(flags) + ", len = " + lenToString(len) + ")")
                    );
                }
                // if (childId instanceof DynamicNetId) {
                // childId = resolve((DynamicNetId<?>) childId, p);
                // }
                if (connection.readMapIds.size() != newId) {
                    throw new InvalidInputDataException("Invalid new ID! We must have gotton out of sync somehow...");
                }
                connection.readMapIds.add(childId);
                if (childId.getLengthForPacketAlloc() != len) {
                    throw new InvalidInputDataException(
                        "Mismatched length! We expect " + lenToString(childId.getLengthForPacketAlloc()) + ", but we received " + lenToString(len)
                    );
                }
                break;
            }
            case ID_INTERNAL_NEW_BANDWIDTH: {
                int maximum = buffer.readUnsignedShort();
                if (connection instanceof BufferedConnection) {
                    ((BufferedConnection) connection).updateTheirMaxBandwidth(maximum);
                }
                break;
            }
            case ID_INTERNAL_DEBUG_TYPES: {
                int bytes = buffer.readVarUnsignedInt();
                int count = buffer.readVarUnsignedInt();
                NetByteBuf data = buffer.readBytes(bytes);
                connection.lastReceivedTypes = data;
                connection.lastReceivedTypesCount = count;
                break;
            }
            case ID_INTERNAL_REQUEST_DEBUG_TYPES: {
                connection.sendTypes = true;
                break;
            }
            default: {
                if (id < 0 || id >= connection.readMapIds.size()) {
                    throw new InvalidInputDataException(connection + " Unknown/invalid ID " + id);
                }
                TreeNetIdBase readId = connection.readMapIds.get(id);
                if (!(readId instanceof NetIdBase)) {
                    throw new InvalidInputDataException("Not a receiving node: " + readId + " for ID " + id);
                }
                NetIdBase netId = (NetIdBase) readId;

                int flags = netId.getFinalFlags();

                MessageContext.Read ctx = new MessageContext.Read(connection, netId);
                if ((flags & NetIdBase.FLAGS_SIDED) == NetIdBase.FLAG_SIDED_RECV_ON_CLIENT) {
                    ctx.assertClientSide();
                } else if ((flags & NetIdBase.FLAGS_SIDED) == NetIdBase.FLAG_SIDED_RECV_ON_SERVER) {
                    ctx.assertServerSide();
                }

                final int len;
                if (netId.hasFixedLength()) {
                    len = netId.totalLength;
                } else if ((flags & NetIdBase.PACKET_SIZE_FLAG) == NetIdBase.FLAG_TINY_PACKET) {
                    len = 1 + buffer.readUnsignedByte();
                } else if ((flags & NetIdBase.PACKET_SIZE_FLAG) == NetIdBase.FLAG_NORMAL_PACKET) {
                    len = 1 + buffer.readUnsignedShort();
                } else {
                    len = 1 + buffer.readUnsignedMedium();
                }
                NetByteBuf payload = buffer.readBytes(len);
                payload.markReaderIndex();

                NetByteBuf typeBuffer = connection.lastReceivedTypes;
                final CheckingNetByteBuf checkingBuffer;
                final boolean hasFullTypeBuffer;
                if (typeBuffer != null) {
                    hasFullTypeBuffer = true;
                    typeBuffer.markReaderIndex();
                    checkingBuffer = new CheckingNetByteBuf(payload, typeBuffer);
                } else if (LibNetworkStack.DEBUG) {
                    hasFullTypeBuffer = false;
                    checkingBuffer = new CheckingNetByteBuf(payload, typeBuffer = NetByteBuf.buffer());
                    checkingBuffer.recordReads();
                } else {
                    hasFullTypeBuffer = false;
                    checkingBuffer = new CheckingNetByteBuf(payload, null);
                }

                try {
                    if (netId.receive(checkingBuffer, ctx)) {
                        if (ctx.dropReason == null) {
                            if (checkingBuffer.readableBytes() > 0) {
                                throw new InvalidInputDataException("The packet has more data than was read!");
                            }
                        } else {
                            if (DEBUG) {
                                LibNetworkStack.LOGGER.info(
                                    connection + " Dropped " + netId.fullName + " because '" + ctx.dropReason + "'!"
                                );
                            }
                        }
                    } else {
                        if (DEBUG) {
                            LibNetworkStack.LOGGER.info(
                                connection + " Dropped " + netId.fullName + " as one of it's parents could not be read!"
                            );
                        }
                    }
                } catch (InvalidInputDataException | InvalidNetTypeException | IndexOutOfBoundsException e) {

                    StringBuilder sb = new StringBuilder();
                    sb.append("Packet failed to read correctly!\n\n");
                    sb.append("Packet: " + netId + "\n");
                    sb.append("Payload Length: " + len + "\n");
                    sb.append("Reader Index: " + payload.readerIndex() + ". (Marked by '#')\n");
                    sb.append("Error: " + e.getMessage() + "\n");

                    sb.append("Raw Bytes:\n");
                    sb.append("+---------\n");
                    sb.append("| ");
                    NetByteBuf tmp = NetByteBuf.buffer();
                    checkingBuffer.getBytes(0, tmp, len);
                    MsgUtil.appendBufferData(tmp, tmp.readableBytes(), sb, "| ", payload.readerIndex());
                    sb.append("\n");
                    sb.append("+---------\n");

                    NetByteBuf.SavedReaderIndex payloadIndex = payload.saveReaderIndex();
                    payload.resetReaderIndex();

                    if (typeBuffer == null) {
                        sb.append("WARNING: No type information found!\n");
                        sb.append("  Add '-Dlibnetworkstack.debug=true' to your VM arguments\n");
                        sb.append("  to record type information on packet read&write.\n");
                        sb.append("  Alternatively you can change 'debug' or 'debug.record_types' to true\n");
                        sb.append("  in the config file: " + LibNetworkStack.CONFIG_FILE_LOCATION + "\n");
                    } else {

                        int typeCount
                            = hasFullTypeBuffer ? connection.lastReceivedTypesCount : checkingBuffer.getCountRead();

                        NetByteBuf.SavedReaderIndex typeIndex = typeBuffer.saveReaderIndex();
                        typeBuffer.resetReaderIndex();

                        sb.append("Payload Types: " + typeCount + "\n");
                        if (!hasFullTypeBuffer) {
                            sb.append("(WARNING: these are from the read() ");
                            sb.append("methods as they weren't provided by the sender)\n");
                        }
                        sb.append("Payload Data:\n");

                        for (int i = 0; i < typeCount; i++) {
                            NetMethod method = checkingBuffer.typeBuffer.readEnumConstant(NetMethod.class);
                            if (method == NetMethod.MARKER_ID) {
                                int marker = checkingBuffer.readMarkerId_data();
                                sb.append("+---------\n");
                                if (marker < COUNT_HARDCODED_IDS || marker >= connection.readMapIds.size()) {
                                    sb.append("|Marker: invalid/unknown! (" + marker + ")\n");
                                } else {
                                    TreeNetIdBase markerId = connection.readMapIds.get(marker);
                                    sb.append("|");
                                    sb.append(markerId);
                                    sb.append("\n");
                                }
                            } else {
                                StringBuilder sb2 = new StringBuilder();
                                try {
                                    method.append(checkingBuffer, sb2);
                                } catch (Throwable t) {
                                    InvalidInputDataException e2 = new InvalidInputDataException(sb.toString(), e);
                                    e2.addSuppressed(t);
                                    throw e2;
                                }
                                sb.append("| ");
                                if (e instanceof InvalidNetTypeException && ((InvalidNetTypeException) e).index == i) {
                                    InvalidNetTypeException netType = (InvalidNetTypeException) e;
                                    int newLine = sb2.indexOf("\n");
                                    String inserted = "    <-- HERE: ";
                                    if (netType.read != method) {
                                        inserted += "tried to read " + netType.read;
                                    } else {
                                        inserted += e.getMessage();
                                    }
                                    if (newLine < 0) {
                                        sb2.append(inserted);
                                    } else {
                                        sb2.insert(newLine, inserted);
                                    }
                                }
                                sb.append(sb2.toString().replace("\n", "\n|"));
                                sb.append("\n");
                                if (
                                    payloadIndex.readerIndex == payload.readerIndex()
                                    && payloadIndex.readerIndex < payload.writerIndex()
                                ) {
                                    sb.append("+---(stopped reading here)\n");
                                }
                            }
                        }
                        typeBuffer.resetReaderIndex(typeIndex);

                        if (payload.readableBytes() > 0) {
                            sb.append("+---------\n");
                            sb.append("|Remaining Bytes (" + payload.readableBytes() + "):\n| ");
                            tmp = payload.readBytes(payload.readableBytes());
                            MsgUtil.appendBufferData(tmp, tmp.readableBytes(), sb, "| ", -1);
                            sb.append("\n");
                        }
                        sb.append("+---------\n");
                    }

                    // TODO: Anything else?

                    sb.append("\n");
                    throw new InvalidInputDataException(sb.toString(), e);
                } finally {
                    payload.release();
                    if (typeBuffer != null) {
                        typeBuffer.release();
                        connection.lastReceivedTypes = null;
                    }
                }
            }
        }
    }

    private static <P, C> ResolvedDynamicNetId<C> resolveChild(ParentDynamicNetId<P, C> parent, String str) {
        DynamicNetId<C> childId = parent.childId;
        if (!childId.name.equals(str)) {
            return null;
        }
        return new ResolvedDynamicNetId<>(parent, childId);
    }

    private static <T> TreeNetIdBase resolveChild(ResolvedParentNetId<?, T> netId, String str) {
        return resolveChild(netId, netId.reader, str);
    }

    private static <T> TreeNetIdBase resolveChild(ResolvedDynamicNetId<T> netId, String str) {
        return resolveChild(netId, netId.wrapped, str);
    }

    private static <T> TreeNetIdBase resolveChild(
        ParentNetIdSingle<T> resolved, ParentNetIdSingle<T> wrapped, String str
    ) {
        ParentNetIdDuel<T, ?> branchId = wrapped.branchChildren.get(str);
        if (branchId != null) {
            return new ResolvedParentNetId<>(resolved, branchId);
        }
        NetIdTyped<T> leafId = wrapped.leafChildren.get(str);
        if (leafId != null) {
            return new ResolvedNetId<>(resolved, leafId);
        }
        return null;
    }

    private static String lenToString(int len) {
        if (len == TreeNetIdBase.DYNAMIC_LENGTH) {
            return "Dynamic";
        } else if (len == TreeNetIdBase.PARENT_LENGTH) {
            return "Parent";
        } else {
            return Integer.toString(len);
        }
    }

    /** Sends a packet with it's priority set to it's default priority.
     * 
     * @param connection The connection to send it to.
     * @param netId The ID to write.
     * @param path The Path to the ID.
     * @param payload The data to write. */
    public static void send(ActiveConnection connection, NetIdBase netId, NetIdPath path, NetByteBuf payload) {
        NetByteBuf fullPayload = wrapFullPayload(connection, netId, path, payload);
        int id = fullPayload.getInt(0);
        connection.sendPacket(fullPayload, id, netId, netId.getDefaultPriority());
        fullPayload.release();
    }

    /** Sends a packet.
     * 
     * @param connection The connection to send it to.
     * @param netId The ID to write.
     * @param path The Path to the ID.
     * @param payload The data to write.
     * @param priority The priority level to use. 0 is the maximum. */
    public static void send(
        ActiveConnection connection, NetIdBase netId, NetIdPath path, NetByteBuf payload, int priority
    ) {
        NetByteBuf fullPayload = wrapFullPayload(connection, netId, path, payload);
        int id = fullPayload.getInt(0);
        connection.sendPacket(fullPayload, id, netId, priority);
        fullPayload.release();
    }

    private static NetByteBuf wrapFullPayload(
        ActiveConnection connection, NetIdBase netId, NetIdPath path, NetByteBuf payload
    ) {
        int id = getWriteId(connection, netId, path);
        int len = 5 + (netId.hasFixedLength() ? 0 : 4) + payload.readableBytes();
        if (netId.hasFixedLength()) {
            assert netId.totalLength == payload.readableBytes();
        }
        NetByteBuf fullPayload = NetByteBuf.buffer(len);
        fullPayload.writeVarUnsignedInt(id);
        if (!netId.hasFixedLength()) {
            if ((netId.getFinalFlags() & NetIdBase.PACKET_SIZE_FLAG) == NetIdBase.FLAG_TINY_PACKET) {
                assert payload.readableBytes() <= (1 << 8);
                fullPayload.writeByte(payload.readableBytes() - 1);
            } else if ((netId.getFinalFlags() & NetIdBase.PACKET_SIZE_FLAG) == NetIdBase.FLAG_NORMAL_PACKET) {
                assert payload.readableBytes() <= (1 << 16);
                fullPayload.writeShort(payload.readableBytes() - 1);
            } else {
                assert payload.readableBytes() <= (1 << 24);
                fullPayload.writeMedium(payload.readableBytes() - 1);
            }
        }
        fullPayload.writeBytes(payload, payload.readerIndex(), payload.readableBytes());
        return fullPayload;
    }

    static int getWriteId(ActiveConnection connection, TreeNetIdBase netId, NetIdPath path) {
        final int id;
        int currentId = connection.writeMapIds.getInt(path);
        if (currentId == 0) {
            id = allocAndSendNewId(connection, netId, path);
        } else {
            id = currentId;
        }
        return id;
    }

    static void sendNextTypes(ActiveConnection connection, NetByteBuf types, int count) {
        int bytes = types.readableBytes();
        if (bytes > NetByteBuf.MAX_VAR_U_INT_2_BYTES) {
            throw new IllegalArgumentException("Too many bytes! (" + bytes + ") > " + NetByteBuf.MAX_VAR_U_INT_2_BYTES);
        }
        if (count > NetByteBuf.MAX_VAR_U_INT_2_BYTES) {
            throw new IllegalArgumentException("Too many types! (" + count + ") > " + NetByteBuf.MAX_VAR_U_INT_2_BYTES);
        }
        int len = 1 + 2 + 2 + bytes;
        NetByteBuf fullPayload = NetByteBuf.buffer(len);
        fullPayload.writeVarUnsignedInt(ID_INTERNAL_DEBUG_TYPES);
        fullPayload.writeVarUnsignedInt(bytes);
        fullPayload.writeVarUnsignedInt(count);
        fullPayload.writeBytes(types, types.readerIndex(), bytes);
        connection.sendPacket(fullPayload, ID_INTERNAL_DEBUG_TYPES, null, 0);
        fullPayload.release();
    }

    private static int allocAndSendNewId(ActiveConnection connection, TreeNetIdBase netId, NetIdPath path) {
        assert netId == path.array[path.array.length - 1];
        byte[] textData = netId.name.getBytes(StandardCharsets.UTF_8);

        int pathLength = path.calculateLength();
        boolean writeLength = netId instanceof NetIdBase && pathLength != TreeNetIdBase.DYNAMIC_LENGTH;
        int len = 0//
            + 1// Packet ID
            + 5// Parent ID
            + 5// Our newly allocated ID (used to ensure that we are still in sync)
            + 4// Flags
            + (writeLength ? 3 : 0) // Packet Length
            + 1// Text bytes length
            + textData.length;// Text bytes

        NetByteBuf allocationData = NetByteBuf.asNetByteBuf(Unpooled.buffer(len));
        allocationData.writeVarUnsignedInt(ID_INTERNAL_ALLOCATE_STATIC);
        ParentNetIdBase parent = path.array.length > 1 ? (ParentNetIdBase) path.array[path.array.length - 2] : null;
        if (parent == null) {
            throw new IllegalArgumentException("The net ID " + netId + " must have a non-null parent!");
        } else if (parent == connection.rootId) {
            allocationData.writeVarUnsignedInt(0);
        } else {
            allocationData.writeVarUnsignedInt(getWriteId(connection, parent, path.parent()));
        }
        int newId = connection.writeMapIds.size() + COUNT_HARDCODED_IDS;
        connection.writeMapIds.put(path, newId);
        allocationData.writeVarUnsignedInt(newId);
        int flags = netId.getFinalFlags();
        allocationData.writeInt(flags);
        if ((flags & NetIdBase.FLAG_IS_PARENT) == 0) {
            if (((flags & NetIdBase.PACKET_SIZE_FLAG) == NetIdBase.FLAG_FIXED_SIZE) != writeLength) {
                throw new IllegalStateException(
                    "The packet " + netId + " has flags of " + flags + " but writeLength of " + writeLength
                );
            }
        }
        if (writeLength) {
            allocationData.writeMedium(pathLength);
        }
        allocationData.writeByte(textData.length);
        allocationData.writeBytes(textData);

        if (LibNetworkStack.DEBUG) {
            LibNetworkStack.LOGGER
                .info(connection + " Sending new ID " + newId + " -> " + netId.getPrintableName() + " " + path);
        }
        connection.sendPacket(allocationData, ID_INTERNAL_ALLOCATE_STATIC, null, NetIdBase.MAXIMUM_PRIORITY);
        allocationData.release();
        return newId;
    }
}
