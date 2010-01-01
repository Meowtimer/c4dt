package net.arctics.clonk.ui.editors;

import net.arctics.clonk.ClonkCore;

import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.RGB;

public class ClonkColorConstants {
	
	public static final class Defaults {
		private Defaults() {}
		
		public static final RGB COMMENT = new RGB(128, 0, 0);
		public static final RGB PROC_INSTR = new RGB(128, 128, 128);
		public static final RGB DEFAULT = new RGB(0, 0, 0);
		public static final RGB STRING = new RGB(128, 128, 128);
		public static final RGB KEYWORD = new RGB(0x30,0,0xFF);
		public static final RGB TYPE = new RGB(0,0,0xFF);
		public static final RGB OPERATOR = new RGB(0,0x99,0);
		public static final RGB ENGINE_FUNCTION = new RGB(0x80,0x80,0);
		public static final RGB NUMBER = new RGB(0xFF,0,0);;
		public static final RGB BRACKET = new RGB(0,0x99,0);
		public static final RGB RETURN = new RGB(0x50,0,0xFF);
		public static final RGB PRAGMA = new RGB(0x33,0x33,0xAA);
		public static final RGB OBJ_CALLBACK = new RGB(0x5C,0xA,0x5C);
	}
	
	public static final class ColorHumanReadable {
		public static final String COMMENT = Messages.ClonkColorConstants_COMMENT;
		public static final String PROC_INSTR = Messages.ClonkColorConstants_PROC_INSTR;
		public static final String DEFAULT = Messages.ClonkColorConstants_DEFAULT;
		public static final String STRING = Messages.ClonkColorConstants_STRING;
		public static final String KEYWORD = Messages.ClonkColorConstants_KEYWORD;
		public static final String TYPE = Messages.ClonkColorConstants_TYPE;
		public static final String OPERATOR = Messages.ClonkColorConstants_OPERATOR;
		public static final String ENGINE_FUNCTION = Messages.ClonkColorConstants_ENGINE_FUNCTION;
		public static final String NUMBER = Messages.ClonkColorConstants_NUMBER;
		public static final String BRACKET = Messages.ClonkColorConstants_BRACKET;
		public static final String RETURN = Messages.ClonkColorConstants_RETURN;
		public static final String PRAGMA = Messages.ClonkColorConstants_DIRECTIVE;
		public static final String OBJ_CALLBACK = Messages.ClonkColorConstants_OBJ_CALLBACK;
	}
	
	public static RGB getColor(String prefName) {
		String actualPrefName = actualPrefName(prefName);
		RGB result = PreferenceConverter.getColor(ClonkCore.getDefault().getPreferenceStore(), actualPrefName);
		if (result == PreferenceConverter.COLOR_DEFAULT_DEFAULT) {
			try {
				result = (RGB) Defaults.class.getField(prefName).get(null);
				PreferenceConverter.setValue(ClonkCore.getDefault().getPreferenceStore(), actualPrefName, result);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return result;
	}

	public static String actualPrefName(String prefName) {
		return "COLOR_"+prefName; //$NON-NLS-1$
	}
	
}
