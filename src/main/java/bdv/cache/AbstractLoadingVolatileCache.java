/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2016 Tobias Pietzsch, Stephan Saalfeld, Stephan Preibisch,
 * Jean-Yves Tinevez, HongKee Moon, Johannes Schindelin, Curtis Rueden, John Bogovic
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package bdv.cache;

import bdv.cache.CacheIoTiming.IoStatistics;
import bdv.cache.CacheIoTiming.IoTimeBudget;
import bdv.cache.util.BlockingFetchQueues;
import bdv.cache.util.FetcherThreads;


/**
 * A loading cache mapping keys to {@link VolatileCacheValue}s. The cache spawns
 * a set of {@link FetcherThreads} that asynchronously load data for cached
 * values.
 * <p>
 * Using {@link #get(Object, CacheHints, VolatileCacheValueLoader)}, a key is
 * added to the cache, specifying a {@link VolatileCacheValueLoader} to provide
 * the value for the key. After adding the key to the cache, it is immediately
 * associated with a value. However, that value may be initially
 * {@link VolatileCacheValue#isValid() invalid}. When the value is made valid
 * (loaded) depends on the provided {@link CacheHints}, specifically the
 * {@link CacheHints#getLoadingStrategy() loading strategy}. The strategy may be
 * to load the value immediately, to load it immediately if there is enough IO
 * budget left, to enqueue it for asynchronous loading, or to not load it at
 * all.
 * <p>
 * Using {@link #getIfPresent(Object, CacheHints)} a value for the specified key
 * is returned if the key is in the cache (otherwise {@code null} is returned).
 * Again, the returned value may be invalid, and when the value is loaded
 * depends on the provided {@link CacheHints}.
 *
 * @param <K>
 *            the key type.
 * @param <V>
 *            the value type.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 * @author Stephan Saalfeld
 */
abstract public class AbstractLoadingVolatileCache< K, V extends VolatileCacheValue > implements CacheControl
{
	protected final Object cacheLock = new Object();

	protected final CacheIoTiming cacheIoTiming = new CacheIoTiming();

	protected final int numPriorityLevels;

	protected final BlockingFetchQueues< K > queue;

	protected volatile long currentQueueFrame = 0;

	/**
	 * Create a new {@link AbstractLoadingVolatileCache} with the specified number of
	 * priority levels and number of {@link FetcherThreads} for asynchronous
	 * loading of cache entries.
	 *
	 * @param numPriorityLevels
	 *            the number of priority levels (see {@link CacheHints}).
	 */
	public AbstractLoadingVolatileCache( final int numPriorityLevels )
	{
		this.numPriorityLevels = numPriorityLevels;
		queue = new BlockingFetchQueues<>( numPriorityLevels );
	}

	/**
	 * Get the value for the specified key if the key is in the cache (otherwise
	 * return {@code null}). Note, that a value being in the cache only means
	 * that there is data, but not necessarily that the data is
	 * {@link VolatileCacheValue#isValid() valid}.
	 * <p>
	 * If the value is present but not valid, do the following, depending on the
	 * {@link LoadingStrategy}:
	 * <ul>
	 * <li>{@link LoadingStrategy#VOLATILE}: Enqueue the key for asynchronous
	 * loading by a fetcher thread, if it has not been enqueued in the current
	 * frame already.
	 * <li>{@link LoadingStrategy#BLOCKING}: Load the data immediately.
	 * <li>{@link LoadingStrategy#BUDGETED}: Load the data immediately if there
	 * is enough {@link IoTimeBudget} left for the current thread group.
	 * Otherwise enqueue for asynchronous loading, if it has not been enqueued
	 * in the current frame already.
	 * <li>{@link LoadingStrategy#DONTLOAD}: Do nothing.
	 * </ul>
	 *
	 * @param key
	 *            the key to query.
	 * @param cacheHints
	 *            {@link LoadingStrategy}, queue priority, and queue order.
	 * @return the value with the specified key in the cache or {@code null}.
	 */
	abstract public V getIfPresent( final K key, final CacheHints cacheHints );

	/**
	 * Add a new key to the cache, unless it is already present. If the key is
	 * new, after adding the key to the cache, it is immediately associated with
	 * a value. However, that value may be initially
	 * {@link VolatileCacheValue#isValid() invalid}. When the value is made
	 * valid (loaded) depends on the provided {@link CacheHints}, specifically
	 * the {@link CacheHints#getLoadingStrategy() loading strategy}. The
	 * strategy may be to load the value immediately, to load it immediately if
	 * there is enough IO budget left, to enqueue it for asynchronous loading,
	 * or to not load it at all.
	 * <p>
	 * The {@code cacheLoader} will be used both to provide the initial
	 * (invalid) value and to load the (valid) value.
	 * <p>
	 * Whether the key was already present in the cache or not, if the value is
	 * not valid do the following, depending on the {@link LoadingStrategy}:
	 * <ul>
	 * <li>{@link LoadingStrategy#VOLATILE}: Enqueue the key for asynchronous
	 * loading by a fetcher thread, if it has not been enqueued in the current
	 * frame already.
	 * <li>{@link LoadingStrategy#BLOCKING}: Load the data immediately.
	 * <li>{@link LoadingStrategy#BUDGETED}: Load the data immediately if there
	 * is enough {@link IoTimeBudget} left for the current thread group.
	 * Otherwise enqueue for asynchronous loading, if it has not been enqueued
	 * in the current frame already.
	 * <li>{@link LoadingStrategy#DONTLOAD}: Do nothing.
	 * </ul>
	 *
	 * @param key
	 *            the key to query.
	 * @param cacheHints
	 *            {@link LoadingStrategy}, queue priority, and queue order.
	 * @param cacheLoader
	 * @return the value with the specified key in the cache.
	 */
	abstract public V get( final K key, final CacheHints cacheHints, final VolatileCacheValueLoader< ? extends V > cacheLoader );

	/**
	 * (Re-)initialize the IO time budget, that is, the time that can be spent
	 * in blocking IO per frame/
	 *
	 * @param partialBudget
	 *            Initial budget (in nanoseconds) for priority levels 0 through
	 *            <em>n</em>. The budget for level <em>i &gt; j</em> must always
	 *            be smaller-equal the budget for level <em>j</em>. If
	 *            <em>n</em> is smaller than the number of priority levels, the
	 *            remaining priority levels are filled up with @code{budget[n]}.
	 */
	@Override
	public void initIoTimeBudget( final long[] partialBudget )
	{
		final IoStatistics stats = cacheIoTiming.getThreadGroupIoStatistics();
		if ( stats.getIoTimeBudget() == null )
			stats.setIoTimeBudget( new IoTimeBudget( numPriorityLevels ) );
		stats.getIoTimeBudget().reset( partialBudget );
	}

	/**
	 * Get the {@link CacheIoTiming} that provides per thread-group IO
	 * statistics and budget.
	 */
	@Override
	public CacheIoTiming getCacheIoTiming()
	{
		return cacheIoTiming;
	}

	/**
	 * Remove all references to loaded data as well as all enqueued requests
	 * from the cache.
	 */
	abstract public void invalidateAll();

	abstract public FetcherThreads< K > getFetcherThreads();
}
