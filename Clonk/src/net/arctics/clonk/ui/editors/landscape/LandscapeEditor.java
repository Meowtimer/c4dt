package net.arctics.clonk.ui.editors.landscape;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import net.arctics.clonk.parser.map.C4MapCreator;
import net.arctics.clonk.parser.map.MapGeneratorLexer;
import net.arctics.clonk.parser.map.MapGeneratorParser;
import net.arctics.clonk.ui.editors.ClonkTextEditor;


public class LandscapeEditor extends ClonkTextEditor {
	
	private C4MapCreator mapCreator = new C4MapCreator();
	
	public C4MapCreator getMapCreator() {
		
		String documentText = getDocumentProvider().getDocument(getEditorInput()).get();
		CharStream charStream = new ANTLRStringStream(documentText);
		MapGeneratorLexer lexer = new MapGeneratorLexer(charStream);
		CommonTokenStream tokenStream = new CommonTokenStream();
		tokenStream.setTokenSource(lexer);
		MapGeneratorParser parser = new MapGeneratorParser(mapCreator, tokenStream);
		
		try {
			parser.parse();
		} catch (RecognitionException e) {
			e.printStackTrace();
		}
		
		return mapCreator;
		
	}
	
	@Override
	public C4MapCreator getTopLevelDeclaration() {
		return getMapCreator();
	}
}
