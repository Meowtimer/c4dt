package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.c4script.C4ScriptParser;

public interface IASTVisitor {
	public TraversalContinuation visitExpression(ASTNode expression, C4ScriptParser parser);
}