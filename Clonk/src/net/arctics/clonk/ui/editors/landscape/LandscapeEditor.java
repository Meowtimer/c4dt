package net.arctics.clonk.ui.editors.landscape;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import net.arctics.clonk.parser.map.C4MapCreator;
import net.arctics.clonk.parser.map.MapGeneratorLexer;
import net.arctics.clonk.parser.map.MapGeneratorParser;
import net.arctics.clonk.ui.editors.ClonkDocumentProvider;
import net.arctics.clonk.ui.editors.ClonkTextEditor;
import net.arctics.clonk.ui.editors.ColorManager;


public class LandscapeEditor extends ClonkTextEditor {
	
	private C4MapCreator mapCreator = new C4MapCreator();
	
	public LandscapeEditor() {
		super();
		ColorManager colorManager = new ColorManager();
		setSourceViewerConfiguration(new LandscapeSourceViewerConfiguration(colorManager,this));
		setDocumentProvider(new ClonkDocumentProvider(this));
	}
	
	private void reparse() {
		String documentText = getDocumentProvider().getDocument(getEditorInput()).get();
		CharStream charStream = new ANTLRStringStream(documentText);
		MapGeneratorLexer lexer = new MapGeneratorLexer(charStream);
		CommonTokenStream tokenStream = new CommonTokenStream();
		tokenStream.setTokenSource(lexer);
		MapGeneratorParser parser = new MapGeneratorParser(mapCreator, tokenStream);
		try {
			mapCreator.clear();
			parser.parse();
		} catch (RecognitionException e) {
			e.printStackTrace();
		}
	}
	
	public C4MapCreator getMapCreator() {
		reparse();
		return mapCreator;
		
	}
	
	@Override
	public C4MapCreator getTopLevelDeclaration() {
		return getMapCreator();
	}
	
	@Override
	public void refreshOutline() {
		if (outlinePage != null) {
			outlinePage.setInput(getTopLevelDeclaration());
			super.refreshOutline();
		}
	}
	
}
