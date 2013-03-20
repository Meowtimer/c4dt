package net.arctics.clonk.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TaskExecution {
	public static int threadPoolSize = Runtime.getRuntime().availableProcessors();
	public static void threadPool(Sink<ExecutorService> action, int timeoutMinutes) {
		final ExecutorService pool = Executors.newFixedThreadPool(threadPoolSize);
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
}
