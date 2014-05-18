package net.arctics.clonk.mapcreator;

import static net.arctics.clonk.util.Utilities.as;
import net.arctics.clonk.c4group.GroupType;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.ProjectIndex;
import net.arctics.clonk.ini.IniData;
import net.arctics.clonk.ini.IniSection;
import net.arctics.clonk.ini.ScenarioUnit;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;

public abstract class MapCreator {
	public MaterialMap MatMap;
	public TextureMap TexMap;
	public ImageData create(final ScenarioUnit scenarioConfiguration, final boolean layers, final int numPlayers) {
		final LandscapeSection section = new LandscapeSection();
		section.Default();
		final IniSection landscapeIniSection = scenarioConfiguration.sectionWithName(IniData.defaultSection(LandscapeSection.class), false);
		landscapeIniSection.commit(section, false);
		final int[] size = section.GetMapSize(numPlayers);
		IContainer materialsContainer = null;
		for (final Index i : scenarioConfiguration.index().relevantIndexes())
			if (i instanceof ProjectIndex) {
				materialsContainer = as(i.nature().getProject().findMember
					(i.engine().groupName("Material", GroupType.ResourceGroup)), IContainer.class);
				if (materialsContainer != null)
					break;
			}
		if (materialsContainer != null) {
			final MaterialMap materials = new MaterialMap();
			materials.load(materialsContainer);
			final TextureMap textureMap = new TextureMap(
				(IFile)Utilities.findMemberCaseInsensitively(materialsContainer, TextureMap.TEXMAP_FILE),
				materials
			);
			final ImageData data = new ImageData(size[0], size[1], 8, textureMap.palette());
			create(data, section, textureMap, layers, numPlayers);
			return data;
		} else
			return new ImageData(size[0], size[1], 32, new PaletteData(0xFF, 0xFF00, 0xFF0000));
	}
	public abstract void create(ImageData sfcMap,
        LandscapeSection rLScape, TextureMap rTexMap,
        boolean fLayers, int iPlayerNum);
}
