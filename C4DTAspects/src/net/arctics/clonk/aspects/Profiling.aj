package net.arctics.clonk.aspects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.aspectj.lang.Signature;
import net.arctics.clonk.util.Profiled;

public aspect Profiling {
	
	private static pointcut profiledMethods(): execution(@Profiled * *(..));
	private static pointcut allMethods(): execution(* *(..)) && !profiledMethods();
	private static boolean ENABLED = false;
	private static Map<Signature, Long> PROFILING = new HashMap<Signature, Long>();
	
	before(): profiledMethods() {
		start(thisJoinPoint.getSignature().toString());
	}
	
	after(): profiledMethods() {
		end(thisJoinPoint.getSignature().toString());
	}
	
	public static void start(String name) {
		System.out.println("Start " + name);
		ENABLED = true;
	}
	
	Object around(): allMethods() {
		if (!ENABLED)
			return proceed();
		else {
			long start = System.currentTimeMillis();
			Object r = proceed();
			long took = System.currentTimeMillis() - start;
			synchronized (PROFILING) {
				Long s = PROFILING.get(thisJoinPoint.getSignature());
				if (s == null)
					s = took;
				else
					s = s + took;
				PROFILING.put(thisJoinPoint.getSignature(), s);
			}
			return r;
		}
	}
	
	public static void end(String name) {
		ENABLED = false;
		System.out.println("End " + name);
		List<Entry<Signature, Long>> list;
		synchronized (PROFILING) {
			list = new ArrayList<Entry<Signature, Long>>(PROFILING.size());
			for (Entry<Signature, Long> s : PROFILING.entrySet())
				list.add(s);
			PROFILING.clear();
		}
		Collections.sort(list, new Comparator<Entry<Signature, Long>>() {
			@Override
			public int compare(Entry<Signature, Long> o1, Entry<Signature, Long> o2) {
				double diff = o2.getValue() - o1.getValue();
				return diff > 0 ? 1 : diff < 0 ? -1 : 0;
			}
		});
		for (Entry<Signature, Long> s : list)
			System.out.println(s.getKey() + ": " + s.getValue());
	}
}
