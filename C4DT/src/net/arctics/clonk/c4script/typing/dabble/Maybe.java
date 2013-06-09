package net.arctics.clonk.c4script.typing.dabble;

import static net.arctics.clonk.util.Utilities.eq;
import net.arctics.clonk.Core;
import net.arctics.clonk.c4script.typing.IType;
import net.arctics.clonk.c4script.typing.PrimitiveType;
import net.arctics.clonk.c4script.typing.TypeChoice;

public class Maybe extends TypeChoice {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	public Maybe(IType type) { super(type, PrimitiveType.ANY); }
	public IType maybe() { return left; }
	public static IType make(IType type) {
		if (eq(type, PrimitiveType.UNKNOWN))
			return PrimitiveType.ANY;
		if (type instanceof Maybe)
			return type;
		return new Maybe(type);
	}
}
