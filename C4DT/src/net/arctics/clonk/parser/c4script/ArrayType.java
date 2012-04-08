package net.arctics.clonk.parser.c4script;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.c4script.ast.TypeExpectancyMode;
import net.arctics.clonk.util.ArrayUtil;

/**
 * Array type where either general element type or the types for specific elements is known.
 * @author madeen
 *
 */
public class ArrayType implements IType {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	private IType generalElementType;
	private final int presumedLength;
	private final Map<Integer, IType> elementTypeMapping;
	
	/**
	 * Construct a new ArrayType.
	 * @param elmType The element type. When providing specific element types, this parameter should be null
	 * @param knownTypesForSpecificElements Specific types for elements. The index of the type in this array corresponds to the index in the array instances of this type.
	 */
	public ArrayType(IType elmType, IType[] knownTypesForSpecificElements) {
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

	/**
	 * Get the general element type. If the general element type is not set a type set consisting of the specific element types will be returned.
	 * @return
	 */
	public IType getGeneralElementType() {
		return generalElementType != null ? generalElementType : TypeSet.create(elementTypeMapping.values().toArray(new IType[elementTypeMapping.size()]));
	}
	
	/**
	 * Return element index -> type map
	 * @return
	 */
	public Map<Integer, IType> getElementTypeMapping() {
		return elementTypeMapping;
	}
	
	public int getPresumedLength() {
		return presumedLength;
	}
	
	@Override
	public Iterator<IType> iterator() {
		return PrimitiveType.ARRAY.iterator();
	}

	@Override
	public boolean canBeAssignedFrom(IType other) {
		if (other == PrimitiveType.ARRAY)
			return true;
		else if (other instanceof ArrayType) {
			ArrayType otherArrayType = (ArrayType) other;
			for (Map.Entry<Integer, IType> elmType : elementTypeMapping.entrySet()) {
				IType otherElmType = otherArrayType.getElementTypeMapping().get(elmType.getKey());
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
			if (otherArrType.generalElementType == null && this.generalElementType != null)
				return false;
			if (this.generalElementType != null && !otherArrType.generalElementType.equals(this.generalElementType))
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
			t = getGeneralElementType();
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

	public ITypeable indexedElementAsTypeable(final Object evaluatedIndexExpression, final Index index) {
		final int concreteIndex = evaluatedIndexExpression instanceof Number ? ((Number)evaluatedIndexExpression).intValue() : -1;
		return new ITypeable() {
			@Override
			public String name() {
				return String.format("Element %s of %s", evaluatedIndexExpression, ArrayType.this);
			}

			@Override
			public void expectedToBeOfType(IType t, TypeExpectancyMode mode) {
				elementTypeHint(concreteIndex, t, mode);
			}

			@Override
			public IType type() {
				return typeForElementWithIndex(evaluatedIndexExpression);
			}

			@Override
			public void forceType(IType type) {
				elementTypeHint(concreteIndex, type, TypeExpectancyMode.Force);
			}

			@Override
			public boolean typeIsInvariant() {
				return false;
			}
			
			@Override
			public Index index() {
				return index;
			}

			@Override
			public boolean matchedBy(Matcher matcher) {
				return matcher.reset(name()).matches();
			}

			@Override
			public String infoText() {
				return toString();
			}
			
		};
	}
	
	@Override
	public void setTypeDescription(String description) {}

}
