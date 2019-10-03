/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import net.fabricmc.fabric.api.event.client.ClientTickCallback;
import net.fabricmc.fabric.api.event.server.ServerStopCallback;
import net.fabricmc.fabric.api.event.server.ServerTickCallback;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.fabricmc.fabric.api.network.PacketContext;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.fabricmc.fabric.api.server.PlayerStream;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.NetworkState;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import alexiil.mc.lib.net.InvalidInputDataException;
import alexiil.mc.lib.net.LibNetworkStack;
import alexiil.mc.lib.net.NetByteBuf;
import alexiil.mc.lib.net.mixin.api.INetworkStateMixin;

public class CoreMinecraftNetUtil {

    private static final boolean DEBUG = LibNetworkStack.DEBUG;

    static int serverExpectedId;
    static int clientExpectedId;

    private static ActiveClientConnection currentClientConnection;
    private static final Map<PacketContext, ActiveServerConnection> serverConnections = new HashMap<>();

    public static List<ActiveMinecraftConnection> getNearbyActiveConnections(BlockEntity be, int distance) {
        List<ActiveMinecraftConnection> list = new ArrayList<>();
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
            if (currentClientConnection.getMinecraftContext().getPlayer().squaredDistanceTo(beX, beY, beZ)
                < distanceSq) {
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

    public static List<ActiveMinecraftConnection> getPlayersWatching(World world, BlockPos pos) {
        List<PlayerEntity> players = PlayerStream.watching(world, pos).collect(Collectors.toList());
        List<ActiveMinecraftConnection> list = new ArrayList<>();
        for (PlayerEntity player : players) {
            list.add(getConnection(player));
        }
        return list;
    }

    public static ActiveMinecraftConnection getConnection(PlayerEntity player) {
        if (player == null) {
            throw new NullPointerException("player");
        }
        if (player instanceof ServerPlayerEntity) {
            return getServerConnection((PacketContext) ((ServerPlayerEntity) player).networkHandler);
        } else if (player.world.isClient
            && ((currentClientConnection == null && player == MinecraftClient.getInstance().player)
            || (currentClientConnection != null && currentClientConnection.ctx.getPlayer() == player))) {
                return getOrCreateClientConnection((PacketContext) MinecraftClient.getInstance().player.networkHandler);
            } else {
                throw new IllegalArgumentException("Unknown PlayerEntity " + player.getClass());
            }
    }

    public static void load() {
        ServerSidePacketRegistry.INSTANCE.register(ActiveMinecraftConnection.PACKET_ID, (ctx, buffer) -> {
            onServerReceivePacket(ctx, NetByteBuf.asNetByteBuf(buffer));
        });

        ServerTickCallback.EVENT.register(server -> onServerTick());
        ServerStopCallback.EVENT.register(server -> onServerStop());

        INetworkStateMixin play = (INetworkStateMixin) (Object) NetworkState.PLAY;
        clientExpectedId = play.libnetworkstack_registerPacket(
            NetworkSide.CLIENTBOUND, CompactDataPacketToClient.class, CompactDataPacketToClient::new
        );
        serverExpectedId = play.libnetworkstack_registerPacket(
            NetworkSide.SERVERBOUND, CompactDataPacketToServer.class, CompactDataPacketToServer::new
        );
    }

    public static void loadClient() {
        ClientSidePacketRegistry.INSTANCE.register(ActiveMinecraftConnection.PACKET_ID, (ctx, buffer) -> {
            onClientReceivePacket(ctx, NetByteBuf.asNetByteBuf(buffer));
        });

        ClientTickCallback.EVENT.register(client -> onClientTick());
    }

    static void onClientReceivePacket(PacketContext ctx, NetByteBuf buffer) {
        ActiveClientConnection connection = getOrCreateClientConnection(ctx);
        NetByteBuf b = buffer.copy();
        ctx.getTaskQueue().execute(() -> {
            try {
                connection.onReceiveRawData(b);
            } catch (InvalidInputDataException e) {
                e.printStackTrace();
            }
        });
    }

    /** @return The current client connection. Throws an exception if this is not available right now (likely because
     *         the client hasn't joined a world). */
    public static ActiveClientConnection getClientConnection() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            throw new IllegalStateException("The client is not currently connected to any server!");
        }
        return getOrCreateClientConnection((PacketContext) player.networkHandler);
    }

    /** @return The current {@link ActiveClientConnection}, or null if one has not been created yet. */
    @Nullable
    public static ActiveClientConnection getCurrentClientConnection() {
        return currentClientConnection;
    }

    private static ActiveClientConnection getOrCreateClientConnection(PacketContext ctx) {
        ActiveClientConnection connection = currentClientConnection;
        if (connection == null || connection.getMinecraftContext() != ctx) {
            connection = new ActiveClientConnection((ClientPlayNetworkHandler) ctx);
            currentClientConnection = connection;
        }
        return connection;
    }

    static void onServerReceivePacket(PacketContext ctx, NetByteBuf buffer) {
        ActiveServerConnection connection = getServerConnection(ctx);
        if (connection == null) {
            return;
        }

        NetByteBuf b = buffer.copy();
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
                        "Disallowed server connection for " + ctx.getPlayer() + " because it's channel is not open!"
                    );
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
            if (!currentClientConnection.netHandler.getConnection().isOpen()) {
                if (DEBUG) {
                    LibNetworkStack.LOGGER.info(
                        "Removed current client connection " + currentClientConnection
                            + " as it's channel is no longer open."
                    );
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
