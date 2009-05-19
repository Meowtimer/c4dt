grammar MapCreator;

@header {
package net.arctics.clonk.parser.mapcreator;

import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.SourceLocation;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
}

@lexer::header {package net.arctics.clonk.parser.mapcreator;}

@members {
C4MapCreator mapCreator;
C4MapOverlay current;
C4MapOverlay lastOverlay;

public MapCreatorParser(C4MapCreator mapCreator, TokenStream input) {
	this(input);
	this.mapCreator = mapCreator;
	this.current = mapCreator;
}

private static int startPos(Token t) {
	return ((CommonToken)t).getStartIndex();
}

private static int endPos(Token t, Token fallback) {
	return ((CommonToken)(t!=null?t:fallback)).getStopIndex()+1;
}

private void setCurrentOverlay(C4MapOverlay overlay, Token typeToken, Token nameToken) {
	current = overlay;
	current.setLocation(new SourceLocation(startPos(typeToken), endPos(nameToken, typeToken)));
}

private void createMapObject(Token typeToken, Token nameToken) {
	try {
		C4MapOverlay newOverlay = current.createOverlay(typeToken.getText(), nameToken!=null?nameToken.getText():null);
		setCurrentOverlay(newOverlay, typeToken, nameToken);
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

private void moveLevelUp() {
	lastOverlay = current;
	current = (C4MapOverlay) current.getParentDeclaration();
}

private void assignOperator(String t) {
	C4MapOverlay.Operator op = C4MapOverlay.Operator.valueOf(t.charAt(0));
	lastOverlay.setOperator(op);
}

private IMarker createMarker(int start, int end, String message, int severity) {
	if (mapCreator.getResource() == null) return null;
	try {
		IMarker marker = mapCreator.getResource().createMarker(IMarker.PROBLEM);
		marker.setAttribute(IMarker.SEVERITY, severity);
		marker.setAttribute(IMarker.TRANSIENT, false);
		marker.setAttribute(IMarker.MESSAGE, message);
		marker.setAttribute(IMarker.CHAR_START, start);
		marker.setAttribute(IMarker.CHAR_END, end);
		return marker;
	} catch (CoreException e) {
		e.printStackTrace();
	}
	return null;
}

private IMarker createErrorMarker(int start, int end, String message) {
	return createMarker(start, end, message, IMarker.SEVERITY_ERROR);
}

private IMarker createWarningMarker(int start, int end, String message) {
	return createMarker(start, end, message, IMarker.SEVERITY_WARNING);
}

private void errorWithCode(ParserErrorCode code, int errorStart, int errorEnd, boolean noThrow, Object... args) throws ParsingException {
	String problem = code.getErrorString(args);
	createErrorMarker(errorStart, errorEnd, problem);
	if (!noThrow)
		throw new ParsingException(problem);
}

}

parse	:	statement*;

statement
	:	{lastOverlay = null;} composition STATEMENTEND;

composition
	:	subobject (op=OPERATOR {assignOperator($op.text);} composition)?;

subobject
	:	type=MAP name=NAME? {createMapObject(type, name);} block
	|	type=OVERLAY name=NAME? {createMapObject(type, name);} block
	|	template=NAME name=NAME? {createMapObject(template, name);} block;

block	:	BLOCKOPEN statementorattrib* BLOCKCLOSE {moveLevelUp();};

statementorattrib
	:	attribute|statement;

attribute
	:	attr=NAME ASSIGN attrValue=NAME STATEMENTEND {setVal($attr.text, $attrValue.text);}
	|	attr=NAME ASSIGN attrValue=NUMBER STATEMENTEND {setVal($attr.text, $attrValue.text);}
	|	attr=NAME ASSIGN attrValue=MATCOMBO STATEMENTEND {setVal($attr.text, $attrValue.text);};

MAP		:	'map';
OVERLAY		:	'overlay';

fragment LETTER	:	'a'..'z'|'A'..'Z'|'_';
fragment DIGIT	:	'0'..'9';
fragment INT		:	('+'|'-')? DIGIT+;
fragment WORD	:	LETTER (LETTER|DIGIT)*;

NUMBER		:	INT;
NAME		:	WORD;
MATCOMBO	:	WORD '-' WORD;
WS		:	(' '|'\t'|'\n'|'\r')+ {skip();};
SLCOMMENT	:	'//' .* '\n' {skip();};
MLCOMMENT	:	'/*' .* '*/' {skip();};
ASSIGN		:	'=';
BLOCKOPEN	:	'{';
BLOCKCLOSE	:	'}';
STATEMENTEND	:	';';
OPERATOR		:	'|'|'&'|'^';
