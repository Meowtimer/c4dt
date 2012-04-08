package net.arctics.clonk.ui.editors.c4script;

import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.C4ScriptParser.ExpressionsAndStatementsReportingFlavour;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.ast.AccessDeclaration;
import net.arctics.clonk.parser.c4script.ast.Block;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.KeywordStatement;
import net.arctics.clonk.parser.c4script.ast.PropListExpression;
import net.arctics.clonk.parser.c4script.ast.StringLiteral;
import net.arctics.clonk.util.Utilities;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultTextDoubleClickStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

public class C4ScriptDoubleClickStrategy extends DefaultTextDoubleClickStrategy {
	
	private final C4ScriptSourceViewerConfiguration configuration;
	
	public C4ScriptDoubleClickStrategy(C4ScriptSourceViewerConfiguration configuration) {
		this.configuration = configuration;
	}
	
	@Override
	protected IRegion findExtendedDoubleClickSelection(IDocument document, int pos) {
		Script script = Utilities.scriptForEditor(configuration.editor());
		Function func = script.funcAt(pos);
		if (func != null) {
			ExpressionLocator locator = new ExpressionLocator(pos-func.body().start());
			C4ScriptParser.reportExpressionsAndStatements(document, script, func, locator, null, ExpressionsAndStatementsReportingFlavour.AlsoStatements, false);
			ExprElm expr = locator.expressionAtRegion();
			if (expr == null)
				return new Region(func.wholeBody().getOffset(), func.wholeBody().getLength());
			else for (; expr != null; expr = expr.parent()) {
				if (expr instanceof StringLiteral)
					return new Region(func.body().getOffset()+expr.start()+1, expr.getLength()-2);
				else if (expr instanceof AccessDeclaration) {
					AccessDeclaration accessDec = (AccessDeclaration) expr;
					return new Region(func.body().getOffset()+accessDec.identifierStart(), accessDec.identifierLength());
				} else if (expr instanceof PropListExpression || expr instanceof Block)
					return new Region(expr.start()+func.body().getOffset(), expr.getLength());
				else if (expr instanceof KeywordStatement) {
					IRegion word = findWord(document, pos);
					try {
						if (word != null && !document.get(word.getOffset(), word.getLength()).equals("\t"))
							return word;
						else
							continue;
					} catch (BadLocationException e) {
						continue;
					}
				}
			}
		}
		return null;
	}
}