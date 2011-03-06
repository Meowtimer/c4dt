package net.arctics.clonk.parser.c4script.specialscriptrules;

import java.util.regex.Matcher;

import org.eclipse.jface.text.Region;

import net.arctics.clonk.index.Definition;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.DeclarationRegion;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.DefinitionFunction;
import net.arctics.clonk.parser.c4script.EffectFunction;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.ProplistDeclaration;
import net.arctics.clonk.parser.c4script.SpecialScriptRules;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.EffectFunction.HardcodedCallbackType;
import net.arctics.clonk.parser.c4script.Variable.Scope;
import net.arctics.clonk.parser.c4script.ast.CallFunc;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.StringLiteral;

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
	@AppliedTo(functions={"GetEffect", "AddEffect", "RemoveEffect"})
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
			ProplistDeclaration result = ProplistDeclaration.adHocDeclaration();
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
			if (callFunc.getParams().length >= 1 && (parmEv = callFunc.getParams()[0].evaluateAtParseTime(context.getContainer())) instanceof String) {
				String effectName = (String) parmEv;
				for (EffectFunction.HardcodedCallbackType t : EffectFunction.HardcodedCallbackType.values()) {
					Declaration d = CallFunc.findFunctionUsingPredecessor(
							callFunc.getPredecessorInSequence(),
							String.format(EffectFunction.FUNCTION_NAME_FORMAT, effectName, t.name()), 
							context
					);
					if (d instanceof EffectFunction) {
						return ((EffectFunction)d).getEffectType();
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
							parser
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
			if (name.equals("Definition")) {
				return new DefinitionFunction();
			}
			else
				return null;
		};
		@Override
		public boolean validateArguments(CallFunc callFunc, ExprElm[] arguments, C4ScriptParser parser) {
			if (arguments.length >= 2 && parser.getCurrentFunc() instanceof DefinitionFunction) {
				Object nameEv = arguments[0].evaluateAtParseTime(parser.getContainer());
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
			if (function.getName().equals("Definition"))
				return;
			Function definitionFunc = function.getScript().findLocalFunction("Definition", false);
			if (definitionFunc != null) {
				try {
					context.parseCodeOfFunction(definitionFunc, true);
				} catch (ParsingException e) {
					e.printStackTrace();
				}
			}
		};
	};
	public SpecialScriptRules_OpenClonk() {
		super();
		// override SetAction link rule to also take into account local 'ActMap' vars
		setActionLinkRule = new SetActionLinkRule() {
			@Override
			protected DeclarationRegion getActionLinkForDefinition(Definition definition, ExprElm parmExpression) {
				if (definition == null)
					return null;
				Object parmEv;
				DeclarationRegion result = super.getActionLinkForDefinition(definition, parmExpression);
				if (result != null)
					return result;
				else if ((parmEv = parmExpression.evaluateAtParseTime(definition)) instanceof String) {
					Variable actMapLocal = definition.findLocalVariable("ActMap", true);
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
						DeclarationRegion result = getActionLinkForDefinition((Definition)ty, parmExpression);
						if (result != null)
							return result;
					}
				}
				return super.locateDeclarationInParameter(callFunc, parser, index, offsetInExpression, parmExpression);
			};
		};
	}
	@Override
	public void initialize() {
		super.initialize();
		putFuncRule(criteriaSearchRule, "FindObject");
	}
}
