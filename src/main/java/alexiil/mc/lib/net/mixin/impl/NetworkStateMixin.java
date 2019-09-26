/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net.mixin.impl;

import java.util.Map;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import org.apache.logging.log4j.LogManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.network.NetworkSide;
import net.minecraft.network.NetworkState;
import net.minecraft.network.Packet;

import alexiil.mc.lib.net.impl.IPacketCustomId;
import alexiil.mc.lib.net.mixin.api.INetworkStateMixin;

@Mixin(NetworkState.class)
public class NetworkStateMixin implements INetworkStateMixin {

    @Final
    @Shadow
    private static Map<Class<? extends Packet<?>>, NetworkState> HANDLER_STATE_MAP;

    @Final
    @Shadow
    private Map<NetworkSide, BiMap<Integer, Class<? extends Packet<?>>>> packetHandlerMap;

    @Override
    public int libnetworkstack_registerPacket(NetworkSide recvSide, Class<? extends IPacketCustomId<?>> clazz) {
        BiMap<Integer, Class<? extends Packet<?>>> biMap = packetHandlerMap.get(recvSide);
        if (biMap == null) {
            biMap = HashBiMap.create();
            packetHandlerMap.put(recvSide, biMap);
        }

        if (biMap.containsValue(clazz)) {
            String string_1 = recvSide + " packet " + clazz + " is already known to ID " + biMap.inverse().get(clazz);
            LogManager.getLogger().fatal(string_1);
            throw new IllegalArgumentException(string_1);
        } else {
            int index = biMap.size();
            biMap.put(index, clazz);
            HANDLER_STATE_MAP.put(clazz, (NetworkState) (Object) this);
            return index;
        }
    }

    @Inject(
        at = @At("HEAD"),
        method = "getPacketId(Lnet/minecraft/network/NetworkSide;Lnet/minecraft/network/Packet;)Ljava/lang/Integer;",
        cancellable = true)
    private void getPacketId(NetworkSide side, Packet<?> pkt, CallbackInfoReturnable<Integer> ci) throws Exception {
        if (pkt instanceof IPacketCustomId) {
            ci.setReturnValue(((IPacketCustomId<?>) pkt).getReadId());
        }
    }
}
