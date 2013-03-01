package net.arctics.clonk.parser.c4script.inference.dabble;

import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.ast.AccessVar;
import net.arctics.clonk.parser.c4script.ast.CallDeclaration;
import net.arctics.clonk.parser.c4script.inference.dabble.DabbleInference.Visitor;

final class VarFunctionsTypeVariable extends TypeVariable {
	private final Function scope;
	private final Function varFunction;
	private final long varIndex;

	VarFunctionsTypeVariable(Function scope, Function function, long val) {
		this.scope = scope;
		this.varFunction = function;
		this.varIndex = val;
	}

	@Override
	public boolean binds(ASTNode node, Visitor visitor) {
		Function fn = node.parentOfType(Function.class);
		if (scope != null && fn != scope)
			return false;
		if (node instanceof CallDeclaration) {
			CallDeclaration callFunc = (CallDeclaration) node;
			Object ev;
			return
				callFunc.declaration() == varFunction &&
				callFunc.params().length == 1 && // don't bother with more complex cases
				((ev = callFunc.params()[0].evaluateStatic(fn)) != null) &&
				ev.equals(varIndex);
		} else if (node instanceof AccessVar) {
			AccessVar accessVar = (AccessVar) node;
			if (accessVar.declaration() instanceof Variable)
				return fn != null && (varFunction == visitor.cachedEngineDeclarations().Par
					? fn.parameters()
					: fn.locals()).indexOf(accessVar) == varIndex;
		}
		return false;
	}

	@Override
	public boolean same(ITypeVariable other) {
		if (other.getClass() == VarFunctionsTypeVariable.class) {
			VarFunctionsTypeVariable otherInfo = (VarFunctionsTypeVariable) other;
			return otherInfo.scope == this.scope && otherInfo.varFunction == this.varFunction && otherInfo.varIndex == this.varIndex;
		}
		else
			return false;
	}

	@Override
	public String toString() {
		return String.format("[%s(%d): %s in %s]", varFunction.name(), varIndex, get().typeName(true), scope.name()); //$NON-NLS-1$
	}

	@Override
	public Declaration declaration(Visitor visitor) { return null; }
}