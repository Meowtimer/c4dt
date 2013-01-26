package net.arctics.clonk.parser.c4script;

import net.arctics.clonk.index.Engine;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.IASTVisitor;
import net.arctics.clonk.parser.IMarkerListener;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.c4script.Function.FunctionScope;
import net.arctics.clonk.parser.c4script.ast.SimpleStatement;
import net.arctics.clonk.parser.c4script.ast.Statement;

public class ScriptsHelper {
	/**
	 * Parse a stand-alone statement with an optional function context.
	 * @param source The statement text to parse
	 * @param function Function context. If null, some temporary context will be created internally.
	 * @param visitor Script parser visitor
	 * @param markerListener Marker visitor
	 * @return The statement or a BunchOfStatement if more than one statement could be parsed from statementText. Possibly null, if erroneous text was passed.
	 * @throws ParsingException
	 */
	public static <T> ASTNode parseStandaloneNode(
		final String source,
		Function function,
		IASTVisitor<T> visitor,
		final IMarkerListener markerListener,
		Engine engine,
		T context
	) throws ParsingException {
		if (function == null) {
			Script tempScript = new TempScript(source, engine);
			function = new Function("<temp>", null, FunctionScope.GLOBAL); //$NON-NLS-1$
			function.setScript(tempScript);
			function.setBodyLocation(new SourceLocation(0, source.length()));
		}
		C4ScriptParser tempParser = new C4ScriptParser(source, function.script(), null) {
			@Override
			public int sectionOffset() { return 0; }
			@Override
			protected ASTNode parseTupleElement(boolean reportErrors) throws ParsingException {
				Statement s = parseStatement();
				if (s instanceof SimpleStatement)
					return ((SimpleStatement)s).expression();
				else
					return s;
			}
		};
		tempParser.markers().setListener(markerListener);
		ASTNode result = tempParser.parseDeclaration();
		if (result == null) {
			result = tempParser.parseStandaloneStatement(source, function);
			if (visitor != null)
				result.traverse(visitor, context);
		}
		return result;
	}
	public static ASTNode parse(final String source, Engine engine) throws ParsingException {
		return ScriptsHelper.parseStandaloneNode(source, null, null, null, engine, null);
	}
	public static ASTNode matchingExpr(final String statementText, Engine engine) {
		try {
			return parse(statementText, engine).matchingExpr();
		} catch (ParsingException e) {
			e.printStackTrace();
			return null;
		}
	}
}
