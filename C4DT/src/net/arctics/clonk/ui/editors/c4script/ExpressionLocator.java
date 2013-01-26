package net.arctics.clonk.ui.editors.c4script;

import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.IASTVisitor;
import net.arctics.clonk.parser.TraversalContinuation;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

/**
 * Helper class for obtaining the expression at a specified region
 * @author madeen
 *
 */
public class ExpressionLocator implements IASTVisitor<Object> {
	
	protected ASTNode exprAtRegion;
	protected ASTNode topLevelInRegion;
	protected IRegion exprRegion;
	
	// derived classes don't need to call the one-arg constructor
	public ExpressionLocator() {}
	
	public ASTNode getTopLevelInRegion() {
		return topLevelInRegion;
	}

	public ExpressionLocator(IRegion exprRegion) {
		this.exprRegion = exprRegion;
	}

	public ExpressionLocator(int pos) {
		this(new Region(pos, 0));
	}

	public ASTNode expressionAtRegion() {
		return exprAtRegion;
	}

	@Override
	public TraversalContinuation visitNode(ASTNode expression, Object context) {
		expression.traverse(new IASTVisitor<Object>() {
			@Override
			public TraversalContinuation visitNode(ASTNode expression, Object context) {
				if (exprRegion.getOffset() >= expression.start() && exprRegion.getOffset() <= expression.end()) {
					if (topLevelInRegion == null)
						topLevelInRegion = expression;
					exprAtRegion = expression;
					return TraversalContinuation.TraverseSubElements;
				}
				return TraversalContinuation.Continue;
			}
		}, context);
		return exprAtRegion != null ? TraversalContinuation.Cancel : TraversalContinuation.Continue;
	}

}
