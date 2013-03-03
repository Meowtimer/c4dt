package net.arctics.clonk.parser.c4script.inference.dabble;

import static net.arctics.clonk.util.Utilities.defaulting;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.ast.TypeUnification;
import net.arctics.clonk.parser.c4script.inference.dabble.DabbleInference.Visitor;

public abstract class TypeVariable implements Cloneable {
	protected IType type = PrimitiveType.UNKNOWN;
	public final IType get() { return type; }
	public final void set(IType type) { this.type = defaulting(type, PrimitiveType.UNKNOWN); }

	public boolean hint(IType hint) {
		if (type == PrimitiveType.UNKNOWN)
			set(hint);
		else if (type == PrimitiveType.ANY)
			type = TypeUnification.unify(type, hint);
		return true;
	}

	public void apply(boolean soft, Visitor visitor) {}
	public abstract Declaration declaration();
	public abstract Declaration key();
	
	@Override
	public Object clone() throws CloneNotSupportedException { return super.clone(); }
	@Override
	public String toString() { return "<>: " + get(); } //$NON-NLS-1$

}
