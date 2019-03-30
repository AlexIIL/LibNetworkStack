package alexiil.mc.lib.net;

import java.util.function.Function;

public class ParentNetIdKeyUp<T, S> extends ParentNetIdKey<T> {

    public final ParentNetIdTyped<S> parent;
    public final Function<T, S> parentGetter;

    public ParentNetIdKeyUp(ParentNetIdTyped<S> parent, String name, NetKeyMapper<T> mapper,
        Function<T, S> parentGetter) {
        super(parent, name, mapper);
        this.parent = parent;
        this.parentGetter = parentGetter;
    }
}
