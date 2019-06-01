package alexiil.mc.lib.net;

public final class NetIdData extends NetIdSeparate {

    @FunctionalInterface
    public interface IMsgDataReceiver {
        void receive(NetByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException;
    }

    @FunctionalInterface
    public interface IMsgDataWriter {
        void write(NetByteBuf buffer, IMsgWriteCtx ctx);
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
    public boolean receive(NetByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException {
        receiver.receive(buffer, ctx);
        return true;
    }

    /** Sends this signal over the specified connection */
    public void send(ActiveConnection connection) {
        send(connection, writer);
    }

    public void send(ActiveConnection connection, IMsgDataWriter writer) {
        NetByteBuf buffer = hasFixedLength() ? NetByteBuf.buffer(totalLength) : NetByteBuf.buffer();
        MessageContext.Write ctx = new MessageContext.Write(connection, this);
        writer.write(buffer, ctx);
        if (buffer.readableBytes() > 0) {
            // Only send data packets if anything was actually written.
            InternalMsgUtil.send(connection, this, path, buffer);
        }
        buffer.release();
    }
}
