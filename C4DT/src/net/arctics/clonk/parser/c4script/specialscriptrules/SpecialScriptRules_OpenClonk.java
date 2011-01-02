package net.arctics.clonk.parser.c4script.specialscriptrules;

import net.arctics.clonk.parser.c4script.SpecialScriptRules;

public class SpecialScriptRules_OpenClonk extends SpecialScriptRules {
	public SpecialScriptRules_OpenClonk() {
		super();
		putFuncRule(criteriaSearchRule, "FindObject");
	}
}
