package alexiil.mc.lib.net.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.netty.buffer.ByteBuf;

import net.fabricmc.fabric.api.event.client.ClientTickCallback;
import net.fabricmc.fabric.api.event.server.ServerStopCallback;
import net.fabricmc.fabric.api.event.server.ServerTickCallback;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.fabricmc.fabric.api.network.PacketContext;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.fabricmc.fabric.api.server.PlayerStream;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.NetworkState;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

import alexiil.mc.lib.net.ActiveConnection;
import alexiil.mc.lib.net.InvalidInputDataException;
import alexiil.mc.lib.net.LibNetworkStack;
import alexiil.mc.lib.net.mixin.api.INetworkStateMixin;

public class CoreMinecraftNetUtil {

    private static final boolean DEBUG = LibNetworkStack.DEBUG;

    static int serverExpectedId;
    static int clientExpectedId;

    private static ActiveClientConnection currentClientConnection;
    private static final Map<PacketContext, ActiveServerConnection> serverConnections = new HashMap<>();

    public static List<ActiveConnection> getNearbyActiveConnections(BlockEntity be, int distance) {
        List<ActiveConnection> list = new ArrayList<>();
        World w = be.getWorld();
        if (w == null) {
            return list;
        }
        double distanceSq = distance * distance;
        double beX = be.getPos().getX() + 0.5;
        double beY = be.getPos().getY() + 0.5;
        double beZ = be.getPos().getZ() + 0.5;
        if (w.isClient) {
            if (currentClientConnection == null) {
                return list;
            }
            if (currentClientConnection.getMinecraftContext().getPlayer().squaredDistanceTo(beX, beY,
                beZ) < distanceSq) {
                list.add(currentClientConnection);
            }
        } else {
            Set<PlayerEntity> players = PlayerStream.around(w, be.getPos(), distance).collect(Collectors.toSet());
            for (PlayerEntity player : players) {
                list.add(getServerConnection((PacketContext) ((ServerPlayerEntity) player).networkHandler));
            }
        }
        return list;
    }

    public static ActiveMinecraftConnection getConnection(PlayerEntity player) {
        if (player == null) {
            throw new NullPointerException("player");
        }
        if (player instanceof ServerPlayerEntity) {
            return getServerConnection((PacketContext) ((ServerPlayerEntity) player).networkHandler);
        } else if (currentClientConnection != null && currentClientConnection.ctx.getPlayer() == player) {
            return currentClientConnection;
        } else {
            throw new IllegalArgumentException("Unknown PlayerEntity " + player.getClass());
        }
    }

    public static void load() {
        ClientSidePacketRegistry.INSTANCE.register(ActiveMinecraftConnection.PACKET_ID, (ctx, buffer) -> {
            onClientReceivePacket(ctx, buffer);
        });
        ServerSidePacketRegistry.INSTANCE.register(ActiveMinecraftConnection.PACKET_ID, (ctx, buffer) -> {
            onServerReceivePacket(ctx, buffer);
        });

        ClientTickCallback.EVENT.register(client -> onClientTick());
        ServerTickCallback.EVENT.register(server -> onServerTick());
        ServerStopCallback.EVENT.register(server -> onServerStop());

        INetworkStateMixin game = (INetworkStateMixin) NetworkState.GAME;
        clientExpectedId = game.libnetworkstack_registerPacket(NetworkSide.CLIENT, CompactDataPacketToClient.class);
        serverExpectedId = game.libnetworkstack_registerPacket(NetworkSide.SERVER, CompactDataPacketToServer.class);
    }

    static void onClientReceivePacket(PacketContext ctx, ByteBuf buffer) {
        ActiveClientConnection connection = currentClientConnection;
        if (connection == null || connection.getMinecraftContext() != ctx) {
            connection = new ActiveClientConnection((ClientPlayNetworkHandler) ctx);
            currentClientConnection = connection;
        }
        ByteBuf b = buffer.copy();
        ActiveClientConnection c = connection;
        ctx.getTaskQueue().execute(() -> {
            try {
                c.onReceiveRawData(b);
            } catch (InvalidInputDataException e) {
                e.printStackTrace();
            }
        });
    }

    static void onServerReceivePacket(PacketContext ctx, ByteBuf buffer) {
        ActiveServerConnection connection = getServerConnection(ctx);
        if (connection == null) {
            return;
        }

        ByteBuf b = buffer.copy();
        ctx.getTaskQueue().execute(() -> {
            try {
                connection.onReceiveRawData(b);
                b.release();
            } catch (InvalidInputDataException e) {
                e.printStackTrace();
            }
        });
    }

    private static ActiveServerConnection getServerConnection(PacketContext ctx) {
        return serverConnections.computeIfAbsent(ctx, c -> {
            ServerPlayNetworkHandler networkHandler = (ServerPlayNetworkHandler) c;
            if (!networkHandler.client.isOpen()) {
                if (DEBUG) {
                    LibNetworkStack.LOGGER.info(
                        "Disallowed server connection for " + ctx.getPlayer() + " because it's channel is not open!");
                }
                return null;
            } else {
                if (DEBUG) {
                    LibNetworkStack.LOGGER.info("Creating new server connection for " + ctx.getPlayer());
                }
            }
            return new ActiveServerConnection(networkHandler);
        });
    }

    private static void onClientTick() {
        if (currentClientConnection != null) {
            if (!currentClientConnection.netHandler.getClientConnection().isOpen()) {
                if (DEBUG) {
                    LibNetworkStack.LOGGER.info("Removed current client connection " + currentClientConnection
                        + " as it's channel is no longer open.");
                }
                currentClientConnection = null;
                return;
            }
            currentClientConnection.tick();
        }
    }

    private static void onServerTick() {
        Iterator<ActiveServerConnection> iterator = serverConnections.values().iterator();
        while (iterator.hasNext()) {
            ActiveServerConnection connection = iterator.next();
            if (!connection.netHandler.client.isOpen()) {
                iterator.remove();
                if (DEBUG) {
                    LibNetworkStack.LOGGER
                        .info("Removed server connection " + connection + " as it's channel is no longer open.");
                }
                continue;
            }
            connection.tick();
        }
    }

    private static void onServerStop() {
        int count = serverConnections.size();
        serverConnections.clear();
        if (DEBUG) {
            LibNetworkStack.LOGGER.info("Removed " + count + " server connections as the server has stopped.");
        }
    }
}
