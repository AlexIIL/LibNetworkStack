/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net;

public final class ParentNetIdCast<Super, Sub extends Super> extends ParentNetIdDuel<Super, Sub> {

    public ParentNetIdCast(ParentNetIdSingle<Super> parent, String name, Class<Sub> clazz) {
        super(parent, name, clazz, 0);
    }

    @Override
    protected Super extractParent(Sub value) {
        return value;
    }

    @Override
    protected void writeContext0(NetByteBuf buffer, IMsgWriteCtx ctx, Sub value) {
        // Nothing to write
    }

    @Override
    protected Sub readContext(NetByteBuf buffer, IMsgReadCtx ctx, Super parentValue) throws InvalidInputDataException {
        if (clazz.isInstance(parentValue)) {
            return clazz.cast(parentValue);
        } else {
            return null;
        }
    }
}
