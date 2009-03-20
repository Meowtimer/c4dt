package net.arctics.clonk.ui.editors.actmap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.texteditor.IDocumentProvider;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.Utilities;
import net.arctics.clonk.parser.C4Object;
import net.arctics.clonk.parser.actmap.ActMapParser;
import net.arctics.clonk.ui.editors.ini.IniEditor;

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

			TableViewer actions = new TableViewer(sectionComp);
			
			// colummns
			TableViewerColumn keyCol = new TableViewerColumn(actions, SWT.BORDER);
			TableViewerColumn valCol = new TableViewerColumn(actions, SWT.BORDER);
			keyCol.getColumn().setText("Key");
			valCol.getColumn().setText("Value");
			
			actions.setLabelProvider(new ITableLabelProvider() {

				public Image getColumnImage(Object element, int columnIndex) {
					return null;
				}

				public String getColumnText(Object element, int columnIndex) {
					switch (columnIndex) {
					case 0:
						return "blub";
					case 1:
						return "ugha";
					}
					return null;
				}

				public void addListener(ILabelProviderListener listener) {
					// do i care?
				}

				public void dispose() {
					// TODO Auto-generated method stub
					
				}

				public boolean isLabelProperty(Object element, String property) {
					return false;
				}

				public void removeListener(ILabelProviderListener listener) {
					// TODO Auto-generated method stub
					
				}
				
			});
			actions.setContentProvider(new IStructuredContentProvider() {
				
				public void dispose() {
					// TODO Auto-generated method stub
					
				}

				public void inputChanged(Viewer viewer, Object oldInput,
						Object newInput) {
					// TODO Auto-generated method stub
					
				}

				public Object[] getElements(Object inputElement) {
					return new String[] {
						"zuhauf",
						"jefreili"
					};
				}
				
			});
			actions.setInput("Awesome");
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
