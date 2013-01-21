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
	 * @param context Function context. If null, some temporary context will be created internally.
	 * @param visitor Script parser visitor
	 * @param markerListener Marker visitor
	 * @return The statement or a BunchOfStatement if more than one statement could be parsed from statementText. Possibly null, if erroneous text was passed.
	 * @throws ParsingException
	 */
	public static ASTNode parseStandaloneNode(
		final String source,
		Function context,
		IASTVisitor<C4ScriptParser> visitor,
		final IMarkerListener markerListener,
		Engine engine
	) throws ParsingException {
		if (context == null) {
			Script tempScript = new TempScript(source, engine);
			context = new Function("<temp>", null, FunctionScope.GLOBAL); //$NON-NLS-1$
			context.setScript(tempScript);
			context.setBodyLocation(new SourceLocation(0, source.length()));
		}
		C4ScriptParser tempParser = new C4ScriptParser(source, context.script(), null) {
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
		if (result == null)
			result = tempParser.parseStandaloneStatement(source, context, visitor);
		return result;
	}

}
