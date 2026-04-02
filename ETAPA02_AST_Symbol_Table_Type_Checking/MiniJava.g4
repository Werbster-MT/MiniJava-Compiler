grammar MiniJava;

// --- REGRAS SINTÁTICAS (Parser) ---
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
    : 'class' Identifier '{' varDecl* methodDecl* '}'                      # ClassSimple
    | 'class' Identifier 'extends' Identifier '{' varDecl* methodDecl* '}' # ClassExtends
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
    : 'int' '[' ']'  # TypeIntArray
    | 'boolean'      # TypeBoolean
    | 'int'          # TypeInt
    | Identifier     # TypeIdentifier
    ;

statement
    : '{' statement* '}'                             # StmtBlock
    | 'if' '(' exp ')' statement 'else' statement    # StmtIf
    | 'while' '(' exp ')' statement                  # StmtWhile
    | 'System.out.println' '(' exp ')' ';'           # StmtPrint
    | Identifier '=' exp ';'                         # StmtAssign
    | Identifier '[' exp ']' '=' exp ';'             # StmtArrayAssign
    ;

exp
    : exp '[' exp ']'                                # ExpArrayAccess
    | exp '.' 'length'                               # ExpLength
    | exp '.' Identifier '(' expList? ')'            # ExpMethodCall
    | exp '*' exp                                    # ExpTimes
    | exp '+' exp                                    # ExpPlus
    | exp '-' exp                                    # ExpMinus
    | exp '<' exp                                    # ExpLessThan
    | exp '&&' exp                                   # ExpAnd
    | INTEGER_LITERAL                                # ExpIntLiteral
    | 'true'                                         # ExpTrue
    | 'false'                                        # ExpFalse
    | Identifier                                     # ExpIdentifier
    | 'this'                                         # ExpThis
    | 'new' 'int' '[' exp ']'                        # ExpNewArray
    | 'new' Identifier '(' ')'                       # ExpNewObject
    | '!' exp                                        # ExpNot
    | '(' exp ')'                                    # ExpParenthesized
    ;

expList
    : exp expRest*
    ;

expRest
    : ',' exp
    ;

// --- REGRAS LÉXICAS (Lexer) ---
INTEGER_LITERAL : [0-9]+ ;
Identifier      : [a-zA-Z][a-zA-Z0-9_]* ;
WS              : [ \t\r\n]+ -> skip ;
LINE_COMMENT    : '//' ~[\r\n]* -> skip ;
BLOCK_COMMENT   : '/*' .*? '*/' -> skip ;
