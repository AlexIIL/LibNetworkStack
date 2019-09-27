/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
/**
 * <h1>LibNetworkStack's base library.</h1>
 * <p>
 * First off: this isn't completely designed around minecraft, so if you are only interested in using this in a normal
 * minecraft networking you probably want to read the javadoc for the {@link alexiil.mc.lib.net.impl} package as well.
 * <p>
 * There are a few main concepts:
 * <ol>
 * <li>Network ID organisation</li>
 * <li>Connections</li>
 * <li>Contextual parent network ID's</li>
 * <li>Dynamic parent network ID's</li>
 * <li>Data network ID's</li>
 * <li>Signal network ID's</li>
 * <li>Caches</li>
 * <li>Debugging</li>
 * </ol>
 * <h2>Network ID organisation</h2>
 * <p>
 * Network ID's are organised into a tree: the root node is always a {@link alexiil.mc.lib.net.ParentNetId ParentNetId},
 * and the child nodes can either extend {@link alexiil.mc.lib.net.ParentNetIdBase ParentNetIdBase} (for organising
 * nodes), or {@link alexiil.mc.lib.net.NetIdBase NetIdBase} (for sending data).<br>
 * Every ID has a name, usually in the form "mod_id:mod_specific_name", however it is only important that the name not
 * collide with other children of the same parent. These names are used to assign integer ID's to each network ID so
 * that the full name doesn't have to be sent with each and every packet, so they must be the same at both ends!
 * <p>
 * <h2>Connections</h2>
 * <p>
 * Each individual connection has it's own {@link alexiil.mc.lib.net.ActiveConnection ActiveConnection} object
 * associated with it: this stores the current network ID registry, any caches, and handles reading and writing packets
 * out to the underlying connection.
 * <p>
 * There is a second base type: a {@link alexiil.mc.lib.net.BufferedConnection BufferedConnection}, which will buffer
 * (or queue) packets until it's next "tick". This is generally a large performance optimisation if you need to send a
 * lot of smaller packets.
 * <p>
 * <h2>Contextual parent network ID's</h2>
 * <p>
 * In addition to organising all network nodes, any parent type that extends {@link alexiil.mc.lib.net.ParentNetIdSingle
 * ParentNetIdSingle} can also write out data for it's represented object. For example you might have a network ID for a
 * BlockEntity, and then several children of that block entity that all need to communicate with the same entity on the
 * other side.
 * <p>
 * <h2>Dynamic parent network ID's</h2>
 * <p>
 * It is occasionally useful for an object to be contained within multiple different types of object, for example a
 * buildcraft combustion engine might be contained within a block entity, or used in a flying robot. Dynamic network
 * ID's allows the same object to communicate even if it doesn't necessarily have a single path upwards to the root.
 * <p>
 * Unlike normal ID's the dynamic parent must know the child that it will be sending, rather than the child needing to
 * know the parent.
 * <p>
 * (List title):
 * <table border="1px solid">
 * <tr>
 * <td>Class</td>
 * <td>Overview</td>
 * </tr>
 * <tbody>
 * <tr>
 * <td>{@link alexiil.mc.lib.net.ParentDynamicNetId ParentDynamicNetId}</td>
 * <td>The parent for a specific {@link alexiil.mc.lib.net.DynamicNetId}</td>
 * </tr>
 * <tr>
 * <td>{@link alexiil.mc.lib.net.DynamicNetId DynamidNetId}</td>
 * <td>The child which doesn't know what is it's parent, but the object itself must have some way to obtain it's current
 * parent is via {@link alexiil.mc.lib.net.DynamicNetId.LinkAccessor#getLink(Object) LinkAccessor#getLink(Object)}</td>
 * </tr>
 * <tr>
 * <td>{@link alexiil.mc.lib.net.DynamicNetLink DynamicNetLink}</td>
 * <td>The per-instance link object that holds both the parent and the child network ID and objects.</td>
 * </tr>
 * </tbody>
 * </table>
 * <p>
 * <h2>Data network ID's</h2>
 * <p>
 * These allow you to send and receive between 1 and 65,536 bytes of binary data. If you know the exact length of the
 * data to be sent you can pre-specify it in the constructor, or you can allow using a shorter (or longer) maximum
 * packet length by calling either {@link alexiil.mc.lib.net.NetIdBase#setTinySize() setTinySize()} or
 * {@link alexiil.mc.lib.net.NetIdBase#setLargeSize() setLargeSize()}.
 * <p>
 * Data ID's come in two variants: either untyped (which are somewhat comparable to the "static" java modifier for
 * methods), or typed, which require (and will deserialise) an object for them to be called upon.
 * <p>
 * <table border="1px solid">
 * <tr>
 * <td></td>
 * <td>Parent</td>
 * <td>Class</td>
 * </tr>
 * <tr>
 * <td>Untyped</td>
 * <td>{@link alexiil.mc.lib.net.ParentNetId ParentNetId}</td>
 * <td>{@link alexiil.mc.lib.net.NetIdData NetIdData}</td>
 * </tr>
 * <tr>
 * <td>Typed</td>
 * <td>{@link alexiil.mc.lib.net.ParentNetIdSingle ParentNetIdSingle}</td>
 * <td>{@link alexiil.mc.lib.net.NetIdDataK NetIdDataK}</td>
 * </tr>
 * </table>
 * <p>
 * <h2>Signal network ID's</h2>
 * <p>
 * These are very similar to data network ID's, except they won't write any data out: instead the mere presence of this
 * packet should convey all of the information required. Two variants exist: typed and untyped depending on whether the
 * parent has a contextual object or not.
 * <table border="1px solid">
 * <tr>
 * <td></td>
 * <td>Parent</td>
 * <td>Class</td>
 * </tr>
 * <tr>
 * <td>Untyped</td>
 * <td>{@link alexiil.mc.lib.net.ParentNetId ParentNetId}</td>
 * <td>{@link alexiil.mc.lib.net.NetIdSignal NetIdSignal}</td>
 * </tr>
 * <tr>
 * <td>Typed</td>
 * <td>{@link alexiil.mc.lib.net.ParentNetIdSingle ParentNetIdSingle}</td>
 * <td>{@link alexiil.mc.lib.net.NetIdSignalK NetIdSignalK}</td>
 * </tr>
 * </table>
 * <p>
 * <h2>Caches</h2>
 * <p>
 * These are a slightly more niche feature, but still useful nonetheless. Essentially these offer a way to create a
 * registry of objects to/from integer ID's on a per-connection, per-use basis. This can help avoid a situation where
 * you need to reliably sync a (possibly large) registry of id's right at the start of a network connection, especially
 * if not all of those ID's will be used over it's duration.
 * <p>
 * The only class associated with caches is {@link alexiil.mc.lib.net.NetObjectCache NetObjectCache}, and it's usage is
 * simple: Just create a new {@link alexiil.mc.lib.net.ParentNetId ParentNetId}, then pass in a hash/equals and network
 * serialiser implementation. (Alternatively if your data can be mapped to/from an {@link net.minecraft.util.Identifier}
 * then you can use the function based factory:
 * {@link alexiil.mc.lib.net.NetObjectCache#createMappedIdentifier(ParentNetId, java.util.function.Function, java.util.function.Function)
 * createMappedIdentifier}).
 * <p>
 * To send an ID you can then call {@link alexiil.mc.lib.net.NetObjectCache#getId(ActiveConnection, Object) getId} to
 * get the ID that will be sent. (Note that the ID's *are* buffered by default, so you must only use them in a buffered
 * connection, or call {@link alexiil.mc.lib.net.NetObjectCache #notBuffered()}).
 * <p>
 * On the receiving end you can call {@link alexiil.mc.lib.net.NetObjectCache#getObj(ActiveConnection, int)} to obtain
 * the original object (although it will return null if the entry isn't present - either if it hasn't sent yet or you
 * passed it the wrong ID.
 * <p>
 * <h2>Debugging</h2>
 * <p>
 * To enable debugging (also known as log spam) you can add the option "-Dlibnetworkstack.debug=true" to the launch
 * arguments. (You can also debug caches separately with "-Dlibnetworkstack.cache.debug=true"). */
package alexiil.mc.lib.net;
