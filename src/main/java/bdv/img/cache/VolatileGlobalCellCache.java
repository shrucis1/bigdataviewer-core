/*
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
package bdv.img.cache;

import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import bdv.cache.CacheControl;
import bdv.cache.CacheHints;
import bdv.cache.VolatileCacheValueLoader;
import bdv.cache.WeakSoftCache;
import bdv.cache.guavaapi.RefCacheFactory;
import bdv.cache.guavaapi.VolatileLoader;
import bdv.cache.guavaapi.WeakRefVolatileCache;
import bdv.cache.util.BlockingFetchQueues;
import bdv.cache.util.FetcherThreads;
import bdv.img.cache.VolatileImgCells.CellCache;
import net.imglib2.img.basictypeaccess.volatiles.VolatileAccess;
import net.imglib2.img.cell.CellImg;

public class VolatileGlobalCellCache implements CacheControl
{
	public static class ImgKey
	{
		private final int timepoint;

		private final int setup;

		private final int level;

		private final CacheHints cacheHints;

		/**
		 * Create a Key for the specified image.
		 *
		 * @param timepoint
		 *            timepoint coordinate of the image.
		 * @param setup
		 *            setup coordinate of the image.
		 * @param level
		 *            level coordinate of the image.
		 * @param cacheHints
		 *            cache hints for the image.
		 */
		public ImgKey( final int timepoint, final int setup, final int level, final CacheHints cacheHints )
		{
			this.timepoint = timepoint;
			this.setup = setup;
			this.level = level;
			this.cacheHints = cacheHints;

			int value = Long.hashCode( level );
			value = 31 * value + setup;
			value = 31 * value + timepoint;
			value = 31 * value + cacheHints.hashCode();
			hashcode = value;
		}

		@Override
		public boolean equals( final Object other )
		{
			if ( this == other )
				return true;
			if ( !( other instanceof ImgKey ) )
				return false;
			final ImgKey that = ( ImgKey ) other;
			return ( this.timepoint == that.timepoint ) && ( this.setup == that.setup ) && ( this.level == that.level );
		}

		private final int hashcode;

		@Override
		public int hashCode()
		{
			return hashcode;
		}
	}

	private final RefCacheFactory cacheFactory = new RefCacheFactory();

	private final BlockingFetchQueues< Callable< Void > > queue;

	private final FetcherThreads fetchers;

	private final HashMap< ImgKey, WeakRefVolatileCache< Long, ? extends VolatileCell< ? > > > imgCaches;

	/**
	 * @param maxNumLevels
	 *            the highest occurring mipmap level plus 1.
	 * @param numFetcherThreads
	 */
	public VolatileGlobalCellCache( final int maxNumLevels, final int numFetcherThreads )
	{
		queue = new BlockingFetchQueues<>( maxNumLevels );
		fetchers = new FetcherThreads( queue, numFetcherThreads );
		imgCaches = new HashMap<>();
	}

	/**
	 * pause all fetcher threads for the specified number of milliseconds.
	 */
	// TODO remove on next opportunity (when API is broken anyways...)
	public void pauseFetcherThreadsFor( final long ms )
	{
		fetchers.pauseFetcherThreadsFor( ms );
	}

	/**
	 * Prepare the cache for providing data for the "next frame":
	 * <ul>
	 * <li>Move pending cell request to the prefetch queue (
	 * {@link BlockingFetchQueues#clearToPrefetch()}).
	 * <li>Perform pending cache maintenance operations (
	 * {@link WeakSoftCache#cleanUp()}).
	 * <li>Increment the internal frame counter, which will enable previously
	 * enqueued requests to be enqueued again for the new frame.
	 * </ul>
	 */
	@Override
	public void prepareNextFrame()
	{
		queue.clearToPrefetch();
		cacheFactory.cleanUp();
	}

	/**
	 * Remove all references to loaded data as well as all enqueued requests
	 * from the cache.
	 */
	public void clearCache()
	{
		synchronized ( imgCaches )
		{
			for ( final WeakRefVolatileCache< Long, ? > cache : imgCaches.values() )
				cache.invalidateAll();
			imgCaches.clear();
			queue.clear();
		}
	}

	/**
	 * <em>For internal use.</em>
	 * <p>
	 * Get the {@link WeakRefVolatileCache} that handles cell loading. This is
	 * used by bigdataviewer-server to directly issue Cell requests without
	 * having {@link CellImg}s and associated {@link VolatileCellCache}s.
	 *
	 * @return the cache that handles cell loading
	 */
//	TODO
//	public LoadingVolatileCache< Key, VolatileCell< ? > > getLoadingVolatileCache()
//	{
//		return volatileCache;
//	}

	< A extends VolatileAccess > WeakRefVolatileCache< Long, VolatileCell< A > > getImageCache( final ImgKey key )
	{
		synchronized ( imgCaches )
		{
			@SuppressWarnings( "unchecked" )
			WeakRefVolatileCache< Long, VolatileCell< A > > cache = ( WeakRefVolatileCache< Long, VolatileCell< A > > ) imgCaches.get( key );
			if ( cache == null )
			{
				cache = cacheFactory.newWeakRefVolatileCache( queue, key.cacheHints );
				imgCaches.put( key, cache );
			}
			return cache;
		}
	}

	/**
	 * A {@link CellCache} that forwards to the {@link VolatileGlobalCellCache}.
	 *
	 * @param <A>
	 *
	 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
	 */
	public class VolatileCellCache< A extends VolatileAccess > implements CellCache< A >
	{
		private final int timepoint;

		private final int setup;

		private final int level;

		private CacheHints cacheHints;

		private final WeakRefVolatileCache< Long, VolatileCell< A > > volatileCache;

		private final CacheArrayLoader< A > cacheArrayLoader;

		public VolatileCellCache(
				final int timepoint,
				final int setup,
				final int level,
				final CacheHints cacheHints,
				final CacheArrayLoader< A > cacheArrayLoader )
		{
			this.timepoint = timepoint;
			this.setup = setup;
			this.level = level;
			this.cacheArrayLoader = cacheArrayLoader;
			volatileCache = getImageCache( new ImgKey( timepoint, setup, level, cacheHints ) );
		}

		@Override
		public VolatileCell< A > get( final long index )
		{
			return volatileCache.getIfPresent( index );
		}

		@Override
		public VolatileCell< A > load( final long index, final int[] cellDims, final long[] cellMin )
		{
			final VolatileCellLoader loader = new VolatileCellLoader( cellDims, cellMin );
			try
			{
				return volatileCache.get( index, loader );
			}
			catch ( final ExecutionException e )
			{
				e.printStackTrace();
				return null;
			}
		}

		// TODO FIXME This doesn't have any effect! because we added it to ImgKey etc. It doesn't work like this!!!
		@Override
		public void setCacheHints( final CacheHints cacheHints )
		{
			this.cacheHints = cacheHints;
		}

		/**
		 * A {@link VolatileCacheValueLoader} for one specific {@link VolatileCell}.
		 */
		private class VolatileCellLoader implements VolatileLoader< VolatileCell< A > >
		{
			private final int[] cellDims;

			private final long[] cellMin;

			/**
			 * Create a loader for a specific cell.
			 *
			 * @param cellDims
			 *            dimensions of the cell in pixels
			 * @param cellMin
			 *            minimum spatial coordinates of the cell in pixels
			 */
			public VolatileCellLoader(
					final int[] cellDims,
					final long[] cellMin )
			{
				this.cellDims = cellDims;
				this.cellMin = cellMin;
			}

			@Override
			public VolatileCell< A > createEmptyValue()
			{
				return new VolatileCell<>( cellDims, cellMin, cacheArrayLoader.emptyArray( cellDims ) );
			}

			@Override
			public VolatileCell< A > call() throws Exception
			{
				return new VolatileCell<>( cellDims, cellMin, cacheArrayLoader.loadArray( timepoint, setup, level, cellDims, cellMin ) );
			}
		}
	}
}
