package net.arctics.clonk.builder;

import java.net.URI;

import net.arctics.clonk.Core;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

public class CustomizationNature implements IProjectNature {

	public static final String NATURE_ID = Core.id("c4dt_customization");
	
	private IProject project;
	
	@Override
	public void configure() throws CoreException {
	}

	@Override
	public void deconfigure() throws CoreException {
	}

	@Override
	public IProject getProject() {
		return project;
	}

	@Override
	public void setProject(IProject project) {
		this.project = project;
	}
	
	public static CustomizationNature get() {
		for (IProject proj : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			CustomizationNature nat = get(proj);
			if (nat != null)
				return nat;
		}
		return null;
	}
	
	public static CustomizationNature get(IProject project) {
		try {
			if (project.isOpen() && project.hasNature(NATURE_ID)) {
				return (CustomizationNature) project.getNature(NATURE_ID);
			} else
				return null;
		} catch (CoreException e) {
			return null;
		}
	}
	
	public static CustomizationNature create(String name) {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProject newProject = workspace.getRoot().getProject(name);
		IProjectDescription desc = workspace.newProjectDescription(name);
		desc.setNatureIds(new String[] { NATURE_ID });
		try {
			newProject.create(desc, null);
			newProject.open(null);
		} catch (CoreException e) {
			e.printStackTrace();
			return null;
		}
		for (String engineName : Core.instance().namesOfAvailableEngines()) {
			URI workspaceStorageURI = URIUtil.toURI(Core.instance().workspaceStorageLocationForEngine(engineName));			
			try {
				newProject.getFolder(engineName).createLink(workspaceStorageURI, 0, null);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		try {
			return (CustomizationNature) newProject.getNature(NATURE_ID);
		} catch (CoreException e) {
			e.printStackTrace();
			return null;
		}
	}

}
