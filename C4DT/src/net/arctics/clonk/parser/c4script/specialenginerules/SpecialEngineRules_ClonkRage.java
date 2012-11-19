package net.arctics.clonk.parser.c4script.specialenginerules;

import static net.arctics.clonk.util.ArrayUtil.map;

import java.util.Map;
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
import net.arctics.clonk.parser.inireader.IniEntry;
import net.arctics.clonk.util.IPredicate;

public class SpecialEngineRules_ClonkRage extends SpecialEngineRules {
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
	
	private static final Pattern ID_PATTERN = Pattern.compile("[A-Z_0-9]{4}");
	
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
	
	private static final Map<String, String> entryToCategoryMap = map(false,
		"Animal", "C4D_SelectAnimal",
		"Buildings", "C4D_SelectBuilding",
		"HomeBaseMaterial", "C4D_SelectHomebase",
		"Nest", "C4D_SelectNest",
		"Vehicles", "C4D_SelectVehicle",
		"Vegetation", "C4D_SelectVegetation"
	);
	
	@Override
	public IPredicate<Definition> configurationEntryDefinitionFilter(final IniEntry entry) {
		{
			final String category = entryToCategoryMap.get(entry.key());
			if (category != null)
				return new IPredicate<Definition>() {
					@Override
					public boolean test(Definition item) {
						return item.categorySet(category);
					}
				};
		}
		if (entry.key().equals("Goals"))
			return new IPredicate<Definition>() {
				final Definition goal = entry.index().anyDefinitionWithID(ID.get("GOAL"));
				@Override
				public boolean test(Definition item) {
					return goal != null && item != goal && item.doesInclude(entry.index(), goal);
				}
			};
		else
			return super.configurationEntryDefinitionFilter(entry);
	}
}