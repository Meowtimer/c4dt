package net.arctics.clonk;

import javax.swing.text.BadLocationException;

import net.arctics.clonk.parser.C4Function;
import net.arctics.clonk.parser.C4Object;
import net.arctics.clonk.parser.C4Variable;
import net.arctics.clonk.parser.ClonkIndex;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.resource.c4group.C4Group;
import net.arctics.clonk.resource.c4group.C4Group.C4GroupType;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

public abstract class Utilities {
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
	
	public static ClonkProjectNature getProject(IResource res) {
		if (res == null) return null;
		IProject project = res.getProject();
		if (project == null) return null;
		try {
			IProjectNature clonkProj = project.getNature("net.arctics.clonk.clonknature");
			return (ClonkProjectNature) clonkProj;
		} catch (CoreException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static ClonkProjectNature getProject(C4Object obj) {
		return getProject((IResource)obj.getScript());
	}
	
	public static ClonkIndex getIndex(IResource res) {
		if (res != null) {
			ClonkProjectNature nature = getProject(res);
			if (nature != null) {
				return nature.getIndexedData();
			}
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
	
	public static ImageDescriptor getIconDescriptor(String path) {
		return ImageDescriptor.createFromURL(FileLocator.find(ClonkCore.getDefault().getBundle(), new Path(path), null));
	}

	public static C4Object getObjectForEditor(ITextEditor editor) {
//		try {
			return C4Object.objectCorrespondingTo(getEditingFile(editor).getParent());
//			return (C4Object)getEditingFile(editor).getParent().getSessionProperty(ClonkCore.C4OBJECT_PROPERTY_ID);
//		} catch (CoreException e) {
//			e.printStackTrace();
//			return null;
//		}
//		 return getProject(editor).getIndexer().getObjectForScript(getEditingFile(editor));
	}
	
	public static C4GroupType groupTypeFromFolderName(String name) {
		C4GroupType result = C4Group.extensionToGroupTypeMap.get(name.substring(name.lastIndexOf(".")+1));
		if (result == null)
			result = C4Group.extensionToGroupTypeMap.get(name.substring(0,3)); // legacy
		if (result != null)
			return result;
		return C4GroupType.OtherGroup;
	}
	
	public static boolean c4FilenameExtensionIs(String filename, String ext) {
		return filename.endsWith(ext);
	}

	public static boolean looksLikeID(String word) {
		if (word == null || word.length() < 4)
			return false;
		for(int i = 0; i < 4;i++) {
			int readChar = word.charAt(i);
			if (('A' <= readChar && readChar <= 'Z') ||
					('0' <= readChar && readChar <= '9') ||
					(readChar == '_')) {
				continue;
			}
			else {
				return false;
			}
		}
		return true;
	}
	
	public static int getStartOfExpression(IDocument doc, int offset) throws org.eclipse.jface.text.BadLocationException {
		for (int off = offset-1; off >= 0; off--) {
			if (doc.getChar(off) == ';')
				return off+1;
		}
		return offset;
	}
	
}
