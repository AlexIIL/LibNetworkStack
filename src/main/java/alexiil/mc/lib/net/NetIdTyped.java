/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net;

public abstract class NetIdTyped<T> extends NetIdBase {

    public final ParentNetIdSingle<T> parent;

    NetIdTyped(ParentNetIdSingle<T> parent, String name, int length) {
        super(parent, name, length);
        this.parent = parent;
        if (!(this instanceof ResolvedNetId)) {
            parent.addChild(this);
        }
    }

    @Override
    protected String getPrintableName() {
        return getRealClassName() + " <" + parent.clazz.getSimpleName() + ">";
    }

    @Override
    final boolean receive(NetByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException {
        T obj = parent.readContext(buffer, ctx);
        if (obj == null) {
            return false;
        } else {
            receive(buffer, ctx, obj);
            return true;
        }
    }

    protected abstract void receive(NetByteBuf buffer, IMsgReadCtx ctx, T obj) throws InvalidInputDataException;

    /** Sends this net id over the specified connection */
    public abstract void send(ActiveConnection connection, T obj);
}
