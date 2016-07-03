package net.arctics.clonk.ui.editors.c4script;

import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.c4script.ast.Statement;

class Replacement {
	private String title;
	public final ASTNode replacementExpression;
	public final ASTNode[] specifiable;
	public final List<Declaration> additionalDeclarations = new LinkedList<Declaration>();
	public boolean regionToBeReplacedSpecifiedByReplacementExpression; // yes!
	public Replacement(final String title, final ASTNode replacementExpression, final ASTNode... specifiable) {
		super();
		this.title = title;
		this.replacementExpression = replacementExpression;
		this.specifiable = specifiable;
		this.regionToBeReplacedSpecifiedByReplacementExpression = !(replacementExpression instanceof Statement);
	}
	public String title() { return title; }
	public void setTitle(final String title) { this.title = title; }
	public ASTNode replacementExpression() { return replacementExpression; }
	public ASTNode[] specifiable() { return specifiable; }
	public List<Declaration> additionalDeclarations() { return additionalDeclarations; }
	@Override
	public boolean equals(final Object other) {
		if (other instanceof Replacement) {
			final Replacement otherR = (Replacement) other;
			return otherR.title.equals(title) && otherR.replacementExpression.equals(replacementExpression);
		} else
			return false;
	}
	@Override
	public String toString() {
		return String.format("%s: %s", title, replacementExpression.toString()); //$NON-NLS-1$
	}
	/**
	 * Method to override if performing additional actions apart from replacing code segments is desired.
	 */
	public void performAdditionalActionsBeforeDoingReplacements() {}
}

