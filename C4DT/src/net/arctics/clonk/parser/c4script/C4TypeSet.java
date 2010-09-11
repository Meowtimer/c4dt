package net.arctics.clonk.parser.c4script;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.C4Object;
import net.arctics.clonk.util.Utilities;

public class C4TypeSet implements IType {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	private static List<C4TypeSet> typeSets = new LinkedList<C4TypeSet>();
	
	public static final IType STRING_OR_OBJECT = create(C4Type.STRING, C4Type.OBJECT);
	public static final IType ARRAY_OR_STRING = create(C4Type.ARRAY, C4Type.STRING);
	public static final IType REFERENCE_OR_ANY_OR_UNKNOWN = create(C4Type.REFERENCE, C4Type.ANY, C4Type.UNKNOWN);
	public static final IType OBJECT_OR_ID = create(C4Type.OBJECT, C4Type.ID);
	
	private Set<IType> types;
	private boolean internalized;
	
	private C4TypeSet(Set<IType> types) {
		this.types = types;
	}
	
	public C4TypeSet(IType... types) {
		this.types = new HashSet<IType>(types.length);
		for (IType t : types)
			this.types.add(t);
	}

	public IType internalize() {
		return create(this);
	}
	
	private static final Comparator<IType> SPECIFICNESS_COMPARATOR = new Comparator<IType>() {
		@Override
		public int compare(IType o1, IType o2) {
			return o2.specificness()-o1.specificness();
		}
	};
	
	public static IType create(IType... ingredients) {
		
		// remove null elements most tediously
		ingredients = Utilities.removeNullElements(ingredients, IType.class);
		int actualCount = ingredients.length;
		
		// remove less specific types that are already contained in more specific ones
		Arrays.sort(ingredients, SPECIFICNESS_COMPARATOR);
		for (int i = 0; i < actualCount; i++) {
			IType t = ingredients[i];
			for (int j = actualCount-1; j > i; j--) {
				if (t.containsType(ingredients[j]))
					actualCount--;
			}
		}
		
		// expand left-over types into set
		Set<IType> set = new HashSet<IType>();
		boolean containsNonStatics = false;
		for (IType s : ingredients) {
			if (s.expandSubtypes()) {
				for (IType t : s) {
					containsNonStatics = containsNonStatics || t.staticType() != t;
					set.add(t);
				}
			} else {
				set.add(s);
			}
		}
		if (containsNonStatics)
			return actualCount == 1 ? ingredients[0] : new C4TypeSet(set);
		return createInternal(set, actualCount, ingredients);
	}

	private static IType createInternal(Set<IType> set, int actualCount, IType... ingredients) {
		if (set.size() == 0)
			return C4Type.UNKNOWN;
		if (set.size() == 1)
			return set.iterator().next();
		if (set.contains(C4Type.ANY))
			return C4Type.ANY;
		for (C4TypeSet r : typeSets) {
			if (r.types.equals(set))
				return r;
		}
		C4TypeSet n = ingredients != null && actualCount == 1 && ingredients[0] instanceof C4TypeSet
			? (C4TypeSet)ingredients[0]
			: new C4TypeSet(set);
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
		StringBuilder builder = new StringBuilder(20);
		builder.append("<");
		boolean started = true;
		for (IType t : this) {
			if (started)
				started = false;
			else
				builder.append(" or ");
			builder.append(t.typeName(special));
		}
		builder.append(">");
		return builder.toString();
	}
	
	@Override
	public String toString() {
		return typeName(false);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof C4TypeSet) {
			return types.equals(((C4TypeSet)obj).types);
		}
		else
			return false;
	}

	@Override
	public boolean subsetOfType(IType typeSet) {
		for (IType t : this) {
			if (!typeSet.containsType(t))
				return false;
		}
		return true;
	}

	@Override
	public boolean containsType(IType type) {
		for (IType t : this)
			if (t == type)
				return true;
		return false;
	}
	
	@Override
	public boolean containsAnyTypeOf(IType... types) {
		return IType.Default.containsAnyTypeOf(this, types);
	}
	
	@Override
	public int specificness() {
		int s = 0, c = 0;
		for (IType t : this) {
			c++;
			s += t.specificness();
		}
		return c == 0 ? 0 : s/c;
	}

	@Override
	public IType staticType() {
		return internalized ? this : C4Type.ANY;
	}

	public static C4Object objectIngredient(IType type) {
		for (IType t : type) {
			if (t instanceof C4Object)
				return (C4Object) t; // return the first one found
		}
		return null;
	}

	public static IType staticIngredients(IType type) {
		Set<IType> s = new HashSet<IType>();
		boolean allStatics = true;
		for (IType t : type) {
			IType st = t.staticType();
			allStatics = allStatics && st == t;
			s.add(st);
		}
		return createInternal(s, 1, allStatics ? new IType[]{type} : (IType[])null);
	}

	@Override
	public boolean expandSubtypes() {
		return true;
	}

}
