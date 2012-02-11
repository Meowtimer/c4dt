package net.arctics.clonk.parser.c4script.ast;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IRegion;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.ConstrainedProplist;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.ITypeable;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.FindDeclarationInfo;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.Function.FunctionScope;
import net.arctics.clonk.parser.c4script.IHasConstraint.ConstraintKind;
import net.arctics.clonk.parser.c4script.Variable.Scope;
import net.arctics.clonk.parser.c4script.ProplistDeclaration;
import net.arctics.clonk.parser.c4script.ast.evaluate.EvaluationContextProxy;
import net.arctics.clonk.parser.c4script.ast.evaluate.IEvaluationContext;

/**
 * Variable name expression element.
 * @author madeen
 *
 */
public class AccessVar extends AccessDeclaration {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	@Override
	public boolean isModifiable(C4ScriptParser context) {
		ExprElm pred = predecessorInSequence();
		if (pred == null) {
			return declaration == null || ((Variable)declaration).scope() != Scope.CONST;
		} else {
			return true; // you can never be so sure 
		}
	}

	public AccessVar(String varName) {
		super(varName);
	}

	public AccessVar(Declaration declaration) {
		this(declaration.name());
		this.declaration = declaration;
	}

	@Override
	public boolean isValidInSequence(ExprElm predecessor, C4ScriptParser context) {
		return
			// either null or
			predecessor == null ||
			// normally, a check would be performed whether the MemberOperator uses '.' instead of '->'
			// but in order to avoid the case where writing obj->StartOfFuncName is interpreted as
			// two not-properly-finished statements (obj->; and StartOfFuncName;) and then having
			// the StartOfFuncName; statement be replaced by the function call which will then have no
			// parameter information since the function only exists in the definition of obj that is not
			// consulted in that case, the '.' rule is enforced not here but in reportErrors (!!1)
			predecessor instanceof MemberOperator;
	}

	@Override
	public Declaration obtainDeclaration(DeclarationObtainmentContext context) {
		super.obtainDeclaration(context);
		ExprElm sequencePredecessor = predecessorInSequence();
		IType type = context.containingScript();
		if (sequencePredecessor != null)
			type = sequencePredecessor.typeInContext(context);
		if (type != null) for (IType t : type) {
			Script scriptToLookIn;
			if ((scriptToLookIn = Definition.scriptFrom(t)) == null) {
				// find pseudo-variable from proplist expression
				if (t instanceof ProplistDeclaration) {
					Variable proplistComponent = ((ProplistDeclaration)t).findComponent(declarationName());
					if (proplistComponent != null)
						return proplistComponent;
				}
			} else {
				FindDeclarationInfo info = new FindDeclarationInfo(context.containingScript().index());
				info.setContextFunction(context.currentFunction());
				info.setSearchOrigin(scriptToLookIn);
				Variable v = scriptToLookIn.findVariable(declarationName, info);
				if (v != null)
					return v;
			}
		}
		return null;
	}

	@Override
	public void reportErrors(C4ScriptParser parser) throws ParsingException {
		super.reportErrors(parser);
		ExprElm pred = predecessorInSequence();
		if (declaration == null && pred == null)
			parser.errorWithCode(ParserErrorCode.UndeclaredIdentifier, this, C4ScriptParser.NO_THROW, declarationName);
		// local variable used in global function
		else if (declaration instanceof Variable) {
			Variable var = (Variable) declaration;
			var.setUsed(true);
			switch (var.scope()) {
				case LOCAL:
					Declaration d = parser.currentDeclaration();
					if (d != null && predecessorInSequence() == null) {
						Function f = d.topLevelParentDeclarationOfType(Function.class);
						Variable v = d.topLevelParentDeclarationOfType(Variable.class);
						if (
							(f != null && f.getVisibility() == FunctionScope.GLOBAL) ||
							(f == null && v != null && v.scope() != Scope.LOCAL)
						) {
							parser.errorWithCode(ParserErrorCode.LocalUsedInGlobal, this, C4ScriptParser.NO_THROW);
						}
					}
					break;
				case STATIC: case CONST:
					parser.containingScript().addUsedScript(var.script());
					break;
				case VAR:
					if (var.location() != null && parser.currentFunction() != null && var.function() == parser.currentFunction()) {
						int locationUsed = parser.currentFunction().body().getOffset()+this.getExprStart();
						if (locationUsed < var.location().getOffset())
							parser.warningWithCode(ParserErrorCode.VarUsedBeforeItsDeclaration, this, var.name());
					}
					break;
			}
		}
		if (pred != null && pred instanceof MemberOperator && !((MemberOperator)pred).dotNotation) {
			parser.errorWithCode(ParserErrorCode.DotNotationInsteadOfArrow, this, C4ScriptParser.NO_THROW, this.declarationName());
		}
	}

	public static IStoredTypeInformation createStoredTypeInformation(Declaration declaration, C4ScriptParser parser) {
		if (declaration != null) {
			return new GenericStoredTypeInformation(new AccessVar(declaration), parser);
		} else {
			return null;
		}
	}
	
	@Override
	protected IType obtainType(DeclarationObtainmentContext context) {
		Declaration d = declarationFromContext(context);
		// getDeclaration(context) ensures that declaration is not null (if there is actually a variable) which is needed for queryTypeOfExpression for example
		if (d == Variable.THIS)
			return new ConstrainedProplist(context.containingScript(), ConstraintKind.CallerType);
		IType stored = context.queryTypeOfExpression(this, null);
		if (stored != null)
			return stored;
		if (d instanceof ITypeable)
			return ((ITypeable) d).type();
			//return new SameTypeAsSomeTypeable((ITypeable)d);
		return PrimitiveType.UNKNOWN;
	}

	@Override
	public void expectedToBeOfType(IType type, C4ScriptParser context, TypeExpectancyMode mode, ParserErrorCode errorWhenFailed) {
		if (declaration() == Variable.THIS)
			return;
		super.expectedToBeOfType(type, context, mode, errorWhenFailed);
	}

	@Override
	public void inferTypeFromAssignment(ExprElm expression, DeclarationObtainmentContext context) {
		if (declaration() == Variable.THIS)
			return;
		if (declaration() == null) {
			IType predType = predecessorInSequence() != null ? predecessorInSequence().typeInContext(context) : null;
			if (predType != null && predType.canBeAssignedFrom(PrimitiveType.PROPLIST)) {
				if (predType instanceof ProplistDeclaration) {
					ProplistDeclaration proplDecl = (ProplistDeclaration) predType;
					if (proplDecl.isAdHoc()) {
						Variable var = new Variable(declarationName(), Variable.Scope.VAR);
						var.expectedToBeOfType(expression.typeInContext(context), TypeExpectancyMode.Expect);
						var.setLocation(context.absoluteSourceLocationFromExpr(this));
						var.forceType(expression.typeInContext(context));
						var.setInitializationExpression(expression);
						proplDecl.addComponent(var);
					}
				}
			}
		}
		super.inferTypeFromAssignment(expression, context);
	}
	
	private static Definition definitionProxiedBy(Variable var) {
		if (var instanceof Definition.ProxyVar)
			return ((Definition.ProxyVar)var).definition();
		else
			return null;
	}

	@Override
	public Object evaluateAtParseTime(final IEvaluationContext context) {
		Definition obj;
		if (declaration instanceof Variable) {
			final Variable var = (Variable) declaration;
			if (var.scope() == Scope.CONST) {
				// if the whole of the initialization expression of the const gets evaluated to some traceable location,
				// report that to the original context as the origin of the AccessVar expression
				// evaluate in the context of the var by proxy
				Object val = var.evaluateInitializationExpression(new EvaluationContextProxy(var) {
					@Override
					public void reportOriginForExpression(ExprElm expression, IRegion location, IFile file) {
						if (expression == var.initializationExpression())
							context.reportOriginForExpression(AccessVar.this, location, file);
					}
				});
				if (val == null)
					val = 1337; // awesome fallback
				return val;
			}
			else if ((obj = definitionProxiedBy(var)) != null) {
				return obj.id(); // just return the id
			}
		}
		return super.evaluateAtParseTime(context);
	}

	public boolean constCondition() {
		return declaration instanceof Variable && ((Variable)declaration).scope() == Scope.CONST;
	}
	
	@Override
	public Object evaluate(IEvaluationContext context) throws ControlFlowException {
		if (context != null) {
			return context.valueForVariable(declarationName());
		}
		else {
			return super.evaluate(context);
		}
	}
	
	@Override
	public boolean isConstant() {
		if (declaration() instanceof Variable) {
			Variable var = (Variable) declaration();
			// naturally, consts are constant
			return var.scope() == Scope.CONST || definitionProxiedBy(var) != null;
		}
		else
			return false;
	}
	
	@Override
	public Class<? extends Declaration> declarationClass() {
		return Variable.class;
	}

}