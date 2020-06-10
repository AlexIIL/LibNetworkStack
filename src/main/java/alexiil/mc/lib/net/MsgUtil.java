/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net;

import java.nio.charset.StandardCharsets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.internal.StringUtil;

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

    /** Checks to make sure that this buffer has been *completely* read (so that there are no readable bytes left
     * over */
    public static void ensureEmpty(ByteBuf buf, boolean throwError, String extra) throws InvalidInputDataException {
        int readableBytes = buf.readableBytes();
        int rb = readableBytes;

        if (buf instanceof NetByteBuf) {
            // TODO: Find a way of checking if the partial bits have been fully read!
        }

        if (readableBytes > 0) {
            int ri = buf.readerIndex();
            ByteBuf dst = Unpooled.buffer();
            buf.getBytes(0, dst, buf.writerIndex());
            String summary = formatPacketSelection(dst, dst.readableBytes(), ri);
            IllegalStateException ex
                = new IllegalStateException("Did not fully read the data! [" + extra + "]\n" + summary + " --" + rb);
            if (throwError) {
                throw ex;
            } else {
                LibNetworkStack.LOGGER.warn(ex);
            }
            buf.clear();
        }
    }

    public static void printWholeBuffer(ByteBuf buf) {
        int readerIndex = buf.readerIndex();
        ByteBuf dst = Unpooled.buffer();
        buf.getBytes(0, dst, buf.writerIndex());
        LibNetworkStack.LOGGER.info("  " + formatPacketSelection(dst, dst.readableBytes(), readerIndex));
    }

    private static String formatPacketSelection(ByteBuf buffer, int length, int readerIndex) {
        StringBuilder sb = new StringBuilder();
        appendBufferData(buffer, length, sb, "", readerIndex);
        return sb.toString();
    }

    public static void appendBufferData(
        ByteBuf buffer, int length, StringBuilder sb, String linePrefix, int readerIndex
    ) {
        linePrefix = "\n" + linePrefix;

        for (int i = 0; true; i++) {
            int from = i * 20;
            int to = Math.min(from + 20, length);
            if (from >= to) break;
            if (i > 0) {
                sb.append(linePrefix);
            }
            for (int j = from; j < to; j++) {
                byte b = buffer.getByte(j);
                sb.append(StringUtil.byteToHexStringPadded(b));
                if (from + j + 1 == readerIndex) {
                    sb.append('#');
                } else {
                    sb.append(' ');
                }
            }
            int leftOver = from - to + 20;
            for (int j = 0; j < leftOver; j++) {
                sb.append("   ");
            }

            sb.append("| ");
            for (int j = from; j < to; j++) {
                byte b = buffer.getByte(j);
                char c = (char) b;
                if (c < 32 || c > 127) {
                    c = ' ';
                }
                sb.append(c);
            }
        }
    }
}
