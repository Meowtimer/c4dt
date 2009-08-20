/**
 * 
 */
package net.arctics.clonk.resource;

import net.arctics.clonk.index.C4ObjectExternGroup;
import net.arctics.clonk.util.ITreeNode;

/**
 * One external lib
 */
public class ExternalLib extends C4ObjectExternGroup {

	private static final long serialVersionUID = 1L;
	
	private int index;
	private boolean scriptsGroup;
	
	public boolean isScriptsGroup() {
		return scriptsGroup;
	}
	
	public ExternalLib(String nodeName, ITreeNode parentNode) {
		super(nodeName, parentNode);
		scriptsGroup = nodeName.endsWith(".c4g");
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}
	
}