package alexiil.mc.lib.net;

public interface IMsgCtx {

    public enum NetworkSide {
        MASTER,
        SLAVE;
    }

    NetworkSide getNetSide();

    /** @return true if the code calling this is running as the master or the slave. (In normal connections the
     *         dedicated or integrated server is the master, and the client is the slave). */
    default boolean isMasterSide() {
        return getNetSide() == NetworkSide.MASTER;
    }

    NetIdBase getNetId();

    <T> void putKey(ParentNetIdTyped<T> key, T value);

    <T> T getKey(ParentNetIdTyped<T> key);
}
