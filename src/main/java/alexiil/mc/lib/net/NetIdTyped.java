package alexiil.mc.lib.net;

import io.netty.buffer.ByteBuf;

public abstract class NetIdTyped<T> extends NetIdBase {

    public final ParentNetIdSingle<T> parent;

    NetIdTyped(ParentNetIdSingle<T> parent, String name, int length) {
        super(parent, name, length);
        this.parent = parent;
    }

    @Override
    protected String getPrintableName() {
        return getRealClassName() + " <" + parent.clazz.getSimpleName() + ">";
    }

    @Override
    public final boolean receive(ByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException {
        T obj = parent.readContext(buffer, ctx);
        if (obj == null) {
            return false;
        } else {
            receive(buffer, ctx, obj);
            return true;
        }
    }

    protected abstract void receive(ByteBuf buffer, IMsgReadCtx ctx, T obj) throws InvalidInputDataException;
}
