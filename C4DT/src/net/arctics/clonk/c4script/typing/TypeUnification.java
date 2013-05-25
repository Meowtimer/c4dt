package net.arctics.clonk.c4script.typing;

import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.defaulting;
import static net.arctics.clonk.util.Utilities.eq;

import java.util.ArrayList;
import java.util.List;

import net.arctics.clonk.c4script.ProplistDeclaration;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.ast.ThisType;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.MetaDefinition;
import net.arctics.clonk.util.IPredicate;
import net.arctics.clonk.util.Utilities;

public class TypeUnification {
	private static final IPredicate<Script> IS_DEFINITION = new IPredicate<Script>() {
		@Override
		public boolean test(Script item) {
			return item instanceof Definition;
		}
	};
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
				if (b instanceof MetaDefinition)
					return b;
				break;
			case OBJECT:
			case INT:
			case STRING:
				break;
			case BOOL:
				if (eq(b, PrimitiveType.INT))
					return b;
				else
					// anything can be passed for a bool
					return TypeChoice.make(PrimitiveType.BOOL, b);
			case PROPLIST:
				if (b == PrimitiveType.OBJECT || b == PrimitiveType.ID)
					return b;
				else if (b instanceof Definition || b instanceof MetaDefinition)
					return b;
				else
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

		if (a instanceof ThisType) {
			final Script sa = ((ThisType)a).script();
			if (b instanceof Script)
				return defaulting(unifyNoChoice(sa, b), PrimitiveType.OBJECT);
			else if (b instanceof ThisType) {
				final Script sb = ((ThisType)b).script();
				final Script u = as(unifyNoChoice(sa, sb), Script.class);
				if (u != null)
					return new ThisType(u);
			}
		}

		if (a instanceof TypeChoice && b instanceof TypeChoice) {
			final TypeChoice tca = (TypeChoice)a;
			final TypeChoice tcb = (TypeChoice)b;
			final IType l = unifyNoChoice(tca.left(), tcb.left());
			final IType r = unifyNoChoice(tca.right(), tcb.right());
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
			final TypeChoice tca = (TypeChoice)a;
			final IType l = tca.left();
			final IType r = tca.right();
			final IType l_ = unifyNoChoice(l, b);
			final IType r_ = unifyNoChoice(r, b);
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

		if (a instanceof PrimitiveType.Unified)
			if (unifyNoChoice(((PrimitiveType.Unified)a).base(), b) != null)
				return a; // refuse specialization

		if (a instanceof ArrayType && b instanceof ArrayType) {
			final ArrayType ata = (ArrayType)a;
			final ArrayType atb = (ArrayType)b;
			return new ArrayType(TypeUnification.unify(ata.elementType(), atb.elementType()));
		}

		if (a instanceof ProplistDeclaration && b instanceof ProplistDeclaration) {
			final ProplistDeclaration _a = (ProplistDeclaration) a;
			final ProplistDeclaration _b = (ProplistDeclaration) b;
			if (_a.numComponents(true) == 0)
				return _b;
			else if (_b.numComponents(true) == 0)
				return _a;
			return TypeChoice.make(_a, _b);
			//return PrimitiveType.PROPLIST.unified(); // screw it
		}

		if (a instanceof WrappedType) {
			final IType u = unifyNoChoice(WrappedType.unwrap(a), b);
			if (u != null)
				if (a instanceof NillableType)
					return NillableType.make(u);
				else if (a instanceof ReferenceType)
					return ReferenceType.make(u);
		}

		if (a instanceof Definition && b instanceof Definition) {
			final Definition da = (Definition)a;
			final Definition db = (Definition)b;
			return unifyDefinitions(da, db);
		}

		if (a instanceof MetaDefinition && b instanceof MetaDefinition) {
			final IType t = unifyDefinitions(((MetaDefinition)a).definition(), ((MetaDefinition)b).definition());
			return t instanceof Definition ? ((Definition)t).metaDefinition() : PrimitiveType.ID.unified();
		}

		if (a instanceof CallTargetType)
			if (b instanceof Definition || b instanceof MetaDefinition || b instanceof ProplistDeclaration ||
				b == PrimitiveType.OBJECT || b == PrimitiveType.ID || b == PrimitiveType.PROPLIST)
				return b;

		return null;
	}
	private static IType unifyDefinitions(final Definition da, final Definition db) {
		if (db.doesInclude(db.index(), da))
			return da;
		else if (da.doesInclude(da.index(), db))
			return db;
		else {
			final List<Script> cda = da.conglomerate();
			final List<Script> cdb = db.conglomerate();
			cda.retainAll(cdb);
			final List<Script> common = Utilities.filter(cda, IS_DEFINITION);
			if (common.size() > 1) {
				final List<Script> commonBases = new ArrayList<>(common.size());
				for (final Script x : common) {
					boolean includedByAll = true;
					for (final Script y : common)
						if (x != y && !y.doesInclude(y.index(), x)) {
							includedByAll = false;
							break;
						}
					if (includedByAll)
						commonBases.add(x);
				}
				common.removeAll(commonBases);
			}
			return common.size() > 0 ? TypeChoice.make(common) : PrimitiveType.OBJECT.unified();
		}
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
		final IType u = unifyNoChoice(a, b);
		return u != null ? u : TypeChoice.make(a, b);
	}
	public static IType unify(Iterable<IType> ingredients) {
		IType unified = null;
		for (final IType t : ingredients)
			unified = unify(unified, t);
		return defaulting(unified, PrimitiveType.UNKNOWN);
	}
	public static boolean compatible(IType a, IType b) { return TypeUnification.unifyNoChoice(a, b) != null; }
}
