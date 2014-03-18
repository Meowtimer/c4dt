package net.arctics.clonk.index;

import static net.arctics.clonk.util.ArrayUtil.indexOfItemSatisfying;
import static net.arctics.clonk.util.Utilities.as;

import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import net.arctics.clonk.ProblemException;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodeMatcher;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.ITransformer;
import net.arctics.clonk.ast.Sequence;
import net.arctics.clonk.builder.CodeConverter;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Operator;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.ScriptParser;
import net.arctics.clonk.c4script.TempScript;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.c4script.ast.AccessVar;
import net.arctics.clonk.c4script.ast.BinaryOp;
import net.arctics.clonk.c4script.ast.CallDeclaration;
import net.arctics.clonk.c4script.ast.Comment;
import net.arctics.clonk.c4script.ast.IDLiteral;
import net.arctics.clonk.c4script.ast.IntegerLiteral;
import net.arctics.clonk.c4script.ast.MemberOperator;
import net.arctics.clonk.c4script.ast.Nil;
import net.arctics.clonk.c4script.ast.SimpleStatement;
import net.arctics.clonk.c4script.ast.Tidy;
import net.arctics.clonk.c4script.ast.Whitespace;
import net.arctics.clonk.c4script.typing.PrimitiveType;
import net.arctics.clonk.util.StreamUtil;
import net.arctics.clonk.util.StringUtil;

/**
 * Helper class containing information about how to transform a Clonk project written for one engine to one consumable by another engine.
 * @author madeen
 *
 */
public class ProjectConversionConfiguration extends CodeConverter {
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
	private final Map<ID, ID> idMap = new HashMap<ID, ID>();
	private final Engine sourceEngine, targetEngine;
	public class ObjParConversion {
		public final Function sourceFunction, targetFunction;
		public final int objParIndex, defParIndex;
		public ObjParConversion(Function sourceFunction, Function targetFunction, int objParIndex, int defParIndex) {
			super();
			this.sourceFunction = sourceFunction;
			this.targetFunction = targetFunction;
			this.objParIndex = objParIndex;
			this.defParIndex = defParIndex;
		}
		public ASTNode convertCall(CallDeclaration call) {
			final ASTNode objPar = objParIndex != -1 && objParIndex < call.params().length ? call.params()[objParIndex] : null;
			final ASTNode defPar = defParIndex != -1 && defParIndex < call.params().length ? call.params()[defParIndex] : null;
			if (objPar != null || defPar != null) {
				final ASTNode[] reducedParms = Arrays.stream(call.params()).filter(n -> n != objPar && n != defPar).toArray(l -> new ASTNode[l]);
				final CallDeclaration c = new CallDeclaration(targetFunction, reducedParms);
				c.setParent(call.parent());
				// prefer to target definition - FIXME?
				final ASTNode targetNode = defPar != null ? defPar : objPar;
				return (targetNode instanceof Whitespace || targetNode instanceof IntegerLiteral)
					? c : new Sequence(targetNode, new MemberOperator(false, false, null, 0), c);
			} else
				return call;
		}
	}
	private final Map<String, ObjParConversion> objParConversions;
	public ProjectConversionConfiguration(final Engine sourceEngine, final Engine targetEngine, List<URL> files) {
		this.sourceEngine = sourceEngine;
		this.targetEngine = targetEngine;
		final List<URL> files1 = files;
		URL codeTransformations = null;
		URL idMap = null;
		for (final URL f : files1)
			if (f.getFile().endsWith("codeTransformations.c"))
				codeTransformations = f;
			else if (f.getFile().endsWith("idMap.txt"))
				idMap = f;
		if (codeTransformations != null)
			loadCodeTransformations(codeTransformations);
		if (idMap != null)
			loadIDMap(idMap);
		objParConversions = prepareObjParConversions();
	}
	public Map<ID, ID> idMap() { return idMap; }
	public List<CodeTransformation> transformations() { return transformations; }
	public Map<String, ObjParConversion> objParConversions() { return objParConversions; }
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
	private Map<String, ObjParConversion> prepareObjParConversions() {
		return sourceEngine.functions().stream().map(sf -> {
			if (sf.name().equals("GetMass"))
				System.out.println("wat");
			final int objNdx = indexOfItemSatisfying(sf.parameters(), p -> p.name().equals("pObj"));
			final int idNdx = indexOfItemSatisfying(sf.parameters(), p -> p.name().equals("idDef"));
			final int diff = (objNdx != -1 ? - 1 : 0) - (idNdx != -1 ? 1 : 0);
			if (diff == 0)
				return null;
			final Function tf = targetEngine.findLocalFunction(sf.name(), false);
			if (tf == null || tf.numParameters() - diff != sf.numParameters())
				return null;
			return new ObjParConversion(sf, tf, objNdx, idNdx);
		}).filter(f -> f != null).collect(Collectors.toMap(c -> c.sourceFunction.name(), c -> c));
	}
	private void loadIDMap(final URL idMap) {
		final String text = StreamUtil.stringFromURL(idMap);
		if (text != null)
			for (final String line : StringUtil.lines(new StringReader(text))) {
				final String[] mapping = line.split("=");
				if (mapping.length == 2)
					this.idMap.put(ID.get(mapping[0]), ID.get(mapping[1]));
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
			final Script script = new TempScript(text, targetEngine);
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
	public void apply(Map<String, Object> conf) {
		final Map<?, ?> idMap = as(conf.getOrDefault("idMap", null), Map.class);
		if (idMap != null)
			idMap.forEach(
				(key, value) -> this.idMap.put(ID.get(key.toString()), ID.get(value.toString()))
			);
	}
	@Override
	public ASTNode performConversion(ASTNode expression, Declaration declaration, ICodeConverterContext context) {
		final ITransformer transformer = new ITransformer() {
			@Override
			public Object transform(final ASTNode prev, final Object prevT, ASTNode expression) {
				if (expression == null)
					return null;
				if (expression instanceof IDLiteral) {
					final IDLiteral lit = (IDLiteral) expression;
					final ID mapped = idMap().get(lit.literal());
					if (mapped != null)
						return new IDLiteral(mapped);
				}
				else if (expression instanceof AccessVar && (((AccessVar)expression).proxiedDefinition()) != null) {
					final ID mapped = idMap().get(ID.get(expression.toString()));
					if (mapped != null)
						return new AccessVar(mapped.stringValue());
				}
				expression = expression.transformSubElements(this);
				boolean success = false;
				if (expression instanceof CallDeclaration) {
					final CallDeclaration cd = (CallDeclaration) expression;
					final ObjParConversion conv = objParConversions().getOrDefault(cd.name(), null);
					if (conv != null)
						expression = conv.convertCall(cd);
					success = true;
				}
				if (!success)
					for (final ProjectConversionConfiguration.CodeTransformation ct : transformations()) {
						for (CodeTransformation c = ct; c != null; c = c.chain()) {
							final Map<String, Object> matched = c.template().match(expression);
							if (matched != null) {
								expression = c.transformation().transform(matched, context);
								success = true;
							}
						}
						if (success)
							break;
					}
				if (expression instanceof CallDeclaration)
					expression = fixSomeParameterTypes((CallDeclaration)expression);
				return expression;
			}
		};
		ASTNode node = as(transformer.transform(null, null, expression), ASTNode.class);
		if (node != null)
			try {
				node = new Tidy(declaration.topLevelStructure(), 2).tidyExhaustive(node);
			} catch (final CloneNotSupportedException e) {}
		return node;
	}
	private CallDeclaration fixSomeParameterTypes(CallDeclaration node) {
		final Function fn = node.function();
		if (fn != null) {
			final ASTNode[] converted = IntStream.range(0, node.params().length).mapToObj(x -> {
				final Variable par = fn.parameter(x);
				final ASTNode arg = node.params()[x];
				if (arg != null && arg.equals(IntegerLiteral.ZERO) && par != null && par.type() != PrimitiveType.INT)
					return new Nil();
				return arg;
			}).toArray(l -> new ASTNode[l]);
			node.setParams(converted);
		}
		return node;
	}
}
