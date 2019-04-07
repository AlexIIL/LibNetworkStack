package alexiil.mc.lib.net;

import java.util.Objects;

import io.netty.buffer.ByteBuf;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.container.Container;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

import alexiil.mc.lib.net.NetObjectCache.IEntrySerialiser;
import alexiil.mc.lib.net.impl.ActiveMinecraftConnection;

import it.unimi.dsi.fastutil.Hash;

/** Holder for everything related to a normal minecraft connection. */
public class McNetworkStack {

    /** The root parent - everything must use as it's parent (somewhere up the chain) or it can never be written out to
     * a minecraft connection. */
    public static final ParentNetId ROOT = new ParentNetId(null, "");

    public static final ParentNetIdSingle<BlockEntity> BLOCK_ENTITY;
    public static final ParentNetIdSingle<Entity> ENTITY;
    public static final ParentNetIdSingle<Container> CONTAINER;

    public static final NetObjectCache<ItemStack> CACHE_ITEMS_WITHOUT_AMOUNT;

    static {
        BLOCK_ENTITY = new ParentNetIdSingle<BlockEntity>(ROOT, BlockEntity.class, "block_entity", 3 * Integer.BYTES) {
            @Override
            public BlockEntity readContext(ByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException {
                ActiveMinecraftConnection mcConn = (ActiveMinecraftConnection) ctx.getConnection();
                PlayerEntity player = mcConn.ctx.getPlayer();
                int x = buffer.readInt();
                int y = buffer.readInt();
                int z = buffer.readInt();
                return player.world.getBlockEntity(new BlockPos(x, y, z));
            }

            @Override
            public void writeContext(ByteBuf buffer, IMsgWriteCtx ctx, BlockEntity value) {
                buffer.writeInt(value.getPos().getX());
                buffer.writeInt(value.getPos().getY());
                buffer.writeInt(value.getPos().getZ());
            }
        };
        ENTITY = new ParentNetIdSingle<Entity>(ROOT, Entity.class, "entity", Integer.BYTES) {
            @Override
            public Entity readContext(ByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException {
                ActiveMinecraftConnection mcConn = (ActiveMinecraftConnection) ctx.getConnection();
                PlayerEntity player = mcConn.ctx.getPlayer();
                return player.world.getEntityById(buffer.readInt());
            }

            @Override
            public void writeContext(ByteBuf buffer, IMsgWriteCtx ctx, Entity value) {
                buffer.writeInt(value.getEntityId());
            }
        };
        CONTAINER = new ParentNetIdSingle<Container>(ROOT, Container.class, "container", Integer.BYTES) {
            @Override
            public Container readContext(ByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException {
                ActiveMinecraftConnection mcConn = (ActiveMinecraftConnection) ctx.getConnection();
                PlayerEntity player = mcConn.ctx.getPlayer();
                int syncId = buffer.readInt();
                Container currentContainer = player.container;
                if (currentContainer == null || currentContainer.syncId != syncId) {
                    return null;
                }
                return currentContainer;
            }

            @Override
            public void writeContext(ByteBuf buffer, IMsgWriteCtx ctx, Container value) {
                buffer.writeInt(value.syncId);
            }
        };

        CACHE_ITEMS_WITHOUT_AMOUNT =
            new NetObjectCache<>(ROOT.child("cache_item_stack"), new Hash.Strategy<ItemStack>() {

                @Override
                public int hashCode(ItemStack o) {
                    return Objects.hash(new Object[] { o.getItem(), o.getTag() });
                }

                @Override
                public boolean equals(ItemStack a, ItemStack b) {
                    if (a == null || b == null) {
                        return a == b;
                    }
                    if (a.getItem() != b.getItem()) {
                        return false;
                    }
                    CompoundTag tagA = a.getTag();
                    CompoundTag tagB = b.getTag();
                    if (tagA == null || tagB == null) {
                        return tagA == tagB;
                    }
                    return tagA.equals(tagB);
                }

            }, new IEntrySerialiser<ItemStack>() {

                @Override
                public void write(ItemStack obj, ActiveConnection connection, ByteBuf buffer) {
                    PacketByteBuf packetBuf = new PacketByteBuf(buffer);
                    if (obj.isEmpty()) {
                        packetBuf.writeBoolean(false);
                    } else {
                        packetBuf.writeBoolean(true);
                        Item item_1 = obj.getItem();
                        packetBuf.writeVarInt(Item.getRawIdByItem(item_1));
                        CompoundTag tag = null;
                        if (item_1.canDamage() || item_1.requiresClientSync()) {
                            tag = obj.getTag();
                        }
                        packetBuf.writeCompoundTag(tag);
                    }
                }

                @Override
                public ItemStack read(ActiveConnection connection, ByteBuf buffer) {
                    PacketByteBuf packetBuf = new PacketByteBuf(buffer);
                    if (!packetBuf.readBoolean()) {
                        return ItemStack.EMPTY;
                    } else {
                        int id = packetBuf.readVarInt();
                        ItemStack stack = new ItemStack(Item.byRawId(id));
                        stack.setTag(packetBuf.readCompoundTag());
                        return stack;
                    }
                }
            });
    }
}