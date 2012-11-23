grammar LandscapeScript;

@header {
package net.arctics.clonk.parser.landscapescript;

import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.resource.c4group.C4GroupItem;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import java.io.IOException;
import java.io.InputStreamReader;
}

@lexer::header {package net.arctics.clonk.parser.landscapescript;}

@members {
LandscapeScript script;
OverlayBase current;
OverlayBase lastOverlay;

Token valueLo, valueHi;
private boolean createMarkers;

public LandscapeScriptParser(LandscapeScript script, TokenStream input) {
	this(input);
	this.script = script;
	createMarkers = script.resource() == null || C4GroupItem.groupItemBackingResource(script.resource()) == null;
	this.current = script;
}

public LandscapeScriptParser(LandscapeScript script) {
	this (script, getTokenStream(script));
}

private static TokenStream getTokenStream(LandscapeScript script) {
	CharStream charStream;
	try {
		charStream = new ANTLRReaderStream(new InputStreamReader(((IFile)script.resource()).getContents()));
		LandscapeScriptLexer lexer = new LandscapeScriptLexer(charStream);
		CommonTokenStream tokenStream = new CommonTokenStream();
		tokenStream.setTokenSource(lexer);
		return tokenStream;
	} catch (IOException e) {
		e.printStackTrace();
		return null;
	} catch (CoreException e) {
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

private void setCurrentOverlay(OverlayBase overlay, Token typeToken, Token nameToken) {
	current = overlay;
	current.setLocation(new SourceLocation(startPos(typeToken), endPos(nameToken, typeToken)));
}

private void createMapObject(Token typeToken, Token nameToken) {
	try {
		if (current instanceof Overlay) {
			OverlayBase newOverlay = ((Overlay) current).createOverlay(typeToken.getText(), nameToken!=null?nameToken.getText():null);
			if (newOverlay == null)
				errorWithCode(ParserErrorCode.UndeclaredIdentifier, startPos(typeToken), endPos(typeToken), typeToken.getText());
			else
				setCurrentOverlay(newOverlay, typeToken, nameToken);
		}
	} catch (Exception e) {
		e.printStackTrace();
	}
}

private void setVal(Token nameToken, Token valueTokenLo, Token valueTokenHi) {
	try {
		if (valueTokenLo == null)
			errorWithCode(ParserErrorCode.ExpressionExpected, endPos(nameToken), endPos(nameToken)+1);
		else
			current.setAttribute(nameToken.getText(), valueTokenLo.getText(), valueTokenHi != null ? valueTokenHi.getText() : null);
	} catch (NoSuchFieldException e) {
		errorWithCode(ParserErrorCode.UndeclaredIdentifier, startPos(nameToken), endPos(nameToken), nameToken.getText());
	} catch (Exception e) {
		errorWithCode(ParserErrorCode.InvalidExpression, startPos(valueTokenLo), endPos(valueTokenLo), nameToken.getText());
	}
}

private void moveLevelUp() {
	lastOverlay = current;
	if (current != null)
		current = (Overlay) current.parentDeclaration();
}

private void assignOperator(String t) {
	Overlay.Operator op = Overlay.Operator.valueOf(t.charAt(0));
	if (lastOverlay instanceof Overlay)
	((Overlay)lastOverlay).setOperator(op);
}

private IMarker createMarker(int start, int end, String message, int severity) {
	if (!createMarkers || script.resource() == null) return null;
	try {
		IMarker marker = script.resource().createMarker(IMarker.PROBLEM);
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

private void errorWithCode(ParserErrorCode code, int errorStart, int errorEnd, Object... args) {
	String problem = code.makeErrorString(args);
	createErrorMarker(errorStart, errorEnd, problem);    	
}

private void deleteMarkers() {
	try {
		if (script.resource() != null)
			script.resource().deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ONE);
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
	|	type=POINT name=NAME? {createMapObject(type, name);} optionalblock
	|	template=NAME name=NAME? {createMapObject(template, name);} optionalblock;

optionalblock
	:	block?  {moveLevelUp();};
	

block	:	open=BLOCKOPEN statementorattrib* close=BLOCKCLOSE {setBody(open, close);};

statementorattrib
	:	attribute|statement;

attribute
	:	{valueLo=valueHi=null;} attr=NAME ASSIGN value STATEMENTEND {setVal(attr, valueLo, valueHi);};

value	:	valName=NAME {valueLo=valName;} | (valLo=NUMBER {valueLo=valLo;} (MINUS valHi=NUMBER  {valueHi=valHi;})?)  | valMat=MATCOMBO {valueLo=valMat;};

MAP		:	'map';
OVERLAY		:	'overlay';
POINT		:	'point';

fragment LETTER	:	'a'..'z'|'A'..'Z'|'_';
fragment UNIT	:	('px'|'%');
fragment DIGIT	:	'0'..'9';
fragment WORD	:	LETTER (LETTER|DIGIT)*;

MATCOMBO	:	WORD MINUS WORD;
MINUS		:	'-';
PLUS		:	'+';
NUMBER		:	(PLUS|MINUS)? DIGIT+UNIT?;
NAME		:	WORD;
WS		:	(' '|'\t'|'\n'|'\r')+ {skip();};
SLCOMMENT	:	'//' .* '\n' {skip();};
MLCOMMENT	:	'/*' .* '*/' {skip();};
ASSIGN		:	'=';
BLOCKOPEN	:	'{';
BLOCKCLOSE	:	'}';
STATEMENTEND	:	';';
OPERATOR		:	'|'|'&'|'^';
