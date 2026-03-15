grammar MiniJava;

// --- REGRAS SINTÁTICAS (Parser) ---
// Regra inicial (o ponto de partida do seu compilador)
goal: 'class' Identifier '{' '}' EOF; 

// --- REGRAS LÉXICAS (Lexer) ---
// Identificadores (nomes de variáveis, classes, etc)
Identifier: [a-zA-Z_][a-zA-Z0-9_]*;

// Ignorar espaços em branco, quebras de linha e tabulações
WS: [ \t\r\n]+ -> skip;