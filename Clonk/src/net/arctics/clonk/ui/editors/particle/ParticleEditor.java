package net.arctics.clonk.ui.editors.particle;

import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;

import net.arctics.clonk.parser.inireader.IniReader;
import net.arctics.clonk.ui.editors.ini.IniEditor;

public class ParticleEditor extends IniEditor {

	private class ParticleSectionPage extends IniSectionPage {

		public ParticleSectionPage(FormEditor editor, String id, String title,
				IDocumentProvider docProvider,
				Class<? extends IniReader> iniReaderClass) {
			super(editor, id, title, docProvider, iniReaderClass);
		}
		
	}
	
	@Override
	public Object getPageConfiguration(PageAttribRequest request) {
		switch (request) {
		case RawSourcePageTitle:
			return "Particle.txt";
		case SectionPageClass:
			return ParticleSectionPage.class;
		case SectionPageId:
			return "Particle";
		case SectionPageTitle:
			return "Particle";
		}
		return null;
	}

}
