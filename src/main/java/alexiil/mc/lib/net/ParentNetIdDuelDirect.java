/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net;

public final class ParentNetIdDuelDirect<T> extends ParentNetIdDuel<T, T> {

    public ParentNetIdDuelDirect(ParentNetIdSingle<T> parent, String name) {
        super(parent, name, parent.clazz, 0);
    }

    @Override
    protected T extractParent(T value) {
        return value;
    }

    @Override
    protected void writeContext0(NetByteBuf buffer, IMsgWriteCtx ctx, T value) {
        // NO-OP
    }

    @Override
    protected T readContext(NetByteBuf buffer, IMsgReadCtx ctx, T parentValue) throws InvalidInputDataException {
        return parentValue;
    }
}
