# LibNetworkStack

This is a library mod for the [Fabric](https://fabricmc.net/) API, based around [Minecraft](https://minecraft.net).

## Maven

Currently you can use this by adding this to your build.gradle:

```
repositories {
    maven {
        name = "BuildCraft"
        url = "https://mod-buildcraft.com/maven"
    }
}

dependencies {
    modCompile "alexiil.mc.lib:libnetworkstack-base:0.2.0"
}
```

## Getting Started

You can either look at the [wiki](https://github.com/AlexIIL/LibNetworkStack/wiki) for a brief overview, or look at [SimplePipes](https://github.com/AlexIIL/SimplePipes) source code for a use-case mod that uses this. To get help you can either open an issue here, or ping AlexIIL on the fabric or CottonMC discord servers.

## A simple example

For example if you wanted to send data from your custom BlockEntity called "ElectricFurnaceBlockEntity", you would do this:

```java

import alexiil.mc.lib.net.ActiveConnection;
import alexiil.mc.lib.net.IMsgReadCtx;
import alexiil.mc.lib.net.IMsgWriteCtx;
import alexiil.mc.lib.net.InvalidInputDataException;
import alexiil.mc.lib.net.NetByteBuf;
import alexiil.mc.lib.net.NetIdDataK;
import alexiil.mc.lib.net.NetIdDataK.IMsgDataWriterK;
import alexiil.mc.lib.net.NetIdSignalK;
import alexiil.mc.lib.net.ParentNetIdSingle;
import alexiil.mc.lib.net.impl.CoreMinecraftNetUtil;
import alexiil.mc.lib.net.impl.McNetworkStack;

public class ElectricFurnaceBlockEntity extends BlockEntity {
    // All base code left out
    public static final ParentNetIdSingle<ElectricFurnaceBlockEntity> NET_PARENT;
    public static final NetIdDataK<ElectricFurnaceBlockEntity> ID_CHANGE_BRIGHTNESS;
    public static final NetIdSignalK<ElectricFurnaceBlockEntity> ID_TURN_ON, ID_TURN_OFF;

    static {
        NET_PARENT = McNetworkStack.BLOCK_ENTITY.subType(
            // Assuming the modid is "electrical_nightmare"
            ElectricFurnaceBlockEntity.class, "electrical_nightmare:electric_furnace"
        );
        // We don't need to re-specify the modid for this because the parent already did
        // and we don't expect any other mods to add new id's.
        ID_CHANGE_BRIGHTNESS = NET_PARENT.idData("CHANGE_BRIGHTNESS").setReceiver(
            ElectricFurnaceBlockEntity::receiveBrightnessChange
        );
        ID_TURN_ON = NET_PARENT.idSignal("TURN_ON").setReceiver(ElectricFurnaceBlockEntity::receiveTurnOn);
        ID_TURN_OFF = NET_PARENT.idSignal("TURN_OFF").setReceiver(ElectricFurnaceBlockEntity::receiveTurnOff);
    }

    /** Used for rendering - somehow. */
    public int clientBrightness;
    public boolean clientIsOn;

    public ElectricFurnaceBlockEntity() {
        super(null);
    }

    /** Sends the new brightness, to be rendered on the front of the block.
     *  This should be called on the server side.
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

    /** Sends the new on/off state, to be rendered on the front of the block.
     *  This should be called on the server side. */
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

```
