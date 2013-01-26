package net.arctics.clonk.parser.c4script.inference.dabble;

import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.ast.AccessVar;
import net.arctics.clonk.parser.c4script.ast.CallDeclaration;
import net.arctics.clonk.parser.c4script.inference.dabble.DabbleInference.ScriptProcessor;

final class VarFunctionsTypeInfo extends TypeInfo {
	private final Function scope;
	private final Function varFunction;
	private final long varIndex;

	VarFunctionsTypeInfo(Function scope, Function function, long val) {
		this.scope = scope;
		this.varFunction = function;
		this.varIndex = val;
	}

	@Override
	public boolean storesTypeInformationFor(ASTNode node, ScriptProcessor processor) {
		Function fn = node.parentOfType(Function.class);
		if (scope != null && fn != scope)
			return false;
		if (node instanceof CallDeclaration) {
			CallDeclaration callFunc = (CallDeclaration) node;
			Object ev;
			return
				callFunc.declaration() == varFunction &&
				callFunc.params().length == 1 && // don't bother with more complex cases
				((ev = callFunc.params()[0].evaluateAtParseTime(fn)) != null) &&
				ev.equals(varIndex);
		} else if (node instanceof AccessVar) {
			AccessVar accessVar = (AccessVar) node;
			if (accessVar.declaration() instanceof Variable)
				return fn != null && (varFunction == processor.cachedEngineDeclarations().Par
					? fn.parameters()
					: fn.localVars()).indexOf(accessVar) == varIndex;
		}
		return false;
	}

	@Override
	public boolean refersToSameExpression(ITypeInfo other) {
		if (other.getClass() == VarFunctionsTypeInfo.class) {
			VarFunctionsTypeInfo otherInfo = (VarFunctionsTypeInfo) other;
			return otherInfo.scope == this.scope && otherInfo.varFunction == this.varFunction && otherInfo.varIndex == this.varIndex;
		}
		else
			return false;
	}

	@Override
	public String toString() {
		return String.format("[%s(%d): %s in %s]", varFunction.name(), varIndex, type().typeName(true), scope.name()); //$NON-NLS-1$
	}
}