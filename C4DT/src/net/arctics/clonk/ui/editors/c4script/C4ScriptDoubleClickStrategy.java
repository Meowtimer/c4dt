package net.arctics.clonk.ui.editors.c4script;

import org.eclipse.jface.text.DefaultTextDoubleClickStrategy;
import org.eclipse.jface.text.ITextViewer;

import net.arctics.clonk.parser.c4script.C4Function;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.util.Utilities;
import net.arctics.clonk.parser.c4script.C4ScriptParser.ExpressionsAndStatementsReportingFlavour;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.StringLiteral;

public class C4ScriptDoubleClickStrategy extends DefaultTextDoubleClickStrategy {
	
	private C4ScriptSourceViewerConfiguration configuration;
	
	public C4ScriptDoubleClickStrategy(
			C4ScriptSourceViewerConfiguration configuration) {
		this.configuration = configuration;
	}
	public void doubleClicked(ITextViewer viewer) {
		int pos = viewer.getSelectedRange().x;

		if (pos < 0)
			return;
		
		C4ScriptBase script = Utilities.getScriptForEditor(configuration.getEditor());
		C4Function func = script.funcAt(pos);
		if (func != null) {
			ExpressionLocator locator = new ExpressionLocator(pos-func.getBody().getOffset());
			C4ScriptParser.reportExpressionsAndStatementsWithSpecificFlavour(viewer.getDocument(), script, func, locator, null, ExpressionsAndStatementsReportingFlavour.AlsoStatements, false);
			ExprElm expr = locator.getExprAtRegion();
			if (expr instanceof StringLiteral) {
				viewer.setSelectedRange(func.getBody().getOffset()+expr.getExprStart()+1, expr.getLength()-2);
				return;
			}
		}
		super.doubleClicked(viewer);
	}
}