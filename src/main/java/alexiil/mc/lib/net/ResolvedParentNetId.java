package alexiil.mc.lib.net;

import io.netty.buffer.ByteBuf;

final class ResolvedParentNetId<Parent, T> extends ParentNetIdDuel<Parent, T> {

    public final DynamicNetId<T> dynamicId;

    ResolvedParentNetId(ParentNetIdSingle<Parent> parent, DynamicNetId<T> dynamicId) {
        super(parent, dynamicId.name, dynamicId.clazz, 0);
        this.dynamicId = dynamicId;
    }

    @Override
    protected Parent extractParent(T value) {
        throw new IllegalStateException("ResolvedParentNetId must only be used for reading!");
    }

    @Override
    protected void writeContext0(ByteBuf buffer, IMsgWriteCtx ctx, T value) {
        throw new IllegalStateException("ResolvedParentNetId must only be used for reading!");
    }

    @Override
    protected T readContext(ByteBuf buffer, IMsgReadCtx ctx, Parent parentValue) throws InvalidInputDataException {
        return dynamicId.childExtractor.apply(parentValue);
    }
}
