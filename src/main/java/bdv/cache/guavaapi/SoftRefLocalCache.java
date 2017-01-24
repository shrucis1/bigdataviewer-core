/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2016 Tobias Pietzsch, Stephan Saalfeld, Stephan Preibisch,
 * Jean-Yves Tinevez, HongKee Moon, Johannes Schindelin, Curtis Rueden, John Bogovic
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package bdv.cache.guavaapi;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.concurrent.ConcurrentHashMap;

import bdv.cache.WeakSoftCacheFinalizeQueue;

/**
 * {@link SoftReference}s in a {@link ConcurrentHashMap}.
 *
 * @param <K>
 *            key type
 * @param <V>
 *            value type
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
class SoftRefLocalCache< K, V >
{
	private final ConcurrentHashMap< K, Reference< V > > map = new ConcurrentHashMap<>();

	private final WeakSoftCacheFinalizeQueue queue;

	public SoftRefLocalCache( final WeakSoftCacheFinalizeQueue finalizeQueue )
	{
		this.queue = finalizeQueue;
	}

	public void put( final K key, final V value )
	{
		map.put( key, queue.createSoftReference( key, value, map ) );
	}

	public V get( final Object key )
	{
		final Reference< V > ref = map.get( key );
		return ref == null ? null : ref.get();
	}

	public void invalidateAll()
	{
		for ( final Reference< ? > ref : map.values() )
			ref.clear();
		map.clear();
	}

	/**
	 * Remove references from the cache that have been garbage-collected. To
	 * avoid long run-times, per call to {@code cleanUp()}, at most
	 * {@link #MAX_PER_FRAME_FINALIZE_ENTRIES} are processed.
	 */
	public void cleanUp()
	{
		queue.cleanUp();
	}

	/**
	 * Returns the approximate number of entries in this cache.
	 */
	public long size()
	{
		return map.size();
	}
}
