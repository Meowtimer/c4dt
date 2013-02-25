package net.arctics.clonk.parser.c4script.inference.dabble;

import static net.arctics.clonk.util.Utilities.defaulting;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.ast.TypeUnification;
import net.arctics.clonk.parser.c4script.inference.dabble.DabbleInference.Visitation;

public abstract class TypeVariable implements ITypeVariable, Cloneable {

	protected IType type = PrimitiveType.UNKNOWN;

	@Override
	public IType get() {
		return type;
	}

	@Override
	public void set(IType type) {
		this.type = defaulting(type, PrimitiveType.UNKNOWN);
	}

	@Override
	public boolean hint(IType hint) {
		if (type == PrimitiveType.UNKNOWN)
			set(hint);
		else if (type == PrimitiveType.ANY)
			type = TypeUnification.unify(type, hint);
		return true;
	}

	@Override
	public void apply(boolean soft, Visitation processor) {}

	@Override
	public void merge(ITypeVariable other) {
		if (get() == PrimitiveType.UNKNOWN)
			// unknown before so now it is assumed to be of this type
			set(other.get());
		else if (!get().equals(other.get()))
			// assignments of multiple types - unify
			set(TypeUnification.unify(get(), other.get()));
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	@Override
	public String toString() {
		return "<>: " + get(); //$NON-NLS-1$
	}

}
