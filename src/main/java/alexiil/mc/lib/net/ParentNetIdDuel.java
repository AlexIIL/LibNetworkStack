/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net;

import java.util.List;

public abstract class ParentNetIdDuel<Parent, T> extends ParentNetIdSingle<T> {

    public final ParentNetIdSingle<Parent> parent;

    public ParentNetIdDuel(ParentNetIdSingle<Parent> parent, String name, Class<T> clazz, int length) {
        super(parent, clazz, name, length);
        this.parent = parent;
        if (!(this instanceof ResolvedParentNetId) && !(this instanceof ResolvedDynamicNetId)) {
            parent.addChild(this);
        }
    }

    public ParentNetIdDuel(ParentNetIdSingle<Parent> parent, String name, Class<T> clazz) {
        this(parent, name, clazz, DYNAMIC_LENGTH);
    }

    @Override
    protected String getPrintableName() {
        return getRealClassName() + "<" + parent.clazz.getSimpleName() + ", " + clazz.getSimpleName() + ">";
    }

    @Override
    protected final void writeContext(NetByteBuf buffer, IMsgWriteCtx ctx, T value) {
        writeContext0(buffer, ctx, value);
    }

    @Override
    final void writeContextCall(CheckingNetByteBuf buffer, IMsgWriteCtx ctx, T value) {
        parent.writeContextCall(buffer, ctx, extractParent(value));
        super.writeContextCall(buffer, ctx, value);
    }

    @Override
    protected void writeDynamicContext(
        CheckingNetByteBuf buffer, IMsgWriteCtx ctx, T value, List<TreeNetIdBase> resolvedPath
    ) {
        parent.writeDynamicContext(buffer, ctx, extractParent(value), resolvedPath);
        resolvedPath.add(this);
        if (buffer.hasTypeData()) {
            buffer.writeMarkerId(InternalMsgUtil.getWriteId(ctx.getConnection(), this, new NetIdPath(resolvedPath)));
        }
        writeContext0(buffer, ctx, value);
    }

    protected abstract Parent extractParent(T value);

    protected abstract void writeContext0(NetByteBuf buffer, IMsgWriteCtx ctx, T value);

    @Override
    protected final T readContext(NetByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException {
        throw new UnsupportedOperationException("Call readContextCall instead!");
    }

    @Override
    final T readContextCall(CheckingNetByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException {
        Parent p = parent.readContextCall(buffer, ctx);
        if (p == null) {
            return null;
        }
        if (!(this instanceof ParentDynamicNetId) && !(this instanceof ResolvedDynamicNetId)) {
            buffer.readMarkerId(ctx, this);
        }
        return readContext(buffer, ctx, p);
    }

    /** @return The read value.
     * @throws InvalidInputDataException if the byte buffer contained invalid data. */
    protected abstract T readContext(NetByteBuf buffer, IMsgReadCtx ctx, Parent parentValue)
        throws InvalidInputDataException;
}
