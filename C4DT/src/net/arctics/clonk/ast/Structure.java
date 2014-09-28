package net.arctics.clonk.ast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.Core;
import net.arctics.clonk.ProblemException;
import net.arctics.clonk.builder.ClonkBuilder;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.ast.FunctionBody;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.parser.Markers;
import net.arctics.clonk.ui.editors.c4script.ScriptWithStorageEditorInput;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.part.FileEditorInput;

/**
 * Declaration that contains sub declarations and describes more complex structures (like DefCores and scripts).
 * Provides support for being pinned to files in the project tree using {@link IResource} session properties.
 */
public abstract class Structure extends Declaration implements ILatestDeclarationVersionProvider, IASTPositionProvider {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	/**
	 * Finds a declaration inside this structure
	 * @param declarationName the name of the declaration
	 * @param declarationClass the class of the declaration
	 * @return the declaration or null if it couldn't be found
	 */
	public Declaration findDeclaration(final String declarationName, final Class<? extends Declaration> declarationClass) {
		return findLocalDeclaration(declarationName, declarationClass);
	}

	/**
	 * Finds a declaration without requiring a specific class
	 * @param declarationName the name of the declaration
	 * @return the declaration or null if it couldn't be found
	 */
	public Declaration findDeclaration(final String declarationName) {
		return findDeclaration(declarationName, Declaration.class);
	}

	public abstract Declaration findLocalDeclaration(String declarationName, Class<? extends Declaration> declarationClass);

	/**
	 * Returns an editor input for this structure
	 * @return the editor input
	 */
	public IEditorInput makeEditorInput() {
		final Object storage = script() != null ? script().source() : resource();
		if (storage instanceof IFile)
			return new FileEditorInput((IFile) storage);
		if (storage instanceof IStorage && this instanceof Script)
			return new ScriptWithStorageEditorInput((Script) this);
		return null;
	}

	/**
	 * Returns whether this structure is editable
	 * @return
	 */
	public boolean isEditable() {
		return true;
	}

	/**
	 * Pins this structure to a file (should be the file the structure was read from)
	 * @param resource the file
	 * @throws CoreException
	 */
	public void pinTo(final IResource resource) {
		try {
			resource.setSessionProperty(Core.FILE_STRUCTURE_REFERENCE_ID, this);
		} catch (final CoreException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns the structure pinned to a file creating it if force is true and the structure does not already exist
	 * @param file the file to return the pinned structure of
	 * @param force whether to create the structure if it does not yet exist
	 * @return the structure or null if something went wrong or file is null as well. In other situations an exception will be thrown.
	 * @throws CoreException
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Structure> T pinned(final IResource file, final boolean force, final boolean duringBuild) {
		if (file == null)
			return null;
		T result;
		try {
			result = (T) file.getSessionProperty(Core.FILE_STRUCTURE_REFERENCE_ID);
			if (result != null)
				result.setFile((IFile)file);
			else if (force) {
				result = (T) createStructureForFile(file, duringBuild);
				if (result != null)
					result.pinTo(file);
			}
			return result;
		} catch (final CoreException e) {
			return null;
		}
	}

	public void setFile(final IFile file) {
		// i'll do that
	}

	/**
	 * Remove the structure pinned to the given file
	 * @param file the file to remove the reference from
	 * @return the previously pinned structure or null if there was none
	 * @throws CoreException
	 */
	public static Structure unPinFrom(final IFile file) {
		final Structure pinned = pinned(file, false, false);
		if (pinned != null)
			try {
				file.setSessionProperty(Core.FILE_STRUCTURE_REFERENCE_ID, null);
			} catch (final CoreException e) {
				e.printStackTrace();
			}
		return pinned;
	}

	/**
	 * factory for creating structures
	 */
	public interface IStructureFactory {
		/**
		 * Create a structure for the given resource.
		 * @param resource The resource to create a Structure for
		 * @param duringBuild Flag to indicate whether the creation request happens during a {@link ClonkBuilder} run.
		 * @return The created structure or null if the factory decides to not be responsible for this kind of file.
		 */
		public Structure create(IResource resource, boolean duringBuild);
	}

	/**
	 * Registered structure factories that are queried if a structure for a file is to be created
	 */
	private static Collection<IStructureFactory> structureFactories = new LinkedList<IStructureFactory>();

	/**
	 * Registers a new structure factory
	 * @param factory the factory
	 */
	public static void registerStructureFactory(final IStructureFactory factory) {
		structureFactories.add(factory);
	}

	/**
	 * Creates a structure for a file. This is achieved by querying registered structure factories
	 * @param file file
	 * @return the newly created structure or null if no suitable factory could be found
	 */
	public static Structure createStructureForFile(final IResource file, final boolean duringBuild) {
		for (final IStructureFactory factory : structureFactories)
			try {
				final Structure result = factory.create(file, duringBuild);
				if (result != null)
					return result;
			} catch (final Exception e) {
				Core.instance().getLog().log(new Status(IStatus.ERROR, Core.PLUGIN_ID,
					String.format("Some Structure factory caused an exception while operating on '%s'", file.getProjectRelativePath().toOSString()), e)
				);
			}
		return null;
	}

	/**
	 * Commits data of this structure to the script. Primarily for {@link Definition}s which are defined by various files in addition to the script file.
	 * @param script the script to commit to
	 * @param builder Builders gonna build
	 */
	public void commitTo(final Script script, final ClonkBuilder builder) {
		// placeholder
	}

	/**
	 * Returns whether changing a structure (by editing the corresponding file) causes a reparsing of the associated script if there is one.
	 * @return Whether a script reparse is required or not.
	 */
	public boolean requiresScriptReparse() {
		return false;
	}

	/**
	 * Called by the {@link ClonkBuilder} in phase 2 to give Structure files a chance to complain about things like missing functions (which ought to have been created as of now since the {@link ClonkBuilder} is in phase 2)
	 * @param markers
	 * @throws ProblemException
	 */
	public void validate(final Markers markers) throws ProblemException {}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends Declaration> T latestVersionOf(final T from) {
		return (T) findLocalDeclaration(from.name(), from.getClass());
	}

	/**
	 * Find a declaration by its path as returned by {@link #pathRelativeToIndexEntity()}.
	 * @param path The path to find the {@link Declaration} of
	 * @return The {@link Declaration} or null if not found.
	 */
	public Declaration findDeclarationByPath(final String path, final Class<? extends Declaration> cls) {
		final String[] parts = path.split("\\.");
		Declaration d = this;
		for (final String p : parts) {
			if (!(d instanceof Structure))
				return null;
			final Declaration n = ((Structure)d).findLocalDeclaration(p, cls);
			if (n == null)
				return null;
			d = n;
		}
		return d;
	}

	@Override
	public IFile file() {
		final Structure p = parent(Structure.class);
		return p != null ? p.file() : null;
	}

	@Override
	public Declaration container() { return this; }
	@Override
	public int fragmentOffset() { return 0; }

	/**
	 * Write text representations of the specified nodes into the underlying document.
	 * The nodes are expected to still have source locations corresponding to the old nodes which are ostensibly being replaced by them
	 * so the document will not end up being garbled garbage.
	 * @param nodes The nodes to save.
	 */
	public void saveNodes(final Collection<? extends ASTNode> nodes) {
		if (nodes.isEmpty())
			return;
		Core.instance().performActionsOnFileDocument(file(), document -> {
			try {
				final List<ASTNode> l = new ArrayList<ASTNode>(nodes);
				Collections.sort(l, (o1, o2) -> {
					final IRegion r1 = o1.absolute();
					final IRegion r2 = o2.absolute();
					return r2.getOffset() - r1.getOffset();
				});
				for (final ASTNode e1 : l) {
					final IRegion region = e1.absolute();
					int depth;
					ASTNode n;
					for (depth = 0, n = e1; n != null && !(n instanceof Declaration || n instanceof FunctionBody); depth++, n = n.parent());
					document.replace(region.getOffset(), region.getLength(), e1.printed(depth));
				}
				return true;
			} catch (final BadLocationException e2) {
				e2.printStackTrace();
				return false;
			}
		}, true);
	}

}
