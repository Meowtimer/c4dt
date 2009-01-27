package net.arctics.clonk.ui.editors.defcore;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.Utilities;
import net.arctics.clonk.parser.C4Object;
import net.arctics.clonk.parser.C4ObjectIntern;
import net.arctics.clonk.ui.editors.ColorManager;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.texteditor.IDocumentProvider;

public class C4DefCoreEditor extends FormEditor {

	private IDocumentProvider documentProvider;
	
	public C4DefCoreEditor() {
	}

	public static class DefCoreSectionPage extends FormPage {
		
		private IDocumentProvider documentProvider;
		
		public DefCoreSectionPage(FormEditor editor, String id, String title, IDocumentProvider docProvider) {
			super(editor, id, title);
			documentProvider = docProvider;
		}

		@Override
		public void doSave(IProgressMonitor monitor) {
			// TODO Auto-generated method stub
			super.doSave(monitor);
			try {
				documentProvider.saveDocument(monitor, getEditorInput(), documentProvider.getDocument(getEditorInput()), true);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}

		protected void createFormContent(IManagedForm managedForm) {
			super.createFormContent(managedForm);

			FormToolkit toolkit = managedForm.getToolkit();
			ScrolledForm form = managedForm.getForm();
			toolkit.decorateFormHeading(form.getForm());
			
			form.setText("DefCore options");
			IFile input = Utilities.getEditingFile(getEditor());
			if (input != null) {
				try { // XXX values should come from document - not from builder cache
					IContainer cont = input.getParent();
					C4Object obj = (C4Object) input.getParent().getSessionProperty(ClonkCore.C4OBJECT_PROPERTY_ID);
					if (obj != null) {
						form.setText(obj.getName() + "(" + obj.getId().getName() + ") definition core");
					}
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
			
			GridLayout layout = new GridLayout(1,false);
			form.getBody().setLayout(layout);
			form.getBody().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			
			
			SectionPart part = new SectionPart(form.getBody(),toolkit,Section.CLIENT_INDENT | Section.TITLE_BAR | Section.EXPANDED);
			
			part.getSection().setText("General options");
			part.getSection().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			
			Composite sectionComp = toolkit.createComposite(part.getSection());
			sectionComp.setLayout(new GridLayout());
			sectionComp.setLayoutData(new GridData(GridData.FILL_BOTH));
			
			part.getSection().setClient(sectionComp);
			
			toolkit.createLabel(sectionComp, "Title bar dingens halt");
			
			toolkit.createLabel(sectionComp, "blub",SWT.LEFT);
//			lab.setText("flub");
//			lab.setVisible(true);
		}
	}

	public static class RawSourcePage extends TextEditor {
		
		public static final String PAGE_ID = "rawDefCore";
		
		private ColorManager colorManager;
		private FormEditor fEditor;
		private String id;
		private String title;
		
		@Override
		public void doSave(IProgressMonitor progressMonitor) {
			// TODO Auto-generated method stub
			super.doSave(progressMonitor);
		}

		public RawSourcePage(FormEditor editor, String id, String title, IDocumentProvider documentProvider) {
			colorManager = new ColorManager();
			fEditor = editor;
			this.id = id;
			setPartName(title);
			setContentDescription(title);
			this.title = title;
			setSourceViewerConfiguration(new DefCoreSourceViewerConfiguration(colorManager, this));
			setDocumentProvider(documentProvider);
		}

		public void resetPartName() {
			setPartName(title);
		}
	}
	
	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		IResource res = (IResource) input.getAdapter(IResource.class);
		if (res != null) {
			setPartName(res.getParent().getName() + "/" + res.getName());
		}
	}

	@Override
	protected void addPages() {
		try {
			documentProvider = new DefCoreDocumentProvider();
			addPage(new DefCoreSectionPage(this, "DefCore", "[DefCore]", documentProvider));
			int index = addPage(new RawSourcePage(this, RawSourcePage.PAGE_ID,"DefCore.txt", documentProvider),this.getEditorInput());
			// editors as pages are not able to handle tab title strings
			// so here is a dirty trick:
			if (getContainer() instanceof CTabFolder)
				((CTabFolder)getContainer()).getItem(index).setText("DefCore.txt");
		} catch (PartInitException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		try {
			documentProvider.saveDocument(monitor, getEditorInput(), documentProvider.getDocument(getEditorInput()), true);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void doSaveAs() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isSaveAsAllowed() {
		// TODO Auto-generated method stub
		return false;
	}

}
