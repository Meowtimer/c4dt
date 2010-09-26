package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.C4Object;
import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.C4Type;
import net.arctics.clonk.parser.c4script.C4TypeSet;
import net.arctics.clonk.parser.c4script.C4Variable;
import net.arctics.clonk.parser.c4script.FindDeclarationInfo;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.C4Function.C4FunctionScope;
import net.arctics.clonk.parser.c4script.C4Variable.C4VariableScope;
import net.arctics.clonk.parser.c4script.ast.evaluate.IEvaluationContext;

public class AccessVar extends AccessDeclaration {

	/**
	 * 
	 */
	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	private static final class VariableTypeInformation extends StoredTypeInformation {
		private C4Declaration decl;

		public VariableTypeInformation(C4Declaration varDeclaration) {
			this.decl = varDeclaration;
			if (varDeclaration instanceof C4Variable) {
				C4Variable var = (C4Variable) varDeclaration;
				storeType(C4TypeSet.create(var.getType(), var.getObjectType()));
			}
		}

		public boolean expressionRelevant(ExprElm expr, C4ScriptParser parser) {
			// obj-> asks for the relevance of obj
			if (expr instanceof Sequence) {
				Sequence seq = (Sequence) expr;
				if (seq.getLastElement() instanceof MemberOperator && seq.getLastElement().getPredecessorInSequence() instanceof AccessVar)
					expr = seq.getLastElement().getPredecessorInSequence();
				else
					return false;
			}
			return expr instanceof AccessVar && ((AccessVar)expr).getDeclaration() == decl;
		}

		@Override
		public void apply(boolean soft) {
			if (decl == null)
				return;
			decl = decl.latestVersion(); 
			if (decl instanceof C4Variable) {
				C4Variable var = (C4Variable) decl;
				if (!soft || var.getScope() == C4VariableScope.VAR) {
					// for serialization, split static and non-static types again
					var.setType(getType());
				}
			}
		}

		public boolean sameExpression(IStoredTypeInformation other) {
			return other.getClass() == AccessVar.VariableTypeInformation.class && ((AccessVar.VariableTypeInformation)other).decl == decl;
		}

		@Override
		public String toString() {
			return "variable " + decl.getName() + " " + super.toString(); //$NON-NLS-1$ //$NON-NLS-2$
		}

	}

	@Override
	public boolean modifiable(C4ScriptParser context) {
		return declaration == null || ((C4Variable)declaration).getScope() != C4VariableScope.CONST;
	}

	public AccessVar(String varName) {
		super(varName);
	}

	public AccessVar(C4Variable v) {
		this(v.getName());
		this.declaration = v;
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
		FindDeclarationInfo info = new FindDeclarationInfo(parser.getContainer().getIndex());
		info.setContextFunction(parser.getActiveFunc());
		ExprElm p = getPredecessorInSequence();
		C4ScriptBase lookIn = p == null ? parser.getContainer() : p.guessObjectType(parser);
		if (lookIn == null)
			return null;
		info.setSearchOrigin(lookIn);
		return lookIn.findVariable(declarationName, info);
	}

	@Override
	public void reportErrors(C4ScriptParser parser) throws ParsingException {
		super.reportErrors(parser);
		if (declaration == null && getPredecessorInSequence() == null)
			parser.errorWithCode(ParserErrorCode.UndeclaredIdentifier, this, true, declarationName);
		// local variable used in global function
		else if (declaration instanceof C4Variable) {
			C4Variable var = (C4Variable) declaration;
			switch (var.getScope()) {
				case LOCAL:
					if (
						(parser.getActiveFunc() != null && parser.getActiveFunc().getVisibility() == C4FunctionScope.GLOBAL) ||
						// initialization a non-local variable with local values -> fail
						(parser.getActiveScriptScopeVariable() != null && parser.getActiveScriptScopeVariable().getScope() != C4VariableScope.LOCAL)
					) {
						parser.errorWithCode(ParserErrorCode.LocalUsedInGlobal, this, true);
					}
					break;
				case STATIC:
					parser.getContainer().addUsedProjectScript(var.getScript());
					break;
				case VAR:
					if (var.getLocation() != null) {
						int locationUsed = parser.getActiveFunc().getBody().getOffset()+this.getExprStart();
						if (locationUsed < var.getLocation().getOffset())
							parser.warningWithCode(ParserErrorCode.VarUsedBeforeItsDeclaration, this, var.getName());
					}
					break;
			}
		}
	}

	public static IStoredTypeInformation createStoredTypeInformation(C4Declaration declaration) {
		return new VariableTypeInformation(declaration);
	}

	@Override
	public IStoredTypeInformation createStoredTypeInformation(C4ScriptParser parser) {
		return createStoredTypeInformation(getDeclaration());
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
			if (var.getScope() == C4VariableScope.CONST)
				return var.getConstValue();
			else if ((obj = getObjectBelongingToStaticVar(var)) != null)
				return obj.getId(); // just return the id
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

}