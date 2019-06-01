package alexiil.mc.lib.net;

public abstract class NetIdSeparate extends NetIdBase {
    NetIdSeparate(ParentNetId parent, String name, int length) {
        super(parent, name, length);
        parent.addChild(this);
    }
}
