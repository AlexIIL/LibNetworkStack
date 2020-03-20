/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net.test;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import alexiil.mc.lib.net.NetByteBuf;

public class McBufferTester {

    @Test
    public void testNegativePacked() {
        NetByteBuf buf = NetByteBuf.buffer();
        Assert.assertEquals(0, buf.writerIndex());
        buf.writeVarInt(-7);
        Assert.assertEquals(1, buf.writerIndex());
    }

    @Test
    public void testVarInts() {
        Random rand = new Random(42);
        for (int i = 0; i < 0x1_000_000; i++) {
            NetByteBuf buf = NetByteBuf.buffer();
            int val = rand.nextInt();
            buf.writeVarInt(val);
            int got = buf.readVarInt();
            if (got != val) {
                Assert.fail(
                    "Expected \n<" + Integer.toBinaryString(val) + ">\nbut got \n<" + Integer.toBinaryString(got) + ">"
                );
            }
        }

        for (int shift = 0; shift < 32; shift++) {
            int value = (1 << shift) - 1;
            NetByteBuf buf = NetByteBuf.buffer();
            buf.writeVarInt(value);
            int used = buf.writerIndex();
            System.out.println("writeVarInt(" + value + ") used " + used + (used == 1 ? " byte" : " bytes"));

            value = (1 << shift);
            buf = NetByteBuf.buffer();
            buf.writeVarInt(value);
            used = buf.writerIndex();
            System.out.println("writeVarInt(" + value + ") used " + used + (used == 1 ? " byte" : " bytes"));

            value = -(1 << shift);
            buf = NetByteBuf.buffer();
            buf.writeVarInt(value);
            used = buf.writerIndex();
            System.out.println("writeVarInt(" + value + ") used " + used + (used == 1 ? " byte" : " bytes"));

            value = -(1 << shift) - 1;
            buf = NetByteBuf.buffer();
            buf.writeVarInt(value);
            used = buf.writerIndex();
            System.out.println("writeVarInt(" + value + ") used " + used + (used == 1 ? " byte" : " bytes"));
        }
    }

    @Test
    public void testVarLongs() {
        Random rand = new Random(42);
        for (int i = 0; i < 0x1_000_000; i++) {
            NetByteBuf buf = NetByteBuf.buffer();
            long val = rand.nextLong();
            buf.writeVarLong(val);
            long got = buf.readVarLong();
            if (got != val) {
                Assert.fail(
                    "Expected \n<" + Long.toBinaryString(val) + ">\nbut got \n<" + Long.toBinaryString(got) + ">"
                );
            }
        }
    }
}
