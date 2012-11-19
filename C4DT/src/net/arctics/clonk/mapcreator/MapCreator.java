package net.arctics.clonk.mapcreator;

import static net.arctics.clonk.util.Utilities.as;
import net.arctics.clonk.parser.inireader.IniSection;
import net.arctics.clonk.parser.inireader.IniUnitParser;
import net.arctics.clonk.parser.inireader.ScenarioUnit;

import org.eclipse.core.resources.IContainer;
import org.eclipse.swt.graphics.ImageData;

public abstract class MapCreator {
	public ImageData Create(ScenarioUnit scenarioConfiguration, boolean layers, int numPlayers) {
		LandscapeSection section = new LandscapeSection();
		section.Default();
		IniSection landscapeIniSection = scenarioConfiguration.sectionWithName(IniUnitParser.defaultSection(LandscapeSection.class), false);
		landscapeIniSection.commit(section, false);
		int[] size = section.GetMapSize(numPlayers);
		TextureMap textureMap = new TextureMap(as(scenarioConfiguration.resource().getProject().findMember("Material.c4g"), IContainer.class));
		ImageData data = new ImageData(size[0], size[1], 8, textureMap.palette());
		Create(data, section, textureMap, layers, numPlayers);
		return data;
	}
	public abstract void Create(ImageData sfcMap,
        LandscapeSection rLScape, TextureMap rTexMap,
        boolean fLayers, int iPlayerNum);
}
