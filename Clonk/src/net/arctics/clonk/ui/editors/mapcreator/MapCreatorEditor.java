package net.arctics.clonk.ui.editors.mapcreator;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.eclipse.core.resources.IFile;
import net.arctics.clonk.parser.mapcreator.C4MapCreator;
import net.arctics.clonk.parser.mapcreator.MapCreatorLexer;
import net.arctics.clonk.parser.mapcreator.MapCreatorParser;
import net.arctics.clonk.ui.editors.ClonkDocumentProvider;
import net.arctics.clonk.ui.editors.ClonkTextEditor;
import net.arctics.clonk.ui.editors.ColorManager;
import net.arctics.clonk.util.Utilities;


public class MapCreatorEditor extends ClonkTextEditor {
	
	private C4MapCreator mapCreator;
	
	public MapCreatorEditor() {
		super();
		ColorManager colorManager = new ColorManager();
		setSourceViewerConfiguration(new MapCreatorSourceViewerConfiguration(colorManager,this));
		setDocumentProvider(new ClonkDocumentProvider(this));
	}
	
	private void reparse() {
		String documentText = getDocumentProvider().getDocument(getEditorInput()).get();
		CharStream charStream = new ANTLRStringStream(documentText);
		MapCreatorLexer lexer = new MapCreatorLexer(charStream);
		CommonTokenStream tokenStream = new CommonTokenStream();
		tokenStream.setTokenSource(lexer);
		MapCreatorParser parser = new MapCreatorParser(mapCreator, tokenStream);
		try {
			mapCreator.clear();
			parser.parse();
		} catch (RecognitionException e) {
			e.printStackTrace();
		}
	}
	
	public C4MapCreator getMapCreator() {
		if (mapCreator == null)
			mapCreator = new C4MapCreator(Utilities.getEditingFile(this));
		return mapCreator;
		
	}
	
	@Override
	public C4MapCreator getTopLevelDeclaration() {
		C4MapCreator result = getMapCreator();
		reparse();
		return result;
	}
	
	public void silentReparse() {
		IFile file = getMapCreator().getFile();
		getMapCreator().setFile(null);
		try {
			reparse();
		}
		finally {
			getMapCreator().setFile(file);
		}
	}
	
	@Override
	public void refreshOutline() {
		if (outlinePage != null) {
			outlinePage.setInput(getTopLevelDeclaration());
			super.refreshOutline();
		}
	}
	
}
