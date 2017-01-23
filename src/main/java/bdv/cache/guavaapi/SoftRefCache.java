package bdv.cache.guavaapi;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheStats;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ExecutionError;
import com.google.common.util.concurrent.UncheckedExecutionException;

public class SoftRefCache< K, V > implements Cache< K, V >
{
	private final SoftRefLocalCache< K, Entry > entryCache;

	final class Entry
	{
		private final K key;

		private V value;

		public Entry( final K key )
		{
			this.key = key;
			this.value = null;
		}

		public K getKey()
		{
			return key;
		}

		public V getValue()
		{
			return value;
		}

		public void setValue( final V value )
		{
			this.value = value;
		}
	}

	private final Object cacheLock = new Object();

	SoftRefCache( final SoftRefLocalCache< K, Entry > entryCache )
	{
		this.entryCache = entryCache;
	}

	/**
	 * Returns the value associated with {@code key} in this cache, or
	 * {@code null} if there is no cached value for {@code key}.
	 */
	@Override
	public V getIfPresent( final Object key )
	{
		final Entry entry = entryCache.get( key );
		return entry == null ? null : entry.getValue();
	}

	@Override
	public V get( final K key, final Callable< ? extends V > loader ) throws ExecutionException
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

		if ( entry.getValue() == null )
		{
			synchronized ( entry )
			{
				if ( entry.getValue() == null )
				{
					try
					{
						entry.setValue( loader.call() );
					}
					catch ( final InterruptedException e )
					{
						Thread.currentThread().interrupt();
						throw new ExecutionException( e );
					}
					catch ( final RuntimeException e )
					{
						throw new UncheckedExecutionException( e );
					}
					catch ( final Exception e )
					{
						throw new ExecutionException( e );
					}
					catch ( final Error e )
					{
						throw new ExecutionError( e );
					}
				}
			}
		}

		return entry.getValue();
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
	}

	@Override
	public long size()
	{
		return entryCache.size();
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
	}
}
