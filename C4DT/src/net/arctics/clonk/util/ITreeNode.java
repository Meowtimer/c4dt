package net.arctics.clonk.util;

import java.util.Collection;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public interface ITreeNode extends INodeWithPath {
	ITreeNode getParentNode();
	Collection<? extends INode> getChildCollection();
	boolean subNodeOf(ITreeNode node);
	void addChild(ITreeNode node);	
	
	public static class Default {
		public static IPath getPath(INodeWithPath node) {
			return node.getParentNode() != null ? node.getParentNode().getPath().append(node.nodeName()) : new Path(node.nodeName());
		}
		public static boolean subNodeOf(ITreeNode node, ITreeNode other) {
			for (ITreeNode n = node; n != null; n = n.getParentNode())
				if (n == other)
					return true;
			return false;
		}
		public static IPath pathRelativeTo(ITreeNode item, ITreeNode other) {
			if (item == other)
				return Path.EMPTY;
			else if (item.getParentNode() == null)
				return new Path(item.nodeName());
			else
				return pathRelativeTo(item.getParentNode(), other).append(item.nodeName());
		}
		public static IPath relativePath(INodeWithPath node, INodeWithPath superNode) {
			return node.getParentNode() != null && node.getParentNode() != superNode ? relativePath(node.getParentNode(), superNode).append(node.nodeName()) : new Path(node.nodeName());
		}
	}
	
}
