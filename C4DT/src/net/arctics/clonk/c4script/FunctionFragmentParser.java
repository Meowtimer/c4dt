package net.arctics.clonk.c4script;

import java.util.EnumSet;
import java.util.List;

import net.arctics.clonk.ProblemException;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.c4script.ast.FunctionBody;
import net.arctics.clonk.parser.Markers;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

public class FunctionFragmentParser extends ScriptParser {
	private final Function function;
	private static String fragmentString(IDocument doc, Function function) {
		String statements_;
		final IRegion statementRegion = function.bodyLocation();
		try {
			// totally important to add the ")". Makes completion proposals work. DO NOT REMOVE!1 - actually, I removed it and it's okay
			statements_ = doc.get(statementRegion.getOffset(), Math.min(statementRegion.getLength(), doc.getLength()-statementRegion.getOffset()));
		} catch (final BadLocationException e) {
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
		synchronized (function) {
			return doUpdate();
		}
	}
	private boolean doUpdate() {
		final String functionSource = functionSource(function);
		final FunctionBody cachedBlock = function != null ? function.bodyMatchingSource(functionSource) : null;
		// if block is non-existent or outdated, parse function code and store block
		if (cachedBlock == null) {
			try {
				if (function != null)
					function.clearLocalVars();
				setCurrentFunction(function);
				markers().enableErrors(DISABLED_INSTANT_ERRORS, false);
				final EnumSet<ParseStatementOption> options = EnumSet.of(ParseStatementOption.ExpectFuncDesc);
				ASTNode body;
				if (function instanceof InitializationFunction)
					body = parseExpression();
				else {
					final List<ASTNode> statements = parseStatements(offset, options, false);
					body = new FunctionBody(function, statements);
				}
				if (function != null) {
					function.storeBody(body, functionSource);
					script().deriveInformation();
				}
			} catch (final ProblemException pe) {}
			return true;
		} else
			return false;
	}
}