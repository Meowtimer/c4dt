package net.arctics.clonk.ui.editors.ini;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.texteditor.IDocumentProvider;

import net.arctics.clonk.ui.editors.ColorManager;
import net.arctics.clonk.ui.editors.ini.IniDocumentProvider;
import net.arctics.clonk.ui.editors.ini.IniSourceViewerConfiguration;

public abstract class IniEditor extends FormEditor {

	private IDocumentProvider documentProvider;
	
	public IniEditor() {
	}

	public static class IniSectionPage extends FormPage {
		
		protected IDocumentProvider documentProvider;
		
		public IniSectionPage(FormEditor editor, String id, String title, IDocumentProvider docProvider) {
			super(editor, id, title);
			documentProvider = docProvider;
		}

		@Override
		public void doSave(IProgressMonitor monitor) {
			super.doSave(monitor);
			try {
				documentProvider.saveDocument(monitor, getEditorInput(), documentProvider.getDocument(getEditorInput()), true);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}

	}

	public static class RawSourcePage extends TextEditor {
		
		public static final String PAGE_ID = "rawIniEditor";
		
		private ColorManager colorManager;
		private String title;
		
		@Override
		public void doSave(IProgressMonitor progressMonitor) {
			super.doSave(progressMonitor);
		}

		public RawSourcePage(FormEditor editor, String id, String title, IDocumentProvider documentProvider) {
			colorManager = new ColorManager();
			setPartName(title);
			setContentDescription(title);
			this.title = title;
			setSourceViewerConfiguration(new IniSourceViewerConfiguration(colorManager, this));
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
	
	public enum PageAttribRequest {
		SectionPageClass,
		SectionPageId,
		SectionPageTitle,
		RawSourcePageTitle
	}
	
	protected abstract Object getPageConfiguration(PageAttribRequest request);

	@SuppressWarnings("unchecked")
	@Override
	protected void addPages() {
		try {
			documentProvider = new IniDocumentProvider();
			Class<IniSectionPage> iniSectionPage = (Class<IniSectionPage>)getPageConfiguration(PageAttribRequest.SectionPageClass);
			addPage(iniSectionPage.getConstructor(FormEditor.class, String.class, String.class, IDocumentProvider.class).newInstance(
				this, getPageConfiguration(PageAttribRequest.SectionPageId), getPageConfiguration(PageAttribRequest.SectionPageTitle), documentProvider)
			);
			int index = addPage(new RawSourcePage(this, RawSourcePage.PAGE_ID, (String) getPageConfiguration(PageAttribRequest.RawSourcePageTitle), documentProvider), this.getEditorInput());
			// editors as pages are not able to handle tab title strings
			// so here is a dirty trick:
			if (getContainer() instanceof CTabFolder)
				((CTabFolder)getContainer()).getItem(index).setText((String)getPageConfiguration(PageAttribRequest.RawSourcePageTitle));
		} catch (Exception e) {
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
