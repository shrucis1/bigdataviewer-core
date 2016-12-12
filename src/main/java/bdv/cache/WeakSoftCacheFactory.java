package bdv.cache;

/**
 * Creates {@link WeakSoftCache}s that share a finalize-queue.
 *
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class WeakSoftCacheFactory
{
	private final WeakSoftCacheFinalizeQueue sharedQueue;

	public WeakSoftCacheFactory()
	{
		sharedQueue = new WeakSoftCacheFinalizeQueue();
	}

	/**
	 * Create a new {@link WeakSoftCache}.
	 */
	public < K, V > WeakSoftCache< K, V > newInstance()
	{
		return new WeakSoftCacheImp<>( sharedQueue );
	}

	public void cleanUp()
	{
		sharedQueue.cleanUp();
	}
}
