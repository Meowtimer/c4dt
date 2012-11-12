package net.arctics.clonk.resource;

import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.findMemberCaseInsensitively;

import java.io.IOException;

import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Scenario;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.SystemScript;
import net.arctics.clonk.parser.inireader.DefCoreUnit;
import net.arctics.clonk.resource.c4group.C4Group;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;

public class ScriptGatherer implements IResourceDeltaVisitor, IResourceVisitor {
	
	private static final boolean INDEX_C4GROUPS = true;
	
	private final ClonkBuilder builder;
	ScriptGatherer(ClonkBuilder clonkBuilder) {
		builder = clonkBuilder;
	}
	public Definition createDefinition(IContainer folder) {
		IFile defCore = as(findMemberCaseInsensitively(folder, "DefCore.txt"), IFile.class); //$NON-NLS-1$
		IFile scenario = defCore != null ? null : as(findMemberCaseInsensitively(folder, "Scenario.txt"), IFile.class); //$NON-NLS-1$
		if (defCore == null && scenario == null)
			return null;
		try {
			Definition def = Definition.definitionCorrespondingToFolder(folder);
			if (defCore != null) {
				DefCoreUnit defCoreWrapper = (DefCoreUnit) Structure.pinned(defCore, true, false);
				if (def == null)
					def = new Definition(builder.index(), defCoreWrapper.definitionID(), defCoreWrapper.name(), folder);
				else {
					def.setId(defCoreWrapper.definitionID());
					def.setName(defCoreWrapper.name(), false);
				}
			} else if (scenario != null)
				def = new Scenario(builder.index(), folder.getName(), folder);
			return def;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	@Override
	public boolean visit(IResourceDelta delta) throws CoreException {
		if (delta == null) 
			return false;

		boolean success = false;
		If: if (delta.getResource() instanceof IFile) {
			IFile file = (IFile) delta.getResource();
			Script script;
			switch (delta.getKind()) {
			case IResourceDelta.CHANGED: case IResourceDelta.ADDED:
				script = Script.get(file, false);
				if (script == null)
					// create if new file
					// script in a system group
					if (builder.isSystemScript(delta.getResource())) //$NON-NLS-1$ //$NON-NLS-2$
						script = new SystemScript(builder.index(), file);
				// object script
					else
						script = createDefinition(delta.getResource().getParent());
				if (script != null && delta.getResource().equals(script.scriptFile()))
					builder.queueScript(script);
				else
					processAuxiliaryFiles(file, script);
				break;
			case IResourceDelta.REMOVED:
				script = SystemScript.scriptCorrespondingTo(file);
				if (script != null && file.equals(script.scriptStorage()))
					script.index().removeScript(script);
			}
			success = true;
		}
		else if (delta.getResource() instanceof IContainer) {
			IContainer container = (IContainer)delta.getResource();
			if (!INDEX_C4GROUPS)
				if (EFS.getStore(delta.getResource().getLocationURI()) instanceof C4Group) {
					success = false;
					break If;
				}
			// make sure the object has a reference to its folder (not to some obsolete deleted one)
			Definition definition;
			switch (delta.getKind()) {
			case IResourceDelta.ADDED:
				definition = Definition.definitionCorrespondingToFolder(container);
				if (definition != null)
					definition.setDefinitionFolder(container);
				//					else if (isSystemGroup(container))
				//						for (IResource res : container.members())
				//							if (isSystemScript(res))
				//								queueScript(new SystemScript(index(), (IFile)res));
				break;
			case IResourceDelta.REMOVED:
				// remove object when folder is removed
				definition = Definition.definitionCorrespondingToFolder(container);
				if (definition != null)
					definition.index().removeDefinition(definition);
				break;
			}
			success = true;
		}
		builder.monitor().worked(1);
		return success;
	}
	@Override
	public boolean visit(IResource resource) throws CoreException {
		if (builder.monitor().isCanceled())
			return false;
		if (resource instanceof IContainer) {
			if (!INDEX_C4GROUPS)
				if (EFS.getStore(resource.getLocationURI()) instanceof C4Group)
					return false;
			Definition definition = createDefinition((IContainer) resource);
			if (definition != null)
				builder.queueScript(definition);
			return true;
		}
		else if (resource instanceof IFile) {
			IFile file = (IFile) resource;
			// only create standalone-scripts for *.c files residing in System groups
			if (builder.isSystemScript(resource)) {
				Script script = SystemScript.pinned(file, true);
				if (script == null)
					script = new SystemScript(builder.index(), file);
				builder.queueScript(script);
				return true;
			}
			else if (processAuxiliaryFiles(file, Script.get(file, true)))
				return true;
		}
		return false;
	}
	private boolean processAuxiliaryFiles(IFile file, Script script) throws CoreException {
		boolean result = true;
		Structure structure;
		if ((structure = Structure.createStructureForFile(file, true)) != null) {
			structure.commitTo(script, builder);
			structure.pinTo(file);
		}
		else
			structure = Structure.pinned(file, false, true);
		if (structure != null)
			builder.addGatheredStructure(structure);
		// not parsed as Structure - let definition process the file
		else if (script instanceof Definition) {
			Definition def = (Definition)script;
			try {
				def.processDefinitionFolderFile(file);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else
			result = false;
		return result;
	}
}