package net.arctics.clonk.ui.editors;

import static net.arctics.clonk.util.Utilities.defaulting;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.DeclMask;
import net.arctics.clonk.ast.ExpressionLocator;
import net.arctics.clonk.ast.Sequence;
import net.arctics.clonk.ast.TraversalContinuation;
import net.arctics.clonk.c4script.ProblemReporter;
import net.arctics.clonk.c4script.ast.AccessDeclaration;
import net.arctics.clonk.c4script.ast.MemberOperator;
import net.arctics.clonk.c4script.typing.IType;
import net.arctics.clonk.c4script.typing.PrimitiveType;
import net.arctics.clonk.util.Utilities;

import org.eclipse.jface.text.Region;

public class PrecedingExpression extends ExpressionLocator<ProblemReporter> {
	public ASTNode contextExpression;
	public Sequence contextSequence;
	public IType precedingType;
	public void pos(int pos) { this.exprRegion = new Region(pos, 0); exprAtRegion = null; }
	public PrecedingExpression() { super(-1); }
	public PrecedingExpression(ASTNode contextExpression, Sequence contextSequence, IType precedingType) {
		super(-1);
		this.contextExpression = contextExpression;
		this.contextSequence = contextSequence;
		this.precedingType = precedingType;
	}
	@Override
	public TraversalContinuation visitNode(ASTNode expression, ProblemReporter context) {
		final ASTNode old = exprAtRegion;
		final TraversalContinuation c = super.visitNode(expression, context);
		if (old != exprAtRegion) {
			contextExpression = exprAtRegion;
			if (
				contextExpression instanceof MemberOperator ||
				(contextExpression instanceof AccessDeclaration && Utilities.regionContainsOffset(contextExpression.identifierRegion(), exprRegion.getOffset()))
			) {
				// we only care about sequences
				final ASTNode pred = contextExpression.predecessorInSequence();
				contextSequence = pred != null ? Utilities.as(contextExpression.parent(), Sequence.class) : null;
				if (contextSequence != null)
					contextSequence = contextSequence.subSequenceIncluding(contextExpression);
				precedingType = pred != null ? context.typeOf(pred) : null;
			}
		}
		return c;
	}
	public IType precedingType() { return defaulting(precedingType, PrimitiveType.UNKNOWN); }
	public int declarationsMask() {
		int mask = 0;
		if (contextSequence == null || MemberOperator.endsWithDot(contextSequence))
			mask |= DeclMask.VARIABLES;
		if (contextSequence == null || !MemberOperator.endsWithDot(contextSequence))
			mask |= DeclMask.FUNCTIONS;
		if (contextSequence == null)
			mask |= DeclMask.STATIC_VARIABLES;

		final ASTNode pred =
			contextExpression instanceof MemberOperator ? contextExpression :
			contextExpression != null ? contextExpression.predecessorInSequence() : null;

		if (pred instanceof MemberOperator) {
			final MemberOperator mo = (MemberOperator) pred;
			if (mo.dotNotation())
				return DeclMask.VARIABLES;
			else
				mask &= ~DeclMask.VARIABLES;
		}

		return mask;
	}
}