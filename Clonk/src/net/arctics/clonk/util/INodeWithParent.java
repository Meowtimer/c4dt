package net.arctics.clonk.util;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public interface INodeWithParent {
	String nodeName();
	INodeWithParent parentNode();
	IPath getPath();
	boolean subNodeOf(INodeWithParent node);
	
	public static class Default {
		public static IPath getPath(INodeWithParent node) {
			return node.parentNode() != null ? node.parentNode().getPath().append(node.nodeName()) : new Path(node.nodeName());
		}
		public static boolean subNodeOf(INodeWithParent node, INodeWithParent other) {
			for (INodeWithParent n = node; n != null; n = n.parentNode())
				if (n == other)
					return true;
			return false;
		}
	}
}
