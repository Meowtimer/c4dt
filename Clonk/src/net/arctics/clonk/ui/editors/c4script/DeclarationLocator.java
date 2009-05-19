package net.arctics.clonk.ui.editors.c4script;

import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.c4script.C4Function;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.FindDeclarationInfo;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.DeclarationRegion;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.ExprElm;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.IExpressionListener;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.TraversalContinuation;
import net.arctics.clonk.util.Utilities;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Encapsulates information about an identifier in a document and the declaration it refers to
 * @author madeen
 *
 */
public class DeclarationLocator extends ExpressionLocator {
	private ITextEditor editor;
	private String line;
	private C4Declaration declaration;
	
	public ITextEditor getEditor() {
		return editor;
	}
	
	public DeclarationLocator(ITextEditor editor, IDocument doc, IRegion region) throws BadLocationException, ParsingException {
		this.editor = editor;
		C4ScriptBase script = Utilities.getScriptForEditor(getEditor());
		if (script == null)
			return;
		C4Function func = script.funcAt(region);
		if (func == null) {
			// outside function, fallback to old technique
			simpleFindField(doc, region, script, null);
			return;
		}
		int bodyStart = func.getBody().getOffset();
		if (region.getOffset() >= bodyStart) {
			exprRegion = new Region(region.getOffset()-bodyStart,0);
			C4ScriptParser parser = C4ScriptParser.reportExpressionsAndStatements(doc, func.getBody(), script, func, this);
			if (exprAtRegion != null) {
				DeclarationRegion declRegion = exprAtRegion.declarationAt(exprRegion.getOffset()-exprAtRegion.getExprStart(), parser);
				if (declRegion != null) {
					this.declaration = declRegion.getDeclaration();
					this.exprRegion = new Region(bodyStart+declRegion.getRegion().getOffset(), declRegion.getRegion().getLength());
				}
			}
		}
		else
			simpleFindField(doc, region, script, func);
	}

	private void simpleFindField(IDocument doc, IRegion region,
			C4ScriptBase script, C4Function func) throws BadLocationException {
		IRegion lineInfo;
		String line;
		try {
			lineInfo = doc.getLineInformationOfOffset(region.getOffset());
			line = doc.get(lineInfo.getOffset(),lineInfo.getLength());
		} catch (BadLocationException e) {
			return;
		}
		int localOffset = region.getOffset() - lineInfo.getOffset();
		int start,end;
		for (start = localOffset; start > 0 && Character.isJavaIdentifierPart(line.charAt(start-1)); start--);
		for (end = localOffset; end < line.length() && Character.isJavaIdentifierPart(line.charAt(end)); end++);
		exprRegion = new Region(lineInfo.getOffset()+start,end-start);
		declaration = script.findDeclaration(doc.get(exprRegion.getOffset(),  exprRegion.getLength()), new FindDeclarationInfo(script.getIndex(), func));
	}
	
	/**
	 * @return the line
	 */
	public String getLine() {
		return line;
	}

	/**
	 * @return the identRegion
	 */
	public IRegion getIdentRegion() {
		return exprRegion;
	}

	/**
	 * @return the declaration
	 */
	public C4Declaration getDeclaration() {
		return declaration;
	}

	public TraversalContinuation expressionDetected(ExprElm expression, C4ScriptParser parser) {
		expression.traverse(new IExpressionListener() {
			public TraversalContinuation expressionDetected(ExprElm expression, C4ScriptParser parser) {
				if (exprRegion.getOffset() >= expression.getExprStart() && exprRegion.getOffset() < expression.getExprEnd()) {
					exprAtRegion = expression;
					return TraversalContinuation.TraverseSubElements;
				}
				return TraversalContinuation.Continue;
			}
		}, parser);
		return exprAtRegion != null ? TraversalContinuation.Cancel : TraversalContinuation.Continue;
	}
}