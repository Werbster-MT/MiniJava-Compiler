grammar MiniJava;

// --- REGRAS SINTÁTICAS (Parser) ---
// Regra inicial (o ponto de partida do seu compilador)
goal 
	: mainClass classDecl* EOF
	; 
	
mainClass
	: 'class' Identifier 	
	  '{'
	  'public' 'static' 'void' 'main'
	  '('
	  'String' '[' ']' Identifier ')'
	  '{' statement '}'
	  '}'
	  ;
	  
classDecl
	: 'class' Identifier '{' varDecl* methodDecl* '}'
	| 'class' Identifier 'extends' Identifier '{' varDecl* methodDecl* '}'
	;
	
varDecl
	: type Identifier ';'
	;
	
methodDecl
	: 'public' type Identifier '(' formalList? ')'
	  '{' varDecl* statement* 'return' exp ';' '}'
	;
	
formalList
	: type Identifier formalRest*
	;
	
formalRest
	: ',' type Identifier
	;
	
type
	: 'int' '[' ']'
	| 'boolean'
	| 'int'
	| Identifier
	;
	
statement
	: '{' statement* '}'
	| 'if' '(' exp ')' statement 'else' statement
	| 'while' '(' exp ')' statement
	| 'System.out.println' '(' exp ')' ';'
	| Identifier '=' exp ';'
	| Identifier '[' exp ']' '=' exp ';'
	;

exp 
	: exp op exp
	| exp '[' exp ']'
	| exp '.' 'length'
	| exp '.' Identifier '(' expList? ')'
	| INTEGER_LITERAL
	| 'true'
	| 'false'
	| Identifier
	| 'this'
	| 'new' 'int' '[' exp ']'
	| 'new' Identifier '(' ')'
	| '!' exp
	| '(' exp ')'
	;
	
expList
	: exp expRest*
	;
	
expRest
	: ',' exp
	;
	
op
	: '+'
	| '-'
	| '*'
	| '<'
	| '&&'
	;

// --- REGRAS LÉXICAS (Lexer) ---
// Identificadores (nomes de variáveis, classes, etc)
INTEGER_LITERAL
	: [0-9]+
	;

Identifier
	: [a-zA-Z_][a-zA-Z0-9_]*
	;

// Ignorar espaços em branco, quebras de linha e tabulações
WS
	: [ \t\r\n]+ -> skip
	;
