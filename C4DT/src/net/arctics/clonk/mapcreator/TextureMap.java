package net.arctics.clonk.mapcreator;

import static net.arctics.clonk.util.Utilities.as;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.parser.inireader.IntegerArray;
import net.arctics.clonk.parser.inireader.MaterialUnit;
import net.arctics.clonk.util.StreamUtil;
import net.arctics.clonk.util.StringUtil;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;

public class TextureMap extends HashMap<String, Integer> {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	public static final int C4M_MaxTexIndex = 127; 
	
	private final PaletteData palette;
	
	public PaletteData palette() { return palette; }
	
	public TextureMap() {
		palette = null;
	}
	
	public TextureMap(IContainer materialGroup) {
		IFile texMap = as(Utilities.findMemberCaseInsensitively(materialGroup, "TEXMAP.txt"), IFile.class);
		if (texMap == null)
			throw new IllegalArgumentException();
		RGB[] colors = new RGB[256];
		colors[0] =  new RGB(225, 243, 255);
		final Pattern linePattern = Pattern.compile("([0-9]+)\\=(\\w+)\\-(\\w+)");
		final Matcher lineMatcher = linePattern.matcher("");
		final Map<String, MaterialUnit> materials = new HashMap<String, MaterialUnit>();
		for (String line : StringUtil.lines(new StringReader(StreamUtil.stringFromFile(texMap)))) {
			line = line.trim();
			if (line.startsWith("#"))
				continue;
			if (lineMatcher.reset(line).matches()) {
				int index = Integer.parseInt(lineMatcher.group(1));
				String material = lineMatcher.group(2);
				String texture = lineMatcher.group(3);
				MaterialUnit unit = materials.get(material);
				RGB color = null;
				if (unit == null) {
					IFile materialDefFile = as(Utilities.findMemberCaseInsensitively(materialGroup, material+".c4m"), IFile.class);
					if (materialDefFile != null) {
						unit = (MaterialUnit)Structure.pinned(materialDefFile, true, false);
						if (unit != null)
							materials.put(material, unit);
					}
				}
				if (unit != null) {
					IntegerArray v = unit.complexValue("Material.Color", IntegerArray.class);
					if (v != null && v.values().length >= 3)
						color = new RGB(
							v.values()[0].summedValue(),
							v.values()[1].summedValue(),
							v.values()[2].summedValue()
						);
				}
				colors[index] = color;
				colors[index+128] = color;
				this.put(material+"-"+texture, index);
			}
		}
		this.palette = new PaletteData(colors);
	}
	
	public int GetIndex(String szMaterial, String szTexture, boolean fAddIfNotExist)
	{
		// Find existing
		String combo = szMaterial+"-"+szTexture;
		Integer byIndex = get(combo);
		if (byIndex != null)
			return byIndex;
		// Add new entry
		if (fAddIfNotExist) {
			byIndex = size()+1;
			put(combo, byIndex);
			return byIndex;
		}
		return 0;
	}

	public int GetIndexMatTex(String szMaterialTexture, String szDefaultTexture, boolean fAddIfNotExist, String szErrorIfFailed)
	{
		// split material/texture pair
		String Material, Texture;
		String[] split = szMaterialTexture.split("-");
		Material = split[0];
		Texture = split.length > 1 ? split[1] : null;
		// texture not given or invalid?
		int iMatTex = 0;
		if (Texture != null)
			if ((iMatTex = GetIndex(Material, Texture, fAddIfNotExist)) != 0)
				return iMatTex;
		if (szDefaultTexture != null)
			if ((iMatTex = GetIndex(Material, szDefaultTexture, fAddIfNotExist)) != 0)
				return iMatTex;
		// return default map entry
		return 1;
	}

	public int GetIndexMatTex(String szMaterialTexture) {
		return GetIndexMatTex(szMaterialTexture, null, false, null);
	}
}
