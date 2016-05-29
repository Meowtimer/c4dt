package net.arctics.clonk.c4script.typing;

import static net.arctics.clonk.util.Utilities.defaulting;
import static net.arctics.clonk.util.Utilities.eq;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
			if ((a == PrimitiveType.VOID || b == PrimitiveType.VOID) && a != b) {
				return null;
			}
			if (eq(a, b)) {
				return a;
			}
			if (a instanceof Definition && b instanceof Definition) {
				final Definition da = (Definition) a;
				final Definition db = (Definition) b;
				if (!db.includes(0).contains(da)) {
					return null;
				}
			}
			if (a instanceof ArrayType && b instanceof ArrayType) {
				if (!compatible(
					((ArrayType)a).elementType(),
					((ArrayType)b).elementType()
				)) {
					return null;
				}
			}
			return super.unifyNoChoice(a, b);
		}
		@Override
		public boolean allowsNonParameterAnnotations() { return true; }
	};

	public static final Typing PARAMETERS_OPTIONALLY_TYPED = INFERRED;

	public boolean allowsNonParameterAnnotations() { return false; }

	private IType unifyLeft(final IType type1, final IType type2) {
		if (type1 == null) {
			return type2;
		}
		if (type2 == null) {
			return type1;
		}
		if (type1.equals(type2)) {
			return type1;
		}

		if (type1 instanceof PrimitiveType) {
			switch ((PrimitiveType)type1) {
			case ARRAY:
			case ID:
				if (type2 instanceof MetaDefinition) {
					return type2;
				} else if (type2 instanceof Definition) {
					return ((Definition)type2).metaDefinition();
				} else if (eq(type2, PrimitiveType.OBJECT)) {
					return PrimitiveType.ID;
				}
				break;
			case OBJECT:
			case STRING:
				break;
			case INT:
				if (type2 instanceof Definition) {
					return type2;
				}
				break;
			case BOOL:
				return eq(type2, PrimitiveType.INT)
					? type2
					// anything can be passed for a bool
					: TypeChoice.make(PrimitiveType.BOOL, type2);
			case PROPLIST:
				if (type2 == PrimitiveType.OBJECT || type2 == PrimitiveType.ID || type2 == PrimitiveType.EFFECT) {
					return type2;
				} else if (type2 instanceof Definition || type2 instanceof MetaDefinition) {
					return type2;
				} else {
					break;
				}
			case UNKNOWN: case VOID:
				return type2;
			case ANY:
				return Maybe.make(type2);
			case REFERENCE:
				return PrimitiveType.REFERENCE;
			default:
				break;
			}
		}

		if (type1 instanceof Maybe && type2 instanceof Maybe) {
			return new Maybe(unify(((Maybe)type1).maybe(), ((Maybe)type2).maybe()));
		}

		if (type1 instanceof TypeChoice && type2 instanceof TypeChoice) {
			final TypeChoice tca = (TypeChoice)type1;
			final TypeChoice tcb = (TypeChoice)type2;
			final IType firstTry = unifyTypeChoices(tca, tcb);
			return firstTry != null ? firstTry : unifyTypeChoices(tca, new TypeChoice(tcb.right(), tcb.left()));
		}

		if (type1 instanceof TypeChoice) {
			final TypeChoice tca = (TypeChoice)type1;
			return unifyTypeAndChoice(tca, type2, 0);
		}

		if (type1 instanceof IRefinedPrimitiveType && type2 instanceof PrimitiveType &&
			((IRefinedPrimitiveType)type1).primitiveType() == type2) {
			return type1;
		}

		if (type1 instanceof PrimitiveType.Unified) {
			if (unifyNoChoice(((PrimitiveType.Unified)type1).base(), type2) != null) {
				return type1; // refuse specialization
			}
		}

		if (type1 instanceof ArrayType && type2 instanceof ArrayType) {
			final ArrayType ata = (ArrayType)type1;
			final ArrayType atb = (ArrayType)type2;
			return new ArrayType(unify(ata.elementType(), atb.elementType()));
		}

		if (type1 instanceof ProplistDeclaration && type2 instanceof ProplistDeclaration) {
			final ProplistDeclaration _a = (ProplistDeclaration) type1;
			final ProplistDeclaration _b = (ProplistDeclaration) type2;
			if (_a.numComponents(true) == 0) {
				return _b;
			} else if (_b.numComponents(true) == 0) {
				return _a;
			}
			return TypeChoice.make(_a, _b);
			//return PrimitiveType.PROPLIST.unified(); // screw it
		}

		if (type1 instanceof WrappedType) {
			final IType u = unifyNoChoice(WrappedType.unwrap(type1), type2);
			if (u != null) {
				if (type1 instanceof ReferenceType) {
					return ReferenceType.make(u);
				}
			}
		}

		if (type1 instanceof Definition && type2 instanceof Definition) {
			final Definition da = (Definition)type1;
			final Definition db = (Definition)type2;
			return unifyDefinitions(da, db);
		}

		if (type1 instanceof MetaDefinition && type2 instanceof MetaDefinition) {
			final IType t = unifyDefinitions(((MetaDefinition)type1).definition(), ((MetaDefinition)type2).definition());
			return t instanceof Definition ? ((Definition)t).metaDefinition() : PrimitiveType.ID.unified();
		}

		if (type1 instanceof CallTargetType) {
			return unify(PrimitiveType.OBJECT, type2);
		}

		if (type1 instanceof FunctionType && type2 instanceof FunctionType) {
			final FunctionType functionTypeA = (FunctionType) type1;
			final FunctionType functionTypeB = (FunctionType) type2;
			return new FunctionType(
				new Function("<unified>", FunctionScope.GLOBAL)
					.setParameters(
						IntStream.range(0, Math.max(
							functionTypeA.prototype().parameters().length,
							functionTypeB.prototype().parameters().length
						)).mapToObj(parameterIndex -> {
							final Variable parameterA = functionTypeA.prototype().parameter(parameterIndex);
							final Variable parameterB = functionTypeB.prototype().parameter(parameterIndex);
							return unifyVariables(parameterA, parameterB);
						}).toArray(length -> new Variable[length])
					)
			);
		}

		return null;
	}

	private Variable unifyVariables(final Variable parameterA, final Variable parameterB) {
		return (
			parameterA == null ? new Variable(parameterB.name(), parameterB.type()) :
			parameterB == null ? new Variable(parameterA.name(), parameterA.type()) :
			new Variable(
				eq(parameterA.name(), parameterB.name())
					? parameterA.name()
					: String.format("%s/%s", parameterA.name(), parameterB.name()),
				unify(parameterA.type(), parameterB.type())
			)
		);
	}

	private IType unifyTypeChoices(final TypeChoice tca, final TypeChoice tcb) {
		final IType unifiedLeft = unifyNoChoice(tca.left(), tcb.left());
		final IType unifiedRight = unifyNoChoice(tca.right(), tcb.right());
		return (
			unifiedLeft != null && unifiedRight != null ? TypeChoice.make(unifiedLeft, unifiedRight) :
			unifiedLeft == null && unifiedRight != null ? TypeChoice.make(TypeChoice.make(tca.left(), tcb.left()), unifiedRight) :
			unifiedLeft != null && unifiedRight == null ? TypeChoice.make(unifiedLeft, TypeChoice.make(tca.right(), tcb.right())) :
			null
		);
	}

	private IType unifyTypeAndChoice(final TypeChoice choice, final IType type, final int recursion) {
		final IType left = choice.left();
		final IType right = choice.right();
		if (left.equals(type) || right.equals(type)) {
			return choice;
		}
		final IType unifiedLeft = unifyNoChoice(left, type);
		final IType unifiedRight = unifyNoChoice(right, type);
		if (unifiedLeft!= null && unifiedRight != null) {
			if (recursion < 5) {
				if (unifiedLeft instanceof TypeChoice && !(unifiedRight instanceof TypeChoice)) {
					return unifyTypeAndChoice((TypeChoice)unifiedLeft, unifiedRight, recursion+1);
				} else if (unifiedRight instanceof TypeChoice && !(unifiedLeft instanceof TypeChoice)) {
					return unifyTypeAndChoice((TypeChoice)unifiedRight, unifiedLeft, recursion+1);
				}
			}
			return TypeChoice.make(unifiedLeft, unifiedRight);
		} else if (unifiedLeft != null) {
			return TypeChoice.make(unifiedLeft, right);
		} else if (unifiedRight != null) {
			return TypeChoice.make(left, unifiedRight);
		} else {
			return null;
		}
	}

	protected IType unifyDefinitions(final Definition definition1, final Definition definition2) {
		if (definition2.doesInclude(definition2.index(), definition1)) {
			return definition1;
		} else if (definition1.doesInclude(definition1.index(), definition2)) {
			return definition2;
		} else {
			final List<Script> conglomerate1 = definition1.conglomerate();
			final List<Script> conglomerate2 = definition2.conglomerate();
			conglomerate1.retainAll(conglomerate2);
			final List<Script> common = conglomerate1;
			final Set<Script> blurps = new HashSet<>(common);
			for (final Script s : blurps) {
				if (!common.contains(s)) {
					continue;
				}
				if (!(s instanceof Definition)) {
					common.remove(s);
				} else {
					final Collection<Script> cong = s.includes(GatherIncludesOptions.Recursive);
					common.removeAll(cong);
				}
			}
			return common.size() > 0
				? TypeChoice.make(common)
				: TypeChoice.make(definition1, definition2);
		}
	}

	public IType unifyNoChoice(final IType a, final IType b) {
		return defaulting(unifyLeft(a, b), () -> unifyLeft(b, a));
	}

	public IType unify(final IType a, final IType b) {
		final IType u = unifyNoChoice(a, b);
		return u != null ? u : TypeChoice.make(a, b);
	}

	public IType unify(final Iterable<? extends IType> ingredients) {
		IType unified = null;
		for (final IType t : ingredients) {
			unified = unify(unified, t);
		}
		return defaulting(unified, PrimitiveType.UNKNOWN);
	}

	public boolean compatible(final IType a, final IType b) { return unifyNoChoice(a, b) != null; }

}
