package net.arctics.clonk.ui.editors.c4script;

import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.C4ScriptParser.ExpressionsAndStatementsReportingFlavour;
import net.arctics.clonk.parser.c4script.ast.AccessDeclaration;
import net.arctics.clonk.parser.c4script.ast.Block;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.PropListExpression;
import net.arctics.clonk.parser.c4script.ast.StringLiteral;
import net.arctics.clonk.util.Utilities;

import org.eclipse.jface.text.DefaultTextDoubleClickStrategy;
import org.eclipse.jface.text.ITextViewer;

public class C4ScriptDoubleClickStrategy extends DefaultTextDoubleClickStrategy {
	
	private C4ScriptSourceViewerConfiguration configuration;
	
	public C4ScriptDoubleClickStrategy(
			C4ScriptSourceViewerConfiguration configuration) {
		this.configuration = configuration;
	}
	
	@Override
	public void doubleClicked(ITextViewer viewer) {
		int pos = viewer.getSelectedRange().x;

		if (pos < 0)
			return;
		
		try {
			if (Character.isLetterOrDigit(configuration.getEditor().getDocumentProvider().getDocument(configuration.getEditor().getEditorInput()).getChar(pos))) {
				super.doubleClicked(viewer);
				return;
			}
		} catch (Exception e) {
		}

		Script script = Utilities.scriptForEditor(configuration.getEditor());
		Function func = script.funcAt(pos);
		if (func != null) {
			ExpressionLocator locator = new ExpressionLocator(pos-func.getBody().getOffset());
			C4ScriptParser.reportExpressionsAndStatements(viewer.getDocument(), script, func, locator, null, ExpressionsAndStatementsReportingFlavour.AlsoStatements, false);
			ExprElm expr = locator.getExprAtRegion();
			if (expr instanceof StringLiteral) {
				viewer.setSelectedRange(func.getBody().getOffset()+expr.getExprStart()+1, expr.getLength()-2);
				return;
			} else if (expr instanceof AccessDeclaration) {
				AccessDeclaration accessDec = (AccessDeclaration) expr;
				viewer.setSelectedRange(func.getBody().getOffset()+accessDec.identifierStart(), accessDec.identifierLength());
				return;
			} else if (expr instanceof PropListExpression || expr instanceof Block) {
				viewer.setSelectedRange(expr.getExprStart()+func.getBody().getOffset(), expr.getLength());
				return;
			}
		}
		super.doubleClicked(viewer);
	}
}