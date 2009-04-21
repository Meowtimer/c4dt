package net.arctics.clonk.ui.editors.actmap;

import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;

import net.arctics.clonk.parser.inireader.ActMapParser;
import net.arctics.clonk.ui.editors.ini.IniEditor;
import net.arctics.clonk.ui.editors.ini.IniSectionPage;

public class ActMapEditor extends IniEditor {
	
	public ActMapEditor() {
	}

	public static class ActMapSectionPage extends IniSectionPage {
		
		public ActMapSectionPage(FormEditor editor, String id, String title, IDocumentProvider docProvider) {
			super(editor, id, title, docProvider, ActMapParser.class);
		}

	}

	@Override
	public Object getPageConfiguration(PageAttribRequest request) {
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
