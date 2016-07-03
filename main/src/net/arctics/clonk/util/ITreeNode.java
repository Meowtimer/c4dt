package net.arctics.clonk.util;

import static net.arctics.clonk.util.Utilities.walk;

import java.util.Collection;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public interface ITreeNode extends INodeWithPath {
	
	@Override
	ITreeNode parentNode();
	
	Collection<? extends INode> childCollection();
	
	default boolean subNodeOf(ITreeNode other) {
		return walk(this, ITreeNode::parentNode).anyMatch(x -> x == other);
	}
	
	default IPath pathRelativeTo(final ITreeNode other) {
		return
			this == other ? Path.EMPTY :
			this.parentNode() == null ? new Path(this.nodeName()) :
			parentNode().pathRelativeTo(other).append(this.nodeName());
	}
	
	void addChild(ITreeNode node);
}
