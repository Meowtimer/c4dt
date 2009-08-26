package net.arctics.clonk.util;

import java.util.Collection;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public interface ITreeNode extends INode {
	ITreeNode getParentNode();
	IPath getPath();
	Collection<? extends INode> getChildCollection();
	boolean subNodeOf(ITreeNode node);
	void addChild(ITreeNode node);	
	
	public static class Default {
		public static IPath getPath(ITreeNode node) {
			return node.getParentNode() != null ? node.getParentNode().getPath().append(node.getNodeName()) : new Path(node.getNodeName());
		}
		public static boolean subNodeOf(ITreeNode node, ITreeNode other) {
			for (ITreeNode n = node; n != null; n = n.getParentNode())
				if (n == other)
					return true;
			return false;
		}
	}
	
}
