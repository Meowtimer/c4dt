package net.arctics.clonk.index;

import net.arctics.clonk.parser.C4Structure;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.inireader.DefCoreUnit;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.resource.c4group.C4Group;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;

public class C4ObjectParser {
	
	private C4ObjectIntern object;
	private IContainer objectFolder;
	private IFile defCore;
	private IFile scenario;
	
	private C4ObjectParser(IContainer folder) {
		objectFolder = folder;
		defCore = (IFile) Utilities.findMemberCaseInsensitively(folder, "DefCore.txt"); //$NON-NLS-1$
		scenario = (IFile) Utilities.findMemberCaseInsensitively(folder, "Scenario.txt"); //$NON-NLS-1$
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
		if (Utilities.findMemberCaseInsensitively(folder, "DefCore.txt") != null || //$NON-NLS-1$
			Utilities.findMemberCaseInsensitively(folder, "Scenario.txt") != null) //$NON-NLS-1$
		{ 
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
			object = C4ObjectIntern.objectCorrespondingTo(objectFolder);
			if (defCore != null) {
				DefCoreUnit defCoreWrapper = (DefCoreUnit) C4Structure.pinned(defCore, true, false);
				if (object == null) {
					object = new C4ObjectIntern(defCoreWrapper.getObjectID(), defCoreWrapper.getName(), objectFolder);
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
		ClonkIndex index = ClonkProjectNature.get(objectFolder).getIndex();
		IFile script = object.getScriptStorage();
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
