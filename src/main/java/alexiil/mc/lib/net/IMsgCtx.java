package alexiil.mc.lib.net;

import net.minecraft.network.NetworkSide;

import alexiil.mc.lib.net.impl.ActiveMinecraftConnection;

public interface IMsgCtx {

    ActiveConnection getConnection();

    /** @return The {@link NetworkSide} for this {@link #getConnection()}, assuming that it is an
     *         {@link ActiveMinecraftConnection}. */
    default NetworkSide getNetSide() {
        ActiveConnection connection = getConnection();
        return ((ActiveMinecraftConnection) connection).getNetSide();
    }

    /** @return true if the code calling this is running as the master or the slave. (In normal connections the
     *         dedicated or integrated server is the master, and the client is the slave). Assumes that the connection
     *         is an {@link ActiveMinecraftConnection}. */
    default boolean isMasterSide() {
        return getNetSide() == NetworkSide.SERVER;
    }

    NetIdBase getNetId();
}
