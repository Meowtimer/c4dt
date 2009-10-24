package net.arctics.clonk.ui.navigator;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IPath;

import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.resource.ExternalLib;
import net.arctics.clonk.util.ITreeNode;

/**
 * Inserted into the CommonNavigator as the node that contains project dependencies
 * @author madeen
 */
public class DependenciesNavigatorNode implements ITreeNode {
	
	private transient ClonkProjectNature clonkNature;
	private transient GlobalDeclarationsNavigatorNode globalsNode = new GlobalDeclarationsNavigatorNode(this);
	
	public DependenciesNavigatorNode(ClonkProjectNature nature) {
		clonkNature = nature;
	}

	public ClonkProjectNature getClonkNature() {
		return clonkNature;
	}

	@Override
	public String toString() {
		return Messages.DependenciesNavigatorNode_0;
	}

	public List<? extends ITreeNode> getChildCollection() {
		List<ITreeNode> result = new LinkedList<ITreeNode>();
		for (ExternalLib lib : clonkNature.getDependencies())
			result.add(lib);
		result.add(globalsNode);
		return result;
	}

	public IPath getPath() {
		return ITreeNode.Default.getPath(this);
	}

	public String getNodeName() {
		return Messages.DependenciesNavigatorNode_1;
	}

	public ITreeNode getParentNode() {
		return null;
	}

	public boolean subNodeOf(ITreeNode node) {
		return ITreeNode.Default.subNodeOf(this, node);
	}

	public void addChild(ITreeNode node) {
		// don't care
	}
}
