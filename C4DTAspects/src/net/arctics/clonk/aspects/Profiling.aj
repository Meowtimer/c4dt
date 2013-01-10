package net.arctics.clonk.aspects;

import java.io.File;
import java.io.PrintWriter;
import java.util.Stack;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Map.Entry;

import org.aspectj.lang.Signature;

import net.arctics.clonk.util.Profiled;

public aspect Profiling {
	
	private static pointcut profiledMethods(): execution(@Profiled * *(..));
	private static pointcut profilingMethods(): within(Profiling);
	private static pointcut allMethods(): !profiledMethods() && !profilingMethods() && execution(* *(..));
	
	private static class ProfilingFrame {
		public final Thread thread;
		public final Map<Signature, Long> times = new HashMap<Signature, Long>();
		public ProfilingFrame() {
			thread = Thread.currentThread();
		}
	}
	
	private static final ThreadLocal<Stack<ProfilingFrame>> framesThreadLocal = new ThreadLocal<Stack<ProfilingFrame>>() {
		protected Stack<ProfilingFrame> initialValue() {
			return new Stack<ProfilingFrame>();
		};
	};
	
	public static Stack<ProfilingFrame> frames() {
		return framesThreadLocal.get();
	}
	
	before(): profiledMethods() {
		start(thisJoinPoint.getSignature().getName());
	}
	
	after(): profiledMethods() {
		end(thisJoinPoint.getSignature().getName());
	}
	
	// handle threads spawned by profiled methods
	
	private static final Map<Runnable, ProfilingFrame> profiledRunnables = new HashMap<Runnable, ProfilingFrame>();

	after(Runnable runnable) returning: this(runnable) && initialization(Runnable+.new(..)) {
		final Stack<ProfilingFrame> frames = frames();
		if (frames.empty())
			return;
		profiledRunnables.put(runnable, frames.peek());
	}
	
	void around(Runnable runnable): target(runnable) && execution(void Runnable+.run()) {
		final ProfilingFrame frame = profiledRunnables.get(runnable);
		final boolean profiled = frame != null && frame.thread != Thread.currentThread();
		final Stack<ProfilingFrame> frames = frames();
		if (profiled)
			frames.push(frame);
		try {
			proceed(runnable);
		} finally {
			if (profiled) {
				frames.pop();
				profiledRunnables.remove(runnable);
			}
		}
	}
	
	public static void start(String name) {
		System.out.println("Start " + name);
		frames().push(new ProfilingFrame());
	}
	
	Object around(): allMethods() {
		final Stack<ProfilingFrame> frames = frames();
		if (frames.empty())
			return proceed();
		else {
			long start = System.currentTimeMillis();
			Object r = proceed();
			long took = System.currentTimeMillis() - start;
			Map<Signature, Long> times = frames.peek().times;
			synchronized (times) {
				Long s = times.get(thisJoinPoint.getSignature());
				if (s == null)
					s = took;
				else
					s = s + took;
				times.put(thisJoinPoint.getSignature(), s);
			}
			return r;
		}
	}
	
	private static File csvFile(String name) {
		File baseFolder = new File(System.getProperty("user.home"), "Library");
		baseFolder = new File(baseFolder, "Profiling");
		baseFolder.mkdirs();
		File f;
		for (int i = 1; (f = new File(baseFolder, String.format("%s%d.csv", name, i))).exists(); i++);
		return f;
	}
	
	public static void end(String name) {
		System.out.println("End " + name);
		List<Entry<Signature, Long>> list;
		final Stack<ProfilingFrame> frames = frames();
		ProfilingFrame frame = frames.pop();
		if (frame.thread != Thread.currentThread())
			return; // sanity
		Map<Signature, Long> times = frame.times;
		synchronized (times) {
			list = new ArrayList<Entry<Signature, Long>>(times.size());
			for (Entry<Signature, Long> s : times.entrySet())
				list.add(s);
		}
		Collections.sort(list, new Comparator<Entry<Signature, Long>>() {
			@Override
			public int compare(Entry<Signature, Long> o1, Entry<Signature, Long> o2) {
				double diff = o2.getValue() - o1.getValue();
				return diff > 0 ? 1 : diff < 0 ? -1 : 0;
			}
		});
		try {
			PrintWriter pw = new PrintWriter(csvFile(name));
			try {
				for (Entry<Signature, Long> s : list)
					pw.print(String.format("%s,%s\n", s.getKey().toString().replaceAll(",", " "), s.getValue()));
			} finally {
				pw.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
