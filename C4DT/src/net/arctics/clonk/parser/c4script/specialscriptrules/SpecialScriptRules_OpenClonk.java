package net.arctics.clonk.parser.c4script.specialscriptrules;

import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.EffectFunction;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.ProplistDeclaration;
import net.arctics.clonk.parser.c4script.SpecialScriptRules;
import net.arctics.clonk.parser.c4script.ast.CallFunc;

public class SpecialScriptRules_OpenClonk extends SpecialScriptRules {
	/**
	 * Assign default parameters to effect functions.
	 * For the effect proplist parameter, an implicit ProplistDeclaration is created
	 * so that type information and proplist value locations (first assignment) can
	 * be stored.
	 */
	public final SpecialFuncRule effectFunctionParmTypes = new SpecialFuncRule() {
		@Override
		public boolean assignDefaultParmTypes(C4ScriptParser parser, Function function) {
			if (function instanceof EffectFunction) {
				EffectFunction fun = (EffectFunction) function;
				fun.findRelatedEffectFunctions();
				EffectFunction startFunction = fun.getEffectFunctionType() == EffectFunction.Type.Start
					? null
					: fun.getRelatedEffectFunction(EffectFunction.Type.Start);
				// parse *Start function first. Will define ad-hoc proplist type
				if (startFunction != null) {
					try {
						parser.parseCodeOfFunction(startFunction, true);
					} catch (ParsingException e) {
						e.printStackTrace();
					}
				}

				IType effectProplistType = startFunction != null && startFunction.getParameters().size() >= 2
					? startFunction.getParameters().get(1).getType()
					: createAdHocProplistDeclaration(fun);
//					: C4Type.PROPLIST;
				function.assignParameterTypes(PrimitiveType.OBJECT, effectProplistType);
				return true;
			}
			return false;
		}
		private IType createAdHocProplistDeclaration(EffectFunction startFunction) {
			ProplistDeclaration result = ProplistDeclaration.adHocDeclaration();
			result.setLocation(startFunction.getLocation());
			result.setParentDeclaration(startFunction);
			startFunction.addOtherDeclaration(result);
			return result;
		}
		@Override
		public Function newFunction(String name) {
			for (EffectFunction.Type t : EffectFunction.Type.values()) {
				if (t.getPattern().matcher(name).matches())
					return new EffectFunction();
			}
			return null;
		};
	};
	/**
	 * Result of GetEffect is appropriate ProplistDeclaration created in <code>effectFunctionParmTypes</code>
	 */
	public final SpecialFuncRule getEffectResultTypeRule = new SpecialFuncRule() {
		@Override
		public IType returnType(C4ScriptParser parser, CallFunc callFunc) {
			Object parmEv;
			if (callFunc.getParams().length >= 1 && (parmEv = callFunc.getParams()[0].evaluateAtParseTime(parser.getContainer())) instanceof String) {
				String effectName = (String) parmEv;
				for (EffectFunction.Type t : EffectFunction.Type.values()) {
					Declaration d = CallFunc.findFunctionUsingPredecessor(
							callFunc.getPredecessorInSequence(),
							String.format(EffectFunction.FUNCTION_NAME_FORMAT, effectName, t.name()), 
							parser
					);
					if (d instanceof EffectFunction && ((EffectFunction)d).getParameters().size() >= 2) {
						return ((EffectFunction)d).getParameters().get(1).getType();
					}
				}
			}
			return null;
		};
	};
	public SpecialScriptRules_OpenClonk() {
		super();
		putFuncRule(criteriaSearchRule, "FindObject");
		putFuncRule(effectFunctionParmTypes);
		putFuncRule(getEffectResultTypeRule, "GetEffect", "AddEffect");
	}
}
