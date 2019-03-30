package alexiil.mc.lib.net;

import io.netty.buffer.ByteBuf;

import buildcraft.api.core.InvalidInputDataException;

public class ParentNetIdKey<T> extends ParentNetIdTyped<T> {
    public final NetKeyMapper<T> mapper;

    public ParentNetIdKey(ParentNetId parent, String name, NetKeyMapper<T> mapper) {
        super(parent, name, mapper.clazz, mapper.length);
        this.mapper = mapper;
    }

    public boolean readContext(ByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException {
        T value = mapper.read(buffer, ctx);
        ctx.putKey(this, value);
        return value != null;
    }

    public void writeContext(ByteBuf buffer, IMsgWriteCtx ctx) {
        mapper.write(buffer, ctx, ctx.getKey(this));
    }
}
