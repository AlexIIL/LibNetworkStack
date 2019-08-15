/**
 * <h1>LibNetworkStack's minecraft-specific library.</h1>
 * <p>
 * First off: this is based fully on the concepts documented in the {@link alexiil.mc.lib.net main library}, but with a
 * few things different:
 * <ol>
 * <li>Every {@link alexiil.mc.lib.net.TreeNetIdBase packet} must be have
 * {@link alexiil.mc.lib.net.impl.McNetworkStack#ROOT} as it's root parent network ID - otherwise it will crash when you
 * try to send it</li>
 * <li>All network connections are {@link alexiil.mc.lib.net.BufferedConnection buffered} until the end of every tick -
 * so you <em>cannot</em> expect them to arrive before normal minecraft packets, unless you explicitly call
 * {@link alexiil.mc.lib.net.NetIdBase#notBuffered()} on the network ID.</li>
 * </ol>
 */
package alexiil.mc.lib.net.impl;
