package net.arctics.clonk.c4script.typing;

import static net.arctics.clonk.util.ArrayUtil.iterable;
import static net.arctics.clonk.util.ArrayUtil.map;

import java.util.Iterator;

import net.arctics.clonk.Core;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.IType;
import net.arctics.clonk.c4script.PrimitiveType;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.util.IConverter;
import net.arctics.clonk.util.StringUtil;

public class FunctionType implements IType {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	private final Function prototype;
	
	public Function prototype() {
		return prototype;
	}
	
	public FunctionType(Function function) {
		this.prototype = function;
	}

	@Override
	public Iterator<IType> iterator() {
		return iterable(this, PrimitiveType.FUNCTION).iterator();
	}

	@Override
	public String typeName(boolean special) {
		if (!special)
			return PrimitiveType.FUNCTION.typeName(false);
		final StringBuilder builder = new StringBuilder();
		builder.append(PrimitiveType.FUNCTION.typeName(false));
		StringUtil.writeBlock(builder, "(", ")", ", ", map(this.prototype.parameters(), new IConverter<Variable, String>() {
			@Override
			public String convert(Variable from) {
				return from.type().typeName(false);
			}
		}));
		return builder.toString();
	}

	@Override
	public IType simpleType() {
		return PrimitiveType.FUNCTION;
	}

}
