package net.arctics.clonk.ui.navigator;

import java.util.List;

import org.eclipse.core.runtime.IPath;

import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.util.ITreeNode;

/**
 * Inserted into the CommonNavigator as the node that contains project dependencies
 * @author madeen
 */
public class DependenciesNavigatorNode implements ITreeNode {
	
	private transient ClonkProjectNature clonkNature;
	
	public DependenciesNavigatorNode(ClonkProjectNature nature) {
		clonkNature = nature;
	}

	public ClonkProjectNature getClonkNature() {
		return clonkNature;
	}

	@Override
	public String toString() {
		return "Dependencies";
	}

	public List<? extends ITreeNode> getChildren() {
		return clonkNature.getDependencies();
	}

	public IPath getPath() {
		return ITreeNode.Default.getPath(this);
	}

	public String nodeName() {
		return "Dependencies";
	}

	public ITreeNode parentNode() {
		return null;
	}

	public boolean subNodeOf(ITreeNode node) {
		return ITreeNode.Default.subNodeOf(this, node);
	}

	public void addChild(ITreeNode node) {
		// don't care
	}
}
