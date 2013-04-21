package net.arctics.clonk.ui.editors.c4script;

import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.ast.AccessDeclaration;
import net.arctics.clonk.parser.c4script.ast.Block;
import net.arctics.clonk.parser.c4script.ast.Comment;
import net.arctics.clonk.parser.c4script.ast.KeywordStatement;
import net.arctics.clonk.parser.c4script.ast.Literal;
import net.arctics.clonk.parser.c4script.ast.PropListExpression;
import net.arctics.clonk.parser.c4script.ast.StringLiteral;
import net.arctics.clonk.util.Utilities;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultTextDoubleClickStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

public class ScriptDoubleClickStrategy extends DefaultTextDoubleClickStrategy {
	
	private final ScriptSourceViewerConfiguration configuration;
	
	public ScriptDoubleClickStrategy(ScriptSourceViewerConfiguration configuration) {
		this.configuration = configuration;
	}
	
	@Override
	protected IRegion findExtendedDoubleClickSelection(IDocument document, int pos) {
		final Script script = Utilities.scriptForEditor(configuration.editor());
		final Function func = script.funcAt(pos);
		if (func != null) {
			final ExpressionLocator<Void> locator = new ExpressionLocator<Void>(pos-func.bodyLocation().start());
			func.traverse(locator, null);
			ASTNode expr = locator.expressionAtRegion();
			if (expr == null)
				return new Region(func.wholeBody().getOffset(), func.wholeBody().getLength());
			else for (; expr != null; expr = expr.parent())
				if (expr instanceof KeywordStatement || expr instanceof Comment || expr instanceof StringLiteral) {
					final IRegion word = findWord(document, pos);
					try {
						if (word != null && !document.get(word.getOffset(), word.getLength()).equals("\t"))
							return word;
						else
							continue;
					} catch (final BadLocationException e) {
						continue;
					}
				} else if (expr instanceof Literal)
					return new Region(func.bodyLocation().getOffset()+expr.start(), expr.getLength());
				else if (expr instanceof AccessDeclaration) {
					final AccessDeclaration accessDec = (AccessDeclaration) expr;
					return new Region(func.bodyLocation().getOffset()+accessDec.identifierStart(), accessDec.identifierLength());
				} else if (expr instanceof PropListExpression || expr instanceof Block)
					return new Region(expr.start()+func.bodyLocation().getOffset(), expr.getLength());
		}
		return null;
	}
}