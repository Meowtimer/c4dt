package net.arctics.clonk.ui.editors.c4script;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.ExpressionListener;
import net.arctics.clonk.parser.c4script.ast.TraversalContinuation;

/**
 * Helper class for obtaining the expression at a specified region
 * @author madeen
 *
 */
public class ExpressionLocator extends ExpressionListener {
	
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

	public ExprElm getExprAtRegion() {
		return exprAtRegion;
	}

	public TraversalContinuation expressionDetected(ExprElm expression, C4ScriptParser parser) {
		expression.traverse(new ExpressionListener() {
			public TraversalContinuation expressionDetected(ExprElm expression, C4ScriptParser parser) {
				if (exprRegion.getOffset() >= expression.getExprStart() && exprRegion.getOffset() < expression.getExprEnd()) {
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
