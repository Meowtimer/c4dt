package net.arctics.clonk.parser.c4script.inference.dabble;

import static net.arctics.clonk.util.Utilities.defaulting;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.ast.TypeUnification;
import net.arctics.clonk.parser.c4script.inference.dabble.DabbleInference.ScriptProcessor;

public abstract class TypeVariable implements ITypeVariable, Cloneable {

	protected IType type = PrimitiveType.UNKNOWN;

	@Override
	public IType type() {
		return type;
	}

	@Override
	public void storeType(IType type) {
		this.type = defaulting(type, PrimitiveType.UNKNOWN);
	}

	@Override
	public boolean hint(IType hint) {
		if (type == PrimitiveType.UNKNOWN)
			storeType(hint);
		else if (type == PrimitiveType.ANY)
			type = TypeUnification.unify(type, hint);
		return true;
	}

	@Override
	public void apply(boolean soft, ScriptProcessor processor) {}

	@Override
	public void merge(ITypeVariable other) {
		if (type() == PrimitiveType.UNKNOWN)
			// unknown before so now it is assumed to be of this type
			storeType(other.type());
		else if (!type().equals(other.type()))
			// assignments of multiple types - unify
			storeType(TypeUnification.unify(type(), other.type()));
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	@Override
	public String toString() {
		return "<>: " + type(); //$NON-NLS-1$
	}

}
