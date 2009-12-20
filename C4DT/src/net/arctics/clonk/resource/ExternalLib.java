/**
 * 
 */
package net.arctics.clonk.resource;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import net.arctics.clonk.index.C4ObjectExternGroup;
import net.arctics.clonk.index.ExternIndex;
import net.arctics.clonk.index.ProjectIndex;
import net.arctics.clonk.resource.c4group.C4Group;
import net.arctics.clonk.resource.c4group.C4Group.C4GroupType;

/**
 * One external lib
 */
public class ExternalLib extends C4ObjectExternGroup {

	private static final long serialVersionUID = 1L;

	private boolean scriptsGroup;
	private String fullPath;
	private ExternIndex externIndex;
	
	public ExternIndex getIndex() {
		return externIndex;
	}

	public String getFullPath() {
		return fullPath;
	}

	public void setFullPath(String fullPath) {
		this.fullPath = fullPath;
	}

	public boolean isScriptsGroup() {
		return scriptsGroup;
	}
	
	public ExternalLib(String nodeName, ExternIndex index) {
		super(nodeName, null);
		this.externIndex = index;
		scriptsGroup = C4Group.groupTypeFromFolderName(nodeName) == C4GroupType.ResourceGroup;
	}
	
	@Override
	public IPath getPath() {
		if (externIndex instanceof ProjectIndex) {
			IProject proj = ((ProjectIndex)externIndex).getProject();
			return new Path(proj.getName()).append(super.getPath());
		}
		else
			return super.getPath();
	}
	
}