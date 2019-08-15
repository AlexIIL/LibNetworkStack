/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net;

import javax.annotation.Nullable;

/** The base type for all parent nodes - that is nodes used for organising the data that is sent (or for writing header
 * information), but you can't send or receive data through this. */
public abstract class ParentNetIdBase extends TreeNetIdBase {

    ParentNetIdBase(ParentNetIdBase parent, String name, int thisLength) {
        super(parent, name, thisLength);
    }

    @Override
    int getFinalFlags() {
        return NetIdBase.FLAG_IS_PARENT;
    }

    @Nullable
    abstract TreeNetIdBase getChild(String childName);
}
