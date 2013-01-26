package net.arctics.clonk.util;



public class PerClass<B, T extends B, S extends PerClass<B, ? super T, ? extends S>> {
	protected final Class<T> cls;
	protected S supr;
	public final Class<T> cls() { return cls; }
	public PerClass(Class<T> cls) { this.cls = cls; }
}
