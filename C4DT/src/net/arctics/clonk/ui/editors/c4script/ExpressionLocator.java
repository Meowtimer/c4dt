package net.arctics.clonk.ui.editors.c4script;

import net.arctics.clonk.parser.ExprElm;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.ast.IASTVisitor;
import net.arctics.clonk.parser.c4script.ast.TraversalContinuation;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

/**
 * Helper class for obtaining the expression at a specified region
 * @author madeen
 *
 */
public class ExpressionLocator implements IASTVisitor {
	
	protected ExprElm exprAtRegion;
	protected ExprElm topLevelInRegion;
	protected IRegion exprRegion;
	
	// derived classes don't need to call the one-arg constructor
	public ExpressionLocator() {}
	
	public ExprElm getTopLevelInRegion() {
		return topLevelInRegion;
	}

	public ExpressionLocator(IRegion exprRegion) {
		this.exprRegion = exprRegion;
	}

	public ExpressionLocator(int pos) {
		this(new Region(pos, 0));
	}

	public ExprElm expressionAtRegion() {
		return exprAtRegion;
	}

	@Override
	public TraversalContinuation visitExpression(ExprElm expression, C4ScriptParser parser) {
		expression.traverse(new IASTVisitor() {
			@Override
			public TraversalContinuation visitExpression(ExprElm expression, C4ScriptParser parser) {
				if (exprRegion.getOffset() >= expression.start() && exprRegion.getOffset() <= expression.end()) {
					if (topLevelInRegion == null)
						topLevelInRegion = expression;
					exprAtRegion = expression;
					return TraversalContinuation.TraverseSubElements;
				}
				return TraversalContinuation.Continue;
			}
		}, parser);
		return exprAtRegion != null ? TraversalContinuation.Cancel : TraversalContinuation.Continue;
	}

}
