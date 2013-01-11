package net.arctics.clonk.parser.c4script.ast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.IResolvableType;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.TypeUtil;
import net.arctics.clonk.util.IPredicate;
import net.arctics.clonk.util.StringUtil;

public final class TypeChoice implements IType, IResolvableType {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	
	private final IType left, right;
	
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
		for (TypeChoice hc : HARD_CHOICES)
			if (
				(hc.left == left && hc.right == right) ||
				(hc.left == right && hc.right == left)
			)
				return hc;
		final TypeChoice r = new TypeChoice(left, right);
		return remove(r, new IPredicate<IType>() {
			private final List<IType> flattened = r.flatten();
			private final boolean[] mask = new boolean[flattened.size()];
			@Override
			public boolean test(IType item) {
				int ndx = flattened.indexOf(item);
				if (ndx < 0)
					return false;
				if (mask[ndx])
					return true;
				mask[ndx] = true;
				return false;
			}
		});
	}
	
	private TypeChoice(IType left, IType right) {
		this.left = left;
		this.right = right;
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
		if (special)
			return String.format("%s | %s", left.typeName(special), right.typeName(special));
		else {
			List<IType> types = new ArrayList<>(10);
			collect(types);
			Set<String> typeNames = new HashSet<>(types.size());
			for (IType t : types)
				typeNames.add(t.typeName(false));
			return StringUtil.blockString("", "", " | ", typeNames);
		}
	}

	@Override
	public int precision() {
		return Math.max(left.precision(), right.precision());
	}

	@Override
	public IType simpleType() {
		IType stLeft = left.simpleType();
		IType stRight = right.simpleType();
		return stLeft == stRight ? stLeft : PrimitiveType.ANY;
	}

	@Override
	public void setTypeDescription(String description) {

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
			TypeChoice c = (TypeChoice)type;
			IType left = remove(c.left(), predicate);
			IType right = remove(c.right(), predicate);
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
		LinkedList<IType> types = new LinkedList<IType>();
		collect(types);
		return types;
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends IType> T contained(IType type, Class<T> containedType) { 
		if (containedType.isInstance(type))
			return (T) type;
		if (type instanceof TypeChoice) {
			TypeChoice choice = (TypeChoice)type;
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
			TypeChoice other = (TypeChoice)obj;
			return
				(other.left.equals(left) && other.right.equals(right)) ||
				(other.right.equals(left) && other.right.equals(left));
		} else
			return false;
	}

	@Override
	public IType resolve(DeclarationObtainmentContext context, IType callerType) {
		IType rl = TypeUtil.resolve(left, context, callerType);
		IType rr = TypeUtil.resolve(right, context, callerType);
		return rl == left && rr == right ? this : TypeChoice.make(rl, rr);
	}

}
