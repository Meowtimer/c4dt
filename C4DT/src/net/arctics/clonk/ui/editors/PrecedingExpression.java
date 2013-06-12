package net.arctics.clonk.ui.editors;

import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.defaulting;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.DeclMask;
import net.arctics.clonk.ast.ExpressionLocator;
import net.arctics.clonk.ast.Sequence;
import net.arctics.clonk.ast.TraversalContinuation;
import net.arctics.clonk.c4script.Function;
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
	public final Function function;
	public void pos(int pos) { this.exprRegion = new Region(pos, 0); exprAtRegion = null; }
	public PrecedingExpression(Function function) { super(-1); this.function = function; }
	public PrecedingExpression(ASTNode contextExpression, Sequence contextSequence, IType precedingType) {
		super(-1);
		this.contextExpression = contextExpression;
		this.contextSequence = contextSequence;
		this.precedingType = precedingType;
		this.function = null;
	}
	@Override
	public TraversalContinuation visitNode(ASTNode expression, ProblemReporter context) {
		if (context.function() != this.function)
			return TraversalContinuation.Cancel;
		final TraversalContinuation c = super.visitNode(expression, context);
		if (exprAtRegion != null) {
			contextExpression = exprAtRegion;
			if (
				contextExpression instanceof MemberOperator ||
				(contextExpression instanceof AccessDeclaration &&
				 Utilities.regionContainsOffset(contextExpression.identifierRegion(), exprRegion.getOffset()))
			) {
				// we only care about sequences
				final ASTNode pred = contextExpression.predecessorInSequence();
				contextSequence = pred != null ? Utilities.as(contextExpression.parent(), Sequence.class) : null;
				if (contextSequence != null)
					contextSequence = contextSequence.subSequenceIncluding(contextExpression);
				precedingType = pred != null ? context.typeOf(pred) : null;
			}
		} else
			super.visitNode(expression, context);
		return c;
	}
	public IType precedingType() { return defaulting(precedingType, PrimitiveType.UNKNOWN); }
	public int declarationsMask() {
		int mask = DeclMask.VARIABLES|DeclMask.FUNCTIONS;
		if (contextSequence == null)
			mask |= DeclMask.STATIC_VARIABLES;

		final ASTNode mo = memberOperator();
		if (mo instanceof MemberOperator)
			if (((MemberOperator) mo).dotNotation())
				return DeclMask.VARIABLES;
			else
				mask &= ~DeclMask.VARIABLES;

		return mask;
	}
	public MemberOperator memberOperator() {
		if (contextExpression instanceof MemberOperator)
			return (MemberOperator)contextExpression;
		else if (contextExpression != null)
			return as(contextExpression.predecessorInSequence(), MemberOperator.class);
		else
			return null;
	}
}