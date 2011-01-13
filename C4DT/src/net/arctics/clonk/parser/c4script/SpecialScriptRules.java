package net.arctics.clonk.parser.c4script;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.Region;

import net.arctics.clonk.index.C4Object;
import net.arctics.clonk.index.C4Scenario;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.parser.DeclarationRegion;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser.IMarkerListener;
import net.arctics.clonk.parser.c4script.IHasConstraint.ConstraintKind;
import net.arctics.clonk.parser.c4script.ast.CallFunc;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.SimpleStatement;
import net.arctics.clonk.parser.c4script.ast.StringLiteral;
import net.arctics.clonk.ui.editors.c4script.ExpressionLocator;

/**
 * This class contains some special rules that are registered with interested parties
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
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	private @interface SignifiesRole {
		public int role();
	}
	
	public abstract class SpecialRule {
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
	
	public abstract class SpecialFuncRule extends SpecialRule {
		@SignifiesRole(role=ARGUMENT_VALIDATOR)
		public boolean validateArguments(CallFunc callFunc, ExprElm[] arguments, C4ScriptParser parser) {
			return false;
		}
		@SignifiesRole(role=RETURNTYPE_MODIFIER)
		public IType returnType(C4ScriptParser parser, CallFunc callFunc) {
			return null;
		}
		@SignifiesRole(role=DECLARATION_LOCATOR)
		public DeclarationRegion locateDeclarationInParameter(CallFunc callFunc, C4ScriptParser parser, int index, int offsetInExpression, ExprElm parmExpression) {
			return null;
		}
		@SignifiesRole(role=DEFAULT_PARMTYPE_ASSIGNER)
		public boolean assignDefaultParmTypes(C4ScriptParser parser, C4Function function) {
			return false;
		}
		@SignifiesRole(role=DEFAULT_PARMTYPE_ASSIGNER)
		public C4Function newFunction(String name) {
			return null;
		}
	}
	
	private Map<String, SpecialFuncRule> argumentValidators = new HashMap<String, SpecialFuncRule>();
	private Map<String, SpecialFuncRule> returnTypeModifiers = new HashMap<String, SpecialFuncRule>();
	private Map<String, SpecialFuncRule> declarationLocators = new HashMap<String, SpecialFuncRule>();
	private List<SpecialFuncRule> defaultParmTypeAssigners = new LinkedList<SpecialFuncRule>();
	
	public Iterable<SpecialFuncRule> defaultParmTypeAssignerRules() {
		return defaultParmTypeAssigners;
	}
	
	protected void putFuncRule(SpecialFuncRule rule, String... forFunctions) {
		int mask = rule.getRoleMask();
		if ((mask & ARGUMENT_VALIDATOR) != 0) for (String func : forFunctions) {
			argumentValidators.put(func, rule);
		}
		if ((mask & DECLARATION_LOCATOR) != 0) for (String func : forFunctions) {
			declarationLocators.put(func, rule);
		}
		if ((mask & RETURNTYPE_MODIFIER) != 0) for (String func : forFunctions) {
			returnTypeModifiers.put(func, rule);
		}
		if ((mask & DEFAULT_PARMTYPE_ASSIGNER) != 0) {
			defaultParmTypeAssigners.add(rule);
		}
	}
	
	public SpecialFuncRule getFuncRuleFor(String function, int role) {
		if (role == ARGUMENT_VALIDATOR)
			return argumentValidators.get(function);
		else if (role == DECLARATION_LOCATOR)
			return declarationLocators.get(function);
		else if (role == RETURNTYPE_MODIFIER)
			return returnTypeModifiers.get(function);
		else
			return null;
	}
	
	/**
	 * CreateObject and similar functions that will return an object of the specified type
	 */
	protected final SpecialFuncRule objectCreationRule = new SpecialFuncRule() {
		@Override
		public IType returnType(C4ScriptParser parser, CallFunc callFunc) {
			if (callFunc.getParams().length >= 1) {
				IType t = callFunc.getParams()[0].getType(parser);
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
	protected final SpecialFuncRule getIDRule = new SpecialFuncRule() {
		@Override
		public IType returnType(C4ScriptParser parser, CallFunc callFunc) {
			C4ScriptBase script = null;
			ConstraintKind constraintKind = null;
			IType t;
			if (callFunc.getParams().length > 0) {
				t = callFunc.getParams()[0].getType(parser);
			} else if (callFunc.getPredecessorInSequence() != null) {
				t = callFunc.getPredecessorInSequence().getType(parser);
			} else {
				constraintKind = ConstraintKind.CallerType;
				script = parser.getContainer();
				t = null;
			}
			if (t instanceof ConstrainedObject) {
				ConstrainedObject cobj = (ConstrainedObject)t;
				constraintKind = cobj.constraintKind();
				script = cobj.constraintScript();
			}
			
			return script != null ? ConstrainedType.get(script, constraintKind) : C4Type.ID;
		}
	};
	
	/**
	 *  It's a criteria search (FindObjects etc) so guess return type from arguments passed to the criteria search function
	 */
	protected final SpecialFuncRule criteriaSearchRule = new SpecialFuncRule() {
		@Override
		public IType returnType(C4ScriptParser parser, CallFunc callFunc) {
			IType t = searchCriteriaAssumedResult(parser, callFunc, true);
			if (t == null) {
				t = C4Type.OBJECT;
			}
			if (t != null) {
				C4Function f = (C4Function) callFunc.getDeclaration();
				if (f != null && f.getReturnType() == C4Type.ARRAY)
					return new ArrayType(t);
			}
			return t;
		}
		
		private C4Object searchCriteriaAssumedResult(C4ScriptParser parser, CallFunc callFunc, boolean topLevel) {
			C4Object result = null;
			String declarationName = callFunc.getDeclarationName();
			// parameters to FindObjects itself are also &&-ed together
			if (topLevel || declarationName.equals("Find_And")) {
				for (ExprElm parm : callFunc.getParams()) {
					if (parm instanceof CallFunc) {
						CallFunc call = (CallFunc)parm;
						C4Object t = searchCriteriaAssumedResult(parser, call, false);
						if (t != null) {
							if (result == null)
								result = t;
							else {
								if (t.includes(result))
									result = t;
							}
						}
					}
				}
			}
			else if (declarationName.equals("Find_ID")) { //$NON-NLS-1$
				if (callFunc.getParams().length > 0) {
					result = callFunc.getParams()[0].guessObjectType(parser);
				}
			}
			return result;
		};
	};
	
	/**
	 *  validate DoSomething() in Schedule("DoSomething()")
	 */
	private final SpecialFuncRule scheduleScriptValidationRule = new SpecialFuncRule() {
		@Override
		public boolean validateArguments(CallFunc callFunc, final ExprElm[] arguments, final C4ScriptParser parser) {
			if (arguments.length < 1)
				return false; // no script expression supplied
			IType objType = arguments.length >= 4 ? arguments[3].getType(parser) : parser.getContainerObject();
			C4ScriptBase script = objType != null ? C4TypeSet.objectIngredient(objType) : null;
			if (script == null)
				script = parser.getContainer(); // fallback
			Object scriptExpr = arguments[0].evaluateAtParseTime(script);
			if (scriptExpr instanceof String) {
				try {
					C4ScriptParser.parseStandaloneStatement((String)scriptExpr, parser.getCurrentFunc(), null, new IMarkerListener() {
						@Override
						public WhatToDo markerEncountered(C4ScriptParser nestedParser, ParserErrorCode code, int markerStart, int markerEnd, boolean noThrow, int severity, Object... args) {
							if (code == ParserErrorCode.NotFinished) {
								// ignore complaining about missing ';'
								ExprElm reporter = nestedParser.getExpressionReportingErrors();
								if (reporter instanceof SimpleStatement && args.length > 0 && args[0] == ((SimpleStatement)reporter).getExpression()) {
									return WhatToDo.DropCharges;
								}
								
								// other cases of NotFinished shall be genuine errors
							}
							try {
								// pass through to the 'real' script parser
								if (parser.errorEnabled(code)) {
									parser.markerWithCode(code, arguments[0].getExprStart()+1+markerStart, arguments[0].getExprStart()+1+markerEnd, true, severity, args);
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
	
	protected final SpecialFuncRule scheduleCallLinkRule = new SpecialFuncRule() {
		@Override
		public DeclarationRegion locateDeclarationInParameter(CallFunc callFunc, C4ScriptParser parser, int index, int offsetInExpression, ExprElm parmExpression) {
			if (index == 1 && parmExpression instanceof StringLiteral) {
				StringLiteral lit = (StringLiteral) parmExpression;
				IType t = callFunc.getParams()[0].getType(parser);
				C4ScriptBase scriptToLookIn = t instanceof C4ScriptBase ? (C4ScriptBase)t : parser.getContainer();
				C4Function func = scriptToLookIn.findFunction(lit.getLiteral());
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
	protected final SpecialFuncRule addCommandValidationRule = new SpecialFuncRule() {
		@Override
		public boolean validateArguments(CallFunc callFunc, ExprElm[] arguments, C4ScriptParser parser) {
			C4Function f = callFunc.getDeclaration() instanceof C4Function ? (C4Function)callFunc.getDeclaration() : null;
			if (f != null && arguments.length >= 3) {
				// look if command is "Call"; if so treat parms 2, 3, 4 as any
				Object command = arguments[1].evaluateAtParseTime(parser.getContainer());
				if (command instanceof String && command.equals("Call")) { //$NON-NLS-1$
					int givenParam = 0;
					for (C4Variable parm : f.getParameters()) {
						if (givenParam >= arguments.length)
							break;
						ExprElm given = arguments[givenParam++];
						if (given == null)
							continue;
						IType parmType = givenParam >= 2 && givenParam <= 4 ? C4Type.ANY : parm.getType();
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
	protected final SpecialFuncRule gameCallLinkRule = new SpecialFuncRule() {
		@Override
		public DeclarationRegion locateDeclarationInParameter(CallFunc callFunc, C4ScriptParser parser, int parameterIndex, int offsetInExpression, ExprElm parmExpression) {
			if (parameterIndex == 0 && parmExpression instanceof StringLiteral) {
				StringLiteral lit = (StringLiteral)parmExpression;
				ClonkIndex index = parser.getContainer().getIndex();
				C4Scenario scenario = ClonkIndex.pickNearest(parser.getContainer().getResource(), index.getIndexedScenarios());
				if (scenario != null) {
					C4Function scenFunc = scenario.findFunction(lit.stringValue());
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
	protected final SpecialFuncRule callLinkRule = new SpecialFuncRule() {
		@Override
		public DeclarationRegion locateDeclarationInParameter(CallFunc callFunc, C4ScriptParser parser, int index, int offsetInExpression, ExprElm parmExpression) {
			if (index == 0 && parmExpression instanceof StringLiteral) {
				StringLiteral lit = (StringLiteral)parmExpression;
				C4Function f = parser.getContainer().findFunction(lit.stringValue());
				if (f != null)
					return new DeclarationRegion(f, lit.identifierRegion());
			}
			return null;
		};
	};
	
	/**
	 * Add link to the function being indirectly called by Private/Public/Protected Call
	 */
	protected final SpecialFuncRule scopedCallLinkRule = new SpecialFuncRule() {
		@Override
		public DeclarationRegion locateDeclarationInParameter(CallFunc callFunc, C4ScriptParser parser, int index, int offsetInExpression, ExprElm parmExpression) {
			if (index == 1 && parmExpression instanceof StringLiteral) {
				StringLiteral lit = (StringLiteral)parmExpression;
				C4Object typeToLookIn = callFunc.getParams()[0].guessObjectType(parser);
				if (typeToLookIn == null && callFunc.getPredecessorInSequence() != null)
					typeToLookIn = callFunc.getPredecessorInSequence().guessObjectType(parser);
				if (typeToLookIn == null)
					typeToLookIn = parser.getContainerObject();
				if (typeToLookIn != null) {
					C4Function f = typeToLookIn.findFunction(lit.stringValue());
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
	protected final SpecialFuncRule localNLinkRule = new SpecialFuncRule() {
		@Override
		public DeclarationRegion locateDeclarationInParameter(CallFunc callFunc, C4ScriptParser parser, int index, int offsetInExpression, ExprElm parmExpression) {
			if (index == 0 && parmExpression instanceof StringLiteral) {
				StringLiteral lit = (StringLiteral)parmExpression;
				C4Object typeToLookIn = callFunc.getParams().length > 1 ? callFunc.getParams()[1].guessObjectType(parser) : null;
				if (typeToLookIn == null && callFunc.getPredecessorInSequence() != null)
					typeToLookIn = callFunc.getPredecessorInSequence().guessObjectType(parser);
				if (typeToLookIn == null)
					typeToLookIn = parser.getContainerObject();
				if (typeToLookIn != null) {
					C4Variable var = typeToLookIn.findVariable(lit.stringValue());
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
	protected final SpecialFuncRule getPlrKnowledgeRule = new SpecialFuncRule() {
		@Override
		public IType returnType(C4ScriptParser parser, CallFunc callFunc) {
			if (callFunc.getParams().length >= 3) {
				return C4Type.ID;
			} else {
				return C4Type.INT;
			}
		};
	};

	public SpecialScriptRules() {
		putFuncRule(objectCreationRule, "CreateObject", "CreateContents");
		putFuncRule(getIDRule, "GetID");
		putFuncRule(criteriaSearchRule, "FindObjects");
		putFuncRule(scheduleScriptValidationRule, "Schedule");
		putFuncRule(scheduleCallLinkRule, "ScheduleCall");
		putFuncRule(addCommandValidationRule, "AddCommand", "AppendCommand", "SetCommand");
		putFuncRule(gameCallLinkRule, "GameCall");
		putFuncRule(callLinkRule, "Call");
		putFuncRule(scopedCallLinkRule, "PrivateCall", "PublicCall", "PrivateCall");
		putFuncRule(localNLinkRule, "LocalN");
		putFuncRule(getPlrKnowledgeRule, "GetPlrKnowledge", "GetPlrMagic");
	}
}
