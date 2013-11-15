package net.arctics.clonk.c4script.typing.dabble;

import java.util.LinkedList;

import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.IASTVisitor;
import net.arctics.clonk.ast.TraversalContinuation;

@SuppressWarnings({ "serial", "unchecked" })
public class ASTNodeGatherer<T extends ASTNode> extends LinkedList<T> implements IASTVisitor<Void> {
	private final Class<T> cls;
	public ASTNodeGatherer(Class<T> cls) { this.cls = cls; }
	public static <T extends ASTNode> ASTNodeGatherer<T> create(Class<T> cls) { return new ASTNodeGatherer<>(cls); }
	@Override
	public TraversalContinuation visitNode(ASTNode node, Void context) {
		if (cls.isInstance(node))
			this.add((T)node);
		return TraversalContinuation.Continue;
	}
}
