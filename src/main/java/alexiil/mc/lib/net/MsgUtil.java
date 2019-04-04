package alexiil.mc.lib.net;

import java.nio.charset.StandardCharsets;

import io.netty.buffer.ByteBuf;

public class MsgUtil {

    public static void writeUTF(ByteBuf buffer, String string) {
        byte[] data = string.getBytes(StandardCharsets.UTF_8);
        if (data.length > 0xFF_FF) {
            throw new IllegalArgumentException("Cannot write a string with more than " + 0xFF_FF + " bytes!");
        }
        buffer.writeShort(data.length);
        buffer.writeBytes(data);
    }

    public static String readUTF(ByteBuf buffer) {
        int len = buffer.readUnsignedShort();
        byte[] data = new byte[len];
        buffer.readBytes(data);
        return new String(data, StandardCharsets.UTF_8);
    }

    public static void writeEnum(ByteBuf buffer, Enum<?> value) {
        Enum<?>[] possible = value.getDeclaringClass().getEnumConstants();
        if (possible == null) {
            throw new IllegalArgumentException("Not an enum " + value.getClass());
        }
        if (possible.length == 0) {
            throw new IllegalArgumentException("Tried to write an enum value without any values! How did you do this?");
        }
        if (possible.length == 1) {
            return;
        }
        buffer.writeInt(value.ordinal());
    }

    public static <E extends Enum<E>> E readEnum(ByteBuf buffer, Class<E> enumClass) throws InvalidInputDataException {
        // No need to lookup the declaring class as you cannot refer to sub-classes of Enum.
        E[] enums = enumClass.getEnumConstants();
        if (enums == null) {
            throw new IllegalArgumentException("Not an enum " + enumClass);
        }
        if (enums.length == 0) {
            throw new IllegalArgumentException(
                "Tried to read an enum value without any values! We don't have any non-null values to return!");
        }
        if (enums.length == 1) {
            return enums[0];
        }
        int index = buffer.readInt();
        if (index < 0 || index >= enums.length) {
            throw new InvalidInputDataException("Invalid index " + index + " for array of length " + enums.length);
        }
        return enums[index];
    }
}
