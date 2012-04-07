package net.arctics.clonk.ui.editors;

import java.util.HashMap;
import java.util.Map;

import net.arctics.clonk.Core;

import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public class ColorManager {

	public class SyntaxElementStyle {
		
		public static final String RGB = "rgb";
		public static final String STYLE = "style";
		
		public String name;
		public RGB defaultRGB;
		public int defaultStyle;
		public String localizedName;
		public String prefName(String forWhat) {
			return name+"."+forWhat;
		}
		public RGB rgb() {
			String prefName = prefName(RGB);
			RGB result = PreferenceConverter.getColor(Core.instance().getPreferenceStore(), prefName);
			if (result == PreferenceConverter.COLOR_DEFAULT_DEFAULT) {
				result = defaultRGB;
				PreferenceConverter.setValue(Core.instance().getPreferenceStore(), prefName, result);
			}
			return result;
		}
		public int style() {
			String prefName = prefName(STYLE);
			return Core.instance().getPreferenceStore().isDefault(prefName)
				? defaultStyle
				: Core.instance().getPreferenceStore().getInt(prefName);
		}
		public SyntaxElementStyle(String name, RGB rgb, int style) {
			super();
			this.name = name;
			this.defaultRGB = rgb;
			this.defaultStyle = style;
			try {
				this.localizedName = (String)Messages.class.getField(ColorManager.class.getSimpleName()+"_"+name).get(null);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (localizedName == null)
				System.out.println("No localizedName for " + name);
			
			syntaxElementStyles.put(name, this);
		}
	}
	
	public final HashMap<String, SyntaxElementStyle> syntaxElementStyles = new HashMap<String, SyntaxElementStyle>();
	{
		new SyntaxElementStyle("COMMENT", new RGB(128, 0, 0), 0);
		new SyntaxElementStyle("JAVADOCCOMMENT", new RGB(120, 30, 0), 0);
		new SyntaxElementStyle("PROC_INSTR", new RGB(128, 128, 128), 0);
		new SyntaxElementStyle("DEFAULT", new RGB(0, 0, 0), 0);
		new SyntaxElementStyle("STRING", new RGB(128, 128, 128), 0);
		new SyntaxElementStyle("KEYWORD", new RGB(0x30,0,0xFF), SWT.BOLD);
		new SyntaxElementStyle("TYPE", new RGB(0,0,0xFF), 0);
		new SyntaxElementStyle("OPERATOR", new RGB(0,0x99,0), 0);
		new SyntaxElementStyle("ENGINE_FUNCTION", new RGB(0x80,0x80,0), 0);
		new SyntaxElementStyle("NUMBER", new RGB(0xFF,0,0), 0);;
		new SyntaxElementStyle("BRACKET", new RGB(0,0x99,0), 0);
		new SyntaxElementStyle("RETURN", new RGB(0x50,0,0xFF), 0);
		new SyntaxElementStyle("DIRECTIVE", new RGB(0x33,0x33,0xAA), 0);
		new SyntaxElementStyle("OBJ_CALLBACK", new RGB(0x5C,0xA,0x5C), 0);
	}
	
	public RGB defaultColorForSyntaxElement(String elementName) {
		try {
			return syntaxElementStyles.get(elementName).defaultRGB;
		} catch (Exception e) {
			return null;
		}
	}
	
	public RGB colorForSyntaxElement(String elementName) {
		try {
			return syntaxElementStyles.get(elementName).rgb();
		} catch (Exception e) {
			return null;
		}
	}
	
	protected Map<RGB, Color> colorTable = new HashMap<RGB, Color>(10);
	
	public ColorManager() {
	}
	
	public void dispose() {
		for (Color c : colorTable.values())
			c.dispose();
	}
	public Color getColor(RGB rgb) {
		Color color = colorTable.get(rgb);
		if (color == null) {
			color = new Color(Display.getCurrent(), rgb);
			colorTable.put(rgb, color);
		}
		return color;
	}
	
	private static final ColorManager instance = new ColorManager();
	public static ColorManager instance() {return instance;}
}
