package net.arctics.clonk.parser.c4script.ast;

import static net.arctics.clonk.util.ArrayUtil.pack;
import static net.arctics.clonk.util.Utilities.objectsEqual;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.IHasIncludes;
import net.arctics.clonk.parser.c4script.ArrayType;
import net.arctics.clonk.parser.c4script.IRefinedPrimitiveType;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.NillableType;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.ProplistDeclaration;
import net.arctics.clonk.parser.c4script.ReferenceType;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.WrappedType;

public class TypeUnification {
	private static IType unifyLeft(IType a, IType b) {
		if (a == null)
			return b;
		if (b == null)
			return a;
		if (a.equals(b))
			return a;
		
		if (a instanceof PrimitiveType)
			switch ((PrimitiveType)a) {
			case ARRAY:
			case ID:
			case OBJECT:
			case INT:
			case STRING:
			case BOOL:
				break;
			case PROPLIST:
				if (b instanceof PrimitiveType)
					switch ((PrimitiveType)b) {
					case OBJECT: case ID:
						return a;
					default:
						break;
					}
				else if (b instanceof Definition)
					return b;
				break;
			case UNKNOWN:
				if (b == PrimitiveType.ANY)
					return PrimitiveType.ANY;
				else
					return NillableType.make(b);
			case ANY:
				return NillableType.make(b);
			case REFERENCE:
				return b;
			default:
				break;
			}
		
		if (a instanceof TypeChoice) {
			TypeChoice tca = (TypeChoice)a;
			if (b instanceof TypeChoice) {
				TypeChoice tcb = (TypeChoice)b;
				return unifyLeft(unifyLeft(tca.left(), tcb.right()), unifyLeft(tcb.left(), tcb.right()));
			}
			else {
				IType l = tca.left();
				IType r = tca.right();
				return TypeChoice.make(unifyLeft(l, b), unifyLeft(r, b));
			}
		}
		
		if (a instanceof IRefinedPrimitiveType && b instanceof PrimitiveType &&
			((IRefinedPrimitiveType)a).primitiveType() == b)
			return a;
		
		if (a instanceof ArrayType && b instanceof ArrayType) {
			ArrayType ata = (ArrayType)a;
			ArrayType atb = (ArrayType)b;
			ArrayType result = ata;
			if (atb.generalElementType() != null && !objectsEqual(ata.generalElementType(), atb.generalElementType()))
				result = new ArrayType(unify(ata.generalElementType(),
					atb.generalElementType()), ata.presumedLength(), ata.elementTypeMapping());
			for (Map.Entry<Integer, IType> e : atb.elementTypeMapping().entrySet()) {
				IType my = ata.elementTypeMapping().get(e.getKey());
				if (!objectsEqual(my, e.getValue())) {
					if (result == ata)
						result = new ArrayType(ata.generalElementType(), ata.presumedLength(), ata.elementTypeMapping());
					result.elementTypeMapping().put(e.getKey(), unify(my, e.getValue()));
				}
			}
			return result;
		}
		
		if (a instanceof ProplistDeclaration && b instanceof ProplistDeclaration) {
			final ProplistDeclaration _a = (ProplistDeclaration) a;
			final ProplistDeclaration _b = (ProplistDeclaration) b;
			return new ProplistDeclaration(new ArrayList<Variable>()) {
				private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
				@Override
				public boolean gatherIncludes(Index contextIndex, IHasIncludes origin, List<IHasIncludes> set, int options) {
					if (set.contains(this))
						return false;
					else
						set.add(this);
					if ((options & GatherIncludesOptions.Recursive) != 0) {
						_a.gatherIncludes(contextIndex, origin, set, options);
						_b.gatherIncludes(contextIndex, origin, set, options);
					} else {
						set.add(_a);
						set.add(_b);
					}
					return true;
				}
			};
		}
		
		if (a instanceof WrappedType) {
			IType u = unifyLeft(WrappedType.unwrap(a), b);
			if (u != null)
				if (a instanceof NillableType)
					return NillableType.make(u);
				else if (a instanceof ReferenceType)
					return ReferenceType.make(u);
		}
		
		return null;
	}
	public static IType unifyNoChoice(IType a, IType b) {
		IType u = unifyLeft(a, b);
		if (u != null)
			return u;
		u = unifyLeft(b, a);
		if (u != null)
			return u;
		return null;
	}
	public static IType unify(IType a, IType b) {
		IType u = unifyNoChoice(a, b);
		return u != null ? u : TypeChoice.make(a, b);
	}
	public static IType unify(IType... ingredients) {
		if (ingredients.length == 0)
			return PrimitiveType.ANY;
		int actualCount = pack(ingredients);
		if (actualCount == 1)
			return ingredients[0];
		IType unified = ingredients[0];
		for (int i = 1; i < actualCount; i++)
			unified = unify(unified, ingredients[i]);
		return unified;
	}
}
