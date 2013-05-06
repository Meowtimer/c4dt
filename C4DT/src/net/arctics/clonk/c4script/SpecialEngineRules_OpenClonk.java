package net.arctics.clonk.c4script;

import static net.arctics.clonk.util.Utilities.as;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.arctics.clonk.Core;
import net.arctics.clonk.Core.IDocumentAction;
import net.arctics.clonk.Problem;
import net.arctics.clonk.ProblemException;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodeMatcher;
import net.arctics.clonk.ast.EntityRegion;
import net.arctics.clonk.ast.IEvaluationContext;
import net.arctics.clonk.ast.SourceLocation;
import net.arctics.clonk.ast.Structure;
import net.arctics.clonk.c4script.Function.FunctionScope;
import net.arctics.clonk.c4script.Variable.Scope;
import net.arctics.clonk.c4script.ast.AccessVar;
import net.arctics.clonk.c4script.ast.CallDeclaration;
import net.arctics.clonk.c4script.ast.IntegerLiteral;
import net.arctics.clonk.c4script.ast.NumberLiteral;
import net.arctics.clonk.c4script.ast.PropListExpression;
import net.arctics.clonk.c4script.ast.SimpleStatement;
import net.arctics.clonk.c4script.ast.Statement;
import net.arctics.clonk.c4script.ast.StringLiteral;
import net.arctics.clonk.c4script.effect.Effect;
import net.arctics.clonk.c4script.effect.EffectFunction;
import net.arctics.clonk.c4script.typing.IType;
import net.arctics.clonk.c4script.typing.PrimitiveType;
import net.arctics.clonk.c4script.typing.TypeUnification;
import net.arctics.clonk.c4script.typing.TypingJudgementMode;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.ID;
import net.arctics.clonk.index.IIndexEntity;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.Scenario;
import net.arctics.clonk.ini.ComplexIniEntry;
import net.arctics.clonk.ini.IDArray;
import net.arctics.clonk.ini.IniEntry;
import net.arctics.clonk.ini.IniItem;
import net.arctics.clonk.ini.IniSection;
import net.arctics.clonk.ini.PlayerControlsUnit;
import net.arctics.clonk.ini.ScenarioUnit;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.Markers;
import net.arctics.clonk.ui.editors.ClonkCompletionProposal;
import net.arctics.clonk.ui.editors.c4script.ScriptCompletionProcessor;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.IPredicate;
import net.arctics.clonk.util.KeyValuePair;
import net.arctics.clonk.util.Pair;
import net.arctics.clonk.util.UI;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

public class SpecialEngineRules_OpenClonk extends SpecialEngineRules {

	private static final String DEFINITION_FUNCTION = "Definition";

	/**
	 * Rule to handle typing of effect proplists.<br>
	 * Assigns default parameters to effect functions.
	 * For the effect proplist parameter, an implicit ProplistDeclaration is created
	 * so that type information and proplist value locations (first assignment) can
	 * be stored.<br>
	 * Get/Add-Effect functions will return the type of the effect to be acquired/created if the effect name can be evaluated and a corresponding effect proplist type
	 * can be found.
	 */
	@AppliedTos(list={
		@AppliedTo(functions={"GetEffect", "AddEffect", "RemoveEffect", "CheckEffect"}),
		@AppliedTo(functions={"GetEffectCount"}, role=DECLARATION_LOCATOR)
	})
	public final SpecialFuncRule effectProplistAdhocTyping = new SpecialFuncRule() {
		@Override
		public boolean assignDefaultParmTypes(ProblemReporter processor, Function function) {
			final EffectFunction fun = as(function, EffectFunction.class);
			if (fun != null && fun.effect() != null) {
				fun.effect();
				fun.assignParameterTypes(Effect.parameterTypesForCallback(fun.callbackName(), fun.script(), fun.effect()));
				return true;
			}
			return false;
		}
		@Override
		public Function newFunction(String name) {
			if (name.startsWith(EffectFunction.FUNCTION_NAME_PREFIX))
				// determine effect and callback name later
				return new EffectFunction();
			return null;
		};
		@Override
		public IType returnType(ProblemReporter processor, CallDeclaration node) {
			Object parmEv;
			if (!node.name().equals("RemoveEffect") && node.params().length >= 1 && (parmEv = node.params()[0].evaluateStatic(node.parentOfType(Function.class))) instanceof String) {
				final String effectName = (String) parmEv;
				return processor.script().effects().get(effectName);
			}
			return null;
		};
		@Override
		public EntityRegion locateEntityInParameter(
			CallDeclaration node, ProblemReporter processor, int index,
			int offsetInExpression, ASTNode parmExpression
		) {
			if (parmExpression instanceof StringLiteral && node.params().length >= 1 && node.params()[0] == parmExpression) {
				final String effectName = ((StringLiteral)parmExpression).literal();
				final Effect effect = processor.script().effects().get(effectName);
				if (effect != null)
					return new EntityRegion(new HashSet<IIndexEntity>(effect.functions().values()), new Region(parmExpression.start()+1, parmExpression.getLength()-2));
			}
			return super.locateEntityInParameter(node, processor, index, offsetInExpression, parmExpression);
		}
	};

	/**
	 * Rule applied to the 'Definition' func.<br/>
	 * Causes local vars to be created for SetProperty-calls.
	 */
	@AppliedTo(functions={"SetProperty"})
	public final SpecialFuncRule definitionFunctionSpecialHandling = new SpecialFuncRule() {
		@Override
		public Function newFunction(String name) {
			if (name.equals(DEFINITION_FUNCTION))
				return new DefinitionFunction();
			else
				return null;
		};
		@Override
		public boolean validateArguments(CallDeclaration node, ASTNode[] arguments, ProblemReporter processor) {
			if (arguments.length >= 2 && node.parentOfType(Function.class) instanceof DefinitionFunction) {
				final Object nameEv = arguments[0].evaluateStatic(node.parentOfType(Function.class));
				if (nameEv instanceof String) {
					final SourceLocation loc = processor.absoluteSourceLocationFromExpr(arguments[0]);
					final Script script = node.parentOfType(Script.class);
					if (script.findLocalVariable((String)nameEv, true) == null) {
						final Variable var = script.createVarInScope(
							Variable.DEFAULT_VARIABLE_FACTORY,
							node.parentOfType(Function.class),
							(String) nameEv, Scope.LOCAL, loc.start(), loc.end(), null
							);
						var.setLocation(processor.absoluteSourceLocationFromExpr(arguments[0]));
						var.setScope(Scope.LOCAL);
						// clone argument since the offset of the expression inside the func body is relative while
						// the variable initialization expression location is supposed to be absolute
						final ASTNode initializationClone = arguments[1].clone();
						initializationClone.incrementLocation(node.sectionOffset());
						var.setInitializationExpression(initializationClone);
						var.forceType(processor.typeOf(arguments[1]));
						final AccessVar av = new AccessVar(var);
						processor.judgement(av, processor.typeOf(arguments[1]), TypingJudgementMode.OVERWRITE);
						var.setParent(node.parentOfType(Function.class));
					}
					//parser.getContainer().addDeclaration(var);
				}
			}
			return false; // default validation
		}
		@Override
		public EntityRegion locateEntityInParameter(CallDeclaration callFunc, ProblemReporter processor, int index, int offsetInExpression, ASTNode parmExpression) {
			String property;
			if (index == 0 && (property = as(parmExpression.evaluateStatic(callFunc.parentOfType(Function.class)), String.class)) != null) {
				final IType ty = callFunc.predecessorInSequence() != null ? processor.typeOf(callFunc.predecessorInSequence()) : processor.script();
				final Set<Variable> vars = new HashSet<Variable>();
				for (final IType t : ty)
					if (t instanceof IProplistDeclaration) {
						final Variable v = ((IProplistDeclaration)t).findComponent(property);
						if (v != null)
							vars.add(v);
					}
				return vars.size() > 0 ? new EntityRegion(vars, parmExpression) : null;
			} else
				return null;
		}
	};

	private static class EvaluationTracer implements IEvaluationContext {
		public ASTNode topLevelExpression;
		public IFile tracedFile;
		public IRegion tracedLocation;
		public Script script;
		public Function function;
		public Object[] arguments;
		public Object evaluation;
		public EvaluationTracer(ASTNode topLevelExpression, Object[] arguments, Function function, Script script) {
			this.topLevelExpression = topLevelExpression;
			this.arguments = arguments;
			this.function = function;
			this.script = script;
		}
		@Override
		public Object[] arguments() { return arguments; }
		@Override
		public Script script() { return script; }
		@Override
		public int codeFragmentOffset() { return function != null ? function.codeFragmentOffset() : 0; }
		@Override
		public Object valueForVariable(AccessVar access) { return function != null ? function.valueForVariable(access) : null; }
		@Override
		public Function function() { return function; }
		@Override
		public void reportOriginForExpression(ASTNode expression, IRegion location, IFile file) {
			if (expression == topLevelExpression) {
				tracedLocation = location;
				tracedFile = file;
			}
		}
		@Override
		public Object cookie() { return null; }
		public static EvaluationTracer evaluate(ASTNode expression, Object[] arguments, Script script, Function function) {
			final EvaluationTracer tracer = new EvaluationTracer(expression, arguments, function, script);
			tracer.evaluation = expression.evaluateStatic(tracer);
			return tracer;
		}
		public static EvaluationTracer evaluate(ASTNode expression, Function function) {
			return evaluate(expression, null, function.script(), function);
		}
	}

	/**
	 * Validate format strings.
	 */
	@AppliedTo(functions={"Log", "Message", "Format"})
	public final SpecialFuncRule formatArgumentsValidationRule = new SpecialFuncRule() {
		private boolean checkParm(CallDeclaration node, final ASTNode[] arguments, final ProblemReporter processor, int parmIndex, String formatString, int rangeStart, int rangeEnd, EvaluationTracer evTracer, IType expectedType) throws ProblemException {
			if (parmIndex+1 >= arguments.length) {
				if (evTracer.tracedFile == null)
					return true;
				if (evTracer.tracedFile.equals(processor.script().scriptFile())) {
					processor.markers().error(processor, Problem.MissingFormatArg, node, evTracer.tracedLocation.getOffset()+rangeStart, evTracer.tracedLocation.getOffset()+rangeEnd, Markers.NO_THROW|Markers.ABSOLUTE_MARKER_LOCATION,
							formatString, evTracer.evaluation, evTracer.tracedFile.getProjectRelativePath().toOSString());
					return !arguments[0].containsOffset(evTracer.tracedLocation.getOffset());
				} else
					processor.markers().error(processor, Problem.MissingFormatArg, node, arguments[0], Markers.NO_THROW,
							formatString, evTracer.evaluation, evTracer.tracedFile.getProjectRelativePath().toOSString());
			}
			else if (!TypeUnification.compatible(expectedType, processor.typeOf(arguments[parmIndex+1]))) {
				if (evTracer.tracedFile == null)
					return true;
				processor.markers().warning(processor, Problem.IncompatibleFormatArgType, node, arguments[parmIndex+1],
					Markers.NO_THROW, expectedType.typeName(false), processor.typeOf(arguments[parmIndex+1]).typeName(false), evTracer.evaluation, evTracer.tracedFile.getProjectRelativePath().toOSString());
			}
			return false;
		}
		@Override
		public boolean validateArguments(CallDeclaration node, ASTNode[] arguments, ProblemReporter processor) throws ProblemException {
			EvaluationTracer evTracer;
			int parmIndex = 0;
			if (arguments.length >= 1 && (evTracer = EvaluationTracer.evaluate(arguments[0], node.parentOfType(Function.class))).evaluation instanceof String) {
				final String formatString = (String)evTracer.evaluation;
				boolean separateIssuesMarker = false;
				for (int i = 0; i < formatString.length(); i++)
					if (formatString.charAt(i) == '%') {
						int j;
						for (j = i+1; j < formatString.length() && (formatString.charAt(j) == '.' || (formatString.charAt(j) >= '0' && formatString.charAt(j) <= '9')); j++);
						if (j >= formatString.length())
							break;
						final String format = formatString.substring(i, j+1);
						IType requiredType;
						switch (formatString.charAt(j)) {
						case 'd': case 'x': case 'X': case 'c':
							requiredType = PrimitiveType.INT;
							break;
						case 'i':
							requiredType = PrimitiveType.ID;
							break;
						case 'v':
							requiredType = PrimitiveType.ANY;
							break;
						case 's':
							requiredType = PrimitiveType.STRING;
							break;
						default:
							requiredType = null;
							parmIndex--;
							break;
						}
						if (requiredType != null)
							separateIssuesMarker |= checkParm(node, arguments, processor, parmIndex, format, i+1, j+2, evTracer, requiredType);
						i = j;
						parmIndex++;
					}
				if (separateIssuesMarker)
					processor.markers().error(processor, Problem.DragonsHere, node, arguments[0], Markers.NO_THROW);
			}
			return false; // let others validate as well
		};
	};

	public SpecialEngineRules_OpenClonk() {
		super();
		// override SetAction link rule to also take into account local 'ActMap' vars
		setActionLinkRule = new SetActionLinkRule() {
			@Override
			protected EntityRegion actionLinkForDefinition(Function currentFunction, Definition definition, ASTNode parmExpression) {
				if (definition == null)
					return null;
				Object parmEv;
				final EntityRegion result = super.actionLinkForDefinition(currentFunction, definition, parmExpression);
				if (result != null)
					return result;
				else if ((parmEv = parmExpression.evaluateStatic(currentFunction)) instanceof String) {
					final Variable actMapLocal = definition.findLocalVariable("ActMap", true); //$NON-NLS-1$
					if (actMapLocal != null && actMapLocal.initializationExpression() instanceof PropListExpression) {
						final IProplistDeclaration proplDecl = ((PropListExpression)actMapLocal.initializationExpression()).definedDeclaration();
						final Variable action = proplDecl.findComponent((String)parmEv);
						if (action != null)
							return new EntityRegion(action, parmExpression);
					}
				}
				return null;
			};
			@Override
			public EntityRegion locateEntityInParameter(CallDeclaration node, ProblemReporter processor, int index, int offsetInExpression, ASTNode parmExpression) {
				if (index != 0)
					return null;
				final IType t = node.predecessorInSequence() != null ? processor.typeOf(node.predecessorInSequence()) : null;
				if (t != null) for (final IType ty : t)
					if (ty instanceof Definition) {
						final EntityRegion result = actionLinkForDefinition(node.parentOfType(Function.class), (Definition)ty, parmExpression);
						if (result != null)
							return result;
					}
				return super.locateEntityInParameter(node, processor, index, offsetInExpression, parmExpression);
			};
			@Override
			public void contributeAdditionalProposals(CallDeclaration node, ProblemReporter processor, int index, ASTNode parmExpression, ScriptCompletionProcessor completions, String prefix, int offset, List<ICompletionProposal> proposals) {
				if (index != 0)
					return;
				final IType t = node.predecessorInSequence() != null ? processor.typeOf(node.predecessorInSequence()) : processor.script();
				if (t != null) for (final IType ty : t)
					if (ty instanceof Definition) {
						final Definition def = (Definition) ty;
						final Variable actMapLocal = def.findLocalVariable("ActMap", true); //$NON-NLS-1$
						if (actMapLocal != null && actMapLocal.type() != null)
							for (final IType a : actMapLocal.type())
								if (a instanceof IProplistDeclaration) {
									final IProplistDeclaration proplDecl = (IProplistDeclaration) a;
									for (final Variable comp : proplDecl.components(true)) {
										if (prefix != null && !comp.name().toLowerCase().contains(prefix))
											continue;
										proposals.add(new ClonkCompletionProposal(comp, "\""+comp.name()+"\"", offset, prefix != null ? prefix.length() : 0, //$NON-NLS-1$ //$NON-NLS-2$
											comp.name().length()+2, UI.variableIcon(comp), comp.name(), null, comp.infoText(processor.script()), "", completions.editor()));
									}
								}
					}
			};
		};
	}

	@Override
	public void initialize() {
		super.initialize();
		putFuncRule(criteriaSearchRule, "FindObject"); //$NON-NLS-1$
	}

	private static final Pattern ID_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z_0-9]*");

	@Override
	public ID parseId(BufferedScanner scanner) {
		// HACK: Script parsers won't get IDs from this method because IDs are actually parsed as AccessVars and parsing them with
		// a <match all identifiers> pattern would cause zillions of err0rs
		if (scanner instanceof ScriptParser)
			return null;
		final Matcher idMatcher = ID_PATTERN.matcher(scanner.bufferSequence(scanner.tell()));
		if (idMatcher.lookingAt()) {
			final String idString = idMatcher.group();
			scanner.advance(idString.length());
			if (BufferedScanner.isWordPart(scanner.peek()) || BufferedScanner.NUMERAL_PATTERN.matcher(idString).matches()) {
				scanner.advance(-idString.length());
				return null;
			}
			return ID.get(idString);
		}
		return null;
	}

	public static final String CREATE_ENVIRONMENT = "CreateEnvironment";

	private static final ASTNode PLACE_CALL = ASTNodeMatcher.matchingExpr
		("$id$->$placeCall:/Place/$($num:NumberLiteral$, $params:...$)", Core.instance().loadEngine("OpenClonk"));

	public static class ComputedScenarioConfigurationEntry extends ComplexIniEntry {
		public ComputedScenarioConfigurationEntry(String key, IDArray values) {
			super(-1, -1, key, values);
		}
		private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
		public static class Item extends KeyValuePair<ID, Integer> {
			private transient CallDeclaration placeCall;
			@Override
			public Item clone() throws CloneNotSupportedException {
				final Item item = (Item)super.clone();
				item.placeCall = null; // better not to
				return item;
			}
			public CallDeclaration placeCall() { return placeCall; }
			public Item(ID first, Integer second, CallDeclaration placeCall) {
				super(first, second);
				this.placeCall = placeCall;
			}
			public boolean updatePlaceCall() {
				final NumberLiteral num = num();
				if (num == null || num.literal().intValue() != value()) {
					placeCall.replaceSubElement(num, new IntegerLiteral(value()), 0);
					return true;
				} else
					return false;
			}
			private NumberLiteral num() {
				return placeCall.params().length > 0 ? as(placeCall.params()[0], NumberLiteral.class) : null;
			}
			private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
		}
		@Override
		public boolean isTransient() {
			return true;
		}
		public boolean updatePlaceCalls(Function function, List<ASTNode> modified) {
			boolean wholeFunc = false;
			for (final KeyValuePair<ID, Integer> kv : value().components())
				if (kv instanceof Item) {
					final Item item = (Item)kv;
					if (item.updatePlaceCall())
						modified.add(item.num());
				} else {
					final Statement newStatement = SimpleStatement.wrapExpression(PLACE_CALL.transform(ArrayUtil.<String, Object>map(false,
						"id", new Object[] {new AccessVar(kv.key().stringValue())},
						"placeCall", new Object[] {new CallDeclaration("Place", new IntegerLiteral(kv.value()))}
					), null));
					wholeFunc = true;
					function.body().addStatements(newStatement);
				}
			return wholeFunc;
		}
		@Override
		public IDArray value() {
			return (IDArray) super.value();
		}
	}

	private static ComputedScenarioConfigurationEntry entry(ScenarioUnit unit, String section, String entry) {
		final IniSection s = unit.sectionWithName(section, true);
		IniItem i = s.subItemByKey(entry);
		if (i != null && !(i instanceof ComputedScenarioConfigurationEntry)) {
			s.removeItem(i);
			i = null;
		}
		if (i == null)
			s.addItem(i = new ComputedScenarioConfigurationEntry(entry, new IDArray()));
		return as(i, ComputedScenarioConfigurationEntry.class);
	}

	@Override
	public void processScenarioConfiguration(final ScenarioUnit unit, ScenarioConfigurationProcessing processing) {
		final Scenario scenario = unit.scenario();
		Function createEnvironment = scenario.findLocalFunction(CREATE_ENVIRONMENT, false);
		final ComputedScenarioConfigurationEntry vegetation = entry(unit, "Landscape", "Vegetation");
		final Definition plantLib = scenario.index().anyDefinitionWithID(ID.get("Library_Plant"));
		if (plantLib == null)
			return; // no plant library - give up
		class PlaceMatch {
			public boolean matched;
			public AccessVar id;
			public CallDeclaration placeCall;
			public NumberLiteral num;
			public Definition definition() { return id != null ? id.proxiedDefinition() : null; }
			public ComputedScenarioConfigurationEntry entry;
			public PlaceMatch(ASTNode s) {
				matched = match(s);
			}
			boolean determineConfigurationInsertionPoint() {
				final Definition d = definition();
				if (d != null)
					if (d.doesInclude(scenario.index(), plantLib)) {
						entry = vegetation;
						return true;
					}
				return false;
			}
			public boolean addComputedEntry() {
				if (matched) {
					entry.value().add(new ComputedScenarioConfigurationEntry.Item(definition().id(), num.literal().intValue(), placeCall));
					return true;
				} else
					return false;
			}
			private boolean match(ASTNode s) {
				return PLACE_CALL.match(s, this) && id != null && placeCall != null && determineConfigurationInsertionPoint();
			}
			public boolean matchedButNoCorrespondingItem() {
				return matched && entry.value().find(definition().id()) == null;
			}
		}
		switch (processing) {
		case Load:
			if (createEnvironment != null)
				for (final ASTNode s : createEnvironment.body().statements())
					(new PlaceMatch(SimpleStatement.unwrap(s))).addComputedEntry();
			break;
		case Save:
			if (createEnvironment == null)
				createEnvironment = appendFunction(scenario, CREATE_ENVIRONMENT);
			final List<ASTNode> list = new LinkedList<ASTNode>();
			boolean wholeFunc = vegetation.updatePlaceCalls(createEnvironment, list);
			final List<ASTNode> statementsCopy = new ArrayList<ASTNode>(Arrays.asList(createEnvironment.body().statements()));
			final List<Pair<ASTNode, PlaceMatch>> matches = new ArrayList<Pair<ASTNode, PlaceMatch>>(statementsCopy.size());
			for (int i = 0; i < statementsCopy.size(); i++)
				matches.add(new Pair<ASTNode, PlaceMatch>(statementsCopy.get(i), new PlaceMatch(statementsCopy.get(i))));
			for (int i = statementsCopy.size()-1; i >= 0; i--)
				if (matches.get(i).second().matchedButNoCorrespondingItem()) {
					statementsCopy.remove(i);
					matches.remove(i);
					wholeFunc = true;
				}
			class C implements Comparator<ASTNode> {
				public boolean reordering;
				private int indexOf(ASTNode s) {
					for (int i = 0; i < matches.size(); i++)
						if (matches.get(i).first() == s && matches.get(i).second().definition() != null) {
							int ndx = 0;
							for (final KeyValuePair<ID, Integer> kv : vegetation.value().components())
								if (kv.key().equals(matches.get(i).second().definition().id()))
									return ndx;
								else
									ndx++;
						}
					return -1;
				}
				@Override
				public int compare(ASTNode o1, ASTNode o2) {
					final int diff = indexOf(o1) - indexOf(o2);
					reordering |= diff < 0;
					return diff;
				}
			}
			final C comp = new C();
			Collections.sort(statementsCopy, comp);
			wholeFunc |= comp.reordering;
			if (wholeFunc) {
				createEnvironment.body().setStatements(statementsCopy.toArray(new Statement[statementsCopy.size()]));
				scenario.saveNodes(Arrays.asList(createEnvironment.body()), true);
			}
			else if (list.size() > 0)
				scenario.saveNodes(list, true);
			break;
		}
	}

	private Function appendFunction(final Script script, final String name) {
		Function f;
		Core.instance().performActionsOnFileDocument(script.scriptFile(), new IDocumentAction<Void>() {
			@Override
			public Void run(IDocument document) {
				final String oldContents = document.get();
				final StringBuilder builder = new StringBuilder(oldContents.length()+100);
				builder.append(oldContents);
				final boolean endsWithEmptyLine = oldContents.endsWith("\n");
				if (!endsWithEmptyLine)
					builder.append('\n');
				builder.append('\n');
				builder.append(Function.scaffoldTextRepresentation(name, FunctionScope.PUBLIC, script.index()));
				if (endsWithEmptyLine)
					builder.append('\n');
				document.set(builder.toString());
				return null;
			}
		}, true);
		try {
			new ScriptParser(script).parse();
		} catch (final ProblemException e) {}
		f = script.findLocalFunction(name, false);
		return f;
	}

	@Override
	public IPredicate<Definition> configurationEntryDefinitionFilter(final IniEntry entry) {
		final IPredicate<Definition> basePredicate =
			entry.key().equals("Vegetation") ?
				new IPredicate<Definition>() {
					final Definition plant = entry.index().anyDefinitionWithID(ID.get("Library_Plant"));
					@Override
					public boolean test(Definition item) {
						return plant != null && item != plant && item.doesInclude(entry.index(), plant);
					}
				} :
			entry.key().equals("Goals") ?
				new IPredicate<Definition>() {
					final Definition goal = entry.index().anyDefinitionWithID(ID.get("Library_Goal"));
					@Override
					public boolean test(Definition item) {
						return goal != null && item != goal && item.doesInclude(entry.index(), goal);
					}
				} :
			super.configurationEntryDefinitionFilter(entry);
		return basePredicate != null ? new IPredicate<Definition>() {
			@Override
			public boolean test(Definition item) {
				if (item.id() != null && item.id().stringValue().startsWith("Library_"))
					return false;
				else
					return basePredicate.test(item);
			}
		} : null;
	}

	private void readVariablesFromPlayerControlsFile(final Index index) {
		try {
			index.nature().getProject().accept(new IResourceVisitor() {
				@Override
				public boolean visit(IResource resource) throws CoreException {
					if (resource instanceof IContainer)
						return true;
					else if (resource instanceof IFile && resource.getName().equals("PlayerControls.txt")) { //$NON-NLS-1$
						final PlayerControlsUnit unit = (PlayerControlsUnit) Structure.pinned(resource, true, true);
						if (unit != null)
							index.addStaticVariables(unit.controlVariables());
						return true;
					}
					else
						return false;
				}
			});
		} catch (final CoreException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void refreshIndex(Index index) {
		super.refreshIndex(index);
		readVariablesFromPlayerControlsFile(index);
	}

}
