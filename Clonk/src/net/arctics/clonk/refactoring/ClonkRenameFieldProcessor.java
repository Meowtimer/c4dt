package net.arctics.clonk.refactoring;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.Utilities;
import net.arctics.clonk.parser.C4Field;
import net.arctics.clonk.ui.search.ClonkSearchQuery;
import net.arctics.clonk.ui.search.ClonkSearchResult;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RenameProcessor;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;
import org.eclipse.search.ui.text.Match;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.ui.texteditor.ITextEditor;

public class ClonkRenameFieldProcessor extends RenameProcessor {
	
	private C4Field field;
	private String newName;

	public ClonkRenameFieldProcessor(C4Field field, String newName) {
		this.newName = newName;
		this.field = field;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor monitor)
			throws CoreException, OperationCanceledException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Change createChange(IProgressMonitor monitor) throws CoreException,
			OperationCanceledException {
		ClonkSearchQuery query = new ClonkSearchQuery(field, Utilities.getProject((ITextEditor) field.getScript().getScriptFile()));
		query.run(monitor);
		ClonkSearchResult searchResult = (ClonkSearchResult) query.getSearchResult();
		Object[] elements = searchResult.getElements();
		CompositeChange composite = new CompositeChange("Renaming " + field.toString());
		for (Object element : elements) {
			if (element instanceof IFile) {
				IFile file = (IFile) element;
				TextFileChange fileChange = new TextFileChange("Renaming " + field.toString() + " in " + file.getFullPath().toString(), file);
				composite.add(fileChange);
				for (Match match : searchResult.getMatches(element)) {
					fileChange.addEdit(new ReplaceEdit(match.getOffset(), match.getLength(), newName));
				}
			}
		}
		return composite;
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor monitor,
			CheckConditionsContext context) throws CoreException,
			OperationCanceledException {
		return null;
	}

	@Override
	public Object[] getElements() {
		return new Object[] {field};
	}

	@Override
	public String getIdentifier() {
		return ClonkCore.PLUGIN_ID + ".refactoring.renameField";
	}

	@Override
	public String getProcessorName() {
		return "Clonk Rename Processor";
	}

	@Override
	public boolean isApplicable() throws CoreException {
		return true;
	}

	@Override
	public RefactoringParticipant[] loadParticipants(RefactoringStatus status,
			SharableParticipants sharableParticipants) throws CoreException {
		return null;
	}

	public C4Field getField() {
		return field;
	}
	
}
