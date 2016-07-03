package net.arctics.clonk.c4script.ast.evaluate;

public class ConcreteVariable implements IVariable {
	private Object value;
	@Override
	public Object get() { return value; }
	@Override
	public void set(Object value) { this.value = value; }
}
