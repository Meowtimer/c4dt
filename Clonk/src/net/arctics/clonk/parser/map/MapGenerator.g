grammar MapGenerator;

@header {
package net.arctics.clonk.parser.map;
}

@lexer::header {package net.arctics.clonk.parser.map;}

@members {
C4MapCreator mapCreator;
C4MapOverlay current;

void createMapObject(String type, String name) {
	try {
		C4MapOverlay newOverlay = current.createOverlay(type, name);
		current = newOverlay;
	} catch (Exception e) {
		e.printStackTrace();
	}
}

private void setVal(String name, String value) {
	try {
		current.setAttribute(name, value);
	} catch (Exception e) {
		e.printStackTrace();
	}
}

public MapGeneratorParser(C4MapCreator mapCreator, TokenStream input) {
	this(input);
	this.mapCreator = mapCreator;
	this.current = mapCreator;
}

}

parse	:	subobject*;

subobject
	:	type=WORD (name=WORD)? {createMapObject($type.text, $name.text);} '{' (subobject|attribute)* '}' ';';

attribute
	:	attr=WORD '=' attrvalue=VALUE ';'  {setVal($attr.text, $attrvalue.text);};

LETTER	:	'a'..'z'|'A'..'Z'|'_';
DIGIT	:	'0'..'9';
WORD	:	LETTER (LETTER|DIGIT)*;
INT	:	('+'|'-')? DIGIT+;
VALUE	:	INT|WORD|(WORD '-' WORD);
WS	:	(' '|'\t'|'\n'|'\r')+ {skip();} ;
