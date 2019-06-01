package alexiil.mc.lib.net;

import java.util.function.Function;

public final class ParentNetIdExtractor<Parent, T> extends ParentNetIdDuel<Parent, T> {

    private final Function<T, Parent> forward;
    private final Function<Parent, T> backward;

    public ParentNetIdExtractor(ParentNetIdSingle<Parent> parent, String name, Class<T> clazz,
        Function<T, Parent> forward, Function<Parent, T> backward) {
        super(parent, name, clazz, 0);
        this.forward = forward;
        this.backward = backward;
    }

    @Override
    protected Parent extractParent(T value) {
        return forward.apply(value);
    }

    @Override
    protected void writeContext0(NetByteBuf buffer, IMsgWriteCtx ctx, T value) {
        // NO-OP
    }

    @Override
    protected T readContext(NetByteBuf buffer, IMsgReadCtx ctx, Parent parentValue) throws InvalidInputDataException {
        return backward.apply(parentValue);
    }
}
