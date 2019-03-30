package alexiil.mc.lib.net;

import java.util.Set;

import io.netty.buffer.ByteBuf;

import buildcraft.api.core.InvalidInputDataException;

public class NetIdDataK<T> extends NetIdBase {

    @FunctionalInterface
    public interface IMsgDataReceiverK<T> {
        void receive(T obj, ByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException;
    }

    @FunctionalInterface
    public interface IMsgKeyNotFoundHandlerK<T> {
        void handle(ByteBuf buffer, IMsgReadCtx ctx, Set<ParentNetIdTyped<?>> notFoundKeys)
            throws InvalidInputDataException;
    }

    @FunctionalInterface
    public interface IMsgDataWriterK<T> {
        void write(T obj, ByteBuf buffer, IMsgWriteCtx ctx);
    }

    public final ParentNetIdTyped<T> key;

    private IMsgDataReceiverK<T> receiver = (t, buffer, ctx) -> {
        throw new InvalidInputDataException("No receiver set for " + ctx.getNetSide());
    };

    /** For when one of the parent keys failed to find non-null keys. */
    private IMsgKeyNotFoundHandlerK<T> exceptionReceiver = (t, buffer, ctx, missing) -> {
        throw new InvalidInputDataException("Failed to read some of the keys!");
    };
    private IMsgDataWriterK<T> writer;

    public NetIdDataK(ParentNetIdTyped<T> parent, String name, int length) {
        this(parent, name, parent, length);
    }

    public NetIdDataK(ParentNetId parent, String name, ParentNetIdTyped<T> key, int length) {
        super(parent, name, length);
        if (!parent.isParent(key)) {
            throw new IllegalArgumentException("");
        }
        this.key = key;
    }

    public NetIdDataK<T> setReadWrite(IMsgDataReceiverK<T> receiver, IMsgDataWriterK<T> writer) {
        this.receiver = receiver;
        this.writer = writer;
        return this;
    }

    public NetIdDataK<T> ignoreReadFailures() {
        exceptionReceiver = (buffer, ctx, missing) -> {};
        return this;
    }

    public NetIdDataK<T> setKeyNotFoundHandler(IMsgKeyNotFoundHandlerK<T> handler) {
        exceptionReceiver = handler;
        return this;
    }

    @Override
    public void receive(ByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException {
        T obj = ctx.getKey(key);
        receiver.receive(obj, buffer, ctx);
    }

    @Override
    public void handleMissingKeys(ByteBuf buffer, IMsgReadCtx ctx, Set<ParentNetIdTyped<?>> notFoundKeys)
        throws InvalidInputDataException {
        exceptionReceiver.handle(buffer, ctx, notFoundKeys);
    }
}
