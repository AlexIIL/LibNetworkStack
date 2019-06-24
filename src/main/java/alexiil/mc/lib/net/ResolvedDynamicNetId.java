/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net;

final class ResolvedDynamicNetId<T> extends ParentNetIdDuel<T, T> {

    final DynamicNetId<T> wrapped;

    public ResolvedDynamicNetId(ParentNetIdSingle<T> parent, DynamicNetId<T> wrapped) {
        super(parent, "__unused!!", wrapped.clazz, 0);
        this.wrapped = wrapped;
    }

    @Override
    TreeNetIdBase getChild(String childName) {
        return wrapped.getChild(childName);
    }

    @Override
    protected T extractParent(T value) {
        return value;
    }

    @Override
    protected void writeContext0(NetByteBuf buffer, IMsgWriteCtx ctx, T value) {
        throw new IllegalStateException("Resolved Net ID's must never be written as they are only for reading!");
    }

    @Override
    protected T readContext(NetByteBuf buffer, IMsgReadCtx ctx, T parentValue) throws InvalidInputDataException {
        return parentValue;
    }
}
