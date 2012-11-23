package net.arctics.clonk.parser.c4script.ast;

import static net.arctics.clonk.util.ArrayUtil.iterable;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.TypeSet;

public final class TypeChoice implements IType {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	
	private final IType left, right;
	
	public IType left() { return left; }
	public IType right() { return right; }
	
	private static final TypeChoice[] HARD_CHOICES = {
		new TypeChoice(PrimitiveType.OBJECT, PrimitiveType.ID),
	};
	
	public static TypeChoice make(IType left, IType right) {
		for (TypeChoice hc : HARD_CHOICES)
			if (
				(hc.left == left && hc.right == right) ||
				(hc.left == right && hc.right == left)
			)
				return hc;
		return new TypeChoice(left, right);
	}
	
	private TypeChoice(IType left, IType right) {
		this.left = left;
		this.right = right;
	}
	
	@Override
	public Iterator<IType> iterator() {
		return iterable(left, right).iterator();
	}

	@Override
	public boolean canBeAssignedFrom(IType other) {
		return left.canBeAssignedFrom(other) || right.canBeAssignedFrom(right);
	}

	@Override
	public String typeName(boolean special) {
		return String.format("%s | %s", left.typeName(special), right.typeName(special));
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
	
	public IType flatten() {
		if (!(left instanceof TypeChoice || right instanceof TypeChoice))
			return this;
		LinkedList<IType> types = new LinkedList<IType>();
		collect(types);
		return
			types.size() == 2 ? new TypeChoice(types.get(0), types.get(1)) :
			types.size() == 1 ? types.get(0) :
			new TypeSet(types.toArray(new IType[types.size()]));
	}
	
	@Override
	public String toString() {
		return typeName(false);
	}

}
