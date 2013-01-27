package net.arctics.clonk.parser.c4script;

import java.util.ArrayList;
import java.util.List;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.c4script.ast.VarInitialization;

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
