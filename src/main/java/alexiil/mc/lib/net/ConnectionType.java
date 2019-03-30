package alexiil.mc.lib.net;

import java.util.List;

public class ConnectionType {

    List<NetIdBase> allAllocatedIds;
    int nextFreeId = InternalMsgUtil.COUNT_HARDCODED_IDS;

    public ConnectionType() {

        for (int i = 0; i < InternalMsgUtil.COUNT_HARDCODED_IDS; i++) {
            allAllocatedIds.add(null);
        }
    }
}
