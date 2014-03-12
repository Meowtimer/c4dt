package net.arctics.clonk.ui;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import net.arctics.clonk.Problem;
import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.builder.ProjectSettings;
import net.arctics.clonk.preferences.ClonkPreferencePage;
import net.arctics.clonk.preferences.Messages;
import net.arctics.clonk.util.StringUtil;
import net.arctics.clonk.util.UI;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPropertyPage;

public class ClonkProjectProperties extends FieldEditorPreferencePage implements IWorkbenchPropertyPage {

	private static final String ENGINENAME_PROPERTY = "engineName"; //$NON-NLS-1$
	private static final String DISABLED_ERRORS_PROPERTY = "disabledErrors"; //$NON-NLS-1$

	public ClonkProjectProperties() {
		super(GRID);
	}

	private final class DisabledErrorsFieldEditor extends FieldEditor implements ICheckStateListener, ICheckStateProvider {
		HashSet<Problem> disabledErrorCodes = new HashSet<Problem>();
		private CheckboxTableViewer tableViewer;
		private Table table;
		private Text filterBox;

		private DisabledErrorsFieldEditor(final String name, final String labelText,
			final Composite parent) {
			super(name, labelText, parent);
		}

		@Override
		public int getNumberOfControls() {
			return 3;
		}

		@Override
		protected void doStore() {
			ClonkProjectNature.get(getProject()).settings().setDisabledErrorsSet(disabledErrorCodes);
		}

		@Override
		protected void doLoadDefault() {
			doLoad();
		}

		@Override
		protected void doLoad() {
			disabledErrorCodes = new HashSet<Problem>(ClonkProjectNature.get(getProject()).settings().disabledErrorsSet());
			if (tableViewer != null)
			for (final Problem c : Problem.values())
				tableViewer.setChecked(c, !disabledErrorCodes.contains(c));
		}

		private Table getTable(final Composite parent) {
			if (table == null) {
				filterBox = new Text(parent, SWT.SEARCH | SWT.CANCEL);
				filterBox.addModifyListener(e -> tableViewer.refresh());
				final ViewerFilter filter = new ViewerFilter() {
					@Override
					public boolean select(final Viewer viewer, final Object parentElement, final Object element) {
						final String text = ((ILabelProvider)tableViewer.getLabelProvider()).getText(element);
						return StringUtil.patternFromRegExOrWildcard(filterBox.getText()).matcher(text).find();
					}
				};
				table = new Table(parent, SWT.CHECK | SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
				tableViewer = new CheckboxTableViewer(table);
				tableViewer.addFilter(filter);
				tableViewer.setContentProvider(new IStructuredContentProvider() {
					@Override
					public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
					@Override
					public void dispose() {}
					@Override
					public Problem[] getElements(final Object inputElement) {
						if (inputElement == Problem.class) {
							final Problem[] elms = Problem.values().clone();
							Arrays.sort(elms, (o1, o2) -> o1.messageWithFormatArgumentDescriptions().compareTo(o2.messageWithFormatArgumentDescriptions()));
							return elms;
						}
						return null;
					}
				});
				tableViewer.setLabelProvider(new LabelProvider() {
					@Override
					public String getText(final Object element) {
						return ((Problem) element).messageWithFormatArgumentDescriptions();
					}
				});
				tableViewer.addCheckStateListener(this);
				tableViewer.setCheckStateProvider(this);
				tableViewer.setInput(Problem.class);
			}
			return table;
		}

		@Override
		protected void doFillIntoGrid(final Composite parent, final int numColumns) {
			final Label label = getLabelControl(parent);
			label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
			final Table table = getTable(parent);
			GridData gd = new GridData();
			gd.heightHint = 0;
			gd.widthHint = SWT.DEFAULT;
			gd.horizontalSpan = numColumns - 1;
			gd.grabExcessHorizontalSpace = true;
			gd.horizontalAlignment = SWT.FILL;
			gd.verticalAlignment = SWT.FILL;
			gd.grabExcessVerticalSpace = true;
		    table.setLayoutData(gd);
		    gd = new GridData();
		    gd.widthHint = SWT.DEFAULT;
		    gd.grabExcessHorizontalSpace = true;
		    gd.horizontalAlignment = SWT.FILL;
		    filterBox.setLayoutData(gd);
		}

		@Override
		protected void adjustForNumColumns(final int numColumns) {
			// yup
		}

		@Override
		public boolean isGrayed(final Object element) {
			return false;
		}
		@Override
		public boolean isChecked(final Object element) {
			return disabledErrorCodes != null && !disabledErrorCodes.contains(element);
		}

		@Override
		public void checkStateChanged(final CheckStateChangedEvent event) {
			if (event.getChecked())
				disabledErrorCodes.remove(event.getElement());
			else
				disabledErrorCodes.add((Problem) event.getElement());
		}
	}

	private final class AdapterStore extends PreferenceStore {
		private final Map<String, String> values = new HashMap<String, String>();

		@Override
		public String getDefaultString(final String name) {
			return ""; //$NON-NLS-1$
		}

		@Override
		public void setValue(final String name, final String value) {
			values.put(name, value);
			commit(name, value);
		}

		@Override
		public void setValue(final String name, final boolean value) {
			setValue(name, String.valueOf(value));
		}

		@Override
		public boolean getBoolean(final String name) {
			return Boolean.parseBoolean(values.get(name));
		}

		@Override
		public String getString(final String name) {
			final String v = values.get(name);
			return v != null ? v : getDefaultString(name);
		}

		public void commit(final String n, final String v) {
			final ProjectSettings settings = getSettings();
			if (n.equals(ENGINENAME_PROPERTY)) {
				settings.setEngineName(v);
				UI.refreshAllProjectExplorers(getProject());
			}
			else if (n.equals(DISABLED_ERRORS_PROPERTY))
				settings.setDisabledErrors(v);
		}

		private ProjectSettings getSettings() {
			return ClonkProjectNature.get(getProject()).settings();
		}

		public AdapterStore() throws CoreException {
			values.put(ENGINENAME_PROPERTY, getSettings().engineName());
			values.put(DISABLED_ERRORS_PROPERTY, getSettings().disabledErrorsString());
		}
	}

	private IAdaptable element;
	private AdapterStore adapterStore;

	private IProject getProject() {
		return (IProject) element.getAdapter(IProject.class);
	}

	@Override
	public IPreferenceStore getPreferenceStore() {
		return adapterStore;
	}

	@Override
	protected void createFieldEditors() {
		addField(new ComboFieldEditor(ENGINENAME_PROPERTY, Messages.ClonkProjectProperties_SpecifiedEngine, ClonkPreferencePage.engineComboValues(true), getFieldEditorParent()));
		addField(new DisabledErrorsFieldEditor(DISABLED_ERRORS_PROPERTY, Messages.EnabledErrors, getFieldEditorParent()));
	}

	@Override
	public IAdaptable getElement() {
		return element;
	}

	@Override
	public void setElement(final IAdaptable element) {
		this.element = element;
		try {
			this.adapterStore = new AdapterStore();
		} catch (final CoreException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean performOk() {
		if (super.performOk()) {
			ClonkProjectNature.get(getProject()).saveSettings();
			return true;
		} else
			return false;
	}

}