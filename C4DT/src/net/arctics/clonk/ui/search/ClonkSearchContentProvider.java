package net.arctics.clonk.ui.search;

import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Scenario;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.SystemScript;
import net.arctics.clonk.resource.c4group.C4Group.GroupType;
import net.arctics.clonk.ui.navigator.ClonkLabelProvider;
import net.arctics.clonk.util.ITreeNode;
import net.arctics.clonk.util.UI;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.search.ui.text.Match;
import org.eclipse.swt.graphics.Image;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;

public class ClonkSearchContentProvider extends ClonkLabelProvider implements ITreeContentProvider, ILabelProvider, DelegatingStyledCellLabelProvider.IStyledLabelProvider {

	private boolean flat;
	private ClonkSearchResult searchResult;
	
	public ClonkSearchContentProvider(ClonkSearchResultPage page, boolean flat) {
		super();
		this.flat = flat;
	}

	@Override
	public Object[] getChildren(Object element) {
		return searchResult.getMatches(element);
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof ITreeNode)
			return ((ITreeNode)element).parentNode();
		else
			return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof ITreeNode)
			return ((ITreeNode)element).childCollection().size() > 0;
		else
			return searchResult.getMatchCount(element) > 0;		
	}

	@Override
	public Object[] getElements(Object input) {
		if (flat) {
			List<Match> matches = new LinkedList<Match>(); 
			for (Object elm : searchResult.getElements()) {
				for (Match m : searchResult.getMatches(elm))
					matches.add(m);
			}
			return matches.toArray(new Match[matches.size()]);
		} else
			return searchResult.getElements();
	}

	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		searchResult = (ClonkSearchResult) newInput;
	}
	
	@Override
	public String getText(Object element) {
		if (element instanceof Function)
			return ((Function)element).qualifiedName();
		else if (element instanceof IHasLabelAndImage)
			return ((IHasLabelAndImage)element).getLabel();
		else
			return element.toString();
	}
	@Override
	public Image getImage(Object element) {
		Engine engine = element instanceof Declaration ? ((Declaration)element).engine() : null;
		if (engine != null) {
			if (element instanceof Scenario) {
				return engine.image(GroupType.ScenarioGroup);
			}
			if (element instanceof Definition) {
				return engine.image(GroupType.DefinitionGroup);
			}
			if (element instanceof SystemScript) {
				return UI.SCRIPT_ICON;
			}
			
		}
		else if (element instanceof IHasLabelAndImage) {
			IHasLabelAndImage lblimg = (IHasLabelAndImage) element;
			return lblimg.getImage();
		}
		return super.getImage(element);
	}
	@Override
	public StyledString getStyledText(Object element) {
		if (element instanceof ClonkSearchMatch) try {
			StyledString result = new StyledString();
			ClonkSearchMatch match = (ClonkSearchMatch) element;
			String firstHalf = match.getLine().substring(0, match.getOffset()-match.getLineOffset());
			String matchStr = match.getLine().substring(match.getOffset()-match.getLineOffset(), match.getOffset()-match.getLineOffset()+match.getLength());
			String secondHalf = match.getLine().substring(match.getOffset()-match.getLineOffset()+match.getLength(), match.getLine().length());
			result.append(firstHalf);
			result.append(matchStr, StyledString.DECORATIONS_STYLER);
			result.append(secondHalf);
			result.append(" - ");
			result.append(match.getStructure().resource().getProjectRelativePath().toOSString(), StyledString.QUALIFIER_STYLER);
			return result;
		} catch (Exception e) {
			return new StyledString(((ClonkSearchMatch)element).getLine());
		}
		else if (element instanceof Function) {
			return new StyledString(((Function)element).qualifiedName());
		}
		else if (element instanceof IHasLabelAndImage) {
			IHasLabelAndImage lblimg = (IHasLabelAndImage) element;
			return new StyledString(lblimg.getLabel());
		}
		return new StyledString(element.toString());
	}

	public ViewerComparator getComparator() {
		return new ViewerComparator() {
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				return getText(e1).compareTo(getText(e2));
			}
		};
	}

}
