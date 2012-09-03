package net.arctics.clonk.aspects;

public aspect Profiling {
	pointcut pluginStart(): execution(* net.arctics.clonk.Core.start(BundleContext));
	before(): pluginStart() {
		System.out.println("Test!");
	}
}
