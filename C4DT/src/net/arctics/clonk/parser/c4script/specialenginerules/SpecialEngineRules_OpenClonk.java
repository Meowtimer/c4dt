package net.arctics.clonk.parser.c4script.specialenginerules;

import static net.arctics.clonk.util.Utilities.as;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.arctics.clonk.Core;
import net.arctics.clonk.Core.IDocumentAction;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.IIndexEntity;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.Scenario;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.EntityRegion;
import net.arctics.clonk.parser.ID;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.DefinitionFunction;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.Function.FunctionScope;
import net.arctics.clonk.parser.c4script.IProplistDeclaration;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.SpecialEngineRules;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.Variable.Scope;
import net.arctics.clonk.parser.c4script.ast.AccessVar;
import net.arctics.clonk.parser.c4script.ast.CallDeclaration;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.LongLiteral;
import net.arctics.clonk.parser.c4script.ast.NumberLiteral;
import net.arctics.clonk.parser.c4script.ast.PropListExpression;
import net.arctics.clonk.parser.c4script.ast.SimpleStatement;
import net.arctics.clonk.parser.c4script.ast.Statement;
import net.arctics.clonk.parser.c4script.ast.StringLiteral;
import net.arctics.clonk.parser.c4script.ast.evaluate.IEvaluationContext;
import net.arctics.clonk.parser.c4script.effect.Effect;
import net.arctics.clonk.parser.c4script.effect.EffectFunction;
import net.arctics.clonk.parser.inireader.ComplexIniEntry;
import net.arctics.clonk.parser.inireader.IDArray;
import net.arctics.clonk.parser.inireader.IniEntry;
import net.arctics.clonk.parser.inireader.IniItem;
import net.arctics.clonk.parser.inireader.IniSection;
import net.arctics.clonk.parser.inireader.ScenarioUnit;
import net.arctics.clonk.parser.playercontrols.PlayerControlsUnit;
import net.arctics.clonk.ui.editors.ClonkCompletionProposal;
import net.arctics.clonk.ui.editors.c4script.C4ScriptCompletionProcessor;
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
		public boolean assignDefaultParmTypes(C4ScriptParser parser, Function function) {
			EffectFunction fun = as(function, EffectFunction.class);
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
		public IType returnType(DeclarationObtainmentContext context, CallDeclaration callFunc) {
			Object parmEv;
			if (callFunc.params().length >= 1 && (parmEv = callFunc.params()[0].evaluateAtParseTime(context.currentFunction())) instanceof String) {
				String effectName = (String) parmEv;
				return context.script().effects().get(effectName);
			}
			return null;
		};
		@Override
		public EntityRegion locateEntityInParameter(
			CallDeclaration callFunc, C4ScriptParser parser, int index,
			int offsetInExpression, ExprElm parmExpression
		) {
			if (parmExpression instanceof StringLiteral && callFunc.params().length >= 1 && callFunc.params()[0] == parmExpression) {
				String effectName = ((StringLiteral)parmExpression).literal();
				Effect effect = parser.script().effects().get(effectName);
				if (effect != null)
					return new EntityRegion(new HashSet<IIndexEntity>(effect.functions().values()), new Region(parmExpression.start()+1, parmExpression.getLength()-2));
			}
			return super.locateEntityInParameter(callFunc, parser, index, offsetInExpression, parmExpression);
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
		public boolean validateArguments(CallDeclaration callFunc, ExprElm[] arguments, C4ScriptParser parser) {
			if (arguments.length >= 2 && parser.currentFunction() instanceof DefinitionFunction) {
				Object nameEv = arguments[0].evaluateAtParseTime(parser.currentFunction());
				if (nameEv instanceof String) {
					SourceLocation loc = parser.absoluteSourceLocationFromExpr(arguments[0]);
					Variable var = parser.createVarInScope((String) nameEv, Scope.LOCAL, loc.start(), loc.end(), null);
					var.setLocation(parser.absoluteSourceLocationFromExpr(arguments[0]));
					var.setScope(Scope.LOCAL);
					// clone argument since the offset of the expression inside the func body is relative while
					// the variable initialization expression location is supposed to be absolute
					ExprElm initializationClone = arguments[1].clone();
					initializationClone.incrementLocation(parser.bodyOffset());
					var.setInitializationExpression(initializationClone);
					var.forceType(arguments[1].type(parser));
					new AccessVar(var).assignment(arguments[1], parser);
					var.setParentDeclaration(parser.currentFunction());
					//parser.getContainer().addDeclaration(var);
				}
			}
			return false; // default validation
		};
		@Override
		public void functionAboutToBeParsed(Function function, C4ScriptParser context) {
			if (function.name().equals(DEFINITION_FUNCTION)) //$NON-NLS-1$
				return;
			Function definitionFunc = function.script().findLocalFunction(DEFINITION_FUNCTION, false); //$NON-NLS-1$
			if (definitionFunc != null)
				context.reportProblems(definitionFunc);
		};
	};
	
	private static class EvaluationTracer implements IEvaluationContext {
		public ExprElm topLevelExpression;
		public IFile tracedFile;
		public IRegion tracedLocation;
		public Script script;
		public Function function;
		public Object[] arguments;
		public Object evaluation;
		public EvaluationTracer(ExprElm topLevelExpression, Object[] arguments, Function function, Script script) {
			this.topLevelExpression = topLevelExpression;
			this.arguments = arguments;
			this.function = function;
			this.script = script;
		}
		@Override
		public Object[] arguments() {
			return arguments;
		}
		@Override
		public Script script() {
			return script;
		}
		@Override
		public int codeFragmentOffset() {
			return function != null ? function.codeFragmentOffset() : 0;
		}
		@Override
		public Object valueForVariable(String varName) {
			return function != null ? function.valueForVariable(varName) : null;
		}
		@Override
		public Function function() {
			return function;
		}
		@Override
		public void reportOriginForExpression(ExprElm expression, IRegion location, IFile file) {
			if (expression == topLevelExpression) {
				tracedLocation = location;
				tracedFile = file;
			}
		}
		public static EvaluationTracer evaluate(ExprElm expression, Object[] arguments, Script script, Function function) {
			EvaluationTracer tracer = new EvaluationTracer(expression, arguments, function, script);
			tracer.evaluation = expression.evaluateAtParseTime(tracer);
			return tracer;
		}
		public static EvaluationTracer evaluate(ExprElm expression, Function function) {
			return evaluate(expression, null, function.script(), function);
		}
	}
	
	/**
	 * Validate format strings.
	 */
	@AppliedTo(functions={"Log", "Message", "Format"})
	public final SpecialFuncRule formatArgumentsValidationRule = new SpecialFuncRule() {
		private boolean checkParm(CallDeclaration callFunc, final ExprElm[] arguments, final C4ScriptParser parser, int parmIndex, String formatString, int rangeStart, int rangeEnd, EvaluationTracer evTracer, IType expectedType) throws ParsingException {
			ExprElm saved = parser.problemReporter();			
			try {
				if (parmIndex+1 >= arguments.length) {
					if (evTracer.tracedFile == null)
						return true;
					parser.setProblemReporter(arguments[0]);
					if (evTracer.tracedFile.equals(parser.script().scriptFile())) {
						parser.error(ParserErrorCode.MissingFormatArg, evTracer.tracedLocation.getOffset()+rangeStart, evTracer.tracedLocation.getOffset()+rangeEnd, C4ScriptParser.NO_THROW|C4ScriptParser.ABSOLUTE_MARKER_LOCATION,
								formatString, evTracer.evaluation, evTracer.tracedFile.getProjectRelativePath().toOSString());
						return !arguments[0].containsOffset(evTracer.tracedLocation.getOffset());
					} else
						parser.error(ParserErrorCode.MissingFormatArg, arguments[0], C4ScriptParser.NO_THROW,
								formatString, evTracer.evaluation, evTracer.tracedFile.getProjectRelativePath().toOSString());
				}
				else if (!expectedType.canBeAssignedFrom(arguments[parmIndex+1].type(parser))) {
					if (evTracer.tracedFile == null)
						return true;
					parser.setProblemReporter(arguments[parmIndex+1]);
					parser.error(ParserErrorCode.IncompatibleFormatArgType, arguments[parmIndex+1],
						C4ScriptParser.NO_THROW, expectedType.typeName(false), arguments[parmIndex+1].type(parser).typeName(false), evTracer.evaluation, evTracer.tracedFile.getProjectRelativePath().toOSString());
				}
			} finally {
				parser.setProblemReporter(saved);
			}
			return false;
		}
		@Override
		public boolean validateArguments(CallDeclaration callFunc, ExprElm[] arguments, C4ScriptParser parser) throws ParsingException {
			EvaluationTracer evTracer;
			int parmIndex = 0;
			if (arguments.length >= 1 && (evTracer = EvaluationTracer.evaluate(arguments[0], parser.currentFunction())).evaluation instanceof String) {
				final String formatString = (String)evTracer.evaluation;
				boolean separateIssuesMarker = false;
				for (int i = 0; i < formatString.length(); i++)
					if (formatString.charAt(i) == '%') {
						int j;
						for (j = i+1; j < formatString.length() && (formatString.charAt(j) == '.' || (formatString.charAt(j) >= '0' && formatString.charAt(j) <= '9')); j++);
						if (j >= formatString.length())
							break;
						String format = formatString.substring(i, j+1);
						IType requiredType = null;
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
						case '%':
							break;
						}
						if (requiredType != null)
							separateIssuesMarker |= checkParm(callFunc, arguments, parser, parmIndex, format, i+1, j+2, evTracer, requiredType);
						i = j;
						parmIndex++;
					}
				if (separateIssuesMarker)
					parser.error(ParserErrorCode.DragonsHere, arguments[0], C4ScriptParser.NO_THROW);
			}
			return false; // let others validate as well
		};
	};
	
	public SpecialEngineRules_OpenClonk() {
		super();
		// override SetAction link rule to also take into account local 'ActMap' vars
		setActionLinkRule = new SetActionLinkRule() {
			@Override
			protected EntityRegion getActionLinkForDefinition(Function currentFunction, Definition definition, ExprElm parmExpression) {
				if (definition == null)
					return null;
				Object parmEv;
				EntityRegion result = super.getActionLinkForDefinition(currentFunction, definition, parmExpression);
				if (result != null)
					return result;
				else if ((parmEv = parmExpression.evaluateAtParseTime(currentFunction)) instanceof String) {
					Variable actMapLocal = definition.findLocalVariable("ActMap", true); //$NON-NLS-1$
					if (actMapLocal != null && actMapLocal.initializationExpression() instanceof PropListExpression) {
						IProplistDeclaration proplDecl = ((PropListExpression)actMapLocal.initializationExpression()).definedDeclaration();
						Variable action = proplDecl.findComponent((String)parmEv);
						if (action != null)
							return new EntityRegion(action, parmExpression);
					}
				}
				return null;
			};
			@Override
			public EntityRegion locateEntityInParameter(CallDeclaration callFunc, C4ScriptParser parser, int index, int offsetInExpression, ExprElm parmExpression) {
				if (index != 0)
					return null;
				IType t = callFunc.predecessorInSequence() != null ? callFunc.predecessorInSequence().type(parser) : null;
				if (t != null) for (IType ty : t)
					if (ty instanceof Definition) {
						EntityRegion result = getActionLinkForDefinition(parser.currentFunction(), (Definition)ty, parmExpression);
						if (result != null)
							return result;
					}
				return super.locateEntityInParameter(callFunc, parser, index, offsetInExpression, parmExpression);
			};
			@Override
			public void contributeAdditionalProposals(CallDeclaration callFunc, C4ScriptParser parser, int index, ExprElm parmExpression, C4ScriptCompletionProcessor processor, String prefix, int offset, List<ICompletionProposal> proposals) {
				if (index != 0)
					return;
				IType t = callFunc.predecessorInSequence() != null ? callFunc.predecessorInSequence().type(parser) : parser.script();
				if (t != null) for (IType ty : t)
					if (ty instanceof Definition) {
						Definition def = (Definition) ty;
						Variable actMapLocal = def.findLocalVariable("ActMap", true); //$NON-NLS-1$
						if (actMapLocal != null && actMapLocal.type() != null)
							for (IType a : actMapLocal.type())
								if (a instanceof IProplistDeclaration) {
									IProplistDeclaration proplDecl = (IProplistDeclaration) a;
									for (Variable comp : proplDecl.components(true)) {
										if (prefix != null && !comp.name().toLowerCase().contains(prefix))
											continue;
										proposals.add(new ClonkCompletionProposal(comp, "\""+comp.name()+"\"", offset, prefix != null ? prefix.length() : 0, //$NON-NLS-1$ //$NON-NLS-2$
											comp.name().length()+2, UI.variableIcon(comp), String.format(Messages.specialEngineRules_OpenClonk_ActionCompletionTemplate, comp.name()), null, comp.infoText(parser.script()), "", processor.editor())); 
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
		if (scanner instanceof C4ScriptParser)
			return null;
		Matcher idMatcher = ID_PATTERN.matcher(scanner.buffer().substring(scanner.tell()));
		if (idMatcher.lookingAt()) {
			String idString = idMatcher.group();
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
	
	private static final ExprElm PLACE_CALL = C4ScriptParser.matchingExpr
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
				Item item = (Item)super.clone();
				item.placeCall = null; // better not to
				return item;
			}
			public CallDeclaration placeCall() { return placeCall; }
			public Item(ID first, Integer second, CallDeclaration placeCall) {
				super(first, second);
				this.placeCall = placeCall;
			}
			public boolean updatePlaceCall() {
				NumberLiteral num = num();
				if (num == null || num.literal().intValue() != value()) {
					placeCall.replaceSubElement(num, new LongLiteral(value()), 0);
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
		public boolean updatePlaceCalls(Function function, List<ExprElm> modified) {
			boolean wholeFunc = false;
			for (KeyValuePair<ID, Integer> kv : value().components())
				if (kv instanceof Item) {
					Item item = (Item)kv;
					if (item.updatePlaceCall())
						modified.add(item.num());
				} else {
					Statement newStatement = SimpleStatement.wrapExpression(PLACE_CALL.transform(ArrayUtil.<String, Object>map(false,
						"id", new AccessVar(kv.key().stringValue()),
						"placeCall", new CallDeclaration("Place", new LongLiteral(kv.value()))
					)));
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
		IniSection s = unit.sectionWithName(section, true);
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
			public PlaceMatch(ExprElm s) {
				matched = match(s);
			}
			boolean determineConfigurationInsertionPoint() {
				Definition d = definition();
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
			private boolean match(ExprElm s) {
				return PLACE_CALL.match(s, this) && id != null && placeCall != null && determineConfigurationInsertionPoint();
			}
			public boolean matchedButNoCorrespondingItem() {
				return matched && entry.value().find(definition().id()) == null;
			}
		}
		switch (processing) {
		case Load:
			if (createEnvironment != null)
				for (final Statement s : createEnvironment.body().statements())
					(new PlaceMatch(SimpleStatement.unwrap(s))).addComputedEntry();
			break;
		case Save:
			if (createEnvironment == null)
				createEnvironment = appendFunction(scenario, CREATE_ENVIRONMENT);
			List<ExprElm> list = new LinkedList<ExprElm>();
			boolean wholeFunc = vegetation.updatePlaceCalls(createEnvironment, list);
			final List<Statement> statementsCopy = new ArrayList<Statement>(Arrays.asList(createEnvironment.body().statements()));
			final List<Pair<Statement, PlaceMatch>> matches = new ArrayList<Pair<Statement, PlaceMatch>>(statementsCopy.size());
			for (int i = 0; i < statementsCopy.size(); i++)
				matches.add(new Pair<Statement, PlaceMatch>(statementsCopy.get(i), new PlaceMatch(statementsCopy.get(i))));
			for (int i = statementsCopy.size()-1; i >= 0; i--)
				if (matches.get(i).second().matchedButNoCorrespondingItem()) {
					statementsCopy.remove(i);
					matches.remove(i);
					wholeFunc = true;
				}
			class C implements Comparator<Statement> {
				public boolean reordering;
				private int indexOf(Statement s) {
					for (int i = 0; i < matches.size(); i++)
						if (matches.get(i).first() == s && matches.get(i).second().definition() != null) {
							int ndx = 0;
							for (KeyValuePair<ID, Integer> kv : vegetation.value().components())
								if (kv.key().equals(matches.get(i).second().definition().id()))
									return ndx;
								else
									ndx++;
						}
					return -1;
				}
				@Override
				public int compare(Statement o1, Statement o2) {
					int diff = indexOf(o1) - indexOf(o2);
					reordering |= diff < 0;
					return diff;
				}
			}
			C comp = new C();
			Collections.sort(statementsCopy, comp);
			wholeFunc |= comp.reordering;
			if (wholeFunc) {
				createEnvironment.body().setStatements(statementsCopy.toArray(new Statement[statementsCopy.size()]));
				scenario.saveExpressions(Arrays.asList(createEnvironment.body()), true);
			}
			else if (list.size() > 0)
				scenario.saveExpressions(list, true);
			break;
		}
	}

	private Function appendFunction(final Script script, final String name) {
		Function f;
		Core.instance().performActionsOnFileDocument(script.scriptFile(), new IDocumentAction<Void>() {
			@Override
			public Void run(IDocument document) {
				String oldContents = document.get();
				StringBuilder builder = new StringBuilder(oldContents.length()+100);
				builder.append(oldContents);
				boolean endsWithEmptyLine = oldContents.endsWith("\n");
				if (!endsWithEmptyLine)
					builder.append('\n');
				builder.append('\n');
				builder.append(Function.scaffoldTextRepresentation(name, FunctionScope.PUBLIC));
				if (endsWithEmptyLine)
					builder.append('\n');
				document.set(builder.toString());
				return null;
			}
		});
		try {
			new C4ScriptParser(script).parse();
		} catch (ParsingException e) {}
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
			index.project().accept(new IResourceVisitor() {
				@Override
				public boolean visit(IResource resource) throws CoreException {
					if (resource instanceof IContainer)
						return true;
					else if (resource instanceof IFile && resource.getName().equals("PlayerControls.txt")) { //$NON-NLS-1$
						PlayerControlsUnit unit = (PlayerControlsUnit) Structure.pinned(resource, true, true);
						if (unit != null)
							index.addStaticVariables(unit.controlVariables());
						return true;
					}
					else
						return false;
				}
			});
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void refreshIndex(Index index) {
		super.refreshIndex(index);
		readVariablesFromPlayerControlsFile(index);
	}

}
