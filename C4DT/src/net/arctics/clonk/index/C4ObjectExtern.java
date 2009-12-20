package net.arctics.clonk.index;

import java.util.ArrayList;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.C4ID;
import net.arctics.clonk.parser.SimpleScriptStorage;
import net.arctics.clonk.parser.stringtbl.StringTbl;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.resource.ExternalLib;
import net.arctics.clonk.resource.c4group.C4GroupEntry;
import net.arctics.clonk.resource.c4group.C4GroupItem;
import net.arctics.clonk.util.INode;
import net.arctics.clonk.util.ITreeNode;

public class C4ObjectExtern extends C4Object implements ITreeNode, IExternalScript {

	private static final long serialVersionUID = -4964785375712432236L;

	private SimpleScriptStorage script;
	private ITreeNode parentNode;
	protected String nodeName;
	private List<INode> childNodes;
	private Map<String, String> localizedDescriptions;
	private Map<String, StringTbl> stringTables;
	
	private transient ExternalLib externalLib;
	
	public void addStringTbl(String lang, StringTbl tbl) {
		if (stringTables == null)
			stringTables = new HashMap<String, StringTbl>();
		stringTables.put(lang, tbl);
	}
	
	public StringTbl getStringTbl(String lang) {
		if (stringTables == null)
			return null;
		else
			return stringTables.get(lang);
	}
	
	public StringTbl getStringTblForLanguagePref() {
		String pref = ClonkPreferences.getLanguagePref();
		return getStringTbl(pref);
	}
	
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
	public String getScriptText() {
		return script.getContentsAsString();
	}

	@Override
	public Object getScriptFile() {
		return script;
	}

	@Override
	public ClonkIndex getIndex() {
		if (this == ClonkCore.getDefault().getActiveEngine())
			return null;
		ExternalLib lib = getExternalLib();
		return lib != null ? getExternalLib().getIndex() : null;
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
		if (childNodes == null)
			childNodes = new ArrayList<INode>();
		childNodes.add(node);
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
		String locDesc = getDesc(ClonkPreferences.getLanguagePref());
		if (locDesc != null)
			return String.format(INFO_TEXT_TEMPLATE, getName(), locDesc, this.getPath().toOSString());
		return super.getInfoText();
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
	
	public String getFilePath() {
		IPath path = this.getPath();
		if (getExternalLib().getIndex() instanceof ProjectIndex)
			path = path.removeFirstSegments(1);
		IPath gamePath = new Path(ClonkPreferences.getPreference(ClonkPreferences.GAME_PATH, "", null)); //$NON-NLS-1$
		return gamePath.append(path).toOSString();
	}
	
	@Override
	public String toString() {
		return getPath().toOSString();
	}

	@Override
	public SimpleScriptStorage getSimpleStorage() {
		return script;
	}

}
