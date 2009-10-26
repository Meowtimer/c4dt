/**
 * 
 */
package net.arctics.clonk.resource;

import net.arctics.clonk.index.C4ObjectExternGroup;
import net.arctics.clonk.resource.c4group.C4Group;
import net.arctics.clonk.resource.c4group.C4Group.C4GroupType;
import net.arctics.clonk.util.ITreeNode;

/**
 * One external lib
 */
public class ExternalLib extends C4ObjectExternGroup {

	private static final long serialVersionUID = 1L;
	
	private int index;
	private boolean scriptsGroup;
	private String fullPath;
	
	public String getFullPath() {
		return fullPath;
	}

	public void setFullPath(String fullPath) {
		this.fullPath = fullPath;
	}

	public boolean isScriptsGroup() {
		return scriptsGroup;
	}
	
	public ExternalLib(String nodeName, ITreeNode parentNode) {
		super(nodeName, parentNode);
		scriptsGroup = C4Group.groupTypeFromFolderName(nodeName) == C4GroupType.ResourceGroup;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}
	
}