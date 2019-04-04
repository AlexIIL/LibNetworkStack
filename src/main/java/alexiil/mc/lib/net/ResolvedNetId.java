package alexiil.mc.lib.net;

import io.netty.buffer.ByteBuf;

final class ResolvedNetId<T> extends NetIdTyped<T> {

    final NetIdTyped<?> wrapped;

    public ResolvedNetId(ParentNetIdSingle<T> parent, NetIdTyped<?> wrapped) {
        super(parent, wrapped.name, wrapped.length);
        this.wrapped = wrapped;
    }

    @Override
    protected void receive(ByteBuf buffer, IMsgReadCtx ctx, T obj) throws InvalidInputDataException {
        receive0(buffer, ctx, obj, wrapped);
    }

    private static <U> void receive0(ByteBuf buffer, IMsgReadCtx ctx, Object obj, NetIdTyped<U> wrapped)
        throws InvalidInputDataException {
        wrapped.receive(buffer, ctx, wrapped.parent.clazz.cast(obj));
    }
}
