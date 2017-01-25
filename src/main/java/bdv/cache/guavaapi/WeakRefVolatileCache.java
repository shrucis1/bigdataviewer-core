package bdv.cache.guavaapi;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheStats;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.UncheckedExecutionException;

import bdv.cache.CacheHints;
import bdv.cache.LoadingStrategy;
import bdv.cache.VolatileCacheValue;
import bdv.cache.iotiming.CacheIoTiming;
import bdv.cache.iotiming.IoStatistics;
import bdv.cache.iotiming.IoTimeBudget;
import bdv.cache.util.BlockingFetchQueues;
import bdv.img.cache.loading.VolatileCacheLoader;

public class WeakRefVolatileCache< K, V extends VolatileCacheValue > implements LoadingCache< K, V >
{
	WeakRefLocalCache< K, Entry > entryCache;

	final class Entry
	{
		private final K key;

		private V value;

		private long enqueueFrame;

		public Entry( final K key )
		{
			this.key = key;
			this.value = cacheLoader.createEmpty( key );
			enqueueFrame = -1;
		}

		public void load() throws ExecutionException
		{
			synchronized ( this )
			{
				if ( ! value.isValid() )
				{
					value = softRefCache.get( key, () -> cacheLoader.load( key ) );
					enqueueFrame = Long.MAX_VALUE;
				}
				notifyAll();
			}
		}

		public K getKey()
		{
			return key;
		}

		public V getValue()
		{
			return value;
		}

		public long getEnqueueFrame()
		{
			return enqueueFrame;
		}

		public void setEnqueueFrame( final long f )
		{
			enqueueFrame = f;
		}
	}

	private final Object cacheLock = new Object();

	private final Cache< K, V > softRefCache;

	private final BlockingFetchQueues< Callable< Void > > queue;

	private final VolatileCacheLoader< K, V > cacheLoader;

	final LoadingStrategy loadingStrategy;

	final int priority;

	final boolean enqueuToFront;

	public WeakRefVolatileCache(
			final WeakRefLocalCache< K, Entry > entryCache,
			final BlockingFetchQueues< Callable< Void > > fetchQueue,
			final Cache< K, V > validCache,
			final CacheHints cacheHints,
			final VolatileCacheLoader< K, V > cacheLoader )
	{
		this.entryCache = entryCache;
		this.queue = fetchQueue;
		this.softRefCache = validCache;
		this.cacheLoader = cacheLoader;
		loadingStrategy = cacheHints.getLoadingStrategy();
		priority = cacheHints.getQueuePriority();
		enqueuToFront = cacheHints.isEnqueuToFront();
	}

	@Override
	public V getIfPresent( final Object key )
	{
		final V v = softRefCache.getIfPresent( key );
		if ( v != null )
			return v;

		final Entry entry = entryCache.get( key );
		if ( entry != null )
		{
			// cannot test entry.loader == null, because loader = null might be reordered.
			// either requires VolatileCacheValue or additional synchronization here.
			if ( !entry.getValue().isValid() )
				try
				{
					loadEntryWithCacheHints( entry );
				}
				catch ( final ExecutionException e )
				{
					e.printStackTrace();
				}

			return entry.getValue();
		}

		return null;
	}

	protected Entry getEntry( final K key )
	{
		Entry entry = entryCache.get( key );
		if ( entry == null )
		{
			synchronized ( cacheLock )
			{
				entry = entryCache.get( key );
				if ( entry == null )
				{
					entry = new Entry( key );
					entryCache.put( key, entry );
				}
			}
		}
		return entry;
	}

	@Override
	public V get( final K key, final Callable< ? extends V > loader ) throws ExecutionException
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException( "this is implemented alternatively with VolatileLoader. TODO");
	}

	private void loadEntryWithCacheHints( final Entry entry ) throws ExecutionException
	{
		switch ( loadingStrategy )
		{
		case VOLATILE:
		default:
			enqueueEntry( entry );
			break;
		case BLOCKING:
			entry.load();
			break;
		case BUDGETED:
			loadOrEnqueue( entry );
			break;
		case DONTLOAD:
			break;
		}
	}

	private void enqueueEntry( final Entry entry )
	{
		final long currentQueueFrame = queue.getCurrentFrame();
		if ( entry.getEnqueueFrame() < currentQueueFrame )
		{
			entry.setEnqueueFrame( currentQueueFrame );
			queue.put( new EntryLoader( entry.getKey() ), priority, enqueuToFront );
		}
	}

	private void loadOrEnqueue( final Entry entry )
	{
		final IoStatistics stats = CacheIoTiming.getIoStatistics();
		final IoTimeBudget budget = stats.getIoTimeBudget();
		final long timeLeft = budget.timeLeft( priority );
		if ( timeLeft > 0 )
		{
			synchronized ( entry )
			{
				if ( entry.getValue().isValid() )
					return;
				enqueueEntry( entry );
				final long t0 = stats.getIoNanoTime();
				stats.start();
				try
				{
					entry.wait( timeLeft  / 1000000l, 1 );
				}
				catch ( final InterruptedException e )
				{}
				stats.stop();
				final long t = stats.getIoNanoTime() - t0;
				budget.use( t, priority );
			}
		}
		else
			enqueueEntry( entry );
	}

	final class EntryLoader implements Callable< Void >
	{
		private final K key;

		public EntryLoader( final K key )
		{
			this.key = key;
		}

		/**
		 * If this key's data is not yet valid, then load it. After the method
		 * returns, the data is guaranteed to be valid.
		 *
		 * @throws InterruptedException
		 *             if the loading operation was interrupted.
		 */
		@Override
		public Void call() throws Exception
		{
			final Entry entry = entryCache.get( key );
			if ( entry != null )
				entry.load();
			return null;
		}
	}

	@Override
	public ImmutableMap< K, V > getAllPresent( final Iterable< ? > keys )
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException( "not implemented yet");
	}

	@Override
	public void put( final K key, final V value )
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException( "not implemented yet");
	}

	@Override
	public void putAll( final Map< ? extends K, ? extends V > m )
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException( "not implemented yet");
	}

	@Override
	public void invalidate( final Object key )
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException( "not implemented yet");
	}

	@Override
	public void invalidateAll( final Iterable< ? > keys )
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException( "not implemented yet");
	}

	@Override
	public void invalidateAll()
	{
		entryCache.invalidateAll();
		softRefCache.invalidateAll();
	}

	@Override
	public long size()
	{
		return softRefCache.size() + entryCache.size();
	}

	@Override
	public CacheStats stats()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException( "not implemented yet");
	}

	@Override
	public ConcurrentMap< K, V > asMap()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException( "not implemented yet");
	}

	/**
	 * Remove keys from the cache whose values have been garbage-collected. To
	 * avoid long run-times, per call to {@code cleanUp()}, the number of
	 * processed entries is limited.
	 */
	@Override
	public void cleanUp()
	{
		entryCache.cleanUp();
		softRefCache.cleanUp();
	}

	@Override
	public V get( final K key ) throws ExecutionException
	{
		final V v = softRefCache.getIfPresent( key );
		if ( v != null )
			return v;

		final Entry entry = getEntry( key );
		if ( !entry.getValue().isValid() )
			loadEntryWithCacheHints( entry );

		return entry.getValue();
	}

	@Override
	public V getUnchecked( final K key )
	{
		try
		{
			return get( key );
		}
		catch ( final ExecutionException e )
		{
			throw new UncheckedExecutionException( e.getCause() );
		}
	}

	@Override
	public ImmutableMap< K, V > getAll( final Iterable< ? extends K > keys ) throws ExecutionException
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException( "not implemented yet");
	}

	@Override
	public V apply( final K key )
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException( "not implemented yet");
	}

	@Override
	public void refresh( final K key )
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException( "not implemented yet");
	}
}
