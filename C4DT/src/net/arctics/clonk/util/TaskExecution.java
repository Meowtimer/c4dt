package net.arctics.clonk.util;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TaskExecution {
	public static final int THRESHOLD = 20;
	public static int threadPoolSize = Runtime.getRuntime().availableProcessors();
	public static void threadPool(final Collection<? extends Runnable> runnables, int timeoutMinutes) {
		if (runnables.size() < THRESHOLD)
			for (final Runnable r : runnables)
				r.run();
		else
			threadPool(new Sink<ExecutorService>() {
				@Override
				public void receivedObject(ExecutorService pool) {
					for (final Runnable r : runnables)
						pool.execute(r);
				}
			}, timeoutMinutes, runnables.size());
	}
	public static void threadPool(Sink<ExecutorService> action, int timeoutMinutes, Integer numWorkUnits) {
		final ExecutorService pool = newPool(numWorkUnits);
		try {
			action.receivedObject(pool);
		} finally {
			pool.shutdown();
			try {
				pool.awaitTermination(timeoutMinutes, TimeUnit.MINUTES);
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	public static ExecutorService newPool(Integer numWorkUnits) {
		return numWorkUnits != null && numWorkUnits >= THRESHOLD
			? Executors.newCachedThreadPool()
			: Executors.newSingleThreadExecutor();
	}
}
