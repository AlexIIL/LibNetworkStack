package alexiil.mc.lib.net;

import java.util.HashSet;
import java.util.Set;

import javax.swing.JLabel;

import io.netty.buffer.ByteBuf;

import buildcraft.api.core.InvalidInputDataException;

import buildcraft.lib.net_new.IMsgCtx.NetworkSide;

public class InternalMsgUtil {

    private static final int ID_INTERNAL_ALLOCATE_STATIC = 0;

    public static final int COUNT_HARDCODED_IDS = 1;

    /** @param buffer All of the data for a single packet. It must be complete! */
    public static void onReceive(ConnectionType connection, ByteBuf buffer) throws InvalidInputDataException {
        int id = buffer.readInt();
        switch (id) {
            case ID_INTERNAL_ALLOCATE_STATIC: {
                int parent = buffer.readInt();
                if (parent == 0) {
                    // No parent
                } else {

                }
                break;
            }
            default: {
                if (id < 0 || id >= connection.allAllocatedIds.size()) {
                    throw new InvalidInputDataException("");
                }
                NetIdBase netId = connection.allAllocatedIds.get(id);
                MessageContext.Read ctx = new MessageContext.Read(NetworkSide.SLAVE, netId);
                Set<ParentNetIdTyped<?>> missingKeys = null;
                for (int i = 0; i < netId.allKeys.length; i++) {
                    ParentNetIdKey<?> key = netId.allKeys[i];
                    if (!key.readContext(buffer, ctx)) {
                        // Assume that all of the next keys depend on this key
                        if (missingKeys == null) {
                            missingKeys = new HashSet<>();
                        }
                        missingKeys.add(key);
                    }
                }
                if (netId.receivingThread == NETWORK_THREAD) {
                    if (missingKeys == null) {
                        netId.receive(buffer, ctx);
                    } else {
                        netId.handleMissingKeys(buffer, ctx, missingKeys);
                    }
                } else {
                    // TODO: Add some way to move this to the main thread!
                }
            }
        }
    }

    public static void sendId(ConnectionType connection, String todo, JLabel todo_as_well) {

    }

    public static void sendRawBytes(ConnectionType connection, ByteBuf buffer) {
        // This would do something if a class actually existed to handle this
    }
}
