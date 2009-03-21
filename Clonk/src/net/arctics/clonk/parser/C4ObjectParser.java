package net.arctics.clonk.parser;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.defcore.DefCoreParser;
import net.arctics.clonk.resource.c4group.C4Group;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;

public class C4ObjectParser {
	
	private C4ObjectIntern object;
	private IContainer objectFolder;
//	private IFile script; there are some objects without a script
	private IFile defCore;
//	private IFile actMap;
	private IFile scenario;
	
//	private C4Group group;
//	private C4Entry extScript;
//	private C4Entry extDefCore;
	
	private C4ObjectParser(IContainer folder) {
		objectFolder = folder;
//		script = (IFile) folder.findMember("Script.c");
		defCore = (IFile) folder.findMember("DefCore.txt");
//		actMap = (IFile) folder.findMember("ActMap.txt");
		scenario = (IFile) folder.findMember("Scenario.txt");
	}
	
	private C4ObjectParser(C4Group group) {
//		this.group = group;
	}
	
	/**
	 * Creates a new parser for this object but parses not yet.
	 * Call <code>parse()</code> to parse the object including Script.c and DefCore.txt.
	 * @param folder
	 * @return the parser object or <code>null</code>, if <code>folder</code> is not a valid/complete c4d
	 */
	public static C4ObjectParser create(IContainer folder) {
		if ( 
				(folder.findMember("DefCore.txt") != null &&
				(folder.findMember("Graphics.png") != null ||
				folder.findMember("Graphics.bmp") != null)) ||
				folder.findMember("Scenario.txt") != null) {
			C4ObjectParser parser = new C4ObjectParser(folder);
			return parser;
		}
		return null;
	}
	
	public static C4ObjectParser create(C4Group group) {
		return new C4ObjectParser(group);
	}
	
	public C4ObjectIntern createObject() {
		try {
			object = (C4ObjectIntern) objectFolder.getSessionProperty(ClonkCore.C4OBJECT_PROPERTY_ID);
			if (defCore != null) {
				DefCoreParser defCoreWrapper = new DefCoreParser(defCore);
				defCoreWrapper.parse();
				if (object == null) {
					object = new C4ObjectIntern(defCoreWrapper.getObjectID(),defCoreWrapper.getName(),objectFolder);
				}
				else {
					object.setId(defCoreWrapper.getObjectID());
					object.setName(defCoreWrapper.getName(), false);
				}
			}
			else if (scenario != null) {
				if (object == null) {
					object = new C4Scenario(null, objectFolder.getName(), objectFolder);
				}
			}
			return object;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public void parseScript(C4ScriptParser scriptParser) {
		ClonkIndex index = Utilities.getProject(objectFolder).getIndex();
		IFile script = object.getScriptFile();
		if (script != null) {
			if (scriptParser == null)
				scriptParser = new C4ScriptParser(object);
			scriptParser.clean();
			scriptParser.parseDeclarations();
		}
		if (object != null) {
			index.addScript(object);
		}
	}

	public C4ObjectIntern getObject() {
		return object;
	}

}
