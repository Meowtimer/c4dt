package net.arctics.clonk.index;

import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.parser.inireader.DefCoreUnit;
import net.arctics.clonk.resource.c4group.C4Group;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;

public class DefinitionParser {
	
	private Definition object;
	private IContainer objectFolder;
	private IFile defCore;
	private IFile scenario;
	private Index index;
	
	private DefinitionParser(Index index, IContainer folder) {
		this.index = index;
		objectFolder = folder;
		defCore = (IFile) Utilities.findMemberCaseInsensitively(folder, "DefCore.txt"); //$NON-NLS-1$
		scenario = defCore == null ? (IFile) Utilities.findMemberCaseInsensitively(folder, "Scenario.txt") : null; //$NON-NLS-1$
	}
	
	private DefinitionParser(C4Group group) {
//		this.group = group;
	}
	
	/**
	 * Creates a new parser for this object but parses not yet.
	 * Call <code>parse()</code> to parse the object including Script.c and DefCore.txt.
	 * @param folder
	 * @return the parser object or <code>null</code>, if <code>folder</code> is not a valid/complete c4d
	 */
	public static DefinitionParser create(IContainer folder, Index index) {
		if (Utilities.findMemberCaseInsensitively(folder, "DefCore.txt") != null || //$NON-NLS-1$
			Utilities.findMemberCaseInsensitively(folder, "Scenario.txt") != null) //$NON-NLS-1$
		{ 
			DefinitionParser parser = new DefinitionParser(index, folder);
			return parser;
		}
		return null;
	}
	
	public static DefinitionParser create(C4Group group) {
		return new DefinitionParser(group);
	}
	
	public Definition createObject() {
		try {
			object = Definition.definitionCorrespondingToFolder(objectFolder);
			if (defCore != null) {
				DefCoreUnit defCoreWrapper = (DefCoreUnit) Structure.pinned(defCore, true, false);
				if (object == null)
					object = new Definition(index, defCoreWrapper.definitionID(), defCoreWrapper.getName(), objectFolder);
				else {
					object.setId(defCoreWrapper.definitionID());
					object.setName(defCoreWrapper.getName(), false);
				}
			} else if (scenario != null && object == null)
				object = new Scenario(index, objectFolder.getName(), objectFolder);
			return object;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public Definition getObject() {
		return object;
	}

}
