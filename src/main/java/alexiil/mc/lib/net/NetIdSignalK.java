package alexiil.mc.lib.net;

import java.util.ArrayList;
import java.util.List;

public final class NetIdSignalK<T> extends NetIdTyped<T> {

    @FunctionalInterface
    public interface IMsgSignalReceiverK<T> {
        void handle(T obj, IMsgReadCtx ctx) throws InvalidInputDataException;
    }

    private IMsgSignalReceiverK<T> receiver;

    public NetIdSignalK(ParentNetIdSingle<T> parent, String name) {
        super(parent, name, 0);
    }

    public NetIdSignalK<T> setReceiver(IMsgSignalReceiverK<T> receiver) {
        this.receiver = receiver;
        return this;
    }

    @Override
    protected void receive(NetByteBuf buffer, IMsgReadCtx ctx, T obj) throws InvalidInputDataException {
        if (receiver != null) {
            receiver.handle(obj, ctx);
        }
    }

    /** Sends this signal over the specified connection */
    @Override
    public void send(ActiveConnection connection, T obj) {
        NetByteBuf buffer = hasFixedLength() ? NetByteBuf.buffer(totalLength) : NetByteBuf.buffer();
        MessageContext.Write ctx = new MessageContext.Write(connection, this);
        if (parent.pathContainsDynamicParent) {
            List<TreeNetIdBase> nPath = new ArrayList<>();
            parent.writeDynamicContext(buffer, ctx, obj, nPath);
            nPath.add(this);
            TreeNetIdBase[] array = nPath.toArray(new TreeNetIdBase[0]);
            InternalMsgUtil.send(connection, this, new NetIdPath(array), buffer);
        } else {
            parent.writeContext(buffer, ctx, obj);
            InternalMsgUtil.send(connection, this, path, buffer);
        }
        buffer.release();
    }
}
