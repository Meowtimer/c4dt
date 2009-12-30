package net.arctics.clonk.ui;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.c4script.C4Function;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.c4script.C4Type;
import net.arctics.clonk.parser.c4script.C4Variable;
import net.arctics.clonk.parser.c4script.C4Function.C4FunctionScope;
import net.arctics.clonk.parser.c4script.C4Variable.C4VariableScope;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.ui.navigator.ClonkOutlineProvider;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.part.*;
import org.eclipse.ui.progress.IProgressService;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.*;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.SWT;

public class EngineDeclarationsView extends ViewPart implements IPropertyChangeListener {
	
	protected class EditDeclarationInputDialog extends Dialog {
		
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
		private C4Declaration declaration;
		private Text declarationNameField;
		private Text descriptionField;
		private Combo returnTypeBox;
		private Combo scopeBox;
		private List<ParameterCombination> parameters = new ArrayList<ParameterCombination>();
		
		/**
		 * Edits the currently selected identifer
		 * @param parent
		 */
		public EditDeclarationInputDialog(Shell parent) {
			super(parent);
		}
		
		/**
		 * Edits the specified declaration
		 * @param parent
		 * @param declaration
		 */
		public EditDeclarationInputDialog(Shell parent, C4Declaration declaration) {
			super(parent);
			this.declaration = declaration;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
		 */
		@Override
		protected Control createDialogArea(Composite parent) {
			
			Composite composite = (Composite) super.createDialogArea(parent);
			
			composite.setLayout(new GridLayout(2,false));
			if (declaration == null) {
				Object activeElement = getActiveElement();
				if (!(activeElement instanceof C4Declaration)) {
					return null;
				}
				declaration = (C4Declaration) activeElement;
			}
			
			if (declaration instanceof C4Function) {
				createFunctionEditDialog(composite, (C4Function) declaration);
			}
			else if (declaration instanceof C4Variable) {
				createVariableEditDialog(composite, (C4Variable) declaration);
			}
			
			return composite;
		}
		
		private void createNewParameterButton(final Composite parent) {
			if (newParameter != null) {
				newParameter.dispose();
				newParameter = null;
			}
			newParameter = new Button(parent, SWT.PUSH);
			newParameter.setText(Messages.Engine_NewParameter);
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
			if (declaration instanceof C4Function) {
				C4Function func = (C4Function) declaration;
				func.setName(declarationNameField.getText());
				func.setReturnType(C4Type.makeType(returnTypeBox.getItem(returnTypeBox.getSelectionIndex())));
				func.setVisibility(C4FunctionScope.makeScope(scopeBox.getItem(scopeBox.getSelectionIndex())));
				func.setUserDescription(descriptionField.getText());
				
				func.getParameters().clear();
				for(ParameterCombination par : parameters) {
					C4Variable var = new C4Variable(par.getName().getText(),C4VariableScope.VAR_LOCAL);
					var.forceType(getSelectedType(par.getType()));
					func.getParameters().add(var);
				}
			}
			else if (declaration instanceof C4Variable) {
				C4Variable var = (C4Variable) declaration;
				var.setName(declarationNameField.getText());
				var.forceType(C4Type.makeType(returnTypeBox.getItem(returnTypeBox.getSelectionIndex()), true));
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
			parent.getShell().setText(String.format(Messages.Engine_EditVariable, var.getName()));
			
			new Label(parent, SWT.NONE).setText(Messages.Engine_NameTitle);
			
			declarationNameField = new Text(parent, SWT.BORDER | SWT.SINGLE);
			declarationNameField.setText(var.getName());
			
			new Label(parent, SWT.NONE).setText(Messages.Engine_TypeTitle);
			returnTypeBox = createComboBoxForType(parent, var.getType());
		
			new Label(parent, SWT.NONE).setText(Messages.Engine_ScopeTitle);
			scopeBox = createComboBoxForScope(parent, var.getScope());
		}

		private void createFunctionEditDialog(Composite parent, C4Function func) {
			
			// set title
			parent.getShell().setText(String.format(Messages.Engine_EditFunction, func.getName()));
			
			new Label(parent, SWT.NONE).setText(Messages.Engine_NameTitle);
			
			declarationNameField = new Text(parent, SWT.BORDER | SWT.SINGLE);
			declarationNameField.setText(func.getName());
			
			new Label(parent, SWT.NONE).setText(Messages.Engine_ReturnTypeTitle);
			returnTypeBox = createComboBoxForType(parent, func.getReturnType());
			
			new Label(parent, SWT.NONE).setText(Messages.Engine_ScopeTitle);
			scopeBox = createComboBoxForScope(parent, func.getVisibility());
			
			new Label(parent, SWT.NONE).setText(Messages.Engine_DescriptionTitle);
			descriptionField = new Text(parent, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
			if (func.getUserDescription() != null) descriptionField.setText(func.getUserDescription());
			
			GridData gridData =
			      new GridData(
			        GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL);
			    gridData.horizontalSpan = 3;
			    gridData.grabExcessVerticalSpace = true;

			descriptionField.setSize(400, 100);
			descriptionField.setLayoutData(gridData);
			
			new Label(parent, SWT.NONE).setText(" "); // placeholder //$NON-NLS-1$
			new Label(parent, SWT.NONE).setText(" "); //$NON-NLS-1$
			if (func.getParameters() != null) {
				for(C4Variable par : func.getParameters()) {
					createParameterControls(parent, par.getType(), par.getName());
				}
			}
			
			createNewParameterButton(parent);
			
		}
		
		private void createParameterControls(Composite parent) {
			createParameterControls(parent, C4Type.ANY, ""); //$NON-NLS-1$
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
	private Action importFromRepoAction;
	private Action reloadAction;
	private Action exportXMLAction;

	/**
	 * The constructor.
	 */
	public EngineDeclarationsView() {
		ClonkCore.getDefault().getPreferenceStore().addPropertyChangeListener(this);
	}

	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		drillDownAdapter = new DrillDownAdapter(viewer);
		
		ClonkOutlineProvider provider = new ClonkOutlineProvider();
		viewer.setContentProvider(provider);
		viewer.setLabelProvider(provider);
		viewer.setSorter(new ViewerSorter() {
			public int category(Object element) {
				return ((C4Declaration)element).sortCategory();
			}
		});
		refresh();
		
		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
		contributeToActionBars();
	}

	/**
	 * Refreshes this viewer completely with information freshly obtained from this viewer's model.
	 */
	public void refresh() {
		viewer.setInput(ClonkCore.getDefault().getActiveEngine());
		//viewer.refresh();
	}
	
	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				EngineDeclarationsView.this.fillContextMenu(manager);
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
		manager.add(new Separator());
		drillDownAdapter.addNavigationActions(manager);
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}
	
	private void fillLocalToolBar(IToolBarManager manager) {
//		manager.add(editAction);
		manager.add(addFunctionAction);
		manager.add(addVariableAction);
		manager.add(new Separator());
		manager.add(importFromRepoAction);
		manager.add(reloadAction);
		manager.add(new Separator());
		manager.add(saveAction);
		manager.add(exportXMLAction);
		manager.add(new Separator());
		drillDownAdapter.addNavigationActions(manager);
	}

	private void makeActions() {
		addFunctionAction = new Action() {
			public void run() {
				C4Function func = new C4Function();
				Dialog dialog = new EditDeclarationInputDialog(viewer.getControl().getShell(),func);
				dialog.create();
				dialog.getShell().setSize(400,600);
				dialog.getShell().pack();
				if (dialog.open() == Dialog.OK) {
					ClonkCore.getDefault().getActiveEngine().addDeclaration(func);
				}
				refresh();
			}
		};
		addFunctionAction.setText(Messages.Engine_AddFunction);
		addFunctionAction.setToolTipText(Messages.Engine_AddFunctionDesc);
		
		addVariableAction = new Action() {
			public void run() {
				C4Variable var = new C4Variable();
				Dialog dialog = new EditDeclarationInputDialog(viewer.getControl().getShell(),var);
				dialog.create();
				dialog.getShell().setSize(400,600);
				dialog.getShell().pack();
				if (dialog.open() == Dialog.OK) {
					ClonkCore.getDefault().getActiveEngine().addDeclaration(var);
				}
				refresh();
			}
		};
		addVariableAction.setText(Messages.Engine_AddVariable);
		addVariableAction.setToolTipText(Messages.Engine_AddVariableDesc);
		
		editAction = new Action() {
			public void run() {
//				Tree tree = viewer.getTree();
//				TreeItem item = tree.getSelection()[0];
//				Object data = item.getData();
//				viewer.editElement(data, 0);
//				viewer.editElement(((IStructuredSelection)viewer.getSelection()).getFirstElement(), 0);
				Dialog dialog = new EditDeclarationInputDialog(viewer.getControl().getShell());
				dialog.create();
				dialog.getShell().setSize(400, 600);
				dialog.getShell().pack();
				dialog.open();
				refresh();
			}
		};
		editAction.setText(Messages.Engine_Edit);
		editAction.setToolTipText(Messages.Engine_EditDesc);
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
						if (selectedItem instanceof C4Declaration) {
							ClonkCore.getDefault().getActiveEngine().removeDeclaration((C4Declaration) selectedItem);
						}
					}
					refresh();
				}
			}
		};
		deleteAction.setText(Messages.Engine_Delete);
		deleteAction.setToolTipText(Messages.Engine_DeleteDesc);
		deleteAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));
		
		saveAction = new Action() {
			public void run() {
				InputDialog dialog = new InputDialog(
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
						Messages.SpecifyEngineName, Messages.SpecifyEngineNameDesc,
						ClonkCore.getDefault().getActiveEngine().getName(),
						null
				);
				switch (dialog.open()) {				
				case Window.OK:
					ClonkCore.getDefault().saveEngineInWorkspace(dialog.getValue());
					break;
				}
			}
		};
		saveAction.setText(Messages.Engine_Save);
		saveAction.setToolTipText(Messages.Engine_SaveTitle);
		saveAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ETOOL_SAVE_EDIT));
		
		doubleClickAction = editAction;
		
		importFromRepoAction = new Action() {
			@Override
			public void run() {
				IProgressService ps = PlatformUI.getWorkbench().getProgressService();
				try {
					final String repo = ClonkCore.getDefault().getPreferenceStore().getString(ClonkPreferences.OPENCLONK_REPO);
					if (repo == null) {
						MessageDialog.openWarning(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
							Messages.Engine_NoRepository, Messages.Engine_NoRepositoryDesc);
					}
					else ps.busyCursorWhile(new IRunnableWithProgress() {
						public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
							try {
								final C4ScriptBase engine = ClonkCore.getDefault().getActiveEngine();
								engine.clearDeclarations();
								engine.importFromRepository(repo, monitor);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					});
					refresh();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		importFromRepoAction.setToolTipText(Messages.Engine_ImportFromRepoDesc);
		importFromRepoAction.setText(Messages.Engine_ImportFromRepo);
		
		reloadAction = new Action() {
			@Override
			public void run() {
			    try {
	                ClonkCore.getDefault().loadActiveEngine();
                } catch (Exception e) { 
	                e.printStackTrace();
                }
			    refresh();
			}
		};
		reloadAction.setToolTipText(Messages.Engine_ReloadDesc);
		reloadAction.setText(Messages.Engine_Reload);
		
		exportXMLAction = new Action() {
			@Override
			public void run() {
				InputDialog dialog = new InputDialog(
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
						Messages.SpecifyEngineName, Messages.SpecifyEngineNameDesc,
						ClonkCore.getDefault().getActiveEngine().getName(),
						null
				);
				switch (dialog.open()) {				
				case Window.OK:
					ClonkCore.getDefault().exportEngineToXMLInWorkspace(dialog.getValue());
					break;
				}
			}
		};
		exportXMLAction.setToolTipText("Export to XML");
		exportXMLAction.setText("Export to XML");
		
	}

	private void hookDoubleClickAction() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
	}
//	private void showMessage(String message) {
//		MessageDialog.openInformation(
//			viewer.getControl().getShell(),
//			"Engine identifiers",
//			message);
//	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(ClonkPreferences.ACTIVE_ENGINE)) {
			refresh();
		}
	}
	
	@Override
	public void dispose() {
		ClonkCore.getDefault().getPreferenceStore().removePropertyChangeListener(this);
	}
	
}