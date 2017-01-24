package bdv.img.cache.loading;

public class AbstractCellLoader
{
	protected final int n;

	private final int[] cellDimensions;

	private final long[] numCells;

	private final int[] borderSize;

	public AbstractCellLoader(
			final long[] dimensions,
			final int[] cellDimensions )
	{
		this.n = dimensions.length;
		this.cellDimensions = cellDimensions.clone();
		numCells = new long[ n ];
		borderSize = new int[ n ];
		for ( int d = 0; d < n; ++d )
		{
			numCells[ d ] = ( dimensions[ d ] - 1 ) / cellDimensions[ d ] + 1;
			borderSize[ d ] = ( int ) ( dimensions[ d ] - ( numCells[ d ] - 1 ) * cellDimensions[ d ] );
		}
	}

	/**
	 * From the index of a cell in the {@link #cells()} grid, compute the image
	 * position of the first pixel of the cell (the offset of the cell in image
	 * coordinates) and the dimensions of the cell. The dimensions will be the
	 * standard {@link #cellDimensions} unless the cell is at the border of the
	 * image in which case it might be truncated.
	 *
	 * @param index
	 *            flattened grid coordinates of the cell.
	 * @param cellMin
	 *            offset of the cell in image coordinates are written here.
	 * @param cellDims
	 *            dimensions of the cell are written here.
	 */
	protected void getCellDimensions( long index, final long[] cellMin, final int[] cellDims )
	{
		for ( int d = 0; d < n; ++d )
		{
			final long j = index / numCells[ d ];
			final long gridPos = index - j * numCells[ d ];
			index = j;
			cellDims[ d ] = ( ( gridPos == numCells[ d ] - 1 ) ? borderSize[ d ] : cellDimensions[ d ] );
			cellMin[ d ] = gridPos * cellDimensions[ d ];
		}
	}
}
