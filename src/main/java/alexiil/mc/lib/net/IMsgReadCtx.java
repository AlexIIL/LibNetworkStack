package alexiil.mc.lib.net;

public interface IMsgReadCtx extends IMsgCtx {

    default void assertClientSide() throws InvalidInputDataException {
        if (isServerSide()) {
            throw new InvalidInputDataException("Cannot read " + getNetId() + " on the server!");
        }
    }

    default void assertServerSide() throws InvalidInputDataException {
        if (isClientSide()) {
            throw new InvalidInputDataException("Cannot read " + getNetId() + " on the client!");
        }
    }
}
