package net.arctics.clonk.parser.c4script.ast;

import static net.arctics.clonk.util.Utilities.foldl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.util.IPredicate;
import net.arctics.clonk.util.StringUtil;
import net.arctics.clonk.util.Utilities.Folder;

public class TypeChoice implements IType {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	protected final IType left, right;

	public IType left() { return left; }
	public IType right() { return right; }

	private static final TypeChoice[] HARD_CHOICES = {
		new TypeChoice(PrimitiveType.OBJECT, PrimitiveType.ID),
		new TypeChoice(PrimitiveType.BOOL, PrimitiveType.INT),
		new TypeChoice(PrimitiveType.OBJECT, PrimitiveType.PROPLIST),
		new TypeChoice(PrimitiveType.ID, PrimitiveType.INT),
		new TypeChoice(PrimitiveType.STRING, PrimitiveType.INT)
	};

	public static IType make(IType left, IType right) {
		if (left == null)
			return right;
		else if (right == null)
			return left;
		else if (left.equals(right))
			return left;
		for (final TypeChoice hc : HARD_CHOICES)
			if (
				(hc.left == left && hc.right == right) ||
				(hc.left == right && hc.right == left)
			)
				return hc;
		return new TypeChoice(left, right).removeDuplicates();
	}
	protected IType removeDuplicates() {
		return remove(this, new IPredicate<IType>() {
			private final List<IType> flattened = flatten();
			private final boolean[] mask = new boolean[flattened.size()];
			@Override
			public boolean test(IType item) {
				final int ndx = flattened.indexOf(item);
				if (ndx < 0)
					return false;
				if (mask[ndx])
					return true;
				mask[ndx] = true;
				return false;
			}
		});
	}

	public static IType make(Collection<? extends IType> types) {
		if (types.size() == 0)
			return null;
		else if (types.size() == 1)
			return types.iterator().next();
		else return foldl(types, new Folder<IType, TypeChoice>() {
			@Override
			public TypeChoice fold(IType interim, IType next, int index) {
				return new TypeChoice(interim, next);
			}

		}).removeDuplicates();
	}

	protected TypeChoice(IType left, IType right) {
		this.left = left != null ? left : PrimitiveType.UNKNOWN;
		this.right = right != null ? right : PrimitiveType.UNKNOWN;
	}

	@Override
	public Iterator<IType> iterator() {
		return flatten().iterator();
	}

	@Override
	public boolean canBeAssignedFrom(IType other) {
		return left.canBeAssignedFrom(other) || right.canBeAssignedFrom(other);
	}

	@Override
	public String typeName(boolean special) {
		final List<IType> types = new ArrayList<>(10);
		collect(types);
		final Set<String> typeNames = new HashSet<>(types.size());
		for (final IType t : types)
			if (t != null)
				typeNames.add(t.typeName(special));
		return StringUtil.blockString("", "", " | ", typeNames);
	}

	@Override
	public IType simpleType() {
		final IType stLeft = left.simpleType();
		final IType stRight = right.simpleType();
		return stLeft == stRight ? stLeft : PrimitiveType.ANY;
	}

	private void collect(Collection<IType> types) {
		if (left instanceof TypeChoice)
			((TypeChoice) left).collect(types);
		else if (!types.contains(left))
			types.add(left);
		if (right instanceof TypeChoice)
			((TypeChoice) right).collect(types);
		else if (!types.contains(right))
			types.add(right);
	}

	public static IType remove(IType type, IPredicate<? super IType> predicate) {
		if (predicate.test(type))
			return null;
		else if (type instanceof TypeChoice) {
			final TypeChoice c = (TypeChoice)type;
			final IType left = remove(c.left(), predicate);
			final IType right = remove(c.right(), predicate);
			if (left == c.left() && right == c.right())
				return c;
			else if (left == null && right == null)
				return null;
			else if (left == null)
				return right;
			else if (right == null)
				return left;
			else
				return TypeChoice.make(left, right);
		}
		else
			return type;
	}

	public List<IType> flatten() {
		final LinkedList<IType> types = new LinkedList<IType>();
		collect(types);
		return types;
	}

	@SuppressWarnings("unchecked")
	public static <T extends IType> T contained(IType type, Class<T> containedType) {
		if (containedType.isInstance(type))
			return (T) type;
		if (type instanceof TypeChoice) {
			final TypeChoice choice = (TypeChoice)type;
			T r = contained(choice.left(), containedType);
			if (r != null)
				return r;
			r = contained(choice.right(), containedType);
			if (r != null)
				return r;
		}
		return null;
	}

	@Override
	public String toString() {
		return typeName(true);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof TypeChoice) {
			final TypeChoice other = (TypeChoice)obj;
			return
				(other.left.equals(left) && other.right.equals(right)) ||
				(other.right.equals(left) && other.right.equals(left));
		} else
			return false;
	}

}
