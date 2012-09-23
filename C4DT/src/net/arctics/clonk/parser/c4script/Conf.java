package net.arctics.clonk.parser.c4script;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.c4script.ast.BraceStyleType;
import net.arctics.clonk.parser.c4script.ast.ControlFlowException;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.ExprWriter;
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
	public static boolean staticTyping = true;

	public static void printIndent(ExprWriter builder, int indentDepth) {
		for (int i = 0; i < indentDepth; i++)
			builder.append(indentString);
	}

	public static final IConverter<ExprElm, Object> EVALUATE_EXPR = new IConverter<ExprElm, Object>() {
		@Override
        public Object convert(ExprElm from) {
            try {
				return from != null ? from.evaluate() : null;
			} catch (ControlFlowException e) {
				return null;
			}
        }
	};
	
	// install property change listener so the indentString will match with the user preferences regarding spaces-to-tabs conversion
	
	private static void configureByEditorPreferences() {
		boolean tabsToSpaces = EditorsUI.getPreferenceStore().getBoolean(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS);
		if (tabsToSpaces)
			indentString = StringUtil.repetitions(" ", EditorsUI.getPreferenceStore().getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH));
		else
			indentString = "\t";
	}
	
	static {
		if (!Core.instance().runsHeadless()) {
			EditorsUI.getPreferenceStore().addPropertyChangeListener(new IPropertyChangeListener() {
				@Override
				public void propertyChange(PropertyChangeEvent event) {
					if (event.getProperty().equals(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS))
						configureByEditorPreferences();
				}
			});
			configureByEditorPreferences();
		}
	}

}
