package net.arctics.clonk.c4script;

import java.util.ArrayList;
import java.util.List;

import net.arctics.clonk.Core;
import net.arctics.clonk.c4script.ast.VarInitialization;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.Declaration;

public class Variables extends Declaration {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private List<VarInitialization> initializations;
	public Variables(List<VarInitialization> initializations) {
		super();
		this.initializations = initializations;
	}
	@Override
	public ASTNode[] subElements() {
		return initializations.toArray(new ASTNode[initializations.size()]);
	}
	@Override
	public void setSubElements(ASTNode[] elms) {
		initializations = new ArrayList<VarInitialization>(elms.length);
		for (ASTNode e : elms)
			initializations.add((VarInitialization)e);
	}
}
