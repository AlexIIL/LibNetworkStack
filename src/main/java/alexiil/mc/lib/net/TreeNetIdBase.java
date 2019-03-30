package alexiil.mc.lib.net;

public abstract class TreeNetIdBase {

    public static final int LENGTH_DYNAMIC = -1;

    public final ConnectionType connectionType;

    /** The name - must be unique to the parent that uses it, so for top level mod packets this MUST include the
     * modid. */
    public final String name;

    /** Used for logging and display purposes only. */
    public final String fullName;

    /** Full length including all parents. */
    public final int totalLength;

    public TreeNetIdBase(ConnectionType connectionType, String name, int thisLength) {
        this.connectionType = connectionType;
        this.name = name;
        this.fullName = name;
        this.totalLength = thisLength;
    }

    public TreeNetIdBase(ParentNetId parent, String name, int thisLength) {
        this.connectionType = parent.connectionType;
        this.name = name;
        this.fullName = parent.fullName + "." + name;
        if (thisLength == LENGTH_DYNAMIC) {
            this.totalLength = LENGTH_DYNAMIC;
        } else {
            if (parent.totalLength == LENGTH_DYNAMIC) {
                this.totalLength = LENGTH_DYNAMIC;
            } else {
                this.totalLength = thisLength + parent.totalLength;
            }
        }
    }
}
