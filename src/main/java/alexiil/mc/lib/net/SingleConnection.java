package alexiil.mc.lib.net;

import io.netty.buffer.ByteBuf;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

public abstract class SingleConnection {

    final IntSet clientKnownIds = new IntOpenHashSet();

    public abstract void send(ByteBuf packet);
}
