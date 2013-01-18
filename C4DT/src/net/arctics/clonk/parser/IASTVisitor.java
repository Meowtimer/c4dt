package net.arctics.clonk.parser;

public interface IASTVisitor<T> {
	public TraversalContinuation visitExpression(ASTNode expression, T parser);
}