/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net;

public interface IMsgWriteCtx extends IMsgCtx {

    default void assertClientSide() {
        if (isServerSide()) {
            throw new IllegalStateException("Cannot write " + getNetId() + " on the server!");
        }
    }

    default void assertServerSide() {
        if (isClientSide()) {
            throw new IllegalStateException("Cannot write " + getNetId() + " on the client!");
        }
    }
}
