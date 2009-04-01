package net.arctics.clonk.refactoring;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.C4Field;
import net.arctics.clonk.parser.C4Function;
import net.arctics.clonk.parser.C4ScriptBase;
import net.arctics.clonk.parser.FindDeclarationInfo;
import net.arctics.clonk.parser.ProjectIndex;
import net.arctics.clonk.ui.search.ClonkSearchMatch;
import net.arctics.clonk.ui.search.ClonkSearchQuery;
import net.arctics.clonk.ui.search.ClonkSearchResult;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
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
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;

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
		// renaming fields that originate from outside the project is not allowed
		C4Field baseField = field instanceof C4Function ? ((C4Function)field).baseFunction() : field;
		if (!(baseField.getScript().getIndex() instanceof ProjectIndex))
			return RefactoringStatus.createFatalErrorStatus(field.getName() + " is either declared outside of the project or overrides a function that is declared outside of the project");
		
		FindDeclarationInfo info = new FindDeclarationInfo(field.getScript().getIndex());
		info.setFieldClass(field.getClass());
		C4Field existingField = field.getScript().findDeclaration(newName, info);
		if (existingField != null) {
			return RefactoringStatus.createFatalErrorStatus("There is already an item with name " + newName + " in " + field.getScript().toString());
		}
		
		return RefactoringStatus.createInfoStatus("Everything awesome");
	}

	@Override
	public Change createChange(IProgressMonitor monitor) throws CoreException,
			OperationCanceledException {
		Object script = field.getScript().getScriptFile();
		if (!(script instanceof IResource))
			return null;
		IResource declaringFile = (IResource) script;
		ClonkSearchQuery query = new ClonkSearchQuery(field, Utilities.getProject(declaringFile));
		query.run(monitor);
		ClonkSearchResult searchResult = (ClonkSearchResult) query.getSearchResult();
		// all references in code
		Set<Object> elements = new HashSet<Object>(Arrays.asList(searchResult.getElements()));
		// declaration of the selected field
		elements.add(field.getScript());
		// if field is a function also look for functions which inherit or are inherited from field
		if (field instanceof C4Function) {
			C4Function fieldAsFunc = (C4Function)field;
			for (C4Function relatedFunc : field.getScript().getIndex().fieldsWithName(field.getName(), C4Function.class)) {
				if (field != relatedFunc && fieldAsFunc.relatedFunction(relatedFunc) && fieldAsFunc.getScript().getScriptFile() instanceof IFile)
					elements.add(relatedFunc);
			}
		}
		CompositeChange composite = new CompositeChange("Renaming " + field.toString());
		for (Object element : elements) {
			IFile file;
			if (element instanceof IFile)
				file = (IFile)element;
			else if (element instanceof C4ScriptBase)
				file = (IFile) ((C4ScriptBase)element).getScriptFile();
			else if (element instanceof C4Function)
				file = (IFile) ((C4Function)element).getScript().getScriptFile();
			else
				file = null;
			if (file != null) {
				TextFileChange fileChange = new TextFileChange("Renaming " + field.toString() + " in " + file.getFullPath().toString(), file);
				fileChange.setEdit(new MultiTextEdit());
				// change declaration
				if (file.equals(declaringFile)) {
					fileChange.addEdit(new ReplaceEdit(field.getLocation().getOffset(), field.getLocation().getLength(), newName));
				}
				if (element instanceof C4Function) {
					C4Function relatedFunc = (C4Function)element;
					fileChange.addEdit(new ReplaceEdit(relatedFunc.getLocation().getOffset(), relatedFunc.getLocation().getLength(), newName));
				}
				for (Match m : searchResult.getMatches(element)) {
					ClonkSearchMatch match = (ClonkSearchMatch) m;
					if (!match.isPotential() && !match.isIndirect())
						fileChange.addEdit(new ReplaceEdit(match.getOffset(), match.getLength(), newName));
				}
				composite.add(fileChange);
			}
		}
		return composite;
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor monitor,
			CheckConditionsContext context) throws CoreException,
			OperationCanceledException {
		return RefactoringStatus.createInfoStatus("Everything still awesome");
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
