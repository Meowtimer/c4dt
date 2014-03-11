package net.arctics.clonk.ast;

@FunctionalInterface
public interface IASTVisitor<T> {
	public TraversalContinuation visitNode(ASTNode node, T context);
}