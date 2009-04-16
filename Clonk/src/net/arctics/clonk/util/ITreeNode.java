package net.arctics.clonk.util;

import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public interface ITreeNode {
	String nodeName();
	ITreeNode parentNode();
	IPath getPath();
	List<? extends ITreeNode> getChildren();
	void addChild(ITreeNode node);
	boolean subNodeOf(ITreeNode node);
	
	public static class Default {
		public static IPath getPath(ITreeNode node) {
			return node.parentNode() != null ? node.parentNode().getPath().append(node.nodeName()) : new Path(node.nodeName());
		}
		public static boolean subNodeOf(ITreeNode node, ITreeNode other) {
			for (ITreeNode n = node; n != null; n = n.parentNode())
				if (n == other)
					return true;
			return false;
		}
	}
}
