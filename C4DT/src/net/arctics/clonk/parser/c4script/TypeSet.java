package net.arctics.clonk.parser.c4script;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.ISerializationResolvable;
import net.arctics.clonk.index.Index;

/**
 * Type that represents a set of multiple possible types.
 * @author madeen
 *
 */
public class TypeSet implements IType, ISerializationResolvable, IResolvableType {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	private static List<TypeSet> typeSets = new LinkedList<TypeSet>();
	
	public static final IType STRING_OR_OBJECT = create(PrimitiveType.STRING, PrimitiveType.OBJECT);
	public static final IType ARRAY_OR_STRING = create(PrimitiveType.ARRAY, PrimitiveType.STRING);
	public static final IType REFERENCE_OR_ANY_OR_UNKNOWN = create(PrimitiveType.REFERENCE, PrimitiveType.ANY, PrimitiveType.UNKNOWN);
	public static final IType OBJECT_OR_ID = create(PrimitiveType.OBJECT, PrimitiveType.ID);
	
	private Set<IType> types;
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
	
	private TypeSet(Set<IType> types) {
		this.types = types;
	}
	
	public TypeSet(IType... types) {
		this.types = new HashSet<IType>(types.length);
		for (IType t : types)
			this.types.add(t);
	}

	@Override
	public IType resolve(Index index) {
		return this;
	}
	
	private static final Comparator<IType> SPECIFICNESS_COMPARATOR = new Comparator<IType>() {
		@Override
		public int compare(IType o1, IType o2) {
			return o2.specificness()-o1.specificness();
		}
	};
	
	public final int size() {
		return types.size();
	}
	
	public Set<IType> types() {
		return this.types;
	}
	
	private static IType[] flatten(IType[] types) {
		int newCount = types.length;
		for (IType t : types) {
			if (t == null)
				newCount--;
			else if (t instanceof TypeSet) {
				newCount += ((TypeSet)t).size() - 1;
			}
		}
		IType[] newArray = newCount == types.length ? types : new IType[newCount];
		int i = 0;
		for (IType t : types) {
			if (t == null)
				continue;
			else if (t instanceof TypeSet) {
				for (IType t2 : ((TypeSet)t)) {
					newArray[i++] = t2;
				}
			} else
				newArray[i++] = t;
		}
		return newArray;
	}
	
	public static IType create(Collection<IType> types) {
		return create(types.toArray(new IType[types.size()]));
	}
	
	public static IType create(IType... ingredients) {
		
		// remove null elements most tediously
		ingredients = flatten(ingredients);
		int actualCount = ingredients.length;
		
		// remove less specific types that are already contained in more specific ones
		Arrays.sort(ingredients, SPECIFICNESS_COMPARATOR);
		for (int i = 0; i < actualCount; i++) {
			IType t = ingredients[i];
			for (int j = actualCount-1; j > i; j--) {
				IType other = ingredients[j];
				if (other.equals(t) || (t.specificness() > other.specificness() && t.containsType(other))) {
					for (int z = actualCount-1; z > j; z--)
						ingredients[z-1] = ingredients[z];
					actualCount--;
				}
			}
		}
		
		// expand left-over types into set
		Set<IType> set = new HashSet<IType>();
		boolean containsNonStatics = false;
		for (int i = 0; i < actualCount; i++) {
			IType s = ingredients[i];
			if (s instanceof TypeSet) {
				for (IType t : s) {
					containsNonStatics = containsNonStatics || t.staticType() != t;
					set.add(t);
				}
			}
			else {
				containsNonStatics = containsNonStatics || s.staticType() != s;
				set.add(s);
			}
		}
		if (set.size() > 1)
			if (set.remove(PrimitiveType.UNKNOWN))
				set.add(PrimitiveType.ANY);
		if (containsNonStatics)
			return set.size() == 1 ? set.iterator().next() : new TypeSet(set);
		return createInternal(set, actualCount, ingredients);
	}

	private static IType createInternal(Set<IType> set, int actualCount, IType... ingredients) {
		if (set.size() == 0)
			return PrimitiveType.UNKNOWN;
		if (set.size() == 1)
			return set.iterator().next();
		/*if (set.contains(C4Type.ANY))
			return C4Type.ANY; */
		for (TypeSet r : typeSets) {
			if (r.types.equals(set))
				return r;
		}
		TypeSet n = ingredients != null && actualCount == 1 && ingredients[0] instanceof TypeSet
			? (TypeSet)ingredients[0]
			: new TypeSet(set);
		n.internalized = true;
		typeSets.add(n);
		return n;
	}
	
	@Override
	public Iterator<IType> iterator() {
		return types.iterator();
	}

	@Override
	public boolean canBeAssignedFrom(IType other) {
		if (other == PrimitiveType.ANY || other == PrimitiveType.UNKNOWN)
			return true;
		if (equals(other))
			return true;
		for (IType t : this) {
			if (t.canBeAssignedFrom(other))
				return true;
		}
		return false;
	}

	@Override
	public String typeName(boolean special) {
		StringBuilder builder = new StringBuilder((description != null ? description.length() : 0) + 20);
		builder.append(IType.COMPLEX_TYPE_START);
		if (description != null) {
			builder.append(description);
			builder.append(": ");
		}
		Set<String> typeNames = new HashSet<String>();
		boolean containsAny = false;
		for (IType t : this) {
			if (t == PrimitiveType.ANY)
				containsAny = true;
			else
				typeNames.add(t.typeName(special));
		}
		
		if (typeNames.size() == 1 && containsAny)
			return typeNames.iterator().next() + "?";
		else if (typeNames.size() == 1)
			return typeNames.iterator().next();
		boolean started = true;
		for (String tn : typeNames) {
			if (started)
				started = false;
			else
				builder.append(Messages.C4TypeSet_Or);
			builder.append(tn);
		}
		builder.append(IType.COMPLEX_TYPE_END);
		if (containsAny)
			builder.append('?');
		if (builder.length() > 200)
			return description != null ? IType.COMPLEX_TYPE_START + description + IType.COMPLEX_TYPE_END : IType.COMPLEX_TYPE_ABBREVIATED;
		else
			return builder.toString();
	}
	
	@Override
	public String toString() {
		return typeName(false);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof TypeSet) {
			return types.equals(((TypeSet)obj).types);
		}
		else
			return false;
	}

	@Override
	public boolean intersects(IType typeSet) {
		for (IType t : this)
			if (t.intersects(typeSet))
				return true;
		return false;
	}

	@Override
	public boolean containsType(IType type) {
		for (IType t : this)
			if (t.containsType(type))
				return true;
		return false;
	}
	
	@Override
	public boolean containsAnyTypeOf(IType... types) {
		return IType.Default.containsAnyTypeOf(this, types);
	}
	
	@Override
	public int specificness() {
		int r = 0;
		for (IType t : this)
			r = Math.max(r, t.specificness());
		return r;
	}

	@Override
	public IType staticType() {
		return internalized ? this : PrimitiveType.ANY;
	}

	public static Definition objectIngredient(IType type) {
		for (IType t : type) {
			if (t instanceof Definition)
				return (Definition) t; // return the first one found
		}
		return null;
	}

	@Override
	public IType resolve(DeclarationObtainmentContext context, IType callerType) {
		IType[] resolvedTypes = new IType[this.size()];
		boolean didResolveSomething = false;
		int i = 0;
		for (IType t : this) {
			IType resolved = IResolvableType._.resolve(t, context, callerType);
			if (resolved != t)
				didResolveSomething = true;
			resolvedTypes[i++] = resolved;
		}
		return didResolveSomething ? create(resolvedTypes) : this;
	}

}
