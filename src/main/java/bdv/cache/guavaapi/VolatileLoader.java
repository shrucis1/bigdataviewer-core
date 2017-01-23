package bdv.cache.guavaapi;

import java.util.concurrent.Callable;

public interface VolatileLoader< V > extends Callable< V >
{
	public V createEmptyValue();
}
