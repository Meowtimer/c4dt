package net.arctics.clonk.ui.editors.defcore;

import net.arctics.clonk.parser.defcore.DefCoreParser;
import net.arctics.clonk.ui.editors.ini.IniEditor;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;

public class C4DefCoreEditor extends IniEditor {

	private IDocumentProvider documentProvider;
	
	public C4DefCoreEditor() {
	}

	public static class DefCoreSectionPage extends IniSectionPage {
		
		private IDocumentProvider documentProvider;
		
		public DefCoreSectionPage(FormEditor editor, String id, String title, IDocumentProvider docProvider) {
			super(editor, id, title, docProvider, DefCoreParser.class);
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
