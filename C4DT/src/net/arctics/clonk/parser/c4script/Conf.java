package net.arctics.clonk.parser.c4script;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.c4script.ast.BraceStyleType;
import net.arctics.clonk.parser.c4script.ast.ControlFlowException;
import net.arctics.clonk.parser.c4script.ast.ASTNodePrinter;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.util.IConverter;
import net.arctics.clonk.util.StringUtil;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;

public abstract class Conf {
	
	// options
	public static boolean alwaysConvertObjectCalls = true;
	public static BraceStyleType braceStyle = BraceStyleType.NewLine;
	public static String indentString = "\t"; //$NON-NLS-1$

	public static void printIndent(ASTNodePrinter output, int indentDepth) {
		if (output.flag(ASTNodePrinter.SINGLE_LINE))
			return;
		for (int i = 0; i < indentDepth; i++)
			output.append(indentString);
	}
	
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

	public static final IConverter<ASTNode, Object> EVALUATE_EXPR = new IConverter<ASTNode, Object>() {
		@Override
        public Object convert(ASTNode from) {
            try {
				return from != null ? from.evaluate() : null;
			} catch (ControlFlowException e) {
				return null;
			}
        }
	};
	
	// install property change listener so the indentString and braceStyle will match with corresponding preferences
	
	private static void configureByEditorPreferences() {
		boolean tabsToSpaces = EditorsUI.getPreferenceStore().getBoolean(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS);
		if (tabsToSpaces)
			indentString = StringUtil.repetitions(" ", EditorsUI.getPreferenceStore().getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH));
		else
			indentString = "\t";
		boolean javaStyleBlocks = Core.instance().getPreferenceStore().getBoolean(ClonkPreferences.JAVA_STYLE_BLOCKS);
		if (javaStyleBlocks)
			braceStyle = BraceStyleType.SameLine;
		else
			braceStyle = BraceStyleType.NewLine;
	}
	
	static {
		if (!Core.instance().runsHeadless()) {
			IPropertyChangeListener listener = new IPropertyChangeListener() {
				@Override
				public void propertyChange(PropertyChangeEvent event) {
					String[] relevantPrefValues = {
						AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS,
						ClonkPreferences.JAVA_STYLE_BLOCKS
					};
					for (String pref : relevantPrefValues)
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
