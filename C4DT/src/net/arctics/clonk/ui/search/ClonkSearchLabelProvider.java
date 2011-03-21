package net.arctics.clonk.ui.search;

import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.ProjectDefinition;

import net.arctics.clonk.index.Scenario;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.c4script.StandaloneProjectScript;
import net.arctics.clonk.resource.c4group.C4Group.GroupType;
import net.arctics.clonk.ui.navigator.ClonkLabelProvider;
import net.arctics.clonk.util.UI;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.swt.graphics.Image;

public class ClonkSearchLabelProvider extends ClonkLabelProvider implements IStyledLabelProvider {
	@Override
	public String getText(Object element) {
		return element.toString();
	}
	@Override
	public Image getImage(Object element) {
		Engine engine = element instanceof Declaration ? ((Declaration)element).getEngine() : null;
		if (engine != null) {
			if (element instanceof Scenario) {
				return engine.getGroupTypeToIconMap().get(GroupType.ScenarioGroup);
			}
			if (element instanceof ProjectDefinition) {
				return engine.getGroupTypeToIconMap().get(GroupType.DefinitionGroup);
			}
			if (element instanceof StandaloneProjectScript) {
				return UI.SCRIPT_ICON;
			}
			
		}
		if (element instanceof ClonkSearchMatch) {
			return super.getImage(((ClonkSearchMatch)element).getCookie());
		}
		return null;
	}
	public StyledString getStyledText(Object element) {
		if (element instanceof ClonkSearchMatch) {
			StyledString result = new StyledString();
			ClonkSearchMatch match = (ClonkSearchMatch) element;
			String firstHalf = match.getLine().substring(match.getLineOffset(), match.getLineOffset()+match.getOffset());
			String matchStr = match.getLine().substring(match.getLineOffset()+match.getOffset(), match.getLineOffset()+match.getOffset()+match.getLength());
			String secondHalf = match.getLine().substring(match.getLineOffset()+match.getOffset()+match.getLength(), match.getLineOffset()+match.getLine().length());
			result.append(firstHalf);
			result.append(matchStr, StyledString.DECORATIONS_STYLER);
			result.append(secondHalf);
			return result;
		}
		return new StyledString(element.toString());
	}
}
