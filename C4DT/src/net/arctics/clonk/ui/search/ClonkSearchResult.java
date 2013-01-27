package net.arctics.clonk.ui.search;

import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.c4script.ProblemReportingContext;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.IEditorMatchAdapter;
import org.eclipse.search.ui.text.IFileMatchAdapter;

public class ClonkSearchResult extends AbstractTextSearchResult {
	
	private final SearchQueryBase query;
	
	public ClonkSearchResult(SearchQueryBase query) {
		this.query = query;
	}

	@Override
	public IEditorMatchAdapter getEditorMatchAdapter() {
		return query;
	}

	@Override
	public IFileMatchAdapter getFileMatchAdapter() {
		return query;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	@Override
	public String getLabel() {
		return query.getLabel();
	}

	@Override
	public ISearchQuery getQuery() {
		return query;
	}

	@Override
	public String getTooltip() {
		return null;
	}
	
	public void addMatch(ProblemReportingContext context, boolean potential, boolean indirect, int s, int l) {
		IRegion lineRegion = context.scanner().regionOfLineContainingRegion(new Region(s, l));
		String line = context.scanner().bufferSubstringAtRegion(lineRegion);
		addMatch(new ClonkSearchMatch(line, lineRegion.getOffset(), context.script(), s, l, potential, indirect));
	}
	
	public void addMatch(ASTNode match, ProblemReportingContext context, boolean potential, boolean indirect) {
		addMatch(context, potential, indirect, match.identifierStart()+match.sectionOffset(), match.identifierLength());
	}

}