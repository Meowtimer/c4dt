grammar StringTbl;

@header {
package net.arctics.clonk.parser.stringtbl;

public StringTblParser(StringTbl tbl, TokenStream input) {
	this(input);
	this.tbl = tbl;
}
}

@lexer::header {package net.arctics.clonk.parser.stringtbl;}

@members {
private StringTbl tbl;
}

start	:	(VALUE '=' VALUE)*;

WS	:	(' '|'\t'|'\n'|'\r')+ {skip();};
VALUE	:	('a'..'z'|'A'..'Z'|'_'|'Š'..'Ÿ'|'€'..'…'
