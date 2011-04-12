package net.arctics.clonk.parser.c4script;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.ProjectDefinition;
import net.arctics.clonk.index.ProjectIndex;
import net.arctics.clonk.index.Scenario;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.DeclarationRegion;
import net.arctics.clonk.parser.ID;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser.IMarkerListener;
import net.arctics.clonk.parser.c4script.Directive.DirectiveType;
import net.arctics.clonk.parser.c4script.IHasConstraint.ConstraintKind;
import net.arctics.clonk.parser.c4script.ast.CallFunc;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.StringLiteral;
import net.arctics.clonk.parser.c4script.ast.evaluate.IEvaluationContext;
import net.arctics.clonk.parser.inireader.ActMapUnit;
import net.arctics.clonk.parser.inireader.IniSection;
import net.arctics.clonk.parser.inireader.IniUnit;
import net.arctics.clonk.parser.inireader.IniUnitWithNamedSections;
import net.arctics.clonk.parser.inireader.ParticleUnit;
import net.arctics.clonk.ui.editors.c4script.C4ScriptCompletionProcessor;
import net.arctics.clonk.ui.editors.c4script.ExpressionLocator;
import net.arctics.clonk.util.Utilities;

/**
 * This class contains some special rules that are applied to certain objects during parsing
 * (like CallFuncians and other species dwelling in ASTs)
 * @author madeen
 *
 */
public class SpecialScriptRules {
	
	/**
	 * Role for SpecialFuncRule: Validate arguments of a function in a special way.
	 */
	public static final int ARGUMENT_VALIDATOR = 1;
	
	/**
	 * Role for SpecialFuncRule: Modify the return type of a function call.
	 */
	public static final int RETURNTYPE_MODIFIER = 2;
	
	/**
	 * Role for SpecialFuncRule: Locate declarations inside a parameter of a function call for link-clicking.
	 */
	public static final int DECLARATION_LOCATOR = 4;
	
	/**
	 * Role for SpecialFuncRule: Assign default types to functions 
	 */
	public static final int DEFAULT_PARMTYPE_ASSIGNER = 8;
	
	/**
	 * Role for SpecialFuncRule: Gets notified when a function is about to be parsed.
	 */
	public static final int FUNCTION_EVENT_LISTENER = 16;
	
	/**
	 * Role for SpecialFuncRule: Gets queried for additional proposals for parameters of certain functions.
	 * @author madeen
	 *
	 */
	public static final int FUNCTION_PARM_PROPOSALS_CONTRIBUTOR = 32;

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface SignifiesRole {
		public int role();
	}
	
	/**
	 * Annotation for specifying what functions a special rule is applied to.
	 * @author madeen
	 *
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface AppliedTo {
		/**
		 * Function names.
		 * @return The function names
		 */
		public String[] functions();
		/**
		 * Only applied to the specified functions in a matching role context.
		 * @return The role mask
		 */
		public int role() default Integer.MAX_VALUE;
	}
	
	/**
	 * For multiple AppliedTo annotations...
	 * @author madeen
	 *
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface AppliedTos {
		public AppliedTo[] list();
	}
	
	/**
	 * Base class for special rules.
	 * @author madeen
	 *
	 */
	public static abstract class SpecialRule {
		/**
		 * Return the list of functions an instance of SpecialRule should be applied to.
		 * Obtained from an @AppliedTo annotation attached to the passed instance field refering to the actual rule. 
		 * @param instanceReference The field with the attached @AppliedTo annotation 
		 * @return Returns empty array if no AppliedTo annotation exists for the field, annotation.functions() if one exists.
		 */
		public static String[] getFunctionsAppliedTo(Field instanceReference, int role) {
			AppliedTo[] annots;
			AppliedTo annot = instanceReference.getAnnotation(AppliedTo.class);
			if (annot != null)
				annots = new AppliedTo[] {annot};
			else {
				AppliedTos list = instanceReference.getAnnotation(AppliedTos.class);
				if (list != null)
					annots = list.list();
				else
					return null;
			}
			Set<String> funs = new HashSet<String>(5);
			for (AppliedTo a : annots) {
				if ((role & a.role()) != 0)
					for (String f : a.functions())
						funs.add(f);
			}
			return funs.toArray(new String[funs.size()]);
		}
		/**
		 * Construct role mask of this rule using reflection. A rule assumes a role if its class overrides a method with a SignifiesRole annotation attached to it.
		 * So for example, if a class overrides validateArguments and validateArguments has the annotation @SignifiesRole(role=ARGUMENT_VALIDATOR), the mask will contain
		 * ARGUMENT_VALIDATOR.
		 * @return The role mask.
		 */
		public final int getRoleMask() {
			int result = 0;
			Outer: for (Method m : getClass().getDeclaredMethods()) {
				try {
					Method baseMethod = m;
					for (Class<?> cls = getClass(); cls != null; cls = cls.getSuperclass()) {
						if (baseMethod == null) {
							try {
								baseMethod = cls.getMethod(m.getName(), m.getParameterTypes());
							} catch (Exception e) {
								baseMethod = null;
							}
						}
						if (baseMethod != null) {
							SignifiesRole annot = baseMethod.getAnnotation(SignifiesRole.class);
							if (annot != null) {
								result |= annot.role();
								continue Outer;
							}
						}
						baseMethod = null;
					}
				} catch (Exception e) {
					continue;
				}
			}
			return result;
		}
	}
	
	/**
	 * Base class for special rules applied to functions.<br>
	 * This class allows
	 * <ul>
	 * <li>validating arguments in a special way</li>
	 * <li>modifying the return type of a function call</li>
	 * <li>making regions of AST expressions link-clickable</li>
	 * <li>Handling creation of new function objects</li>
	 * </ul>
	 * @author madeen
	 *
	 */
	public abstract class SpecialFuncRule extends SpecialRule {
		/**
		 * Validate arguments of a function call.
		 * @param callFunc The CallFunc
		 * @param arguments The arguments (also obtainable from callFunc)
		 * @param parser The parser serving as context
		 * @return Returns false if this rule doesn't handle validation of the passed function call. Default validation will be executed.
		 * @throws ParsingException 
		 */
		@SignifiesRole(role=ARGUMENT_VALIDATOR)
		public boolean validateArguments(CallFunc callFunc, ExprElm[] arguments, C4ScriptParser parser) throws ParsingException {
			return false;
		}
		/**
		 * Modify return type of function call.
		 * @param context context
		 * @param callFunc function call
		 * @return Modified type or null, in which case the default type will be returned.
		 */
		@SignifiesRole(role=RETURNTYPE_MODIFIER)
		public IType returnType(DeclarationObtainmentContext context, CallFunc callFunc) {
			return null;
		}
		/**
		 * Provide DeclarationRegion for an AST expression at a certain offset. Used for making stuff link-clickable.
		 * @param callFunc Function call the passed expression is a parameter of
		 * @param parser context
		 * @param index parameter index
		 * @param offsetInExpression Character offset in the parameter expression
		 * @param parmExpression Parameter expression that should be made link-clickable
		 * @return Return null if no declaration could be found that is referred to.
		 */
		@SignifiesRole(role=DECLARATION_LOCATOR)
		public DeclarationRegion locateDeclarationInParameter(CallFunc callFunc, C4ScriptParser parser, int index, int offsetInExpression, ExprElm parmExpression) {
			return null;
		}
		/**
		 * Assign default parameter types to a function.
		 * @param parser context
		 * @param function The function
		 * @return Returns false if this rule doesn't handle assigning default parameters of the passed function call. No types will be assigned and regular type inference takes place.
		 */
		@SignifiesRole(role=DEFAULT_PARMTYPE_ASSIGNER)
		public boolean assignDefaultParmTypes(C4ScriptParser parser, Function function) {
			return false;
		}
		/**
		 * Create new function with the given name. Called by the parser after it has parsed the function name in a function declaration.
		 * @param name The name of the function.
		 * @return Returns null if this rule didn't detect a special name, requiring a specialized function class. Parser will use default class (Function). 
		 */
		@SignifiesRole(role=DEFAULT_PARMTYPE_ASSIGNER)
		public Function newFunction(String name) {
			return null;
		}
		
		/**
		 * Called when a function is about to be parsed.
		 * @param function
		 */
		@SignifiesRole(role=FUNCTION_EVENT_LISTENER)
		public void functionAboutToBeParsed(Function function, C4ScriptParser context) {}
		
		@SignifiesRole(role=FUNCTION_PARM_PROPOSALS_CONTRIBUTOR)
		public void contributeAdditionalProposals(CallFunc callFunc, C4ScriptParser parser, int index, ExprElm parmExpression, C4ScriptCompletionProcessor processor, String prefix, int offset, List<ICompletionProposal> proposals) {}
	}
	
	private Map<String, SpecialFuncRule> argumentValidators = new HashMap<String, SpecialFuncRule>();
	private Map<String, SpecialFuncRule> returnTypeModifiers = new HashMap<String, SpecialFuncRule>();
	private Map<String, SpecialFuncRule> declarationLocators = new HashMap<String, SpecialFuncRule>();
	private Map<String, SpecialFuncRule> parmProposalContributors = new HashMap<String, SpecialFuncRule>();
	private List<SpecialFuncRule> defaultParmTypeAssigners = new LinkedList<SpecialFuncRule>();
	private List<SpecialFuncRule> functionEventListeners = new LinkedList<SpecialFuncRule>();
	
	public Iterable<SpecialFuncRule> defaultParmTypeAssignerRules() {
		return defaultParmTypeAssigners;
	}
	
	public Iterable<SpecialFuncRule> functionEventListeners() {
		return functionEventListeners;
	}
	
	private interface FunctionsGett0r {
		String[] functions(int role);
	}
	private void putFuncRule(SpecialFuncRule rule, FunctionsGett0r functionsGetter) {
		int mask = rule.getRoleMask();
		if ((mask & ARGUMENT_VALIDATOR) != 0) for (String func : functionsGetter.functions(ARGUMENT_VALIDATOR)) {
			argumentValidators.put(func, rule);
		}
		if ((mask & DECLARATION_LOCATOR) != 0) for (String func : functionsGetter.functions(DECLARATION_LOCATOR)) {
			declarationLocators.put(func, rule);
		}
		if ((mask & RETURNTYPE_MODIFIER) != 0) for (String func : functionsGetter.functions(RETURNTYPE_MODIFIER)) {
			returnTypeModifiers.put(func, rule);
		}
		if ((mask & DEFAULT_PARMTYPE_ASSIGNER) != 0) {
			defaultParmTypeAssigners.add(rule);
		}
		if ((mask & FUNCTION_EVENT_LISTENER) != 0) {
			functionEventListeners.add(rule);
		}
		if ((mask & FUNCTION_PARM_PROPOSALS_CONTRIBUTOR) != 0) for (String func : functionsGetter.functions(FUNCTION_PARM_PROPOSALS_CONTRIBUTOR)) {
			parmProposalContributors.put(func, rule);
		}
	}
	
	public void putFuncRule(SpecialFuncRule rule, final Field fieldReference) {
		putFuncRule(rule, new FunctionsGett0r() {
			@Override
			public String[] functions(int role) {
				return SpecialRule.getFunctionsAppliedTo(fieldReference, role);
			}
		});
	}
	
	public void putFuncRule(SpecialFuncRule rule, final String... functions) {
		putFuncRule(rule, new FunctionsGett0r() {
			@Override
			public String[] functions(int role) {
				return functions;
			}
		});
	}
	
	public SpecialFuncRule getFuncRuleFor(String function, int role) {
		if (role == ARGUMENT_VALIDATOR)
			return argumentValidators.get(function);
		else if (role == DECLARATION_LOCATOR)
			return declarationLocators.get(function);
		else if (role == RETURNTYPE_MODIFIER)
			return returnTypeModifiers.get(function);
		else if (role == FUNCTION_PARM_PROPOSALS_CONTRIBUTOR)
			return parmProposalContributors.get(function);
		else
			return null;
	}
	
	/**
	 * CreateObject and similar functions that will return an object of the specified type
	 */
	@AppliedTo(functions={"CreateObject", "CreateContents"})
	public final SpecialFuncRule objectCreationRule = new SpecialFuncRule() {
		@Override
		public IType returnType(DeclarationObtainmentContext context, CallFunc callFunc) {
			if (callFunc.getParams().length >= 1) {
				IType t = callFunc.getParams()[0].getType(context);
				if (t instanceof ConstrainedType) {
					ConstrainedType ct = (ConstrainedType) t;
					switch (ct.constraintKind()) {
					case Exact:
						return ct.getObjectType();
					case CallerType:
						return new ConstrainedObject(ct.constraintScript(), ConstraintKind.CallerType);
					case Includes:
						return new ConstrainedObject(ct.constraintScript(), ConstraintKind.Includes);
					}
				}
			}
			return null;
		}
	};
	
	/**
	 *  GetID() returns type of calling object
	 */
	@AppliedTo(functions={"GetID"})
	public final SpecialFuncRule getIDRule = new SpecialFuncRule() {
		@Override
		public IType returnType(DeclarationObtainmentContext context, CallFunc callFunc) {
			ScriptBase script = null;
			ConstraintKind constraintKind = null;
			IType t;
			if (callFunc.getParams().length > 0) {
				t = callFunc.getParams()[0].getType(context);
			} else if (callFunc.getPredecessorInSequence() != null) {
				t = callFunc.getPredecessorInSequence().getType(context);
			} else {
				constraintKind = ConstraintKind.CallerType;
				script = context.getContainer();
				t = null;
			}
			if (t instanceof ConstrainedObject) {
				ConstrainedObject cobj = (ConstrainedObject)t;
				constraintKind = cobj.constraintKind();
				script = cobj.constraintScript();
			}
			
			return script != null ? ConstrainedType.get(script, constraintKind) : PrimitiveType.ID;
		}
	};
	
	/**
	 *  It's a criteria search (FindObjects etc) so guess return type from arguments passed to the criteria search function
	 */
	@AppliedTo(functions={"FindObjects"})
	public final SpecialFuncRule criteriaSearchRule = new SpecialFuncRule() {
		@Override
		public IType returnType(DeclarationObtainmentContext context, CallFunc callFunc) {
			IType t = searchCriteriaAssumedResult(context, callFunc, true);
			if (t == null || t == PrimitiveType.UNKNOWN) {
				t = PrimitiveType.OBJECT;
			}
			if (t != null) {
				Function f = (Function) callFunc.getDeclaration();
				if (f != null && f.getReturnType() == PrimitiveType.ARRAY)
					return new ArrayType(t);
			}
			return t;
		}
		
		private IType searchCriteriaAssumedResult(DeclarationObtainmentContext context, CallFunc callFunc, boolean topLevel) {
			IType result = null;
			String declarationName = callFunc.getDeclarationName();
			// parameters to FindObjects itself are also &&-ed together
			if (topLevel || declarationName.equals("Find_And") || declarationName.equals("Find_Or")) {
				List<IType> types = new LinkedList<IType>();
				for (ExprElm parm : callFunc.getParams()) {
					if (parm instanceof CallFunc) {
						CallFunc call = (CallFunc)parm;
						IType t = searchCriteriaAssumedResult(context, call, false);
						if (t != null) for (IType ty : t) {
							types.add(ty);
						}
					}
				}
				result = TypeSet.create(types);
			}
			else if (declarationName.equals("Find_ID")) { //$NON-NLS-1$
				if (callFunc.getParams().length >= 1) {
					result = callFunc.getParams()[0].guessObjectType(context);
				}
			}
			else if (declarationName.equals("Find_Func") && callFunc.getParams().length >= 1) {
				Object ev = callFunc.getParams()[0].evaluateAtParseTime(context.getCurrentFunc());
				if (ev instanceof String) {
					List<IType> types = new LinkedList<IType>();
					for (ClonkIndex index : context.getContainer().getIndex().relevantIndexes()) {
						for (Function f : index.declarationsWithName((String)ev, Function.class)) {
							if (f.getScript() instanceof Definition) {
								types.add((Definition)f.getScript());
							}
							else for (Directive directive : f.getScript().directives()) {
								if (directive.getType() == DirectiveType.APPENDTO) {
									Definition def = f.getScript().getIndex().getDefinitionNearestTo(context.getContainer().getResource(), directive.contentAsID());
									if (def != null) {
										types.add(def);
									}
								}
							}
						}
					}
					return TypeSet.create(types);
				}
			}
			return result;
		};
	};
	
	/**
	 *  validate DoSomething() in Schedule("DoSomething()")
	 */
	@AppliedTo(functions={"Schedule"})
	public final SpecialFuncRule scheduleScriptValidationRule = new SpecialFuncRule() {
		@Override
		public boolean validateArguments(CallFunc callFunc, final ExprElm[] arguments, final C4ScriptParser parser) {
			if (arguments.length < 1)
				return false; // no script expression supplied
			IType objType = arguments.length >= 4 ? arguments[3].getType(parser) : parser.getContainerAsDefinition();
			ScriptBase script = objType != null ? TypeSet.objectIngredient(objType) : null;
			if (script == null)
				script = parser.getContainer(); // fallback
			Object scriptExpr = arguments[0].evaluateAtParseTime(script);
			if (scriptExpr instanceof String) {
				try {
					C4ScriptParser.parseStandaloneStatement((String)scriptExpr, parser.getCurrentFunc(), null, new IMarkerListener() {
						@Override
						public WhatToDo markerEncountered(C4ScriptParser nestedParser, ParserErrorCode code, int markerStart, int markerEnd, int flags, int severity, Object... args) {
							if (code == ParserErrorCode.NotFinished) {
								// ignore complaining about missing ';' - some genuine errors might slip through but who cares
								return WhatToDo.DropCharges;
							}
							try {
								// pass through to the 'real' script parser
								if (parser.errorEnabled(code)) {
									parser.markerWithCode(code, arguments[0].getExprStart()+1+markerStart, arguments[0].getExprStart()+1+markerEnd, flags, severity, args);
								}
							} catch (ParsingException e) {
								// shouldn't happen
								e.printStackTrace();
							}
							return WhatToDo.PassThrough;
						}
					});
				} catch (ParsingException e) {
					// that on slipped through - pretend nothing happened
				}
			}
			return false; // don't stop regular parameter validating
		};
		@Override
		public DeclarationRegion locateDeclarationInParameter(CallFunc callFunc, C4ScriptParser parser, int index, int offsetInExpression, ExprElm parmExpression) {
			if (index == 0 && parmExpression instanceof StringLiteral) {
				StringLiteral lit = (StringLiteral) parmExpression;
				ExpressionLocator locator = new ExpressionLocator(offsetInExpression-1); // make up for '"'
				try {
					C4ScriptParser.parseStandaloneStatement(lit.getLiteral(), parser.getCurrentFunc(), locator, null);
				} catch (ParsingException e) {}
				if (locator.getExprAtRegion() != null) {
					DeclarationRegion reg = locator.getExprAtRegion().declarationAt(offsetInExpression, parser);
					if (reg != null)
						return reg.addOffsetInplace(lit.getExprStart()+1);
				}
			}
			return null;
		};
	};
	
	@AppliedTo(functions={"ScheduleCall"})
	public final SpecialFuncRule scheduleCallLinkRule = new SpecialFuncRule() {
		@Override
		public DeclarationRegion locateDeclarationInParameter(CallFunc callFunc, C4ScriptParser parser, int index, int offsetInExpression, ExprElm parmExpression) {
			if (index == 1 && parmExpression instanceof StringLiteral) {
				StringLiteral lit = (StringLiteral) parmExpression;
				IType t = callFunc.getParams()[0].getType(parser);
				ScriptBase scriptToLookIn = t instanceof ScriptBase ? (ScriptBase)t : parser.getContainer();
				Function func = scriptToLookIn.findFunction(lit.getLiteral());
				if (func != null) {
					return new DeclarationRegion(func, new Region(lit.getExprStart()+1, lit.getLength()-2));
				}
			}
			return null;
		};
	};
	
	/**
	 * Validate parameters for Add/Append/Set Command differently according to the command parameter
	 */
	@AppliedTo(functions={"AddCommand", "AppendCommand", "SetCommand"})
	public final SpecialFuncRule addCommandValidationRule = new SpecialFuncRule() {
		@Override
		public boolean validateArguments(CallFunc callFunc, ExprElm[] arguments, C4ScriptParser parser) {
			Function f = callFunc.getDeclaration() instanceof Function ? (Function)callFunc.getDeclaration() : null;
			if (f != null && arguments.length >= 3) {
				// look if command is "Call"; if so treat parms 2, 3, 4 as any
				Object command = arguments[1].evaluateAtParseTime(parser.getCurrentFunc());
				if (command instanceof String && command.equals("Call")) { //$NON-NLS-1$
					int givenParam = 0;
					for (Variable parm : f.getParameters()) {
						if (givenParam >= arguments.length)
							break;
						ExprElm given = arguments[givenParam++];
						if (given == null)
							continue;
						IType parmType = givenParam >= 2 && givenParam <= 4 ? PrimitiveType.ANY : parm.getType();
						if (!given.validForType(parmType, parser))
							parser.warningWithCode(ParserErrorCode.IncompatibleTypes, given, parmType, given.getType(parser));
						else
							given.expectedToBeOfType(parmType, parser);
					}
					return true;
				}
			}
			return false;
		};
	};
	
	/**
	 * Add link to the function being indirectly called by GameCall
	 */
	@AppliedTo(functions={"GameCall"})
	public final SpecialFuncRule gameCallLinkRule = new SpecialFuncRule() {
		@Override
		public DeclarationRegion locateDeclarationInParameter(CallFunc callFunc, C4ScriptParser parser, int parameterIndex, int offsetInExpression, ExprElm parmExpression) {
			if (parameterIndex == 0 && parmExpression instanceof StringLiteral) {
				StringLiteral lit = (StringLiteral)parmExpression;
				ClonkIndex index = parser.getContainer().getIndex();
				Scenario scenario = ClonkIndex.pickNearest(index.getIndexedScenarios(), parser.getContainer().getResource());
				if (scenario != null) {
					Function scenFunc = scenario.findFunction(lit.stringValue());
					if (scenFunc != null)
						return new DeclarationRegion(scenFunc, lit.identifierRegion());
				}
			}
			return null;
		};
	};
	
	/**
	 * Add link to the function being indirectly called by Call
	 */
	@AppliedTo(functions={"Call"})
	public final SpecialFuncRule callLinkRule = new SpecialFuncRule() {
		@Override
		public DeclarationRegion locateDeclarationInParameter(CallFunc callFunc, C4ScriptParser parser, int index, int offsetInExpression, ExprElm parmExpression) {
			if (index == 0 && parmExpression instanceof StringLiteral) {
				StringLiteral lit = (StringLiteral)parmExpression;
				Function f = parser.getContainer().findFunction(lit.stringValue());
				if (f != null)
					return new DeclarationRegion(f, lit.identifierRegion());
			}
			return null;
		};
	};
	
	/**
	 * Add link to the function being indirectly called by Private/Public/public Call
	 */
	@AppliedTo(functions={"PrivateCall", "PublicCall", "PrivateCall"})
	public final SpecialFuncRule scopedCallLinkRule = new SpecialFuncRule() {
		@Override
		public DeclarationRegion locateDeclarationInParameter(CallFunc callFunc, C4ScriptParser parser, int index, int offsetInExpression, ExprElm parmExpression) {
			if (index == 1 && parmExpression instanceof StringLiteral) {
				StringLiteral lit = (StringLiteral)parmExpression;
				Definition typeToLookIn = callFunc.getParams()[0].guessObjectType(parser);
				if (typeToLookIn == null && callFunc.getPredecessorInSequence() != null)
					typeToLookIn = callFunc.getPredecessorInSequence().guessObjectType(parser);
				if (typeToLookIn == null)
					typeToLookIn = parser.getContainerAsDefinition();
				if (typeToLookIn != null) {
					Function f = typeToLookIn.findFunction(lit.stringValue());
					if (f != null)
						return new DeclarationRegion(f, lit.identifierRegion());
				}
			}
			return null;
		};
	};

	/**
	 * Link to local vars referenced by LocalN
	 */
	@AppliedTo(functions={"LocalN"})
	public final SpecialFuncRule localNLinkRule = new SpecialFuncRule() {
		@Override
		public DeclarationRegion locateDeclarationInParameter(CallFunc callFunc, C4ScriptParser parser, int index, int offsetInExpression, ExprElm parmExpression) {
			if (index == 0 && parmExpression instanceof StringLiteral) {
				StringLiteral lit = (StringLiteral)parmExpression;
				Definition typeToLookIn = callFunc.getParams().length > 1 ? callFunc.getParams()[1].guessObjectType(parser) : null;
				if (typeToLookIn == null && callFunc.getPredecessorInSequence() != null)
					typeToLookIn = callFunc.getPredecessorInSequence().guessObjectType(parser);
				if (typeToLookIn == null)
					typeToLookIn = parser.getContainerAsDefinition();
				if (typeToLookIn != null) {
					Variable var = typeToLookIn.findVariable(lit.stringValue());
					if (var != null)
						return new DeclarationRegion(var, lit.identifierRegion());
				}
			}
			return null;
		};
	};
	
	/**
	 * Special rule to change the return type of GetPlrKnowledge if certain parameters are passed to it.
	 */
	@AppliedTo(functions={"GetPlrKnowledge", "GetPlrMagic"})
	public final SpecialFuncRule getPlrKnowledgeRule = new SpecialFuncRule() {
		@Override
		public IType returnType(DeclarationObtainmentContext context, CallFunc callFunc) {
			if (callFunc.getParams().length >= 3) {
				return PrimitiveType.ID;
			} else {
				return PrimitiveType.INT;
			}
		};
	};
	
	/**
	 * Links to particle definitions from various particle functions.
	 */
	@AppliedTo(functions={"CreateParticle", "CastAParticles", "CastParticles", "CastBackParticles", "PushParticles"})
	public final SpecialFuncRule linkToParticles = new SpecialFuncRule() {
		@Override
		public DeclarationRegion locateDeclarationInParameter(CallFunc callFunc, C4ScriptParser parser, int index, int offsetInExpression, ExprElm parmExpression) {
			Object parmEv;
			if (index == 0 && (parmEv = parmExpression.evaluateAtParseTime(parser.getCurrentFunc())) instanceof String) {
				String particleName = (String)parmEv;
				ProjectIndex projIndex = (ProjectIndex)parser.getContainer().getIndex();
				ParticleUnit unit = projIndex.findPinnedStructure(ParticleUnit.class, particleName, parser.getContainer().getResource(), true, "Particle.txt");
				if (unit != null) {
					return new DeclarationRegion(unit, parmExpression);
				}
			}
			return null;
		};
	};
	
	/**
	 * Base class for SetAction link rules.
	 * @author madeen
	 *
	 */
	protected class SetActionLinkRule extends SpecialFuncRule {
		@Override
		public DeclarationRegion locateDeclarationInParameter(CallFunc callFunc, C4ScriptParser parser, int index, int offsetInExpression, ExprElm parmExpression) {
			return getActionLinkForDefinition(parser.getCurrentFunc(), parser.getContainerAsDefinition(), parmExpression);
		}
		protected DeclarationRegion getActionLinkForDefinition(Function currentFunction, Definition definition, ExprElm actionNameExpression) {
			Object parmEv;
			if (definition != null && (parmEv = actionNameExpression.evaluateAtParseTime(currentFunction)) instanceof String) {
				final String actionName = (String)parmEv;
				if (definition instanceof ProjectDefinition) {
					ProjectDefinition projDef = (ProjectDefinition)definition;
					IResource res = Utilities.findMemberCaseInsensitively(projDef.getObjectFolder(), "ActMap.txt");
					if (res instanceof IFile) {
						IniUnit unit;
						try {
							unit = (IniUnit) ActMapUnit.pinned(res, true, false);
						} catch (CoreException e) {
							e.printStackTrace();
							return null;
						}
						if (unit instanceof IniUnitWithNamedSections) {
							IniSection actionSection = unit.sectionMatching(((IniUnitWithNamedSections) unit).nameMatcherPredicate(actionName));
							if (actionSection != null) {
								return new DeclarationRegion(actionSection, actionNameExpression);
							}
						}
					}
				}
			}
			return null;
		}
	}
	
	@AppliedTo(functions={"SetAction"})
	public SpecialFuncRule setActionLinkRule = new SetActionLinkRule();
	
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
			ExprElm saved = parser.currentFunctionContext.expressionReportingErrors;
			try {
				if (parmIndex+1 >= arguments.length) {
					if (evTracer.tracedFile == null)
						return true;
					parser.currentFunctionContext.expressionReportingErrors = arguments[0];
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
					parser.currentFunctionContext.expressionReportingErrors = arguments[parmIndex+1];
					parser.errorWithCode(ParserErrorCode.IncompatibleFormatArgType, arguments[parmIndex+1],
						C4ScriptParser.NO_THROW, expectedType.typeName(false), arguments[parmIndex+1].getType(parser).typeName(false), evTracer.evaluation, evTracer.tracedFile.getProjectRelativePath().toOSString());
				}
			} finally {
				parser.currentFunctionContext.expressionReportingErrors = saved;
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

	/**
	 * Add rules declared as public instance variables to various internal lists so they will be recognized.
	 * Annotations are consulted to decide what lists the rules are added to.
	 */
	public void initialize() {
		for (Class<?> c = getClass(); c != null; c = c.getSuperclass()) {
			for (Field f : c.getDeclaredFields()) {
				Object obj;
				try {
					obj = f.get(this);
				} catch (Exception e) {
					continue;
				}
				if (obj instanceof SpecialFuncRule) {
					SpecialFuncRule funcRule = (SpecialFuncRule) obj;
					putFuncRule(funcRule, f);
				}
			}
		}
	}
	
	/**
	 * Parse an ID. Default implementation returns null. To be overridden for specific engines.
	 * @param scanner The scanner to parse the id from
	 * @return The parsed id or null if parsing failed
	 */
	public ID parseId(BufferedScanner scanner) {
		return null;
	}
	
}
