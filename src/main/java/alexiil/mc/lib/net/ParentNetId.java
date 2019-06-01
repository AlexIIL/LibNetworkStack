package alexiil.mc.lib.net;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

public final class ParentNetId extends ParentNetIdBase {

    private final Map<String, NetIdSeparate> leafChildren = new HashMap<>();
    private final Map<String, ParentNetIdBase> branchChildren = new HashMap<>();

    public ParentNetId(@Nullable ParentNetId parent, String name) {
        super(parent, name, 0);
        if (parent != null) {
            parent.addChild(this);
        }
    }

    void addChild(NetIdSeparate netId) {
        if (leafChildren.containsKey(netId.name) || branchChildren.containsKey(netId.name)) {
            throw new IllegalStateException("There is already a child with the name " + netId.name + "!");
        }
        leafChildren.put(netId.name, netId);
    }

    void addChild(ParentNetIdBase netId) {
        if (leafChildren.containsKey(netId.name) || branchChildren.containsKey(netId.name)) {
            throw new IllegalStateException("There is already a child with the name " + netId.name + "!");
        }
        branchChildren.put(netId.name, netId);
    }

    @Override
    TreeNetIdBase getChild(String childName) {
        NetIdSeparate leaf = leafChildren.get(childName);
        return leaf != null ? leaf : branchChildren.get(childName);
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
