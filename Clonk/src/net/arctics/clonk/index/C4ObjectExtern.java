package net.arctics.clonk.index;

import java.util.ArrayList;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IPath;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.C4ID;
import net.arctics.clonk.parser.SimpleScriptStorage;
import net.arctics.clonk.resource.ExternalLib;
import net.arctics.clonk.resource.c4group.C4GroupEntry;
import net.arctics.clonk.resource.c4group.C4GroupItem;
import net.arctics.clonk.util.INode;
import net.arctics.clonk.util.ITreeNode;

public class C4ObjectExtern extends C4Object implements ITreeNode {

	private static final long serialVersionUID = -4964785375712432236L;
	
	private SimpleScriptStorage script;
	private ITreeNode parentNode;
	private String nodeName;
	private List<INode> childNodes;
	private Map<String, String> localizedDescriptions;
	
	private transient ExternalLib externalLib;
	
	public C4ObjectExtern(C4ID id, String name, C4GroupItem script, ITreeNode parentNode) {
		super(id, name);
		this.script = script != null ? new SimpleScriptStorage((C4GroupEntry)script) : null;
		this.parentNode = parentNode;
		if (parentNode != null)
			parentNode.addChild(this);
		if (script != null && script.getParentGroup() != null)
			this.nodeName = script.getParentGroup().getName();
		else
			this.nodeName = name;
	}

	@Override
	public Object getScriptFile() {
		return script;
	}

	@Override
	public ClonkIndex getIndex() {
		if (this == ClonkCore.getDefault().getEngineObject())
			return null;
		return ClonkCore.getDefault().getExternIndex();
	}

	// return name of folder instead of name of object (so constructed paths don't look wierd)
	public String getNodeName() {
		if (nodeName == null)
			nodeName = getName();
		return nodeName;
	}

	public ITreeNode getParentNode() {
		return parentNode;
	}

	public IPath getPath() {
		return ITreeNode.Default.getPath(this); 
	}

	public boolean subNodeOf(ITreeNode node) {
		return ITreeNode.Default.subNodeOf(this, node);
	}

	public List<INode> getChildCollection() {
		if (childNodes == null)
			childNodes = new ArrayList<INode>();
		if (hasSubDeclarationsInOutline()) {
			List<INode> result = new LinkedList<INode>();
			for (int i = 0; i < childNodes.size(); i++)
				result.add(childNodes.get(i));
			Collections.addAll(result, getSubDeclarationsForOutline());
			return result;
		}
		return childNodes;
	}

	public void addChild(ITreeNode node) {
		getChildCollection().add(node);
	}
	
	@Override
	public boolean isEditable() {
		return false;
	}
	
	public void addDesc(String lang, String text) {
		if (localizedDescriptions == null)
			localizedDescriptions = new HashMap<String, String>();
		localizedDescriptions.put(lang, text);
	}
	
	public String getDesc(String lang) {
		if (localizedDescriptions == null)
			return null;
		return localizedDescriptions.get(lang);
	}
	
	@Override
	public String getInfoText() {
		String locDesc = getDesc(ClonkCore.getDefault().getLanguagePref());
		if (locDesc != null)
			return getName() + ": " + locDesc;
		return getName();
	}

	public ExternalLib getExternalLib() {
		if (externalLib == null)
			for (ITreeNode node = this; node != null; node = node.getParentNode())
				if (node instanceof ExternalLib) {
					externalLib = (ExternalLib) node;
					break;
				}
		return externalLib;
	}

}
