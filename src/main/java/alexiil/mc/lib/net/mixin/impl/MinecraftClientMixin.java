/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net.mixin.impl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;

import alexiil.mc.lib.net.impl.ActiveClientConnection;
import alexiil.mc.lib.net.impl.CoreMinecraftNetUtil;
import alexiil.mc.lib.net.mixin.api.ITickCounterMixin;

@Mixin(value = MinecraftClient.class)
public class MinecraftClientMixin {

    @Shadow
    private RenderTickCounter renderTickCounter;

    @Inject(at = @At(value = "CONSTANT", args = { "stringValue=scheduledExecutables" }), method = "render(Z)V")
    private void afterIncrementTickCounter(CallbackInfo ci) {
        ActiveClientConnection acc = CoreMinecraftNetUtil.getCurrentClientConnection();
        if (acc != null) {
            acc.onIncrementMinecraftTickCounter(
                ((ITickCounterMixin) renderTickCounter).libnetworkstack__getPrevTimeMillis()
            );
        }
    }
}
