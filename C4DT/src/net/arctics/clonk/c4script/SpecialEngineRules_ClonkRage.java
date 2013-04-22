package net.arctics.clonk.c4script;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.EntityRegion;
import net.arctics.clonk.ast.ID;
import net.arctics.clonk.c4script.ast.CallDeclaration;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.parser.BufferedScanner;

public class SpecialEngineRules_ClonkRage extends SpecialEngineRules {
	private static final Pattern ID_PATTERN = Pattern.compile("[A-Z_0-9]{4}");
	public SpecialEngineRules_ClonkRage() {
		super();
		putFuncRule(criteriaSearchRule, "FindObject2");
		putFuncRule(objectCreationRule, "FindObject");
		putFuncRule(setActionLinkRule = new SetActionLinkRule() {
			@Override
			public EntityRegion locateEntityInParameter(CallDeclaration node, ProblemReportingContext processor, int index, int offsetInExpression, ASTNode parmExpression) {
				if (index == 1 && node.name().equals("ObjectSetAction")) {
					IType t = processor.typeOf(node.params()[0]);
					if (t != null) for (IType ty : t)
						if (ty instanceof Definition) {
							Definition def = (Definition)ty;
							EntityRegion result = actionLinkForDefinition(node.parentOfType(Function.class), def, parmExpression);
							if (result != null)
								return result;
						}
				}
				return super.locateEntityInParameter(node, processor, index, offsetInExpression, parmExpression);
			};
		}, "ObjectSetAction");
	}
	@Override
	public ID parseId(BufferedScanner scanner) {
		Matcher idMatcher = ID_PATTERN.matcher(scanner.bufferSequence(scanner.tell()));
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
