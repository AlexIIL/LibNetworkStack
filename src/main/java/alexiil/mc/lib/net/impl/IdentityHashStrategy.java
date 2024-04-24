package alexiil.mc.lib.net.impl;

import it.unimi.dsi.fastutil.Hash;

public class IdentityHashStrategy implements Hash.Strategy<Object> {
    private static final IdentityHashStrategy INSTANCE = new IdentityHashStrategy();
    
    @SuppressWarnings("unchecked")
    public static <T> Hash.Strategy<T> getInstance() {
        return (Hash.Strategy<T>) INSTANCE;
    }
    
    private IdentityHashStrategy() {}
    
    @Override
    public int hashCode(Object o) {
        return System.identityHashCode(o);
    }

    @Override
    public boolean equals(Object a, Object b) {
        return a == b;
    }
}
