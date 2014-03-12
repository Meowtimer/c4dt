package net.arctics.clonk.c4script;

import static net.arctics.clonk.util.ArrayUtil.iterable;
import static net.arctics.clonk.util.ArrayUtil.map;
import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.c4script.ast.BraceStyleType;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.util.StringUtil;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;

/**
 * Container class containing some global configuration values affecting
 * pretty-printing of C4Script syntax trees. Listens on global preferences to synchronize
 * static field values with preference values.
 * Also provides some utility functions which print things based on field configuration in here.
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
	public static void printIndent(final ASTNodePrinter output, final int indentDepth) {
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
	public static void blockPrelude(final ASTNodePrinter output, final int indentDepth) {
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
			indentString = StringUtil.multiply(" ", EditorsUI.getPreferenceStore().getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH));
		else
			indentString = "\t";
		final boolean javaStyleBlocks = Core.instance().getPreferenceStore().getBoolean(ClonkPreferences.JAVA_STYLE_BLOCKS);
		if (javaStyleBlocks)
			braceStyle = BraceStyleType.SameLine;
		else
			braceStyle = BraceStyleType.NewLine;
	}

	static {
		if (Core.instance() != null && !Core.runsHeadless()) {
			final IPropertyChangeListener listener = event -> {
				final String[] relevantPrefValues = {
					AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS,
					ClonkPreferences.JAVA_STYLE_BLOCKS
				};
				for (final String pref : relevantPrefValues)
					if (event.getProperty().equals(pref))
						configureByEditorPreferences();
			};
			EditorsUI.getPreferenceStore().addPropertyChangeListener(listener);
			Core.instance().getPreferenceStore().addPropertyChangeListener(listener);
			configureByEditorPreferences();
		}
	}

	/**
	 * Print a list of nodes, spreading the output over multiple lines
	 * if the length of the nodes when printed exceeds some threshold (currently hardcoded to 80).
	 * @param output Output to print to
	 * @param depth Indentation level of the list, taken into account when spreading over multiple lines.
	 * @param blockStart Start of block ("(", "[", ...)
	 * @param blockEnd End of block (")", "]", ...)
	 */
	public static void printNodeList(final ASTNodePrinter output, final ASTNode[] params, final int depth, String blockStart, String blockEnd) {
		final Iterable<String> parmStrings = map(iterable(params), from -> from.printed(depth+(braceStyle==BraceStyleType.NewLine?1:0)).trim());
		int len = 0;
		for (final String ps : parmStrings)
			len += ps.length();
		if (len < 80)
			StringUtil.writeBlock(output, blockStart, blockEnd, ", ", parmStrings);
		else {
			final String indent = "\n"+StringUtil.multiply(indentString, depth+1);
			switch (braceStyle) {
			case NewLine:
				blockStart = "\n"+StringUtil.multiply(indentString, depth)+blockStart+indent;
				blockEnd = "\n"+StringUtil.multiply(indentString, depth)+blockEnd;
				break;
			default:
				break;
			}
			StringUtil.writeBlock(output, blockStart, blockEnd, ","+indent, parmStrings);
		}
	}

}
