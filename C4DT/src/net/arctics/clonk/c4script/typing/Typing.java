package net.arctics.clonk.c4script.typing;

import static net.arctics.clonk.util.Utilities.defaulting;
import static net.arctics.clonk.util.Utilities.eq;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Function.FunctionScope;
import net.arctics.clonk.c4script.IHasIncludes.GatherIncludesOptions;
import net.arctics.clonk.c4script.ProplistDeclaration;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.MetaDefinition;

public enum Typing {

	/** Static typing completely disabled. No parameter annotations allowed. */
	DYNAMIC {
		@Override
		public IType unifyNoChoice(final IType a, final IType b) { return PrimitiveType.ANY; }
	},

	/** Allow type annotations for parameters, as the engine does. */
	INFERRED,

	/** Statically typed */
	STATIC {
		@Override
		public IType unifyNoChoice(final IType a, final IType b) {
			if ((a == PrimitiveType.VOID || b == PrimitiveType.VOID) && a != b)
				return null;
			if (eq(a, b))
				return a;
			if (a instanceof Definition && b instanceof Definition) {
				final Definition da = (Definition) a;
				final Definition db = (Definition) b;
				if (!db.includes(0).contains(da))
					return null;
			}
			if (a instanceof ArrayType && b instanceof ArrayType)
				if (!compatible(
					((ArrayType)a).elementType(),
					((ArrayType)b).elementType()
				))
					return null;
			return super.unifyNoChoice(a, b);
		}
		@Override
		public boolean allowsNonParameterAnnotations() { return true; }
	};

	public static final Typing PARAMETERS_OPTIONALLY_TYPED = INFERRED;

	public boolean allowsNonParameterAnnotations() { return false; }

	private IType unifyLeft(final IType a, final IType b) {
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
			case STRING:
				break;
			case INT:
				if (b instanceof Definition)
					return b;
				break;
			case BOOL:
				return eq(b, PrimitiveType.INT)
					? b
					// anything can be passed for a bool
					: TypeChoice.make(PrimitiveType.BOOL, b);
			case PROPLIST:
				if (b == PrimitiveType.OBJECT || b == PrimitiveType.ID || b == PrimitiveType.EFFECT)
					return b;
				else if (b instanceof Definition || b instanceof MetaDefinition)
					return b;
				else
					break;
			case UNKNOWN: case VOID:
				return b;
			case ANY:
				return Maybe.make(b);
			case REFERENCE:
				return PrimitiveType.REFERENCE;
			default:
				break;
			}

		if (a instanceof Maybe && b instanceof Maybe)
			return new Maybe(unify(((Maybe)a).maybe(), ((Maybe)b).maybe()));

		if (a instanceof TypeChoice && b instanceof TypeChoice) {
			final TypeChoice tca = (TypeChoice)a;
			final TypeChoice tcb = (TypeChoice)b;
			final IType firstTry = unifyTypeChoices(tca, tcb);
			return firstTry != null ? firstTry : unifyTypeChoices(tca, new TypeChoice(tcb.right(), tcb.left()));
		}

		if (a instanceof TypeChoice) {
			final TypeChoice tca = (TypeChoice)a;
			return unifyTypeAndChoice(tca, b, 0);
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
		
		if (a instanceof FunctionType && b instanceof FunctionType) {
			final FunctionType functionTypeA = (FunctionType) a;
			final FunctionType functionTypeB = (FunctionType) b;
			final List<Variable> unifiedParameters = IntStream.range(0, Math.max(
				functionTypeA.prototype().parameters().size(),
				functionTypeB.prototype().parameters().size()
			)).mapToObj(parameterIndex -> {
				Variable parameterA = functionTypeA.prototype().parameter(parameterIndex);
				Variable parameterB = functionTypeB.prototype().parameter(parameterIndex);
				return (Variable)(
					parameterA == null ? new Variable(parameterB.name(), parameterB.type()) :
					parameterB == null ? new Variable(parameterA.name(), parameterA.type()) :
					new Variable(
						eq(parameterA.name(), parameterB.name())
							? parameterA.name() : String.format("%s/%s", parameterA.name(), parameterB.name()),
						unify(parameterA.type(), parameterB.type())
					)
				);
			}).collect(Collectors.toList());
			final Function unifiedFunction = new Function("<unified>", FunctionScope.GLOBAL);
			unifiedFunction.setParameters(unifiedParameters);
			return new FunctionType(unifiedFunction);
		}

		return null;
	}

	private IType unifyTypeChoices(final TypeChoice tca, final TypeChoice tcb) {
		final IType l = unifyNoChoice(tca.left(), tcb.left());
		final IType r = unifyNoChoice(tca.right(), tcb.right());
		return
			l != null && r != null ? TypeChoice.make(l, r) :
			l == null && r != null ? TypeChoice.make(TypeChoice.make(tca.left(), tcb.left()), r) :
			l != null && r == null ? TypeChoice.make(l, TypeChoice.make(tca.right(), tcb.right())) :
			null;
	}

	private IType unifyTypeAndChoice(final TypeChoice choice, final IType type, final int recursion) {
		final IType l = choice.left();
		final IType r = choice.right();
		if (l.equals(type) || r.equals(type))
			return choice;
		final IType l_ = unifyNoChoice(l, type);
		final IType r_ = unifyNoChoice(r, type);
		if (l_ != null && r_ != null) {
			if (recursion < 5)
				if (l_ instanceof TypeChoice && !(r_ instanceof TypeChoice))
					return unifyTypeAndChoice((TypeChoice)l_, r_, recursion+1);
				else if (r_ instanceof TypeChoice && !(l_ instanceof TypeChoice))
					return unifyTypeAndChoice((TypeChoice)r_, l_, recursion+1);
			return TypeChoice.make(l_, r_);
		} else if (l_ != null)
			return TypeChoice.make(l_, r);
		else if (r_ != null)
			return TypeChoice.make(l, r_);
		else
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
			return common.size() > 0
				? TypeChoice.make(common)
				: TypeChoice.make(da, db);
		}
	}
	public IType unifyNoChoice(final IType a, final IType b) {
		IType u = unifyLeft(a, b);
		if (u != null)
			return u;
		u = unifyLeft(b, a);
		if (u != null)
			return u;
		return null;
	}
	public IType unify(final IType a, final IType b) {
		final IType u = unifyNoChoice(a, b);
		return u != null ? u : TypeChoice.make(a, b);
	}
	public IType unify(final Iterable<? extends IType> ingredients) {
		IType unified = null;
		for (final IType t : ingredients)
			unified = unify(unified, t);
		return defaulting(unified, PrimitiveType.UNKNOWN);
	}
	public boolean compatible(final IType a, final IType b) { return unifyNoChoice(a, b) != null; }
}
