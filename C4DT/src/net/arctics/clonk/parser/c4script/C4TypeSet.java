package net.arctics.clonk.parser.c4script;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class C4TypeSet implements ITypeSet {

	private static final long serialVersionUID = 1L;

	private static List<C4TypeSet> typeSets = new LinkedList<C4TypeSet>();
	
	public static final ITypeSet STRING_OR_OBJECT = create(C4Type.STRING, C4Type.OBJECT);
	public static final ITypeSet ARRAY_OR_STRING = create(C4Type.ARRAY, C4Type.STRING);
	public static final ITypeSet REFERENCE_OR_ANY_OR_UNKNOWN = create(C4Type.REFERENCE, C4Type.ANY, C4Type.UNKNOWN);
	public static final ITypeSet OBJECT_OR_ID = create(C4Type.OBJECT, C4Type.ID);
	
	private Set<C4Type> types;
	
	private C4TypeSet(Set<C4Type> types) {
		this.types = types;
	}
	
	public ITypeSet internalize() {
		return create(this);
	}
	
	public static ITypeSet create(ITypeSet... ingredients) {
		Set<C4Type> set = new HashSet<C4Type>();
		for (ITypeSet s : ingredients) {
			for (C4Type t : s) {
				set.add(t);
			}
		}
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
		C4TypeSet n = ingredients.length == 1 && ingredients[0] instanceof C4TypeSet
			? (C4TypeSet)ingredients[0]
			: new C4TypeSet(set);
		typeSets.add(n);
		return n;
	}
	
	@Override
	public Iterator<C4Type> iterator() {
		return types.iterator();
	}

	@Override
	public boolean canBeAssignedFrom(ITypeSet other) {
		for (C4Type t : this) {
			if (t.canBeAssignedFrom(other))
				return true;
		}
		return false;
	}

	@Override
	public String toString(boolean special) {
		StringBuilder builder = new StringBuilder(20);
		builder.append("<");
		boolean started = true;
		for (C4Type t : this) {
			if (started)
				started = false;
			else
				builder.append(" or ");
			builder.append(t.toString(special));
		}
		builder.append(">");
		return builder.toString();
	}
	
	@Override
	public String toString() {
		return toString(false);
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
	public boolean subsetOf(ITypeSet typeSet) {
		for (C4Type t : this) {
			if (!typeSet.contains(t))
				return false;
		}
		return true;
	}

	@Override
	public boolean contains(C4Type type) {
		for (C4Type t : this)
			if (t == type)
				return true;
		return false;
	}
	
	@Override
	public int specificness() {
		int s = 0, c = 0;
		for (C4Type t : this) {
			c++;
			s += t.specificness();
		}
		return c == 0 ? 0 : s/c;
	}

}
