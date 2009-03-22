package net.arctics.clonk.ui.editors.c4script;

import net.arctics.clonk.parser.C4Field;
import net.arctics.clonk.parser.C4Function;
import net.arctics.clonk.parser.C4ScriptBase;
import net.arctics.clonk.parser.C4ScriptParser;
import net.arctics.clonk.parser.FindFieldInfo;
import net.arctics.clonk.parser.C4ScriptExprTree.ExprElm;
import net.arctics.clonk.parser.C4ScriptExprTree.FieldRegion;
import net.arctics.clonk.parser.C4ScriptExprTree.IExpressionListener;
import net.arctics.clonk.parser.C4ScriptExprTree.TraversalContinuation;
import net.arctics.clonk.parser.C4ScriptParser.ParsingException;
import net.arctics.clonk.util.Utilities;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Encapsulates information about an identifier in a document and the field it refers to
 * @author madeen
 *
 */
public class IdentInfo implements IExpressionListener {
	private ITextEditor editor;
	private String line;
	private IRegion identRegion;
	private ExprElm exprAtRegion;
	private C4Field field;
	
	public ITextEditor getEditor() {
		return editor;
	}
	
	public IdentInfo(ITextEditor editor, IDocument doc, IRegion region) throws BadLocationException, ParsingException {
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
		int statementStart = func.getBody().getOffset();
		if (region.getOffset() >= statementStart) {
			identRegion = new Region(region.getOffset()-statementStart,0);
			C4ScriptParser parser = C4ScriptParser.reportExpressionsAndStatements(doc, func.getBody(), script, func, this);
			if (exprAtRegion != null) {
				FieldRegion fieldRegion = exprAtRegion.fieldAt(identRegion.getOffset()-exprAtRegion.getExprStart(), parser);
				if (fieldRegion != null) {
					this.field = fieldRegion.getField();
					this.identRegion = new Region(statementStart+fieldRegion.getRegion().getOffset(), fieldRegion.getRegion().getLength());
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
		identRegion = new Region(lineInfo.getOffset()+start,end-start);
		field = script.findField(doc.get(identRegion.getOffset(),  identRegion.getLength()), new FindFieldInfo(script.getIndex(), func));
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
		return identRegion;
	}

	/**
	 * @return the field
	 */
	public C4Field getField() {
		return field;
	}

	public TraversalContinuation expressionDetected(ExprElm expression, C4ScriptParser parser) {
		expression.traverse(new IExpressionListener() {
			public TraversalContinuation expressionDetected(ExprElm expression, C4ScriptParser parser) {
				if (identRegion.getOffset() >= expression.getExprStart() && identRegion.getOffset() < expression.getExprEnd()) {
					exprAtRegion = expression;
					return TraversalContinuation.TraverseSubElements;
				}
				return TraversalContinuation.Continue;
			}
		}, parser);
		return exprAtRegion != null ? TraversalContinuation.Cancel : TraversalContinuation.Continue;
	}
}