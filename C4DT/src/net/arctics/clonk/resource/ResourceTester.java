package net.arctics.clonk.resource;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.resource.c4group.C4Group;
import net.arctics.clonk.resource.c4group.C4Group.GroupType;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IFileEditorInput;

public class ResourceTester extends PropertyTester {

	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expected) {
		try {
			// Editor? Resolve to associated resource
			if (receiver instanceof IFileEditorInput)
				receiver = ((IFileEditorInput) receiver).getFile();

			IResource res = receiver instanceof IResource ? (IResource) receiver : null;
			if (res == null)
				return false;

			boolean result = false;		
			// Calculate property value
			if (property.equals("isScenario")) //$NON-NLS-1$
				result = isScenario(res);
			else if (property.equals("isDefinition")) //$NON-NLS-1$
				result = isDefinition(res);
			else if (property.equals("isFolder")) //$NON-NLS-1$
				result = isFolder(res);
			else if (property.equals("isResource")) //$NON-NLS-1$
				result = isResource(res);
			else if (property.equals("isInScenario")) //$NON-NLS-1$
				result = isInScenario(res);
			else if (property.equals("isInClonkProject"))
				result = isInClonkProject(res);
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
		Engine engine = ClonkProjectNature.engineFromResource(res);
		return engine != null && engine.groupTypeForFileName(res.getName()) == gt;
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

	/** @return Whether the given resource is inside a project sporting a Clonk project nature */
	public static boolean isInClonkProject(IResource res) {
		try {
			return res.getProject().equals(res.getParent()) && res.getProject().hasNature(Core.NATURE_ID);
		} catch (Exception e) {
			return false;
		}
	}

	/** @return Whether the given folder is a packed c4group */
	public static boolean isPackedGroup(IResource res) {
		try {
			return res instanceof IContainer && EFS.getStore(res.getLocationURI()) instanceof C4Group;
		} catch (CoreException e) {
			return false;
		}
	}

}