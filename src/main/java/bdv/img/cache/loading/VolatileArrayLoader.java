package bdv.img.cache.loading;

public interface VolatileArrayLoader< A > extends ArrayLoader< A >
{
	public A emptyArray( final int[] dimensions );
}
