package net.arctics.clonk.parser.c4script;

import static net.arctics.clonk.util.Utilities.as;

import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.IIndexEntity;
import net.arctics.clonk.index.IReplacedWhenSaved;
import net.arctics.clonk.index.ISerializationResolvable;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.MetaDefinition;
import net.arctics.clonk.index.ProjectIndex;
import net.arctics.clonk.index.ProjectResource;
import net.arctics.clonk.index.Scenario;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.EntityRegion;
import net.arctics.clonk.parser.IASTPositionProvider;
import net.arctics.clonk.parser.ID;
import net.arctics.clonk.parser.IMarkerListener;
import net.arctics.clonk.parser.Markers;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.parser.c4script.Directive.DirectiveType;
import net.arctics.clonk.parser.c4script.ast.CallDeclaration;
import net.arctics.clonk.parser.c4script.ast.StringLiteral;
import net.arctics.clonk.parser.c4script.ast.TypeUnification;
import net.arctics.clonk.parser.c4script.ast.TypingJudgementMode;
import net.arctics.clonk.parser.inireader.CategoriesValue;
import net.arctics.clonk.parser.inireader.ComplexIniEntry;
import net.arctics.clonk.parser.inireader.IDArray;
import net.arctics.clonk.parser.inireader.IniData.IniConfiguration;
import net.arctics.clonk.parser.inireader.IniData.IniEntryDefinition;
import net.arctics.clonk.parser.inireader.IniData.IniSectionDefinition;
import net.arctics.clonk.parser.inireader.IniEntry;
import net.arctics.clonk.parser.inireader.IniSection;
import net.arctics.clonk.parser.inireader.IniUnit;
import net.arctics.clonk.parser.inireader.IniUnitWithNamedSections;
import net.arctics.clonk.parser.inireader.ParticleUnit;
import net.arctics.clonk.parser.inireader.ScenarioUnit;
import net.arctics.clonk.parser.inireader.SignedInteger;
import net.arctics.clonk.resource.c4group.C4Group.GroupType;
import net.arctics.clonk.ui.editors.c4script.C4ScriptCompletionProcessor;
import net.arctics.clonk.ui.editors.c4script.ExpressionLocator;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.IPredicate;
import net.arctics.clonk.util.Sink;
import net.arctics.clonk.util.StringUtil;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

/**
 * This class contains some special rules that are applied to certain objects during parsing
 * (like CallFuncians and other species dwelling in ASTs)
 * @author madeen
 *
 */
public abstract class SpecialEngineRules {

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
		 * Only applied to the specified functions in a matching role processor.
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
	 */
	public static abstract class SpecialRule implements IReplacedWhenSaved {

		public static class Ticket implements ISerializationResolvable, Serializable {
			private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
			private final String name;
			public Ticket(SpecialRule obj) {
				this.name = obj.name;
			}
			@Override
			public Object resolve(Index index) {
				return index.engine().specialRules().rule(name);
			}
		}

		@Override
		public Object saveReplacement() { return new Ticket(this); }

		public String name;

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
			for (AppliedTo a : annots)
				if ((role & a.role()) != 0)
					for (String f : a.functions())
						funs.add(f);
			return funs.toArray(new String[funs.size()]);
		}
		/**
		 * Construct role mask of this rule using reflection. A rule assumes a role if its class overrides a method with a SignifiesRole annotation attached to it.
		 * So for example, if a class overrides validateArguments and validateArguments has the annotation @SignifiesRole(role=ARGUMENT_VALIDATOR), the mask will contain
		 * ARGUMENT_VALIDATOR.
		 * @return The role mask.
		 */
		public final int computeRoleMask() {
			int result = 0;
			for (Method m : getClass().getMethods())
				try {
					Class<?> cls = m.getDeclaringClass().getSuperclass();
					while (cls != null) {
						Method baseMethod = cls.getMethod(m.getName(), m.getParameterTypes());
						if (baseMethod == null)
							break;
						SignifiesRole annot = baseMethod.getAnnotation(SignifiesRole.class);
						if (annot != null) {
							result |= annot.role();
							break;
						}
						cls = baseMethod.getDeclaringClass().getSuperclass();
					}
				} catch (Exception e) {
					continue;
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
	public static abstract class SpecialFuncRule extends SpecialRule {

		/**
		 * Validate arguments of a function call.
		 * @param callFunc The CallFunc
		 * @param arguments The arguments (also obtainable from callFunc)
		 * @param processor The processor serving as processor
		 * @return Returns false if this rule doesn't handle validation of the passed function call. Default validation will be executed.
		 * @throws ParsingException
		 */
		@SignifiesRole(role=ARGUMENT_VALIDATOR)
		public boolean validateArguments(CallDeclaration callFunc, ASTNode[] arguments, ProblemReportingContext processor) throws ParsingException {
			return false;
		}
		/**
		 * Modify return type of function call.
		 * @param processor processor
		 * @param callFunc function call
		 * @return Modified type or null, in which case the default type will be returned.
		 */
		@SignifiesRole(role=RETURNTYPE_MODIFIER)
		public IType returnType(ProblemReportingContext processor, CallDeclaration callFunc) {
			return null;
		}
		/**
		 * Provide an {@link EntityRegion} for an AST expression at a certain offset. Used for making stuff link-clickable.
		 * Actually only called for {@link StringLiteral}s since other kinds of expressions don't lend themselves to refer to
		 * something only in special contexts.
		 * @param callFunc Function call the passed expression is a parameter of
		 * @param processor processor
		 * @param index parameter index
		 * @param offsetInExpression Character offset in the parameter expression
		 * @param parmExpression Parameter expression that should be made link-clickable
		 * @return Return null if no declaration could be found that is referred to.
		 */
		@SignifiesRole(role=DECLARATION_LOCATOR)
		public EntityRegion locateEntityInParameter(CallDeclaration callFunc, ProblemReportingContext processor, int index, int offsetInExpression, ASTNode parmExpression) {
			return null;
		}
		/**
		 * Assign default parameter types to a function.
		 * @param processor processor
		 * @param function The function
		 * @return Returns false if this rule doesn't handle assigning default parameters of the passed function call. No types will be assigned and regular type inference takes place.
		 */
		@SignifiesRole(role=DEFAULT_PARMTYPE_ASSIGNER)
		public boolean assignDefaultParmTypes(ProblemReportingContext processor, Function function) {
			return false;
		}
		/**
		 * Create new function with the given name. Called by the processor after it has parsed the function name in a function declaration.
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
		public void functionAboutToBeParsed(Function function, C4ScriptParser processor) {}

		@SignifiesRole(role=FUNCTION_PARM_PROPOSALS_CONTRIBUTOR)
		public void contributeAdditionalProposals(
			CallDeclaration callFunc, ProblemReportingContext processor, int index, ASTNode parmExpression,
			C4ScriptCompletionProcessor completions, String prefix,
			int offset, List<ICompletionProposal> proposals) {}
	}

	private final Map<String, SpecialRule> allRules = new HashMap<String, SpecialEngineRules.SpecialRule>();
	private final Map<String, SpecialFuncRule> argumentValidators = new HashMap<String, SpecialFuncRule>();
	private final Map<String, SpecialFuncRule> returnTypeModifiers = new HashMap<String, SpecialFuncRule>();
	private final Map<String, SpecialFuncRule> declarationLocators = new HashMap<String, SpecialFuncRule>();
	private final Map<String, SpecialFuncRule> parmProposalContributors = new HashMap<String, SpecialFuncRule>();
	private final List<SpecialFuncRule> defaultParmTypeAssigners = new LinkedList<SpecialFuncRule>();
	private final List<SpecialFuncRule> functionEventListeners = new LinkedList<SpecialFuncRule>();

	public Iterable<SpecialFuncRule> defaultParmTypeAssignerRules() {
		return defaultParmTypeAssigners;
	}

	public Iterable<SpecialFuncRule> functionEventListeners() {
		return functionEventListeners;
	}

	private interface FunctionsByRole {
		String[] functions(int role);
	}
	private void putFuncRule(SpecialFuncRule rule, FunctionsByRole functionsGetter) {
		int mask = rule.computeRoleMask();
		if ((mask & ARGUMENT_VALIDATOR) != 0) for (String func : functionsGetter.functions(ARGUMENT_VALIDATOR))
			argumentValidators.put(func, rule);
		if ((mask & DECLARATION_LOCATOR) != 0) for (String func : functionsGetter.functions(DECLARATION_LOCATOR))
			declarationLocators.put(func, rule);
		if ((mask & RETURNTYPE_MODIFIER) != 0) for (String func : functionsGetter.functions(RETURNTYPE_MODIFIER))
			returnTypeModifiers.put(func, rule);
		if ((mask & DEFAULT_PARMTYPE_ASSIGNER) != 0)
			defaultParmTypeAssigners.add(rule);
		if ((mask & FUNCTION_EVENT_LISTENER) != 0)
			functionEventListeners.add(rule);
		if ((mask & FUNCTION_PARM_PROPOSALS_CONTRIBUTOR) != 0) for (String func : functionsGetter.functions(FUNCTION_PARM_PROPOSALS_CONTRIBUTOR))
			parmProposalContributors.put(func, rule);
	}

	public SpecialRule rule(String name) {
		return allRules.get(name);
	}

	public void putFuncRule(SpecialFuncRule rule, final Field fieldReference) {
		putRule(rule, fieldReference);
		putFuncRule(rule, new FunctionsByRole() {
			@Override
			public String[] functions(int role) {
				return SpecialRule.getFunctionsAppliedTo(fieldReference, role);
			}
		});
	}

	private void putRule(SpecialFuncRule rule, final Field field) {
		rule.name = field.getName();
		allRules.put(field.getName(), rule);
	}

	private Field fieldWithValue(Object value) {
		for (Class<?> c = getClass(); c != null; c = c.getSuperclass())
			for (Field f : c.getDeclaredFields()) {
				Object obj;
				try {
					obj = f.get(this);
				} catch (Exception e) {
					continue;
				}
				if (obj == value)
					return f;
			}
		return null;
	}

	public void putFuncRule(SpecialFuncRule rule, final String... functions) {
		Field field  = fieldWithValue(rule);
		if (field != null)
			putRule(rule, field);
		putFuncRule(rule, new FunctionsByRole() {
			@Override
			public String[] functions(int role) {
				return functions;
			}
		});
	}

	public SpecialFuncRule funcRuleFor(String function, int role) {
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
	@AppliedTo(functions={"CreateObject", "CreateContents", "CreateConstruction"})
	public final SpecialFuncRule objectCreationRule = new SpecialFuncRule() {
		@Override
		public IType returnType(ProblemReportingContext processor, CallDeclaration callFunc) {
			if (callFunc.params().length >= 1) {
				IType t = processor.typeOf(callFunc.params()[0]);
				IType r = PrimitiveType.OBJECT;
				for (IType ty : t)
					if (ty instanceof MetaDefinition)
						r = TypeUnification.unify(r, ((MetaDefinition)ty).definition());
				return r;
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
		public IType returnType(ProblemReportingContext processor, CallDeclaration callFunc) {
			IType t;
			if (callFunc.params().length > 0)
				t = processor.typeOf(callFunc.params()[0]);
			else if (callFunc.predecessorInSequence() != null)
				t = processor.typeOf(callFunc.predecessorInSequence());
			else
				t = processor.script();
			if (t instanceof Definition)
				return ((Definition)t).metaDefinition();
			else
				return PrimitiveType.ID;
		}
	};

	/**
	 *  It's a criteria search (FindObjects etc) so guess return type from arguments passed to the criteria search function
	 */
	@AppliedTo(functions={"FindObjects"})
	public final SpecialFuncRule criteriaSearchRule = new SearchCriteriaRuleBase() {
		@Override
		public IType returnType(ProblemReportingContext processor, CallDeclaration callFunc) {
			IType t = searchCriteriaAssumedResult(processor, callFunc, true);
			if (t == null || t == PrimitiveType.UNKNOWN)
				t = PrimitiveType.OBJECT;
			if (t != null) {
				Function f = (Function) callFunc.declaration();
				if (f != null && f.returnType() == PrimitiveType.ARRAY)
					return new ArrayType(t, (IType[])null);
			}
			return t;
		}
		private IType searchCriteriaAssumedResult(ProblemReportingContext processor, CallDeclaration node, boolean topLevel) {
			IType result = null;
			String declarationName = node.declarationName();
			// parameters to FindObjects itself are also &&-ed together
			if (topLevel || declarationName.equals("Find_And") || declarationName.equals("Find_Or")) {
				List<IType> types = new LinkedList<IType>();
				for (ASTNode parm : node.params())
					if (parm instanceof CallDeclaration) {
						CallDeclaration call = (CallDeclaration)parm;
						IType t = searchCriteriaAssumedResult(processor, call, false);
						if (t != null) for (IType ty : t)
							types.add(ty);
					}
				result = TypeUnification.unify(types);
			}
			else if (declarationName.equals("Find_ID")) { //$NON-NLS-1$
				if (node.params().length >= 1)
					result = processor.typeOf(node.params()[0], Definition.class);
			}
			else if (declarationName.equals("Find_Func") && node.params().length >= 1) {
				Object ev = node.params()[0].evaluateAtParseTime(node.parentOfType(Function.class));
				if (ev instanceof String) {
					List<Declaration> functions = functionsNamed(processor, (String)ev);
					List<IType> types = new ArrayList<IType>(functions.size());
					for (Declaration f : functions)
						if (f.script() instanceof Definition)
							types.add(f.script());
						else for (Directive directive : f.script().directives())
							if (directive.type() == DirectiveType.APPENDTO) {
								Definition def = f.script().index().definitionNearestTo(processor.script().resource(), directive.contentAsID());
								if (def != null)
									types.add(def);
							}
					for (Index index : processor.script().index().relevantIndexes())
						for (Function f : index.declarationsWithName((String)ev, Function.class))
							if (f.script() instanceof Definition)
								types.add(f.script());
							else for (Directive directive : f.script().directives())
								if (directive.type() == DirectiveType.APPENDTO) {
									Definition def = f.script().index().definitionNearestTo(processor.script().resource(), directive.contentAsID());
									if (def != null)
										types.add(def);
								}
					IType ty = TypeUnification.unify(types);
					return ty;
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
		public boolean validateArguments(CallDeclaration node, final ASTNode[] arguments, final ProblemReportingContext processor) {
			if (arguments.length < 1)
				return false; // no script expression supplied
			IType objType = arguments.length >= 4 ? processor.typeOf(arguments[3]) : processor.definition();
			Script script = objType != null ? TypeUtil.definition(objType) : null;
			if (script == null)
				script = processor.script(); // fallback
			Object scriptExpr = arguments[0].evaluateAtParseTime(script);
			if (scriptExpr instanceof String)
				try {
					ScriptsHelper.parseStandaloneNode((String)scriptExpr, node.parentOfType(Function.class), null, new IMarkerListener() {
						@Override
						public Decision markerEncountered(
							Markers markers, IASTPositionProvider positionProvider,
							ParserErrorCode code, ASTNode node,
							int markerStart, int markerEnd, int flags, int severity, Object... args
						) {
							switch (code) {
							// ignore complaining about missing ';' - some genuine errors might slip through but who cares
							case NotFinished:
							// also ignore undeclared identifier error - local variables and whatnot, checking for all of this is also overkill
							case UndeclaredIdentifier:
								return Decision.DropCharges;
							default:
								if (markers.errorEnabled(code))
									try {
										markers.marker(positionProvider, code, node, arguments[0].start()+1+markerStart, arguments[0].start()+1+markerEnd, flags, severity, args);
									} catch (ParsingException e1) {}
								return Decision.PassThrough;
							}
						}
					}, processor.script().engine(), null);
				} catch (ParsingException e) {
					// that on slipped through - pretend nothing happened
				}
			return false; // don't stop regular parameter validating
		};
		@Override
		public EntityRegion locateEntityInParameter(CallDeclaration node, ProblemReportingContext processor, int index, int offsetInExpression, ASTNode parmExpression) {
			if (index == 0 && parmExpression instanceof StringLiteral) {
				StringLiteral lit = (StringLiteral) parmExpression;
				ExpressionLocator locator = new ExpressionLocator(offsetInExpression-1); // make up for '"'
				try {
					ScriptsHelper.parseStandaloneNode(lit.literal(), node.parentOfType(Function.class), locator, null, processor.script().engine(), null);
				} catch (ParsingException e) {}
				if (locator.expressionAtRegion() != null) {
					EntityRegion reg = locator.expressionAtRegion().entityAt(offsetInExpression, processor);
					if (reg != null)
						return reg.incrementRegionBy(lit.start()+1);
				}
			}
			return null;
		};
	};

	@AppliedTo(functions={"ScheduleCall"})
	public final SpecialFuncRule scheduleCallLinkRule = new SpecialFuncRule() {
		@Override
		public EntityRegion locateEntityInParameter(CallDeclaration callFunc, ProblemReportingContext processor, int index, int offsetInExpression, ASTNode parmExpression) {
			if (index == 1 && parmExpression instanceof StringLiteral) {
				StringLiteral lit = (StringLiteral) parmExpression;
				IType t = processor.typeOf(callFunc.params()[0]);
				Script scriptToLookIn = t instanceof Script ? (Script)t : processor.script();
				Function func = scriptToLookIn.findFunction(lit.literal());
				if (func != null)
					return new EntityRegion(func, new Region(lit.start()+1, lit.getLength()-2));
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
		public boolean validateArguments(CallDeclaration node, ASTNode[] arguments, ProblemReportingContext processor) {
			Function f = node.declaration() instanceof Function ? (Function)node.declaration() : null;
			if (f != null && arguments.length >= 3) {
				// look if command is "Call"; if so treat parms 2, 3, 4 as any
				Object command = arguments[1].evaluateAtParseTime(node.parentOfType(Function.class));
				if (command instanceof String && command.equals("Call")) { //$NON-NLS-1$
					int givenParam = 0;
					for (Variable parm : f.parameters()) {
						if (givenParam >= arguments.length)
							break;
						ASTNode given = arguments[givenParam++];
						if (given == null)
							continue;
						IType parmType = givenParam >= 2 && givenParam <= 4 ? PrimitiveType.ANY : parm.type();
						if (!processor.validForType(given, parmType))
							processor.incompatibleTypes(node, given, parmType, processor.typeOf(given));
						else
							processor.typingJudgement(given, parmType, TypingJudgementMode.Unify);
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
		public EntityRegion locateEntityInParameter(CallDeclaration callFunc, ProblemReportingContext processor, int parameterIndex, int offsetInExpression, ASTNode parmExpression) {
			if (parameterIndex == 0 && parmExpression instanceof StringLiteral) {
				final StringLiteral lit = (StringLiteral)parmExpression;
				Index index = processor.script().index();
				Scenario scenario = Scenario.nearestScenario(processor.script().resource().getParent());
				if (scenario != null) {
					Function scenFunc = scenario.findFunction(lit.stringValue());
					if (scenFunc != null)
						return new EntityRegion(scenFunc, lit.identifierRegion());
				} else {
					final List<Declaration> decs = new LinkedList<Declaration>();
					index.forAllRelevantIndexes(new Sink<Index>() {
						@Override
						public void receivedObject(Index index) {
							for (Scenario s : index.indexedScenarios()) {
								Function f = s.findLocalFunction(lit.literal(), true);
								if (f != null)
									decs.add(f);
							}
						};
					});
					if (decs.size() > 0)
						return new EntityRegion(new HashSet<IIndexEntity>(decs), lit.identifierRegion());
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
		public EntityRegion locateEntityInParameter(CallDeclaration callFunc, ProblemReportingContext processor, int index, int offsetInExpression, ASTNode parmExpression) {
			if (index == 0 && parmExpression instanceof StringLiteral) {
				StringLiteral lit = (StringLiteral)parmExpression;
				Function f = processor.script().findFunction(lit.stringValue());
				if (f != null)
					return new EntityRegion(f, lit.identifierRegion());
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
		public EntityRegion locateEntityInParameter(CallDeclaration callFunc, ProblemReportingContext processor, int index, int offsetInExpression, ASTNode parmExpression) {
			if (index == 1 && parmExpression instanceof StringLiteral) {
				StringLiteral lit = (StringLiteral)parmExpression;
				Definition def = processor.typeOf(callFunc.params()[0], Definition.class);
				if (def == null && callFunc.predecessorInSequence() != null)
					def = processor.typeOf(callFunc.predecessorInSequence(), Definition.class);
				if (def == null)
					def = processor.definition();
				if (def != null) {
					Function f = def.findFunction(lit.stringValue());
					if (f != null)
						return new EntityRegion(f, lit.identifierRegion());
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
		public EntityRegion locateEntityInParameter(CallDeclaration callFunc, ProblemReportingContext processor, int index, int offsetInExpression, ASTNode parmExpression) {
			if (index == 0 && parmExpression instanceof StringLiteral) {
				StringLiteral lit = (StringLiteral)parmExpression;
				Definition def = callFunc.params().length > 1 ? processor.typeOf(callFunc.params()[1], Definition.class) : null;
				if (def == null && callFunc.predecessorInSequence() != null)
					def = processor.typeOf(callFunc.predecessorInSequence(), Definition.class);
				if (def == null)
					def = processor.definition();
				if (def != null) {
					Variable var = def.findVariable(lit.stringValue());
					if (var != null)
						return new EntityRegion(var, lit.identifierRegion());
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
		public IType returnType(ProblemReportingContext processor, CallDeclaration callFunc) {
			if (callFunc.params().length >= 3)
				return PrimitiveType.ID;
			else
				return PrimitiveType.INT;
		};
	};

	public abstract class LocateResourceByNameRule extends SpecialFuncRule {
		public abstract Set<IIndexEntity> locateEntitiesByName(CallDeclaration node, String name, ProjectIndex pi, ProblemReportingContext processor);
		@Override
		public EntityRegion locateEntityInParameter(CallDeclaration node, ProblemReportingContext processor, int index, int offsetInExpression, ASTNode parmExpression) {
			Object parmEv;
			if (index == 0 && (parmEv = parmExpression.evaluateAtParseTime(node.parentOfType(Function.class))) instanceof String) {
				String resourceName = (String)parmEv;
				ProjectIndex pi = (ProjectIndex)processor.script().index();
				Set<IIndexEntity> e = locateEntitiesByName(node, resourceName, pi, processor);
				if (e != null)
					return new EntityRegion(e, parmExpression);
			}
			return null;
		}
	}

	/**
	 * Links to particle definitions from various particle functions.
	 */
	@AppliedTo(functions={"CreateParticle", "CastAParticles", "CastParticles", "CastBackParticles", "PushParticles"})
	public final SpecialFuncRule linkToParticles = new LocateResourceByNameRule() {
		@Override
		public Set<IIndexEntity> locateEntitiesByName(CallDeclaration callFunc, String name, ProjectIndex pi, ProblemReportingContext processor) {
			return ArrayUtil.set((IIndexEntity)pi.findPinnedStructure(ParticleUnit.class, name, processor.script().resource(), true, "Particle.txt"));
		}
	};

	@AppliedTo(functions={"Format"})
	public final SpecialFuncRule linkFormat = new LocateResourceByNameRule() {
		@Override
		public Set<IIndexEntity> locateEntitiesByName(CallDeclaration callFunc, String name, ProjectIndex pi, ProblemReportingContext processor) {
			if (callFunc.parent() instanceof CallDeclaration && ((CallDeclaration)callFunc.parent()).indexOfParm(callFunc) == 0) {
				SpecialFuncRule rule = ((CallDeclaration)callFunc.parent()).specialRuleFromContext(processor, DECLARATION_LOCATOR);
				if (rule instanceof LocateResourceByNameRule)
					return ((LocateResourceByNameRule)rule).locateEntitiesByName(callFunc, name, pi, processor);
			}
			return null;
		}
	};

	/**
	 * Rule to link to sound files.
	 */
	@AppliedTo(functions={"Sound"})
	public final SpecialFuncRule linkToSound = new LocateResourceByNameRule() {
		protected void collectSoundResourcesInFolder(Set<IIndexEntity> set, Matcher nameMatcher, Engine engine, IContainer container, ProjectIndex pi) {
			try {
				for (IResource r : container.members())
					if (nameMatcher.reset(r.getName()).matches())
						set.add(new ProjectResource(pi, r));
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		@Override
		public Set<IIndexEntity> locateEntitiesByName(CallDeclaration callFunc, String name, ProjectIndex pi, ProblemReportingContext processor) {
			Engine engine = processor.script().engine();
			name = name.replace(".", "\\\\.").replaceAll("[\\*\\?]", ".*?").replace("%d", "[0-9]*");
			HashSet<IIndexEntity> results = new HashSet<IIndexEntity>();
			boolean extensionWildcardNeeded = true;
			for (String e : engine.settings().supportedSoundFileExtensions())
				if (name.endsWith("\\\\."+e)) {
					extensionWildcardNeeded = false;
					break;
				}
			if (extensionWildcardNeeded)
				name += "\\." + StringUtil.writeBlock(null, "(", ")", "|", engine.settings().supportedSoundFileExtensions());
			Matcher nameMatcher = Pattern.compile(name).matcher("");
			String soundGroupName = "Sound."+engine.settings().groupTypeToFileExtensionMapping().get(GroupType.ResourceGroup);
			IResource r = processor.script().resource();
			for (
				IContainer c = r instanceof IContainer ? (IContainer)r : r != null ? r.getParent() : null, d = null;
				c != null;
				c = c.getParent(), d = c != null ? as(c.findMember(soundGroupName), IContainer.class) : null
			) {
				collectSoundResourcesInFolder(results, nameMatcher, engine, c, pi);
				if (d != null)
					collectSoundResourcesInFolder(results, nameMatcher, engine, d, pi);
			}
			return results;
		}
	};

	/**
	 * Base class for SetAction link rules.
	 * @author madeen
	 *
	 */
	protected class SetActionLinkRule extends SpecialFuncRule {
		@Override
		public EntityRegion locateEntityInParameter(CallDeclaration node, ProblemReportingContext processor, int index, int offsetInExpression, ASTNode parmExpression) {
			return actionLinkForDefinition(node.parentOfType(Function.class), processor.definition(), parmExpression);
		}
		protected EntityRegion actionLinkForDefinition(Function currentFunction, Definition definition, ASTNode actionNameExpression) {
			Object parmEv;
			if (definition != null && (parmEv = actionNameExpression.evaluateAtParseTime(currentFunction)) instanceof String) {
				final String actionName = (String)parmEv;
				if (definition instanceof Definition) {
					Definition projDef = definition;
					IResource res = Utilities.findMemberCaseInsensitively(projDef.definitionFolder(), "ActMap.txt");
					if (res instanceof IFile) {
						IniUnit unit;
						unit = (IniUnit) Structure.pinned(res, true, false);
						if (unit instanceof IniUnitWithNamedSections) {
							IniSection actionSection = unit.sectionMatching(((IniUnitWithNamedSections) unit).nameMatcherPredicate(actionName));
							if (actionSection != null)
								return new EntityRegion(actionSection, actionNameExpression);
						}
					}
				}
			}
			return null;
		}
	}

	@AppliedTo(functions={"SetAction"})
	public SpecialFuncRule setActionLinkRule = new SetActionLinkRule();

	private abstract class SearchCriteriaRuleBase extends SpecialFuncRule {
		protected List<Declaration> functionsNamed(ProblemReportingContext processor, final String name) {
			final List<Declaration> matchingDecs = new LinkedList<Declaration>();
			final Sink<Script> scriptSink = new Sink<Script>() {
				@Override
				public void receivedObject(Script item) {
					if (item.dictionary() != null && item.dictionary().contains(name)) {
						item.requireLoaded();
						Function f = item.findFunction(name);
						if (f != null)
							matchingDecs.add(f);
					}
				}
			};
			processor.script().index().forAllRelevantIndexes(new Sink<Index>() {
				@Override
				public void receivedObject(Index index) {
					index.allScripts(scriptSink);
				}
			});
			return matchingDecs;
		}
	}

	/**
	 * Rule to make function names inside Find_Func link-clickable (will open DeclarationChooser).
	 */
	@AppliedTo(functions={"Find_Func"})
	public final SpecialFuncRule findFuncRule = new SearchCriteriaRuleBase() {
		@Override
		public EntityRegion locateEntityInParameter(CallDeclaration callFunc, ProblemReportingContext processor,
			int index, int offsetInExpression, ASTNode parmExpression) {
			if (parmExpression instanceof StringLiteral) {
				final StringLiteral lit = (StringLiteral)parmExpression;
				final List<Declaration> matchingDecs = functionsNamed(processor, lit.stringValue());
				if (matchingDecs.size() > 0)
					return new EntityRegion(new HashSet<IIndexEntity>(matchingDecs), lit.identifierRegion());
			}
			return null;
		};
	};

	/**
	 * Modifies the return type of CreateArray to be an ArrayType
	 */
	@AppliedTo(functions={"CreateArray"})
	public final SpecialFuncRule createArrayTypingRule = new SpecialFuncRule() {
		@Override
		public IType returnType(ProblemReportingContext processor, CallDeclaration callFunc) {
			int arrayLength = ArrayType.NO_PRESUMED_LENGTH;
			if (callFunc.params().length >= 1) {
				Object ev = callFunc.params()[0].evaluateAtParseTime(processor);
				if (ev instanceof Number)
					arrayLength = ((Number) ev).intValue();
			}
			return new ArrayType(PrimitiveType.ANY, arrayLength);
		};
	};

	@AppliedTo(functions={"GetScenarioVal"})
	public final SpecialFuncRule scenarioValResultRule = new SpecialFuncRule() {
		@Override
		public IType returnType(ProblemReportingContext processor, CallDeclaration callFunc) {
			if (callFunc.params().length > 0 && callFunc.params()[0] instanceof StringLiteral) {
				String entryName = ((StringLiteral)callFunc.params()[0]).stringValue();
				String sectionName =
					callFunc.params().length > 1 &&
					callFunc.params()[1] instanceof StringLiteral ?
						((StringLiteral)callFunc.params()[1]).stringValue() : null;
				IniConfiguration conf = processor.script().engine().iniConfigurations().configurationFor("Scenario.txt");
				IniEntryDefinition entry = null;
				for (IniSectionDefinition def : conf.sections().values())
					if (sectionName != null && sectionName.equals(def.sectionName())) {
						IniEntryDefinition trueEntry = as(def.entries().get(entryName), IniEntryDefinition.class);
						if (trueEntry != null) {
							entry = trueEntry;
							break;
						}
					}
					else {
						IniEntryDefinition ersatz = as(def.entries().get(entryName), IniEntryDefinition.class);
						if (ersatz != null)
							entry = ersatz;
					}
				if (entry != null)
					return typeFromEntryClass(entry.entryClass());
			}
			return PrimitiveType.ANY;
		}
		private IType typeFromEntryClass(Class<?> cls) {
			if (SignedInteger.class.isAssignableFrom(cls))
				return PrimitiveType.INT;
			if (IDArray.class == cls)
				return PrimitiveType.ID;
			return PrimitiveType.ANY;
		}
	};

	/**
	 * Add rules declared as public instance variables to various internal lists so they will be recognized.
	 * Annotations are consulted to decide what lists the rules are added to.
	 */
	public void initialize() {
		for (Class<?> c = getClass(); c != null; c = c.getSuperclass())
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

	/**
	 * Parse an ID. Default implementation returns null. To be overridden for specific engines.
	 * @param scanner The scanner to parse the id from
	 * @return The parsed id or null if parsing failed
	 */
	public ID parseId(BufferedScanner scanner) {
		return null;
	}

	public enum ScenarioConfigurationProcessing {
		Load,
		Save
	}

	public void processScenarioConfiguration(ScenarioUnit unit, ScenarioConfigurationProcessing processing) {
		// do nothing
	}

	public IPredicate<Definition> configurationEntryDefinitionFilter(final IniEntry entry) {
		if (entry instanceof ComplexIniEntry && ((ComplexIniEntry)entry).definition() != null && ((ComplexIniEntry)entry).definition().categoryFilter() != null)
			return new IPredicate<Definition>() {
				final String filter = ((ComplexIniEntry)entry).definition().categoryFilter();
				@Override
				public boolean test(Definition item) {
					CategoriesValue category = item.category();
					return category != null && category.constants() != null && category.constants().contains(filter);
				}
			};
		else if (entry.key().equals("Animal"))
			return new IPredicate<Definition>() {
				@Override
				public boolean test(Definition item) {
					return item.findFunction("IsAnimal") != null;
				}
			};
		else if (entry.key().equals("Crew"))
			return new IPredicate<Definition>() {
				@Override
				public boolean test(Definition item) {
					for (Directive d : item.directives())
						if (d.type() == DirectiveType.APPENDTO)
							return false; // ignore definitions that append
					return item.findFunction("IsClonk") != null;
				}
			};
		else
			return new IPredicate<Definition>() {
				@Override
				public boolean test(Definition item) {
					return true;
				}
			};
	}

	public void refreshIndex(Index index) {}

}
