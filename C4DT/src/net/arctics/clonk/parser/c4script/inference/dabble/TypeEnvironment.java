package net.arctics.clonk.parser.c4script.inference.dabble;

import java.util.HashMap;
import java.util.Map;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.c4script.ast.TypeUnification;
import net.arctics.clonk.parser.c4script.inference.dabble.DabbleInference.Visitor;

public final class TypeEnvironment extends HashMap<Declaration, TypeVariable> {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	public TypeEnvironment up;
	public TypeEnvironment() { super(5); }
	public TypeEnvironment(TypeEnvironment up) { this(); this.up = up; }
	public TypeEnvironment inject(TypeEnvironment other, boolean ignoreLocals) {
		for (Map.Entry<Declaration, TypeVariable> otherInfo : other.entrySet()) {
			TypeVariable myVar = this.get(otherInfo.getKey());
			if (myVar != null)
				myVar.set(TypeUnification.unify(myVar.get(), otherInfo.getValue().get()));
			else
				this.put(otherInfo.getKey(), otherInfo.getValue());
		}
		return this;
	}
	public void apply(Visitor visitor, boolean soft) {
		for (TypeVariable info : this.values())
			info.apply(soft, visitor);
	}
	public final void injectIntoUpper(boolean ignoreLocals) {
		if (up != null)
			inject(up, ignoreLocals);
	}
	public TypeVariable find(Declaration declaration) {
		return this.get(declaration);
	}
	public void add(TypeVariable var) { this.put(var.key(), var); }
}