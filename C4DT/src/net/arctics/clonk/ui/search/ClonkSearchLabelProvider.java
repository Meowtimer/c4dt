package net.arctics.clonk.ui.search;

import net.arctics.clonk.index.ProjectDefinition;
import net.arctics.clonk.index.Scenario;
import net.arctics.clonk.parser.c4script.StandaloneProjectScript;
import net.arctics.clonk.util.UI;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.swt.graphics.Image;

public class ClonkSearchLabelProvider extends LabelProvider implements IStyledLabelProvider {
	@Override
	public String getText(Object element) {
		return element.toString();
	}
	@Override
	public Image getImage(Object element) {
		if (element instanceof Scenario) {
			return UI.SCENARIO_ICON;
		}
		if (element instanceof ProjectDefinition) {
			return UI.GENERAL_OBJECT_ICON;
		}
		if (element instanceof StandaloneProjectScript) {
			return UI.SCRIPT_ICON;
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
