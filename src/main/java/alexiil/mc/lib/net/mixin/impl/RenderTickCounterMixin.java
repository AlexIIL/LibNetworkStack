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

import net.minecraft.client.render.RenderTickCounter;

import alexiil.mc.lib.net.mixin.api.ITickCounterMixin;

@Mixin(RenderTickCounter.class)
public class RenderTickCounterMixin implements ITickCounterMixin {

    @Shadow
    private long prevTimeMillis;

    @Override
    public long libnetworkstack__getPrevTimeMillis() {
        return prevTimeMillis;
    }
}
