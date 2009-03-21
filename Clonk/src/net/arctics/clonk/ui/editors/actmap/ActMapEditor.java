package net.arctics.clonk.ui.editors.actmap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.texteditor.IDocumentProvider;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.C4Object;
import net.arctics.clonk.parser.actmap.ActMapParser;
import net.arctics.clonk.parser.inireader.IniReader;
import net.arctics.clonk.ui.editors.ini.IniEditor;
import net.arctics.clonk.ui.editors.ini.IniEditorColumnLabelProvider;
import net.arctics.clonk.ui.editors.ini.IniEditorColumnLabelProvider.WhatLabel;
import net.arctics.clonk.util.Utilities;

public class ActMapEditor extends IniEditor {
	
	public ActMapEditor() {
	}

	public static class ActMapSectionPage extends IniSectionPage {
		
		private ActMapParser parser;
		
		public ActMapSectionPage(FormEditor editor, String id, String title, IDocumentProvider docProvider) {
			super(editor, id, title, docProvider);
			parser = new ActMapParser(Utilities.getEditingFile(getEditor()));
		}

		protected void createFormContent(IManagedForm managedForm) {
			super.createFormContent(managedForm);

			FormToolkit toolkit = managedForm.getToolkit();
			ScrolledForm form = managedForm.getForm();
			toolkit.decorateFormHeading(form.getForm());
			
			form.setText("ActMap options");
			IFile input = Utilities.getEditingFile(getEditor());
			if (input != null) {
				try { // XXX values should come from document - not from builder cache
					//IContainer cont = input.getParent();
					C4Object obj = (C4Object) input.getParent().getSessionProperty(ClonkCore.C4OBJECT_PROPERTY_ID);
					if (obj != null) {
						form.setText(obj.getName() + "(" + obj.getId().getName() + ") ActMap");
					}
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}

			Layout layout = new GridLayout(1, true);
			form.getBody().setLayout(layout);
			form.getBody().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			parser.parse();

			SectionPart part = new SectionPart(form.getBody(), toolkit,Section.CLIENT_INDENT | Section.TITLE_BAR | Section.EXPANDED);
			part.getSection().setText("Actions");
			part.getSection().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			Composite sectionComp = toolkit.createComposite(part.getSection());
			sectionComp.setLayout(new FillLayout());
			//sectionComp.setLayoutData(new GridData(GridData.FILL_BOTH));

			part.getSection().setClient(sectionComp);

			Table actionsTable = toolkit.createTable(sectionComp, SWT.BOTTOM);
			TableViewer actions = new TableViewer(actionsTable);
			
			// colummns
			TableViewerColumn keyCol = new TableViewerColumn(actions, SWT.BORDER);
			keyCol.setLabelProvider(new IniEditorColumnLabelProvider(WhatLabel.Key));
			keyCol.getColumn().setText("Key");
			keyCol.getColumn().setWidth(actionsTable.getSize().x/2);
			
			TableViewerColumn valCol = new TableViewerColumn(actions, SWT.BOTTOM);
			valCol.setLabelProvider(new IniEditorColumnLabelProvider(WhatLabel.Value));
			valCol.getColumn().setText("Value");
			valCol.getColumn().setWidth(actionsTable.getSize().x/2);
			
			actionsTable.setSize(actionsTable.getSize().x, sectionComp.getSize().y-10);
			
			actions.setContentProvider(new IStructuredContentProvider() {
				
				public void dispose() {
					// TODO Auto-generated method stub
					
				}

				public void inputChanged(Viewer viewer, Object oldInput,
						Object newInput) {
					// TODO Auto-generated method stub
					
				}

				public Object[] getElements(Object inputElement) {
					if (inputElement instanceof IniReader) {
						return ((IniReader)inputElement).getSections();
					}
					return new Object[0];
				}
				
			});
			actions.setInput(parser);
		}
	}

	@Override
	protected Object getPageConfiguration(PageAttribRequest request) {
		switch (request) {
		case RawSourcePageTitle:
			return "ActMap.txt";
		case SectionPageClass:
			return ActMapSectionPage.class;
		case SectionPageId:
			return "ActMap";
		case SectionPageTitle:
			return "[ActMap]";
		}
		return null;
	}

}
