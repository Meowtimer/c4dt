grammar ini;

@header {
package net.arctics.clonk.parser.ini;

import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.SourceLocation;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import java.io.IOException;
}

@lexer::header {package net.arctics.clonk.parser.ini;;}

start	:	linerecurse;

line	:	(section|assignment|comment);

linerecurse
	:	line (LINEEND linerecurse)?;

section	:	OPENBRACKET TEXT CLOSEBRACKET;

assignment
	:	WS? TEXT WS? ASSIGN TEXT;

comment	:	COMMENTSTART (options {greedy=false;} :.)*;

TEXT	:	('A'..'Z'|'a'..'z'|'_'|'0'..'9')*;

LINEEND	:	'\n';

WS	:	(' '|'\t'|'\r')+;

ASSIGN	:	'=';

OPENBRACKET
	:	'[';
	
CLOSEBRACKET
	:	']';

COMMENTSTART
	:	(';'|'#'|'//');
