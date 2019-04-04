package alexiil.mc.lib.net.impl;

import net.minecraft.network.Packet;
import net.minecraft.network.listener.PacketListener;

public interface IPacketCustomId<T extends PacketListener> extends Packet<T> {
    /** @return The integer ID that the other side is expecting. */
    int getReadId();
}
