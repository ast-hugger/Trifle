// Copyright (c) 2018 Vassili Bykov. Licensed under the Apache License, Version 2.0.

/*
    Adapted from the Newspeak variant grammar in Victory Coffee;
    not necessarily 100% correct.

    As Smalltalk is lacking a standard top-level class file syntax,
    we are using a homebrew variant vaguely inspired by the classic
    chunk format in its reliance on the exclamation mark.
*/

grammar Smalltalk;

options {
	language = Java;
}


sourceUnit : classDecl methodEntry* EOF;

classDecl
	: IDENTIFIER 'subclass:' IDENTIFIER 'instanceVariables:' '(' instVarName* ')';

instVarName
    : IDENTIFIER;

methodEntry
	: ('!' | '!!') methodDecl;

methodDecl
    : messagePattern (VBAR IDENTIFIER* VBAR)? codeBody;

messagePattern
	: IDENTIFIER # unaryPattern
	| BINARY_SELECTOR IDENTIFIER #binaryPattern
	| (KEYWORD IDENTIFIER)+ #keywordPattern
	;

/*
 *  Code
 */

codeBody
	: /* empty */
	| statement (DOT statement)* DOT?;

statement
	: expression
	| returnStatement ;

returnStatement
	: CARET expression ;

// TODO no cascades yet

expression
    : object
    | assignment
    | messageSend
    ;

object
    : varReference
    | specialReceiver
    | literal
    | LPAREN expression RPAREN
    ;

varReference: IDENTIFIER;

assignment
    : IDENTIFIER ':=' expression;

messageSend
		: unarySend
		| binarySend
		| keywordSend
 		;

binaryReceiver
	  : object
	  | unarySend;

keywordReceiver
    : object
    | unarySend
    | binarySend;


unarySend
		: object unaryMessage;

unaryMessage
    : IDENTIFIER unaryMessage?;

binarySend
		: binaryReceiver binaryMessage;

binaryMessage
		: BINARY_SELECTOR binaryReceiver binaryMessage?;

keywordSend
		: keywordReceiver keywordMessage;

keywordMessage
		: (KEYWORD keywordReceiver)+ ;

specialReceiver
	: nilReceiver
	| trueReceiver
	| falseReceiver
	| selfReceiver
	| superReceiver
	;

nilReceiver : NIL;

trueReceiver : TRUE;

falseReceiver : FALSE;

selfReceiver : SELF;

superReceiver : SUPER;

literal
	: block    # blockLiteral
	| INTEGER  # integerLiteral
	| STRING   # stringLiteral
	;

block :
	LBRACKET blockArgs? blockTemps? codeBody RBRACKET ;

blockArgs :
	BLOCK_ARG* VBAR ;

blockTemps :
	VBAR IDENTIFIER* VBAR ;

/*
 * Lexer
 */

NIL   : 'nil';
TRUE  : 'true';
FALSE : 'false';
SELF  : 'self';
SUPER : 'super';
OUTER : 'outer';

CARET : '^';
COLON : ':';
COMMA : ',';
DOT : '.';
EQUAL_SIGN : '=';
CCE_SIGN : '::=';
LBRACKET : '[';
LCURLY : '{';
LPAREN : '(';
//LANGLE : '<';
POUND : '#';
//RANGLE : '>';
RBRACKET : ']';
RCURLY : '}';
RPAREN : ')';
SEMICOLON : ';';
// SLASH : '/';
VBAR : '|';

BINARY_SELECTOR : '+' | '/' | '*' | '-' | '<' | '>'; // good enough for now

IDENTIFIER : [a-zA-Z_][a-zA-Z_0-9]*;
KEYWORD : IDENTIFIER ':';
SETTER_KEYWORD : KEYWORD ':';
BLOCK_ARG : ':' IDENTIFIER ;

STRING : '\'' ~[']* '\'' ;
INTEGER : [0-9]+;

COMMENT    : '(*' .*? '*)' -> skip;
WHITESPACE : [ \t\r\n]+ -> skip;
