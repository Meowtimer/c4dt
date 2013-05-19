package net.arctics.clonk.c4script;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.c4script.ast.BraceStyleType;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.util.StringUtil;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;

/**
 * Container class containing some global configuration values affecting
 * pretty-printing of C4Script syntax trees. Listens on global preferences to synchronize
 * static field values with preference values.
 * @author madeen
 *
 */
public abstract class Conf {

	// options
	/** Always convert ObjectCall/Call constructs to ->(~) calls when tidying up code */
	public static boolean alwaysConvertObjectCalls = true;
	/** Brace style when pretty printing blocks. */
	public static BraceStyleType braceStyle = BraceStyleType.NewLine;
	/** String used for one indentation level when pretty-printing */
	public static String indentString = "\t"; //$NON-NLS-1$

	/**
	 * Print indentation using {@link #indentString}.
	 * @param output Printer to print indentation into
	 * @param indentDepth Indentation depth
	 */
	public static void printIndent(ASTNodePrinter output, int indentDepth) {
		if (output.flag(ASTNodePrinter.SINGLE_LINE))
			return;
		for (int i = 0; i < indentDepth; i++)
			output.append(indentString);
	}

	/**
	 * Print indentation/new line prelude before a braces block. Depends on {@link #braceStyle}.
	 * @param output Printer to print prelude into
	 * @param indentDepth Current indentation depth
	 */
	public static void blockPrelude(ASTNodePrinter output, int indentDepth) {
		switch (braceStyle) {
		case NewLine:
			output.append('\n');
			Conf.printIndent(output, indentDepth);
			break;
		case SameLine:
			output.append(' ');
			break;
		}
	}

	// install property change listener so the indentString and braceStyle will match with corresponding preferences

	private static void configureByEditorPreferences() {
		final boolean tabsToSpaces = EditorsUI.getPreferenceStore().getBoolean(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS);
		if (tabsToSpaces)
			indentString = StringUtil.repetitions(" ", EditorsUI.getPreferenceStore().getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH));
		else
			indentString = "\t";
		final boolean javaStyleBlocks = Core.instance().getPreferenceStore().getBoolean(ClonkPreferences.JAVA_STYLE_BLOCKS);
		if (javaStyleBlocks)
			braceStyle = BraceStyleType.SameLine;
		else
			braceStyle = BraceStyleType.NewLine;
	}

	static {
		if (Core.instance() != null && !Core.instance().runsHeadless()) {
			final IPropertyChangeListener listener = new IPropertyChangeListener() {
				@Override
				public void propertyChange(PropertyChangeEvent event) {
					final String[] relevantPrefValues = {
						AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS,
						ClonkPreferences.JAVA_STYLE_BLOCKS
					};
					for (final String pref : relevantPrefValues)
						if (event.getProperty().equals(pref))
							configureByEditorPreferences();
				}
			};
			EditorsUI.getPreferenceStore().addPropertyChangeListener(listener);
			Core.instance().getPreferenceStore().addPropertyChangeListener(listener);
			configureByEditorPreferences();
		}
	}

}
