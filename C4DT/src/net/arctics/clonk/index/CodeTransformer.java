package net.arctics.clonk.index;

import static java.util.Arrays.stream;
import static java.util.function.Function.identity;
import static net.arctics.clonk.util.ArrayUtil.indexOfItemSatisfying;
import static net.arctics.clonk.util.ArrayUtil.iterable;
import static net.arctics.clonk.util.StringUtil.blockString;
import static net.arctics.clonk.util.StringUtil.lines;
import static net.arctics.clonk.util.StringUtil.nullOrEmpty;
import static net.arctics.clonk.util.Utilities.as;

import java.io.File;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import net.arctics.clonk.ProblemException;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodeMatcher;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.ITransformer;
import net.arctics.clonk.ast.Sequence;
import net.arctics.clonk.builder.CodeConverter;
import net.arctics.clonk.builder.ProjectConverter;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Operator;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.ScriptParser;
import net.arctics.clonk.c4script.Standalone;
import net.arctics.clonk.c4script.TempScript;
import net.arctics.clonk.c4script.ast.AccessVar;
import net.arctics.clonk.c4script.ast.BinaryOp;
import net.arctics.clonk.c4script.ast.CallDeclaration;
import net.arctics.clonk.c4script.ast.Comment;
import net.arctics.clonk.c4script.ast.IDLiteral;
import net.arctics.clonk.c4script.ast.IntegerLiteral;
import net.arctics.clonk.c4script.ast.MemberOperator;
import net.arctics.clonk.c4script.ast.SimpleStatement;
import net.arctics.clonk.c4script.ast.Tidy;
import net.arctics.clonk.c4script.ast.Whitespace;
import net.arctics.clonk.ini.DefinitionPack;
import net.arctics.clonk.util.StreamUtil;
import net.arctics.clonk.util.StringUtil;

import org.eclipse.core.runtime.Path;

/**
 * Helper class containing information about how to transform a Clonk project written for one engine to one consumable by another engine.
 * @author madeen
 *
 */
public class CodeTransformer extends CodeConverter {
	private static final String FROM_REPO = "fromrepo.txt";
	public class CodeTransformation {
		private final ASTNode template;
		private final ASTNode transformation;
		private final boolean exhaustive;
		private final CodeTransformation chain;
		public CodeTransformation(final ASTNode template, final ASTNode transformation, boolean exhaustive, final CodeTransformation chain) {
			super();
			this.template = template;
			this.transformation = transformation;
			this.exhaustive = exhaustive;
			this.chain = chain;
		}
		public ASTNode template() { return template; }
		public ASTNode transformation() { return transformation; }
		public CodeTransformation chain() { return chain; }
		private class Extract {
			public Extract(ASTNode left, ASTNode right, boolean exhaustive) {
				super();
				this.left = left;
				this.right = right;
				this.exhaustive = exhaustive;
			}
			public final ASTNode left;
			public final ASTNode right;
			public final boolean exhaustive;
		}
		private Extract extract(final ASTNode stmt) {
			final BinaryOp op = as(stmt, BinaryOp.class);
			final CallDeclaration cd = as(stmt, CallDeclaration.class);
			if (op != null && op.operator() == Operator.Transform)
				return new Extract (op.leftSide(), op.rightSide(), false);
			if (op != null && op.operator() == Operator.TransformExhaustive)
				return new Extract (op.leftSide(), op.rightSide(), true);
			else if (cd != null && cd.name().equals("Transform") && cd.params().length == 2)
				return new Extract(cd.params()[0], cd.params()[1], false);
			else
				return null;
		}
		public CodeTransformation(final ASTNode stmt, final CodeTransformation chain) {
			this.chain = chain;
			final Extract tt = extract(stmt);
			if (tt != null) {
				this.template = ASTNodeMatcher.prepareForMatching(tt.left);
				this.transformation = ASTNodeMatcher.parsePlaceholders(tt.right);
				this.exhaustive = tt.exhaustive;
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
			return String.format("%s %s %s",
				template.printed(),
				(exhaustive ? Operator.TransformExhaustive : Operator.Transform).operatorName(),
				transformation.printed()
			);
		}
		public ASTNode transform(ASTNode expression, ICodeConverterContext context) {
			ASTNode result = expression;
			do {
				final Map<String, Object> matched = template().match(result);
				if (matched != null) {
					result = transformation().transform(matched, context);
					if (!exhaustive)
						break;
				}
				else
					break;
			} while (true);
			return result == expression ? null : result;
		}
	}
	private final List<CodeTransformation> transformations = new ArrayList<CodeTransformation>();
	private final Map<ID, ID> idMap = new HashMap<ID, ID>();
	private final Engine sourceEngine, targetEngine;
	private final Map<String, byte[]> compatibilityFiles = new HashMap<>();
	public Map<ID, ID> idMap() { return idMap; }
	public List<CodeTransformation> transformations() { return transformations; }
	public Map<String, ObjParConversion> objParConversions() { return objParConversions; }
	public Map<String, byte[]> compatibilityFiles() { return compatibilityFiles; }
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
				final ASTNode[] reducedParms = stream(call.params()).filter(n -> n != objPar && n != defPar).toArray(l -> new ASTNode[l]);
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
	public CodeTransformer(final Engine sourceEngine, final Engine targetEngine, List<URL> files) {
		this.sourceEngine = sourceEngine;
		this.targetEngine = targetEngine;
		final List<URL> files1 = files;
		URL codeTransformations = null;
		URL idMap = null;
		final BiFunction<String[], String, String[]> subSeqToRight = (segments, startIndicator) -> {
			for (int i = segments.length - 1; i >= 0; i--)
				if (segments[i].equals(startIndicator))
					return Arrays.copyOfRange(segments, i+1, segments.length);
			return null;
		};
		for (final URL f : files1) {
			final String[] segments = f.getFile().split("/");
			final String last = segments[segments.length-1];
			String[] s;
			if (last.equals("codeTransformations.c"))
				codeTransformations = f;
			else if (last.equals("idMap.txt"))
				idMap = f;
			else if ((s = subSeqToRight.apply(segments, "compatibility")) != null && !s[s.length-1].startsWith("."))
				switch (s[s.length-1]) {
				case FROM_REPO:
					if (nullOrEmpty(targetEngine.settings().repositoryPath))
						throw new IllegalStateException();
					final File repoDir = new File(targetEngine.settings().repositoryPath);
					lines(new StringReader(StreamUtil.stringFromURL(f))).forEach(repoFile -> {
						final byte[] bytes = StreamUtil.bytesFromFile(new File(repoDir, "planet/" + repoFile));
						if (bytes != null)
							compatibilityFiles.put(repoFile, bytes);
					});
					break;
				default:
					final byte[] bytes = StreamUtil.bytesFromURL(f);
					if (bytes != null)
						compatibilityFiles.put(blockString("", "", "/", iterable(s)), bytes);
				}
		}
		objParConversions = prepareObjParConversions();
		if (codeTransformations != null)
			loadCodeTransformations(codeTransformations);
		if (idMap != null)
			loadIDMap(idMap);
	}
	private void addTransformationFromStatement(ASTNode stmt) {
		try {
			stmt = SimpleStatement.unwrap(stmt);
			final CallDeclaration call = as(stmt, CallDeclaration.class);
			transformations.add(
				call != null && call.name().equals("Chain")
					? new CodeTransformation(call.params(), 0)
					: new CodeTransformation(stmt, null)
			);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
	private Map<String, ObjParConversion> prepareObjParConversions() {
		return sourceEngine.functions().stream().map(sf -> {
			final int objNdx = indexOfItemSatisfying(sf.parameters(), p -> p.name().equals("pObj"));
			final int idNdx = indexOfItemSatisfying(sf.parameters(), p -> p.name().equals("idDef"));
			final int diff = (objNdx != -1 ? - 1 : 0) - (idNdx != -1 ? 1 : 0);
			if (diff == 0)
				return null;
			final Function tf = targetEngine.findLocalFunction(sf.name(), false);
			if (tf == null || tf.numParameters() - diff != sf.numParameters())
				return null;
			return new ObjParConversion(sf, tf, objNdx, idNdx);
		}).filter(f -> f != null).collect(Collectors.toMap(c -> c.sourceFunction.name(), identity()));
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
			final ScriptParser parser = new Standalone.Parser(text, script, null);
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
			idMap.forEach((key, value) -> {
				final String target = value.toString();
				final String nonConflict = targetEngine.findLocalDeclaration(target, Declaration.class) != null ? target + "_" : target;
				this.idMap.put(ID.get(key.toString()), ID.get(nonConflict));
			});
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
				else if (expression instanceof DefinitionPack) {
					final DefinitionPack dp = (DefinitionPack) expression;
					return new DefinitionPack(ProjectConverter.convertPath(sourceEngine, targetEngine, new Path(dp.value())).toPortableString());
				}
				expression = expression.transformSubElements(this);
				boolean success = false;
				final CallDeclaration cd = as(expression, CallDeclaration.class);
				if (cd != null && !(cd.predecessor() instanceof MemberOperator)) {
					final ObjParConversion conv = objParConversions().getOrDefault(cd.name(), null);
					if (conv != null) {
						expression = conv.convertCall(cd);
						success = true;
					}
				}
				if (!success)
					for (final CodeTransformer.CodeTransformation ct : transformations()) {
						for (CodeTransformation c = ct; c != null; c = c.chain()) {
							final ASTNode transformed = c.transform(expression, context);
							if (transformed != null) {
								expression = transformed;
								success = true;
							}
						}
						if (success)
							break;
					}
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
}
