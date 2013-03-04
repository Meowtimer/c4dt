package net.arctics.clonk.parser.c4script.inference.dabble;

import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.ast.AccessVar;
import net.arctics.clonk.parser.c4script.ast.TypingJudgementMode;
import net.arctics.clonk.parser.c4script.inference.dabble.DabbleInference.Visitor;

public class VariableTypeVariable extends TypeVariable {
	private final Variable variable;
	public Variable variable() { return variable; }
	public VariableTypeVariable(Variable variable) {
		this.variable = variable;
		this.type = PrimitiveType.UNKNOWN;
	}
	public VariableTypeVariable(AccessVar origin) { this((Variable) origin.declaration()); }
	@Override
	public void apply(boolean soft, Visitor visitor) {
		variable.expectedToBeOfType(type, TypingJudgementMode.Expect);
	}
	@Override
	public String toString() {
		return String.format("[%s: %s]", variable.name(), get().typeName(true));
	}
	@Override
	public Declaration declaration() { return variable; }
	@Override
	public Declaration key() { return variable; }
}