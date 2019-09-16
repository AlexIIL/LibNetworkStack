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
 * </ol>
 * <h2>Network ID organisation</h2>
 * <p>
 * Network ID's are organised into a tree: the root node is always a {@link alexiil.mc.lib.net.ParentNetId ParentNetId},
 * and the child nodes can be any of it's subclasses.<br>
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
 * ParentNetidSingle} can also write out data for it's represented object. For example you might have a network ID for a
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
 * (words...)
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
 * also simple. */
package alexiil.mc.lib.net;
