package net.arctics.clonk.mapcreator;

import net.arctics.clonk.parser.inireader.IniSection;
import net.arctics.clonk.parser.inireader.IniUnitParser;
import net.arctics.clonk.parser.inireader.ScenarioUnit;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;

public abstract class MapCreator {
	public ImageData Create(ScenarioUnit scenarioConfiguration, boolean layers, int numPlayers) {
		LandscapeSection section = new LandscapeSection();
		section.Default();
		IniSection landscapeIniSection = scenarioConfiguration.sectionWithName(IniUnitParser.defaultSection(LandscapeSection.class), false);
		landscapeIniSection.commit(section, false);
		int[] size = section.GetMapSize(numPlayers);
		ImageData data = new ImageData(size[0], size[1], 32, new PaletteData(0xFF0000, 0xFF00, 0xFF));
		TextureMap textureMap = new TextureMap();
		Create(data, section, textureMap, layers, numPlayers);
		return data;
	}
	public abstract void Create(ImageData sfcMap,
        LandscapeSection rLScape, TextureMap rTexMap,
        boolean fLayers, int iPlayerNum);
}
