package net.arctics.clonk.ast;

public interface IASTVisitorNoContext extends IASTVisitor<Object> {
	TraversalContinuation visitNode(ASTNode node);
	@Override
	default TraversalContinuation visitNode(ASTNode node, Object context) { return visitNode(node); }
}
