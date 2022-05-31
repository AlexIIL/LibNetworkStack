/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net.impl;

import java.util.Objects;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.ScreenHandler;

import alexiil.mc.lib.net.ActiveConnection;
import alexiil.mc.lib.net.IMsgReadCtx;
import alexiil.mc.lib.net.IMsgWriteCtx;
import alexiil.mc.lib.net.InvalidInputDataException;
import alexiil.mc.lib.net.MessageContext;
import alexiil.mc.lib.net.NetByteBuf;
import alexiil.mc.lib.net.NetObjectCache;
import alexiil.mc.lib.net.NetObjectCache.IEntrySerialiser;
import alexiil.mc.lib.net.ParentNetId;
import alexiil.mc.lib.net.ParentNetIdSingle;

import it.unimi.dsi.fastutil.Hash;

/** Holder for everything related to a normal minecraft connection. */
public final class McNetworkStack {
    private McNetworkStack() {}

    /** The root parent - everything must use as it's parent (somewhere up the chain) or it can never be written out to
     * a minecraft connection. */
    public static final ParentNetId ROOT = new ParentNetId(null, "");

    public static final ParentNetIdSingle<BlockEntity> BLOCK_ENTITY;
    public static final ParentNetIdSingle<Entity> ENTITY;

    public static final ParentNetIdSingle<ScreenHandler> SCREEN_HANDLER;

    /** @deprecated Use {@link #SCREEN_HANDLER} instead, due to the yarn name change. */
    @Deprecated
    public static final ParentNetIdSingle<ScreenHandler> CONTAINER;

    public static final NetObjectCache<ItemStack> CACHE_ITEMS_WITHOUT_AMOUNT;

    static {
        BLOCK_ENTITY = new ParentNetIdSingle<BlockEntity>(ROOT, BlockEntity.class, "block_entity", -1) {
            @Override
            public BlockEntity readContext(NetByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException {
                ActiveMinecraftConnection mcConn = (ActiveMinecraftConnection) ctx.getConnection();
                PlayerEntity player = mcConn.ctx.getPlayer();
                return player.world.getBlockEntity(buffer.readBlockPos());
            }

            @Override
            public void writeContext(NetByteBuf buffer, IMsgWriteCtx ctx, BlockEntity value) {
                buffer.writeBlockPos(value.getPos());

                // begin TMP_FIX_BLANKETCON_2022_ALEX_01
                // TO REVERT:
                // 1: Remove everything between start and end (the next 3 lines)
                if (ctx instanceof MessageContext.Write wCtx) {
                    wCtx.blockEntity = value;
                }
                // end TMP_FIX_BLANKETCON_2022_ALEX_01
            }
        };
        ENTITY = new ParentNetIdSingle<Entity>(ROOT, Entity.class, "entity", Integer.BYTES) {
            @Override
            public Entity readContext(NetByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException {
                ActiveMinecraftConnection mcConn = (ActiveMinecraftConnection) ctx.getConnection();
                PlayerEntity player = mcConn.ctx.getPlayer();
                return player.world.getEntityById(buffer.readInt());
            }

            @Override
            public void writeContext(NetByteBuf buffer, IMsgWriteCtx ctx, Entity value) {
                buffer.writeInt(value.getId());
            }
        };
        // We're not changing the internal name, because that would needlessly break compat
        SCREEN_HANDLER = new ParentNetIdSingle<ScreenHandler>(ROOT, ScreenHandler.class, "container", Integer.BYTES) {
            @Override
            public ScreenHandler readContext(NetByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException {
                ActiveMinecraftConnection mcConn = (ActiveMinecraftConnection) ctx.getConnection();
                PlayerEntity player = mcConn.ctx.getPlayer();
                int syncId = buffer.readInt();
                ScreenHandler currentContainer = player.currentScreenHandler;
                if (currentContainer == null || currentContainer.syncId != syncId) {
                    return null;
                }
                return currentContainer;
            }

            @Override
            public void writeContext(NetByteBuf buffer, IMsgWriteCtx ctx, ScreenHandler value) {
                buffer.writeInt(value.syncId);
            }
        };
        CONTAINER = SCREEN_HANDLER;

        CACHE_ITEMS_WITHOUT_AMOUNT
            = new NetObjectCache<>(ROOT.child("cache_item_stack"), new Hash.Strategy<ItemStack>() {

                @Override
                public int hashCode(ItemStack o) {
                    return Objects.hash(new Object[] { o.getItem(), o.getNbt() });
                }

                @Override
                public boolean equals(ItemStack a, ItemStack b) {
                    if (a == null || b == null) {
                        return a == b;
                    }
                    if (a.getItem() != b.getItem()) {
                        return false;
                    }
                    NbtCompound tagA = a.getNbt();
                    NbtCompound tagB = b.getNbt();
                    if (tagA == null || tagB == null) {
                        return tagA == tagB;
                    }
                    return tagA.equals(tagB);
                }

            }, new IEntrySerialiser<ItemStack>() {

                @Override
                public void write(ItemStack obj, ActiveConnection connection, NetByteBuf buffer) {
                    if (obj.isEmpty()) {
                        buffer.writeBoolean(false);
                    } else {
                        buffer.writeBoolean(true);
                        Item item_1 = obj.getItem();
                        buffer.writeVarInt(Item.getRawId(item_1));
                        NbtCompound tag = null;
                        if (item_1.isDamageable() || item_1.isNbtSynced()) {
                            tag = obj.getNbt();
                        }
                        buffer.writeNbt(tag);
                    }
                }

                @Override
                public ItemStack read(ActiveConnection connection, NetByteBuf buffer) {
                    if (!buffer.readBoolean()) {
                        return ItemStack.EMPTY;
                    } else {
                        int id = buffer.readVarInt();
                        ItemStack stack = new ItemStack(Item.byRawId(id));
                        stack.setNbt(buffer.readNbt());
                        return stack;
                    }
                }
            });
    }
}
