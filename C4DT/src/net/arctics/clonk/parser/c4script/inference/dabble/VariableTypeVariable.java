package net.arctics.clonk.parser.c4script.inference.dabble;

import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.ast.AccessVar;
import net.arctics.clonk.parser.c4script.ast.TypingJudgementMode;
import net.arctics.clonk.parser.c4script.inference.dabble.DabbleInference.Visitor;

public class VariableTypeVariable extends TypeVariable {
	private final Variable variable;
	public Variable variable() { return variable; }
	public VariableTypeVariable(AccessVar origin) {
		this.variable = (Variable)origin.declaration();
		this.type = PrimitiveType.UNKNOWN;
	}
	@Override
	public boolean binds(ASTNode expr, Visitor visitor) {
		return expr instanceof AccessVar && ((AccessVar)expr).declaration() == variable;
	}
	@Override
	public boolean same(ITypeVariable other) {
		return
			other instanceof VariableTypeVariable &&
			((VariableTypeVariable)other).variable == this.variable;
	}
	@Override
	public void apply(boolean soft, Visitor visitor) {
		variable.expectedToBeOfType(type, TypingJudgementMode.Expect);
	}
	@Override
	public String toString() {
		return String.format("[%s: %s]", variable.name(), get().typeName(true));
	}
	@Override
	public Declaration declaration(Visitor visitor) { return variable; }
}