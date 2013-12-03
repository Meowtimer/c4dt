package net.arctics.clonk.c4script.ast;

import static net.arctics.clonk.util.Utilities.as;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ITransformer;
import net.arctics.clonk.ast.Structure;

public class Tidy implements ITransformer {
	private final Structure structure;
	private final int strictLevel;
	public Tidy(final Structure structure, final int strictLevel) {
		this.structure = structure;
		this.strictLevel = strictLevel;
	}
	public int strictLevel() { return strictLevel; }
	public Structure structure() { return structure; }
	@Override
	public Object transform(final ASTNode prev, final Object prevT, final ASTNode expression) {
		try {
			return innerTidy(expression);
		} catch (final CloneNotSupportedException e) {
			e.printStackTrace();
			return expression;
		}
	}
	public ASTNode tidy(final ASTNode node) throws CloneNotSupportedException {
		return innerTidy(node);
	}
	private ASTNode innerTidy(ASTNode node) throws CloneNotSupportedException {
		if (node == null)
			return null;
		final ITidyable tidyable = as(node, ITidyable.class);
		if (tidyable != null)
			node = tidyable.tidy(this);
		return node.transformSubElements(this);
	}
	/**
	 * Keeps applying optimize to the expression and its modified versions until an expression and its replacement are identical e.g. there is nothing to be modified anymore
	 * @param context
	 * @return
	 * @throws CloneNotSupportedException
	 */
	public ASTNode tidyExhaustive(final ASTNode node) throws CloneNotSupportedException {
		ASTNode repl;
		for (ASTNode original = node; (repl = tidy(original)) != original; original = repl);
		return repl;
	}
}
