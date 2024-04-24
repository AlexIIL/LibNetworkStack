/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net.mixin.impl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.network.NetworkStateBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.state.PlayStateFactories;

import alexiil.mc.lib.net.impl.CoreMinecraftNetUtil;

// Mixes into the lambdas where packet types are being registered to add our compact data packets
@Mixin(PlayStateFactories.class)
public class PlayStateFactoriesMixin {
    @Inject(method = "method_55959", at = @At("RETURN"))
    private static void onConstructC2SFactory(NetworkStateBuilder<ServerPlayPacketListener, RegistryByteBuf> builder, CallbackInfo ci) {
        CoreMinecraftNetUtil.addToServerPackets(builder);
    }
    
    @Inject(method = "method_55958", at = @At("RETURN"))
    private static void onConstructS2CFactory(NetworkStateBuilder<ClientPlayPacketListener, RegistryByteBuf> builder, CallbackInfo ci) {
        CoreMinecraftNetUtil.addToClientPackets(builder);
    }
}
