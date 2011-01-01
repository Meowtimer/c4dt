package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.C4Object;
import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4Function;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.C4Type;
import net.arctics.clonk.parser.c4script.C4Variable;
import net.arctics.clonk.parser.c4script.FindDeclarationInfo;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.C4Function.C4FunctionScope;
import net.arctics.clonk.parser.c4script.C4Variable.C4VariableScope;
import net.arctics.clonk.parser.c4script.ast.evaluate.IEvaluationContext;

public class AccessVar extends AccessDeclaration {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	@Override
	public boolean modifiable(C4ScriptParser context) {
		ExprElm pred = getPredecessorInSequence();
		if (pred == null) {
			return declaration == null || ((C4Variable)declaration).getScope() != C4VariableScope.CONST;
		} else {
			return true; // you can never be so sure 
		}
	}

	public AccessVar(String varName) {
		super(varName);
	}

	public AccessVar(C4Declaration declaration) {
		this(declaration.getName());
		this.declaration = declaration;
	}

	@Override
	public boolean isValidInSequence(ExprElm predecessor, C4ScriptParser context) {
		return
			// either null or
			predecessor == null ||
			// following a dot
			(predecessor instanceof MemberOperator && ((MemberOperator)predecessor).dotNotation);
	}

	@Override
	public C4Declaration obtainDeclaration(C4ScriptParser parser) {
		ExprElm p = getPredecessorInSequence();
		C4ScriptBase scriptToLookIn = null;
		if (p != null) {
			IType type = p.getType(parser);
			if ((scriptToLookIn = C4Object.objectTypeFrom(type)) == null) {
				// find pseudo-variable from proplist expression
				if (type instanceof PropListExpression) {
					return ((PropListExpression)type).findComponent(getDeclarationName());
				}
			}
		} else {
			scriptToLookIn = parser.getContainer();
		}
		if (scriptToLookIn != null) {
			FindDeclarationInfo info = new FindDeclarationInfo(parser.getContainer().getIndex());
			info.setContextFunction(parser.getCurrentFunc());
			info.setSearchOrigin(scriptToLookIn);
			return scriptToLookIn.findVariable(declarationName, info);
		} else {
			return null;
		}
	}

	@Override
	public void reportErrors(C4ScriptParser parser) throws ParsingException {
		super.reportErrors(parser);
		if (declaration == null && getPredecessorInSequence() == null) {
			parser.errorWithCode(ParserErrorCode.UndeclaredIdentifier, this, true, declarationName);
		}
		// local variable used in global function
		else if (declaration instanceof C4Variable) {
			C4Variable var = (C4Variable) declaration;
			var.setUsed(true);
			switch (var.getScope()) {
				case LOCAL:
					C4Declaration d = parser.getCurrentDeclaration();
					if (d != null) {
						C4Function f = d.getTopLevelParentDeclarationOfType(C4Function.class);
						C4Variable v = d.getTopLevelParentDeclarationOfType(C4Variable.class);
						if (
							(f != null && f.getVisibility() == C4FunctionScope.GLOBAL) ||
							(f == null && v != null && v.getScope() != C4VariableScope.LOCAL)
						) {
							parser.errorWithCode(ParserErrorCode.LocalUsedInGlobal, this, true);
						}
					}
					break;
				case STATIC:
					parser.getContainer().addUsedProjectScript(var.getScript());
					break;
				case VAR:
					if (var.getLocation() != null && parser.getCurrentFunc() != null && var.getFunction() == parser.getCurrentFunc()) {
						int locationUsed = parser.getCurrentFunc().getBody().getOffset()+this.getExprStart();
						if (locationUsed < var.getLocation().getOffset())
							parser.warningWithCode(ParserErrorCode.VarUsedBeforeItsDeclaration, this, var.getName());
					}
					break;
			}
		}
	}

	public static IStoredTypeInformation createStoredTypeInformation(C4Declaration declaration) {
		if (declaration != null) {
			return new GenericStoredTypeInformation(new AccessVar(declaration));
		} else {
			return null;
		}
	}
	
	@Override
	public IType getType(C4ScriptParser context) {
		C4Declaration d = getDeclaration(context);
		// getDeclaration(context) ensures that declaration is not null (if there is actually a variable) which is needed for queryTypeOfExpression for example
		if (d == C4Variable.THIS)
			return context.getContainerObject() != null ? context.getContainerObject() : C4Type.OBJECT;
		IType stored = context.queryTypeOfExpression(this, null);
		if (stored != null)
			return stored;
		if (d instanceof C4Variable) {
			C4Variable v = (C4Variable) d;
			if (v.getObjectType() != null)
				return v.getObjectType();
			else
				return v.getType();
		}
		return C4Type.UNKNOWN;
	}

	@Override
	public void expectedToBeOfType(IType type, C4ScriptParser context, TypeExpectancyMode mode, ParserErrorCode errorWhenFailed) {
		if (getDeclaration() == C4Variable.THIS)
			return;
		super.expectedToBeOfType(type, context, mode, errorWhenFailed);
	}

	@Override
	public void inferTypeFromAssignment(ExprElm expression, C4ScriptParser context) {
		if (getDeclaration() == C4Variable.THIS)
			return;
		super.inferTypeFromAssignment(expression, context);
	}
	
	private static C4Object getObjectBelongingToStaticVar(C4Variable var) {
		C4Declaration parent = var.getParentDeclaration();
		if (parent instanceof C4Object && ((C4Object)parent).getStaticVariable() == var)
			return (C4Object) parent;
		else
			return null;
	}

	@Override
	public Object evaluateAtParseTime(C4ScriptBase context) {
		C4Object obj;
		if (declaration instanceof C4Variable) {
			C4Variable var = (C4Variable) declaration;
			if (var.getScope() == C4VariableScope.CONST) {
				Object val = var.getDefaultValue();
				if (val == null)
					val = 1337; // awesome fallback
				return val;
			}
			else if ((obj = getObjectBelongingToStaticVar(var)) != null) {
				return obj.getId(); // just return the id
			}
		}
		return super.evaluateAtParseTime(context);
	}

	public boolean constCondition() {
		return declaration instanceof C4Variable && ((C4Variable)declaration).getScope() == C4VariableScope.CONST;
	}
	
	@Override
	public Object evaluate(IEvaluationContext context) throws ControlFlowException {
		if (context != null) {
			return context.getValueForVariable(getDeclarationName());
		}
		else {
			return super.evaluate(context);
		}
	}
	
	@Override
	public boolean isConstant() {
		if (getDeclaration() instanceof C4Variable) {
			C4Variable var = (C4Variable) getDeclaration();
			// naturally, consts are constant
			return var.getScope() == C4VariableScope.CONST || getObjectBelongingToStaticVar(var) != null;
		}
		else
			return false;
	}
	
	@Override
	public Class<? extends C4Declaration> declarationClass() {
		return C4Variable.class;
	}

}