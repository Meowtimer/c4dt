package net.arctics.clonk.parser.c4script.ast;

import static net.arctics.clonk.util.Utilities.as;
import net.arctics.clonk.Core;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.ConstrainedProplist;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.FindDeclarationInfo;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.Function.FunctionScope;
import net.arctics.clonk.parser.c4script.FunctionType;
import net.arctics.clonk.parser.c4script.IHasConstraint.ConstraintKind;
import net.arctics.clonk.parser.c4script.IProplistDeclaration;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.ITypeable;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.Variable.Scope;
import net.arctics.clonk.parser.c4script.ast.evaluate.EvaluationContextProxy;
import net.arctics.clonk.parser.c4script.ast.evaluate.IEvaluationContext;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IRegion;

/**
 * Variable name expression element.
 * @author madeen
 *
 */
public class AccessVar extends AccessDeclaration {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	@Override
	public boolean isModifiable(C4ScriptParser context) {
		ExprElm pred = predecessorInSequence();
		if (pred == null)
			return declaration == null || ((Variable)declaration).scope() != Scope.CONST;
		else
			return true; // you can never be so sure 
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
			// and anyways, everything is different now that function values are possible yadda
			predecessor instanceof MemberOperator;
	}

	@Override
	public Declaration obtainDeclaration(DeclarationObtainmentContext context) {
		super.obtainDeclaration(context);
		ExprElm sequencePredecessor = predecessorInSequence();
		IType type = context.script();
		if (sequencePredecessor != null)
			type = sequencePredecessor.type(context);
		if (type != null) for (IType t : type) {
			Script scriptToLookIn;
			if ((scriptToLookIn = Definition.scriptFrom(t)) == null) {
				// find pseudo-variable from proplist expression
				if (t instanceof IProplistDeclaration) {
					Variable proplistComponent = ((IProplistDeclaration)t).findComponent(declarationName());
					if (proplistComponent != null)
						return proplistComponent;
				}
			} else {
				FindDeclarationInfo info = new FindDeclarationInfo(context.script().index());
				info.contextFunction = sequencePredecessor == null ? context.currentFunction() : null;
				info.searchOrigin = scriptToLookIn;
				info.findGlobalVariables = sequencePredecessor == null;
				Declaration v = scriptToLookIn.findDeclaration(declarationName, info);
				if (v instanceof Definition)
					//	context.performParsingPhaseTwo((Definition)v);
					v = ((Definition)v).proxyVar();
				if (v != null)
					return v;
			}
		}
		return null;
	}

	@Override
	public void reportProblems(C4ScriptParser parser) throws ParsingException {
		super.reportProblems(parser);
		ExprElm pred = predecessorInSequence();
		if (declaration == null && pred == null)
			parser.error(ParserErrorCode.UndeclaredIdentifier, this, C4ScriptParser.NO_THROW, declarationName);
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
							(f != null && f.visibility() == FunctionScope.GLOBAL) ||
							(f == null && v != null && v.scope() != Scope.LOCAL)
						)
							parser.error(ParserErrorCode.LocalUsedInGlobal, this, C4ScriptParser.NO_THROW);
					}
					break;
				case STATIC: case CONST:
					parser.script().addUsedScript(var.script());
					break;
				case VAR:
					if (var.location() != null && parser.currentFunction() != null && var.parentDeclaration() == parser.currentFunction()) {
						int locationUsed = parser.currentFunction().bodyLocation().getOffset()+this.start();
						if (locationUsed < var.location().getOffset())
							parser.warning(ParserErrorCode.VarUsedBeforeItsDeclaration, this, 0, var.name());
					}
					break;
				case PARAMETER:
					break;
			}
		} else if (declaration instanceof Function)
			if (!parser.script().engine().settings().supportsFunctionRefs)
				parser.error(ParserErrorCode.FunctionRefNotAllowed, this, C4ScriptParser.NO_THROW, parser.script().engine().name());
	}

	public static ITypeInfo makeTypeInfo(Declaration declaration, C4ScriptParser parser) {
		if (declaration != null)
			return new GenericTypeInfo(new AccessVar(declaration), parser);
		else
			return null;
	}
	
	@Override
	public IType unresolvedType(DeclarationObtainmentContext context) {
		Declaration d = declarationFromContext(context);
		// declarationFromContext(context) ensures that declaration is not null (if there is actually a variable) which is needed for queryTypeOfExpression for example
		if (d == Variable.THIS)
			if (context.script() instanceof Definition)
				return ((Definition)context.script()).thisType();
			else
				return new ConstrainedProplist(context.script(), ConstraintKind.CallerType, true, true);
		IType stored = context.queryTypeOfExpression(this, null);
		if (stored != null)
			return stored;
		if (d instanceof Function)
			return new FunctionType((Function)d);
		else if (d instanceof ITypeable)
			return ((ITypeable) d).type();
			//return new SameTypeAsSomeTypeable((ITypeable)d);
		return PrimitiveType.UNKNOWN;
	}
	
	@Override
	protected IType callerType(DeclarationObtainmentContext context) {
		Variable v = as(declaration, Variable.class);
		if (v != null) switch (v.scope()) {
		case CONST: case STATIC:
			return null;
		default:
			break;
		}
		return super.callerType(context);
	}

	@Override
	public boolean typingJudgement(IType type, C4ScriptParser context, TypingJudgementMode mode) {
		if (declaration() == Variable.THIS)
			return true;
		return super.typingJudgement(type, context, mode);
	}

	@Override
	public void assignment(ExprElm expression, C4ScriptParser context) {
		if (declaration() == Variable.THIS)
			return;
		if (declaration() == null) {
			IType predType = predecessorType(context);
			if (predType != null && predType.canBeAssignedFrom(PrimitiveType.PROPLIST))
				if (predType instanceof IProplistDeclaration) {
					IProplistDeclaration proplDecl = (IProplistDeclaration) predType;
					if (proplDecl.isAdHoc()) {
						Variable var = new Variable(declarationName(), Variable.Scope.VAR);
						var.initializeFromAssignment(this, expression, context);
						proplDecl.addComponent(var, true);
						declaration = var;
					}
				} else for (IType t : predType)
					if (t == context.script()) {
						Variable var = new Variable(declarationName(), Variable.Scope.LOCAL);
						var.initializeFromAssignment(this, expression, context);
						context.script().addDeclaration(var);
						declaration = var;
						break;
					}
		}
		super.assignment(expression, context);
	}
	
	private static Definition definitionProxiedBy(Declaration var) {
		if (var instanceof Definition.ProxyVar)
			return ((Definition.ProxyVar)var).definition();
		else
			return null;
	}
	
	public Definition proxiedDefinition() {
		return definitionProxiedBy(declaration());
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
			else if ((obj = definitionProxiedBy(var)) != null)
				return obj.id(); // just return the id
		}
		return super.evaluateAtParseTime(context);
	}

	public boolean constCondition() {
		return declaration instanceof Variable && ((Variable)declaration).scope() == Scope.CONST;
	}
	
	@Override
	public Object evaluate(IEvaluationContext context) throws ControlFlowException {
		if (context != null)
			return context.valueForVariable(declarationName());
		else
			return super.evaluate(context);
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
	public ITypeInfo createTypeInfo(C4ScriptParser parser) {
		if (declaration instanceof Variable && declaration.parentDeclaration() instanceof Function) {
			class LocalVariableInfo extends TypeInfo {
				public LocalVariableInfo() {
					this.type = PrimitiveType.UNKNOWN;
				}
				public AccessVar origin() { return AccessVar.this; }
				@Override
				public boolean storesTypeInformationFor(ExprElm expr, C4ScriptParser parser) {
					return expr instanceof AccessVar && ((AccessVar)expr).declaration() == declaration();
				}
				@Override
				public boolean refersToSameExpression(ITypeInfo other) {
					return
						other instanceof LocalVariableInfo &&
						((LocalVariableInfo)other).origin().declaration() == declaration();
				}
				@Override
				public void apply(boolean soft, C4ScriptParser parser) {
					Variable v = (Variable)origin().declaration();
					v.expectedToBeOfType(type, TypingJudgementMode.Expect);
				}
				@Override
				public String toString() {
					return String.format("[%s: %s]", declarationName, type().typeName(true));
				}
			}
			return new LocalVariableInfo();
		} else
			return super.createTypeInfo(parser);
	}
	
	@Override
	public Class<? extends Declaration> declarationClass() {
		return Variable.class;
	}

}