package net.arctics.clonk.parser.c4script;

import static net.arctics.clonk.util.ArrayUtil.arrayIterable;
import static net.arctics.clonk.util.ArrayUtil.map;

import java.util.Iterator;

import net.arctics.clonk.Core;
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
		return arrayIterable(this, PrimitiveType.FUNCTION).iterator();
	}

	@Override
	public boolean canBeAssignedFrom(IType other) {
		return other == PrimitiveType.FUNCTION || other instanceof FunctionType;
	}

	@Override
	public String typeName(boolean special) {
		StringBuilder builder = new StringBuilder();
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
	public boolean intersects(IType typeSet) {
		return PrimitiveType.FUNCTION.intersects(typeSet);
	}

	@Override
	public boolean subsetOf(IType type) {
		return type == PrimitiveType.FUNCTION;
	}
	
	@Override
	public IType eat(IType other) {return this;}

	@Override
	public boolean subsetOfAny(IType... types) {
		return IType.Default.containsAnyTypeOf(this, types);
	}

	@Override
	public int specificness() {
		return PrimitiveType.FUNCTION.specificness()+1;
	}

	@Override
	public IType staticType() {
		return PrimitiveType.FUNCTION;
	}

	@Override
	public void setTypeDescription(String description) {
	}

}
