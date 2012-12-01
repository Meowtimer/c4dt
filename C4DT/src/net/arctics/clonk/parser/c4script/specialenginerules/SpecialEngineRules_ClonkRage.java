package net.arctics.clonk.parser.c4script.specialenginerules;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.arctics.clonk.index.Definition;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.EntityRegion;
import net.arctics.clonk.parser.ID;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.SpecialEngineRules;
import net.arctics.clonk.parser.c4script.ast.CallDeclaration;
import net.arctics.clonk.parser.c4script.ast.ExprElm;

public class SpecialEngineRules_ClonkRage extends SpecialEngineRules {
	private static final Pattern ID_PATTERN = Pattern.compile("[A-Z_0-9]{4}");
	public SpecialEngineRules_ClonkRage() {
		super();
		putFuncRule(criteriaSearchRule, "FindObject2");
		putFuncRule(objectCreationRule, "FindObject");
		putFuncRule(setActionLinkRule = new SetActionLinkRule() {
			@Override
			public EntityRegion locateEntityInParameter(CallDeclaration callFunc, C4ScriptParser parser, int index, int offsetInExpression, ExprElm parmExpression) {
				if (index == 1 && callFunc.declarationName().equals("ObjectSetAction")) {
					IType t = callFunc.params()[0].type(parser);
					if (t != null) for (IType ty : t)
						if (ty instanceof Definition) {
							Definition def = (Definition)ty;
							EntityRegion result = getActionLinkForDefinition(parser.currentFunction(), def, parmExpression);
							if (result != null)
								return result;
						}
				}
				return super.locateEntityInParameter(callFunc, parser, index, offsetInExpression, parmExpression);
			};
		}, "ObjectSetAction");
	}
	@Override
	public ID parseId(BufferedScanner scanner) {
		Matcher idMatcher = ID_PATTERN.matcher(scanner.buffer().substring(scanner.tell()));
		if (idMatcher.lookingAt()) {
			String idString = idMatcher.group();
			scanner.advance(idString.length());
			if (BufferedScanner.isWordPart(scanner.peek()) || BufferedScanner.NUMERAL_PATTERN.matcher(idString).matches()) {
				scanner.advance(-idString.length());
				return null;
			}
			return ID.get(idString);
		}
		return null;
	}
}
