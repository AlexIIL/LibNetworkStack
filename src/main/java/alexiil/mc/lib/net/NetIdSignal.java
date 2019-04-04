package alexiil.mc.lib.net;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public final class NetIdSignal extends NetIdBase {

    @FunctionalInterface
    public interface IMsgSignalReceiver {
        void handle(IMsgReadCtx ctx);
    }

    private IMsgSignalReceiver receiver;

    public NetIdSignal(ParentNetId parent, String name) {
        super(parent, name, 0);
    }

    public NetIdSignal setReceiver(IMsgSignalReceiver receiver) {
        this.receiver = receiver;
        return this;
    }

    @Override
    public boolean receive(ByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException {
        if (receiver != null) {
            receiver.handle(ctx);
        }
        return true;
    }

    /** Sends this signal over the specified connection */
    public void send(ActiveConnection connection) {
        InternalMsgUtil.send(connection, this, path, Unpooled.EMPTY_BUFFER);
    }
}
