package alexiil.mc.lib.net;

import java.nio.charset.StandardCharsets;

import io.netty.buffer.Unpooled;

public class InternalMsgUtil {

    private static final boolean DEBUG = LibNetworkStack.DEBUG;

    /** The ID to allocate a new packet. */
    public static final int ID_INTERNAL_ALLOCATE_STATIC = 0;

    /** The ID to inform the other side of a connection about the bandwidth we want to send/receive. */
    public static final int ID_INTERNAL_NEW_BANDWIDTH = 1;

    // /** The ID for a packet that lib network stack has split up into smaller parts that need to be re-assembled. */
    // private static final int ID_INTERNAL_SPLIT_PACKET = 2;

    public static final int COUNT_HARDCODED_IDS = 2;

    /** @param buffer All of the data for a single packet. It must be complete! */
    public static void onReceive(ActiveConnection connection, NetByteBuf buffer) throws InvalidInputDataException {
        int id = buffer.readInt();
        switch (id) {
            case ID_INTERNAL_ALLOCATE_STATIC: {
                int parent = buffer.readInt();
                int newId = buffer.readInt();
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
                if (DEBUG) {
                    LibNetworkStack.LOGGER.info(
                        (connection + " Allocating " + newId + " to " + str + " with parent '" + p.fullName + " '")
                        + ("(flags " + Integer.toBinaryString(flags) + ", len = " + lenToString(len) + ")")
                    );
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
            default: {
                if (id < 0 || id >= connection.readMapIds.size()) {
                    throw new InvalidInputDataException("");
                }
                TreeNetIdBase readId = connection.readMapIds.get(id);
                if (!(readId instanceof NetIdBase)) {
                    throw new InvalidInputDataException("Not a receiving node: " + readId);
                }
                NetIdBase netId = (NetIdBase) readId;

                final int len;
                if (netId.hasFixedLength()) {
                    len = netId.totalLength;
                } else if ((netId.getFlags() & NetIdBase.PACKET_SIZE_FLAG) == NetIdBase.FLAG_TINY_PACKET) {
                    len = 1 + buffer.readUnsignedByte();
                } else if ((netId.getFlags() & NetIdBase.PACKET_SIZE_FLAG) == NetIdBase.FLAG_NORMAL_PACKET) {
                    len = 1 + buffer.readUnsignedShort();
                } else {
                    len = 1 + buffer.readUnsignedMedium();
                }
                NetByteBuf payload = buffer.readBytes(len);
                MessageContext.Read ctx = new MessageContext.Read(connection, netId);
                if (!netId.receive(payload, ctx)) {
                    if (DEBUG) {
                        LibNetworkStack.LOGGER
                            .info("Dropped " + netId.fullName + " as one of it's parents could not be read!");
                    }
                }
                payload.release();
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

    private static <T> TreeNetIdBase resolveChild(ParentNetIdSingle<T> resolved, ParentNetIdSingle<T> wrapped,
        String str) {
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
        NetByteBuf fullPayload = send0(connection, netId, path, payload);
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
    public static void send(ActiveConnection connection, NetIdBase netId, NetIdPath path, NetByteBuf payload,
        int priority) {
        NetByteBuf fullPayload = send0(connection, netId, path, payload);
        int id = fullPayload.getInt(0);
        connection.sendPacket(fullPayload, id, netId, priority);
        fullPayload.release();
    }

    private static NetByteBuf send0(ActiveConnection connection, NetIdBase netId, NetIdPath path, NetByteBuf payload) {
        int id = getWriteId(connection, netId, path);
        int len = 4 + (netId.hasFixedLength() ? 0 : 4) + payload.readableBytes();
        if (netId.hasFixedLength()) {
            assert netId.totalLength == payload.readableBytes();
        }
        NetByteBuf fullPayload = NetByteBuf.asNetByteBuf(Unpooled.buffer(len));
        fullPayload.writeInt(id);
        if (!netId.hasFixedLength()) {
            if ((netId.getFlags() & NetIdBase.PACKET_SIZE_FLAG) == NetIdBase.FLAG_TINY_PACKET) {
                assert payload.readableBytes() <= (1 << 8);
                fullPayload.writeByte(payload.readableBytes() - 1);
            } else if ((netId.getFlags() & NetIdBase.PACKET_SIZE_FLAG) == NetIdBase.FLAG_NORMAL_PACKET) {
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

    private static int getWriteId(ActiveConnection connection, TreeNetIdBase netId, NetIdPath path) {
        final int id;
        int currentId = connection.writeMapIds.getInt(path);
        if (currentId == 0) {
            id = allocAndSendNewId(connection, netId, path);
        } else {
            id = currentId;
        }
        return id;
    }

    private static int allocAndSendNewId(ActiveConnection connection, TreeNetIdBase netId, NetIdPath path) {
        assert netId == path.array[path.array.length - 1];
        byte[] textData = netId.name.getBytes(StandardCharsets.UTF_8);

        int pathLength = path.calculateLength();
        boolean writeLength = netId instanceof NetIdBase && pathLength != TreeNetIdBase.DYNAMIC_LENGTH;
        int len = 0//
        + 4// Packet ID
        + 4// Parent ID
        + 4// Our newly allocated ID (used to ensure that we are still in sync)
        + 4// Flags
        + (writeLength ? 3 : 0) // Packet Length
        + 1// Text bytes length
        + textData.length;// Text bytes

        NetByteBuf allocationData = NetByteBuf.asNetByteBuf(Unpooled.buffer(len));
        allocationData.writeInt(ID_INTERNAL_ALLOCATE_STATIC);
        ParentNetIdBase parent = path.array.length > 1 ? (ParentNetIdBase) path.array[path.array.length - 2] : null;
        if (parent == null) {
            throw new IllegalArgumentException("The net ID " + netId + " must have a non-null parent!");
        } else if (parent == connection.rootId) {
            allocationData.writeInt(0);
        } else {
            allocationData.writeInt(getWriteId(connection, parent, path.parent()));
        }
        int newId = connection.writeMapIds.size() + COUNT_HARDCODED_IDS;
        connection.writeMapIds.put(path, newId);
        allocationData.writeInt(newId);
        int flags = netId.getFlags();
        allocationData.writeInt(flags);
        if (((flags & NetIdBase.FLAG_IS_PARENT) == 0)
        && (((flags & NetIdBase.PACKET_SIZE_FLAG) == NetIdBase.FLAG_FIXED_SIZE) != writeLength)) {
            throw new IllegalStateException(
                "The packet " + netId + " has flags of " + flags + " but writeLength of " + writeLength
            );
        }
        if (writeLength) {
            allocationData.writeMedium(pathLength);
        }
        allocationData.writeByte(textData.length);
        allocationData.writeBytes(textData);

        LibNetworkStack.LOGGER
            .info(connection + " Sending new ID " + newId + " -> " + netId.getPrintableName() + " " + path);
        connection.sendPacket(allocationData, ID_INTERNAL_ALLOCATE_STATIC, null, NetIdBase.MAXIMUM_PRIORITY);
        allocationData.release();
        return newId;
    }
}
