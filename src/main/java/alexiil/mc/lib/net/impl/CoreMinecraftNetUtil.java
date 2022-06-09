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

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.client.ClientTickCallback;
import net.fabricmc.fabric.api.event.server.ServerStopCallback;
import net.fabricmc.fabric.api.event.server.ServerTickCallback;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.fabricmc.fabric.api.network.PacketContext;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.server.PlayerStream;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.NetworkState;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
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
    private static final Map<ServerPlayNetworkHandler, ActiveServerConnection> serverConnections = new HashMap<>();

    public static List<ActiveMinecraftConnection> getNearbyActiveConnections(BlockEntity be, int distance) {
        List<ActiveMinecraftConnection> list = new ArrayList<>();
        World w = be.getWorld();
        if (w == null) {
            return list;
        }
        if (w.isClient) {
            if (currentClientConnection == null) {
                return list;
            }
            double distanceSq = distance * distance;
            double x = be.getPos().getX() + 0.5;
            double y = be.getPos().getY() + 0.5;
            double z = be.getPos().getZ() + 0.5;
            if (currentClientConnection.getPlayer().squaredDistanceTo(x, y, z) < distanceSq) {
                list.add(currentClientConnection);
            }
        } else {
            Set<PlayerEntity> players = PlayerStream.around(w, be.getPos(), distance).collect(Collectors.toSet());
            for (PlayerEntity player : players) {
                ActiveServerConnection con = getServerConnection(((ServerPlayerEntity) player).networkHandler);
                if (con != null) {
                    list.add(con);
                }
            }
        }
        return list;
    }

    public static List<ActiveMinecraftConnection> getPlayersWatching(World world, BlockPos pos) {
        List<PlayerEntity> players = PlayerStream.watching(world, pos).collect(Collectors.toList());
        List<ActiveMinecraftConnection> list = new ArrayList<>();
        for (PlayerEntity player : players) {
            ActiveMinecraftConnection connection = getConnection(player);
            if (connection != null) {
                list.add(connection);
            }
        }
        return list;
    }

    public static ActiveMinecraftConnection getConnection(PlayerEntity player) {
        if (player == null) {
            throw new NullPointerException("player");
        }
        if (player instanceof ServerPlayerEntity) {
            return getServerConnection(((ServerPlayerEntity) player).networkHandler);
        } else if (player.world.isClient) {
            ClientPlayerEntity clientPlayer = MinecraftClient.getInstance().player;
            if (clientPlayer == null) {
                throw new IllegalStateException(
                    "Cannot create a connection for " + player + " because they cannot be the client player (null)!"
                );
            }
            if (currentClientConnection == null && player == clientPlayer) {
                return getOrCreateClientConnection(clientPlayer.networkHandler);
            } else if (currentClientConnection != null && currentClientConnection.getPlayer() == player) {
                return currentClientConnection;
            } else {
                throw new IllegalArgumentException("Unknown PlayerEntity " + player.getClass());
            }
        } else {
            throw new IllegalArgumentException("Unknown PlayerEntity " + player.getClass());
        }
    }

    public static void load() {
        if (PacketContext.class.isAssignableFrom(ServerPlayNetworkHandler.class)) {
            // Fabric API: Networking V0
            ServerSidePacketRegistry.INSTANCE.register(ActiveMinecraftConnection.PACKET_ID, (ctx, buffer) -> {
                onServerReceivePacket((ServerPlayNetworkHandler) ctx, NetByteBuf.asNetByteBuf(buffer));
            });
        } else {
            // Fabric API: Networking V1
            // Object and then cast to avoid the verifier classloading
            Object handler = new ServerPlayNetworking.PlayChannelHandler() {
                @Override
                public void receive(
                    MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler,
                    PacketByteBuf buf, PacketSender responseSender
                ) {
                    ActiveServerConnection connection = getServerConnection(handler);
                    if (connection == null) {
                        return;
                    }

                    NetByteBuf b = NetByteBuf.asNetByteBuf(buf.copy());
                    server.execute(() -> {
                        try {
                            connection.onReceiveRawData(b);
                            b.release();
                        } catch (InvalidInputDataException e) {
                            e.printStackTrace();
                            handler.disconnect(
                                new LiteralText("LibNetworkStack: read error (see server logs for more details)\n" + e)
                            );
                        }
                    });
                }
            };
            ServerPlayNetworking.registerGlobalReceiver(
                ActiveMinecraftConnection.PACKET_ID, (ServerPlayNetworking.PlayChannelHandler) handler
            );
        }

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
        if (PacketContext.class.isAssignableFrom(ClientPlayNetworkHandler.class)) {
            // Fabric API: Networking v0
            ClientSidePacketRegistry.INSTANCE.register(ActiveMinecraftConnection.PACKET_ID, (ctx, buffer) -> {
                onClientReceivePacket((ClientPlayNetworkHandler) ctx, NetByteBuf.asNetByteBuf(buffer));
            });
        } else {
            // Fabric API: Networking V1
            Object handler = new ClientPlayNetworking.PlayChannelHandler() {
                @Override
                public void receive(
                    MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buffer,
                    PacketSender responseSender
                ) {
                    ActiveClientConnection connection = getOrCreateClientConnection(handler);
                    NetByteBuf b = NetByteBuf.asNetByteBuf(buffer.copy());
                    client.execute(() -> {
                        try {
                            connection.onReceiveRawData(b);
                            b.release();
                        } catch (InvalidInputDataException e) {
                            e.printStackTrace();
                            handler.getConnection()
                                .disconnect(new LiteralText("LibNetworkStack: read error (see logs for details)"));
                        }
                    });
                }
            };

            // Object and then cast to avoid the verifier classloading
            ClientPlayNetworking.registerGlobalReceiver(
                ActiveMinecraftConnection.PACKET_ID, (ClientPlayNetworking.PlayChannelHandler) handler
            );
        }

        ClientTickCallback.EVENT.register(client -> onClientTick());
    }

    static void onClientReceivePacket(ClientPlayNetworkHandler ctx, NetByteBuf buffer) {
        ActiveClientConnection connection = getOrCreateClientConnection(ctx);
        NetByteBuf b = buffer.copy();
        MinecraftClient.getInstance().execute(() -> {
            try {
                connection.onReceiveRawData(b);
                b.release();
            } catch (InvalidInputDataException e) {
                e.printStackTrace();
                ctx.getConnection().disconnect(new LiteralText("LibNetworkStack: read error (see logs for details)"));
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
        return getOrCreateClientConnection(player.networkHandler);
    }

    /** @return The current {@link ActiveClientConnection}, or null if one has not been created yet. */
    @Nullable
    public static ActiveClientConnection getCurrentClientConnection() {
        return currentClientConnection;
    }

    private static ActiveClientConnection getOrCreateClientConnection(ClientPlayNetworkHandler ctx) {
        ActiveClientConnection connection = currentClientConnection;
        if (connection == null || connection.netHandler != ctx) {
            connection = new ActiveClientConnection(ctx);
            connection.postConstruct();
            currentClientConnection = connection;
        }
        return connection;
    }

    static void onServerReceivePacket(ServerPlayNetworkHandler ctx, NetByteBuf buffer) {
        ActiveServerConnection connection = getServerConnection(ctx);
        if (connection == null) {
            return;
        }

        NetByteBuf b = buffer.copy();
        ctx.player.server.execute(() -> {
            try {
                connection.onReceiveRawData(b);
                b.release();
            } catch (InvalidInputDataException e) {
                e.printStackTrace();
                ctx.disconnect(new LiteralText("LibNetworkStack: read error (see server logs for more details)\n" + e));
            }
        });
    }

    private static ActiveServerConnection getServerConnection(ServerPlayNetworkHandler netHandler) {
        return serverConnections.computeIfAbsent(netHandler, c -> {
            if (!netHandler.connection.isOpen()) {
                LibNetworkStack.LOGGER.warn(
                    "Disallowed server connection for " + netHandler.player + " because it's channel is not open!"
                );
                return null;
            } else {
                if (DEBUG) {
                    LibNetworkStack.LOGGER.info("Creating new server connection for " + netHandler.player);
                }
            }
            ActiveServerConnection connection = new ActiveServerConnection(netHandler);
            connection.postConstruct();
            return connection;
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
            if (!connection.netHandler.connection.isOpen()) {
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
