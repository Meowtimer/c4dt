package net.arctics.clonk.ast;

@FunctionalInterface
public interface IASTVisitor<T> {
	TraversalContinuation visitNode(ASTNode node, T context);
}