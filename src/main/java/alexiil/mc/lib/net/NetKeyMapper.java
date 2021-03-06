/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net;

import java.util.function.Function;

public abstract class NetKeyMapper<T> {
    public static final int LENGTH_DYNAMIC = TreeNetIdBase.DYNAMIC_LENGTH;

    public final Class<T> clazz;

    /** The known length in bytes of this key, or {@link #LENGTH_DYNAMIC} if this isn't a constant. */
    public final int length;

    public NetKeyMapper(Class<T> clazz, int length) {
        this.clazz = clazz;
        this.length = length;
    }

    public NetKeyMapper(Class<T> clazz) {
        this(clazz, LENGTH_DYNAMIC);
    }

    /** Reads the value for the key from the buffer, optionally using the parent keys.
     * <p>
     * Note that any of the <em>parent</em> keys might be null if they failed to read!
     * 
     * @return null if the key couldn't be found in whatever context this requires. */
    public abstract T read(NetByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException;

    public abstract void write(NetByteBuf buffer, IMsgWriteCtx ctx, T value);

    public static class ToString<T> extends NetKeyMapper<T> {

        private final Function<T, String> toStringMapper;
        private final Function<String, T> fromStringMapper;

        public ToString(Class<T> clazz, Function<T, String> toString, Function<String, T> fromString) {
            super(clazz);
            toStringMapper = toString;
            fromStringMapper = fromString;
        }

        @Override
        public T read(NetByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException {
            String s = MsgUtil.readUTF(buffer);
            T read = fromStringMapper.apply(s);
            if (read == null) {
                throw new InvalidInputDataException("Cannot read the string value '" + read + "' as " + clazz);
            }
            return read;
        }

        @Override
        public void write(NetByteBuf buffer, IMsgWriteCtx ctx, T value) {
            MsgUtil.writeUTF(buffer, toStringMapper.apply(value));
        }
    }

    public static class OfEnum<E extends Enum<E>> extends NetKeyMapper<E> {

        public OfEnum(Class<E> clazz) {
            super(clazz, 4);
        }

        @Override
        public E read(NetByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException {
            return buffer.readEnumConstant(clazz);
        }

        @Override
        public void write(NetByteBuf buffer, IMsgWriteCtx ctx, E value) {
            buffer.writeEnumConstant(value);
        }
    }
}
