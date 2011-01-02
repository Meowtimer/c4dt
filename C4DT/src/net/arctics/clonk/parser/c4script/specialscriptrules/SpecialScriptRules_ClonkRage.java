package net.arctics.clonk.parser.c4script.specialscriptrules;

import net.arctics.clonk.parser.c4script.SpecialScriptRules;

public class SpecialScriptRules_ClonkRage extends SpecialScriptRules {
	public SpecialScriptRules_ClonkRage() {
		super();
		putFuncRule(criteriaSearchRule, "FindObject2");
	}
}
