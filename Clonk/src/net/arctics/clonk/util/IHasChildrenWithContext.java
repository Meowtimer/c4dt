package net.arctics.clonk.util;

public interface IHasChildrenWithContext {
	public IHasContext[] getChildren(Object context);
	public boolean hasChildren();
}
