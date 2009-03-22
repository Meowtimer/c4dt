package net.arctics.clonk.ui.editors.defcore;

import net.arctics.clonk.parser.defcore.DefCoreParser;
import net.arctics.clonk.ui.editors.ColorManager;
import net.arctics.clonk.ui.editors.ini.IniEditor;
import net.arctics.clonk.ui.editors.ini.IniSourceViewerConfiguration;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;

public class C4DefCoreEditor extends IniEditor {

	private IDocumentProvider documentProvider;
	
	public C4DefCoreEditor() {
	}

	public static class DefCoreSectionPage extends IniSectionPage {
		
		private IDocumentProvider documentProvider;
		
		public DefCoreSectionPage(FormEditor editor, String id, String title, IDocumentProvider docProvider) {
			super(editor, id, title, docProvider);
			iniReader = new DefCoreParser(Utilities.getEditingFile(getEditor()));
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
		
		public static final String PAGE_ID = "rawDefCore";
		
		private ColorManager colorManager;
		private FormEditor fEditor;
		private String id;
		private String title;
		
		@Override
		public void doSave(IProgressMonitor progressMonitor) {
			super.doSave(progressMonitor);
		}

		public RawSourcePage(FormEditor editor, String id, String title, IDocumentProvider documentProvider) {
			colorManager = new ColorManager();
			fEditor = editor;
			this.id = id;
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
	protected Object getPageConfiguration(PageAttribRequest request) {
		switch (request) {
		case RawSourcePageTitle:
			return "DefCore.txt";
		case SectionPageClass:
			return DefCoreSectionPage.class;
		case SectionPageId:
			return "DefCore";
		case SectionPageTitle:
			return "[DefCore]";
		}
		return null;
	}

}
