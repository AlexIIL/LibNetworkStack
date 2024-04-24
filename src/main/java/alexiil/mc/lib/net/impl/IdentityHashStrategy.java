/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net.impl;

import it.unimi.dsi.fastutil.Hash;

public class IdentityHashStrategy implements Hash.Strategy<Object> {
    private static final IdentityHashStrategy INSTANCE = new IdentityHashStrategy();
    
    @SuppressWarnings("unchecked")
    public static <T> Hash.Strategy<T> getInstance() {
        return (Hash.Strategy<T>) INSTANCE;
    }
    
    private IdentityHashStrategy() {}
    
    @Override
    public int hashCode(Object o) {
        return System.identityHashCode(o);
    }

    @Override
    public boolean equals(Object a, Object b) {
        return a == b;
    }
}
