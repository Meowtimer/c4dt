package net.arctics.clonk.util;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public interface INodeWithPath extends INode {
	
	default IPath path() {
		return this.parentNode() != null ? this.parentNode().path().append(this.nodeName()) : new Path(this.nodeName());
	}
	
	default IPath relativePath(final INodeWithPath superNode) {
		return this.parentNode() != null && this.parentNode() != superNode
			? parentNode().relativePath(superNode).append(this.nodeName()) : new Path(this.nodeName());
	}
	
	public INodeWithPath parentNode();
}
