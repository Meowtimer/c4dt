package net.arctics.clonk.util;

import org.eclipse.core.runtime.IPath;

public interface INodeWithPath extends INode {
	public IPath path();
	public INodeWithPath parentNode();
}
