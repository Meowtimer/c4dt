package net.arctics.clonk.parser.c4script.typing.dabble;

import static net.arctics.clonk.util.Utilities.defaulting;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.PrimitiveType;

public abstract class TypeVariable implements Cloneable {
	protected IType type = PrimitiveType.UNKNOWN;
	public final IType get() { return type; }
	public final void set(IType type) { this.type = defaulting(type, PrimitiveType.UNKNOWN); }
	public void apply(boolean soft) {}
	public abstract Declaration declaration();
	public abstract Declaration key();
	@Override
	public TypeVariable clone() throws CloneNotSupportedException { return (TypeVariable)super.clone(); }
	@Override
	public String toString() { return "<>: " + get(); } //$NON-NLS-1$

}
