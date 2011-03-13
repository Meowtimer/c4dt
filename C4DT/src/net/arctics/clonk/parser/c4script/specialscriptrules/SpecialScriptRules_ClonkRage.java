package net.arctics.clonk.parser.c4script.specialscriptrules;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.arctics.clonk.index.Definition;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.DeclarationRegion;
import net.arctics.clonk.parser.ID;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.SpecialScriptRules;
import net.arctics.clonk.parser.c4script.ast.CallFunc;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.C4ScriptParser;

public class SpecialScriptRules_ClonkRage extends SpecialScriptRules {
	public SpecialScriptRules_ClonkRage() {
		super();
		putFuncRule(criteriaSearchRule, "FindObject2");
		putFuncRule(setActionLinkRule = new SetActionLinkRule() {
			@Override
			public DeclarationRegion locateDeclarationInParameter(CallFunc callFunc, C4ScriptParser parser, int index, int offsetInExpression, ExprElm parmExpression) {
				if (index == 1 && callFunc.getDeclarationName().equals("ObjectSetAction")) {
					IType t = callFunc.getParams()[0].getType(parser);
					if (t != null) for (IType ty : t) {
						if (ty instanceof Definition) {
							Definition def = (Definition)ty;
							DeclarationRegion result = getActionLinkForDefinition(def, parmExpression);
							if (result != null)
								return result;
						}
					}
				}
				return super.locateDeclarationInParameter(callFunc, parser, index, offsetInExpression, parmExpression);
			};
		}, "ObjectSetAction");
	}
	
	private static final Matcher ID_MATCHER = Pattern.compile("[A-Z_0-9]{4}").matcher("");
	
	@Override
	public ID parseId(BufferedScanner scanner) {
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
