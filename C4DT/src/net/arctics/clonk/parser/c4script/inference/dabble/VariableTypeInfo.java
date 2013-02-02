package net.arctics.clonk.parser.c4script.inference.dabble;

import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.ast.AccessVar;
import net.arctics.clonk.parser.c4script.ast.TypingJudgementMode;
import net.arctics.clonk.parser.c4script.inference.dabble.DabbleInference.ScriptProcessor;

public class VariableTypeInfo extends TypeInfo {
	private final Variable variable;
	public Variable variable() { return variable; }
	public VariableTypeInfo(AccessVar origin) {
		this.variable = (Variable)origin.declaration();
		this.type = PrimitiveType.UNKNOWN;
	}
	@Override
	public boolean storesTypeInformationFor(ASTNode expr, ScriptProcessor processor) {
		return expr instanceof AccessVar && ((AccessVar)expr).declaration() == variable;
	}
	@Override
	public boolean refersToSameExpression(ITypeInfo other) {
		return
			other instanceof VariableTypeInfo &&
			((VariableTypeInfo)other).variable == this.variable;
	}
	@Override
	public void apply(boolean soft, ScriptProcessor processor) {
		variable.expectedToBeOfType(type, TypingJudgementMode.Expect);
	}
	@Override
	public String toString() {
		return String.format("[%s: %s]", variable.name(), type().typeName(true));
	}
	@Override
	public Declaration declaration(ScriptProcessor processor) { return variable; }
}