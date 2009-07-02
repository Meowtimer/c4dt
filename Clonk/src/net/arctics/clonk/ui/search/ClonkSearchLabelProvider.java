package net.arctics.clonk.ui.search;

import net.arctics.clonk.index.C4ObjectIntern;
import net.arctics.clonk.index.C4Scenario;
import net.arctics.clonk.parser.c4script.C4ScriptIntern;
import net.arctics.clonk.util.Icons;
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
		if (element instanceof C4Scenario) {
			return Icons.SCENARIO_ICON;
		}
		if (element instanceof C4ObjectIntern) {
			return Icons.GENERAL_OBJECT_ICON;
		}
		if (element instanceof C4ScriptIntern) {
			return Icons.SCRIPT_ICON;
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
