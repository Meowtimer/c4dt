package net.arctics.clonk.parser;

public interface IASTVisitor<T> {
	public TraversalContinuation visitNode(ASTNode node, T context);
}