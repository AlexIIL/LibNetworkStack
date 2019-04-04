package alexiil.mc.lib.net;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import io.netty.buffer.ByteBuf;

public abstract class ParentNetIdSingle<T> extends ParentNetIdBase {

    public final Class<T> clazz;

    public ParentNetIdSingle(ParentNetIdBase parent, Class<T> clazz, String name, int thisLength) {
        super(parent, name, thisLength);
        this.clazz = clazz;
    }

    @Override
    protected String getPrintableName() {
        return getRealClassName() + "<" + clazz.getSimpleName() + ">";
    }

    public NetIdDataK<T> idData(String name) {
        return new NetIdDataK<>(this, name, TreeNetIdBase.DYNAMIC_LENGTH);
    }

    public NetIdDataK<T> idData(String name, int dataLength) {
        return new NetIdDataK<>(this, name, dataLength);
    }

    public NetIdSignalK<T> idSignal(String name) {
        return new NetIdSignalK<>(this, name);
    }

    public <U extends T> ParentNetIdCast<T, U> subType(Class<U> subClass, String subName) {
        return new ParentNetIdCast<>(this, subName, subClass);
    }

    public <U> ParentNetIdExtractor<T, U> extractor(Class<U> targetClass, String subName, Function<U, T> forward,
        Function<T, U> backward) {
        return new ParentNetIdExtractor<>(this, subName, targetClass, forward, backward);
    }

    /** @return The read value, or null if the parent couldn't be read.
     * @throws InvalidInputDataException if the byte buffer contained invalid data. */
    public abstract T readContext(ByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException;

    public abstract void writeContext(ByteBuf buffer, IMsgWriteCtx ctx, T value);

    public void writeDynamicContext(ByteBuf buffer, IMsgWriteCtx ctx, T value, List<TreeNetIdBase> resolvedPath) {
        writeContext(buffer, ctx, value);
        Collections.addAll(resolvedPath, this.path.array);
    }
}
