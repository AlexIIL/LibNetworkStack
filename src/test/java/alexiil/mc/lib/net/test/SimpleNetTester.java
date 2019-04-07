package alexiil.mc.lib.net.test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import io.netty.buffer.ByteBuf;

import net.fabricmc.fabric.api.network.PacketContext;

import alexiil.mc.lib.net.ActiveConnection;
import alexiil.mc.lib.net.DynamicNetId;
import alexiil.mc.lib.net.DynamicNetId.ParentData;
import alexiil.mc.lib.net.IMsgReadCtx;
import alexiil.mc.lib.net.IMsgWriteCtx;
import alexiil.mc.lib.net.InvalidInputDataException;
import alexiil.mc.lib.net.MsgUtil;
import alexiil.mc.lib.net.NetIdBase;
import alexiil.mc.lib.net.NetIdData;
import alexiil.mc.lib.net.NetIdDataK;
import alexiil.mc.lib.net.NetIdSignal;
import alexiil.mc.lib.net.NetIdSignalK;
import alexiil.mc.lib.net.ParentNetId;
import alexiil.mc.lib.net.ParentNetIdCast;
import alexiil.mc.lib.net.ParentNetIdDuel;
import alexiil.mc.lib.net.ParentNetIdSingle;

public class SimpleNetTester {

    static abstract class TestConnection extends ActiveConnection {
        final int id;

        private TestConnection(ParentNetId rootId, int id) {
            super(rootId);
            this.id = id;
        }

        @Override
        public PacketContext getMinecraftContext() {
            throw new IllegalStateException("This isn't a minecraft connection!");
        }

        @Override
        public String toString() {
            return "TestConnection " + id;
        }
    }

    static ActiveConnection side1;
    static ActiveConnection side2;

    static final Queue<ByteBuf> incomingSide1 = new ArrayDeque<>();
    static final Queue<ByteBuf> incomingSide2 = new ArrayDeque<>();

    public static final ParentNetId ROOT = new ParentNetId(null, "");
    public static final NetIdSignal SIGNAL_PING = ROOT.idSignal("ping");
    public static final NetIdSignal SIGNAL_PONG = ROOT.idSignal("pong");
    public static final NetIdData DATA_STRING = ROOT.idData("data");

    public static final ParentNetId TF2_GAME = ROOT.child("tf2");
    public static final ParentNetId TF2_CLASS_SELCTION = TF2_GAME.child("class");
    public static final ParentNetId TF2_CLASS_SPY = TF2_CLASS_SELCTION.child("spy");

    public static final NetIdData SPY_EQUIP = TF2_CLASS_SPY.idData("equip");
    public static final NetIdSignal SPY_SHOOT = TF2_CLASS_SPY.idSignal("shoot");

    public static Tf2Player[] players = new Tf2Player[] { //
        new Tf2Medic(),//
    };

    public static final ParentNetIdSingle<Tf2Player> TF2_PLAYER =
        new ParentNetIdSingle<Tf2Player>(TF2_GAME, Tf2Player.class, "player", Byte.BYTES) {
            @Override
            public Tf2Player readContext(ByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException {
                return players[buffer.readByte()];
            }

            @Override
            public void writeContext(ByteBuf buffer, IMsgWriteCtx ctx, Tf2Player value) {
                buffer.writeByte(Arrays.asList(players).indexOf(value));
            }
        };
    public static final ParentNetIdCast<Tf2Player, Tf2Medic> TF2_MEDIC = TF2_PLAYER.subType(Tf2Medic.class, "medic");

    public static final NetIdSignalK<Tf2Player> SPAM_E_KEY = TF2_PLAYER.idSignal("spam_the_e_key");
    public static final NetIdDataK<Tf2Medic> SET_HEAL_TARGET = TF2_MEDIC.idData("set_heal_target");

    public static final ParentNetIdDuel<Tf2Player, Tf2Weapon> PLAYER_WEAPON =
        new ParentNetIdDuel<Tf2Player, Tf2Weapon>(TF2_PLAYER, "held_weapon", Tf2Weapon.class, Byte.BYTES) {
            @Override
            protected void writeContext0(ByteBuf buffer, IMsgWriteCtx ctx, Tf2Weapon weapon) {
                buffer.writeByte(weapon.holder.weapons.indexOf(weapon));
            }

            @Override
            protected Tf2Weapon readContext(ByteBuf buffer, IMsgReadCtx ctx, Tf2Player player)
                throws InvalidInputDataException {
                int weaponIndex = buffer.readUnsignedByte();
                return player.weapons.get(weaponIndex);
            }

            @Override
            protected Tf2Player extractParent(Tf2Weapon weapon) {
                return weapon.holder;
            }
        };

    public static final DynamicNetId<AnimationElement> ANIMATED = new DynamicNetId<>(AnimationElement.class,
        "animation", SimpleNetTester::extractParentData, SimpleNetTester::extractChildData, TF2_PLAYER, PLAYER_WEAPON);

    public static final NetIdSignalK<AnimationElement> ANIMATE_FRAME = ANIMATED.idSignal("animate_frame");

    private static ParentData<?> extractParentData(AnimationElement element) {
        Object owner = element.owner;
        if (owner instanceof Tf2Player) {
            return new ParentData<>(TF2_PLAYER, (Tf2Player) owner);
        } else if (owner instanceof Tf2Weapon) {
            return new ParentData<>(PLAYER_WEAPON, (Tf2Weapon) owner);
        } else {
            throw new IllegalStateException("Unknown animation owner " + owner);
        }
    }

    private static AnimationElement extractChildData(Object object) {
        if (object instanceof Tf2Player) {
            return ((Tf2Player) object).animation;
        } else if (object instanceof Tf2Weapon) {
            return ((Tf2Weapon) object).animation;
        } else {
            throw new IllegalStateException("Unknown animation owner " + object);
        }
    }

    public static class AnimationElement {
        public final Object owner;
        public int frames = 0;

        public AnimationElement(Object owner) {
            this.owner = owner;
        }

        @Override
        public String toString() {
            return "Animation " + frames + " in " + owner;
        }
    }

    public static class Tf2Weapon {
        public final Tf2Player holder;
        public final String name;
        public final int clipSize;
        public final AnimationElement animation = new AnimationElement(this);
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

    public static abstract class Tf2Player {
        public final List<Tf2Weapon> weapons = new ArrayList<>();
        public final AnimationElement animation = new AnimationElement(this);

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
            public void sendPacket(ByteBuf data, int packetId, NetIdBase netId, int priority) {
                incomingSide2.add(data.copy());
            }
        };
        side2 = new TestConnection(ROOT, 2) {
            @Override
            public void sendPacket(ByteBuf data, int packetId, NetIdBase netId, int priority) {
                incomingSide1.add(data.copy());
            }
        };

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
            System.out.println("Animated a frame of " + elem);
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

        SPAM_E_KEY.send(side1, players[0]);
        SET_HEAL_TARGET.send(side1, (Tf2Medic) players[0], (medic, buffer, ctx) -> {
            MsgUtil.writeUTF(buffer, "Scout");
        });
        ANIMATE_FRAME.send(side1, players[0].animation);
        ANIMATE_FRAME.send(side1, players[0].weapons.get(0).animation);
        ANIMATE_FRAME.send(side1, players[0].weapons.get(0).animation);
        ANIMATE_FRAME.send(side1, players[0].weapons.get(1).animation);
        ANIMATE_FRAME.send(side1, players[0].animation);
        ANIMATE_FRAME.send(side1, players[0].weapons.get(0).animation);
        ANIMATE_FRAME.send(side1, players[0].weapons.get(0).animation);

        process();
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
            ByteBuf buf;
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
}