package alexiil.mc.lib.net.mixin.api;

import net.minecraft.network.NetworkSide;

import alexiil.mc.lib.net.impl.IPacketCustomId;

public interface INetworkStateMixin {
    int libnetworkstack_registerPacket(NetworkSide recvSide, Class<? extends IPacketCustomId<?>> packet);
}
