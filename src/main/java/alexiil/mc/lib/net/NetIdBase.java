package alexiil.mc.lib.net;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.netty.buffer.ByteBuf;

import buildcraft.api.core.InvalidInputDataException;

/** A leaf node that will send and receive messages. */
public abstract class NetIdBase extends TreeNetIdBase {

    public final ParentNetId parent;

    final ParentNetIdKey<?>[] allKeys;

    NetIdBase(ParentNetId parent, String name, int length) {
        super(parent, name, length);
        this.parent = parent;
        allKeys = gatherKeys(parent);
    }

    /* public final void send(ConnectionInfo connection) { } protected abstract void send(ConnectionInfo connection,
     * ByteBuf buf, IMsgWriteCtx ctx); */

    private static ParentNetIdKey<?>[] gatherKeys(ParentNetId parent) {
        List<ParentNetIdKey<?>> keys = new ArrayList<>();

        while (parent != null) {
            if (parent instanceof ParentNetIdKey<?>) {
                keys.add(0, (ParentNetIdKey<?>) parent);
            }
            parent = parent.parent;
        }
        return keys.toArray(new ParentNetIdKey<?>[0]);
    }

    public abstract void receive(ByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException;

    public abstract void handleMissingKeys(ByteBuf buffer, IMsgReadCtx ctx, Set<ParentNetIdTyped<?>> notFoundKeys)
        throws InvalidInputDataException;
}
