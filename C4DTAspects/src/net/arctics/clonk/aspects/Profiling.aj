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
	private static pointcut allMethods(): execution(* *(..)) && !profiledMethods();
	
	private static class ProfilingFrame {
		public Map<Signature, Long> times = new HashMap<Signature, Long>();
		public String name;
		public ProfilingFrame(String name) {
			this.name = name;
		}
	}
	
	private static boolean ENABLED = false;
	private static final Stack<ProfilingFrame> frames = new Stack<ProfilingFrame>();
	
	before(): profiledMethods() {
		start(thisJoinPoint.getSignature().getName());
	}
	
	after(): profiledMethods() {
		end(thisJoinPoint.getSignature().getName());
	}
	
	public static void start(String name) {
		System.out.println("Start " + name);
		frames.push(new ProfilingFrame(name));
		ENABLED = true;
	}
	
	Object around(): allMethods() {
		if (!ENABLED)
			return proceed();
		else {
			long start = System.currentTimeMillis();
			Object r = proceed();
			long took = System.currentTimeMillis() - start;
			synchronized (frames) {
				if (!ENABLED)
					return r;
				Map<Signature, Long> times = frames.peek().times;
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
		File baseFolder = new File(System.getProperty("user.home"), "Profiling");
		baseFolder.mkdirs();
		File f;
		for (int i = 1; (f = new File(baseFolder, String.format("%s%d.csv", name, i))).exists(); i++);
		return f;
	}
	
	public static void end(String name) {
		System.out.println("End " + name);
		List<Entry<Signature, Long>> list;
		synchronized (frames) {
			Map<Signature, Long> times = frames.pop().times;
			list = new ArrayList<Entry<Signature, Long>>(times.size());
			for (Entry<Signature, Long> s : times.entrySet())
				list.add(s);
			ENABLED = frames.size() > 0;
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
