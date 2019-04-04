package alexiil.mc.lib.net;

import javax.annotation.Nullable;

public final class ParentNetId extends ParentNetIdBase {

    public ParentNetId(@Nullable ParentNetIdBase parent, String name) {
        super(parent, name, 0);
    }

    public NetIdData idData(String name) {
        return new NetIdData(this, name, TreeNetIdBase.DYNAMIC_LENGTH);
    }

    public NetIdData idData(String name, int dataLength) {
        return new NetIdData(this, name, dataLength);
    }

    public NetIdSignal idSignal(String name) {
        return new NetIdSignal(this, name);
    }

    public ParentNetId child(String childName) {
        return new ParentNetId(this, childName);
    }
}
