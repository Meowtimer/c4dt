package net.arctics.clonk.parser.c4script.ast;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.IResolvableType;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.TypeUtil;

public final class TypeChoice implements IType, IResolvableType {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	
	private final IType left, right;
	
	public IType left() { return left; }
	public IType right() { return right; }
	
	private static final TypeChoice[] HARD_CHOICES = {
		new TypeChoice(PrimitiveType.OBJECT, PrimitiveType.ID),
		new TypeChoice(PrimitiveType.BOOL, PrimitiveType.INT),
		new TypeChoice(PrimitiveType.OBJECT, PrimitiveType.PROPLIST)
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
		return new TypeChoice(left, right);
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
	public IType staticType() {
		IType stLeft = left.staticType();
		IType stRight = right.staticType();
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
	
	public List<IType> flatten() {
		LinkedList<IType> types = new LinkedList<IType>();
		collect(types);
		return types;
	}
	
	@Override
	public String toString() {
		return typeName(false);
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
		return rl == left && rr == right ? this : new TypeChoice(rl, rr);
	}

}
