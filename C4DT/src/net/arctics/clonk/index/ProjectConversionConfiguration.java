package net.arctics.clonk.index;

import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.Function.FunctionScope;
import net.arctics.clonk.parser.c4script.Operator;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.TempScript;
import net.arctics.clonk.parser.c4script.ast.BinaryOp;
import net.arctics.clonk.parser.c4script.ast.BunchOfStatements;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.MatchingPlaceholder;
import net.arctics.clonk.parser.c4script.ast.Placeholder;
import net.arctics.clonk.parser.c4script.ast.SimpleStatement;
import net.arctics.clonk.parser.c4script.ast.Statement;
import net.arctics.clonk.util.StreamUtil;
import net.arctics.clonk.util.StringUtil;

/**
 * Helper class containing information about how to transform a Clonk project written for one engine to one consumable by another engine. 
 * @author madeen
 *
 */
public class ProjectConversionConfiguration {
	
	public class CodeTransformation {
		private final ExprElm template;
		private final ExprElm transformation;
		public CodeTransformation(ExprElm template, ExprElm transformation) {
			super();
			this.template = template;
			this.transformation = transformation;
		}
		public ExprElm template() {
			return template;
		}
		public ExprElm transformation() {
			return transformation;
		}
		public CodeTransformation(Statement transformationStatement) {
			ExprElm unwrapped = SimpleStatement.unwrap(transformationStatement);
			if (unwrapped instanceof BinaryOp && ((BinaryOp)unwrapped).operator() == Operator.Transform) {
				BinaryOp op = (BinaryOp)unwrapped;
				this.template = op.leftSide();
				this.transformation = op.rightSide();
			} else
				throw new IllegalArgumentException(String.format("'%s' is not a transformation statement", transformationStatement.toString()));
		}
	}
	
	private final List<CodeTransformation> transformations = new ArrayList<CodeTransformation>();
	private final Map<String, String> idMap = new HashMap<String, String>();
	private final Engine sourceEngine;
	
	public ProjectConversionConfiguration(Engine sourceEngine) {
		this.sourceEngine = sourceEngine;
	}
	
	private void addTransformationFromStatement(Statement statement) {
		try {
			transformations.add(new CodeTransformation(statement));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void load(List<URL> files) {
		URL codeTransformations = null;
		URL idMap = null;
		for (URL f : files)
			if (f.getFile().endsWith("codeTransformations.c"))
				codeTransformations = f;
			else if (f.getFile().endsWith("idMap.txt"))
				idMap = f;
		if (codeTransformations != null)
			loadCodeTransformations(codeTransformations);
		if (idMap != null)
			loadIDMap(idMap);
	}
	
	private void loadIDMap(URL idMap) {
		String text = StreamUtil.stringFromURL(idMap);
		if (text != null)
			for (String line : StringUtil.lines(new StringReader(text))) {
				String[] mapping = line.split("=");
				if (mapping.length == 2)
					this.idMap.put(mapping[0], mapping[1]);
			}
	}

	private void loadCodeTransformations(URL transformationsFile) {
		try {
			String text = StreamUtil.stringFromURL(transformationsFile);
			if (text == null)
				return;
			Script script = new TempScript(text, sourceEngine);
			C4ScriptParser parser = new C4ScriptParser(text, script, null) {
				@Override
				protected Placeholder makePlaceholder(String placeholder) throws ParsingException {
					return new MatchingPlaceholder(placeholder);
				}
			};
			Function context = new Function("<temp>", null, FunctionScope.GLOBAL); //$NON-NLS-1$
			context.setScript(script);
			context.setBodyLocation(new SourceLocation(0, text.length()));
			Statement s = parser.parseStandaloneStatement(text, context, null);
			if (s instanceof BunchOfStatements)
				for (Statement stmt : ((BunchOfStatements)s).statements())
					addTransformationFromStatement(stmt);
			else
				addTransformationFromStatement(s);
		} catch (ParsingException e) {
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
