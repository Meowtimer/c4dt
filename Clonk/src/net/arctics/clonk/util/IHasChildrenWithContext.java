package net.arctics.clonk.util;

public interface IHasChildrenWithContext {
	public IHasContext[] getChildren(Object context);
	public boolean hasChildren();
	public Object getChildValue(int index);
	public void setChildValue(int index, Object value);
}
