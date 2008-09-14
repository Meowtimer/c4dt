package net.arctics.clonk.ui.editors;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public class ColorManager {

	protected Map fColorTable = new HashMap(10);
	public static ColorManager instance;
	
	public ColorManager() {
		instance = this;
	}
	
	public void dispose() {
		Iterator e = fColorTable.values().iterator();
		while (e.hasNext())
			 ((Color) e.next()).dispose();
	}
	public Color getColor(RGB rgb) {
		Color color = (Color) fColorTable.get(rgb);
		if (color == null) {
			color = new Color(Display.getCurrent(), rgb);
			fColorTable.put(rgb, color);
		}
		return color;
	}
}
