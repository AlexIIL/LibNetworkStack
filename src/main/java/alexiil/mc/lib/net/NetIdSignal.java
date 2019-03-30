package alexiil.mc.lib.net;

import io.netty.buffer.ByteBuf;

import buildcraft.api.core.InvalidInputDataException;

public class NetIdSignal extends NetIdBase {

    @FunctionalInterface
    public interface IMsgSignalReceiver {
        void handle(IMsgReadCtx ctx);
    }

    private IMsgSignalReceiver receiver;

    public NetIdSignal(ParentNetId parent, String name) {
        super(parent, name, 0);
    }

    public void setReceiver(IMsgSignalReceiver receiver) {
        this.receiver = receiver;
    }

    @Override
    protected void send(ConnectionType connection, ByteBuf buf, IMsgWriteCtx ctx) {
        // TODO Auto-generated method stub
        throw new AbstractMethodError("// TODO: Implement this!");
    }

    @Override
    public void receive(ByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException {
        if (receiver != null) {
            receiver.handle(ctx);
        }
    }
}
