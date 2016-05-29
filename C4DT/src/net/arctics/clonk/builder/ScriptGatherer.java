package net.arctics.clonk.builder;

import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.attempt;
import static net.arctics.clonk.util.Utilities.block;
import static net.arctics.clonk.util.Utilities.findMemberCaseInsensitively;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;

import net.arctics.clonk.ProblemException;
import net.arctics.clonk.ast.Structure;
import net.arctics.clonk.c4group.C4Group;
import net.arctics.clonk.c4group.FileExtension;
import net.arctics.clonk.c4script.LocalizedScript;
import net.arctics.clonk.c4script.MapScript;
import net.arctics.clonk.c4script.ObjectsScript;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.SystemScript;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.ID;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.Scenario;
import net.arctics.clonk.ini.DefCoreUnit;
import net.arctics.clonk.ini.IniUnit;
import net.arctics.clonk.ini.IniUnitParser;
import net.arctics.clonk.util.Sink;

public class ScriptGatherer implements IResourceDeltaVisitor, IResourceVisitor {

	private static final boolean INDEX_C4GROUPS = true;
	private final Set<Script> obsoleted = new HashSet<Script>();
	private final ClonkBuilder builder;
	
	public ScriptGatherer(final ClonkBuilder clonkBuilder) { builder = clonkBuilder; }
	
	public void obsoleteCorrespondingScriptFromIndex(final IResource deleted, final Index index) {
		index.allScripts(new Sink<Script>() {
			@Override
			public void receive(final Script item) {}
			@Override
			public Decision decide(Script item) {
				if (item instanceof Definition) {
					final Definition d = (Definition)item;
					if (d.definitionFolder() == null || !d.definitionFolder().equals(deleted)) {
						return Decision.Continue;
					}
				} else if (item.file() == null || !item.file().equals(deleted)) {
					return Decision.Continue;
				}
				obsoleted.add(item);
				return Decision.AbortIteration;
			}
		});
	}
	
	private Definition createDefinition(final IContainer folder) {
		final IFile defCore = as(findMemberCaseInsensitively(folder, "DefCore.txt"), IFile.class); //$NON-NLS-1$
		final IFile scenario = as(findMemberCaseInsensitively(folder, "Scenario.txt"), IFile.class); //$NON-NLS-1$
		final IFile script = Script.findScriptFile(folder);
		if (defCore == null && scenario == null && script == null) {
			return null;
		}
		try {
			Definition def = Definition.at(folder);
			if (defCore != null) {
				final DefCoreUnit defCoreWrapper = (DefCoreUnit) Structure.pinned(defCore, true, false);
				if (def == null) {
					def = new Definition(builder.index(), defCoreWrapper.definitionID(), defCoreWrapper.name(), folder);
				} else {
					def.setId(defCoreWrapper.definitionID());
					def.setName(defCoreWrapper.name());
				}
			} else if (scenario != null) {
				if (def == null) {
					def = new Scenario(builder.index(), folder.getName(), folder);
				}
			}
			else if (script != null && builder.index().engine().extensionForFileName(folder.getName()) == FileExtension.DefinitionGroup) {
				if (def == null) {
					def = new Definition(builder.index(), ID.get(folder.getName()), folder.getName(), folder);
				}
			}
			return def;
		} catch (final Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public boolean visit(final IResourceDelta delta) {
		if (delta == null) {
			return false;
		}

		boolean visitChildren = false;
		If: if (delta.getResource() instanceof IFile) {
			final IFile file = (IFile) delta.getResource();
			switch (delta.getKind()) {
			case IResourceDelta.CHANGED: case IResourceDelta.ADDED:
				final Script existing = Script.get(file, false);
				final Script script = existing == null
					? block(() -> {
						final SystemScript sy = attempt(() -> makeSystemScript(delta.getResource()), CoreException.class, Exception::printStackTrace);
						return sy != null ? sy : createDefinition(delta.getResource().getParent());
					}) : block(() -> {
						if (Script.looksLikeScriptFile(file.getName())) {
							existing.setScriptFile(file); // ensure files match up
						} else {
							final Structure s = Structure.pinned(file, true, true);
							if (existing instanceof Definition && s instanceof DefCoreUnit) {
								// reparse for good measure
								try {
									new IniUnitParser((IniUnit) s).parse(false);
								} catch (final ProblemException e) {
									e.printStackTrace();
								}
								final ID id = ((DefCoreUnit)s).definitionID();
								final Definition def = (Definition)existing;
								if (id != null) {
									def.setId(id);
								}
							}
						}
						obsoleted.remove(existing);
						return existing;
					});
				if (script != null && file.equals(script.file())) {
					builder.queueScript(script);
				} else {
					try {
						processAuxiliaryFiles(file, script);
					} catch (final CoreException e) {
						e.printStackTrace();
					}
				}
				break;
			case IResourceDelta.REMOVED:
				obsoleteCorrespondingScriptFromIndex(file, builder.index());
				break;
			}
			visitChildren = true;
		}
		else if (delta.getResource() instanceof IContainer) {
			final IContainer container = (IContainer)delta.getResource();
			if (!INDEX_C4GROUPS) {
				try {
					if (EFS.getStore(delta.getResource().getLocationURI()) instanceof C4Group) {
						visitChildren = false;
						break If;
					}
				} catch (final CoreException e) {
					e.printStackTrace();
				}
			}
			// make sure the object has a reference to its folder (not to some obsolete deleted one)
			switch (delta.getKind()) {
			case IResourceDelta.ADDED:
				final Definition definition = Definition.at(container);
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
	public boolean visit(final IResource resource) throws CoreException {
		if (builder.monitor().isCanceled()) {
			return false;
		}
		if (resource instanceof IContainer) {
			if (!INDEX_C4GROUPS) {
				if (EFS.getStore(resource.getLocationURI()) instanceof C4Group) {
					return false;
				}
			}
			final Definition definition = createDefinition((IContainer) resource);
			if (definition != null) {
				builder.queueScript(definition);
			}
			return true;
		}
		else if (resource instanceof IFile) {
			final IFile file = (IFile) resource;
			final SystemScript sy = makeSystemScript(resource);
			if (sy != null) {
				builder.queueScript(sy);
				return true;
			}
			else if (processAuxiliaryFiles(file, Script.get(file, false))) {
				return true;
			}
		}
		return false;
	}
	
	private boolean processAuxiliaryFiles(final IFile file, final Script script) throws CoreException {
		boolean result = true;
		Structure structure;
		if ((structure = Structure.createStructureForFile(file, true)) != null) {
			structure.commitTo(script, builder);
			structure.pinTo(file);
		} else {
			structure = Structure.pinned(file, false, true);
		}
		if (structure != null) {
			builder.addGatheredStructure(structure);
		} else if (script instanceof Definition) {
			final Definition def = (Definition)script;
			try {
				def.processDefinitionFolderFile(file);
			} catch (final IOException e) {
				e.printStackTrace();
			}
		} else {
			result = false;
		}
		return result;
	}
	
	public void removeObsoleteScripts() {
		obsoleted.forEach(builder.index()::removeScript);
		obsoleted.clear();
	}
	
	private SystemScript makeSystemScript(final IResource resource) {
		final SystemScript pinned = SystemScript.pinned(resource, true);
		return pinned != null ? pinned : resource instanceof IFile ? block(() -> {
			final String ln = resource.getName().toLowerCase();
			final IFile file = (IFile) resource;
			try {
				if (Script.looksLikeScriptFile(ln) && isSystemGroup(resource.getParent())) {
					return new SystemScript(builder.index(), file);
				} else if (ln.equals("map.c") && Scenario.get(resource.getParent()) != null) {
					return new MapScript(builder.index(), file);
				} else if (ln.equals("objects.c") && Scenario.get(resource.getParent()) != null) {
					return new ObjectsScript(builder.index(), file);
				} else if (LocalizedScript.FILENAME_PATTERN.matcher(ln).matches() && Definition.at(resource.getParent()) != null) {
					return new LocalizedScript(builder.index(), file);
				} else {
					return null;
				}
			} catch (final CoreException ce) {
				ce.printStackTrace();
				return null;
			}
		}) : null;
	}
	
	public boolean isMapScript(final IResource resource) {
		return resource instanceof IFile && resource.getName().equals("Map.c");
	}
	
	private boolean isSystemGroup(final IContainer container) {
		return container.getName().equals(builder.index().engine().groupName("System", FileExtension.ResourceGroup)); //$NON-NLS-1$
	}

}