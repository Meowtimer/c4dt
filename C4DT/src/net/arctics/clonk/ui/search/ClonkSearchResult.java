package net.arctics.clonk.ui.search;

import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.ExprElm;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IRegion;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.IEditorMatchAdapter;
import org.eclipse.search.ui.text.IFileMatchAdapter;
import org.eclipse.search.ui.text.Match;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

public class ClonkSearchResult extends AbstractTextSearchResult implements IEditorMatchAdapter, IFileMatchAdapter {
	
	private static final Match[] NO_MATCHES = new Match[0];
	
	private ClonkSearchQuery query;
	
	public ClonkSearchResult(ClonkSearchQuery query) {
		this.query = query;
	}

	@Override
	public IEditorMatchAdapter getEditorMatchAdapter() {
		return this;
	}

	@Override
	public IFileMatchAdapter getFileMatchAdapter() {
		return this;
	}

	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	public String getLabel() {
		return query.getLabel();
	}

	public ISearchQuery getQuery() {
		return query;
	}

	public String getTooltip() {
		return null;
	}

	public Match[] computeContainedMatches(AbstractTextSearchResult result,
			IEditorPart editor) {
		if (editor instanceof ITextEditor) {
			C4ScriptBase script = Utilities.getScriptForEditor((ITextEditor) editor);
			if (script != null)
				return result.getMatches(script);
		}
		return NO_MATCHES;
	}

	public boolean isShownInEditor(Match match, IEditorPart editor) {
		if (editor instanceof ITextEditor) {
			C4ScriptBase script = Utilities.getScriptForEditor((ITextEditor)editor);
			if (script != null && match.getElement().equals(script.getScriptFile()))
				return true;
		}
		return false;
	}

	public Match[] computeContainedMatches(AbstractTextSearchResult result,
			IFile file) {
		C4ScriptBase script = C4ScriptBase.get(file, true);
		if (script != null)
			return result.getMatches(script);
		return NO_MATCHES;
	}

	public IFile getFile(Object element) {
		if (element instanceof C4ScriptBase)
			return (IFile) ((C4ScriptBase)element).getScriptFile();
		if (element instanceof IFile) {
			return (IFile)element;
		}
		return null;
	}
	
	public void addMatch(ExprElm match, C4ScriptParser parser, boolean potential, boolean indirect, int s, int l) {
		IRegion lineRegion = parser.getLineRegion(match);
		String line = parser.getSubstringOfScript(lineRegion);
		addMatch(new ClonkSearchMatch(line, lineRegion.getOffset(), parser.getContainer(), s, l, potential, indirect));
	}
	
	public void addMatch(ExprElm match, C4ScriptParser parser, boolean potential, boolean indirect) {
		addMatch(match, parser, potential, indirect, match.getIdentifierStart(), match.getIdentifierLength());
	}

}