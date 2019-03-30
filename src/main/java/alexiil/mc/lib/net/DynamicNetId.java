package alexiil.mc.lib.net;

import java.util.function.Function;

public class DynamicNetId<T> extends ParentNetIdTyped<T> {

    final Function<T, ParentNetId> parentExtractor;

    public DynamicNetId(String name, Class<T> clazz, Function<T, ParentNetId> parentExtractor) {
        super(null, name, clazz, LENGTH_DYNAMIC);
        this.parentExtractor = parentExtractor;
    }
}
