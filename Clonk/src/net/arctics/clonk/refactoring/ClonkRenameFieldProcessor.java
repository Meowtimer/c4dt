package net.arctics.clonk.refactoring;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.ProjectIndex;
import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.c4script.C4Function;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.c4script.FindDeclarationInfo;
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
import org.eclipse.ui.PlatformUI;

public class ClonkRenameFieldProcessor extends RenameProcessor {
	
	private C4Declaration decl;
	private String newName;

	public ClonkRenameFieldProcessor(C4Declaration field, String newName) {
		this.newName = newName;
		this.decl = field;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor monitor)
			throws CoreException, OperationCanceledException {
		// renaming fields that originate from outside the project is not allowed
		C4Declaration baseField = decl instanceof C4Function ? ((C4Function)decl).baseFunction() : decl;
		if (!(baseField.getScript().getIndex() instanceof ProjectIndex))
			return RefactoringStatus.createFatalErrorStatus(decl.getName() + " is either declared outside of the project or overrides a function that is declared outside of the project");
		
		FindDeclarationInfo info = new FindDeclarationInfo(decl.getScript().getIndex());
		info.setDeclarationClass(decl.getClass());
		C4Declaration existingDec = decl.getScript().findDeclaration(newName, info);
		if (existingDec != null) {
			return RefactoringStatus.createFatalErrorStatus("There is already an item with name " + newName + " in " + decl.getScript().toString());
		}
		
		return RefactoringStatus.createInfoStatus("Everything awesome");
	}

	@Override
	public Change createChange(IProgressMonitor monitor) throws CoreException,
			OperationCanceledException {
		Object script = decl.getScript().getScriptFile();
		if (!(script instanceof IResource))
			return null;
		IResource declaringFile = (IResource) script;
		ClonkSearchQuery query = new ClonkSearchQuery(decl, Utilities.getProject(declaringFile));
		query.run(monitor);
		ClonkSearchResult searchResult = (ClonkSearchResult) query.getSearchResult();
		// all references in code
		Set<Object> elements = new HashSet<Object>(Arrays.asList(searchResult.getElements()));
		// declaration of the selected field
		elements.add(decl.getScript());
		// if field is a function also look for functions which inherit or are inherited from field
		if (decl instanceof C4Function) {
			C4Function fieldAsFunc = (C4Function)decl;
			for (C4Function relatedFunc : decl.getScript().getIndex().declarationsWithName(decl.getName(), C4Function.class)) {
				if (decl != relatedFunc && fieldAsFunc.isRelatedFunction(relatedFunc) && fieldAsFunc.getScript().getScriptFile() instanceof IFile)
					elements.add(relatedFunc);
			}
		}
		CompositeChange composite = new CompositeChange("Renaming " + decl.toString());
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
				TextFileChange fileChange = new TextFileChange("Renaming " + decl.toString() + " in " + file.getFullPath().toString(), file);
				fileChange.setEdit(new MultiTextEdit());
				// change declaration
				if (file.equals(declaringFile)) {
					fileChange.addEdit(new ReplaceEdit(decl.getLocation().getOffset(), decl.getLocation().getLength(), newName));
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
		return new Object[] {decl};
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

	public C4Declaration getField() {
		return decl;
	}
	
}
