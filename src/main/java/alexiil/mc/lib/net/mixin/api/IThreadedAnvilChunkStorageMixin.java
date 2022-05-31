/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net.mixin.api;

import net.minecraft.server.world.ChunkHolder;
import net.minecraft.util.math.ChunkPos;

// begin TMP_FIX_BLANKETCON_2022_ALEX_01
public interface IThreadedAnvilChunkStorageMixin {
    ChunkHolder libnetworkstack_getChunkHolder(ChunkPos chunkPos);
}
// end TMP_FIX_BLANKETCON_2022_ALEX_01
