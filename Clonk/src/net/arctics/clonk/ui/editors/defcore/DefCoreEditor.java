package net.arctics.clonk.ui.editors.defcore;

import net.arctics.clonk.parser.defcore.DefCoreParser;
import net.arctics.clonk.ui.editors.ini.IniEditor;
import net.arctics.clonk.ui.editors.ini.IniSectionPage;

import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;

public class DefCoreEditor extends IniEditor {
	
	public DefCoreEditor() {
	}

	public static class DefCoreSectionPage extends IniSectionPage {
		
		public DefCoreSectionPage(FormEditor editor, String id, String title, IDocumentProvider docProvider) {
			super(editor, id, title, docProvider, DefCoreParser.class);
		}
		
	}

	@Override
	public Object getPageConfiguration(PageAttribRequest request) {
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
