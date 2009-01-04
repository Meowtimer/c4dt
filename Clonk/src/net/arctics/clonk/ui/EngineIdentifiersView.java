package net.arctics.clonk.ui;

import java.util.ArrayList;
import java.util.List;
import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.C4Field;
import net.arctics.clonk.parser.C4Function;
import net.arctics.clonk.parser.C4Type;
import net.arctics.clonk.parser.C4Variable;
import net.arctics.clonk.parser.C4Function.C4FunctionScope;
import net.arctics.clonk.parser.C4Variable.C4VariableScope;
import net.arctics.clonk.ui.editors.ClonkContentOutlineLabelAndContentProvider;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.part.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.*;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.SWT;


/**
 * This sample class demonstrates how to plug-in a new
 * workbench view. The view shows data obtained from the
 * model. The sample creates a dummy model on the fly,
 * but a real implementation would connect to the model
 * available either in this or another plug-in (e.g. the workspace).
 * The view is connected to the model using a content provider.
 * <p>
 * The view uses a label provider to define how model
 * objects should be presented in the view. Each
 * view can present the same model objects using
 * different labels and icons, if needed. Alternatively,
 * a single label provider can be shared between views
 * in order to ensure that objects of the same type are
 * presented in the same way everywhere.
 * <p>
 */

public class EngineIdentifiersView extends ViewPart {
	
	protected class EditIdentifierInputDialog extends Dialog {
		
		private class ParameterCombination {
			private Combo type;
			private Text name;
			
			public ParameterCombination(Combo type, Text name) {
				this.type = type;
				this.name = name;
			}

			/**
			 * @return the type
			 */
			public Combo getType() {
				return type;
			}

			/**
			 * @return the name
			 */
			public Text getName() {
				return name;
			}
		}
		
		private Button newParameter;
		private C4Field identifier;
		private Text identifierNameField;
		private Text descriptionField;
		private Combo returnTypeBox;
		private Combo scopeBox;
		private List<ParameterCombination> parameters = new ArrayList<ParameterCombination>();
		
		/**
		 * Edits the currently selected identifer
		 * @param parent
		 */
		public EditIdentifierInputDialog(Shell parent) {
			super(parent);
		}
		
		/**
		 * Edits the specified identifier
		 * @param parent
		 * @param identifier
		 */
		public EditIdentifierInputDialog(Shell parent, C4Field identifier) {
			super(parent);
			this.identifier = identifier;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
		 */
		@Override
		protected Control createDialogArea(Composite parent) {
			
			Composite composite = (Composite) super.createDialogArea(parent);
			
			composite.setLayout(new GridLayout(2,false));
			if (identifier == null) {
				Object activeElement = getActiveElement();
				if (!(activeElement instanceof C4Field)) {
					return null;
				}
				identifier = (C4Field) activeElement;
			}
			
			if (identifier instanceof C4Function) {
				createFunctionEditDialog(composite, (C4Function) identifier);
			}
			else if (identifier instanceof C4Variable) {
				createVariableEditDialog(composite, (C4Variable) identifier);
			}
			
			return composite;
		}
		
		private void createNewParameterButton(final Composite parent) {
			if (newParameter != null) {
				newParameter.dispose();
				newParameter = null;
			}
			newParameter = new Button(parent, SWT.PUSH);
			newParameter.setText("New Parameter");
			newParameter.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					newParameter.dispose();
					newParameter = null;

					createParameterControls(parent);
					createNewParameterButton(parent);

					parent.layout(true); // show new controls
					parent.pack(true); // resize sub composite
					parent.getParent().pack(true); // resize composite
					parent.getParent().getParent().pack(true); // resize window 
				}
			});
		}
		
		@Override
		protected void okPressed() {
			if (identifier instanceof C4Function) {
				C4Function func = (C4Function) identifier;
				func.setName(identifierNameField.getText());
				func.setReturnType(C4Type.makeType(returnTypeBox.getItem(returnTypeBox.getSelectionIndex())));
				func.setVisibility(C4FunctionScope.makeScope(scopeBox.getItem(scopeBox.getSelectionIndex())));
				func.setUserDescription(descriptionField.getText());
				
				func.getParameters().clear();
				for(ParameterCombination par : parameters) {
					C4Variable var = new C4Variable(par.getName().getText(),C4VariableScope.VAR_LOCAL);
					var.setType(getSelectedType(par.getType()));
					func.getParameters().add(var);
				}
			}
			else if (identifier instanceof C4Variable) {
				C4Variable var = (C4Variable) identifier;
				var.setName(identifierNameField.getText());
				var.setType(C4Type.makeType(returnTypeBox.getItem(returnTypeBox.getSelectionIndex())));
				var.setScope(C4VariableScope.valueOf(scopeBox.getItem(scopeBox.getSelectionIndex())));
			}
			
			super.okPressed();
		}
		
		private C4Type getSelectedType(Combo combo) {
			return C4Type.makeType(combo.getItem(combo.getSelectionIndex()));
		}

		private void createVariableEditDialog(Composite parent,
				C4Variable var) {
			// set title
			parent.getShell().setText("Edit variable " + var.getName());
			
			new Label(parent, SWT.NONE).setText("Name: ");
			
			identifierNameField = new Text(parent, SWT.BORDER | SWT.SINGLE);
			identifierNameField.setText(var.getName());
			
			new Label(parent, SWT.NONE).setText("Type: ");
			returnTypeBox = createComboBoxForType(parent, var.getType());
		
			new Label(parent, SWT.NONE).setText("Scope: ");
			scopeBox = createComboBoxForScope(parent, var.getScope());
		}

		private void createFunctionEditDialog(Composite parent, C4Function func) {
			
			// set title
			parent.getShell().setText("Edit function " + func.getName());
			
			new Label(parent, SWT.NONE).setText("Name: ");
			
			identifierNameField = new Text(parent, SWT.BORDER | SWT.SINGLE);
			identifierNameField.setText(func.getName());
			
			new Label(parent, SWT.NONE).setText("Return type: ");
			returnTypeBox = createComboBoxForType(parent, func.getReturnType());
			
			new Label(parent, SWT.NONE).setText("Scope: ");
			scopeBox = createComboBoxForScope(parent, func.getVisibility());
			
			new Label(parent, SWT.NONE).setText("Beschreibung: ");
			descriptionField = new Text(parent, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
			if (func.getUserDescription() != null) descriptionField.setText(func.getUserDescription());
			
			GridData gridData =
			      new GridData(
			        GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL);
			    gridData.horizontalSpan = 3;
			    gridData.grabExcessVerticalSpace = true;

			descriptionField.setSize(400, 100);
			descriptionField.setLayoutData(gridData);
			
			new Label(parent, SWT.NONE).setText(" "); // placeholder
			new Label(parent, SWT.NONE).setText(" ");
			if (func.getParameters() != null) {
				for(C4Variable par : func.getParameters()) {
					createParameterControls(parent, par.getType(), par.getName());
				}
			}
			
			createNewParameterButton(parent);
			
		}
		
		private void createParameterControls(Composite parent) {
			createParameterControls(parent, C4Type.ANY, "");
		}
		
		private void createParameterControls(Composite parent, C4Type type, String parameterName) {
			Combo combo = createComboBoxForType(parent, type);
			Text parNameField = new Text(parent, SWT.BORDER | SWT.SINGLE);
			parNameField.setText(parameterName);
			parameters.add(new ParameterCombination(combo, parNameField));
		}
		
		private Combo createComboBoxForScope(Composite parent, Object scope) {
			Object[] values = null;
			if (scope instanceof C4VariableScope) {
				values = C4VariableScope.values();
			}
			else if (scope instanceof C4FunctionScope) {
				values = C4FunctionScope.values();
			}
			Combo combo = new Combo(parent, SWT.READ_ONLY);
			int select = 0;
			List<String> items = new ArrayList<String>(values.length);
			for(int i = 0; i < values.length;i++) {
				items.add(values[i].toString());
				if (scope == values[i])
					select = i;
			}
			combo.setItems(items.toArray(new String[items.size()]));
			combo.select(select);	
			
			return combo;
		}
		
		private Combo createComboBoxForType(Composite parent, C4Type currentType) {
			Combo combo = new Combo(parent, SWT.READ_ONLY);
			int select = 0;
			List<String> items = new ArrayList<String>(C4Type.values().length);
			for(int i = 0; i < C4Type.values().length;i++) {
				items.add(C4Type.values()[i].toString());
				if (currentType == C4Type.values()[i])
					select = i;
			}
			combo.setItems(items.toArray(new String[items.size()]));
			combo.select(select);	
			
			return combo;
		}
		
		private Object getActiveElement() {
			return viewer.getTree().getSelection()[0].getData();
		}
	}
	
	protected TreeViewer viewer;
	private DrillDownAdapter drillDownAdapter;
	private Action editAction;
	private Action deleteAction;
	private Action addFunctionAction;
	private Action addVariableAction;
	private Action saveAction;
	private Action doubleClickAction;

	/**
	 * The constructor.
	 */
	public EngineIdentifiersView() {
	}

	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		drillDownAdapter = new DrillDownAdapter(viewer);
		
		ClonkContentOutlineLabelAndContentProvider provider = new ClonkContentOutlineLabelAndContentProvider();
		viewer.setContentProvider(provider);
		viewer.setLabelProvider(provider);
		viewer.setSorter(new ViewerSorter() {
			public int category(Object element) {
				return ((C4Field)element).sortCategory();
			}
		});
		viewer.setInput(ClonkCore.ENGINE_OBJECT);
		
		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
		contributeToActionBars();
	}

	/**
	 * Refreshes this viewer completely with information freshly obtained from this viewer's model.
	 */
	public void refresh() {
		viewer.refresh();
	}
	
	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				EngineIdentifiersView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(IMenuManager manager) {
		manager.add(addFunctionAction);
		manager.add(addVariableAction);
		manager.add(new Separator());
		manager.add(saveAction);
	}

	private void fillContextMenu(IMenuManager manager) {
		manager.add(editAction);
		manager.add(deleteAction);
//		manager.add(saveAction);
		manager.add(new Separator());
		drillDownAdapter.addNavigationActions(manager);
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}
	
	private void fillLocalToolBar(IToolBarManager manager) {
//		manager.add(editAction);
		manager.add(addFunctionAction);
		manager.add(addVariableAction);
		manager.add(saveAction);
		manager.add(new Separator());
		drillDownAdapter.addNavigationActions(manager);
	}

	private void makeActions() {
		addFunctionAction = new Action() {
			public void run() {
				C4Function func = new C4Function();
				Dialog dialog = new EditIdentifierInputDialog(viewer.getControl().getShell(),func);
				dialog.create();
				dialog.getShell().setSize(400,600);
				dialog.getShell().pack();
				if (dialog.open() == Dialog.OK) {
					ClonkCore.ENGINE_OBJECT.addField(func);
				}
				refresh();
			}
		};
		addFunctionAction.setText("Add Function");
		addFunctionAction.setToolTipText("Adds a new function to engine index");
		
		addVariableAction = new Action() {
			public void run() {
				C4Variable var = new C4Variable();
				Dialog dialog = new EditIdentifierInputDialog(viewer.getControl().getShell(),var);
				dialog.create();
				dialog.getShell().setSize(400,600);
				dialog.getShell().pack();
				if (dialog.open() == Dialog.OK) {
					ClonkCore.ENGINE_OBJECT.addField(var);
				}
				refresh();
			}
		};
		addVariableAction.setText("Add Variable");
		addVariableAction.setToolTipText("Adds a new variable to engine index");
		
		editAction = new Action() {
			public void run() {
//				Tree tree = viewer.getTree();
//				TreeItem item = tree.getSelection()[0];
//				Object data = item.getData();
//				viewer.editElement(data, 0);
//				viewer.editElement(((IStructuredSelection)viewer.getSelection()).getFirstElement(), 0);
				Dialog dialog = new EditIdentifierInputDialog(viewer.getControl().getShell());
				dialog.create();
				dialog.getShell().setSize(400, 600);
				dialog.getShell().pack();
				dialog.open();
				refresh();
			}
		};
		editAction.setText("Edit");
		editAction.setToolTipText("Edit this identifier");
		editAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
		
		deleteAction = new Action() {
			public void run() {
				Dialog dialog = new SimpleConfirmDialog(viewer.getControl().getShell());
				int result = dialog.open();
				if (result == IDialogConstants.OK_ID) {
					TreeItem[] selection = viewer.getTree().getSelection();
					for (TreeItem t : selection) {
						Object selectedItem = t.getData();
						if (selectedItem instanceof C4Field) {
							ClonkCore.ENGINE_OBJECT.removeField((C4Field) selectedItem);
						}
					}
					refresh();
				}
			}
		};
		deleteAction.setText("Delete");
		deleteAction.setToolTipText("Delete this identifier");
		deleteAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));
		
		saveAction = new Action() {
			public void run() {
				ClonkCore.saveEngineObject();
			}
		};
		saveAction.setText("Save");
		saveAction.setToolTipText("Save changes");
		saveAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_ETOOL_SAVE_EDIT));
		doubleClickAction = new Action() {
			public void run() {
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();
				showMessage("Double-click detected on "+obj.toString());
			}
		};
	}

	private void hookDoubleClickAction() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
	}
	private void showMessage(String message) {
		MessageDialog.openInformation(
			viewer.getControl().getShell(),
			"Engine identifiers",
			message);
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}
}