package alexiil.mc.lib.net;

final class ResolvedParentNetId<Parent, T> extends ParentNetIdDuel<Parent, T> {

    public final ParentNetIdDuel<Parent, T> reader;

    ResolvedParentNetId(ParentNetIdSingle<Parent> parent, ParentNetIdDuel<Parent, T> reader) {
        super(parent, reader.name, reader.clazz, reader.length);
        this.reader = reader;
    }

    @Override
    TreeNetIdBase getChild(String childName) {
        return reader.getChild(childName);
    }

    @Override
    protected Parent extractParent(T value) {
        throw new IllegalStateException("ResolvedParentNetId must only be used for reading!");
    }

    @Override
    protected void writeContext0(NetByteBuf buffer, IMsgWriteCtx ctx, T value) {
        throw new IllegalStateException("ResolvedParentNetId must only be used for reading!");
    }

    @Override
    protected T readContext(NetByteBuf buffer, IMsgReadCtx ctx, Parent parentValue) throws InvalidInputDataException {
        return reader.readContext(buffer, ctx, parentValue);
    }
}
