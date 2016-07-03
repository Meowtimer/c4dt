package net.arctics.clonk.ui.search;

import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.c4group.FileExtension;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.SystemScript;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.Scenario;
import net.arctics.clonk.ui.navigator.ClonkLabelProvider;
import net.arctics.clonk.util.IHasLabelAndImage;
import net.arctics.clonk.util.ITreeNode;
import net.arctics.clonk.util.UI;

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.search.ui.text.Match;
import org.eclipse.swt.graphics.Image;

public class SearchContentProvider extends ClonkLabelProvider implements ITreeContentProvider, ILabelProvider, DelegatingStyledCellLabelProvider.IStyledLabelProvider {

	private final boolean flat;
	private SearchResult searchResult;

	public SearchContentProvider(final SearchResultPage page, final boolean flat) {
		super();
		this.flat = flat;
	}

	@Override
	public Object[] getChildren(final Object element) {
		return searchResult.getMatches(element);
	}

	@Override
	public Object getParent(final Object element) {
		if (element instanceof ITreeNode)
			return ((ITreeNode)element).parentNode();
		else
			return null;
	}

	@Override
	public boolean hasChildren(final Object element) {
		if (element instanceof ITreeNode)
			return ((ITreeNode)element).childCollection().size() > 0;
		else
			return searchResult.getMatchCount(element) > 0;
	}

	@Override
	public Object[] getElements(final Object input) {
		if (flat) {
			final List<Match> matches = new LinkedList<Match>();
			for (final Object elm : searchResult.getElements())
				for (final Match m : searchResult.getMatches(elm))
					matches.add(m);
			return matches.toArray(new Match[matches.size()]);
		} else
			return searchResult.getElements();
	}

	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
		searchResult = (SearchResult) newInput;
	}

	@Override
	public String getText(final Object element) {
		if (element instanceof Function)
			return ((Function)element).qualifiedName();
		else if (element instanceof IHasLabelAndImage)
			return ((IHasLabelAndImage)element).label();
		else
			return element.toString();
	}
	@Override
	public Image getImage(final Object element) {
		final Engine engine = element instanceof Declaration ? ((Declaration)element).engine() : null;
		if (engine != null) {
			if (element instanceof Scenario)
				return engine.image(FileExtension.ScenarioGroup);
			if (element instanceof Definition)
				return engine.image(FileExtension.DefinitionGroup);
			if (element instanceof SystemScript)
				return UI.SCRIPT_ICON;

		}
		else if (element instanceof IHasLabelAndImage) {
			final IHasLabelAndImage lblimg = (IHasLabelAndImage) element;
			return lblimg.image();
		}
		return super.getImage(element);
	}
	@Override
	public StyledString getStyledText(final Object element) {
		if (element instanceof SearchMatch) try {
			final StyledString result = new StyledString();
			final SearchMatch match = (SearchMatch) element;
			final String firstHalf = match.line().substring(0, match.getOffset()-match.lineOffset());
			final String matchStr = match.line().substring(match.getOffset()-match.lineOffset(), match.getOffset()-match.lineOffset()+match.getLength());
			final String secondHalf = match.line().substring(match.getOffset()-match.lineOffset()+match.getLength(), match.line().length());
			result.append(firstHalf);
			result.append(matchStr, StyledString.DECORATIONS_STYLER);
			result.append(secondHalf);
			result.append(" - ");
			result.append(match.structure().file().getProjectRelativePath().toOSString(), StyledString.QUALIFIER_STYLER);
			return result;
		} catch (final Exception e) {
			return new StyledString(((SearchMatch)element).line());
		}
		else if (element instanceof Function)
			return new StyledString(((Function)element).qualifiedName());
		else if (element instanceof IHasLabelAndImage) {
			final IHasLabelAndImage lblimg = (IHasLabelAndImage) element;
			return new StyledString(lblimg.label());
		}
		return new StyledString(element.toString());
	}

	public ViewerComparator getComparator() {
		return new ViewerComparator() {
			@Override
			public int compare(final Viewer viewer, final Object e1, final Object e2) {
				try {
					return getText(e1).compareTo(getText(e2));
				} catch (final Exception e) {
					return -1;
				}
			}
		};
	}

}
