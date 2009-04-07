package net.arctics.clonk.ui.editors.material;

import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;

import net.arctics.clonk.parser.material.MaterialParser;
import net.arctics.clonk.ui.editors.ini.IniEditor;
import net.arctics.clonk.ui.editors.ini.IniSectionPage;

public class MaterialEditor extends IniEditor {

	public MaterialEditor() {
	}

	public static class MaterialSectionPage extends IniSectionPage {
		
		public MaterialSectionPage(FormEditor editor, String id, String title, IDocumentProvider docProvider) {
			super(editor, id, title, docProvider, MaterialParser.class);
		}

	}

	@Override
	public Object getPageConfiguration(PageAttribRequest request) {
		switch (request) {
		case RawSourcePageTitle:
			return "Material.txt";
		case SectionPageClass:
			return MaterialSectionPage.class;
		case SectionPageId:
			return "Material";
		case SectionPageTitle:
			return "[Material]";
		}
		return null;
	}

}
