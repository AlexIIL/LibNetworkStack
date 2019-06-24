/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.api.ModInitializer;

import alexiil.mc.lib.net.impl.CoreMinecraftNetUtil;

public class LibNetworkStack implements ModInitializer {

    public static final boolean DEBUG = Boolean.getBoolean("libnetworkstack.debug");
    public static final Logger LOGGER = LogManager.getLogger("LibNetworkStack");

    @Override
    public void onInitialize() {
        CoreMinecraftNetUtil.load();
    }
}
