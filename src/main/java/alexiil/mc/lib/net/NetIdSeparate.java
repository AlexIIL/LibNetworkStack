/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net;

public abstract class NetIdSeparate extends NetIdBase {
    NetIdSeparate(ParentNetId parent, String name, int length) {
        super(parent, name, length);
        parent.addChild(this);
    }
}
