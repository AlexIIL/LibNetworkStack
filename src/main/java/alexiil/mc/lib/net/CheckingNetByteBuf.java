/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net;

import javax.annotation.Nullable;

import io.netty.buffer.ByteBuf;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

/** Debug variant of {@link NetByteBuf} that writes the method calls to ensure that it's a perfect mirror. This uses a
 * separate data buffer for reading & writing types, which can be null if no type data is present. */
public class CheckingNetByteBuf extends NetByteBuf {

    /** Unchecked exception that's thrown when one of the types doesn't match. */
    public static final class InvalidNetTypeException extends RuntimeException {
        public final NetMethod written, read;
        public final String writeArgs, readArgs;
        public final int index;

        public InvalidNetTypeException(NetMethod written, NetMethod read, int index) {
            super("Read the wrong type! (Wrote " + written + ", but read " + read + " @ " + index + ")");
            this.written = written;
            this.read = read;
            this.writeArgs = "";
            this.readArgs = "";
            this.index = index;
        }

        public InvalidNetTypeException(NetMethod method, int index, String writeArgs, String readArgs) {
            super(
                "Read the wrong extra data for type " + method + "! (Wrote " + writeArgs + ", but read " + readArgs
                    + " @ " + index + ")"
            );
            this.written = method;
            this.read = method;
            this.writeArgs = writeArgs;
            this.readArgs = readArgs;
            this.index = index;
        }
    }

    /** A networking related method that wrote data to the buffer. Also markers for separating sections. */
    public enum NetMethod {
        MARKER_ID((buf, to) -> to.append("marker: " + buf.typeBuffer.readVarUnsignedInt())),
        // Data Types
        // (The order shouldn't be changed)
        BOOLEAN((buf, to) -> to.append("boolean: ").append(buf.wrapped.readBoolean())),
        BYTE((buf, to) -> to.append("byte: ").append(buf.wrapped.readByte())),
        SHORT((buf, to) -> to.append("short: ").append(buf.wrapped.readShort())),
        MEDIUM((buf, to) -> to.append("medium: ").append(buf.wrapped.readMedium())),
        INT((buf, to) -> to.append("int: ").append(buf.wrapped.readInt())),
        LONG((buf, to) -> to.append("long: ").append(buf.wrapped.readLong())),
        SHORT_LE((buf, to) -> to.append("short(le): ").append(buf.wrapped.readShortLE())),
        MEDIUM_LE((buf, to) -> to.append("medium(le): ").append(buf.wrapped.readMediumLE())),
        INT_LE((buf, to) -> to.append("int(le): ").append(buf.wrapped.readIntLE())),
        LONG_LE((buf, to) -> to.append("long(le): ").append(buf.wrapped.readLongLE())),
        FLOAT((buf, to) -> to.append("float: ").append(buf.wrapped.readFloat())),
        FLOAT_LE((buf, to) -> to.append("float(le): ").append(buf.wrapped.readFloatLE())),
        DOUBLE((buf, to) -> to.append("double: ").append(buf.wrapped.readDouble())),
        DOUBLE_LE((buf, to) -> to.append("double(le): ").append(buf.wrapped.readDoubleLE())),
        CHAR((buf, to) -> {
            to.append("char: ");
            char c = buf.wrapped.readChar();
            if (Character.isISOControl(c)) {
                to.append("0x");
                to.append(Integer.toHexString(c));
            } else {
                to.append("'");
                to.append(c);
                to.append("'");
            }
        }),
        BYTES((buf, to) -> {
            int count = buf.typeBuffer.readVarUnsignedInt();
            NetByteBuf bytes = buf.wrapped.readBytes(count);
            to.append("bytes (" + count + "):\n  ");
            MsgUtil.appendBufferData(bytes, count, to, "  ", -1);
        }),
        BLOCK_POS((buf, to) -> {
            BlockPos pos = buf.wrapped.readBlockPos();
            to.append("BlockPos: [ ");
            to.append(pos.getX()).append(", ");
            to.append(pos.getY()).append(", ");
            to.append(pos.getZ()).append(" ]");
        }),
        BLOCK_HIT_RESULT((buf, to) -> {
            to.append("BlockHitResult: { ");
            BlockHitResult hit = buf.wrapped.readBlockHitResult();
            to.append("  pos = ").append(hit.getBlockPos().getX()).append(", ");
            to.append(hit.getBlockPos().getY()).append(", ");
            to.append(hit.getBlockPos().getZ()).append("\n");
            to.append("  side = ").append(hit.getSide()).append("\n");
            to.append("  type = ").append(hit.getType()).append("\n");
            to.append("  is inside = ").append(hit.isInsideBlock());
        }),
        COMPOUND_TAG((buf, to) -> to.append("nbt: ").append(buf.wrapped.readCompoundTag())),
        STRING((buf, to) -> to.append("string: \"").append(buf.wrapped.readString()).append("\"")),
        ENUM((buf, to) -> {
            int enumCount = buf.typeBuffer.readVarUnsignedInt();
            to.append("enum (").append(enumCount).append("): ");
            int bits = MathHelper.log2DeBruijn(enumCount);
            to.append(buf.wrapped.readFixedBits(bits));
        }),
        VAR_UINT((buf, to) -> to.append("var_uint: ").append(buf.wrapped.readVarUnsignedInt())),
        VAR_ULONG((buf, to) -> to.append("var_ulong: ").append(buf.wrapped.readVarUnsignedLong())),
        FIXED_INT((buf, to) -> {
            int count = buf.typeBuffer.readByte();
            to.append("fixed_int (" + count + "): ");
            to.append(buf.wrapped.readFixedBits(count));
        }),
        VAR_INT((buf, to) -> to.append("var_int: ").append(buf.wrapped.readVarInt())),
        VAR_LONG((buf, to) -> to.append("var_long: ").append(buf.wrapped.readVarLong()));

        @FunctionalInterface
        interface NetMethodAppender {
            void readAndAppend(CheckingNetByteBuf buffer, StringBuilder to);
        }

        private final NetMethodAppender appender;

        private NetMethod(NetMethodAppender appender) {
            this.appender = appender;
        }

        public static void readAndAppend(CheckingNetByteBuf buffer, StringBuilder to) {
            buffer.typeBuffer.readEnumConstant(NetMethod.class).append(buffer, to);
        }

        public void append(CheckingNetByteBuf buffer, StringBuilder to) {
            appender.readAndAppend(buffer, to);
        }
    }

    private final NetByteBuf wrapped;
    final NetByteBuf typeBuffer;
    private boolean recordReads;
    private int countRead = 0;
    private int countWrite = 0;

    public CheckingNetByteBuf(NetByteBuf wrapped, @Nullable NetByteBuf typeData) {
        super(wrapped);
        this.wrapped = wrapped;
        this.typeBuffer = typeData;
    }

    /** Call this to make this use the typeData buffer to store the types read rather than written. Useful only if the
     * type data wasn't provided by the writer. */
    public void recordReads() {
        this.recordReads = typeBuffer != null;
    }

    /** @return The number of read calls made. */
    public int getCountRead() {
        return countRead;
    }

    /** @return The number of write calls made. */
    public int getCountWrite() {
        return countWrite;
    }

    // Util

    private void validateRead(NetMethod expected) {
        if (recordReads) {
            return;
        }
        countRead++;
        if (typeBuffer == null) {
            return;
        }
        NetMethod written = typeBuffer.readEnumConstant(NetMethod.class);
        if (expected != written) {
            throw new InvalidNetTypeException(written, expected, countRead - 1);
        }
    }

    private void recordRead(NetMethod expected) {
        if (!recordReads) {
            return;
        }
        countRead++;
        typeBuffer.writeEnumConstant(expected);
    }

    private void write(NetMethod method) {
        countWrite++;
        if (typeBuffer != null) {
            typeBuffer.writeEnumConstant(method);
        }
    }

    public void readMarkerId(int expected) {
        if (recordReads) {
            recordRead(NetMethod.MARKER_ID);
            typeBuffer.writeVarUnsignedInt(expected);
            return;
        }
        validateRead(NetMethod.MARKER_ID);
        if (typeBuffer != null) {
            int id = typeBuffer.readVarUnsignedInt();
            if (expected != id) {
                throw new InvalidNetTypeException(NetMethod.MARKER_ID, countRead - 1, "" + id, "" + expected);
            }
        }
    }

    public void readMarkerId(IMsgReadCtx ctx, TreeNetIdBase netId) {
        if (typeBuffer == null) {
            return;
        }
        for (int i = 0; i < ctx.getConnection().readMapIds.size(); i++) {
            TreeNetIdBase id = ctx.getConnection().readMapIds.get(i);
            if (id == netId) {
                readMarkerId(i);
                return;
            }
        }
        throw new IllegalStateException("Don't know how to read " + netId);
    }

    int readMarkerId_data() {
        if (typeBuffer == null) {
            return -1;
        } else {
            return typeBuffer.readVarUnsignedInt();
        }
    }

    public void writeMarkerId(int id) {
        write(NetMethod.MARKER_ID);
        if (typeBuffer != null) {
            typeBuffer.writeVarUnsignedInt(id);
        }
    }

    public boolean hasTypeData() {
        return typeBuffer != null;
    }

    // #################
    //
    // Read&Write
    //
    // #################

    // ByteBuf

    // Validate before reading, just so we know that it's meant to work
    // Record after reading, as then we know it did work
    // (as it only ever either validates or records we can't join them together)

    @Override
    public boolean readBoolean() {
        validateRead(NetMethod.BOOLEAN);
        boolean val = wrapped.readBoolean();
        recordRead(NetMethod.BOOLEAN);
        return val;
    }

    @Override
    public CheckingNetByteBuf writeBoolean(boolean flag) {
        write(NetMethod.BOOLEAN);
        wrapped.writeBoolean(flag);
        return this;
    }

    @Override
    public byte readByte() {
        validateRead(NetMethod.BYTE);
        byte val = wrapped.readByte();
        recordRead(NetMethod.BYTE);
        return val;
    }

    @Override
    public short readUnsignedByte() {
        validateRead(NetMethod.BYTE);
        short val = wrapped.readUnsignedByte();
        recordRead(NetMethod.BYTE);
        return val;
    }

    @Override
    public CheckingNetByteBuf writeByte(int i) {
        write(NetMethod.BYTE);
        wrapped.writeByte(i);
        return this;
    }

    @Override
    public short readShort() {
        validateRead(NetMethod.SHORT);
        short val = wrapped.readShort();
        recordRead(NetMethod.SHORT);
        return val;
    }

    @Override
    public int readUnsignedShort() {
        validateRead(NetMethod.SHORT);
        int val = wrapped.readUnsignedShort();
        recordRead(NetMethod.SHORT);
        return val;
    }

    @Override
    public CheckingNetByteBuf writeShort(int i) {
        write(NetMethod.SHORT);
        wrapped.writeShort(i);
        return this;
    }

    @Override
    public int readMedium() {
        validateRead(NetMethod.MEDIUM);
        int val = wrapped.readMedium();
        recordRead(NetMethod.MEDIUM);
        return val;
    }

    @Override
    public int readUnsignedMedium() {
        validateRead(NetMethod.MEDIUM);
        int val = wrapped.readUnsignedMedium();
        recordRead(NetMethod.MEDIUM);
        return val;
    }

    @Override
    public CheckingNetByteBuf writeMedium(int i) {
        write(NetMethod.MEDIUM);
        wrapped.writeMedium(i);
        return this;
    }

    @Override
    public int readInt() {
        validateRead(NetMethod.INT);
        int val = wrapped.readInt();
        recordRead(NetMethod.INT);
        return val;
    }

    @Override
    public long readUnsignedInt() {
        validateRead(NetMethod.INT);
        long val = wrapped.readUnsignedInt();
        recordRead(NetMethod.INT);
        return val;
    }

    @Override
    public CheckingNetByteBuf writeInt(int i) {
        write(NetMethod.INT);
        wrapped.writeInt(i);
        return this;
    }

    @Override
    public long readLong() {
        validateRead(NetMethod.LONG);
        long val = wrapped.readLong();
        recordRead(NetMethod.LONG);
        return val;
    }

    @Override
    public CheckingNetByteBuf writeLong(long l) {
        write(NetMethod.LONG);
        wrapped.writeLong(l);
        return this;
    }

    @Override
    public short readShortLE() {
        validateRead(NetMethod.SHORT_LE);
        short val = wrapped.readShortLE();
        recordRead(NetMethod.SHORT_LE);
        return val;
    }

    @Override
    public int readUnsignedShortLE() {
        validateRead(NetMethod.SHORT_LE);
        int val = wrapped.readUnsignedShortLE();
        recordRead(NetMethod.SHORT_LE);
        return val;
    }

    @Override
    public CheckingNetByteBuf writeShortLE(int i) {
        write(NetMethod.SHORT_LE);
        wrapped.writeShortLE(i);
        return this;
    }

    @Override
    public int readMediumLE() {
        validateRead(NetMethod.MEDIUM_LE);
        int val = wrapped.readMediumLE();
        recordRead(NetMethod.MEDIUM_LE);
        return val;
    }

    @Override
    public int readUnsignedMediumLE() {
        validateRead(NetMethod.MEDIUM_LE);
        int val = wrapped.readUnsignedMediumLE();
        recordRead(NetMethod.MEDIUM_LE);
        return val;
    }

    @Override
    public CheckingNetByteBuf writeMediumLE(int i) {
        write(NetMethod.MEDIUM_LE);
        wrapped.writeMediumLE(i);
        return this;
    }

    @Override
    public int readIntLE() {
        validateRead(NetMethod.INT_LE);
        int val = wrapped.readIntLE();
        recordRead(NetMethod.INT_LE);
        return val;
    }

    @Override
    public long readUnsignedIntLE() {
        validateRead(NetMethod.INT_LE);
        long val = wrapped.readUnsignedIntLE();
        recordRead(NetMethod.INT_LE);
        return val;
    }

    @Override
    public CheckingNetByteBuf writeIntLE(int i) {
        write(NetMethod.INT_LE);
        wrapped.writeIntLE(i);
        return this;
    }

    @Override
    public long readLongLE() {
        validateRead(NetMethod.LONG_LE);
        long val = wrapped.readLongLE();
        recordRead(NetMethod.LONG_LE);
        return val;
    }

    @Override
    public CheckingNetByteBuf writeLongLE(long l) {
        write(NetMethod.LONG_LE);
        wrapped.writeLongLE(l);
        return this;
    }

    @Override
    public float readFloat() {
        validateRead(NetMethod.FLOAT);
        float val = wrapped.readFloat();
        recordRead(NetMethod.FLOAT);
        return val;
    }

    @Override
    public CheckingNetByteBuf writeFloat(float f) {
        write(NetMethod.FLOAT);
        wrapped.writeFloat(f);
        return this;
    }

    @Override
    public float readFloatLE() {
        validateRead(NetMethod.FLOAT_LE);
        float val = wrapped.readFloatLE();
        recordRead(NetMethod.FLOAT_LE);
        return val;
    }

    @Override
    public CheckingNetByteBuf writeFloatLE(float value) {
        write(NetMethod.FLOAT_LE);
        wrapped.writeFloatLE(value);
        return this;
    }

    @Override
    public double readDouble() {
        validateRead(NetMethod.DOUBLE);
        double val = wrapped.readDouble();
        recordRead(NetMethod.DOUBLE);
        return val;
    }

    @Override
    public CheckingNetByteBuf writeDouble(double d) {
        write(NetMethod.DOUBLE);
        wrapped.writeDouble(d);
        return this;
    }

    @Override
    public double readDoubleLE() {
        validateRead(NetMethod.DOUBLE_LE);
        double val = wrapped.readDoubleLE();
        recordRead(NetMethod.DOUBLE_LE);
        return val;
    }

    @Override
    public CheckingNetByteBuf writeDoubleLE(double value) {
        write(NetMethod.DOUBLE_LE);
        wrapped.writeDoubleLE(value);
        return this;
    }

    @Override
    public char readChar() {
        validateRead(NetMethod.CHAR);
        char val = wrapped.readChar();
        recordRead(NetMethod.CHAR);
        return val;
    }

    @Override
    public CheckingNetByteBuf writeChar(int i) {
        write(NetMethod.CHAR);
        wrapped.writeChar(i);
        return this;
    }

    // Bytes

    private void validateReadByteLength(int length) {
        validateRead(NetMethod.BYTES);
        if (recordReads) {
            return;
        }
        if (typeBuffer != null) {
            int len = typeBuffer.readVarUnsignedInt();
            if (len != length) {
                throw new InvalidNetTypeException(NetMethod.BYTES, countRead - 1, "" + len, "" + length);
            }
        }
    }

    private void recordReadByteLength(int length) {
        recordRead(NetMethod.BYTES);
        if (recordReads) {
            typeBuffer.writeVarUnsignedInt(length);
        }
    }

    private void checkWriteByteLength(int length) {
        write(NetMethod.BYTES);
        if (typeBuffer != null) {
            typeBuffer.writeVarUnsignedInt(length);
        }
    }

    @Override
    public final CheckingNetByteBuf readBytes(byte[] dst) {
        return readBytes(dst, 0, dst.length);
    }

    @Override
    public final CheckingNetByteBuf writeBytes(byte[] src) {
        return writeBytes(src, 0, src.length);
    }

    @Override
    public CheckingNetByteBuf readBytes(byte[] dst, int dstIndex, int length) {
        validateReadByteLength(length);
        wrapped.readBytes(dst, dstIndex, length);
        recordReadByteLength(length);
        return this;
    }

    @Override
    public CheckingNetByteBuf writeBytes(byte[] src, int srcIndex, int length) {
        checkWriteByteLength(length);
        wrapped.writeBytes(src, srcIndex, length);
        return this;
    }

    @Override
    public final CheckingNetByteBuf readBytes(ByteBuf byteBuf) {
        return readBytes(byteBuf, byteBuf.writableBytes());
    }

    @Override
    public final CheckingNetByteBuf writeBytes(ByteBuf byteBuf) {
        return writeBytes(byteBuf, byteBuf.readableBytes());
    }

    @Override
    public CheckingNetByteBuf readBytes(ByteBuf byteBuf, int length) {
        validateReadByteLength(length);
        wrapped.readBytes(byteBuf, length);
        recordReadByteLength(length);
        return this;
    }

    @Override
    public CheckingNetByteBuf writeBytes(ByteBuf byteBuf, int length) {
        checkWriteByteLength(length);
        wrapped.writeBytes(byteBuf, length);
        return this;
    }

    // PacketByteBuf

    @Override
    public BlockPos readBlockPos() {
        validateRead(NetMethod.BLOCK_POS);
        BlockPos val = wrapped.readBlockPos();
        recordRead(NetMethod.BLOCK_POS);
        return val;
    }

    @Override
    public CheckingNetByteBuf writeBlockPos(BlockPos pos) {
        write(NetMethod.BLOCK_POS);
        wrapped.writeBlockPos(pos);
        return this;
    }

    @Override
    public BlockHitResult readBlockHitResult() {
        validateRead(NetMethod.BLOCK_HIT_RESULT);
        BlockHitResult val = wrapped.readBlockHitResult();
        recordRead(NetMethod.BLOCK_HIT_RESULT);
        return val;
    }

    @Override
    public void writeBlockHitResult(BlockHitResult blockHitResult) {
        write(NetMethod.BLOCK_HIT_RESULT);
        wrapped.writeBlockHitResult(blockHitResult);
    }

    @Override
    public CompoundTag readCompoundTag() {
        validateRead(NetMethod.COMPOUND_TAG);
        CompoundTag val = wrapped.readCompoundTag();
        recordRead(NetMethod.COMPOUND_TAG);
        return val;
    }

    @Override
    public CheckingNetByteBuf writeCompoundTag(CompoundTag compoundTag) {
        write(NetMethod.COMPOUND_TAG);
        wrapped.writeCompoundTag(compoundTag);
        return this;
    }

    @Override
    public String readString() {
        return readString(Short.MAX_VALUE);
    }

    @Override
    public CheckingNetByteBuf writeString(String string) {
        return writeString(string, Short.MAX_VALUE);
    }

    @Override
    public String readString(int maxLength) {
        validateRead(NetMethod.STRING);
        String val = wrapped.readString(maxLength);
        recordRead(NetMethod.STRING);
        return val;
    }

    @Override
    public CheckingNetByteBuf writeString(String string, int maxLength) {
        write(NetMethod.STRING);
        wrapped.writeString(string, maxLength);
        return this;
    }

    @Override
    public <E extends Enum<E>> E readEnumConstant(Class<E> enumClass) {
        // Inlined wrapped.readEnumConstant
        // No need to lookup the declaring class as you cannot refer to sub-classes of Enum.
        E[] enums = enumClass.getEnumConstants();
        if (enums == null) {
            throw new IllegalArgumentException("Not an enum " + enumClass);
        }
        if (enums.length == 0) {
            throw new IllegalArgumentException("Tried to read an enum value without any values! How did you do this?");
        }
        validateRead(NetMethod.ENUM);
        if (enums.length == 1) {
            if (recordReads) {
                typeBuffer.writeVarUnsignedInt(1);
            } else if (typeBuffer != null) {
                int len = typeBuffer.readVarUnsignedInt();
                if (len != 1) {
                    throw new InvalidNetTypeException(NetMethod.ENUM, countRead - 1, "" + len, "" + 1);
                }
            }
            return enums[0];
        }
        if (!recordReads && typeBuffer != null) {
            int len = typeBuffer.readVarUnsignedInt();
            if (len != enums.length) {
                throw new InvalidNetTypeException(NetMethod.ENUM, countRead - 1, "" + len, "" + enums.length);
            }
        }
        int index = wrapped.readFixedBits(MathHelper.log2DeBruijn(enums.length));
        if (recordReads) {
            typeBuffer.writeVarUnsignedInt(enums.length);
        }
        return enums[index];
    }

    @Override
    public CheckingNetByteBuf writeEnumConstant(Enum<?> value) {
        // Inlined wrapped.writeEnumConstant
        Enum<?>[] possible = value.getDeclaringClass().getEnumConstants();
        if (possible == null) {
            throw new IllegalArgumentException("Not an enum " + value.getClass());
        }
        if (possible.length == 0) {
            throw new IllegalArgumentException("Tried to write an enum value without any values! How did you do this?");
        }
        write(NetMethod.ENUM);
        if (possible.length == 1) {
            if (typeBuffer != null) {
                typeBuffer.writeVarUnsignedInt(1);
            }
            return this;
        }
        if (typeBuffer != null) {
            typeBuffer.writeVarUnsignedInt(possible.length);
        }
        wrapped.writeFixedBits(value.ordinal(), MathHelper.log2DeBruijn(possible.length));
        return this;
    }

    @Override
    public int readVarUnsignedInt() {
        validateRead(NetMethod.VAR_UINT);
        int val = wrapped.readVarUnsignedInt();
        recordRead(NetMethod.VAR_UINT);
        return val;
    }

    @Override
    public CheckingNetByteBuf writeVarUnsignedInt(int ival) {
        write(NetMethod.VAR_UINT);
        wrapped.writeVarUnsignedInt(ival);
        return this;
    }

    @Override
    public long readVarUnsignedLong() {
        validateRead(NetMethod.VAR_ULONG);
        long val = wrapped.readVarUnsignedLong();
        recordRead(NetMethod.VAR_ULONG);
        return val;
    }

    @Override
    public CheckingNetByteBuf writeVarUnsignedLong(long lval) {
        write(NetMethod.VAR_ULONG);
        wrapped.writeVarUnsignedLong(lval);
        return this;
    }

    // NetByteBuf

    @Override
    public int readFixedBits(int length) throws IllegalArgumentException {
        validateRead(NetMethod.FIXED_INT);
        if (!recordReads && typeBuffer != null) {
            int len = typeBuffer.readByte();
            if (len != length) {
                throw new InvalidNetTypeException(NetMethod.FIXED_INT, countRead - 1, "" + len, "" + length);
            }
        }
        int val = wrapped.readFixedBits(length);
        recordRead(NetMethod.FIXED_INT);
        if (recordReads && typeBuffer != null) {
            typeBuffer.writeByte(length);
        }
        return val;
    }

    @Override
    public CheckingNetByteBuf writeFixedBits(int value, int length) throws IllegalArgumentException {
        write(NetMethod.FIXED_INT);
        if (typeBuffer != null) {
            typeBuffer.writeByte(length);
        }
        wrapped.writeFixedBits(value, length);
        return this;
    }

    @Override
    public int readVarInt() {
        validateRead(NetMethod.VAR_INT);
        int val = wrapped.readVarInt();
        recordRead(NetMethod.VAR_INT);
        return val;
    }

    @Override
    public CheckingNetByteBuf writeVarInt(int ival) {
        write(NetMethod.VAR_INT);
        wrapped.writeVarInt(ival);
        return this;
    }

    @Override
    public long readVarLong() {
        validateRead(NetMethod.VAR_LONG);
        long val = wrapped.readVarLong();
        recordRead(NetMethod.VAR_LONG);
        return val;
    }

    @Override
    public CheckingNetByteBuf writeVarLong(long lval) {
        write(NetMethod.VAR_LONG);
        wrapped.writeVarLong(lval);
        return this;
    }
}
