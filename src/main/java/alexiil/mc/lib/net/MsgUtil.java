/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net;

import java.nio.charset.StandardCharsets;

/** Various utilities for reading and writing data */
public final class MsgUtil {
    private MsgUtil() {}

    public static void writeUTF(NetByteBuf buffer, String string) {
        byte[] data = string.getBytes(StandardCharsets.UTF_8);
        if (data.length > 0xFF_FF) {
            throw new IllegalArgumentException("Cannot write a string with more than " + 0xFF_FF + " bytes!");
        }
        buffer.writeShort(data.length);
        buffer.writeBytes(data);
    }

    public static String readUTF(NetByteBuf buffer) {
        int len = buffer.readUnsignedShort();
        byte[] data = new byte[len];
        buffer.readBytes(data);
        return new String(data, StandardCharsets.UTF_8);
    }
}
