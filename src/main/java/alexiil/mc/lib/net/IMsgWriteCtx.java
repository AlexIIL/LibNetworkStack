package alexiil.mc.lib.net;

public interface IMsgWriteCtx extends IMsgCtx {

    default void assertClientSide() {
        if (isServerSide()) {
            throw new IllegalStateException("Cannot write " + getNetId() + " on the server!");
        }
    }

    default void assertServerSide() {
        if (isClientSide()) {
            throw new IllegalStateException("Cannot write " + getNetId() + " on the client!");
        }
    }
}
