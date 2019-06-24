/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net;

public abstract class TreeNetIdBase {

    public static final int DYNAMIC_LENGTH = -1;

    /** Used by {@link InternalMsgUtil} to assert that this is a parent. */
    static final int PARENT_LENGTH = -2;

    public static final int MAXIMUM_PACKET_LENGTH = 0xFF_FF_FF;

    /** The name - must be unique to the parent that uses it, so for top level mod packets this MUST include the
     * modid. */
    public final String name;

    /** Used for logging and display purposes only. */
    public final String fullName;

    /** The length of this ID alone. Might not be a sensible number even if {@link #totalLength} is. */
    public final int length;

    /** Full length including all parents. */
    public final int totalLength;

    public final ParentNetIdBase parent;

    final boolean pathContainsDynamicParent;
    final NetIdPath path;

    public TreeNetIdBase(ParentNetIdBase parent, String name, int thisLength) {
        this.parent = parent;
        this.name = name;
        this.fullName = parent == null ? name : parent.fullName + "." + name;
        this.length = thisLength;
        if (thisLength == DYNAMIC_LENGTH) {
            this.totalLength = DYNAMIC_LENGTH;
        } else if (parent == null) {
            this.totalLength = thisLength;
        } else if (parent.totalLength == DYNAMIC_LENGTH) {
            this.totalLength = DYNAMIC_LENGTH;
        } else {
            this.totalLength = thisLength + parent.totalLength;
        }
        if (totalLength != DYNAMIC_LENGTH) {
            if (totalLength > MAXIMUM_PACKET_LENGTH) {
                throw new IllegalArgumentException(
                    "The length ("
                    + totalLength
                    + ") exceeded the maximum packet length ("
                    + MAXIMUM_PACKET_LENGTH
                    + ")"
                );
            } else if (totalLength < 0) {
                throw new IllegalArgumentException(
                    "The length ("
                    + totalLength
                    + ") has likely overflowed, exceeded the maximum packet length ("
                    + MAXIMUM_PACKET_LENGTH
                    + ")"
                );
            }
        }

        if (parent == null) {
            path = new NetIdPath(this);
            pathContainsDynamicParent = this instanceof DynamicNetId;
        } else {
            path = parent.path.withChild(this);
            pathContainsDynamicParent = parent.pathContainsDynamicParent || this instanceof DynamicNetId;
        }
    }

    @Override
    public final String toString() {
        return getPrintableName() + " '" + fullName + "'";
    }

    protected String getPrintableName() {
        return getRealClassName();
    }

    protected final String getRealClassName() {
        Class<?> cls = getClass();
        while (cls.isAnonymousClass()) {
            cls = cls.getSuperclass();
        }
        return cls.getSimpleName();
    }

    @Override
    public final boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public final int hashCode() {
        return System.identityHashCode(this);
    }

    public final boolean hasFixedLength() {
        return totalLength != DYNAMIC_LENGTH;
    }

    final int getLengthForPacketAlloc() {
        return this instanceof NetIdBase ? totalLength : PARENT_LENGTH;
    }

    abstract int getFlags();
}
