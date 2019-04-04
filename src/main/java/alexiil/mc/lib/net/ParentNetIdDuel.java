package alexiil.mc.lib.net;

import java.util.List;

import io.netty.buffer.ByteBuf;

public abstract class ParentNetIdDuel<Parent, T> extends ParentNetIdSingle<T> {

    public final ParentNetIdSingle<Parent> parent;

    public ParentNetIdDuel(ParentNetIdSingle<Parent> parent, String name, Class<T> clazz, int length) {
        super(parent, clazz, name, length);
        this.parent = parent;
    }

    public ParentNetIdDuel(ParentNetIdSingle<Parent> parent, String name, Class<T> clazz) {
        this(parent, name, clazz, DYNAMIC_LENGTH);
    }

    @Override
    protected String getPrintableName() {
        return getRealClassName() + "<" + parent.clazz.getSimpleName() + ", " + clazz.getSimpleName() + ">";
    }

    @Override
    public final void writeContext(ByteBuf buffer, IMsgWriteCtx ctx, T value) {
        Parent p = extractParent(value);
        parent.writeContext(buffer, ctx, p);
        writeContext0(buffer, ctx, value);
    }

    @Override
    public void writeDynamicContext(ByteBuf buffer, IMsgWriteCtx ctx, T value, List<TreeNetIdBase> resolvedPath) {
        Parent p = extractParent(value);
        parent.writeDynamicContext(buffer, ctx, p, resolvedPath);
        writeContext0(buffer, ctx, value);
        resolvedPath.add(this);
    }

    protected abstract Parent extractParent(T value);

    protected abstract void writeContext0(ByteBuf buffer, IMsgWriteCtx ctx, T value);

    @Override
    public final T readContext(ByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException {
        Parent p = parent.readContext(buffer, ctx);
        if (p == null) {
            return null;
        }
        return readContext(buffer, ctx, p);
    }

    /** @return The read value.
     * @throws InvalidInputDataException if the byte buffer contained invalid data. */
    protected abstract T readContext(ByteBuf buffer, IMsgReadCtx ctx, Parent parentValue)
        throws InvalidInputDataException;
}
