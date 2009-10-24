package net.arctics.clonk.ui.navigator;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IPath;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.ExternIndex;
import net.arctics.clonk.index.ProjectIndex;
import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.util.INode;
import net.arctics.clonk.util.ITreeNode;

public class GlobalDeclarationsNavigatorNode implements ITreeNode {

	private DependenciesNavigatorNode dependenciesNode;
	
	public GlobalDeclarationsNavigatorNode(DependenciesNavigatorNode dependenciesNavigatorNode) {
		dependenciesNode = dependenciesNavigatorNode;
	}

	public void addChild(ITreeNode node) {
	}

	public Collection<? extends INode> getChildCollection() {
		DependenciesNavigatorNode deps = (DependenciesNavigatorNode) getParentNode();
		ExternIndex externIndex = ClonkCore.getDefault().getExternIndex();
		ProjectIndex projIndex = deps.getClonkNature().getIndex();
		List<INode> result = new LinkedList<INode>();
		for (C4Declaration d : externIndex.getGlobalFunctions())
			if (projIndex.acceptsDeclaration(d))
				result.add(d);
		for (C4Declaration d : externIndex.getStaticVariables())
			if (projIndex.acceptsDeclaration(d))
				result.add(d);
		return result;
	}

	public String getNodeName() {
		return "Globals"; //$NON-NLS-1$
	}

	public ITreeNode getParentNode() {
		return dependenciesNode;
	}

	public IPath getPath() {
		return ITreeNode.Default.getPath(this);
	}

	public boolean subNodeOf(ITreeNode node) {
		return ITreeNode.Default.subNodeOf(this, node);
	}
	
	@Override
	public String toString() {
		return getNodeName();
	}

}
