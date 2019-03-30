package alexiil.mc.lib.net;

import io.netty.buffer.ByteBuf;

import buildcraft.api.core.InvalidInputDataException;

public class NetIdSignalK<T> extends NetIdBase {

    @FunctionalInterface
    public interface IMsgSignalReceiverK<T> {
        void handle(T obj, IMsgReadCtx ctx);
    }

    private IMsgSignalReceiverK<T> receiver;

    public final ParentNetIdTyped<T> key;

    public NetIdSignalK(ParentNetIdTyped<T> parent, String name) {
        this(parent, name, parent);
    }

    public NetIdSignalK(ParentNetId parent, String name, ParentNetIdTyped<T> key) {
        super(parent, name, 0);
        if (!parent.isParent(key)) {
            throw new IllegalArgumentException("");
        }
        this.key = key;
    }

    public NetIdSignalK<T> setReceiver(IMsgSignalReceiverK<T> receiver) {
        this.receiver = receiver;
        return this;
    }

    public void sendSimple(T obj) {
        // also todo: how should player connections work?
        IMsgWriteCtx ctx = new MessageContext.Write(null, this);
        ctx.putKey(key, obj);
        sendFull(ctx);
    }

    public void sendFull(IMsgWriteCtx ctx) {
        // TODO!
    }

    @Override
    public void receive(ByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException {
        if (receiver != null) {
            T obj = ctx.getKey(key);
            receiver.handle(obj, ctx);
        }
    }
}
