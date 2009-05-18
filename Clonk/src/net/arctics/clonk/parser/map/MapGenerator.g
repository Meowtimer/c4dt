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


void createMapObject(Class<? extends C4MapOverlay> cls, String name) {
	try {
		C4MapOverlay newOverlay = current.createOverlay(cls, name);
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
	:	MAP name=WORD? {createMapObject(C4Map.class, $name.text);} '{' (subobject)* '}' ';'
	|	OVERLAY name=WORD? {createMapObject(C4MapOverlay.class, $name.text);} '{' subobject* '}' ';'
	|	template=WORD name=WORD? {createMapObject($template.text, $name.text);} '{' subobject* '}' ';';

attribute
	:	attr=WORD '=' attrValue=INT
	|	attr=WORD '=' attrValue=WORD
	|	attr=WORD '=' attrValue=MATERIAL;

MAP	:	'map';
OVERLAY	:	'overlay';
LETTER	:	'a'..'z'|'A'..'Z'|'_';
DIGIT	:	'0'..'9';
WORD	:	LETTER (LETTER|DIGIT)*;
INT	:	('+'|'-')? DIGIT+;
MATERIAL	:	WORD '-' WORD;
WS	:	(' '|'\t'|'\n'|'\r')+ {skip();} ;
