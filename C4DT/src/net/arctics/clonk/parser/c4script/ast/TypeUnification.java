package net.arctics.clonk.parser.c4script.ast;

import static net.arctics.clonk.util.Utilities.defaulting;
import static net.arctics.clonk.util.Utilities.objectsEqual;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.c4script.ArrayType;
import net.arctics.clonk.parser.c4script.IRefinedPrimitiveType;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.NillableType;
import net.arctics.clonk.parser.c4script.ParameterType;
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
				return b;
			case ANY:
				return TypeChoice.make(a, b);
			case REFERENCE:
				return b;
			default:
				break;
			}

		if (a instanceof TypeChoice && b instanceof TypeChoice) {
			TypeChoice tca = (TypeChoice)a;
			TypeChoice tcb = (TypeChoice)b;
			IType l = unifyNoChoice(tca.left(), tcb.left());
			IType r = unifyNoChoice(tca.right(), tcb.right());
			if (l != null && r != null)
				return TypeChoice.make(l, r);
			else if (l == null && r != null)
				return TypeChoice.make(TypeChoice.make(tca.left(), tcb.left()), r);
			else if (l != null && r == null)
				return TypeChoice.make(l, TypeChoice.make(tca.right(), tcb.right()));
			else
				return null;
		}

		if (a instanceof TypeChoice) {
			TypeChoice tca = (TypeChoice)a;
			IType l = tca.left();
			IType r = tca.right();
			IType l_ = unifyNoChoice(l, b);
			IType r_ = unifyNoChoice(r, b);
			if (l_ == null && r_ == null)
				return null;
			else if (r_ == null)
				return TypeChoice.make(l_, r);
			else if (l_ == null)
				return TypeChoice.make(l, r_);
			else
				return TypeChoice.make(l_, r_);
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
			if (_a.numComponents(true) == 0)
				return _b;
			else if (_b.numComponents(true) == 0)
				return _a;
			return new ProplistDeclaration(new ArrayList<Variable>()) {
				private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
				@Override
				public boolean gatherIncludes(Index contextIndex, Object origin, Collection<ProplistDeclaration> set, int options) {
					if (!set.add(this))
						return false;
					if ((options & GatherIncludesOptions.Recursive) != 0) {
						_a.gatherIncludes(contextIndex, origin, set, options);
						_b.gatherIncludes(contextIndex, origin, set, options);
					} else {
						set.add(_a);
						set.add(_b);
					}
					return true;
				}
				@Override
				public Collection<Variable> components(boolean includeAdhocComponents) {
					Map<String, Variable> vars = new HashMap<String, Variable>();
					for (Variable v : _a.components(includeAdhocComponents))
						vars.put(v.name(), v);
					for (Variable v : _b.components(includeAdhocComponents))
						if (!vars.containsKey(v.name()))
							vars.put(v.name(), v);
					return vars.values();
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

		if (a instanceof ParameterType && b instanceof PrimitiveType)
			return b;

		if (a instanceof StructuralType && b instanceof StructuralType) {
			StructuralType sa = (StructuralType) a;
			StructuralType sb = (StructuralType) b;
			return new StructuralType(sa, sb);
		}

		if (a instanceof StructuralType && b instanceof Definition)
			if (((StructuralType)a).satisfiedBy((Definition)b))
				return b;

		return null;
	}
	public static IType unifyNoChoice(IType a, IType b) {
		IType u = unifyLeft(a, b);
		if (u != null)
			return u;
		u = unifyLeft(b, a);
//		if (u != null)
//			System.out.println(String.format("unify %s | %s -> %s", a.typeName(true), b.typeName(true), u.typeName(true)));
		if (u != null)
			return u;
		return null;
	}
	public static IType unify(IType a, IType b) {
		IType u = unifyNoChoice(a, b);
		return u != null ? u : TypeChoice.make(a, b);
	}
	public static IType unify(Iterable<IType> ingredients) {
		IType unified = null;
		for (IType t : ingredients)
			unified = unify(unified, t);
		return defaulting(unified, PrimitiveType.UNKNOWN);
	}
}
