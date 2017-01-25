package bdv.img.cache.loading;

public interface VolatileCacheLoader< K, V > extends CacheLoader< K, V >
{
	public V createEmpty( final K key );
}
