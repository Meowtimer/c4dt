package net.arctics.clonk.parser.c4script;

import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Index;

public class EffectPropListDeclaration extends ProplistDeclaration {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	public EffectPropListDeclaration(Index index, String effectName, List<Variable> variables) {
		super(variables);
		this.name = effectName;
		this.adHoc = true;
		this.components = new LinkedList<Variable>();
	}
	@Override
	public String typeName(boolean special) {
		return String.format("'%s' proplist", name);
	}
}
