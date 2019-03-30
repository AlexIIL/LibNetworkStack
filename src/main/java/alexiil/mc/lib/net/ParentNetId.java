package alexiil.mc.lib.net;

import javax.annotation.Nullable;

public class ParentNetId extends TreeNetIdBase {

    @Nullable
    public final ParentNetId parent;

    final int keyValueIndex;
    final int nextKeyValueIndex;

    public ParentNetId(@Nullable ParentNetId parent, String name, int length) {
        super(parent, name, length);
        this.parent = parent;
        this.keyValueIndex = parent == null ? 0 : parent.nextKeyValueIndex;
        if (this instanceof ParentNetIdKey<?>) {
            this.nextKeyValueIndex = keyValueIndex + 1;
        } else {
            this.nextKeyValueIndex = keyValueIndex;
        }
    }

    public NetIdData idData(String name) {
        return new NetIdData(this, name);
    }

    public NetIdSignal idSignal(String name) {
        return new NetIdSignal(this, name);
    }

    public boolean isParent(ParentNetId other) {
        if (other == this) {
            return true;
        }
        if (parent == null) {
            return false;
        }
        return parent.isParent(other);
    }
}
