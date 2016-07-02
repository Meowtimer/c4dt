package net.arctics.clonk.util;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import net.arctics.clonk.preferences.ClonkPreferences;

public class TaskExecution {

	public static final int THRESHOLD = 20;

	public static int threadPoolSize() { return ClonkPreferences.integer(ClonkPreferences.TASKEXECUTION_THREADS); }

	public static void threadPool(final Collection<? extends Runnable> runnables, final int timeoutMinutes) {
		if (runnables.size() < THRESHOLD) {
			runnables.forEach(r -> r.run());
		} else {
			threadPool(pool -> runnables.forEach(r -> pool.execute(r)), timeoutMinutes, runnables.size());
		}
	}

	public static void threadPool(final Sink<ExecutorService> action, final int timeoutMinutes, final Integer numWorkUnits) {
		final ExecutorService pool = newPool(numWorkUnits);
		try {
			action.receive(pool);
		} finally {
			pool.shutdown();
			try {
				pool.awaitTermination(timeoutMinutes, TimeUnit.MINUTES);
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public static ExecutorService newPool(final Integer numWorkUnits) {
		return numWorkUnits != null && numWorkUnits >= THRESHOLD
			? Executors.newFixedThreadPool(threadPoolSize())
			: Executors.newSingleThreadExecutor();
	}

	public static <Key, Value> ConcurrentMap<Key, Value> newConcurrentMap() {
		return new ConcurrentHashMap<Key, Value>(10, 0.75f, ClonkPreferences.integer(ClonkPreferences.TASKEXECUTION_THREADS));
	}

}
