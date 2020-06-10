/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net;

import java.util.Arrays;
import java.util.List;

final class NetIdPath {
    private static final NetIdPath EMPTY = new NetIdPath(new TreeNetIdBase[0]);

    final TreeNetIdBase[] array;
    final int hash;

    NetIdPath(TreeNetIdBase[] array) {
        this.array = array;
        this.hash = Arrays.hashCode(array);
    }

    NetIdPath(TreeNetIdBase single) {
        this(new TreeNetIdBase[] { single });
    }

    NetIdPath(List<? extends TreeNetIdBase> list) {
        this(list.toArray(new TreeNetIdBase[0]));
    }

    @Override
    public String toString() {
        if (array.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(array[0].name);
        for (int i = 1; i < array.length; i++) {
            TreeNetIdBase id = array[i];
            sb.append(".");
            sb.append(id.name);
        }
        return sb.toString();
    }

    NetIdPath parent() {
        if (array.length <= 1) {
            return EMPTY;
        }
        return new NetIdPath(Arrays.copyOfRange(array, 0, array.length - 1));
    }

    NetIdPath withChild(TreeNetIdBase child) {
        TreeNetIdBase[] arr = Arrays.copyOf(array, array.length + 1);
        arr[array.length] = child;
        return new NetIdPath(arr);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null) return false;
        if (obj instanceof NetIdPath) {
            NetIdPath other = (NetIdPath) obj;
            if (hash != other.hash || array.length != other.array.length) {
                return false;
            }
            // Compare the last element first (the child)
            // as that's the one that is most likely to be different.
            for (int i = array.length - 1; i >= 0; i--) {
                // Use identity comparison as everything works that way.
                if (array[i] != other.array[i]) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    int calculateLength() {
        int len = 0;
        for (TreeNetIdBase id : array) {
            if (id.length == TreeNetIdBase.DYNAMIC_LENGTH) {
                return TreeNetIdBase.DYNAMIC_LENGTH;
            }
            len += id.length;
        }
        return len;
    }
}
