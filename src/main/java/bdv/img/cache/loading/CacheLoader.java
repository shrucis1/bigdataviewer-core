package bdv.img.cache.loading;

public interface CacheLoader< K, V >
{
	public V load( K key )
			throws InterruptedException; // TODO: ExecutionException?,
											// Exception? Guava CacheLoader.load
											// throws Exception
}
