package bdv.cache.guavaapi;

import java.util.concurrent.Callable;

import bdv.cache.CacheHints;
import bdv.cache.VolatileCacheValue;
import bdv.cache.WeakSoftCacheFinalizeQueue;
import bdv.cache.util.BlockingFetchQueues;

/**
 * Creates Reference caches that share a finalize-queue.
 *
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class RefCacheFactory
{
	private final WeakSoftCacheFinalizeQueue sharedQueue;

	public RefCacheFactory()
	{
		sharedQueue = new WeakSoftCacheFinalizeQueue();
	}

//	< K, V > SoftRefLocalCache< K, V > newSoftRefLocalCache()
//	{
//		return new SoftRefLocalCache<>( sharedQueue );
//	}

	public < K, V > SoftRefCache< K, V > newSoftRefCache()
	{
		return new SoftRefCache< K, V >( new SoftRefLocalCache<>( sharedQueue ) );
	}

	public < K, V extends VolatileCacheValue > WeakRefVolatileCache< K, V > newWeakRefVolatileCache(
			final BlockingFetchQueues< Callable< Void > > fetchQueue,
			final CacheHints cacheHints )
	{
		return new WeakRefVolatileCache< K, V >(
				new WeakRefLocalCache<>( sharedQueue ),
				fetchQueue,
				newSoftRefCache(),
				cacheHints );
	}

	public void cleanUp()
	{
		sharedQueue.cleanUp();
	}
}
