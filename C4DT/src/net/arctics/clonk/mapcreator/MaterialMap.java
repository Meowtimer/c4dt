package net.arctics.clonk.mapcreator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.Structure;
import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.ini.MaterialUnit;
import net.arctics.clonk.util.StringUtil;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

public class MaterialMap extends HashMap<String, MaterialUnit>  {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	
	private final List<MaterialUnit> orderedMaterials = new ArrayList<MaterialUnit>();

	public MaterialUnit get(final int index) {
		return orderedMaterials.get(index);
	}
	
	@Override
	public MaterialUnit put(final String name, final MaterialUnit material) {
		orderedMaterials.add(material);
		return super.put(name, material);
	}
	
	public void load(final IContainer materialsContainer) {
		final String ext = "."+ClonkProjectNature.get(materialsContainer).index().engine().settings().materialExtension;
		try {
			for (final IResource member : materialsContainer.members())
				if (member instanceof IFile && member.getName().endsWith(ext)) {
					final MaterialUnit unit = (MaterialUnit)Structure.pinned(member, true, false);
					if (unit != null)
						put(StringUtil.rawFileName(member.getName()), unit);
				}
		} catch (final CoreException e) {
			e.printStackTrace();
		}
	}
}
