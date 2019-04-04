package alexiil.mc.lib.net;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class NetIdDataK<T> extends NetIdTyped<T> {

    @FunctionalInterface
    public interface IMsgDataReceiverK<T> {
        void receive(T obj, ByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException;
    }

    @FunctionalInterface
    public interface IMsgDataWriterK<T> {
        void write(T obj, ByteBuf buffer, IMsgWriteCtx ctx);
    }

    private IMsgDataReceiverK<T> receiver = (t, buffer, ctx) -> {
        throw new InvalidInputDataException("No receiver set for " + ctx.getNetSide());
    };
    private IMsgDataWriterK<T> writer;

    public NetIdDataK(ParentNetIdSingle<T> parent, String name, int length) {
        super(parent, name, length);
    }

    public NetIdDataK<T> setReceiver(IMsgDataReceiverK<T> receiver) {
        this.receiver = receiver;
        return this;
    }

    public NetIdDataK<T> setReadWrite(IMsgDataReceiverK<T> receiver, IMsgDataWriterK<T> writer) {
        this.receiver = receiver;
        this.writer = writer;
        return this;
    }

    @Override
    public void receive(ByteBuf buffer, IMsgReadCtx ctx, T parentValue) throws InvalidInputDataException {
        receiver.receive(parentValue, buffer, ctx);
    }

    /** Sends this signal over the specified connection */
    public void send(ActiveConnection connection, T obj) {
        send(connection, obj, writer);
    }

    public void send(ActiveConnection connection, T obj, IMsgDataWriterK<T> writer) {
        ByteBuf buffer = hasFixedLength() ? Unpooled.buffer(totalLength) : Unpooled.buffer();
        MessageContext.Write ctx = new MessageContext.Write(connection, this);
        parent.writeContext(buffer, ctx, obj);
        writer.write(obj, buffer, ctx);
        InternalMsgUtil.send(connection, this, path, buffer);
        buffer.release();
    }
}
