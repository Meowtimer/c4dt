package net.arctics.clonk;

import net.arctics.clonk.parser.C4Function;
import net.arctics.clonk.parser.C4Object;
import net.arctics.clonk.parser.C4ObjectIntern;
import net.arctics.clonk.parser.C4ScriptBase;
import net.arctics.clonk.parser.C4SystemScript;
import net.arctics.clonk.parser.C4Variable;
import net.arctics.clonk.parser.ClonkIndex;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.resource.c4group.C4Group;
import net.arctics.clonk.resource.c4group.C4Group.C4GroupType;
import net.arctics.clonk.ui.editors.ObjectExternEditorInput;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.text.BadLocationException;
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
		if (obj == null) return null;
		if (obj instanceof C4ObjectIntern)
			return getProject(((C4ObjectIntern)obj).getObjectFolder());
		else
			return null;
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
		if (element instanceof C4Object)
			return getIconForC4ID((C4Object)element);
		return null;
	}
	
	public static Image getIconForC4ID(C4Object element) {
		return null;
//		Image base = new Image(PlatformUI.getWorkbench().getDisplay(),FileLocator.find(null, null, null).openStream());
//		ImageData data = base.getImageData();
//		org.eclipse.swt.graphics.
//		ImageData newData = data.scaledTo(16, 16);
//		return new Image(PlatformUI.getWorkbench().getDisplay(), newData);
	}

	public static ImageDescriptor getIconDescriptor(String path) {
		return ImageDescriptor.createFromURL(FileLocator.find(ClonkCore.getDefault().getBundle(), new Path(path), null));
	}
	
	public static C4ScriptBase getScriptForFile(IFile scriptFile) {
		C4ScriptBase script;
		try {
			script = C4SystemScript.scriptCorrespondingTo(scriptFile);
		} catch (CoreException e) {
			script = null;
		}
		if (script == null)
			script = C4ObjectIntern.objectCorrespondingTo(scriptFile.getParent());
		// there can only be one script oO (not ScriptDE or something)
		if (!script.getScriptFile().equals(scriptFile))
			return null;
		return script;
	}

	public static C4ScriptBase getScriptForEditor(ITextEditor editor) {
		if (editor.getEditorInput() instanceof ObjectExternEditorInput) {
			return ((ObjectExternEditorInput)editor.getEditorInput()).getObject();
		}
		return getScriptForFile(getEditingFile(editor));
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
		int digits = 0;
		for(int i = 0; i < 4;i++) {
			int readChar = word.charAt(i);
			if ('0' <= readChar && readChar <= '9')
				digits++;
			if (('A' <= readChar && readChar <= 'Z') ||
					('0' <= readChar && readChar <= '9') ||
					(readChar == '_')) {
				continue;
			}
			else {
				return false;
			}
		}
		return digits != 4; // rather interpret 1000 as int
	}
	
	public static int getStartOfStatement(IDocument doc, int offset) throws BadLocationException {
		int bracketDepth = 0;
		for (int off = offset-1; off >= 0; off--) {
			switch (doc.getChar(off)) {
			case ';': case '{': case '}': case ']': case ':':
				if (bracketDepth <= 0)
					return off+1;
				break;
			case ')':
				bracketDepth++;
				break;
			case '(':
				if (--bracketDepth < 0)
					return off+1;
				break;
			}
		}
		return offset;
	}
	
	public static int getEndOfStatement(IDocument doc, int offset) throws BadLocationException {
		int bracketDepth = 0, blockDepth = 0;
		for (int off = offset+1; off < doc.getLength(); off++) {
			switch (doc.getChar(off)) {
			case ';':
				if (bracketDepth <= 0 && blockDepth <= 0)
					return off+1;
				break;
			case '{':
				blockDepth++;
				break;
			case '}':
				if (--blockDepth <= 0)
					return off+1;
				break;
			case '(':
				bracketDepth++;
				break;
			case ')':
				bracketDepth--;
				break;
			}
		}
		return offset;
	}
	
}
