package alexiil.mc.lib.net;

import javax.annotation.Nullable;

public abstract class ParentNetIdBase extends TreeNetIdBase {

    ParentNetIdBase(ParentNetIdBase parent, String name, int thisLength) {
        super(parent, name, thisLength);
    }

    @Override
    int getFlags() {
        return NetIdBase.FLAG_IS_PARENT;
    }

    @Nullable
    abstract TreeNetIdBase getChild(String childName);
}
