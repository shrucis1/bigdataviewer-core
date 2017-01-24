package bdv.img.cache.loading;

import bdv.img.cache.VolatileCell;
import net.imglib2.img.basictypeaccess.volatiles.VolatileAccess;

public class VolatileCellVolatileCacheLoader< A extends VolatileAccess >
		extends AbstractCellLoader
		implements VolatileCacheLoader< Long, VolatileCell< A > >
{
	private final VolatileArrayLoader< A > arrayLoader;

	public VolatileCellVolatileCacheLoader(
			final VolatileArrayLoader< A > arrayLoader,
			final long[] dimensions,
			final int[] cellDimensions )
	{
		super( dimensions, cellDimensions );
		this.arrayLoader = arrayLoader;
	}

	@Override
	public VolatileCell< A > createEmpty( final Long key )
	{
		final long[] cellMin = new long[ n ];
		final int[] cellDims = new int[ n ];
		getCellDimensions( key, cellMin, cellDims );
		return new VolatileCell<>( cellDims, cellMin, arrayLoader.emptyArray( cellDims ) );
	}

	@Override
	public VolatileCell< A > load( final Long key )
			throws InterruptedException // TODO: ExecutionException?
	{
		final long[] cellMin = new long[ n ];
		final int[] cellDims = new int[ n ];
		getCellDimensions( key, cellMin, cellDims );
		return new VolatileCell<>( cellDims, cellMin, arrayLoader.loadArray( cellDims, cellMin ) );
	}
}
