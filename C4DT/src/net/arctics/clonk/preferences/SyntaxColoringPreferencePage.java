package net.arctics.clonk.preferences;

import net.arctics.clonk.Core;
import net.arctics.clonk.ui.editors.StructureTextEditor;
import net.arctics.clonk.ui.editors.ColorManager;
import net.arctics.clonk.ui.editors.EditorUtil;
import net.arctics.clonk.ui.editors.StructureTextScanner.ScannerPerEngine;
import net.arctics.clonk.ui.editors.ColorManager.SyntaxElementStyle;

import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class SyntaxColoringPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public SyntaxColoringPreferencePage() {
		super (GRID);
		setPreferenceStore(Core.instance().getPreferenceStore());
	}

	public static class StyleEditor extends ColorFieldEditor {
		private Button boldButton, italicButton;
		private final SyntaxElementStyle element;
		public StyleEditor(final SyntaxElementStyle element, final Composite parent) {
			super(element.prefName(SyntaxElementStyle.RGB), element.localizedName, parent);
			this.element = element;
		}
		@Override
		public int getNumberOfControls() {
			return super.getNumberOfControls()+2;
		}
		@Override
		protected void adjustForNumColumns(final int numColumns) {
		}
		@Override
		protected void doFillIntoGrid(final Composite parent, final int numColumns) {
			super.doFillIntoGrid(parent, numColumns);
			getLabelControl().setLayoutData(new GridData());
			boldButton = new Button(parent, SWT.CHECK|SWT.LEFT);
			boldButton.setText("Bold");
			boldButton.setLayoutData(new GridData());
			italicButton = new Button(parent, SWT.CHECK|SWT.LEFT);
			italicButton.setText("Italic");
			italicButton.setLayoutData(new GridData());
		}
		@Override
		protected void doLoad() {
			super.doLoad();
			load(getPreferenceStore().getInt(stylePrefName()));
		}
		protected String stylePrefName() {
			return element.prefName(SyntaxElementStyle.STYLE);
		}
		private void load(final int value) {
			boldButton.setSelection((value & SWT.BOLD) != 0);
			italicButton.setSelection((value & SWT.ITALIC) != 0);
		}
		@Override
		protected void doLoadDefault() {
			super.doLoadDefault();
			load(getPreferenceStore().getDefaultInt(stylePrefName()));
		}
		@Override
		protected void doStore() {
			super.doStore();
			int v = 0;
			if (boldButton.getSelection())
				v |= SWT.BOLD;
			if (italicButton.getSelection())
				v |= SWT.ITALIC;
			getPreferenceStore().setValue(stylePrefName(), v);
		}
	}

	@Override
	protected void createFieldEditors() {
		try {
			final ColorManager manager = ColorManager.INSTANCE;
			for (final SyntaxElementStyle syntaxElement : manager.syntaxElementStyles.values()) {
				PreferenceConverter.setDefault(getPreferenceStore(),
					syntaxElement.prefName(SyntaxElementStyle.RGB),
					syntaxElement.defaultRGB
				);
				addField(new StyleEditor(syntaxElement, getFieldEditorParent()));
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void init(final IWorkbench workbench) {
	}

	@Override
	public boolean performOk() {
		if (super.performOk()) {
			ScannerPerEngine.refreshScanners();
			for (final StructureTextEditor part : EditorUtil.clonkTextEditors(StructureTextEditor.class, false))
				part.reconfigureSourceViewer();
			return true;
		} else
			return false;
	}

}
