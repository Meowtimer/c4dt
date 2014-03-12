package net.arctics.clonk.ast;


import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

/**
 * Helper class for obtaining the expression at a specified region
 * @author madeen
 *
 */
public class ExpressionLocator<T> implements IASTVisitor<T> {
	protected ASTNode exprAtRegion;
	protected ASTNode topLevelInRegion;
	protected IRegion exprRegion;
	// derived classes don't need to call the one-arg constructor
	public ExpressionLocator() {}
	public ASTNode topLevelInRegion() { return topLevelInRegion; }
	public ExpressionLocator(final IRegion exprRegion) { this.exprRegion = exprRegion; }
	public ExpressionLocator(final int pos) { this(new Region(pos, 0)); }
	public ASTNode expressionAtRegion() { return exprAtRegion; }
	@Override
	public TraversalContinuation visitNode(final ASTNode expression, final Object _context) {
		expression.traverse((xpr, context) -> {
			if (exprRegion.getOffset() >= xpr.start() && exprRegion.getOffset() <= xpr.end()) {
				if (topLevelInRegion == null)
					topLevelInRegion = xpr;
				exprAtRegion = xpr;
				return TraversalContinuation.TraverseSubElements;
			}
			return TraversalContinuation.Continue;
		}, _context);
		return exprAtRegion != null ? TraversalContinuation.Cancel : TraversalContinuation.Continue;
	}
}
