package alexiil.mc.lib.net;

public abstract class MessageContext implements IMsgCtx {

    public final NetworkSide side;
    public final NetIdBase id;
    public final Object[] values;

    public MessageContext(NetworkSide side, NetIdBase id) {
        this.side = side;
        this.id = id;
        this.values = new Object[id.parent.nextKeyValueIndex];
    }

    @Override
    public NetworkSide getNetSide() {
        return side;
    }

    @Override
    public NetIdBase getNetId() {
        return id;
    }

    @Override
    public <T> void putKey(ParentNetIdTyped<T> key, T value) {
        values[key.keyValueIndex] = value;
    }

    @Override
    public <T> T getKey(ParentNetIdTyped<T> key) {
        return key.clazz.cast(values[key.keyValueIndex]);
    }

    public static class Read extends MessageContext implements IMsgReadCtx {

        public Read(NetworkSide side, NetIdBase id) {
            super(side, id);
        }
    }

    public static class Write extends MessageContext implements IMsgWriteCtx {
        public Write(NetworkSide side, NetIdBase id) {
            super(side, id);
        }
    }
}
