package net.arctics.clonk.refactoring;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.ProjectIndex;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.ScriptBase;
import net.arctics.clonk.parser.c4script.FindDeclarationInfo;
import net.arctics.clonk.parser.inireader.IniUnit;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.ui.search.ClonkSearchMatch;
import net.arctics.clonk.ui.search.ClonkSearchQuery;
import net.arctics.clonk.ui.search.ClonkSearchResult;
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
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;

public class RenameDeclarationProcessor extends RenameProcessor {
	
	private Declaration decl;
	private String newName;

	public RenameDeclarationProcessor(Declaration field, String newName) {
		this.newName = newName;
		this.decl = field;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		// renaming fields that originate from outside the project is not allowed
		Declaration baseDecl = decl instanceof Function ? ((Function)decl).baseFunction() : decl;
		if (!(baseDecl.getIndex() instanceof ProjectIndex)) {
			return RefactoringStatus.createFatalErrorStatus(String.format(Messages.OutsideProject, decl.getName()));
		}
		
		Declaration existingDec;
		FindDeclarationInfo info = new FindDeclarationInfo(decl.getIndex());
		info.setDeclarationClass(decl.getClass());
		Structure parentStructure = decl.getParentDeclarationOfType(Structure.class);
		if (parentStructure != null) {
			existingDec = parentStructure.findLocalDeclaration(newName, decl.getClass());
			if (existingDec != null) {
				return RefactoringStatus.createFatalErrorStatus(String.format(Messages.DuplicateItem, newName, decl.getScript().toString()));
			}
		}
		
		return RefactoringStatus.createInfoStatus(Messages.Success);
	}

	@Override
	public Change createChange(IProgressMonitor monitor) throws CoreException,
			OperationCanceledException {
		Object script = decl.getScript().getScriptStorage();
		if (!(script instanceof IResource))
			return null;
		IResource declaringFile = (IResource) script;
		ClonkSearchQuery query = new ClonkSearchQuery(decl, ClonkProjectNature.get(declaringFile));
		query.run(monitor);
		ClonkSearchResult searchResult = (ClonkSearchResult) query.getSearchResult();
		// all references in code
		Set<Object> elements = new HashSet<Object>(Arrays.asList(searchResult.getElements()));
		// declaration of the selected declaration
		elements.add(decl.getScript());
		// if decl is a function also look for functions which inherit or are inherited from decl
		if (decl instanceof Function) {
			Function fieldAsFunc = (Function)decl;
			for (Function relatedFunc : decl.getIndex().declarationsWithName(decl.getName(), Function.class)) {
				if (decl != relatedFunc && fieldAsFunc.isRelatedFunction(relatedFunc) && fieldAsFunc.getScript().getScriptStorage() instanceof IFile)
					elements.add(relatedFunc);
			}
		}
		CompositeChange composite = new CompositeChange(String.format(Messages.RenamingProgress, decl.toString()));
		for (Object element : elements) {
			IFile file;
			if (element instanceof IFile)
				file = (IFile)element;
			else if (element instanceof ScriptBase)
				file = (IFile) ((ScriptBase)element).getScriptStorage();
			else if (element instanceof Function)
				file = (IFile) ((Function)element).getScript().getScriptStorage();
			else if (element instanceof IniUnit)
				file = ((IniUnit)element).getIniFile();
			else
				file = null;
			if (file != null) {
				TextFileChange fileChange = new TextFileChange(String.format(Messages.RenameChangeDescription, decl.toString(), file.getFullPath().toString()), file);
				fileChange.setEdit(new MultiTextEdit());
				// change declaration
				if (file.equals(declaringFile)) {
					fileChange.addEdit(new ReplaceEdit(decl.getLocation().getOffset(), decl.getLocation().getLength(), newName));
				}
//				else if (element instanceof C4Function) {
//					C4Function relatedFunc = (C4Function)element;
//					fileChange.addEdit(new ReplaceEdit(relatedFunc.getLocation().getOffset(), relatedFunc.getLocation().getLength(), newName));
//				}
				for (Match m : searchResult.getMatches(element)) {
					ClonkSearchMatch match = (ClonkSearchMatch) m;
					if (!match.isPotential() && !match.isIndirect()) try {
						fileChange.addEdit(new ReplaceEdit(match.getOffset(), match.getLength(), newName));
					} catch (MalformedTreeException e) {
						// gonna ignore that; there is one case where it's even normal this is thrown (for (e in a) ... <- e is reference and declaration)
					}
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
		return RefactoringStatus.createInfoStatus(Messages.Success);
	}

	@Override
	public Object[] getElements() {
		return new Object[] {decl};
	}

	@Override
	public String getIdentifier() {
		return ClonkCore.id("refactoring.renameDeclaration"); //$NON-NLS-1$
	}

	@Override
	public String getProcessorName() {
		return Messages.RenameProcessorName;
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

	public Declaration getField() {
		return decl;
	}
	
}
