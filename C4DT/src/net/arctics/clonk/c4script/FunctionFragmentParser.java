package net.arctics.clonk.c4script;

import static net.arctics.clonk.util.Utilities.attempt;
import static net.arctics.clonk.util.Utilities.defaulting;

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
	private static String fragmentString(final IDocument doc, final Function function) {
		final IRegion statementRegion = function.bodyLocation();
		return defaulting(
			attempt(() -> doc.get(statementRegion.getOffset(), Math.min(statementRegion.getLength(), doc.getLength()-statementRegion.getOffset())),
				BadLocationException.class, e -> {}),
			""
		);
	}
	public FunctionFragmentParser(final IDocument document, final Script script, final Function function, final Markers markers) {
		super(fragmentString(document, function), script, null);
		setMarkers(markers);
		this.function = function;
		this.offsetOfScriptFragment = function.bodyLocation().start();
	}
	@Override
	public int sectionOffset() { return 0; }
	@Override
	protected String functionSource(final Function function) { return new String(buffer); }
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