/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.net.test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;

import alexiil.mc.lib.net.ActiveConnection;
import alexiil.mc.lib.net.DynamicNetId;
import alexiil.mc.lib.net.DynamicNetLink;
import alexiil.mc.lib.net.DynamicNetLink.IDynamicLinkFactory;
import alexiil.mc.lib.net.IMsgReadCtx;
import alexiil.mc.lib.net.IMsgWriteCtx;
import alexiil.mc.lib.net.InvalidInputDataException;
import alexiil.mc.lib.net.MsgUtil;
import alexiil.mc.lib.net.NetByteBuf;
import alexiil.mc.lib.net.NetIdBase;
import alexiil.mc.lib.net.NetIdData;
import alexiil.mc.lib.net.NetIdDataK;
import alexiil.mc.lib.net.NetIdDataK.IMsgDataWriterK;
import alexiil.mc.lib.net.NetIdSignal;
import alexiil.mc.lib.net.NetIdSignalK;
import alexiil.mc.lib.net.ParentDynamicNetId;
import alexiil.mc.lib.net.ParentNetId;
import alexiil.mc.lib.net.ParentNetIdCast;
import alexiil.mc.lib.net.ParentNetIdDuel;
import alexiil.mc.lib.net.ParentNetIdSingle;
import alexiil.mc.lib.net.impl.CoreMinecraftNetUtil;
import alexiil.mc.lib.net.impl.McNetworkStack;
import net.minecraft.util.math.BlockPos;

public class SimpleNetTester {

    static abstract class TestConnection extends ActiveConnection {
        final int id;

        private TestConnection(ParentNetId rootId, int id) {
            super(rootId);
            this.id = id;
        }

        @Override
        public String toString() {
            return "TestConnection " + id;
        }
    }

    static ActiveConnection side1;
    static ActiveConnection side2;

    static final Queue<NetByteBuf> incomingSide1 = new ArrayDeque<>();
    static final Queue<NetByteBuf> incomingSide2 = new ArrayDeque<>();

    public static final ParentNetId ROOT = new ParentNetId(null, "");
    public static final NetIdSignal SIGNAL_PING = ROOT.idSignal("ping");
    public static final NetIdSignal SIGNAL_PONG = ROOT.idSignal("pong");
    public static final NetIdData DATA_STRING = ROOT.idData("data");

    public static final ParentNetId TF2_GAME = ROOT.child("tf2");
    public static final ParentNetId TF2_CLASS_SELCTION = TF2_GAME.child("class");
    public static final ParentNetId TF2_CLASS_SPY = TF2_CLASS_SELCTION.child("spy");

    public static final NetIdData SPY_EQUIP = TF2_CLASS_SPY.idData("equip");
    public static final NetIdSignal SPY_SHOOT = TF2_CLASS_SPY.idSignal("shoot");

    public static Tf2Player[] players = new Tf2Player[1];

    public static final ParentNetIdSingle<Tf2Team> TF2_TEAM
        = new ParentNetIdSingle<Tf2Team>(TF2_GAME, Tf2Team.class, "team", -1) {
            @Override
            protected Tf2Team readContext(NetByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException {
                return buffer.readBoolean() ? Tf2Team.RED : Tf2Team.BLUE;
            }

            @Override
            protected void writeContext(NetByteBuf buffer, IMsgWriteCtx ctx, Tf2Team team) {
                buffer.writeBoolean(team == Tf2Team.RED);
            }
        };

    public static final NetIdDataK<Tf2Team> TEAM_WON = TF2_TEAM.idData("did_win").setReceiver((team, buf, ctx) -> {
        boolean won = buf.readBoolean();
        System.out.println(team + (won ? " won!" : " lost :("));
    });

    public static final DynamicNetId<AnimationElement> ANIMATED
        = new DynamicNetId<>(AnimationElement.class, elem -> elem.netLink);
    public static final NetIdSignalK<AnimationElement> ANIMATE_FRAME = ANIMATED.idSignal("animate_frame");
    public static final NetIdDataK<AnimationElement> ANIMATE_MOVE
        = ANIMATED.idData("animate_move").setReadWrite(AnimationElement::read, AnimationElement::write);

    public static final ParentNetIdSingle<Tf2Player> TF2_PLAYER
        = new ParentNetIdSingle<Tf2Player>(TF2_GAME, Tf2Player.class, "player", Byte.BYTES) {
            @Override
            public Tf2Player readContext(NetByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException {
                return players[buffer.readByte()];
            }

            @Override
            public void writeContext(NetByteBuf buffer, IMsgWriteCtx ctx, Tf2Player value) {
                buffer.writeByte(Arrays.asList(players).indexOf(value));
            }
        };

    public static final ParentDynamicNetId<Tf2Player, AnimationElement> PLAYER_ANIMATION
        = new ParentDynamicNetId<>(TF2_PLAYER, "animation", ANIMATED, pl -> pl.animation);

    public static final ParentNetIdCast<Tf2Player, Tf2Medic> TF2_MEDIC = TF2_PLAYER.subType(Tf2Medic.class, "medic");

    public static final NetIdSignalK<Tf2Player> SPAM_E_KEY = TF2_PLAYER.idSignal("spam_the_e_key");
    public static final NetIdDataK<Tf2Medic> SET_HEAL_TARGET = TF2_MEDIC.idData("set_heal_target");

    public static final ParentNetIdDuel<Tf2Player, Tf2Weapon> PLAYER_WEAPON
        = new ParentNetIdDuel<Tf2Player, Tf2Weapon>(TF2_PLAYER, "held_weapon", Tf2Weapon.class, Byte.BYTES) {
            @Override
            protected void writeContext0(NetByteBuf buffer, IMsgWriteCtx ctx, Tf2Weapon weapon) {
                buffer.writeByte(weapon.holder.weapons.indexOf(weapon));
            }

            @Override
            protected Tf2Weapon readContext(NetByteBuf buffer, IMsgReadCtx ctx, Tf2Player player)
                throws InvalidInputDataException {
                int weaponIndex = buffer.readUnsignedByte();
                return player.weapons.get(weaponIndex);
            }

            @Override
            protected Tf2Player extractParent(Tf2Weapon weapon) {
                return weapon.holder;
            }
        };

    public static final ParentDynamicNetId<Tf2Weapon, AnimationElement> WEAPON_ANIMATION
        = new ParentDynamicNetId<>(PLAYER_WEAPON, "animation", ANIMATED, w -> w.animation);

    public static class AnimationElement {
        public final DynamicNetLink<?, AnimationElement> netLink;
        public int frames = 0;

        public AnimationElement(IDynamicLinkFactory<AnimationElement> netLink) {
            this.netLink = netLink.create(this);
        }

        @Override
        public String toString() {
            return "Animation " + frames + " in " + netLink.parent;
        }

        public void read(NetByteBuf buffer, IMsgReadCtx ctx) {
            buffer.readString();
            buffer.readDouble();
            buffer.readBoolean();
            // buffer.readDouble();
            // buffer.readShort();
        }

        public void write(NetByteBuf buffer, IMsgWriteCtx ctx) {
            buffer.writeString("Hi");
            buffer.writeDouble(45.6);
            buffer.writeBoolean(true);
            buffer.writeDouble(Math.random());
        }
    }

    public static class Tf2Weapon {
        public final Tf2Player holder;
        public final String name;
        public final int clipSize;
        public final AnimationElement animation = new AnimationElement(WEAPON_ANIMATION.linkFactory(this));
        public int bullets;

        public Tf2Weapon(Tf2Player holder, String name, int clipSize) {
            this.holder = holder;
            this.name = name;
            this.clipSize = clipSize;
        }

        @Override
        public String toString() {
            return "Weapon " + name + " of " + holder;
        }
    }

    public enum Tf2Team {
        RED,
        BLUE;
    }

    public static abstract class Tf2Player {
        public final List<Tf2Weapon> weapons = new ArrayList<>();
        public final AnimationElement animation = new AnimationElement(PLAYER_ANIMATION.linkFactory(this));

        @Override
        public String toString() {
            return "{Player}";
        }
    }

    public static class Tf2Medic extends Tf2Player {

        public Tf2Medic() {
            weapons.add(new Tf2Weapon(this, "Syringe Gun", 40));
            weapons.add(new Tf2Weapon(this, "Crusader's Crossbow", 1));
        }

        @Override
        public String toString() {
            return "{Medic}";
        }

    }

    public static void main(String[] args) throws InvalidInputDataException {
        side1 = new TestConnection(ROOT, 1) {
            @Override
            public void sendPacket(NetByteBuf data, int packetId, NetIdBase netId, int priority) {
                incomingSide2.add(data.copy());
            }
        };
        side2 = new TestConnection(ROOT, 2) {
            @Override
            public void sendPacket(NetByteBuf data, int packetId, NetIdBase netId, int priority) {
                incomingSide1.add(data.copy());
            }
        };
        side1.postConstruct();
        side2.postConstruct();
        process();

        TEAM_WON.send(side1, Tf2Team.RED, (team, buf, ctx) -> buf.writeBoolean(true));
        TEAM_WON.send(side1, Tf2Team.RED, (team, buf, ctx) -> buf.writeBoolean(false));
        TEAM_WON.send(side1, Tf2Team.BLUE, (team, buf, ctx) -> buf.writeBoolean(true));
        TEAM_WON.send(side1, Tf2Team.BLUE, (team, buf, ctx) -> buf.writeBoolean(false));

        process();

        TEAM_WON.send(side2, Tf2Team.RED, (team, buf, ctx) -> buf.writeBoolean(true));
        TEAM_WON.send(side2, Tf2Team.RED, (team, buf, ctx) -> buf.writeBoolean(false));
        TEAM_WON.send(side2, Tf2Team.BLUE, (team, buf, ctx) -> buf.writeBoolean(true));
        TEAM_WON.send(side2, Tf2Team.BLUE, (team, buf, ctx) -> buf.writeBoolean(false));

        process();

        SIGNAL_PING.setReceiver(ctx -> {
            System.out.println("Recv Ping on " + ctx.getConnection());
            SIGNAL_PONG.send(ctx.getConnection());

        });
        SIGNAL_PONG.setReceiver(ctx -> {
            System.out.println("Recv Pong on " + ctx.getConnection());
        });
        DATA_STRING.setReceiver((buffer, ctx) -> {
            String str = MsgUtil.readUTF(buffer);
            System.out.println("Recv Data '" + str + "' on " + ctx.getConnection());
        });
        SPY_EQUIP.setReceiver((buffer, ctx) -> {
            int id = buffer.readByte();
            System.out.println("Recv spy equip " + spyWeaponName(id));
        });
        SPAM_E_KEY.setReceiver((medic, ctx) -> {
            System.out.println(medic + " spammed the 'E' key!");
        });
        SET_HEAL_TARGET.setReceiver((medic, buffer, ctx) -> {
            String player = MsgUtil.readUTF(buffer);
            System.out.println(medic + " is now targetting '" + player + "'");
        });
        ANIMATE_FRAME.setReceiver((elem, ctx) -> {
            elem.frames++;
            // System.out.println("Animated a frame of " + elem);
        });

        SIGNAL_PING.send(side1);
        SIGNAL_PING.send(side1);
        SIGNAL_PING.send(side1);
        SIGNAL_PING.send(side1);
        process();
        SIGNAL_PING.send(side2);

        DATA_STRING.send(side1, (buffer, ctx) -> {
            MsgUtil.writeUTF(buffer, "Hi!");
        });
        DATA_STRING.send(side1, (buffer, ctx) -> {
            MsgUtil.writeUTF(buffer, "Network test 2");
        });
        process();
        SPY_EQUIP.send(side1, (buffer, ctx) -> {
            buffer.writeByte(new Random().nextInt(SPY_WEAPONS.length));
        });
        process();

        players[0] = new Tf2Medic();

        SPAM_E_KEY.send(side1, players[0]);
        process();
        SET_HEAL_TARGET.send(side1, (Tf2Medic) players[0], (medic, buffer, ctx) -> {
            buffer.writeMarker("target_player");
            MsgUtil.writeUTF(buffer, "Scout");
            buffer.writeMarker("some_data");
            buffer.writeBoolean(true);
            buffer.writeVarInt(10);

        });
        process();

        for (int i = 0; i < 1000; i++) {
            ANIMATE_FRAME.send(side1, players[0].animation);
            process();
            ANIMATE_FRAME.send(side1, players[0].weapons.get(0).animation);
            process();
            ANIMATE_FRAME.send(side1, players[0].weapons.get(0).animation);
            ANIMATE_FRAME.send(side1, players[0].weapons.get(1).animation);
            ANIMATE_FRAME.send(side1, players[0].animation);
            ANIMATE_FRAME.send(side1, players[0].weapons.get(0).animation);
            ANIMATE_FRAME.send(side1, players[0].weapons.get(0).animation);
            process();
            ANIMATE_MOVE.send(side1, players[0].weapons.get(1).animation);

            process();
        }

        System.out.println(players[0].animation.frames);
        System.out.println(players[0].weapons.get(0).animation.frames);
        System.out.println(players[0].weapons.get(1).animation.frames);
    }

    private static final String[] SPY_WEAPONS = { //
        "Revolver", "Ambassador", "Diamondback", "L'Etranger", "Enforcer", //
        "Knife", "Black Rose", "Your Eternal Reward", "Conniver's Kunai", "Big Earner", "Spy-cicle", //
        "Invis Watch", "Cloak and Dagger", "Dead Ringer", //
        "Sapper", "Ap-Sap", "Snack Attack", "Red-Tape Recorder", //
        "Disguise Kit"//
    };

    private static String spyWeaponName(int id) {
        if (id < 0 || id >= SPY_WEAPONS.length) {
            return "?" + id + "?";
        }
        return SPY_WEAPONS[id];
    }

    private static void process() throws InvalidInputDataException {
        boolean read1 = false, read2 = false;
        do {
            read1 = false;
            read2 = false;
            NetByteBuf buf;
            if ((buf = incomingSide1.poll()) != null) {
                side1.onReceiveRawData(buf);
                buf.release();
                read1 = true;
            }
            if ((buf = incomingSide2.poll()) != null) {
                side2.onReceiveRawData(buf);
                buf.release();
                read2 = true;
            }
        } while (read1 | read2);
    }

    // The example from the README.md (approx)
    public static class ElectricFurnaceBlockEntity extends BlockEntity {

        public static final ParentNetIdSingle<ElectricFurnaceBlockEntity> NET_PARENT;
        public static final NetIdDataK<ElectricFurnaceBlockEntity> ID_CHANGE_BRIGHTNESS;
        public static final NetIdSignalK<ElectricFurnaceBlockEntity> ID_TURN_ON, ID_TURN_OFF;

        static {
            NET_PARENT = McNetworkStack.BLOCK_ENTITY
                .subType(ElectricFurnaceBlockEntity.class, "electrical_nightmare:electric_furnace");
            ID_CHANGE_BRIGHTNESS = NET_PARENT.idData("CHANGE_BRIGHTNESS")
                .setReceiver(ElectricFurnaceBlockEntity::receiveBrightnessChange);
            ID_TURN_ON = NET_PARENT.idSignal("TURN_ON").setReceiver(ElectricFurnaceBlockEntity::receiveTurnOn);
            ID_TURN_OFF = NET_PARENT.idSignal("TURN_OFF").setReceiver(ElectricFurnaceBlockEntity::receiveTurnOff);
        }

        /** Used for rendering - somehow. */
        public int clientBrightness;
        public boolean clientIsOn;

        public ElectricFurnaceBlockEntity() {
            super(null, BlockPos.ORIGIN, Blocks.AIR.getDefaultState());
        }

        /** Sends the new brightness, to be rendered on the front of the block.
         * 
         * @param newBrightness A value between 0 and 255. */
        protected final void sendBrightnessChange(int newBrightness) {
            for (ActiveConnection connection : CoreMinecraftNetUtil.getPlayersWatching(getWorld(), getPos())) {
                ID_CHANGE_BRIGHTNESS.send(connection, this, new IMsgDataWriterK<ElectricFurnaceBlockEntity>() {

                    // In your own code you probably want to change this to a lambda
                    // but the full types are used here for clarity.

                    @Override
                    public void write(ElectricFurnaceBlockEntity be, NetByteBuf buf, IMsgWriteCtx ctx) {
                        ctx.assertServerSide();

                        buf.writeByte(newBrightness);
                    }
                });
            }
        }

        /** Sends the new on/off state, to be rendered on the front of the block. */
        protected final void sendSwitchState(boolean isOn) {
            for (ActiveConnection connection : CoreMinecraftNetUtil.getPlayersWatching(getWorld(), getPos())) {
                (isOn ? ID_TURN_ON : ID_TURN_OFF).send(connection, this);
            }
        }

        protected void receiveBrightnessChange(NetByteBuf buf, IMsgReadCtx ctx) throws InvalidInputDataException {
            // Ensure that this was sent by the server, to the client (and that we are on the client side)
            ctx.assertClientSide();

            this.clientBrightness = buf.readUnsignedByte();
        }

        protected void receiveTurnOn(IMsgReadCtx ctx) throws InvalidInputDataException {
            // Ensure that this was sent by the server, to the client (and that we are on the client side)
            ctx.assertClientSide();

            this.clientIsOn = true;
        }

        protected void receiveTurnOff(IMsgReadCtx ctx) throws InvalidInputDataException {
            // Ensure that this was sent by the server, to the client (and that we are on the client side)
            ctx.assertClientSide();

            this.clientIsOn = false;
        }
    }
}
