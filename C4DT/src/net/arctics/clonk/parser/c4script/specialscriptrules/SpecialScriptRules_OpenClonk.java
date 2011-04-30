package net.arctics.clonk.parser.c4script.specialscriptrules;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.arctics.clonk.index.Definition;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.DeclarationRegion;
import net.arctics.clonk.parser.ID;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.DefinitionFunction;
import net.arctics.clonk.parser.c4script.EffectFunction;
import net.arctics.clonk.parser.c4script.EffectFunction.HardcodedCallbackType;
import net.arctics.clonk.parser.c4script.EffectPropListDeclaration;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.ProplistDeclaration;
import net.arctics.clonk.parser.c4script.ScriptBase;
import net.arctics.clonk.parser.c4script.SpecialScriptRules;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.Variable.Scope;
import net.arctics.clonk.parser.c4script.ast.CallFunc;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.StringLiteral;
import net.arctics.clonk.parser.c4script.ast.evaluate.IEvaluationContext;
import net.arctics.clonk.ui.editors.ClonkCompletionProposal;
import net.arctics.clonk.ui.editors.c4script.C4ScriptCompletionProcessor;
import net.arctics.clonk.util.UI;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

public class SpecialScriptRules_OpenClonk extends SpecialScriptRules {

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
			if (function instanceof EffectFunction) {
				EffectFunction fun = (EffectFunction) function;
				fun.findStartCallback();
				EffectFunction startFunction = fun.getHardcodedCallbackType() == EffectFunction.HardcodedCallbackType.Start
					? null
					: fun.getStartFunction();
				// parse *Start function first. Will define ad-hoc proplist type
				if (startFunction != null) {
					try {
						parser.parseCodeOfFunction(startFunction, true);
					} catch (ParsingException e) {
						e.printStackTrace();
					}
				}
				IType effectProplistType;
				if (startFunction != null) {
					// not the start function - get effect parameter type from start function
					effectProplistType = startFunction.getEffectType();
				} else {
					// this is the start function - create type if parameter present
					if (fun.getParameters().size() < 2)
						effectProplistType = PrimitiveType.PROPLIST;
					else
						effectProplistType = createAdHocProplistDeclaration(fun, fun.getParameters().get(1));
				}
				function.assignParameterTypes(PrimitiveType.OBJECT, effectProplistType);
				return true;
			}
			return false;
		}
		private IType createAdHocProplistDeclaration(EffectFunction startFunction, Variable effectParameter) {
			ProplistDeclaration result = new EffectPropListDeclaration(startFunction.getEffectName(), null);
			result.setLocation(effectParameter.getLocation());
			result.setParentDeclaration(startFunction);
			startFunction.addOtherDeclaration(result);
			return result;
		}
		@Override
		public Function newFunction(String name) {
			if (name.startsWith(EffectFunction.FUNCTION_NAME_PREFIX)) {
				for (EffectFunction.HardcodedCallbackType t : EffectFunction.HardcodedCallbackType.values()) {
					Matcher m = t.getPattern().matcher(name);
					if (m.matches())
						return new EffectFunction(m.group(1), t);
				}
				// hard to match sequence of two arbitrary, non-separate strings ;c
				return new EffectFunction(null, null);
			}
			return null;
		};
		@Override
		public IType returnType(DeclarationObtainmentContext context, CallFunc callFunc) {
			Object parmEv;
			if (callFunc.getParams().length >= 1 && (parmEv = callFunc.getParams()[0].evaluateAtParseTime(context.getCurrentFunc())) instanceof String) {
				String effectName = (String) parmEv;
				for (EffectFunction.HardcodedCallbackType t : EffectFunction.HardcodedCallbackType.values()) {
					Declaration d = CallFunc.findFunctionUsingPredecessor(
							callFunc.getPredecessorInSequence(),
							String.format(EffectFunction.FUNCTION_NAME_FORMAT, effectName, t.name()), 
							context, null
					);
					if (d instanceof EffectFunction) {
						EffectFunction effFun = (EffectFunction)d;
						// parse Start function of effect so ad-hoc variables are known
						if (!(context.getCurrentFunc() instanceof EffectFunction)) {
							EffectFunction f = effFun.getStartFunction() != null ? effFun.getStartFunction() : effFun;
							try {
								context.parseCodeOfFunction(f, false);
							} catch (ParsingException e) {
								// e.printStackTrace();
							}
						}
						return effFun.getEffectType();
					}
				}
			}
			return null;
		};
		@Override
		public DeclarationRegion locateDeclarationInParameter(
				CallFunc callFunc, C4ScriptParser parser, int index,
				int offsetInExpression, ExprElm parmExpression) {
			if (parmExpression instanceof StringLiteral && callFunc.getParams().length >= 1 && callFunc.getParams()[0] == parmExpression) {
				String effectName = ((StringLiteral)parmExpression).getLiteral();
				for (HardcodedCallbackType t : HardcodedCallbackType.values()) {
					Declaration d = CallFunc.findFunctionUsingPredecessor(
							callFunc.getPredecessorInSequence(),
							String.format(EffectFunction.FUNCTION_NAME_FORMAT, effectName, t.name()), 
							parser,
							null
					);
					if (d instanceof EffectFunction) {
						return new DeclarationRegion(d, new Region(parmExpression.getExprStart()+1, parmExpression.getLength()-2));
					}
				}
			}
			return super.locateDeclarationInParameter(callFunc, parser, index, offsetInExpression, parmExpression);
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
			if (name.equals("Definition")) { //$NON-NLS-1$
				return new DefinitionFunction();
			}
			else
				return null;
		};
		@Override
		public boolean validateArguments(CallFunc callFunc, ExprElm[] arguments, C4ScriptParser parser) {
			if (arguments.length >= 2 && parser.getCurrentFunc() instanceof DefinitionFunction) {
				Object nameEv = arguments[0].evaluateAtParseTime(parser.getCurrentFunc());
				if (nameEv instanceof String) {
					Variable var = new Variable((String) nameEv, arguments[1].getType(parser));
					var.setLocation(parser.absoluteSourceLocationFromExpr(arguments[0]));
					var.setScope(Scope.LOCAL);
					var.setInitializationExpression(arguments[1]);
					var.setParentDeclaration(parser.getCurrentFunc());
					parser.getContainer().addDeclaration(var);
				}
			}
			return false; // default validation
		};
		@Override
		public void functionAboutToBeParsed(Function function, C4ScriptParser context) {
			if (function.getName().equals("Definition")) //$NON-NLS-1$
				return;
			Function definitionFunc = function.getScript().findLocalFunction("Definition", false); //$NON-NLS-1$
			if (definitionFunc != null) {
				try {
					context.parseCodeOfFunction(definitionFunc, true);
				} catch (ParsingException e) {
					e.printStackTrace();
				}
			}
		};
	};
	
	private static class EvaluationTracer implements IEvaluationContext {
		public ExprElm topLevelExpression;
		public IFile tracedFile;
		public IRegion tracedLocation;
		public ScriptBase script;
		public Function function;
		public Object[] arguments;
		public Object evaluation;
		public EvaluationTracer(ExprElm topLevelExpression, Object[] arguments, Function function, ScriptBase script) {
			this.topLevelExpression = topLevelExpression;
			this.arguments = arguments;
			this.function = function;
			this.script = script;
		}
		@Override
		public Object[] getArguments() {
			return arguments;
		}
		@Override
		public ScriptBase getScript() {
			return script;
		}
		@Override
		public int getCodeFragmentOffset() {
			return function != null ? function.getCodeFragmentOffset() : 0;
		}
		@Override
		public Object getValueForVariable(String varName) {
			return function != null ? function.getValueForVariable(varName) : null;
		}
		@Override
		public Function getFunction() {
			return function;
		}
		@Override
		public void reportOriginForExpression(ExprElm expression, IRegion location, IFile file) {
			if (expression == topLevelExpression) {
				tracedLocation = location;
				tracedFile = file;
			}
		}
		public static EvaluationTracer evaluate(ExprElm expression, Object[] arguments, ScriptBase script, Function function) {
			EvaluationTracer tracer = new EvaluationTracer(expression, arguments, function, script);
			tracer.evaluation = expression.evaluateAtParseTime(tracer);
			return tracer;
		}
		public static EvaluationTracer evaluate(ExprElm expression, Function function) {
			return evaluate(expression, null, function.getScript(), function);
		}
	}
	
	/**
	 * Validate format strings.
	 */
	@AppliedTo(functions={"Log", "Message", "Format"})
	public final SpecialFuncRule formatArgumentsValidationRule = new SpecialFuncRule() {
		private boolean checkParm(CallFunc callFunc, final ExprElm[] arguments, final C4ScriptParser parser, int parmIndex, String formatString, int rangeStart, int rangeEnd, EvaluationTracer evTracer, IType expectedType) throws ParsingException {
			ExprElm saved = parser.currentFunctionContext().expressionReportingErrors;			
			try {
				if (parmIndex+1 >= arguments.length) {
					if (evTracer.tracedFile == null)
						return true;
					parser.currentFunctionContext().expressionReportingErrors = arguments[0];
					if (evTracer.tracedFile.equals(parser.getContainer().getScriptFile())) {
						parser.errorWithCode(ParserErrorCode.MissingFormatArg, evTracer.tracedLocation.getOffset()+rangeStart, evTracer.tracedLocation.getOffset()+rangeEnd, C4ScriptParser.NO_THROW|C4ScriptParser.ABSOLUTE_MARKER_LOCATION,
								formatString, evTracer.evaluation, evTracer.tracedFile.getProjectRelativePath().toOSString());
						return !arguments[0].containsOffset(evTracer.tracedLocation.getOffset());
					} else {
						parser.errorWithCode(ParserErrorCode.MissingFormatArg, arguments[0], C4ScriptParser.NO_THROW,
								formatString, evTracer.evaluation, evTracer.tracedFile.getProjectRelativePath().toOSString());
					}
				}
				else if (!expectedType.canBeAssignedFrom(arguments[parmIndex+1].getType(parser))) {
					if (evTracer.tracedFile == null)
						return true;
					parser.currentFunctionContext().expressionReportingErrors = arguments[parmIndex+1];
					parser.errorWithCode(ParserErrorCode.IncompatibleFormatArgType, arguments[parmIndex+1],
						C4ScriptParser.NO_THROW, expectedType.typeName(false), arguments[parmIndex+1].getType(parser).typeName(false), evTracer.evaluation, evTracer.tracedFile.getProjectRelativePath().toOSString());
				}
			} finally {
				parser.currentFunctionContext().expressionReportingErrors = saved;
			}
			return false;
		}
		@Override
		public boolean validateArguments(CallFunc callFunc, ExprElm[] arguments, C4ScriptParser parser) throws ParsingException {
			EvaluationTracer evTracer;
			int parmIndex = 0;
			if (arguments.length >= 1 && (evTracer = EvaluationTracer.evaluate(arguments[0], parser.getCurrentFunc())).evaluation instanceof String) {
				final String formatString = (String)evTracer.evaluation;
				boolean separateIssuesMarker = false;
				for (int i = 0; i < formatString.length(); i++) {
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
						if (requiredType != null) {
							separateIssuesMarker |= checkParm(callFunc, arguments, parser, parmIndex, format, i+1, j+2, evTracer, requiredType); 
						}
						i = j;
						parmIndex++;
					}
				}
				if (separateIssuesMarker)
					parser.errorWithCode(ParserErrorCode.DragonsHere, arguments[0], C4ScriptParser.NO_THROW);
			}
			return false; // let others validate as well
		};
	};
	
	public SpecialScriptRules_OpenClonk() {
		super();
		// override SetAction link rule to also take into account local 'ActMap' vars
		setActionLinkRule = new SetActionLinkRule() {
			@Override
			protected DeclarationRegion getActionLinkForDefinition(Function currentFunction, Definition definition, ExprElm parmExpression) {
				if (definition == null)
					return null;
				Object parmEv;
				DeclarationRegion result = super.getActionLinkForDefinition(currentFunction, definition, parmExpression);
				if (result != null)
					return result;
				else if ((parmEv = parmExpression.evaluateAtParseTime(currentFunction)) instanceof String) {
					Variable actMapLocal = definition.findLocalVariable("ActMap", true); //$NON-NLS-1$
					if (actMapLocal != null && actMapLocal.getType() != null) {
						for (IType ty : actMapLocal.getType()) if (ty instanceof ProplistDeclaration) {
							ProplistDeclaration proplDecl = (ProplistDeclaration) ty;
							Variable action = proplDecl.findComponent((String)parmEv);
							if (action != null)
								return new DeclarationRegion(action, parmExpression);
						}
					}
				}
				return null;
			};
			@Override
			public DeclarationRegion locateDeclarationInParameter(CallFunc callFunc, C4ScriptParser parser, int index, int offsetInExpression, ExprElm parmExpression) {
				if (index != 0)
					return null;
				IType t = callFunc.getPredecessorInSequence() != null ? callFunc.getPredecessorInSequence().getType(parser) : null;
				if (t != null) for (IType ty : t) {
					if (ty instanceof Definition) {
						DeclarationRegion result = getActionLinkForDefinition(parser.getCurrentFunc(), (Definition)ty, parmExpression);
						if (result != null)
							return result;
					}
				}
				return super.locateDeclarationInParameter(callFunc, parser, index, offsetInExpression, parmExpression);
			};
			@Override
			public void contributeAdditionalProposals(CallFunc callFunc, C4ScriptParser parser, int index, ExprElm parmExpression, C4ScriptCompletionProcessor processor, String prefix, int offset, List<ICompletionProposal> proposals) {
				if (index != 0)
					return;
				IType t = callFunc.getPredecessorInSequence() != null ? callFunc.getPredecessorInSequence().getType(parser) : parser.getContainer();
				if (t != null) for (IType ty : t) {
					if (ty instanceof Definition) {
						Definition def = (Definition) ty;
						Variable actMapLocal = def.findLocalVariable("ActMap", true); //$NON-NLS-1$
						if (actMapLocal != null && actMapLocal.getType() != null) {
							for (IType a : actMapLocal.getType()) {
								if (a instanceof ProplistDeclaration) {
									ProplistDeclaration proplDecl = (ProplistDeclaration) a;
									for (Variable comp : proplDecl.getComponents()) {
										if (prefix != null && !comp.getName().toLowerCase().contains(prefix))
											continue;
										proposals.add(new ClonkCompletionProposal(comp, "\""+comp.getName()+"\"", offset, prefix != null ? prefix.length() : 0, //$NON-NLS-1$ //$NON-NLS-2$
											comp.getName().length()+2, UI.getIconForVariable(comp), String.format(Messages.SpecialScriptRules_OpenClonk_ActionCompletionTemplate, comp.getName()), null, comp.getInfoText(), "", processor.getEditor())); //$NON-NLS-2$
									}
								}
							}
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

	private static final Matcher ID_MATCHER = Pattern.compile("[A-Za-z_][A-Za-z_0-9]*").matcher("");
	
	@Override
	public ID parseId(BufferedScanner scanner) {
		// HACK: Script parsers won't get IDs from this method because IDs are actually parsed as AccessVars and parsing them with
		// a <match all identifiers> pattern would cause zillions of err0rs
		if (scanner instanceof C4ScriptParser)
			return null;
		if (ID_MATCHER.reset(scanner.getBuffer().substring(scanner.getPosition())).lookingAt()) {
			String idString = ID_MATCHER.group();
			scanner.advance(idString.length());
			if (BufferedScanner.isWordPart(scanner.peek()) || C4ScriptParser.NUMERAL_PATTERN.matcher(idString).matches()) {
				scanner.advance(-idString.length());
				return null;
			}
			return ID.getID(idString);
		}
		return null;
	}

}
