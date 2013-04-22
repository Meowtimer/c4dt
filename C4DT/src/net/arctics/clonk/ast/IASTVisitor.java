package net.arctics.clonk.ast;

public interface IASTVisitor<T> {
	public TraversalContinuation visitNode(ASTNode node, T context);
}