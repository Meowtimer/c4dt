package net.arctics.clonk.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.resource.ExternalLib;
import net.arctics.clonk.util.INode;
import net.arctics.clonk.util.ITreeNode;

public class ExternIndex extends ClonkIndex {
	
	private static final long serialVersionUID = 1L;

	private List<ExternalLib> libs;
	
	public ExternIndex() {
		libs = new ArrayList<ExternalLib>();
	}
	
	public List<ExternalLib> getLibs() {
		return libs;
	}
	
	@Override
	public synchronized void refreshCache() {
		for (int i = 0; i < libs.size(); i++)
			libs.get(i).setIndex(i);
		super.refreshCache();
	}
	
	@Override
	public void clear() {
		super.clear();
		libs.clear();
	}

	public C4ScriptBase findScriptByPath(String path) {
		IPath p = new Path(path);
		if (p.segmentCount() >= 2) {
			INode node = null;
			int seg = 0;
			for (Collection<? extends INode> col = libs; col != null && seg < p.segmentCount(); col = node instanceof ITreeNode ? ((ITreeNode)node).getChildCollection() : null, seg++) {
				node = null;
				for (INode n : col) {
					if (n.getNodeName().equals(p.segment(seg))) {
						node = n;
						break;
					}
				}
			}
			if (node instanceof C4ScriptBase)
				return (C4ScriptBase) node;
		}
		return null;
	}
	
	@Override
	public String toString() {
		return "Extern Index"; //$NON-NLS-1$
	}

}
