package net.arctics.clonk.ui.search;

import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.c4script.C4ScriptParser;

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
	
	public void addMatch(C4ScriptParser parser, boolean potential, boolean indirect, int s, int l) {
		IRegion lineRegion = parser.regionOfLineContainingRegion(new Region(s, l));
		String line = parser.bufferSubstringAtRegion(lineRegion).trim();
		addMatch(new ClonkSearchMatch(line, lineRegion.getOffset(), parser.script(), s, l, potential, indirect));
	}
	
	public void addMatch(ASTNode match, C4ScriptParser parser, boolean potential, boolean indirect) {
		addMatch(parser, potential, indirect, match.identifierStart()+parser.sectionOffset(), match.identifierLength());
	}

}