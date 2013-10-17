package net.arctics.clonk.c4script.typing;

import static net.arctics.clonk.util.Utilities.eq;
import net.arctics.clonk.Core;
import net.arctics.clonk.index.Definition;

public class Maybe extends TypeChoice {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	public Maybe(IType type) { super(type, PrimitiveType.ANY); }
	public IType maybe() { return left; }
	public static IType make(IType type) {
		if (type instanceof IRefinedPrimitiveType && !(type instanceof Definition))
			return type;
		if (eq(type, PrimitiveType.UNKNOWN))
			return PrimitiveType.ANY;
		if (type instanceof Maybe)
			return type;
		return new Maybe(type);
	}
}
