package net.arctics.clonk;

import net.arctics.clonk.parser.C4Function;
import net.arctics.clonk.parser.C4Variable;
import net.arctics.clonk.resource.ClonkProjectNature;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

public class Utilities {
	public static ClonkProjectNature getProject(ITextEditor editor) {
		try {
			if (editor.getEditorInput() instanceof FileEditorInput) {
				IProjectNature clonkProj = ((FileEditorInput)editor.getEditorInput()).getFile().getProject().getNature("net.arctics.clonk.clonknature");
				if (clonkProj instanceof ClonkProjectNature) {
					return (ClonkProjectNature)clonkProj;
				}
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static IFile getEditingFile(ITextEditor editor) {
		if (editor.getEditorInput() instanceof FileEditorInput) {
			return ((FileEditorInput)editor.getEditorInput()).getFile();
		}
		else return null;
	}
	
	public static Image getIconForFunction(C4Function function) {
		String iconName = function.getVisibility().toString().toLowerCase();
		ImageRegistry reg = ClonkCore.getDefault().getImageRegistry();
		Image img = reg.get(iconName);
		if (img != null)
			return img;
		reg.put(iconName, ImageDescriptor.createFromURL(FileLocator.find(ClonkCore.getDefault().getBundle(), new Path("icons/"+iconName+".png"), null)));
		return reg.get(iconName);
	}
	
	public static Image getIconForVariable(C4Variable variable) {
		String iconName = variable.getScope().toString().toLowerCase();
		ImageRegistry reg = ClonkCore.getDefault().getImageRegistry();
		Image img = reg.get(iconName);
		if (img != null)
			return img;
		reg.put(iconName, ImageDescriptor.createFromURL(FileLocator.find(ClonkCore.getDefault().getBundle(), new Path("icons/"+iconName+".png"), null)));
		return reg.get(iconName);
	}

	public static Image getIconForObject(Object element) {
		if (element instanceof C4Function)
			return getIconForFunction((C4Function)element);
		if (element instanceof C4Variable)
			return getIconForVariable((C4Variable)element);
		return null;
	}
}
