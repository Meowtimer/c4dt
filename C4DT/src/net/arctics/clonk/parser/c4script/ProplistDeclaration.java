package net.arctics.clonk.parser.c4script;

import java.util.List;

import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.C4Structure;

public class ProplistDeclaration extends C4Structure {

	private static final long serialVersionUID = 1L;
	
	private List<C4Variable> components;
	
	public List<C4Variable> getComponents() {
		return components;
	}

	public ProplistDeclaration(List<C4Variable> components) {
		super();
		this.components = components;
		setName("proplist {...}");
	}

	@Override
	public C4Declaration findLocalDeclaration(String declarationName, Class<? extends C4Declaration> declarationClass) {
		for (C4Variable v : getComponents()) {
			if (v.getName().equals(declarationName)) {
				return v;
			}
		}
		return null;
	}
	
	@Override
	public Iterable<? extends C4Declaration> allSubDeclarations(int mask) {
		if ((mask & VARIABLES) != 0)
			return components;
		else
			return NO_SUB_DECLARATIONS;
	}

}
