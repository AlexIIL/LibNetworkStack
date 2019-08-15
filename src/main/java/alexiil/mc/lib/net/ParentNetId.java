/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

/** A {@link ParentNetIdBase parent node} that doesn't write any header information for it's children: the static
 * context (given by the {@link IMsgReadCtx} or {@link IMsgWriteCtx}) should be enough. */
public final class ParentNetId extends ParentNetIdBase {

    private final Map<String, NetIdSeparate> leafChildren = new HashMap<>();
    private final Map<String, ParentNetIdBase> branchChildren = new HashMap<>();

    /** @param parent The parent. If this is null then this is a root node, and therefore is managed separately. (In all
     *            other cases you probably want to call {@link #child(String)} instead of using this). */
    public ParentNetId(@Nullable ParentNetId parent, String name) {
        super(parent, name, 0);
        if (parent != null) {
            parent.addChild(this);
        }
    }

    void addChild(NetIdSeparate netId) {
        if (leafChildren.containsKey(netId.name) || branchChildren.containsKey(netId.name)) {
            throw new IllegalStateException("There is already a child with the name " + netId.name + "!");
        }
        leafChildren.put(netId.name, netId);
    }

    void addChild(ParentNetIdBase netId) {
        if (leafChildren.containsKey(netId.name) || branchChildren.containsKey(netId.name)) {
            throw new IllegalStateException("There is already a child with the name " + netId.name + "!");
        }
        branchChildren.put(netId.name, netId);
    }

    @Override
    TreeNetIdBase getChild(String childName) {
        NetIdSeparate leaf = leafChildren.get(childName);
        return leaf != null ? leaf : branchChildren.get(childName);
    }

    /** Returns a new {@link NetIdData} (with this as it's parent) that can write an arbitrary length of data. */
    public NetIdData idData(String name) {
        return new NetIdData(this, name, TreeNetIdBase.DYNAMIC_LENGTH);
    }

    /** Returns a new {@link NetIdData} (with this as it's parent) that must write exactly the given "length" bytes of
     * data.
     * 
     * @param dataLength The exact number of bytes that must be written out. (This is so that the exact length isn't
     *            written out each time as it's known in advance). There's generally no reason to use this unless this
     *            is for a very small (and frequently sent) packet, as otherwise the space gained will be negligible. */
    public NetIdData idData(String name, int dataLength) {
        return new NetIdData(this, name, dataLength);
    }

    /** Returns a new {@link NetIdSignal} (with this as it's parent) that won't write any data out: instead the mere
     * presence of this packet should convey all of the information required. */
    public NetIdSignal idSignal(String name) {
        return new NetIdSignal(this, name);
    }

    /** Returns a new {@link ParentNetId} with this as it's child. This is mostly useful for organising different
     * packets or mods from the root node. */
    public ParentNetId child(String childName) {
        return new ParentNetId(this, childName);
    }
}
