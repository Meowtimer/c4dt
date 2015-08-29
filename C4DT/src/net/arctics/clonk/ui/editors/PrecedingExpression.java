package net.arctics.clonk.ui.editors;

import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.defaulting;

import org.eclipse.jface.text.Region;

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

/**
 * Base class for {@link ProposalsSite}, I guess.
 * @author madeen
 *
 */
public class PrecedingExpression extends ExpressionLocator<ProblemReporter> {
	
	public ASTNode contextExpression;
	public Sequence contextSequence;
	public IType precedingType;
	public final Function function;
	public void pos(final int pos) { this.exprRegion = new Region(pos, 0); exprAtRegion = null; }
	
	/**
	 * Create in the context of a function
	 * @param function
	 * @param contextExpression
	 * @param contextSequence
	 * @param precedingType
	 */
	public PrecedingExpression(final Function function, final ASTNode contextExpression, final Sequence contextSequence, final IType precedingType) {
		super(-1);
		this.function = function;
		this.contextExpression = contextExpression;
		this.contextSequence = contextSequence;
		this.precedingType = precedingType;
	}
	
	@Override
	public TraversalContinuation visitNode(final ASTNode expression, final ProblemReporter context) {
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
				final ASTNode pred = contextExpression.predecessor();
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
	
	/**
	 * Decide what kind of declarations to propose after this expression.
	 * @return Mask consisting of {@link DeclMask} fields |-ed together.
	 */
	public int declarationsMask() {
		int mask = DeclMask.VARIABLES|DeclMask.FUNCTIONS;
		if (contextSequence == null)
			mask |= DeclMask.STATIC_VARIABLES;

		final MemberOperator memberOperator = memberOperator();
		if (memberOperator != null && !memberOperator.dotNotation())
			mask &= ~DeclMask.VARIABLES;

		return mask;
	}
	
	public MemberOperator memberOperator() {
		return contextExpression instanceof MemberOperator ?
			(MemberOperator)contextExpression :
			contextExpression != null ? as(contextExpression.predecessor(), MemberOperator.class) :
			null;
	}

}