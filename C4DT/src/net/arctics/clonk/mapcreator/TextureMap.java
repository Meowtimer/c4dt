package net.arctics.clonk.mapcreator;

import java.util.HashMap;

import net.arctics.clonk.Core;

public class TextureMap extends HashMap<String, Integer> {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	
	public static final int C4M_MaxTexIndex = 127; 
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
