package alexiil.mc.lib.net;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import com.google.common.collect.ImmutableSet;

import io.netty.buffer.ByteBuf;

public final class DynamicNetId<T> extends ParentNetIdSingle<T> {

    public static final class ParentData<T> {
        public final ParentNetIdSingle<T> parent;
        public final T data;

        public ParentData(ParentNetIdSingle<T> parent, T data) {
            this.parent = parent;
            this.data = data;
        }

        public void writeDynamicContext(ByteBuf buffer, IMsgWriteCtx ctx, List<TreeNetIdBase> resolvedPath) {
            parent.writeDynamicContext(buffer, ctx, data, resolvedPath);
        }
    }

    final Function<T, ParentData<?>> parentExtractor;
    final Function<Object, T> childExtractor;
    final Set<ParentNetIdSingle<?>> possibleParents;

    public DynamicNetId(Class<T> clazz, String name, Function<T, ParentData<?>> parentExtractor,
        Function<Object, T> childExtractor, ParentNetIdSingle<?>... possibleParents) {
        super(null, clazz, name, 0);
        this.parentExtractor = parentExtractor;
        this.childExtractor = childExtractor;
        this.possibleParents = ImmutableSet.copyOf(possibleParents);
        for (ParentNetIdSingle<?> p : possibleParents) {
            p.addChild(this);
        }
    }

    @Override
    public T readContext(ByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException {
        throw new IllegalStateException("Dynamic Net ID's must be fully resolved before they can be read!");
    }

    @Override
    public void writeContext(ByteBuf buffer, IMsgWriteCtx ctx, T value) {
        throw new IllegalStateException("Dynamic Net ID's must be written with the dynamic variant!");
    }

    @Override
    public void writeDynamicContext(ByteBuf buffer, IMsgWriteCtx ctx, T value, List<TreeNetIdBase> resolvedPath) {
        ParentData<?> data = parentExtractor.apply(value);
        assert possibleParents.contains(data.parent);
        data.writeDynamicContext(buffer, ctx, resolvedPath);
        resolvedPath.add(this);
    }
}
