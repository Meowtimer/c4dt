grammar ini;

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
