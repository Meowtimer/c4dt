package net.arctics.clonk.ui.editors.scenario;

import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;

import net.arctics.clonk.parser.scenario.ScenarioParser;
import net.arctics.clonk.ui.editors.ini.IniEditor;

public class ScenarioEditor extends IniEditor {

	public class ScenarioSectionPage extends IniSectionPage {

		public ScenarioSectionPage(FormEditor editor, String id, String title, IDocumentProvider docProvider) {
			super(editor, id, title, docProvider, ScenarioParser.class);
		}
		
	}
	
	@Override
	protected Object getPageConfiguration(PageAttribRequest request) {
		switch (request) {
		case RawSourcePageTitle:
			return "Scenario.txt";
		case SectionPageClass:
			return ScenarioSectionPage.class;
		case SectionPageId:
			return "Scenario";
		case SectionPageTitle:
			return "[Scenario]";
		}
		return null;
	}

}
