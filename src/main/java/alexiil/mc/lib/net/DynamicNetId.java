/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net;

import java.util.List;

public final class DynamicNetId<T> extends ParentNetIdSingle<T> {

    @FunctionalInterface
    public interface LinkAccessor<T> {
        DynamicNetLink<?, T> getLink(T value);
    }

    final LinkAccessor<T> linkGetter;

    public DynamicNetId(Class<T> clazz, LinkAccessor<T> linkGetter) {
        super(null, clazz, "dynamic", DYNAMIC_LENGTH);
        this.linkGetter = linkGetter;
    }

    @Override
    protected T readContext(NetByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException {
        throw new IllegalStateException("Dynamic Net ID's must be fully resolved before they can be read!");
    }

    @Override
    protected void writeContext(NetByteBuf buffer, IMsgWriteCtx ctx, T value) {
        throw new IllegalStateException("Dynamic Net ID's must be written with the dynamic variant!");
    }

    @Override
    protected void writeDynamicContext(
        CheckingNetByteBuf buffer, IMsgWriteCtx ctx, T value, List<TreeNetIdBase> resolvedPath
    ) {
        writeParent(buffer, ctx, linkGetter.getLink(value), resolvedPath);
        resolvedPath.add(this);
    }

    private <P> void writeParent(
        CheckingNetByteBuf buffer, IMsgWriteCtx ctx, DynamicNetLink<P, T> link, List<TreeNetIdBase> resolvedPath
    ) {
        assert link.parentId.childId == this;
        link.parentId.parent.writeDynamicContext(buffer, ctx, link.parent, resolvedPath);
        resolvedPath.add(link.parentId);
    }
}
