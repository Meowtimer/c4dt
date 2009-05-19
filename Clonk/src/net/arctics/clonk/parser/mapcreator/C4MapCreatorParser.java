package net.arctics.clonk.parser.mapcreator;

import org.eclipse.core.resources.IFile;

import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.ParserErrorCode;

public class C4MapCreatorParser {
	
	private BufferedScanner scanner;
	private C4MapCreator creator;
	
	public C4MapCreatorParser(C4MapCreator creator, IFile file) {
		scanner = new BufferedScanner(file);
		this.creator = creator;
	}
	
	public void clear() {
		creator.clear();
	}
	
	public void parse() {
		clear();
		parseOverlays();
	}

	private void parseOverlays() {
		scanner.eatWhitespace();
		String type = scanner.readWord();
		scanner.eatWhitespace();
		String name = scanner.readWord();
		expect('{');
		scanner.eatWhitespace();
	}

	private void expect(char c) {
		scanner.eatWhitespace();
		if (scanner.read() != c) {
			scanner.unread();
			error(scanner.getPosition(), ParserErrorCode.UnexpectedToken);
		}
	}

	private void error(int position, ParserErrorCode errorCode) {
		
	}

	public BufferedScanner getScanner() {
		return scanner;
	}

	public C4MapCreator getCreator() {
		return creator;
	}
}
