package net.arctics.clonk.parser.c4script;

import static net.arctics.clonk.util.ArrayUtil.arrayIterable;
import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.objectsEqual;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.arctics.clonk.Core;
import net.arctics.clonk.parser.c4script.ast.TypeExpectancyMode;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.Utilities;

/**
 * Array type where either general element type or the types for specific elements is known.
 * @author madeen
 *
 */
public class ArrayType implements IType {

	public static final int NO_PRESUMED_LENGTH = -1;
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	private IType generalElementType;
	private int presumedLength;
	private final Map<Integer, IType> elementTypeMapping;
	
	/**
	 * Construct a new ArrayType.
	 * @param elmType The element type. When providing specific element types, this parameter should be null
	 * @param knownTypesForSpecificElements Specific types for elements. The index of the type in this array corresponds to the index in the array instances of this type.
	 */
	public ArrayType(IType elmType, IType... knownTypesForSpecificElements) {
		this(elmType, knownTypesForSpecificElements != null ? knownTypesForSpecificElements.length : 0);
		int i = 0;
		if (knownTypesForSpecificElements != null)
			for (IType t : knownTypesForSpecificElements)
				elementTypeMapping.put(i++, t);
	}
	
	public ArrayType(IType generalElementType, int presumedLength) {
		this.generalElementType = generalElementType;
		this.presumedLength = presumedLength;
		elementTypeMapping = new HashMap<Integer, IType>();
	}
	
	public ArrayType(IType generalElementType, int presumedLength, Map<Integer, IType> typeMapping) {
		this.generalElementType = generalElementType;
		this.presumedLength = presumedLength;
		this.elementTypeMapping = new HashMap<Integer, IType>(typeMapping);
	}

	/**
	 * Get the general element type. If the general element type is not set a type set consisting of the specific element types will be returned.
	 * @return
	 */
	public IType generalElementType() {
		return generalElementType != null ? generalElementType : TypeSet.create(elementTypeMapping.values().toArray(new IType[elementTypeMapping.size()]));
	}
	
	/**
	 * Return element index -> type map
	 * @return
	 */
	public Map<Integer, IType> elementTypeMapping() {
		return elementTypeMapping;
	}
	
	public int presumedLength() {
		return presumedLength;
	}
	
	@Override
	public Iterator<IType> iterator() {
		return arrayIterable(PrimitiveType.ARRAY, this).iterator();
	}

	@Override
	public boolean canBeAssignedFrom(IType other) {
		if (other == PrimitiveType.ARRAY)
			return true;
		else if (other instanceof ArrayType) {
			ArrayType otherArrayType = (ArrayType) other;
			for (Map.Entry<Integer, IType> elmType : elementTypeMapping.entrySet()) {
				IType otherElmType = otherArrayType.elementTypeMapping().get(elmType.getKey());
				if (otherElmType != null && !elmType.getValue().canBeAssignedFrom(otherElmType))
					return false;
			}
			return true;
		} else
			return false;
	}

	/**
	 * The type name of an array type will either describe the type in terms of its general element type or the specific element types.
	 */
	@Override
	public String typeName(final boolean special) {
		if (elementTypeMapping.size() > 0) {
			StringBuilder builder = new StringBuilder();
			builder.append('[');
			
			List<Integer> sorted = ArrayUtil.asSortedList(elementTypeMapping.keySet());
			sorted.add(sorted.get(sorted.size()-1)+2);
			int old = -1, min = -1;
			IType t = null;
			boolean hadCluster = false;
			for (Integer i : sorted) {
				IType it = elementTypeMapping.get(i);
				if (old == -1) {
					old = min = i;
					t = it;
				} else if (i != old+1 || !t.typeName(false).equals(it.typeName(false))) {
					if (hadCluster)
						builder.append(", ");
					builder.append(min);
					if (min != old) {
						builder.append('-');
						builder.append(old);
					}
					builder.append(": ");
					builder.append(t.typeName(false));
					hadCluster = true;
					old = min = i;
					t = it;
				} else {
					old = i;
				}
			}
			if (presumedLength == NO_PRESUMED_LENGTH) {
				if (hadCluster)
					builder.append(", ");
				builder.append("...");
			}
			builder.append(']');
			return builder.toString();
		} else if (generalElementType != null)
			return String.format("%s[%s, ...]", PrimitiveType.ARRAY.typeName(special), generalElementType.typeName(special));
		else
			return PrimitiveType.ARRAY.typeName(special);
	}

	@Override
	public boolean intersects(IType typeSet) {
		return PrimitiveType.ARRAY.intersects(typeSet);
	}

	@Override
	public boolean subsetOf(IType type) {
		type = NillableType.unwrap(type);
		if (type == PrimitiveType.ARRAY)
			return true;
		else if (type instanceof ArrayType) {
			ArrayType other = (ArrayType)type;
			for (Map.Entry<Integer, IType> e : elementTypeMapping.entrySet()) {
				IType o = other.elementTypeMapping.get(e.getKey());
				if (o != null && !e.getValue().subsetOf(o))
					return false;
			}
			if (generalElementType != null && other.generalElementType != null)
				if (!generalElementType.subsetOf(other.generalElementType))
					return false;
			return true;
		} else
			return false;
	}
	
	@Override
	public IType eat(IType other) {
		ArrayType at = as(other, ArrayType.class);
		if (at != null) {
			ArrayType result = this;
			if (at.generalElementType != null && !objectsEqual(this.generalElementType, at.generalElementType)) {
				result = new ArrayType(TypeSet.create(this.generalElementType,
					at.generalElementType), presumedLength, this.elementTypeMapping);
			}
			for (Map.Entry<Integer, IType> e : at.elementTypeMapping.entrySet()) {
				IType my = this.elementTypeMapping.get(e.getKey());
				if (!objectsEqual(my, e.getValue())) {
					if (result == this)
						result = new ArrayType(this.generalElementType, presumedLength, this.elementTypeMapping);
					result.elementTypeMapping.put(e.getKey(), TypeSet.create(my, e.getValue()));
				}
			}
			return result;
		}
		else
			return this;
	}

	@Override
	public int specificness() {
		int s = PrimitiveType.ARRAY.specificness()+1;
		if (generalElementType != null)
			s += generalElementType.specificness();
		for (IType t : elementTypeMapping.values())
			s += t.specificness();
		return s;
	}

	@Override
	public IType staticType() {
		return PrimitiveType.ARRAY;
	}

	@Override
	public boolean subsetOfAny(IType... types) {
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
			if (!Utilities.objectsEqual(this.generalElementType, otherArrType.generalElementType))
				return false;
			return otherArrType.elementTypeMapping.equals(elementTypeMapping);
		} else
			return false;
	}

	/**
	 * Return element type for an evaluated index expression. This will be either the general element type if the evaluation isn't a number or no type information is available for the respective element.
	 * Otherwise the corresponding specific element type will be returned.
	 * @param evaluateIndexExpression Evaluated index expression. Can be anything.
	 * @return The element type
	 */
	public IType typeForElementWithIndex(Object evaluateIndexExpression) {
		IType t = null;
		if (evaluateIndexExpression instanceof Number)
			t = elementTypeMapping.get(((Number)evaluateIndexExpression).intValue());;
		if (t == null)
			t = generalElementType();
		if (t == null)
			t = PrimitiveType.ANY;
		return t;
	}
	
	protected void elementTypeHint(int elementIndex, IType type, TypeExpectancyMode mode) {
		if (elementIndex < 0)
			generalElementType = TypeSet.create(generalElementType, type);
		else {
			IType known = elementTypeMapping.get(elementIndex);
			if (known != null)
				elementTypeMapping.put(elementIndex, TypeSet.create(known, type));
			else
				elementTypeMapping.put(elementIndex, type);
		}
	}

	/**
	 * Return a type for a slice of an array of this type
	 * @param loEv Lower boundary of slice
	 * @param hiEv Upper boundary of slice
	 * @return A type representing slices of the specified range.
	 */
	public IType typeForSlice(Object loEv, Object hiEv) {
		
		int lo, hi;
		if (loEv == null)
			lo = 0;
		else if (loEv instanceof Number)
			lo = ((Number)loEv).intValue();
		else
			return PrimitiveType.ARRAY;
		if (hiEv == null)
			hi = presumedLength;
		else if (hiEv instanceof Number)
			hi = ((Number)hiEv).intValue();
		else
			return PrimitiveType.ARRAY;
		
		if (elementTypeMapping.size() > 0) {
			ArrayType sliceType = new ArrayType(generalElementType, presumedLength);
			for (int i = lo; i < hi; i++) {
				IType t = this.elementTypeMapping.get(i);
				if (t != null)
					sliceType.elementTypeMapping.put(i-lo, t);
			}
			return sliceType;
		} else
			return new ArrayType(generalElementType, this.presumedLength-(hi-lo));
	}
	
	/**
	 * Return a type representing this type after a slice assignment.
	 * @param loEv Slice lower bound
	 * @param hiEv Slice upper bound
	 * @param sliceType Type of right side of assignment
	 * @return The after-slice type
	 */
	public IType modifiedBySliceAssignment(Object loEv, Object hiEv, IType sliceType) {
		
		ArrayType sat = as(sliceType, ArrayType.class);
		if (sat == null)
			return PrimitiveType.ARRAY;
		
		if (sat.presumedLength() == NO_PRESUMED_LENGTH)
			return new ArrayType(TypeSet.create(this.generalElementType(), sat.generalElementType()), NO_PRESUMED_LENGTH);
		
		int lo, hi;
		if (loEv == null)
			lo = 0;
		else if (loEv instanceof Number)
			lo = ((Number)loEv).intValue();
		else
			return PrimitiveType.ARRAY;
		if (hiEv == null)
			hi = presumedLength;
		else if (hiEv instanceof Number)
			hi = ((Number)hiEv).intValue();
		else
			return PrimitiveType.ARRAY;
		
		if (elementTypeMapping.size() > 0 || sat.elementTypeMapping().size() > 0) {
			ArrayType result = new ArrayType(generalElementType, presumedLength);
			for (Map.Entry<Integer, IType> t : this.elementTypeMapping.entrySet()) {
				if (t.getKey() < lo)
					result.elementTypeMapping.put(t.getKey(), t.getValue());
				else if (t.getKey() >= hi)
					result.elementTypeMapping.put(t.getKey()-(hi-lo)+sat.presumedLength(), t.getValue());
			}
			if (sat.elementTypeMapping().size() > 0) {
				for (Map.Entry<Integer, IType> t : sat.elementTypeMapping().entrySet())
					result.elementTypeMapping.put(t.getKey()+lo, t.getValue());
			} else {
				for (int i = lo; i < hi; i++)
					result.elementTypeMapping.put(i, sat.generalElementType());
			}
			return result;
		} else
			return new ArrayType(generalElementType,
				this.presumedLength == NO_PRESUMED_LENGTH ? NO_PRESUMED_LENGTH : this.presumedLength-(hi-lo)+sat.presumedLength());
	}
	
	@Override
	public void setTypeDescription(String description) {}

	/**
	 * Return a type equivalent to this one, except {@link #presumedLength()} is set to {@link #NO_PRESUMED_LENGTH}
	 * @return The type.
	 */
	public IType unknownLength() {
		//return new ArrayType(generalElementType, NO_PRESUMED_LENGTH, elementTypeMapping);
		this.presumedLength = NO_PRESUMED_LENGTH;
		return this;
	}
	
	public static IType elementTypeSet(IType arrayTypes) {
		List<IType> elementTypes = null;
		for (IType t : arrayTypes) {
			ArrayType at = as(t, ArrayType.class);
			if (at != null) {
				if (elementTypes == null)
					elementTypes = new ArrayList<IType>();
				elementTypes.add(at.generalElementType());
			}
		}
		return elementTypes != null ? TypeSet.create(elementTypes) : null;
	}

}
