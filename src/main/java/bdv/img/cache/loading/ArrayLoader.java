package bdv.img.cache.loading;

public interface ArrayLoader< A >
{
	public A loadArray( int[] dimensions, long[] min )
			throws InterruptedException; // TODO: ExecutionException?
}
