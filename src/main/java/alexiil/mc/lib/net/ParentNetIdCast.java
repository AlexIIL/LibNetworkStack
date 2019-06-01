package alexiil.mc.lib.net;

public final class ParentNetIdCast<Super, Sub extends Super> extends ParentNetIdDuel<Super, Sub> {

    public ParentNetIdCast(ParentNetIdSingle<Super> parent, String name, Class<Sub> clazz) {
        super(parent, name, clazz, 0);
    }

    @Override
    protected Super extractParent(Sub value) {
        return value;
    }

    @Override
    protected void writeContext0(NetByteBuf buffer, IMsgWriteCtx ctx, Sub value) {
        // Nothing to write
    }

    @Override
    protected Sub readContext(NetByteBuf buffer, IMsgReadCtx ctx, Super parentValue) throws InvalidInputDataException {
        if (clazz.isInstance(parentValue)) {
            return clazz.cast(parentValue);
        } else {
            return null;
        }
    }
}
