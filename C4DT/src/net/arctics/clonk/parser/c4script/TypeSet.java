package net.arctics.clonk.parser.c4script;

import static net.arctics.clonk.util.ArrayUtil.iterable;
import static net.arctics.clonk.util.ArrayUtil.map;

import java.util.Arrays;
import java.util.Iterator;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.ISerializationResolvable;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.c4script.ast.TypeUnification;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.IConverter;
import net.arctics.clonk.util.StringUtil;

/**
 * Type that represents a set of multiple possible types.
 * @author madeen
 *
 */
public class TypeSet implements IType, ISerializationResolvable, IResolvableType {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	
	private final IType[] types;
	private boolean internalized;
	private String description;
	
	/**
	 * Get description. See {@link #setTypeDescription(String)}
	 * @return The description.
	 */
	public String getTypeDescription() {
		return description;
	}
	
	/**
	 * Description of the type set. Will be incorporated into {@link #typeName(boolean)} if set. Internalized type sets ({@link #STRING_OR_OBJECT} or similar) will ignore attempts to set a description.
	 * @param description The description explaining how this typeset was constructed
	 */
	@Override
	public void setTypeDescription(String description) {
		if (!internalized)
			this.description = description;
	}
	
	public TypeSet(IType... types) {
		this.types = types;
	}

	@Override
	public IType resolve(Index index) {
		return this;
	}
	
	public final int size() {
		return types.length;
	}
	
	public IType[] types() {
		return Arrays.copyOf(types, types.length);
	}
	
	public boolean containsType(IType type) {
		for (IType t : types)
			if (t.equals(type))
				return true;
		return false;
	}
	
	@Override
	public Iterator<IType> iterator() {
		return iterable(types).iterator();
	}

	@Override
	public boolean canBeAssignedFrom(IType other) {
		if (other == PrimitiveType.ANY || other == PrimitiveType.UNKNOWN)
			return true;
		if (equals(other))
			return true;
		for (IType t : this)
			if (t.canBeAssignedFrom(other))
				return true;
		return false;
	}

	public PrimitiveType primitiveType() {
		PrimitiveType result = null;
		for (IType t : this) {
			IType s = t.simpleType();
			if (s instanceof PrimitiveType) {
				if (result == null)
					result = (PrimitiveType) s;
				else if (result != s)
					return PrimitiveType.ANY;
			} else
				return PrimitiveType.ANY;
		}
		return result;
	}
	
	@Override
	public String typeName(final boolean special) {
		return StringUtil.blockString("", "", " | ", map(iterable(types), new IConverter<IType, String>() {
			@Override
			public String convert(IType from) {
				return from.typeName(special);
			}
		}));
	}
	
	@Override
	public String toString() {
		return typeName(false);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof TypeSet)
			return ArrayUtil.elementsEqual(types, ((TypeSet)obj).types);
		else
			return false;
	}
	
	@Override
	public int precision() {
		int r = 0;
		for (IType t : this)
			r = Math.max(r, t.precision());
		return r;
	}

	@Override
	public IType simpleType() {
		return internalized ? this : PrimitiveType.ANY;
	}

	public static Definition definition(IType type) {
		for (IType t : type)
			if (t instanceof Definition)
				return (Definition) t; // return the first one found
		return null;
	}

	@Override
	public IType resolve(DeclarationObtainmentContext context, IType callerType) {
		IType[] resolvedTypes = new IType[this.size()];
		boolean didResolveSomething = false;
		int i = 0;
		for (IType t : this) {
			IType resolved = TypeUtil.resolve(t, context, callerType);
			if (resolved != t)
				didResolveSomething = true;
			resolvedTypes[i++] = resolved;
		}
		return didResolveSomething ? TypeUnification.unify(resolvedTypes) : this;
	}

}
