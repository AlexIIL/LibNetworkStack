package alexiil.mc.lib.net;

public class ParentNetIdKeyCast<Sub extends Super, Super> extends ParentNetIdTyped<Sub> {
    public final ParentNetIdKey<Super> parent;

    public ParentNetIdKeyCast(ParentNetIdKey<Super> parent, String name, Class<Sub> subClass) {
        super(parent, name, subClass, 0);
        this.parent = parent;
    }
}
