package net.arctics.clonk.parser.c4script;

import java.util.Arrays;
import java.util.Iterator;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.StringUtil;

/**
 * Array type where either general element type or the types for specific elements is known.
 * @author madeen
 *
 */
public class ArrayType implements IType {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	private IType elementType;
	private IType[] knownTypesForSpecificElements;
	
	/**
	 * Construct a new ArrayType.
	 * @param elmType The element type. When providing specific element types, this parameter should be null
	 * @param knownTypesForSpecificElements Specific types for elements. The index of the type in this array corresponds to the index in the array instances of this type.
	 */
	public ArrayType(IType elmType, IType[] knownTypesForSpecificElements) {
		this.elementType = elmType;
		this.knownTypesForSpecificElements = knownTypesForSpecificElements;
	}

	/**
	 * Get the general element type. If the general element type is not set a type set consisting of the specific element types will be returned.
	 * @return
	 */
	public IType getElementType() {
		return elementType != null ? elementType : TypeSet.create(knownTypesForSpecificElements);
	}
	
	/**
	 * Return the specific known element types for arrays of this type.
	 * The index of the type in this array corresponds to the index in the array instances of this type.
	 * @return The types
	 */
	public IType[] getKnownTypesForSpecificElements() {
		return knownTypesForSpecificElements;
	}
	
	@Override
	public Iterator<IType> iterator() {
		return PrimitiveType.ARRAY.iterator();
	}

	@Override
	public boolean canBeAssignedFrom(IType other) {
		return other == PrimitiveType.ARRAY;
	}

	/**
	 * The type name of an array type will either describe the type in terms of its general element type or the specific element types.
	 */
	@Override
	public String typeName(boolean special) {
		if (elementType != null)
			return String.format("<%s of %s>", PrimitiveType.ARRAY.typeName(special), elementType.typeName(special));
		else if (knownTypesForSpecificElements != null)
			return String.format("<%s with types for first elements: %s>", PrimitiveType.ARRAY.typeName(special), StringUtil.writeBlock(null, "[", "]", ", ", ArrayUtil.arrayIterable(knownTypesForSpecificElements)));
		else
			return PrimitiveType.ARRAY.typeName(special);
	}

	@Override
	public boolean intersects(IType typeSet) {
		return PrimitiveType.ARRAY.intersects(typeSet);
	}

	@Override
	public boolean containsType(IType type) {
		return type == PrimitiveType.ARRAY;
	}

	@Override
	public int specificness() {
		return PrimitiveType.ARRAY.specificness()+1;
	}

	@Override
	public IType staticType() {
		return PrimitiveType.ARRAY;
	}

	@Override
	public boolean containsAnyTypeOf(IType... types) {
		return IType.Default.containsAnyTypeOf(this, types);
	}
	
	@Override
	public String toString() {
		return typeName(false);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ArrayType) {
			ArrayType otherArrType = (ArrayType) obj;
			if (otherArrType.elementType == null && this.elementType != null)
				return false;
			if (this.elementType != null && !otherArrType.elementType.equals(this.elementType))
				return false;
			return Arrays.deepEquals(otherArrType.knownTypesForSpecificElements, this.knownTypesForSpecificElements);
		} else
			return false;
	}

	/**
	 * Return element type for an evaluated index expression. This will be either the general element type if the evaluation isn't a number or the index is out of range, or if no specific element types are known.
	 * Otherwise the corresponding specific element type will be returned.
	 * @param evaluateIndexExpression Evaluated index expression. Can be anything.
	 * @return The element type
	 */
	public IType getTypeForElementWithIndex(Object evaluateIndexExpression) {
		if (evaluateIndexExpression instanceof Number) {
			int index = ((Number)evaluateIndexExpression).intValue();
			if (knownTypesForSpecificElements != null && index >= 0 && index < knownTypesForSpecificElements.length && knownTypesForSpecificElements[index] != null)
				return knownTypesForSpecificElements[index];
		}
		return elementType != null ? elementType : PrimitiveType.UNKNOWN;
	}

}
