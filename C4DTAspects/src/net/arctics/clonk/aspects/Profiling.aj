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
import org.aspectj.lang.annotation.SuppressAjWarnings;

import net.arctics.clonk.util.Profiled;

@SuppressAjWarnings
public aspect Profiling {

	private static pointcut profiledMethods(): execution(@Profiled * *(..));
	private static pointcut profilingMethods(): within(Profiling);
	private static pointcut allMethods(): !profiledMethods() && !profilingMethods() && execution(* *(..));

	private static final class Timer { public long time; }

	private static final class SignatureProfile {
		public long executionTime;
		public long timesCalled;
	}

	private static final class ProfilingFrame {
		public final Thread thread;
		public final Map<Signature, SignatureProfile> times = new HashMap<Signature, SignatureProfile>();
		public Timer timer;
		public ProfilingFrame() { thread = Thread.currentThread(); }
		public void merge(ProfilingFrame other) {
			synchronized (times) {
				for (final Map.Entry<Signature, SignatureProfile> t : other.times.entrySet())
					addTime(t.getKey(), t.getValue().executionTime, t.getValue().timesCalled);
			}
		}
		public void addTime(Signature sig, long time, long num) {
			synchronized (times) {
				SignatureProfile s = times.get(sig);
				if (s == null) {
					s = new SignatureProfile();
					s.executionTime = time;
					s.timesCalled = num;
					times.put(sig, s);
				}
				else {
					s.executionTime += time;
					s.timesCalled += num;
				}
			}
		}
	}

	private static final ThreadLocal<Stack<ProfilingFrame>> framesThreadLocal = new ThreadLocal<Stack<ProfilingFrame>>() {
		@Override
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
		synchronized (profiledRunnables) {
			profiledRunnables.put(runnable, frames.peek());
		}
	}

	void around(Runnable runnable): target(runnable) && execution(void Runnable+.run()) {
		ProfilingFrame mainFrame;
		synchronized (profiledRunnables) {
			mainFrame = profiledRunnables.get(runnable);
		}
		final boolean profiled = mainFrame != null && mainFrame.thread != Thread.currentThread();
		if (profiled) {
			final Stack<ProfilingFrame> frames = frames();
			final ProfilingFrame runnableFrame = new ProfilingFrame();
			frames.push(runnableFrame);
			try {
				proceed(runnable);
			} finally {
				frames.pop();
				synchronized (profiledRunnables) {
					profiledRunnables.remove(runnable);
				}
				mainFrame.merge(runnableFrame);
			}
		} else
			proceed(runnable);
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
			final ProfilingFrame frame = frames.peek();
			final Timer timer = new Timer();
			final Timer parent = frame.timer;
			frame.timer = timer;
			final long start = System.currentTimeMillis();
			final Object r = proceed();
			final long took = System.currentTimeMillis() - start;
			if (parent != null)
				parent.time -= took; 
			timer.time += took;
			frame.timer = parent;
			frame.addTime(thisJoinPoint.getSignature(), timer.time, 1);
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
		List<Entry<Signature, SignatureProfile>> list;
		final Stack<ProfilingFrame> frames = frames();
		final ProfilingFrame frame = frames.pop();
		if (frame.thread != Thread.currentThread())
			return; // sanity
		final Map<Signature, SignatureProfile> times = frame.times;
		synchronized (times) {
			list = new ArrayList<Entry<Signature, SignatureProfile>>(times.size());
			for (final Entry<Signature, SignatureProfile> s : times.entrySet())
				list.add(s);
		}
		Collections.sort(list, new Comparator<Entry<Signature, SignatureProfile>>() {
			@Override
			public int compare(Entry<Signature, SignatureProfile> o1, Entry<Signature, SignatureProfile> o2) {
				final double diff = o2.getValue().executionTime - o1.getValue().executionTime;
				return diff > 0 ? 1 : diff < 0 ? -1 : 0;
			}
		});
		try {
			final PrintWriter pw = new PrintWriter(csvFile(name));
			try {
				for (final Entry<Signature, SignatureProfile> s : list)
					pw.print(String.format("%s,%s,%s\n",
						s.getKey().toString().replaceAll(",", " "),
						s.getValue().executionTime,
						s.getValue().timesCalled
						));
			} finally { pw.close(); }
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
}
