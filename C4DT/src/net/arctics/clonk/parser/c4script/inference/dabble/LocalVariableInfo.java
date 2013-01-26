package net.arctics.clonk.parser.c4script.inference.dabble;

import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.ast.AccessVar;
import net.arctics.clonk.parser.c4script.ast.TypingJudgementMode;
import net.arctics.clonk.parser.c4script.inference.dabble.DabbleInference.ScriptProcessor;

public class LocalVariableInfo extends TypeInfo {
	private final AccessVar origin;
	public LocalVariableInfo(AccessVar origin) {
		this.origin = origin;
		this.type = PrimitiveType.UNKNOWN;
	}
	public AccessVar origin() { return origin; }
	@Override
	public boolean storesTypeInformationFor(ASTNode expr, ScriptProcessor processor) {
		return expr instanceof AccessVar && ((AccessVar)expr).declaration() == origin.declaration();
	}
	@Override
	public boolean refersToSameExpression(ITypeInfo other) {
		return
			other instanceof LocalVariableInfo &&
			((LocalVariableInfo)other).origin().declaration() == origin.declaration();
	}
	@Override
	public void apply(boolean soft, ScriptProcessor processor) {
		Variable v = (Variable)origin().declaration();
		v.expectedToBeOfType(type, TypingJudgementMode.Expect);
	}
	@Override
	public String toString() {
		return String.format("[%s: %s]", origin.declarationName(), type().typeName(true));
	}
}