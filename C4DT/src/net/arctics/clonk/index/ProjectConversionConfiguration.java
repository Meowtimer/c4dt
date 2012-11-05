package net.arctics.clonk.index;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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
import net.arctics.clonk.parser.c4script.ast.Placeholder;
import net.arctics.clonk.parser.c4script.ast.SimpleStatement;
import net.arctics.clonk.parser.c4script.ast.Statement;
import net.arctics.clonk.parser.c4script.ast.SubstitutionPoint;
import net.arctics.clonk.util.StreamUtil;

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
	
	private void addTransformationFromStatement(Statement statement) {
		try {
			transformations.add(new CodeTransformation(statement));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void load(List<URL> files) {
		URL codeTransformations = null;
		for (URL f : files)
			if (f.getFile().endsWith("codeTransformations.c"))
				codeTransformations = f;
		if (codeTransformations != null)
			loadCodeTransformations(codeTransformations);
	}
	
	private void loadCodeTransformations(URL transformationsFile) {
		try {
			InputStream stream = transformationsFile.openStream();
			String text;
			try {
				text = StreamUtil.stringFromInputStream(stream);
			} finally {
				stream.close();
			}
			Script script = new TempScript(text);
			C4ScriptParser parser = new C4ScriptParser(text, script, null) {
				@Override
				protected Placeholder makePlaceholder(String placeholder) {
					return new SubstitutionPoint(placeholder);
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
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	public List<CodeTransformation> transformations() {
		return transformations;
	}
}
