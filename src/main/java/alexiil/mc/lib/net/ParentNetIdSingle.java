package alexiil.mc.lib.net;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public abstract class ParentNetIdSingle<T> extends ParentNetIdBase {

    public final Class<T> clazz;

    final Map<String, NetIdTyped<T>> leafChildren = new HashMap<>();
    final Map<String, ParentNetIdDuel<T, ?>> branchChildren = new HashMap<>();

    ParentNetIdSingle(ParentNetIdBase parent, Class<T> clazz, String name, int thisLength) {
        super(parent, name, thisLength);
        this.clazz = clazz;
    }

    public ParentNetIdSingle(ParentNetId parent, Class<T> clazz, String name, int thisLength) {
        super(parent, name, thisLength);
        this.clazz = clazz;
        if (parent != null) {
            parent.addChild(this);
        }
    }

    void addChild(NetIdTyped<T> netId) {
        if (this instanceof ParentDynamicNetId) {
            throw new IllegalArgumentException("ParentDynamicNetId can only have 1 child!");
        }
        if (leafChildren.containsKey(netId.name) || branchChildren.containsKey(netId.name)) {
            throw new IllegalStateException("There is already a child with the name " + netId.name + "!");
        }
        leafChildren.put(netId.name, netId);
    }

    void addChild(ParentNetIdDuel<T, ?> netId) {
        if (this instanceof ParentDynamicNetId) {
            throw new IllegalArgumentException("ParentDynamicNetId can only have 1 child!");
        }
        if (leafChildren.containsKey(netId.name) || branchChildren.containsKey(netId.name)) {
            throw new IllegalStateException("There is already a child with the name " + netId.name + "!");
        }
        branchChildren.put(netId.name, netId);
    }

    @Override
    TreeNetIdBase getChild(String childName) {
        NetIdTyped<T> leaf = leafChildren.get(childName);
        return leaf != null ? leaf : branchChildren.get(childName);
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
    public abstract T readContext(NetByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException;

    public abstract void writeContext(NetByteBuf buffer, IMsgWriteCtx ctx, T value);

    public void writeDynamicContext(NetByteBuf buffer, IMsgWriteCtx ctx, T value, List<TreeNetIdBase> resolvedPath) {
        writeContext(buffer, ctx, value);
        Collections.addAll(resolvedPath, this.path.array);
    }
}
