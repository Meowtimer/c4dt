package net.arctics.clonk.refactoring;

import static net.arctics.clonk.util.ArrayUtil.map;
import static net.arctics.clonk.util.StringUtil.rawFileName;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.Structure;
import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.c4group.C4Group.GroupType;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.ast.CallInherited;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.ProjectIndex;
import net.arctics.clonk.ui.search.ReferencesSearchQuery;
import net.arctics.clonk.ui.search.SearchMatch;
import net.arctics.clonk.ui.search.SearchResult;
import net.arctics.clonk.util.IConverter;
import net.arctics.clonk.util.StreamUtil;

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
import org.eclipse.ltk.core.refactoring.resource.RenameResourceChange;
import org.eclipse.search.ui.text.Match;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;

public class RenameDeclarationProcessor extends RenameProcessor {

	private final Declaration decl;
	private String newName;
	private final String oldName;

	/**
	 * Create a new RenameDeclarationProcessor.
	 * @param declarationToRename The declaration to rename
	 * @param newName The new name (default value shown in the renaming UI)
	 */
	public RenameDeclarationProcessor(final Declaration declarationToRename, final String newName) {
		this.newName = newName;
		this.oldName = declarationToRename.name();
		this.decl = declarationToRename;
	}

	@Override
	public Change createChange(final IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		final IFile declaringFile = decl.file();
		final ReferencesSearchQuery query = new ReferencesSearchQuery(ClonkProjectNature.get(declaringFile), decl);
		query.run(monitor);
		final SearchResult searchResult = (SearchResult) query.getSearchResult();
		// all references in code
		final Set<Object> elements = new HashSet<Object>(Arrays.asList(searchResult.getElements()));
		// if decl is a function also look for functions which inherit or are inherited from decl
		if (decl instanceof Function) {
			final Function fieldAsFunc = (Function)decl;
			for (final Function relatedFunc : decl.index().declarationsWithName(decl.name(), Function.class))
				if (decl != relatedFunc && fieldAsFunc.isRelatedFunction(relatedFunc) && fieldAsFunc.script().source() instanceof IFile)
					elements.add(relatedFunc);
		}
		final Map<IFile, Object> reverseLookup = new HashMap<>();
		final Set<IFile> files = new HashSet<IFile>(Arrays.asList(map(elements.toArray(), IFile.class, new IConverter<Object, IFile>() {
			@Override
			public IFile convert(final Object element) {
				IFile file;
				if (element instanceof IFile)
					file = (IFile)element;
				else if (element instanceof Declaration)
					file = ((Declaration)element).file();
				else
					file = null;
				if (file != null)
					reverseLookup.put(file, element);
				return file;
			}
		})));
		files.add(declaringFile);
		final CompositeChange composite = new CompositeChange(String.format(Messages.RenamingProgress, decl.toString()));
		for (final IFile file : files)
			if (file != null) {
				final TextFileChange fileChange = new TextFileChange(String.format(Messages.RenameChangeDescription, decl.toString(), file.getFullPath().toString()), file);
				fileChange.setEdit(new MultiTextEdit());
				// change declaration
				if (file.equals(declaringFile))
					if (decl instanceof Definition) {
						final Definition def = (Definition)decl;
						if (def.definitionFolder() != null && rawFileName(def.definitionFolder().getName()).equals(oldName)) {
							final RenameResourceChange renameFolder = new RenameResourceChange(
								def.definitionFolder().getFullPath(),
								def.engine().groupName(newName, GroupType.DefinitionGroup)
							);
							composite.add(renameFolder);
						}
					} else {
						final int nameStart = decl.nameStart();
						fileChange.addEdit(new ReplaceEdit(nameStart, decl.name().length(), newName));
					}
				for (final Match m : searchResult.getMatches(reverseLookup.get(file))) {
					final SearchMatch match = (SearchMatch) m;
					try {
						if (!(match.node() instanceof CallInherited)) {
							final String s = StreamUtil.stringFromFile(file);
							if (match.getOffset() < 0 || match.getOffset()+match.getLength() >= s.length())
								throw new IllegalStateException();
							fileChange.addEdit(new ReplaceEdit(match.getOffset(), match.getLength(), newName));
						}
					} catch (final MalformedTreeException e) {
						// gonna ignore that; there is one case where it's even normal this is thrown (for (e in a) ... <- e is reference and declaration)
					}
				}
				if (fileChange.getEdit().getChildrenSize() > 0)
					composite.add(fileChange);
			}
		return composite;
	}

	@Override
	public RefactoringStatus checkInitialConditions(final IProgressMonitor pm) throws CoreException, OperationCanceledException {
		return new RefactoringStatus();
	}

	@Override
	public RefactoringStatus checkFinalConditions(final IProgressMonitor monitor,
			final CheckConditionsContext context) throws CoreException,
			OperationCanceledException {
		// renaming fields that originate from outside the project is not allowed
		final Declaration baseDecl = decl instanceof Function ? ((Function)decl).baseFunction() : decl;
		if (!(baseDecl.index() instanceof ProjectIndex))
			return RefactoringStatus.createFatalErrorStatus(String.format(Messages.OutsideProject, decl.name()));

		Declaration existingDec;
		final Structure parentStructure = decl.parent(Structure.class);
		if (parentStructure != null) {
			existingDec = parentStructure.findLocalDeclaration(newName, decl.getClass());
			if (existingDec != null)
				return RefactoringStatus.createFatalErrorStatus(String.format(Messages.DuplicateItem, newName, decl.parent(Structure.class).toString()));
		}

		return new RefactoringStatus();
	}

	@Override
	public RefactoringParticipant[] loadParticipants(
		final RefactoringStatus status,
		final SharableParticipants sharableParticipants
	) throws CoreException { return null; }
	@Override
	public String getIdentifier() { return Core.id("refactoring.renameDeclaration"); } //$NON-NLS-1$
	@Override
	public Object[] getElements() { return new Object[] {decl}; }
	@Override
	public String getProcessorName() { return String.format(Messages.RenameProcessorName, oldName, newName); }
	@Override
	public boolean isApplicable() throws CoreException { return true; }
	public Declaration declarationBeingRenamed() { return decl; }
	public void setNewName(final String newName) { this.newName = newName; }
	public String newName() { return newName; }

}
