/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net.mixin.impl;

import java.util.List;
import java.util.function.Function;

import net.minecraft.network.PacketByteBuf;
import org.apache.logging.log4j.LogManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;

import alexiil.mc.lib.net.mixin.api.IPacketHandlerMixin;

import it.unimi.dsi.fastutil.objects.Object2IntMap;

@Mixin(targets = "net.minecraft.network.NetworkState$InternalPacketHandler")
abstract class InternalPacketHandlerMixin<T extends PacketListener> implements IPacketHandlerMixin {

    @Final
    @Shadow
    Object2IntMap<Class<? extends Packet<T>>> packetIds;

    @Final
    @Shadow
    private List<Function<PacketByteBuf, ? extends Packet<T>>> packetFactories;

    @Override
    public int libnetworkstack_register(Class<? extends Packet<?>> klass, Function<PacketByteBuf, ? extends Packet<?>> factory) {
        int int_1 = this.packetFactories.size();
        int int_2 = this.packetIds.put((Class<? extends Packet<T>>) klass, int_1);
        if (int_2 != -1) {
            String string_1 = "Packet " + klass + " is already registered to ID " + int_2;
            LogManager.getLogger().fatal(string_1);
            throw new IllegalArgumentException(string_1);
        } else {
            this.packetFactories.add((Function<PacketByteBuf, ? extends Packet<T>>) factory);
        }

        return int_1;
    }
}
