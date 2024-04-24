/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import io.netty.buffer.Unpooled;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import it.unimi.dsi.fastutil.objects.Object2IntMap;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.NetworkStateBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.packet.PacketType;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import alexiil.mc.lib.net.InvalidInputDataException;
import alexiil.mc.lib.net.LibNetworkStack;
import alexiil.mc.lib.net.NetByteBuf;

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
            Collection<ServerPlayerEntity> players = PlayerLookup.around((ServerWorld) w, be.getPos(), distance);
            for (ServerPlayerEntity player : players) {
                ActiveServerConnection con = getServerConnection(player.networkHandler);
                if (con != null) {
                    list.add(con);
                }
            }
        }
        return list;
    }

    public static List<ActiveMinecraftConnection> getPlayersWatching(World world, BlockPos pos) {
        if (!(world instanceof ServerWorld)) {
            throw new IllegalArgumentException("getPlayersWatching must be given a ServerWorld instance.");
        }

        Collection<ServerPlayerEntity> players = PlayerLookup.tracking((ServerWorld) world, pos);
        List<ActiveMinecraftConnection> list = new ArrayList<>();
        for (ServerPlayerEntity player : players) {
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
        } else if (player.getWorld().isClient) {
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
        if (LibNetworkStack.DEBUG) {
            LibNetworkStack.LOGGER.info("Loading CoreMinecraftNetUtil...");
        }
        
        PayloadTypeRegistry.playC2S().register(NormalPacketPayload.ID, NormalPacketPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NormalPacketPayload.ID, NormalPacketPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(NormalPacketPayload.ID, (payload, context) -> {
            ActiveServerConnection connection = getServerConnection(context.player().networkHandler);
            if (connection == null) {
                return;
            }

            NetByteBuf b = NetByteBuf.asNetByteBuf(Unpooled.wrappedBuffer(payload.payload()));
            // on fabric, PLAY receivers run on main threads
            try {
                connection.onReceiveRawData(b);
            } catch (InvalidInputDataException e) {
                e.printStackTrace();
                context.responseSender().disconnect(
                    Text.of("LibNetworkStack: read error (see server logs for more details)\n" + e)
                );
            } finally {
                b.release();
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> onServerTick());
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> onServerStop());
    }

    public static void addToServerPackets(NetworkStateBuilder<ServerPlayPacketListener, RegistryByteBuf> builder) {
        if (LibNetworkStack.DEBUG) {
            LibNetworkStack.LOGGER.info("Adding CompactDataPacketToServer...");
        }
        builder.add(CompactDataPacketToServer.TYPE, CompactDataPacketToServer.CODEC);
    }

    public static void addToClientPackets(NetworkStateBuilder<ClientPlayPacketListener, RegistryByteBuf> builder) {
        if (LibNetworkStack.DEBUG) {
            LibNetworkStack.LOGGER.info("Adding CompactDataPacketToClient...");
        }
        builder.add(CompactDataPacketToClient.TYPE, CompactDataPacketToClient.CODEC);
    }

    public static void setExpectedIds(Object2IntMap<PacketType<?>> packetTypeIds) {
        if (packetTypeIds.containsKey(CompactDataPacketToClient.TYPE)) {
            if (LibNetworkStack.DEBUG) {
                LibNetworkStack.LOGGER.info("Extracting CompactDataPacketToClient id...");
            }
            clientExpectedId = packetTypeIds.getInt(CompactDataPacketToClient.TYPE);
        }
        if (packetTypeIds.containsKey(CompactDataPacketToServer.TYPE)) {
            if (LibNetworkStack.DEBUG) {
                LibNetworkStack.LOGGER.info("Extracting CompactDataPacketToServer id...");
            }
            serverExpectedId = packetTypeIds.getInt(CompactDataPacketToServer.TYPE);
        }
    }

    @Environment(EnvType.CLIENT)
    public static void loadClient() {
        ClientPlayNetworking.registerGlobalReceiver(NormalPacketPayload.ID, (payload, context) -> {
            ActiveClientConnection connection = getOrCreateClientConnection(context.player().networkHandler);
            
            NetByteBuf b = NetByteBuf.asNetByteBuf(Unpooled.wrappedBuffer(payload.payload()));
            // on fabric, PLAY receivers run on main threads
            try {
                connection.onReceiveRawData(b);
                b.release();
            } catch (InvalidInputDataException e) {
                e.printStackTrace();
                context.responseSender()
                    .disconnect(Text.of("LibNetworkStack: read error (see logs for details)"));
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> onClientTick());
    }

    @Environment(EnvType.CLIENT)
    static void onClientReceivePacket(ClientPlayPacketListener listener, NetByteBuf buffer) {
        if (!(listener instanceof ClientPlayNetworkHandler ctx)) return;
        ActiveClientConnection connection = getOrCreateClientConnection(ctx);
        NetByteBuf b = buffer.copy();
        MinecraftClient.getInstance().execute(() -> {
            try {
                connection.onReceiveRawData(b);
                b.release();
            } catch (InvalidInputDataException e) {
                e.printStackTrace();
                ctx.getConnection().disconnect(Text.of("LibNetworkStack: read error (see logs for details)"));
            }
        });
    }

    /**
     * @return The current client connection. Throws an exception if this is not available right now (likely because
     * the client hasn't joined a world).
     */
    public static ActiveClientConnection getClientConnection() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            throw new IllegalStateException("The client is not currently connected to any server!");
        }
        return getOrCreateClientConnection(player.networkHandler);
    }

    /**
     * @return The current {@link ActiveClientConnection}, or null if one has not been created yet.
     */
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

    static void onServerReceivePacket(ServerPlayPacketListener listener, NetByteBuf buffer) {
        if (!(listener instanceof ServerPlayNetworkHandler ctx)) return;
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
                ctx.disconnect(Text.of("LibNetworkStack: read error (see server logs for more details)\n" + e));
            }
        });
    }

    private static ActiveServerConnection getServerConnection(ServerPlayNetworkHandler netHandler) {
        return serverConnections.computeIfAbsent(netHandler, c -> {
            if (!netHandler.isConnectionOpen()) {
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
            if (!connection.netHandler.isConnectionOpen()) {
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
