package alexiil.mc.lib.net;

import io.netty.buffer.ByteBuf;

public final class ParentNetIdCast<Super, Sub extends Super> extends ParentNetIdDuel<Super, Sub> {

    public ParentNetIdCast(ParentNetIdSingle<Super> parent, String name, Class<Sub> clazz) {
        super(parent, name, clazz, 0);
    }

    @Override
    protected Super extractParent(Sub value) {
        return value;
    }

    @Override
    protected void writeContext0(ByteBuf buffer, IMsgWriteCtx ctx, Sub value) {
        // Nothing to write
    }

    @Override
    protected Sub readContext(ByteBuf buffer, IMsgReadCtx ctx, Super parentValue) throws InvalidInputDataException {
        if (clazz.isInstance(parentValue)) {
            return clazz.cast(parentValue);
        } else {
            return null;
        }
    }
}
