package net.arctics.clonk.ui.search;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.ClonkCore.IDocumentAction;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.c4script.ScriptBase;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IDocument;
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
	
	private ClonkSearchQueryBase query;
	
	public ClonkSearchResult(ClonkSearchQueryBase query) {
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

	@Override
	public Match[] computeContainedMatches(AbstractTextSearchResult result, IEditorPart editor) {
		if (editor instanceof ITextEditor) {
			ScriptBase script = Utilities.getScriptForEditor((ITextEditor) editor);
			if (script != null)
				return result.getMatches(script);
		}
		return NO_MATCHES;
	}

	@Override
	public boolean isShownInEditor(Match match, IEditorPart editor) {
		if (editor instanceof ITextEditor) {
			ScriptBase script = Utilities.getScriptForEditor((ITextEditor)editor);
			if (script != null && match.getElement().equals(script.getScriptStorage()))
				return true;
		}
		return false;
	}

	@Override
	public Match[] computeContainedMatches(AbstractTextSearchResult result, IFile file) {
		ScriptBase script = ScriptBase.get(file, true);
		if (script != null)
			return result.getMatches(script);
		return NO_MATCHES;
	}

	@Override
	public IFile getFile(Object element) {
		if (element instanceof ScriptBase)
			return (IFile) ((ScriptBase)element).getScriptStorage();
		if (element instanceof IFile) {
			return (IFile)element;
		}
		return null;
	}
	
	public void addMatch(final Declaration declaration, final Declaration parentNode) {
		try {
			ClonkCore.getDefault().performActionsOnFileDocument(declaration.getScript().getScriptFile(), new IDocumentAction() {
				@Override
				public void run(IDocument document) {
					IRegion lineRegion = BufferedScanner.getLineRegion(document.get(), declaration.getLocation());
					String line = new BufferedScanner(document.get()).getSubstringOfBuffer(lineRegion);
					ClonkSearchMatch match = new ClonkSearchMatch(line, lineRegion.getOffset(), parentNode, declaration.getLocation().getOffset(), declaration.getLocation().getLength(), false, false);
					match.setCookie(declaration);
					addMatch(match);
				}
			});
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
	
	public void addMatch(ExprElm match, C4ScriptParser parser, boolean potential, boolean indirect, int s, int l) {
		IRegion lineRegion = parser.getLineRegion(parser.convertRelativeRegionToAbsolute(match));
		String line = parser.getSubstringOfBuffer(lineRegion);
		addMatch(new ClonkSearchMatch(line, lineRegion.getOffset(), parser.getContainer(), s, l, potential, indirect));
	}
	
	public void addMatch(ExprElm match, C4ScriptParser parser, boolean potential, boolean indirect) {
		addMatch(match, parser, potential, indirect, match.getIdentifierStart()+parser.bodyOffset(), match.getIdentifierLength());
	}

}