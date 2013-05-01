package net.arctics.clonk.c4script.ast;

import static net.arctics.clonk.util.Utilities.as;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ITransformer;
import net.arctics.clonk.c4script.ProblemReporter;

public class Tidy implements ITransformer {
	public final ProblemReporter reporter;
	public Tidy(ProblemReporter reporter) { this.reporter = reporter; }
	@Override
	public Object transform(ASTNode prev, Object prevT, ASTNode expression) {
		try {
			return innerTidy(expression);
		} catch (final CloneNotSupportedException e) {
			e.printStackTrace();
			return expression;
		}
	}
	public ASTNode tidy(ASTNode node) throws CloneNotSupportedException {
		return innerTidy(node);
	}
	private ASTNode innerTidy(ASTNode node) throws CloneNotSupportedException {
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
	public ASTNode tidyExhaustive(ASTNode node) throws CloneNotSupportedException {
		ASTNode repl;
		for (ASTNode original = node; (repl = tidy(original)) != original; original = repl);
		return repl;
	}
}
