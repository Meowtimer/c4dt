package net.arctics.clonk.parser.c4script.specialscriptrules;

import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4EffectFunction;
import net.arctics.clonk.parser.c4script.C4Function;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.C4Type;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.ProplistDeclaration;
import net.arctics.clonk.parser.c4script.SpecialScriptRules;

public class SpecialScriptRules_OpenClonk extends SpecialScriptRules {
	/**
	 * Assign default parameters to effect functions
	 */
	protected final SpecialFuncRule effectFunctionParmTypes = new SpecialFuncRule() {
		@Override
		public boolean assignDefaultParmTypes(C4ScriptParser parser, C4Function function) {
			if (function instanceof C4EffectFunction) {
				C4EffectFunction fun = (C4EffectFunction) function;
				fun.findRelatedEffectFunctions();
				C4EffectFunction startFunction = fun.getEffectFunctionType() == C4EffectFunction.Type.Start
					? null
					: fun.getRelatedEffectFunction(C4EffectFunction.Type.Start);
				// parse *Start function first. Will define ad-hoc proplist type
				if (startFunction != null) {
					try {
						parser.parseCodeOfFunction(startFunction);
					} catch (ParsingException e) {
						e.printStackTrace();
					}
				}

				IType effectProplistType = startFunction != null && startFunction.getParameters().size() >= 2
					? startFunction.getParameters().get(1).getType()
					: createAdHocProplistDeclaration(fun);
//					: C4Type.PROPLIST;
				function.assignParameterTypes(C4Type.OBJECT, effectProplistType);
				return true;
			}
			return false;
		}
		private IType createAdHocProplistDeclaration(C4EffectFunction startFunction) {
			ProplistDeclaration result = ProplistDeclaration.adHocDeclaration();
			result.setLocation(startFunction.getLocation());
			result.setParentDeclaration(startFunction);
			startFunction.addOtherDeclaration(result);
			return result;
		}
		@Override
		public C4Function newFunction(String name) {
			for (C4EffectFunction.Type t : C4EffectFunction.Type.values()) {
				if (t.getPattern().matcher(name).matches())
					return new C4EffectFunction();
			}
			return null;
		};
	};
	public SpecialScriptRules_OpenClonk() {
		super();
		putFuncRule(criteriaSearchRule, "FindObject");
		putFuncRule(effectFunctionParmTypes);
	}
}
