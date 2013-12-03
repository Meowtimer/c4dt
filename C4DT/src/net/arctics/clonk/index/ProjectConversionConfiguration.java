package net.arctics.clonk.index;

import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.arctics.clonk.ProblemException;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodeMatcher;
import net.arctics.clonk.c4script.ScriptParser;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Operator;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.TempScript;
import net.arctics.clonk.c4script.ast.BinaryOp;
import net.arctics.clonk.c4script.ast.CallDeclaration;
import net.arctics.clonk.c4script.ast.Comment;
import net.arctics.clonk.c4script.ast.SimpleStatement;
import net.arctics.clonk.util.StreamUtil;
import net.arctics.clonk.util.StringUtil;

/**
 * Helper class containing information about how to transform a Clonk project written for one engine to one consumable by another engine. 
 * @author madeen
 *
 */
public class ProjectConversionConfiguration {
	
	public class CodeTransformation {
		private final ASTNode template;
		private final ASTNode transformation;
		private final CodeTransformation chain;
		public CodeTransformation(final ASTNode template, final ASTNode transformation, final CodeTransformation chain) {
			super();
			this.template = template;
			this.transformation = transformation;
			this.chain = chain;
		}
		public ASTNode template() { return template; }
		public ASTNode transformation() { return transformation; }
		public CodeTransformation chain() { return chain; }
		public CodeTransformation(final ASTNode stmt, final CodeTransformation chain) {
			this.chain = chain;
			if (stmt instanceof BinaryOp && ((BinaryOp)stmt).operator() == Operator.Transform) {
				final BinaryOp op = (BinaryOp)stmt;
				this.template = ASTNodeMatcher.prepareForMatching(op.leftSide());
				this.transformation = ASTNodeMatcher.prepareForMatching(op.rightSide());
			} else
				throw new IllegalArgumentException(String.format("'%s' is not a transformation statement", stmt.toString()));
		}
		public CodeTransformation(final ASTNode[] tuple, final int tupleElementIndex) {
			this(tuple[tupleElementIndex], tuple.length > tupleElementIndex+1
				? new CodeTransformation(tuple, tupleElementIndex+1)
				: null
			);
		}
		@Override
		public String toString() {
			return String.format("%s => %s", template.printed(), transformation.printed());
		}
	}
	
	private final List<CodeTransformation> transformations = new ArrayList<CodeTransformation>();
	private final Map<String, String> idMap = new HashMap<String, String>();
	private final Engine sourceEngine;
	
	public ProjectConversionConfiguration(final Engine sourceEngine) {
		this.sourceEngine = sourceEngine;
	}
	
	private void addTransformationFromStatement(ASTNode stmt) {
		try {
			stmt = SimpleStatement.unwrap(stmt);
			if (stmt instanceof CallDeclaration) {
				final CallDeclaration call = (CallDeclaration) stmt;
				if (call.name().equals("Chain"))
					transformations.add(new CodeTransformation(call.params(), 0));
				else
					throw new IllegalArgumentException(String.format("Unknown call '%s'", call.name()));
			}
			else
				transformations.add(new CodeTransformation(stmt, null));
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
	
	public void load(final List<URL> files) {
		URL codeTransformations = null;
		URL idMap = null;
		for (final URL f : files)
			if (f.getFile().endsWith("codeTransformations.c"))
				codeTransformations = f;
			else if (f.getFile().endsWith("idMap.txt"))
				idMap = f;
		if (codeTransformations != null)
			loadCodeTransformations(codeTransformations);
		if (idMap != null)
			loadIDMap(idMap);
	}
	
	private void loadIDMap(final URL idMap) {
		final String text = StreamUtil.stringFromURL(idMap);
		if (text != null)
			for (final String line : StringUtil.lines(new StringReader(text))) {
				final String[] mapping = line.split("=");
				if (mapping.length == 2)
					this.idMap.put(mapping[0], mapping[1]);
			}
	}

	private void loadCodeTransformations(final URL transformationsFile) {
		try {
			String text = StreamUtil.stringFromURL(transformationsFile);
			if (text == null)
				return;
			final StringBuilder builder = new StringBuilder();
			builder.append("func Transformations() {\n");
			builder.append(text);
			builder.append("\n}");
			text = builder.toString();
			final Script script = new TempScript(text, sourceEngine);
			final ScriptParser parser = new ScriptParser(text, script, null);
			parser.parse();
			final Function transformations = parser.script().findLocalFunction("Transformations", false);
			if (transformations != null && transformations.body() != null)
				for (final ASTNode stmt : transformations.body().statements()) {
					if (stmt instanceof Comment)
						continue;
					addTransformationFromStatement(stmt);
				}
		} catch (final ProblemException e) {
			e.printStackTrace();
		}
	}
	
	public Map<String, String> idMap() {
		return idMap;
	}
	
	public List<CodeTransformation> transformations() {
		return transformations;
	}
}
