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
		getLibs();
	}
	
	public List<ExternalLib> getLibs() {
		if (libs == null)
			libs = new ArrayList<ExternalLib>();
		return libs;
	}
	
	public String[] getLibPaths() {
		if (libs == null) {
			return new String[0];
		} else {
			String[] result = new String[libs.size()];
			for (int i = 0; i < result.length; i++)
				result[i] = libs.get(i).getFullPath();
			return result;
		}
	}
	
	@Override
	public void clear() {
		super.clear();
		if (libs != null)
			libs.clear();
	}

	@Override
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
	
	public String libsEncodedAsString() {
		if (libs == null)
			return ""; //$NON-NLS-1$
		StringBuilder builder = new StringBuilder();
		int i = 0;
		for (ExternalLib lib : libs) {
			String s = lib.getFullPath();
			builder.append(s);
			builder.append("<>"); //$NON-NLS-1$
			i++;
		}
		return builder.toString();
	}
	
	@Override
	public String toString() {
		return "Extern Index"; //$NON-NLS-1$
	}
	
	private void removeObjects(ITreeNode container) {
		for (INode node : container.getChildCollection()) {
			if (node instanceof ITreeNode) {
				removeObjects((ITreeNode) node);
			}
			if (node instanceof C4ScriptBase) {
				removeScript((C4ScriptBase) node);
			}
		}
	}
	
	public void removeExternalLib(ExternalLib lib) {
		removeObjects(lib);
		libs.remove(lib);
	}

}
