package alexiil.mc.lib.net;

public abstract class ParentNetIdTyped<T> extends ParentNetId {

    public final Class<T> clazz;

    public ParentNetIdTyped(ParentNetId parent, String name, Class<T> clazz, int length) {
        super(parent, name, length);
        this.clazz = clazz;
    }

    public NetIdDataK<T> idTypedData(String name) {
        return new NetIdDataK<>(this, name);
    }

    public NetIdDataK<T> idTypedData(ParentNetId actualParent, String name) {
        return new NetIdDataK<>(actualParent, name, this);
    }

    public NetIdSignalK<T> idTypedSignal(String name) {
        return new NetIdSignalK<>(this, name);
    }

    public NetIdSignalK<T> idTypedSignal(ParentNetId actualParent, String name) {
        return new NetIdSignalK<>(actualParent, name, this);
    }
}
