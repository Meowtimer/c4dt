package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.parser.ExprElm;
import net.arctics.clonk.parser.c4script.C4ScriptParser;

public interface IASTVisitor {
	public TraversalContinuation visitExpression(ExprElm expression, C4ScriptParser parser);
}