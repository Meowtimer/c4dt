package net.arctics.clonk.ui.editors;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import net.arctics.clonk.Core;

public enum ColorManager {
	INSTANCE;

	public class SyntaxElementStyle {

		public static final String RGB = "rgb";
		public static final String STYLE = "style";

		public String name;
		public RGB defaultRGB;
		public int defaultStyle;
		public String localizedName;
		public String prefName(final String forWhat) {
			return name+"."+forWhat;
		}
		public RGB rgb() {
			final String prefName = prefName(RGB);
			RGB result = PreferenceConverter.getColor(Core.instance().getPreferenceStore(), prefName);
			if (result == PreferenceConverter.COLOR_DEFAULT_DEFAULT) {
				result = defaultRGB;
				PreferenceConverter.setValue(Core.instance().getPreferenceStore(), prefName, result);
			}
			return result;
		}
		public int style() {
			final String prefName = prefName(STYLE);
			if (Core.instance().getPreferenceStore().isDefault(prefName)) {
				Core.instance().getPreferenceStore().setValue(prefName, defaultStyle);
			}
			return Core.instance().getPreferenceStore().getInt(prefName);
		}
		public SyntaxElementStyle(final String name, final RGB rgb, final int style) {
			super();
			this.name = name;
			this.defaultRGB = rgb;
			this.defaultStyle = style;
			try {
				this.localizedName = (String)Messages.class.getField(ColorManager.class.getSimpleName()+"_"+name).get(null);
			} catch (final Exception e) {
				e.printStackTrace();
			}
			if (localizedName == null) {
				System.out.println("No localizedName for " + name);
			}

			syntaxElementStyles.put(name, this);
		}
	}

	public final HashMap<String, SyntaxElementStyle> syntaxElementStyles = new HashMap<String, SyntaxElementStyle>();
	{
		new SyntaxElementStyle("COMMENT", new RGB(128, 0, 0), 0);
		new SyntaxElementStyle("JAVADOCCOMMENT", new RGB(120, 30, 0), 0);
		new SyntaxElementStyle("DEFAULT", new RGB(0, 0, 0), 0);
		new SyntaxElementStyle("STRING", new RGB(128, 128, 128), 0);
		new SyntaxElementStyle("KEYWORD", new RGB(0x30,0,0xFF), 0);
		new SyntaxElementStyle("TYPE", new RGB(0,0,0xFF), 0);
		new SyntaxElementStyle("OPERATOR", new RGB(0,0x99,0), 0);
		new SyntaxElementStyle("ENGINE_FUNCTION", new RGB(0x80,0x80,0), 0);
		new SyntaxElementStyle("NUMBER", new RGB(0xFF,0,0), 0);;
		new SyntaxElementStyle("BRACKET", new RGB(0,0x99,0), 0);
		new SyntaxElementStyle("RETURN", new RGB(0x50,0,0xFF), 0);
		new SyntaxElementStyle("DIRECTIVE", new RGB(0x33,0x33,0xAA), 0);
		new SyntaxElementStyle("OBJ_CALLBACK", new RGB(0x5C,0xA,0x5C), 0);
	}

	public RGB defaultColorForSyntaxElement(final String elementName) {
		try {
			return syntaxElementStyles.get(elementName).defaultRGB;
		} catch (final Exception e) {
			return null;
		}
	}

	public RGB colorForSyntaxElement(final String elementName) {
		try {
			return syntaxElementStyles.get(elementName).rgb();
		} catch (final Exception e) {
			return null;
		}
	}

	protected Map<RGB, Color> colorTable = new HashMap<RGB, Color>(10);

	public void dispose() {
		for (final Color c : colorTable.values()) {
			c.dispose();
		}
	}
	public Color getColor(final RGB rgb) {
		Color color = colorTable.get(rgb);
		if (color == null) {
			color = new Color(Display.getCurrent(), rgb);
			colorTable.put(rgb, color);
		}
		return color;
	}
}
