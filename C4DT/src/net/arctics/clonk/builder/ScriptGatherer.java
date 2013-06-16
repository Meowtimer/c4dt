package net.arctics.clonk.builder;

import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.findMemberCaseInsensitively;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import net.arctics.clonk.ast.Structure;
import net.arctics.clonk.c4group.C4Group;
import net.arctics.clonk.c4group.C4Group.GroupType;
import net.arctics.clonk.c4script.MapScript;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.SystemScript;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.ID;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.Scenario;
import net.arctics.clonk.ini.DefCoreUnit;
import net.arctics.clonk.util.Sink;

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
	private final Set<Script> obsoleted = new HashSet<Script>();
	private final ClonkBuilder builder;

	public ScriptGatherer(ClonkBuilder clonkBuilder) { builder = clonkBuilder; }

	public void obsoleteCorrespondingScriptFromIndex(final IResource deleted, Index index) {
		index.allScripts(new Sink<Script>() {
			@Override
			public void receivedObject(Script item) {
				if (item instanceof Definition) {
					final Definition d = (Definition)item;
					if (d.definitionFolder() == null || !d.definitionFolder().equals(deleted))
						return;
				} else if (item.scriptFile() == null || !item.scriptFile().equals(deleted))
					return;
				obsoleted.add(item);
				decision(Decision.AbortIteration);
			}
		});
	}

	private Definition createDefinition(IContainer folder) {
		final IFile defCore = as(findMemberCaseInsensitively(folder, "DefCore.txt"), IFile.class); //$NON-NLS-1$
		final IFile scenario = defCore != null ? null : as(findMemberCaseInsensitively(folder, "Scenario.txt"), IFile.class); //$NON-NLS-1$
		if (defCore == null && scenario == null)
			return null;
		try {
			Definition def = Definition.definitionCorrespondingToFolder(folder);
			if (defCore != null) {
				final DefCoreUnit defCoreWrapper = (DefCoreUnit) Structure.pinned(defCore, true, false);
				if (def == null)
					def = new Definition(builder.index(), defCoreWrapper.definitionID(), defCoreWrapper.name(), folder);
				else {
					def.setId(defCoreWrapper.definitionID());
					def.setName(defCoreWrapper.name());
				}
			} else if (scenario != null)
				def = new Scenario(builder.index(), folder.getName(), folder);
			return def;
		} catch (final Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	@Override
	public boolean visit(IResourceDelta delta) throws CoreException {
		if (delta == null)
			return false;

		boolean visitChildren = false;
		If: if (delta.getResource() instanceof IFile) {
			final IFile file = (IFile) delta.getResource();
			Script script;
			switch (delta.getKind()) {
			case IResourceDelta.CHANGED: case IResourceDelta.ADDED:
				script = Script.get(file, false);
				if (script == null) {
					final SystemScript sy = makeSystemScript(delta.getResource());
					if (sy != null)
						script = sy;
					else
						script = createDefinition(delta.getResource().getParent());
				} else {
					if (file.getName().endsWith(".c"))
						script.setScriptFile(file); // ensure files match up
					else {
						final Structure s = Structure.pinned(file, true, true);
						if (script instanceof Definition && s instanceof DefCoreUnit) {
							final ID id = ((DefCoreUnit)s).definitionID();
							final Definition def = (Definition)script;
							if (id != null)
								def.setId(id);
						}
					}
					obsoleted.remove(script);
				}
				if (script != null && file.equals(script.scriptFile()))
					builder.queueScript(script);
				else
					processAuxiliaryFiles(file, script);
				break;
			case IResourceDelta.REMOVED:
				obsoleteCorrespondingScriptFromIndex(file, builder.index());
				break;
			}
			visitChildren = true;
		}
		else if (delta.getResource() instanceof IContainer) {
			final IContainer container = (IContainer)delta.getResource();
			if (!INDEX_C4GROUPS)
				if (EFS.getStore(delta.getResource().getLocationURI()) instanceof C4Group) {
					visitChildren = false;
					break If;
				}
			// make sure the object has a reference to its folder (not to some obsolete deleted one)
			Definition definition;
			switch (delta.getKind()) {
			case IResourceDelta.ADDED:
				definition = Definition.definitionCorrespondingToFolder(container);
				if (definition != null) {
					definition.setDefinitionFolder(container);
					obsoleted.remove(definition);
				}
				break;
			case IResourceDelta.REMOVED:
				obsoleteCorrespondingScriptFromIndex(container, builder.index());
				break;
			}
			visitChildren = true;
		}
		builder.monitor().worked(1);
		return visitChildren;
	}
	@Override
	public boolean visit(IResource resource) throws CoreException {
		if (builder.monitor().isCanceled())
			return false;
		if (resource instanceof IContainer) {
			if (!INDEX_C4GROUPS)
				if (EFS.getStore(resource.getLocationURI()) instanceof C4Group)
					return false;
			final Definition definition = createDefinition((IContainer) resource);
			if (definition != null)
				builder.queueScript(definition);
			return true;
		}
		else if (resource instanceof IFile) {
			final IFile file = (IFile) resource;
			final SystemScript sy = makeSystemScript(resource);
			if (sy != null) {
				builder.queueScript(sy);
				return true;
			}
			else if (processAuxiliaryFiles(file, Script.get(file, false)))
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
			final Definition def = (Definition)script;
			try {
				def.processDefinitionFolderFile(file);
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
		else
			result = false;
		return result;
	}
	public void removeObsoleteScripts() {
		for (final Script s : obsoleted)
			builder.index().removeScript(s);
		obsoleted.clear();
	}
	private SystemScript makeSystemScript(IResource resource) throws CoreException {
		final SystemScript pinned = SystemScript.pinned(resource, true);
		if (pinned != null)
			return pinned;
		if (resource instanceof IFile)
			if (
				resource.getName().toLowerCase().endsWith(".c") && //$NON-NLS-1$
				isSystemGroup(resource.getParent())
			)
				return new SystemScript(builder.index(), (IFile) resource);
			else if (
				resource.getName().toLowerCase().equals("map.c") && //$NON-NLS-1$
				Scenario.get(resource.getParent()) != null
			)
				return new MapScript(builder.index(), (IFile) resource);
		return null;
	}
	public boolean isMapScript(IResource resource) {
		return
			resource instanceof IFile &&
			resource.getName().equals("Map.c");
	}
	private boolean isSystemGroup(IContainer container) {
		return container.getName().equals(builder.index().engine().groupName("System", GroupType.ResourceGroup)); //$NON-NLS-1$
	}
}