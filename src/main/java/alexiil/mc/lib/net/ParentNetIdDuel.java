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
        Parent p = extractParent(value);
        parent.writeContext(buffer, ctx, p);
        writeContext0(buffer, ctx, value);
    }

    @Override
    protected void writeDynamicContext(NetByteBuf buffer, IMsgWriteCtx ctx, T value, List<TreeNetIdBase> resolvedPath) {
        Parent p = extractParent(value);
        parent.writeDynamicContext(buffer, ctx, p, resolvedPath);
        writeContext0(buffer, ctx, value);
        resolvedPath.add(this);
    }

    protected abstract Parent extractParent(T value);

    protected abstract void writeContext0(NetByteBuf buffer, IMsgWriteCtx ctx, T value);

    @Override
    protected final T readContext(NetByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException {
        Parent p = parent.readContext(buffer, ctx);
        if (p == null) {
            return null;
        }
        return readContext(buffer, ctx, p);
    }

    /** @return The read value.
     * @throws InvalidInputDataException if the byte buffer contained invalid data. */
    protected abstract T readContext(NetByteBuf buffer, IMsgReadCtx ctx, Parent parentValue)
        throws InvalidInputDataException;
}
