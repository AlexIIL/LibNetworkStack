/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net;

final class ResolvedNetId<T> extends NetIdTyped<T> {

    final NetIdTyped<?> wrapped;

    public ResolvedNetId(ParentNetIdSingle<T> parent, NetIdTyped<?> wrapped) {
        super(parent, wrapped.name, wrapped.length);
        this.wrapped = wrapped;
        changeFlag(wrapped.getFinalFlags());
    }

    @Override
    protected void receive(NetByteBuf buffer, IMsgReadCtx ctx, T obj) throws InvalidInputDataException {
        receive0(buffer, ctx, obj, wrapped);
    }

    @Override
    public void send(ActiveConnection connection, T obj) {
        throw new IllegalStateException(
            "You can never write out a Resolved Net ID!"
                + "\n(How did you even call this anyway? This is meant to be an internal class!)"
        );
    }

    private static <U> void receive0(NetByteBuf buffer, IMsgReadCtx ctx, Object obj, NetIdTyped<U> wrapped)
        throws InvalidInputDataException {
        wrapped.receive(buffer, ctx, wrapped.parent.clazz.cast(obj));
    }

    @Override
    public ResolvedNetId<T> withoutBuffering() {
        notBuffered();
        return this;
    }

    @Override
    public ResolvedNetId<T> withTinySize() {
        setTinySize();
        return this;
    }

    @Override
    public ResolvedNetId<T> withNormalSize() {
        setNormalSize();
        return this;
    }

    @Override
    public ResolvedNetId<T> withLargeSize() {
        setLargeSize();
        return this;
    }

    @Override
    public ResolvedNetId<T> toClientOnly() {
        _toClientOnly();
        return this;
    }

    @Override
    public ResolvedNetId<T> toServerOnly() {
        _toServerOnly();
        return this;
    }

    @Override
    public ResolvedNetId<T> toEitherSide() {
        _toEitherSide();
        return this;
    }
}
