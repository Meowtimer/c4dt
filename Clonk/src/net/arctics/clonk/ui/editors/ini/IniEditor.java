package net.arctics.clonk.ui.editors.ini;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.ui.texteditor.IDocumentProvider;

import net.arctics.clonk.ui.editors.c4script.ColorManager;
import net.arctics.clonk.ui.editors.c4script.ShowInAdapter;
import net.arctics.clonk.ui.editors.ini.IniDocumentProvider;
import net.arctics.clonk.ui.editors.ini.IniSourceViewerConfiguration;

import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.text.IDocument;

public class IniEditor extends FormEditor {

	private IniDocumentProvider documentProvider;
	private ShowInAdapter showInAdapter;
	private RawSourcePage sourcePage;
	private IniSectionPage sectionPage;
		
	public IniEditor() {
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
	
	private IFileEditorInput fileEditorInput() {
		return (IFileEditorInput)getEditorInput();
	}
	
	public Object getPageConfiguration(PageAttribRequest request) {
		IFile file = fileEditorInput().getFile();
		switch (request) {
		case RawSourcePageTitle:
			return file.getName();
		case SectionPageClass:
			return IniSectionPage.class;
		case SectionPageId:
			return file.getName().substring(0, file.getName().lastIndexOf('.'));
		case SectionPageTitle:
			return "Editor";
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void addPages() {
		try {
			documentProvider = new IniDocumentProvider();
			Class<IniSectionPage> iniSectionPageClass = (Class<IniSectionPage>)getPageConfiguration(PageAttribRequest.SectionPageClass);
			sectionPage = iniSectionPageClass.getConstructor(FormEditor.class, String.class, String.class, IDocumentProvider.class).newInstance(
					this, getPageConfiguration(PageAttribRequest.SectionPageId), getPageConfiguration(PageAttribRequest.SectionPageTitle), documentProvider);
			addPage(sectionPage);
			sourcePage = new RawSourcePage(this, RawSourcePage.PAGE_ID, (String) getPageConfiguration(PageAttribRequest.RawSourcePageTitle), documentProvider);
			int index = addPage(sourcePage, this.getEditorInput());
			// editors as pages are not able to handle tab title strings
			// so here is a dirty trick:
			if (getContainer() instanceof CTabFolder)
				((CTabFolder)getContainer()).getItem(index).setText((String)getPageConfiguration(PageAttribRequest.RawSourcePageTitle));
			addPageChangedListener(new IPageChangedListener() {
				public void pageChanged(PageChangedEvent event) {
					if (event.getSelectedPage() == sectionPage) {
						IDocument doc = sourcePage.getDocumentProvider().getDocument(sourcePage.getEditorInput());
						String completeText = doc.get();
						sectionPage.updateIniReader(completeText);
					}
				}
			});
			sectionPage.initializeReader();
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
	
	@SuppressWarnings("unchecked")
	@Override
	public Object getAdapter(Class adapter) {
		if (adapter.equals(IShowInSource.class) || adapter.equals(IShowInTargetList.class)) {
			if (showInAdapter == null)
				showInAdapter = new ShowInAdapter(this);
			return showInAdapter;
		}
		return super.getAdapter(adapter);
	}

	public RawSourcePage getSourcePage() {
		return sourcePage;
	}

}
