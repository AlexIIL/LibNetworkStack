package alexiil.mc.lib.net;

/** The container object
 * 
 * @param <P> The parent type.
 * @param <C> The child type. */
public final class DynamicNetLink<P, C> {

    @FunctionalInterface
    public interface IDynamicLinkFactory<C> {
        DynamicNetLink<?, C> create(C child);
    }

    public final ParentDynamicNetId<P, C> parentId;
    public final P parent;
    public final C child;

    public DynamicNetLink(ParentDynamicNetId<P, C> parentId, P parent, C child) {
        this.parentId = parentId;
        this.parent = parent;
        this.child = child;
    }
}
