package alexiil.mc.lib.net.impl;

import net.fabricmc.fabric.api.network.PacketContext;

import net.minecraft.network.NetworkSide;
import net.minecraft.network.Packet;
import net.minecraft.util.Identifier;

import alexiil.mc.lib.net.BufferedConnection;
import alexiil.mc.lib.net.LibNetworkStack;
import alexiil.mc.lib.net.McNetworkStack;
import alexiil.mc.lib.net.NetByteBuf;
import alexiil.mc.lib.net.NetIdData;

/** A connection to the other side - this is either an {@link ActiveClientConnection} or an
 * {@link ActiveServerConnection}. */
public abstract class ActiveMinecraftConnection extends BufferedConnection {

    // As this is networking we need to ensure that we send as little data as possible
    // Which is why we use the full domain, and a fully descriptive path for it
    public static final Identifier PACKET_ID = new Identifier("libnetworkstack", "data");

    private static final NetIdData NET_ID_COMPACT_PACKET = //
        McNetworkStack.ROOT.idData("libnetworkstack:compact_id", 4)//
            .setReceiver((buffer, ctx) -> {
                int theirCustomId = buffer.readInt();
                ActiveMinecraftConnection connection = (ActiveMinecraftConnection) ctx.getConnection();
                connection.theirCustomId = theirCustomId;

                if (LibNetworkStack.DEBUG) {
                    LibNetworkStack.LOGGER.info(connection + " Received their custom id " + theirCustomId);
                }
            });

    private static final int NET_ID_NOT_OPTIMISED = 0;

    private static final boolean COMPACT_PACKETS = true;

    /** The raw minecraft ID to use if the other side has told us which ID the custom packet is mapped to. */
    private int theirCustomId = NET_ID_NOT_OPTIMISED;
    private boolean hasSentCustomId;

    public final PacketContext ctx;

    public ActiveMinecraftConnection(PacketContext ctx) {
        super(McNetworkStack.ROOT, /* Drop after 3 seconds */ 3 * 20);
        this.ctx = ctx;
    }

    @Override
    public PacketContext getMinecraftContext() {
        return ctx;
    }

    @Override
    public final void sendRawData0(NetByteBuf data) {
        final Packet<?> packet;
        if (COMPACT_PACKETS && theirCustomId != NET_ID_NOT_OPTIMISED) {
            packet = toCompactPacket(theirCustomId, data);
        } else {
            data.retain();
            packet = toNormalPacket(data);
        }
        sendPacket(packet);
    }

    @Override
    public void tick() {
        super.tick();
        if (COMPACT_PACKETS && !hasSentCustomId) {
            hasSentCustomId = true;
            sendCustomId();
        }
    }

    private void sendCustomId() {
        int ourCustomId = this instanceof ActiveServerConnection ? CoreMinecraftNetUtil.serverExpectedId
            : CoreMinecraftNetUtil.clientExpectedId;
        NET_ID_COMPACT_PACKET.send(this, (buffer, c) -> {
            buffer.writeInt(ourCustomId);
        });
        if (LibNetworkStack.DEBUG) {
            LibNetworkStack.LOGGER.info(this + " Sent our custom id " + ourCustomId);
        }
    }

    protected abstract Packet<?> toNormalPacket(NetByteBuf data);

    protected abstract Packet<?> toCompactPacket(int receiverId, NetByteBuf data);

    protected abstract void sendPacket(Packet<?> packet);

    public abstract NetworkSide getNetSide();
}
