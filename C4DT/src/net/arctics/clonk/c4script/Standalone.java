package net.arctics.clonk.c4script;

import java.util.HashSet;

import net.arctics.clonk.Core;
import net.arctics.clonk.ProblemException;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.IASTVisitor;
import net.arctics.clonk.ast.SourceLocation;
import net.arctics.clonk.ast.Structure;
import net.arctics.clonk.c4script.Function.FunctionScope;
import net.arctics.clonk.c4script.ast.SimpleStatement;
import net.arctics.clonk.c4script.ast.Statement;
import net.arctics.clonk.c4script.typing.Typing;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.IMarkerListener;

/**
 * Helper class to parse single standalone C4Script statements/expressions
 * @author madeen
 *
 */
public class Standalone {

	/** {@link Engine} the statement/expression is considered to be compatible with. */
	public final Engine engine;
	/** {@link Typing} mode */
	public final Typing typing;
	/** {@link Index}es which will be searched when determining referenced declarations. */
	public final Index[] referencedIndices;

	/**
	 * Create standalone context from a set of scripts. {@link #typing} and {@link #engine} will be to respective properties common to all scripts.
	 * If scripts have different typings and/or engines an {@link IllegalArgumentException} will be thrown.
	 * {@link #referencedIndices} will be set to the super set of all {@link Script#index()}.{@link Index#relevantIndexes()}.
	 * @param scripts Scripts to create standalone context from
	 */
	public Standalone(final Iterable<? extends Structure> scripts) {
		Engine engine = null;
		Typing typing = null;
		final HashSet<Index> indices = new HashSet<>();
		for (final Structure s : scripts) {
			if (engine == null)
				engine = s.engine();
			else if (s.engine() != null && engine != s.engine())
				throw new IllegalArgumentException("Structures from different engines");
			if (typing == null)
				typing = s.typing();
			else if (typing != s.typing())
				throw new IllegalArgumentException("Structures have different typing modes");
			indices.addAll(s.index().relevantIndexes());
		}
		this.engine = engine;
		this.typing = typing;
		this.referencedIndices = indices.toArray(new Index[indices.size()]);
	}

	/**
	 * Create standalone context from parameters.
	 * @param engine Value of {@link #engine}
	 * @param typing Value of {@link #typing}
	 * @param referencedIndices Value of {@link #referencedIndices}
	 */
	public Standalone(final Engine engine, final Typing typing, final Index... referencedIndices) {
		super();
		this.engine = engine;
		this.typing = typing;
		this.referencedIndices = referencedIndices;
	}

	/**
	 * Create standalone context from an {@link Engine}. {@link #typing} will be set to the engine's {@link Engine#typing()}
	 * while {@link #referencedIndices} will be set to an empty array.
	 * @param engine {@link Engine} to create standalone context from
	 */
	public Standalone(final Engine engine) {
		this.engine = engine;
		this.typing = engine.typing();
		this.referencedIndices = new Index[0];
	}

	/**
	 * Parse a stand-alone statement with an optional function context.
	 * @param source The statement text to parse
	 * @param function Function context. If null, some temporary context will be created internally.
	 * @param visitor Script parser visitor
	 * @param markerListener Marker visitor
	 * @return The statement or a BunchOfStatement if more than one statement could be parsed from statementText. Possibly null, if erroneous text was passed.
	 * @throws ProblemException
	 */
	public <T> ASTNode parse(
		final String source,
		Function function,
		final IASTVisitor<T> visitor,
		final IMarkerListener markerListener,
		final T context
	) throws ProblemException {
		if (function == null) {
			final Script tempScript = new TempScript(source, engine, referencedIndices) {
				private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
				@Override
				public Typing typing() { return typing; }
			};
			function = new Function(null, FunctionScope.GLOBAL, "<temp>"); //$NON-NLS-1$
			function.setParent(tempScript);
			function.setBodyLocation(new SourceLocation(0, source.length()));
		}
		final ScriptParser tempParser = new ScriptParser(source, function.script(), null) {
			@Override
			public int sectionOffset() { return 0; }
			@Override
			protected ASTNode parseTupleElement() throws ProblemException {
				final Statement s = parseStatement();
				return s instanceof SimpleStatement ? ((SimpleStatement)s).expression() : s;
			}
		};
		tempParser.markers().setListener(markerListener);
		ASTNode result = tempParser.parseDeclaration();
		if (result == null || result instanceof Variables) {
			tempParser.seek(0);
			result = tempParser.parseStandaloneStatement(source, function);
		}
		if (visitor != null && result != null)
			result.traverse(visitor, context);
		return result;
	}

	/**
	 * Parse a standalone statement. Shortcut for {@link #parse(String, Function, IASTVisitor, IMarkerListener, Object)} where all parameters except source are null.
	 * @param source The source to parse
	 * @return Node representing the parsed source.
	 * @throws ProblemException
	 */
	public ASTNode parse(final String source) throws ProblemException {
		return parse(source, null, null, null, null);
	}
}
