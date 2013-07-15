package net.arctics.clonk.c4script.typing;

import static net.arctics.clonk.util.Utilities.defaulting;
import static net.arctics.clonk.util.Utilities.eq;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.arctics.clonk.c4script.IHasIncludes.GatherIncludesOptions;
import net.arctics.clonk.c4script.ProplistDeclaration;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.typing.dabble.Maybe;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.MetaDefinition;

public enum Typing {

	/** Static typing completely disabled. No parameter annotations allowed. */
	DYNAMIC,
	/** Allow type annotations for parameters, as the engine does. */
	INFERRED,
	/** Statically typed */
	STATIC {
		@Override
		public IType unifyNoChoice(IType a, IType b) {
			if (eq(a, b))
				return a;
			if (a instanceof Definition && b instanceof Definition) {
				final Definition da = (Definition) a;
				final Definition db = (Definition) b;
				if (!db.includes(0).contains(da))
					return null;
			}
			return super.unifyNoChoice(a, b);
		}
	};

	public static final Typing PARAMETERS_OPTIONALLY_TYPED = INFERRED;

	public boolean allowsNonParameterAnnotations() {
		switch (this) {
		case STATIC:
			return true;
		default:
			return false;
		}
	}

	private IType unifyLeft(IType a, IType b) {
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
				else if (b instanceof Definition)
					return ((Definition)b).metaDefinition();
				else if (eq(b, PrimitiveType.OBJECT))
					return PrimitiveType.ID;
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
				return Maybe.make(b);
			case REFERENCE:
				return b;
			default:
				break;
			}

		if (a instanceof Maybe && b instanceof Maybe)
			return new Maybe(unify(((Maybe)a).maybe(), ((Maybe)b).maybe()));

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
			/*final IType assumed = tca.assumed();
			if (assumed != null) {
				final IType b_ = unifyNoChoice(assumed, b);
				if (b_ != null)
					return TypeChoice.make(PrimitiveType.ANY, b_);
			}*/
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
			return new ArrayType(unify(ata.elementType(), atb.elementType()));
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
				if (a instanceof ReferenceType)
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
			return unify(PrimitiveType.OBJECT, b);
//			if (b instanceof Definition || b instanceof MetaDefinition || b instanceof ProplistDeclaration ||
//				b == PrimitiveType.OBJECT || b == PrimitiveType.ID || b == PrimitiveType.PROPLIST)
//				return b;

		return null;
	}
	protected IType unifyDefinitions(final Definition da, final Definition db) {
		if (db.doesInclude(db.index(), da))
			return da;
		else if (da.doesInclude(da.index(), db))
			return db;
		else {
			final List<Script> cda = da.conglomerate();
			final List<Script> cdb = db.conglomerate();
			cda.retainAll(cdb);
			final List<Script> common = cda;
			final Set<Script> blurps = new HashSet<>(common);
			for (final Script s : blurps) {
				if (!common.contains(s))
					continue;
				if (!(s instanceof Definition))
					common.remove(s);
				else {
					final Collection<Script> cong = s.includes(GatherIncludesOptions.Recursive);
					common.removeAll(cong);
				}
			}
			return common.size() > 0 ? TypeChoice.make(common) : PrimitiveType.OBJECT.unified();
		}
	}
	public IType unifyNoChoice(IType a, IType b) {
		IType u = unifyLeft(a, b);
		if (u != null)
			return u;
		u = unifyLeft(b, a);
		if (u != null)
			return u;
		return null;
	}
	public IType unify(IType a, IType b) {
		final IType u = unifyNoChoice(a, b);
		return u != null ? u : TypeChoice.make(a, b);
	}
	public IType unify(Iterable<? extends IType> ingredients) {
		IType unified = null;
		for (final IType t : ingredients)
			unified = unify(unified, t);
		return defaulting(unified, PrimitiveType.UNKNOWN);
	}
	public boolean compatible(IType a, IType b) { return unifyNoChoice(a, b) != null; }
}
