package net.arctics.clonk.parser;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.Utilities;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

public class C4ObjectParser {
	
	private IContainer objectFolder;
	private IFile script;
	private IFile defCore;
	private IFile actMap;
	
	private C4ObjectParser(IContainer folder) {
		objectFolder = folder;
		script = (IFile) folder.findMember("Script.c");
		defCore = (IFile) folder.findMember("DefCore.txt");
		actMap = (IFile) folder.findMember("ActMap.txt");
	}
	
	/**
	 * Creates a new parser for this object but parses not yet.
	 * Call <code>parse()</code> to parse the object including Script.c and DefCore.txt.
	 * @param folder
	 * @return the parser object or <code>null</code>, if <code>folder</code> is not a valid/complete c4d
	 */
	public static C4ObjectParser create(IContainer folder) {
		if (folder.findMember("Script.c") != null && 
				folder.findMember("DefCore.txt") != null &&
				(folder.findMember("Graphics.png") != null ||
				folder.findMember("Graphics.bmp") != null)) {
			C4ObjectParser parser = new C4ObjectParser(folder);
			return parser;
		}
		return null;
	}
	
	public void parse() throws CompilerException {
		
		try {
			C4ObjectIntern object = (C4ObjectIntern) objectFolder.getSessionProperty(ClonkCore.C4OBJECT_PROPERTY_ID);
			
			if (defCore != null) {
				C4DefCoreWrapper defCoreWrapper = new C4DefCoreWrapper(defCore);
				defCoreWrapper.parse();
				if (object == null) {
					object = new C4ObjectIntern(defCoreWrapper.getObjectID(),defCoreWrapper.getName(),objectFolder);
				}
				else {
					if (object.getId() != defCoreWrapper.getObjectID()) { // new C4ID set
						ClonkIndex index = Utilities.getProject(objectFolder).getIndexedData(); 
						index.removeObject(object);
						object.setId(defCoreWrapper.getObjectID());
						index.addObject(object);
					}
//					if (!object.getName().equals(defCoreWrapper.getName())) {
						object.setName(defCoreWrapper.getName(), false);
//					}
				}
			}
			if (script != null) {
				C4ScriptParser p = new C4ScriptParser(script, object);
				p.parse();
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
}
