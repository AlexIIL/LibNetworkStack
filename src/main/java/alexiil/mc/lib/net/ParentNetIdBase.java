/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net;

import javax.annotation.Nullable;

public abstract class ParentNetIdBase extends TreeNetIdBase {

    ParentNetIdBase(ParentNetIdBase parent, String name, int thisLength) {
        super(parent, name, thisLength);
    }

    @Override
    int getFlags() {
        return NetIdBase.FLAG_IS_PARENT;
    }

    @Nullable
    abstract TreeNetIdBase getChild(String childName);
}
