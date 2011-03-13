package net.arctics.clonk.resource;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.resource.c4group.C4Group;
import net.arctics.clonk.resource.c4group.C4Group.GroupType;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IResource;
import org.eclipse.ui.IFileEditorInput;

public class ResourceTester extends PropertyTester {
	
	public boolean test(Object receiver, String property, Object[] args, Object expected) {
		try {
			// Editor? Resolve to associated resource
			if(receiver instanceof IFileEditorInput)
				receiver = ((IFileEditorInput) receiver).getFile();

			IResource res = receiver instanceof IResource ? (IResource) receiver : null;
			if (res == null)
				return false;

			boolean result = false;		
			// Calculate property value
			if(property.equals("isScenario")) //$NON-NLS-1$
				result = isScenario(res);
			else if(property.equals("isDefinition")) //$NON-NLS-1$
				result = isDefinition(res);
			else if(property.equals("isFolder")) //$NON-NLS-1$
				result = isFolder(res);
			else if(property.equals("isResource")) //$NON-NLS-1$
				result = isResource(res);
			else if(property.equals("isInScenario")) //$NON-NLS-1$
				result = isInScenario(res);
			else
				assert false;

			// Compare to expected value, if given
			return expected == null ? result : (expected instanceof Boolean && result == (Boolean)expected);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private static boolean checkGroupType(IResource res, C4Group.GroupType gt) {
		return ClonkProjectNature.getEngine(res).getGroupTypeForFileName(res.getName()) == gt;
	}
	
	/** @return Whether the given resource is a scenario */
	public static boolean isScenario(IResource res) {
		return checkGroupType(res, GroupType.ScenarioGroup);
	}
	
	/** @return Whether the given resource is an object definition or object package */
	public static boolean isDefinition(IResource res) {
		return checkGroupType(res, GroupType.DefinitionGroup);
	}

	/** @return Whether the given resource is a scenario folder */
	public static boolean isFolder(IResource res) {
		return checkGroupType(res, GroupType.FolderGroup);
	}

	/** @return Whether the given resource is a resource group */
	public static boolean isResource(IResource res) {
		return checkGroupType(res, GroupType.ResourceGroup);
	}
	
	/** @return Whether the given resource is contained in a scenario */
	public static boolean isInScenario(IResource res) {
		for (; res != null; res = res.getParent())
			if (isScenario(res))
				return true;
		return false;
	}

	public static boolean isInClonkProject(IResource res) {
		try {
			return res.getProject().hasNature(ClonkCore.CLONK_NATURE_ID);
		} catch (Exception e) {
			return false;
		}
	}
	
}