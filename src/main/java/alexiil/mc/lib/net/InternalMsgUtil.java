/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.CodecException;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.ChunkPos;

import alexiil.mc.lib.net.ActiveConnection.MultiTraceLines;
import alexiil.mc.lib.net.ActiveConnection.SingleTraceLine;
import alexiil.mc.lib.net.ActiveConnection.StringTraceSegment;
import alexiil.mc.lib.net.ActiveConnection.StringTraceSeparator;
import alexiil.mc.lib.net.CheckingNetByteBuf.InvalidNetTypeException;
import alexiil.mc.lib.net.CheckingNetByteBuf.NetMethod;
import alexiil.mc.lib.net.NetByteBuf.SavedReaderIndex;
import alexiil.mc.lib.net.mixin.api.IThreadedAnvilChunkStorageMixin;

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

    /** Sent as part of {@link #ID_INTERNAL_DEBUG_STACKTRACE}. */
    public static final int ID_INTERNAL_ALLOCATE_STACKTRACE_ELEMENT = 2;

    // /** The ID to inform the sender that the given number of packets have been read successfully, and so the sender
    // can
    // * drop any debug information they had about those packets. */
    // public static final int ID_INTERNAL_READ_SUCCESS = 3;

    // /** The ID to inform the sender that something went badly wrong when reading one of it's packets, and both sides
    // * should print debug information about that packet. */
    // public static final int ID_INTERNAL_READ_FAILURE = 4;

    /** Debug data with type information for the next packet. */
    public static final int ID_INTERNAL_DEBUG_TYPES = 5;

    /** Debug data with the stacktrace for the writer. */
    public static final int ID_INTERNAL_DEBUG_STACKTRACE = 6;

    /** Sent by a receiver who wishes to receive {@link #ID_INTERNAL_DEBUG_TYPES}. */
    public static final int ID_INTERNAL_REQUEST_DEBUG_TYPES = 7;

    /** Sent by a receiver who wishes to receive {@link #ID_INTERNAL_DEBUG_STACKTRACE}. */
    // The sender should reply with
    // * {@link #ID_INTERNAL_DEBUG_STACKTRACE_ENABLED} if it is enabled, along with the first index that can be
    // * requested. */
    public static final int ID_INTERNAL_REQUEST_STACKTRACES = 8;

    // /** Reply for {@link #ID_INTERNAL_REQUEST_STACKTRACES}, which also includes the index of the first packet that
    // can
    // * have it's stacktrace requested. */
    // public static final int ID_INTERNAL_DEBUG_STACKTRACE_ENABLED = 9;

    // /** The ID for a packet that lib network stack has split up into smaller parts that need to be re-assembled. */
    // private static final int ID_INTERNAL_SPLIT_PACKET = 9;

    public static final int COUNT_HARDCODED_IDS = 9;

    private static final Method STACK_TRACE_ELEMENT_MODULE_NAME;

    static {
        Method method = null;
        try {
            method = StackTraceElement.class.getMethod("getModuleName");
        } catch (NoSuchMethodException e) {
            // ignore
        }
        STACK_TRACE_ELEMENT_MODULE_NAME = method;
    }

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
                        "Mismatched length! We expect " + lenToString(childId.getLengthForPacketAlloc())
                            + ", but we received " + lenToString(len)
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
            case ID_INTERNAL_ALLOCATE_STACKTRACE_ELEMENT: {
                int count = buffer.readUnsignedByte() + 1;
                int byteCount = buffer.readVarUnsignedInt();
                NetByteBuf subBuffer = buffer.readBytes(byteCount);
                if (DEBUG) {
                    LibNetworkStack.LOGGER
                        .info(connection + " Received " + count + " stacktrace elements in " + byteCount + " bytes.");
                }
                for (int i = 0; i < count; i++) {
                    if (DEBUG) {
                        LibNetworkStack.LOGGER.info(connection + " reading stacktrace " + (i + 1));
                    }
                    readStacktraceAllocation(connection, subBuffer);
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
            case ID_INTERNAL_DEBUG_STACKTRACE: {
                int stackId = buffer.readVarUnsignedInt();
                connection.lastReceivedStacktrace = connection.receivedJoinedTraces.get(stackId);
                if (connection.lastReceivedStacktrace == null) {
                    throw new InvalidInputDataException("Unknown MultiTraceLines id " + stackId);
                }
                break;
            }
            case ID_INTERNAL_REQUEST_DEBUG_TYPES: {
                connection.sendTypes = true;
                break;
            }
            case ID_INTERNAL_REQUEST_STACKTRACES: {
                if (!connection.sendStacktraces) {
                    connection.sendStacktraces = true;
                    if (LibNetworkStack.CONFIG_RECORD_STACKTRACES) {
                        LibNetworkStack.LOGGER.info(connection + " is now being sent stacktraces for every packet.");
                    } else {
                        LibNetworkStack.LOGGER.info(
                            connection
                                + " requested stacktraces, but debug.record_stacktraces is disabled so we won't send any."
                        );
                    }
                }
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
                NetByteBuf payload = connection.allocBuffer(len);
                buffer.readBytes(payload, len);
                SavedReaderIndex payloadStart = payload.saveReaderIndex();

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
                } catch (
                    InvalidInputDataException | InvalidNetTypeException | IndexOutOfBoundsException | CodecException e
                ) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Packet failed to read correctly!\n\n");
                    sb.append("Packet: " + netId + "\n");
                    sb.append("Payload Length: " + len + "\n");
                    sb.append("Reader Index: " + payload.readerIndex() + ". (Marked by '#')\n");
                    sb.append("Error: " + e.getMessage() + "\n");

                    sb.append("Raw Bytes:\n");
                    sb.append("+---------\n");
                    sb.append("| ");
                    MsgUtil.appendBufferData(checkingBuffer, 0, len, sb, "| ", payload.readerIndex());
                    sb.append("\n");
                    sb.append("+---------\n");

                    NetByteBuf.SavedReaderIndex payloadIndex = payload.saveReaderIndex();
                    payload.resetReaderIndex(payloadStart);

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
                                    method.appender.readAndAppend(checkingBuffer, sb2);
                                } catch (Throwable t) {
                                    InvalidInputDataException e2 = new InvalidInputDataException(sb.toString(), e);
                                    e2.addSuppressed(t);
                                    throw e2;
                                }
                                if (method == NetMethod.CUSTOM_MARKER) {
                                    sb.append("+-");
                                } else {
                                    sb.append("| ");
                                }
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

                        int rem = payload.readableBytes();
                        if (rem > 0) {
                            sb.append("+---------\n");
                            sb.append("|Remaining Bytes (" + rem + "):\n| ");
                            MsgUtil.appendBufferData(payload, payload.readerIndex(), rem, sb, "| ", -1);
                            sb.append("\n");
                        }
                        sb.append("+---------\n");
                    }

                    if (connection.lastReceivedStacktrace != null) {
                        sb.append("\n+---------\n");
                        sb.append("|Sender Stacktrace:\n|\n");
                        boolean first = true;
                        MultiTraceLines line = connection.lastReceivedStacktrace;
                        do {
                            sb.append(first ? "|     " : "|  at ");
                            sb.append(line.line);
                            sb.append("\n");
                            first = false;
                        } while ((line = line.parent) != null);
                        sb.append("+---------\n");
                    }

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

    private static void readStacktraceAllocation(ActiveConnection connection, NetByteBuf buffer)
        throws InvalidInputDataException {

        int type = buffer.readFixedBits(2);
        switch (type) {
            case 0: {
                int newId = buffer.readVarUnsignedInt();
                int parentId = buffer.readVarUnsignedInt();
                StringTraceSeparator sep = buffer.readEnumConstant(StringTraceSeparator.class);
                String text = buffer.readString();
                StringTraceSegment parent = connection.receivedTraceStringSegments.get(parentId);
                if (parent == null && parentId != 0) {
                    throw new InvalidInputDataException("Unknown parent ID for StringTraceSegment " + parent);
                }
                StringTraceSegment segment = new StringTraceSegment(newId, parent, sep, text);
                StringTraceSegment old = connection.receivedTraceStringSegments.put(newId, segment);
                if (old != null) {
                    throw new InvalidInputDataException("Duplicate StringTraceSegment " + old + " vs added " + segment);
                }
                if (DEBUG) {
                    LibNetworkStack.LOGGER
                        .info(connection + " Received new stacktrace string element " + newId + " as " + segment);
                }
                break;
            }
            case 1: {
                int newId = buffer.readVarUnsignedInt();
                int parentId = buffer.readVarUnsignedInt();
                int lineNumber = buffer.readVarUnsignedInt();
                StringTraceSegment parent = connection.receivedTraceStringSegments.get(parentId);
                if (parent == null) {
                    throw new InvalidInputDataException("Unknown parent ID for SingleTraceLine " + parentId);
                }
                SingleTraceLine single = new SingleTraceLine(newId, parent, lineNumber);
                SingleTraceLine old = connection.receivedTraceLines.put(newId, single);
                if (old != null) {
                    throw new InvalidInputDataException("Duplicate SingleTraceLine " + old + " vs added " + single);
                }
                if (DEBUG) {
                    LibNetworkStack.LOGGER
                        .info(connection + " Received new stacktrace line element " + newId + " as " + single);
                }
                break;
            }
            case 2: {
                int newId = buffer.readVarUnsignedInt();
                int parentId = buffer.readVarUnsignedInt();
                int lineId = buffer.readVarUnsignedInt();
                MultiTraceLines parent = connection.receivedJoinedTraces.get(parentId);
                if (parent == null && parentId != 0) {
                    throw new InvalidInputDataException("Unknown parent ID for MultiTraceLines " + parentId);
                }
                SingleTraceLine line = connection.receivedTraceLines.get(lineId);
                if (line == null) {
                    throw new InvalidInputDataException("Unknown ID for SingleTraceLine " + lineId);
                }
                MultiTraceLines multi = new MultiTraceLines(newId, parent, line);
                MultiTraceLines old = connection.receivedJoinedTraces.put(newId, multi);
                if (old != null) {
                    throw new InvalidInputDataException("Duplicate MultiTraceLines " + old + " vs added " + multi);
                }
                if (DEBUG) {
                    LibNetworkStack.LOGGER.info(
                        connection + " Received new stacktrace multi element " + newId + " p " + parentId + " as "
                            + multi
                    );
                }
                break;
            }
            default: {
                throw new InvalidInputDataException("Unknown ID_INTERNAL_ALLOCATE_STACKTRACE_ELEMENT type " + type);
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
                if (payload.readableBytes() > (1 << 8)) {
                    throw new IllegalArgumentException(
                        "Packet Payload too large for TINY packet size - was " + payload.readableBytes()
                            + ", which is bigger than 256!"
                    );
                }
                fullPayload.writeByte(payload.readableBytes() - 1);
            } else if ((netId.getFinalFlags() & NetIdBase.PACKET_SIZE_FLAG) == NetIdBase.FLAG_NORMAL_PACKET) {
                if (payload.readableBytes() > (1 << 16)) {
                    throw new IllegalArgumentException(
                        "Packet Payload too large for NORMAL packet size - was " + payload.readableBytes()
                            + ", which is bigger than " + (1 << 16) + "!"
                    );
                }
                fullPayload.writeShort(payload.readableBytes() - 1);
            } else {
                if (payload.readableBytes() > (1 << 24)) {
                    throw new IllegalArgumentException(
                        "Packet Payload too large for LARGE packet size - was " + payload.readableBytes()
                            + ", which is bigger than " + (1 << 24) + "!"
                    );
                }
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
        if (bytes > NetByteBuf.MAX_VAR_U_INT_3_BYTES) {
            throw new IllegalArgumentException("Too many bytes! (" + bytes + ") > " + NetByteBuf.MAX_VAR_U_INT_3_BYTES);
        }
        if (count > NetByteBuf.MAX_VAR_U_INT_3_BYTES) {
            throw new IllegalArgumentException("Too many types! (" + count + ") > " + NetByteBuf.MAX_VAR_U_INT_3_BYTES);
        }
        int len = 1 + 3 + 3 + bytes;
        NetByteBuf fullPayload = NetByteBuf.buffer(len);
        fullPayload.writeVarUnsignedInt(ID_INTERNAL_DEBUG_TYPES);
        fullPayload.writeVarUnsignedInt(bytes);
        fullPayload.writeVarUnsignedInt(count);
        fullPayload.writeBytes(types, types.readerIndex(), bytes);
        connection.sendPacket(fullPayload, ID_INTERNAL_DEBUG_TYPES, null, 0);
        fullPayload.release();
    }

    /** Creates a debug throwable, with special handling for debugging {@link BlockEntity}s sent too early. */
    static void createAndSendDebugThrowable(ActiveConnection connection, MessageContext.Write ctx) {
        final Throwable throwable;
        BlockEntity be = ctx.__debugBlockEntity;
        if (be == null) {
            throwable = __NOT_A_BLOCK_ENTITY();
        } else if (!(be.getWorld() instanceof ServerWorld svWorld)) {
            throwable = __BLOCK_ENTITY_NOT_ON_SERVER();
        } else {
            ThreadedAnvilChunkStorage tacs = svWorld.getChunkManager().threadedAnvilChunkStorage;
            IThreadedAnvilChunkStorageMixin mixinTacs = (IThreadedAnvilChunkStorageMixin) tacs;
            ChunkHolder chunkHolder = mixinTacs.libnetworkstack_getChunkHolder(new ChunkPos(be.getPos()));
            if (chunkHolder.getWorldChunk() == null) {
                throwable = __BLOCK_ENTITY_NOT_SENT();
            } else {
                throwable = __BLOCK_ENTITY_PROBABLY_SENT();
            }
        }
        sendNextStacktrace(connection, throwable);
    }

    private static Throwable __NOT_A_BLOCK_ENTITY() {
        return new Throwable();
    }

    private static Throwable __BLOCK_ENTITY_NOT_ON_SERVER() {
        return new Throwable();
    }

    private static Throwable __BLOCK_ENTITY_NOT_SENT() {
        return new Throwable();
    }

    private static Throwable __BLOCK_ENTITY_PROBABLY_SENT() {
        return new Throwable();
    }

    static void sendNextStacktrace(ActiveConnection connection, Throwable t) {
        class Gen {
            NetByteBuf traceAllocationBuf = null;
            int allocationCount = 0;

            StringBuilder sb = new StringBuilder();
            StringTraceSegment sParent = null;

            void beginAllocation() {
                if (traceAllocationBuf == null) {
                    traceAllocationBuf = NetByteBuf.buffer();
                }
            }

            void finishAllocation() {
                allocationCount++;
                if (allocationCount == 256) {
                    if (DEBUG) {
                        LibNetworkStack.LOGGER.info(
                            connection + " Sending 256 (complete) stacktrace allocations in "
                                + traceAllocationBuf.writerIndex() + " bytes."
                        );
                    }

                    NetByteBuf buffer = NetByteBuf.buffer();
                    buffer.writeVarUnsignedInt(ID_INTERNAL_ALLOCATE_STACKTRACE_ELEMENT);
                    buffer.writeByte(255);
                    buffer.writeVarUnsignedInt(traceAllocationBuf.readableBytes());
                    buffer.writeBytes(traceAllocationBuf);
                    connection.sendPacket(buffer, ID_INTERNAL_ALLOCATE_STACKTRACE_ELEMENT, null, 0);
                    buffer.release();
                    traceAllocationBuf.release();
                    traceAllocationBuf = null;
                    allocationCount = 0;
                }
            }

            void finish() {
                if (allocationCount > 0) {
                    if (DEBUG) {
                        LibNetworkStack.LOGGER.info(
                            connection + " Sending " + allocationCount + " (partial) stacktrace allocations in "
                                + traceAllocationBuf.writerIndex() + " bytes."
                        );
                    }

                    NetByteBuf buffer = NetByteBuf.buffer();
                    buffer.writeVarUnsignedInt(ID_INTERNAL_ALLOCATE_STACKTRACE_ELEMENT);
                    buffer.writeByte(allocationCount - 1);
                    buffer.writeVarUnsignedInt(traceAllocationBuf.readableBytes());
                    buffer.writeBytes(traceAllocationBuf);
                    connection.sendPacket(buffer, ID_INTERNAL_ALLOCATE_STACKTRACE_ELEMENT, null, 0);
                    buffer.release();
                    traceAllocationBuf.release();
                    traceAllocationBuf = null;
                    allocationCount = 0;
                }
            }

            void allocSegment(StringTraceSeparator sep) {
                String text = sb.toString();
                sb.replace(0, Integer.MAX_VALUE, "");
                StringTraceSegment next = sParent.getCharChild(sep).get(text);
                if (next == null) {

                    int newId = ++connection.allocatedStringSegemnts;
                    next = new StringTraceSegment(newId, sParent, sep, text);
                    if (DEBUG) {
                        LibNetworkStack.LOGGER
                            .info(connection + " Sending new stacktrace string element " + newId + " as " + next);
                    }

                    // TYPES:
                    // 0 = StringTraceSegment
                    // 1 = SingleTraceLine
                    // 2 = MultiTraceLines
                    // 3 = (unused)
                    beginAllocation();
                    traceAllocationBuf.writeFixedBits(0, 2);
                    traceAllocationBuf.writeVarUnsignedInt(newId);
                    traceAllocationBuf.writeVarUnsignedInt(sParent.id);
                    traceAllocationBuf.writeEnumConstant(sep);
                    traceAllocationBuf.writeString(text);

                    finishAllocation();
                }
                sParent = next;
            }

            public StringTraceSeparator walk(String fullName, StringTraceSeparator lastSep) {
                for (int j = 0; j < fullName.length(); j++) {
                    char c = fullName.charAt(j);
                    StringTraceSeparator sep = StringTraceSeparator.from(c);
                    if (sep != null) {
                        allocSegment(lastSep);
                        lastSep = sep;
                    } else {
                        sb.append(c);
                    }
                }
                if (sb.length() > 0) {
                    allocSegment(lastSep);
                }
                return lastSep;
            }
        }
        Gen gen = new Gen();

        StackTraceElement[] trace = t.getStackTrace();

        ActiveConnection.MultiTraceLines parent = null;

        for (int i = trace.length - 1; i >= 0; i--) {
            StackTraceElement ste = trace[i];
            String moduleName = grabModuleName(ste);
            String fqn = ste.getClassName();
            String mth = ste.getMethodName();
            int line = ste.getLineNumber();

            gen.sParent = connection.rootTraceSegment;
            StringTraceSeparator lastSep = StringTraceSeparator.DOT;

            if (moduleName != null) {
                lastSep = gen.walk(moduleName, lastSep);
                lastSep = StringTraceSeparator.SLASH;
            }
            lastSep = gen.walk(fqn, lastSep);
            gen.walk(mth, lastSep);

            SingleTraceLine fullLine = gen.sParent.lineChildren.get(line);
            if (fullLine == null) {
                int newId = ++connection.allocatedTraceSegemnts;
                fullLine = new SingleTraceLine(newId, gen.sParent, line);

                if (DEBUG) {
                    LibNetworkStack.LOGGER
                        .info(connection + " Sending new stacktrace line element " + newId + " as " + fullLine);
                }

                gen.beginAllocation();
                gen.traceAllocationBuf.writeFixedBits(1, 2);
                gen.traceAllocationBuf.writeVarUnsignedInt(newId);
                gen.traceAllocationBuf.writeVarUnsignedInt(gen.sParent.id);
                gen.traceAllocationBuf.writeVarUnsignedInt(line);
                gen.finishAllocation();
            }

            MultiTraceLines next = fullLine.children.get(parent);
            if (next == null) {
                int newId = ++connection.allocatedJoinedTraces;
                next = new MultiTraceLines(newId, parent, fullLine);

                if (DEBUG) {
                    LibNetworkStack.LOGGER.info(
                        connection + " Sending new stacktrace multi element " + newId + " p "
                            + (parent == null ? 0 : parent.id) + " as " + next
                    );
                }

                gen.beginAllocation();
                gen.traceAllocationBuf.writeFixedBits(2, 2);
                gen.traceAllocationBuf.writeVarUnsignedInt(newId);
                gen.traceAllocationBuf.writeVarUnsignedInt(parent == null ? 0 : parent.id);
                gen.traceAllocationBuf.writeVarUnsignedInt(fullLine.id);
                gen.finishAllocation();
            }
            parent = next;
        }

        gen.finish();

        if (parent == null) {
            throw new IllegalStateException("No stacktrace to send! " + Arrays.toString(trace));
        }

        NetByteBuf fullPayload = NetByteBuf.buffer(6);
        fullPayload.writeVarUnsignedInt(ID_INTERNAL_DEBUG_STACKTRACE);
        fullPayload.writeVarUnsignedInt(parent.id);
        connection.sendPacket(fullPayload, ID_INTERNAL_DEBUG_STACKTRACE, null, 0);
        fullPayload.release();
    }

    private static String grabModuleName(StackTraceElement ste) {
        if (STACK_TRACE_ELEMENT_MODULE_NAME != null) {
            try {
                return (String) STACK_TRACE_ELEMENT_MODULE_NAME.invoke(ste);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new Error(e);
            }
        } else {
            return null;
        }
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
