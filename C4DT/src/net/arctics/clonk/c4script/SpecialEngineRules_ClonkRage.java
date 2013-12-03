package net.arctics.clonk.c4script;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.EntityRegion;
import net.arctics.clonk.c4script.ast.CallDeclaration;
import net.arctics.clonk.c4script.typing.IType;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.ID;
import net.arctics.clonk.parser.BufferedScanner;

public class SpecialEngineRules_ClonkRage extends SpecialEngineRules {
	private static final Pattern ID_PATTERN = Pattern.compile("[A-Z_0-9]{4}");
	public SpecialEngineRules_ClonkRage(final Engine engine) {
		super(engine);
		putFuncRule(criteriaSearchRule, "FindObject2");
		putFuncRule(objectCreationRule, "FindObject");
		putFuncRule(setActionLinkRule = new SetActionLinkRule() {
			@Override
			public EntityRegion locateEntityInParameter(final CallDeclaration node, final Script script, final int index, final int offsetInExpression, final ASTNode parmExpression) {
				if (index == 1 && node.name().equals("ObjectSetAction")) {
					final IType t = script.typings().get(node.params()[0]);
					if (t != null) for (final IType ty : t)
						if (ty instanceof Definition) {
							final Definition def = (Definition)ty;
							final EntityRegion result = actionLinkForDefinition(node.parent(Function.class), def, parmExpression);
							if (result != null)
								return result;
						}
				}
				return super.locateEntityInParameter(node, script, index, offsetInExpression, parmExpression);
			};
		}, "ObjectSetAction");
	}
	@Override
	public ID parseId(final BufferedScanner scanner) {
		final Matcher idMatcher = ID_PATTERN.matcher(scanner.bufferSequence(scanner.tell()));
		if (idMatcher.lookingAt()) {
			final String idString = idMatcher.group();
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
