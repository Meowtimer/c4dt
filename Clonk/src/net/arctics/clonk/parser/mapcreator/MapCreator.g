grammar MapCreator;

@header {
package net.arctics.clonk.parser.mapcreator;

import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.SourceLocation;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import java.io.IOException;
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

public MapCreatorParser(C4MapCreator mapCreator) {
	this (mapCreator, getTokenStream(mapCreator));
}

private static TokenStream getTokenStream(C4MapCreator mapCreator) {
	CharStream charStream;
	try {
		charStream = new ANTLRFileStream(mapCreator.getResource().getLocation().toOSString());
		MapCreatorLexer lexer = new MapCreatorLexer(charStream);
		CommonTokenStream tokenStream = new CommonTokenStream();
		tokenStream.setTokenSource(lexer);
		return tokenStream;
	} catch (IOException e) {
		e.printStackTrace();
		return null;
	}
}

private static int startPos(Token t) {
	return ((CommonToken)t).getStartIndex();
}

private static int endPos(Token t, Token fallback) {
	return ((CommonToken)(t!=null?t:fallback)).getStopIndex()+1;
}

private static int endPos(Token t) {
	return endPos(t, null);
}

private void setCurrentOverlay(C4MapOverlay overlay, Token typeToken, Token nameToken) {
	current = overlay;
	current.setLocation(new SourceLocation(startPos(typeToken), endPos(nameToken, typeToken)));
}

private void createMapObject(Token typeToken, Token nameToken) {
	try {
		if (current == null)
			return;	
		C4MapOverlay newOverlay = current.createOverlay(typeToken.getText(), nameToken!=null?nameToken.getText():null);
		if (newOverlay == null)
			errorWithCode(ParserErrorCode.UndeclaredIdentifier, startPos(typeToken), endPos(typeToken), typeToken.getText());
		else
			setCurrentOverlay(newOverlay, typeToken, nameToken);
	} catch (Exception e) {
		e.printStackTrace();
	}
}

private void setVal(Token nameToken, Token valueToken) {
	try {
		current.setAttribute(nameToken.getText(), valueToken.getText());
	} catch (NoSuchFieldException e) {
		errorWithCode(ParserErrorCode.UndeclaredIdentifier, startPos(nameToken), endPos(nameToken), nameToken.getText());
	} catch (Exception e) {
		errorWithCode(ParserErrorCode.InvalidExpression, startPos(valueToken), endPos(valueToken), nameToken.getText());
	}
}

private void moveLevelUp() {
	lastOverlay = current;
	if (current != null)
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

private void errorWithCode(ParserErrorCode code, int errorStart, int errorEnd, Object... args) {
	String problem = code.getErrorString(args);
	createErrorMarker(errorStart, errorEnd, problem);    	
}

private void deleteMarkers() {
	try {
		if (mapCreator.getResource() != null)
			mapCreator.getResource().deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ONE);
	} catch (CoreException e) {
		e.printStackTrace();
	}
		}

@Override
public void reportError(RecognitionException error) {
	if (error.token.getText() != null)	
		errorWithCode(ParserErrorCode.UnexpectedToken, startPos(error.token), endPos(error.token), error.token.getText());
	super.reportError(error);
}

private void setBody(Token blockOpen, Token blockClose) {
	if (current != null)
		current.setBody(new SourceLocation(startPos(blockOpen), endPos(blockClose)));
}

@Override
public void displayRecognitionError(String[] tokenNames, RecognitionException e) {
	// do nothing
}

public void parse() {
	try {
		start();
	} catch (RecognitionException e) {
		e.printStackTrace();
	}
}


}

start	:	{deleteMarkers();} statement*;

statement
	:	{lastOverlay = null;} composition STATEMENTEND;

composition
	:	subobject (op=OPERATOR {assignOperator($op.text);} composition)?;

subobject
	:	type=MAP name=NAME? {createMapObject(type, name);} optionalblock
	|	type=OVERLAY name=NAME? {createMapObject(type, name);} optionalblock
	|	template=NAME name=NAME? {createMapObject(template, name);} optionalblock;

optionalblock
	:	block?  {moveLevelUp();};

block	:	open=BLOCKOPEN statementorattrib* close=BLOCKCLOSE {setBody(open, close);};

statementorattrib
	:	attribute|statement;

attribute
	:	attr=NAME ASSIGN attrValue=NAME STATEMENTEND {setVal(attr, attrValue);}
	|	attr=NAME ASSIGN attrValue=NUMBER STATEMENTEND {setVal(attr, attrValue);}
	|	attr=NAME ASSIGN attrValue=MATCOMBO STATEMENTEND {setVal(attr, attrValue);};

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
