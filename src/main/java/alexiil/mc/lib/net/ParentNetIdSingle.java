/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** A {@link ParentNetIdBase parent node} that writes out the context for the receiver to obtain the same object.
 * 
 * @param <T> The type of object that will be written and read. This must not have any generic type parameters itself:
 *            as it must be representable with a {@link Class}. */
public abstract class ParentNetIdSingle<T> extends ParentNetIdBase {

    /** The type of the object to be written and read. */
    public final Class<T> clazz;

    final Map<String, NetIdTyped<T>> leafChildren = new HashMap<>();
    final Map<String, ParentNetIdDuel<T, ?>> branchChildren = new HashMap<>();

    ParentNetIdSingle(ParentNetIdBase parent, Class<T> clazz, String name, int thisLength) {
        super(parent, name, thisLength);
        this.clazz = clazz;
    }

    /** @param parent The parent.
     * @param clazz The type of the object to be written and read.
     * @param name The name. (See
     * @param thisLength */
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

    public <U> ParentNetIdExtractor<T, U> extractor(
        Class<U> targetClass, String subName, Function<U, T> forward, Function<T, U> backward
    ) {
        return new ParentNetIdExtractor<>(this, subName, targetClass, forward, backward);
    }

    public ParentNetIdSingle<T> child(String name) {
        return new ParentNetIdDuelDirect<>(this, name);
    }

    /** @return The read value, or null if the parent couldn't be read.
     * @throws InvalidInputDataException if the byte buffer contained invalid data. */
    protected abstract T readContext(NetByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException;

    protected abstract void writeContext(NetByteBuf buffer, IMsgWriteCtx ctx, T value);

    T readContextCall(CheckingNetByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException {
        buffer.readMarkerId(ctx, this);
        return readContext(buffer, ctx);
    }

    void writeContextCall(CheckingNetByteBuf buffer, IMsgWriteCtx ctx, T value) {
        if (buffer.hasTypeData()) {
            buffer.writeMarkerId(InternalMsgUtil.getWriteId(ctx.getConnection(), this, path));
        }
        writeContext(buffer, ctx, value);
    }

    protected void writeDynamicContext(
        CheckingNetByteBuf buffer, IMsgWriteCtx ctx, T value, List<TreeNetIdBase> resolvedPath
    ) {
        writeContextCall(buffer, ctx, value);
        Collections.addAll(resolvedPath, this.path.array);
    }

    public final void writeKey(CheckingNetByteBuf buffer, IMsgWriteCtx ctx, T value) {
        if (pathContainsDynamicParent) {
            int wIndex = buffer.writerIndex();
            buffer.writeInt(0);
            List<TreeNetIdBase> nPath = new ArrayList<>();
            writeDynamicContext(buffer, ctx, value, nPath);
            nPath.add(this);
            TreeNetIdBase[] array = nPath.toArray(new TreeNetIdBase[0]);
            NetIdPath resolvedPath = new NetIdPath(array);
            buffer.setInt(wIndex, InternalMsgUtil.getWriteId(ctx.getConnection(), this, resolvedPath));
        } else {
            writeContext(buffer, ctx, value);
        }
    }

    public final T readKey(CheckingNetByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException {
        if (!pathContainsDynamicParent) {
            // Static path
            return readContext(buffer, ctx);
        }
        ActiveConnection connection = ctx.getConnection();
        int pathId = buffer.readInt();
        if (pathId < 0 || pathId >= connection.readMapIds.size()) {
            throw new InvalidInputDataException("Unknown/invalid ID " + pathId);
        }
        TreeNetIdBase readId = connection.readMapIds.get(pathId);
        if (!(readId instanceof ParentNetIdSingle<?>)) {
            throw new InvalidInputDataException("Not a receiving node: " + readId + " for id " + pathId);
        }
        ParentNetIdSingle<?> op = (ParentNetIdSingle<?>) readId;
        Object read = op.readContext(buffer, ctx);
        if (!clazz.isInstance(read)) {
            throw new InvalidInputDataException(
                "The id sent to us (" + pathId + ") was for a node of a different class! (" + op + ")"
            );
        }
        return clazz.cast(read);
    }
}
