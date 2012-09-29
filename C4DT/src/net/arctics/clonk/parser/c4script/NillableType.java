package net.arctics.clonk.parser.c4script;

import static net.arctics.clonk.util.ArrayUtil.iterable;

import java.util.Iterator;

import net.arctics.clonk.Core;

public class NillableType implements IType {
	
	public static IType unwrap(IType type) {
		return type instanceof NillableType ? ((NillableType)type).baseType : type;
	}

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	
	private final IType baseType; 
	
	public static IType make(IType baseType) {
		// gnah, nillable catastrophy
		return baseType instanceof PrimitiveType ? new NillableType(baseType) : baseType;
	}
	
	private NillableType(IType baseType) {
		this.baseType = baseType;
	}
	
	@Override
	public Iterator<IType> iterator() {
		return iterable(baseType, PrimitiveType.ANY).iterator(); 
	}

	@Override
	public boolean canBeAssignedFrom(IType other) {
		return PrimitiveType.ANY.canBeAssignedFrom(other);
	}

	@Override
	public String typeName(boolean special) {
		return special ? baseType.typeName(true) + "?" : baseType.typeName(false);
	}

	@Override
	public boolean intersects(IType type) {
		return baseType.intersects(type) || PrimitiveType.ANY.intersects(type);
	}

	@Override
	public boolean subsetOf(IType type) {
		return baseType.subsetOf(type);
	}

	@Override
	public boolean subsetOfAny(IType... types) {
		return baseType.subsetOfAny(types);
	}

	@Override
	public int precision() {
		return baseType.precision();
	}

	@Override
	public IType simpleType() {
		return baseType.simpleType();
	}

	@Override
	public void setTypeDescription(String description) {
		baseType.setTypeDescription(description);
	}
	
	@Override
	public IType eat(IType other) {return this;}

}
