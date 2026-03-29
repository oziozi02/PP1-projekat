package rs.ac.bg.etf.pp1;

import java_cup.runtime.Symbol;
import org.apache.log4j.Logger;

%%

%{

	private Logger log = Logger.getLogger(getClass());

	// ukljucivanje informacije o poziciji tokena
	private Symbol new_symbol(int type) {
		return new Symbol(type, yyline+1, yycolumn);
	}
	
	// ukljucivanje informacije o poziciji tokena
	private Symbol new_symbol(int type, Object value) {
		return new Symbol(type, yyline+1, yycolumn, value);
	}

%}

%cup
%line
%column

%eofval{
	return new_symbol(sym.EOF);
%eofval}

LineTerminator = \r\n|\r|\n
InputCharacter = [^\r\n]
Whitespace = {LineTerminator}|[\t\f\b ]

SingleLineComment = "//" {InputCharacter}*{LineTerminator}?

Comment = {SingleLineComment}

IntegerLiteral = [0-9]+
PrintableCharLiteral = "'"[^'\r\n]"'"
BooleanLiteral = "true" | "false"
Identifier = [a-zA-Z][a-zA-Z0-9_]*

%%


<YYINITIAL> {

	{Whitespace}    { /* preskoci beline znakove */ }

	//Kljucne reci
	"program"       { return new_symbol(sym.PROG, yytext()); }
	"break"         { return new_symbol(sym.BREAK, yytext()); }
	"enum"          { return new_symbol(sym.ENUM, yytext()); }
	"class"         { return new_symbol(sym.CLASS, yytext()); }
	"abstract"      { return new_symbol(sym.ABSTRACT, yytext()); }
	"else"          { return new_symbol(sym.ELSE, yytext()); }
	"const"         { return new_symbol(sym.CONST, yytext()); }
	"if"            { return new_symbol(sym.IF, yytext()); }
	"new"           { return new_symbol(sym.NEW, yytext()); }
	"print"         { return new_symbol(sym.PRINT, yytext()); }
	"read"          { return new_symbol(sym.READ, yytext()); }
	"return"        { return new_symbol(sym.RETURN, yytext()); }
	"void"          { return new_symbol(sym.VOID, yytext()); }
	"extends"      { return new_symbol(sym.EXTENDS, yytext()); }
	"continue"      { return new_symbol(sym.CONTINUE, yytext()); }
	"for"           { return new_symbol(sym.FOR, yytext()); }
	"length"        { return new_symbol(sym.LENGTH, yytext()); }
	"switch"         { return new_symbol(sym.SWITCH, yytext()); }
	"case"          { return new_symbol(sym.CASE, yytext()); }

	//Operatori i specijalni simboli
	"*"             { return new_symbol(sym.MUL, yytext()); }
	"/"             { return new_symbol(sym.DIV, yytext()); }
	"%"             { return new_symbol(sym.MOD, yytext()); }
	"=="            { return new_symbol(sym.EQ, yytext()); }
	"!="            { return new_symbol(sym.NE, yytext()); }
	">="			{ return new_symbol(sym.GE, yytext()); }
	">"             { return new_symbol(sym.GT, yytext()); }
	"<="			{ return new_symbol(sym.LE, yytext()); }
	"<"             { return new_symbol(sym.LT, yytext()); }
	"&&"            { return new_symbol(sym.AND, yytext()); }
	"||"            { return new_symbol(sym.OR, yytext()); }
	"="             { return new_symbol(sym.EQUAL, yytext()); }
	"++"            { return new_symbol(sym.INC, yytext()); }
	"--"            { return new_symbol(sym.DEC, yytext()); }
	"+"             { return new_symbol(sym.PLUS, yytext()); }
	"-"             { return new_symbol(sym.MINUS, yytext()); }
	";"             { return new_symbol(sym.SEMI, yytext()); }
	":"			    { return new_symbol(sym.COLON, yytext()); }
	","             { return new_symbol(sym.COMMA, yytext()); }
	"."             { return new_symbol(sym.DOT, yytext()); }
	"("             { return new_symbol(sym.LPAREN, yytext()); }
	")"             { return new_symbol(sym.RPAREN, yytext()); }
	"["             { return new_symbol(sym.LBRACKET, yytext()); }
	"]"             { return new_symbol(sym.RBRACKET, yytext()); }
	"{"             { return new_symbol(sym.LBRACE, yytext()); }
	"}"             { return new_symbol(sym.RBRACE, yytext()); }
	"?"             { return new_symbol(sym.QUESTION, yytext()); }

	//Komentari
	{Comment}      { /* preskoci komentare */ }

	//Literali
	{IntegerLiteral}       { return new_symbol(sym.INT, Integer.parseInt(yytext())); }
	{BooleanLiteral}   	   { return new_symbol(sym.BOOL, Boolean.valueOf(yytext())); }
	{PrintableCharLiteral} { return new_symbol(sym.CHAR, Character.valueOf(yytext().charAt(1))); }

	//Identifikatori
	{Identifier}          { return new_symbol(sym.IDENT, yytext()); }

	//Greske
	.                    { log.error("Leksicka greska ("+yytext()+") u liniji "+(yyline+1)+",kolona: "+yycolumn); }
}






