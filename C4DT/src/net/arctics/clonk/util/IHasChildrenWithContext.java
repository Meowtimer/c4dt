package net.arctics.clonk.util;

public interface IHasChildrenWithContext {
	public IHasContext[] children(Object context);
	public boolean hasChildren();
	public Object valueOfChildAt(int index);
	public void setValueOfChildAt(int index, Object value);
}
