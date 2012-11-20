package net.arctics.clonk.mapcreator;

import static net.arctics.clonk.util.Utilities.as;
import net.arctics.clonk.parser.inireader.IniSection;
import net.arctics.clonk.parser.inireader.IniUnitParser;
import net.arctics.clonk.parser.inireader.ScenarioUnit;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.swt.graphics.ImageData;

public abstract class MapCreator {
	public MaterialMap MatMap;
	public TextureMap TexMap;
	public ImageData create(ScenarioUnit scenarioConfiguration, boolean layers, int numPlayers) {
		LandscapeSection section = new LandscapeSection();
		section.Default();
		IniSection landscapeIniSection = scenarioConfiguration.sectionWithName(IniUnitParser.defaultSection(LandscapeSection.class), false);
		landscapeIniSection.commit(section, false);
		int[] size = section.GetMapSize(numPlayers);
		IContainer materialsContainer = as(scenarioConfiguration.resource().getProject().findMember("Material.c4g"), IContainer.class);
		MaterialMap materials = new MaterialMap();
		materials.load(materialsContainer);
		TextureMap textureMap = new TextureMap(
			(IFile)Utilities.findMemberCaseInsensitively(materialsContainer, TextureMap.TEXMAP_FILE),
			materials
		);
		ImageData data = new ImageData(size[0], size[1], 8, textureMap.palette());
		create(data, section, textureMap, layers, numPlayers);
		return data;
	}
	public abstract void create(ImageData sfcMap,
        LandscapeSection rLScape, TextureMap rTexMap,
        boolean fLayers, int iPlayerNum);
}
