package net.arctics.clonk.ui.search;

import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.IEditorMatchAdapter;
import org.eclipse.search.ui.text.IFileMatchAdapter;

public class ClonkSearchResult extends AbstractTextSearchResult {
	
	private ClonkSearchQueryBase query;
	
	public ClonkSearchResult(ClonkSearchQueryBase query) {
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
	
	public void addMatch(ExprElm match, C4ScriptParser parser, boolean potential, boolean indirect, int s, int l) {
		IRegion lineRegion = parser.getLineRegion(new Region(s, l));
		String line = parser.getSubstringOfBuffer(lineRegion);
		addMatch(new ClonkSearchMatch(line, lineRegion.getOffset(), parser.getContainer(), s, l, potential, indirect));
	}
	
	public void addMatch(ExprElm match, C4ScriptParser parser, boolean potential, boolean indirect) {
		addMatch(match, parser, potential, indirect, match.identifierStart()+parser.bodyOffset(), match.identifierLength());
	}

}