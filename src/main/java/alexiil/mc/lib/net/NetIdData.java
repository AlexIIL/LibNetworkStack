package alexiil.mc.lib.net;

import io.netty.buffer.ByteBuf;

import buildcraft.api.core.InvalidInputDataException;

public class NetIdData extends NetIdBase {
    public NetIdData(ParentNetId parent, String name, int length) {
        super(parent, name, length);
    }

    @Override
    public void receive(ByteBuf buffer, IMsgReadCtx ctx) throws InvalidInputDataException {
        // TODO Auto-generated method stub
        throw new AbstractMethodError("// TODO: Implement this!");
    }
}
