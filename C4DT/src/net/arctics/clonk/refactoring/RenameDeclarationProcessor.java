package net.arctics.clonk.refactoring;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.arctics.clonk.Core;
import net.arctics.clonk.c4script.FindDeclarationInfo;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.ProjectIndex;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.parser.inireader.DefCoreUnit;
import net.arctics.clonk.parser.inireader.IniEntry;
import net.arctics.clonk.parser.inireader.IniUnit;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.ui.search.SearchMatch;
import net.arctics.clonk.ui.search.SearchResult;
import net.arctics.clonk.ui.search.ReferencesSearchQuery;

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

	/**
	 * Option: The processor won't attempt to change the id value inside DefCore.txt if the declaration being rename is a {@link Definition}
	 */
	public static final int CONSIDER_DEFCORE_ID_ALREADY_CHANGED = 1;

	private final Declaration decl;
	private String newName;
	private final String oldName;

	private final int options;

	/**
	 * Create a new RenameDeclarationProcessor.
	 * @param declarationToRename The declaration to rename
	 * @param newName The new name (default value shown in the renaming UI)
	 * @param options Mask |-red together from {@link #CONSIDER_DEFCORE_ID_ALREADY_CHANGED}
	 */
	public RenameDeclarationProcessor(Declaration declarationToRename, String newName, int options) {
		this.newName = newName;
		this.oldName = declarationToRename.name();
		this.decl = declarationToRename;
		this.options = options;
	}

	@Override
	public Change createChange(IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		Object script = decl.script().source();
		if (!(script instanceof IResource))
			return null;
		IResource declaringFile = (IResource) script;
		ReferencesSearchQuery query = new ReferencesSearchQuery(decl, ClonkProjectNature.get(declaringFile));
		query.run(monitor);
		SearchResult searchResult = (SearchResult) query.getSearchResult();
		// all references in code
		Set<Object> elements = new HashSet<Object>(Arrays.asList(searchResult.getElements()));
		// declaration location
		elements.add(decl.script());
		// if decl is a function also look for functions which inherit or are inherited from decl
		if (decl instanceof Function) {
			Function fieldAsFunc = (Function)decl;
			for (Function relatedFunc : decl.index().declarationsWithName(decl.name(), Function.class))
				if (decl != relatedFunc && fieldAsFunc.isRelatedFunction(relatedFunc) && fieldAsFunc.script().source() instanceof IFile)
					elements.add(relatedFunc);
		}
		CompositeChange composite = new CompositeChange(String.format(Messages.RenamingProgress, decl.toString()));
		// now that references by the old name have been detected, rename the declaration (in case of Definition.ProxyVar, this will change the id of the definition being proxied)
		//composite.add(new SetNameChange("Setting the declaration's name", decl, newName));
		for (Object element : elements) {
			IFile file;
			if (element instanceof IFile)
				file = (IFile)element;
			else if (element instanceof Script)
				file = (IFile) ((Script)element).source();
			else if (element instanceof Function)
				file = (IFile) ((Function)element).script().source();
			else if (element instanceof IniUnit)
				file = ((IniUnit)element).file();
			else
				file = null;
			if (file != null) {
				TextFileChange fileChange = new TextFileChange(String.format(Messages.RenameChangeDescription, decl.toString(), file.getFullPath().toString()), file);
				fileChange.setEdit(new MultiTextEdit());
				// change declaration
				if (file.equals(declaringFile))
					if (decl instanceof Definition.ProxyVar) {
						if ((options & CONSIDER_DEFCORE_ID_ALREADY_CHANGED) == 0) {
							Definition def = ((Definition.ProxyVar)decl).definition();
							DefCoreUnit unit = (DefCoreUnit) Structure.pinned(def.defCoreFile(), true, false);
							if (unit != null)
								try {
									IniEntry entry = (IniEntry) unit.sectionWithName("DefCore", false).subItemByKey("id");
									TextFileChange defCoreChange = new TextFileChange(String.format("Change id in DefCore.txt of %s", decl.toString()), def.defCoreFile());
									defCoreChange.setEdit(new ReplaceEdit(entry.end()-entry.stringValue().length(), entry.stringValue().length(), newName));
									composite.add(defCoreChange);
								} catch (Exception e) {
									e.printStackTrace();
								}
						}
					} else {
						int nameStart = decl.nameStart();
						fileChange.addEdit(new ReplaceEdit(nameStart, decl.name().length(), newName));
					}
//				else if (element instanceof C4Function) {
//					C4Function relatedFunc = (C4Function)element;
//					fileChange.addEdit(new ReplaceEdit(relatedFunc.getLocation().getOffset(), relatedFunc.getLocation().getLength(), newName));
//				}
				for (Match m : searchResult.getMatches(element)) {
					SearchMatch match = (SearchMatch) m;
					try {
						fileChange.addEdit(new ReplaceEdit(match.getOffset(), match.getLength(), newName));
					} catch (MalformedTreeException e) {
						// gonna ignore that; there is one case where it's even normal this is thrown (for (e in a) ... <- e is reference and declaration)
					}
				}
				if (fileChange.getEdit().getChildrenSize() > 0)
					composite.add(fileChange);
			}
		}
		return composite;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		return new RefactoringStatus();
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor monitor,
			CheckConditionsContext context) throws CoreException,
			OperationCanceledException {
		// renaming fields that originate from outside the project is not allowed
		Declaration baseDecl = decl instanceof Function ? ((Function)decl).baseFunction() : decl;
		if (!(baseDecl.index() instanceof ProjectIndex))
			return RefactoringStatus.createFatalErrorStatus(String.format(Messages.OutsideProject, decl.name()));

		Declaration existingDec;
		FindDeclarationInfo info = new FindDeclarationInfo(decl.index());
		info.declarationClass = decl.getClass();
		Structure parentStructure = decl.parentOfType(Structure.class);
		if (parentStructure != null) {
			existingDec = parentStructure.findLocalDeclaration(newName, decl.getClass());
			if (existingDec != null)
				return RefactoringStatus.createFatalErrorStatus(String.format(Messages.DuplicateItem, newName, decl.script().toString()));
		}

		return new RefactoringStatus();
	}

	@Override
	public Object[] getElements() {
		return new Object[] {decl};
	}

	@Override
	public String getIdentifier() {
		return Core.id("refactoring.renameDeclaration"); //$NON-NLS-1$
	}

	@Override
	public String getProcessorName() {
		return String.format(Messages.RenameProcessorName, oldName, newName);
	}

	@Override
	public boolean isApplicable() throws CoreException {
		return true;
	}

	@Override
	public RefactoringParticipant[] loadParticipants(RefactoringStatus status, SharableParticipants sharableParticipants) throws CoreException {
		return null;
	}

	public Declaration declarationBeingRenamed() {
		return decl;
	}

	public void setNewName(String newName) {
		this.newName = newName;
	}

	public String getNewName() {
		return newName;
	}

}
