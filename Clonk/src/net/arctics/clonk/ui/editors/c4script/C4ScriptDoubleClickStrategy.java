package net.arctics.clonk.ui.editors.c4script;

import net.arctics.clonk.parser.c4script.C4Function;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.util.Utilities;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.ExprElm;
import net.arctics.clonk.parser.c4script.C4ScriptParser.ParsingException;

import org.eclipse.jface.text.*;

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
			try {
				ExpressionLocator locator = new ExpressionLocator(pos-func.getBody().getOffset());
				C4ScriptParser.reportExpressionsAndStatements(viewer.getDocument(), func.getBody(), script, func, locator);
				ExprElm expr = locator.getExprAtRegion();
				if (expr != null) {
					viewer.setSelectedRange(func.getBody().getOffset()+expr.getExprStart(), expr.getLength());
					return;
				}
			} catch (BadLocationException e) {
				e.printStackTrace();
			} catch (ParsingException e) {
				//e.printStackTrace();
			}
		}
		super.doubleClicked(viewer);
	}
}