/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net;

public interface IMsgReadCtx extends IMsgCtx {

    default void assertClientSide() throws InvalidInputDataException {
        if (isServerSide()) {
            throw new InvalidInputDataException("Cannot read " + getNetId() + " on the server!");
        }
    }

    default void assertServerSide() throws InvalidInputDataException {
        if (isClientSide()) {
            throw new InvalidInputDataException("Cannot read " + getNetId() + " on the client!");
        }
    }

    /** Informs the supplier that this packet has been dropped (for any reason) and so there might be unread data left
     * in the buffer. */
    default void drop() {
        drop("");
    }

    /** Informs the supplier that this packet has been dropped (for the given reason) and so there might be unread data
     * left in the buffer. */
    void drop(String reason);
}
