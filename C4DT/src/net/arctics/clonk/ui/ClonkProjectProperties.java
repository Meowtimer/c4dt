package net.arctics.clonk.ui;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.preferences.ClonkPreferencePage;
import net.arctics.clonk.preferences.Messages;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.resource.ProjectSettings;
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
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
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
		HashSet<ParserErrorCode> disabledErrorCodes = new HashSet<ParserErrorCode>();
		private CheckboxTableViewer tableViewer;
		private Table table;
		private Text filterBox;

		private DisabledErrorsFieldEditor(String name, String labelText,
			Composite parent) {
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

		@SuppressWarnings("unchecked")
		@Override
		protected void doLoad() {
			disabledErrorCodes = (HashSet<ParserErrorCode>) ClonkProjectNature.get(getProject()).settings().getDisabledErrorsSet().clone();
			if (tableViewer != null)
			for (ParserErrorCode c : ParserErrorCode.values())
				tableViewer.setChecked(c, !disabledErrorCodes.contains(c));
		}

		private Table getTable(Composite parent) {
			if (table == null) {
				filterBox = new Text(parent, SWT.SEARCH | SWT.CANCEL);
				filterBox.addModifyListener(new ModifyListener() {
					@Override
					public void modifyText(ModifyEvent e) {
						tableViewer.refresh();
					}
				});
				ViewerFilter filter = new ViewerFilter() {
					@Override
					public boolean select(Viewer viewer, Object parentElement, Object element) {
						String text = ((LabelProvider)tableViewer.getLabelProvider()).getText(element);
						return StringUtil.patternFromRegExOrWildcard(filterBox.getText()).matcher(text).find();
					}
				};
				table = new Table(parent, SWT.CHECK | SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
				tableViewer = new CheckboxTableViewer(table);
				tableViewer.addFilter(filter);
				tableViewer.setContentProvider(new IStructuredContentProvider() {
					@Override
					public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
					@Override
					public void dispose() {}
					@Override
					public Object[] getElements(Object inputElement) {
						if (inputElement == ParserErrorCode.class) {
							ParserErrorCode[] elms = ParserErrorCode.values().clone();
							Arrays.sort(elms, new Comparator<ParserErrorCode>() {
								@Override
								public int compare(ParserErrorCode o1, ParserErrorCode o2) {
									return o1.messageWithFormatArgumentDescriptions().compareTo(o2.messageWithFormatArgumentDescriptions());
								}
							});
							return elms;
						}
						return null;
					}
				});
				tableViewer.setLabelProvider(new LabelProvider() {
					@Override
					public String getText(Object element) {
						return ((ParserErrorCode)element).messageWithFormatArgumentDescriptions();
					}
				});
				tableViewer.addCheckStateListener(this);
				tableViewer.setCheckStateProvider(this);
				tableViewer.setInput(ParserErrorCode.class);
			}
			return table;
		}

		@Override
		protected void doFillIntoGrid(Composite parent, int numColumns) {
			Label label = getLabelControl(parent);
			label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
			Table table = getTable(parent);
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
		protected void adjustForNumColumns(int numColumns) {
			// yup
		}

		@Override
		public boolean isGrayed(Object element) {
			return false;
		}
		@Override
		public boolean isChecked(Object element) {
			return disabledErrorCodes != null && !disabledErrorCodes.contains(element);
		}

		@Override
		public void checkStateChanged(CheckStateChangedEvent event) {
			if (event.getChecked())
				disabledErrorCodes.remove(event.getElement());
			else
				disabledErrorCodes.add((ParserErrorCode) event.getElement());
		}
	}

	private final class AdapterStore extends PreferenceStore {
		private final Map<String, String> values = new HashMap<String, String>();

		@Override
		public String getDefaultString(String name) {
			return ""; //$NON-NLS-1$
		}

		@Override
		public void setValue(String name, String value) {
			values.put(name, value);
			commit(name, value);
		}
		
		@Override
		public void setValue(String name, boolean value) {
			setValue(name, String.valueOf(value));
		}
		
		@Override
		public boolean getBoolean(String name) {
			return Boolean.parseBoolean(values.get(name));
		}

		@Override
		public String getString(String name) {
			String v = values.get(name);
			return v != null ? v : getDefaultString(name);
		}

		public void commit(String n, String v) {
			ProjectSettings settings = getSettings();
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
			values.put(ENGINENAME_PROPERTY, getSettings().getEngineName());
			values.put(DISABLED_ERRORS_PROPERTY, getSettings().getDisabledErrors());
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
	public void setElement(IAdaptable element) {
		this.element = element;
		try {
			this.adapterStore = new AdapterStore();
		} catch (CoreException e) {
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