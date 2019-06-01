package alexiil.mc.lib.net;

import java.util.List;
import java.util.function.Function;

import alexiil.mc.lib.net.DynamicNetLink.IDynamicLinkFactory;

public final class ParentDynamicNetId<P, C> extends ParentNetIdDuel<P, C> {

    public final DynamicNetId<C> childId;
    private final Function<P, C> childGetter;

    public ParentDynamicNetId(ParentNetIdSingle<P> parent, String name, DynamicNetId<C> childId,
        Function<P, C> childGetter) {
        super(parent, name, childId.clazz);
        this.childId = childId;
        this.childGetter = childGetter;
    }

    @Override
    TreeNetIdBase getChild(String childName) {
        if (childName.equals(childId.name)) {
            return childId;
        }
        return null;
    }

    @Override
    protected P extractParent(C value) {
        throw new IllegalStateException("Dynamic Net ID's must be written with the dynamic variant!");
    }

    @Override
    protected void writeContext0(NetByteBuf buffer, IMsgWriteCtx ctx, C value) {
        throw new IllegalStateException("Dynamic Net ID's must be written with the dynamic variant!");
    }

    @Override
    public void writeDynamicContext(NetByteBuf buffer, IMsgWriteCtx ctx, C value, List<TreeNetIdBase> resolvedPath) {
        throw new IllegalStateException("This should never be called by DynamicNetId!");
    }

    @Override
    protected C readContext(NetByteBuf buffer, IMsgReadCtx ctx, P parentValue) throws InvalidInputDataException {
        return childGetter.apply(parentValue);
    }

    public DynamicNetLink<P, C> link(P parent, C child) {
        return new DynamicNetLink<>(this, parent, child);
    }

    public IDynamicLinkFactory<C> linkFactory(P parent) {
        return child -> link(parent, child);
    }
}
