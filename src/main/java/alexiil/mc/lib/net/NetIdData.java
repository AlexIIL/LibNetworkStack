package alexiil.mc.lib.net;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class NetIdData extends NetIdBase {
    @FunctionalInterface
    public interface IMsgDataReceiver {
        void receive(ByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException;
    }

    @FunctionalInterface
    public interface IMsgDataWriter {
        void write(ByteBuf buffer, IMsgWriteCtx ctx);
    }

    private IMsgDataReceiver receiver = (buffer, ctx) -> {
        throw new InvalidInputDataException("No receiver set!");
    };
    private IMsgDataWriter writer;

    public NetIdData(ParentNetId parent, String name, int length) {
        super(parent, name, length);
    }

    public NetIdData setReceiver(IMsgDataReceiver receiver) {
        this.receiver = receiver;
        return this;
    }

    public NetIdData setReadWrite(IMsgDataReceiver receiver, IMsgDataWriter writer) {
        this.receiver = receiver;
        this.writer = writer;
        return this;
    }

    @Override
    public boolean receive(ByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException {
        receiver.receive(buffer, ctx);
        return true;
    }

    /** Sends this signal over the specified connection */
    public void send(ActiveConnection connection) {
        send(connection, writer);
    }

    public void send(ActiveConnection connection, IMsgDataWriter writer) {
        ByteBuf buffer = hasFixedLength() ? Unpooled.buffer(totalLength) : Unpooled.buffer();
        MessageContext.Write ctx = new MessageContext.Write(connection, this);
        writer.write(buffer, ctx);
        InternalMsgUtil.send(connection, this, path, buffer);
        buffer.release();
    }
}
