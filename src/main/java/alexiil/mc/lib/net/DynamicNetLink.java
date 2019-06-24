/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net;

/** The container object
 * 
 * @param <P> The parent type.
 * @param <C> The child type. */
public final class DynamicNetLink<P, C> {

    @FunctionalInterface
    public interface IDynamicLinkFactory<C> {
        DynamicNetLink<?, C> create(C child);
    }

    public final ParentDynamicNetId<P, C> parentId;
    public final P parent;
    public final C child;

    public DynamicNetLink(ParentDynamicNetId<P, C> parentId, P parent, C child) {
        this.parentId = parentId;
        this.parent = parent;
        this.child = child;
    }
}
