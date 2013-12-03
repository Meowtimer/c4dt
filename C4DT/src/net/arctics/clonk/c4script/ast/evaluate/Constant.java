package net.arctics.clonk.c4script.ast.evaluate;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class Constant implements IVariable {
	private final Object value;
	public Constant(final Object value) {
		super();
		this.value = value;
	}
	@Override
	public Object get() { return value; }
	@Override
	public void set(final Object value) { throw new NotImplementedException(); }
}
