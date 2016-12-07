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
package bdv.cache;

import java.lang.ref.Reference;

import net.imglib2.Dirty;

/**
 * Implementation of {@link WeakSoftCache} for {@link Dirty} values.
 *
 * @param <K>
 *            key type
 * @param <V>
 *            value type
 *
 * @author Stephan Saalfeld
 */
public class DirtyWeakSoftCacheImp< K, V extends Dirty > extends WeakSoftCacheImp< K, V > implements DirtyWeakSoftCache< K, V >
{
	DirtyWeakSoftCacheImp()
	{}

	@Override
	public void clearAll()
	{
		for ( final Reference< ? > ref : softReferenceCache.values() )
			ref.enqueue();
	}

	/**
	 * Remove references from the cache that have been garbage-collected.
	 * To avoid long run-times, per call to {@code cleanUp()}, at most
	 * {@link #MAX_PER_FRAME_FINALIZE_ENTRIES} are processed.
	 */
	@Override
	public void cleanUp()
	{
		synchronized ( softReferenceCache )
		{
			for ( int i = 0; i < MAX_PER_FRAME_FINALIZE_ENTRIES; ++i )
			{
				final Reference< ? extends V > poll = finalizeQueue.poll();
				if ( poll == null )
					break;
				final Object key = ( ( GetKey< ? > ) poll ).getKey();
				final Reference< V > ref = softReferenceCache.get( key );
				if ( ref == poll )
				{
					/* TODO persist */
					System.out.println( key.toString() + ( ref.get().isDirty() ? " is dirty!" : " is not dirty." ) );
					softReferenceCache.remove( key );
				}
			}
		}
	}
}
