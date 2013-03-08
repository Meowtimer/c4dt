package net.arctics.clonk.parser.c4script;

import java.util.EnumSet;
import java.util.LinkedList;

import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.Markers;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.ast.FunctionBody;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

public class FunctionFragmentParser extends C4ScriptParser {
	private final Function function;
	private static String fragmentString(IDocument doc, Function function) {
		String statements_;
		final IRegion statementRegion = function.bodyLocation();
		try {
			// totally important to add the ")". Makes completion proposals work. DO NOT REMOVE!1 - actually, I removed it and it's okay
			statements_ = doc.get(statementRegion.getOffset(), Math.min(statementRegion.getLength(), doc.getLength()-statementRegion.getOffset()));
		} catch (BadLocationException e) {
			statements_ = ""; // well... //$NON-NLS-1$
		}
		return statements_;
	}
	public FunctionFragmentParser(IDocument document, Script script, Function function, Markers markers) {
		super(fragmentString(document, function), script, null);
		setMarkers(markers);
		this.function = function;
		this.offsetOfScriptFragment = function.bodyLocation().start();
	}
	@Override
	public int sectionOffset() { return 0; }
	@Override
	protected String functionSource(Function function) { return new String(buffer); }
	public boolean update() {
		String functionSource = functionSource(function);
		FunctionBody cachedBlock = function != null ? function.bodyMatchingSource(functionSource) : null;
		// if block is non-existent or outdated, parse function code and store block
		if (cachedBlock == null) {
			try {
				if (function != null)
					function.clearLocalVars();
				setCurrentFunction(function);
				markers().enableErrors(DISABLED_INSTANT_ERRORS, false);
				EnumSet<ParseStatementOption> options = EnumSet.of(ParseStatementOption.ExpectFuncDesc);
				ASTNode body;
				if (function instanceof InitializationFunction)
					body = parseExpression();
				else {
					LinkedList<ASTNode> statements = new LinkedList<ASTNode>();
					parseStatementBlock(offset, statements, options, false);
					body = cachedBlock = new FunctionBody(function, statements);
				}
				if (function != null) {
					function.storeBody(body, functionSource);
					script().generateFindDeclarationCache();
				}
			} catch (ParsingException pe) {}
			return true;
		} else
			return false;
	}
	public static FunctionFragmentParser update(IDocument document, Script script, Function function, Markers markers) {
		FunctionFragmentParser updater = new FunctionFragmentParser(document, script, function, markers);
		updater.update();
		return updater;
	}
}