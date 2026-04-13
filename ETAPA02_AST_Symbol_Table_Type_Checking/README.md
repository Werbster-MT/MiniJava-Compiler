# Compilador MiniJava para arquitetura MIPS — AST, Tabela de Simbolos e Verificacao de Tipos [Etapa 02]

**Equipe 19**
- Werbster Marques Teixeira [537205]
- Guilherme Gomes Botelho [539008]

---

## Descricao

Esta etapa corresponde a **segunda fase** do desenvolvimento do compilador MiniJava. A partir da analise lexica e sintatica (etapa anterior), o projeto agora:

- constroi a **AST** (Abstract Syntax Tree) a partir da Parse Tree do ANTLR;
- percorre a AST para montar a **Tabela de Simbolos** (classes, campos, metodos, parametros e variaveis locais);
- executa a **Verificacao de Tipos** para identificar erros semanticos.

O pipeline executado em `Main.java` e:

1. Leitura da entrada (`arquivo.mj` ou `stdin`);
2. Analise lexica e sintatica com ANTLR;
3. Construcao da AST com `BuildASTVisitor`;
4. Pretty print da AST;
5. Construcao da tabela de simbolos com `SymbolTableBuilder`;
6. Verificacao de tipos com `TypeCheckVisitor`.

---

## Status da Etapa

A etapa esta **funcional e validada com casos de teste semanticos**.

Implementacoes concluidas:

- Conversao Parse Tree -> AST (`visitor/BuildASTVisitor.java`);
- Estruturas da AST (`syntaxtree/*`);
- Estruturas de simbolos (`symboltable/*`);
- Visitor de construcao da tabela de simbolos (`symboltable/SymbolTableBuilder.java`);
- Visitor de verificacao de tipos (`visitor/TypeCheckVisitor.java`);
- Impressao da AST e resumo da tabela de simbolos para diagnostico.


---

## Erros de Execução Encontrados

Nao foram observadas excecoes de execucao (crash) durante os testes da etapa.

Quando ha problema semantico, o compilador reporta mensagens no formato:

`[ERRO SEMANTICO] ...`

Ao final, se houver erros, o processo encerra com falha e resumo:

`[ETAPA 04] Verificacao de tipos: N erro(s) semantico(s) encontrado(s).`

Exemplos de erros detectados:

- Condicao de `if`/`while` com tipo diferente de `boolean`;
- Atribuicao com tipos incompativeis;
- Variavel nao declarada;
- Operacoes aritmeticas com tipos invalidos;
- Chamada de metodo inexistente.

---

## Estrutura do Projeto

```text
ETAPA02_AST_Symbol_Table_Type_Checking/
├── MiniJava.g4
├── Main.java
├── build.ps1
├── parser/                     # Gerado pelo ANTLR (lexer/parser/visitor)
├── syntaxtree/                 # Nos da AST (Program, ClassDecl, Exp, Statement, etc.)
├── visitor/                    # Visitors (BuildASTVisitor, PrettyPrint, TypeCheck)
├── symboltable/                # Tabela de simbolos e bindings
├── testes/                     # Casos de teste semanticos
│   ├── semantico_valido_01_factorial.mj
│   ├── semantico_valido_02_arrays_while.mj
│   ├── semantico_valido_03_objetos_logica.mj
│   ├── semantico_invalido_01_if_com_int.mj
│   ├── semantico_invalido_02_while_com_int.mj
│   ├── semantico_invalido_03_atribuicao_tipo_errado.mj
│   ├── semantico_invalido_04_variavel_nao_declarada.mj
│   ├── semantico_invalido_05_soma_de_booleanos.mj
│   └── semantico_invalido_06_metodo_inexistente.mj
└── imgs/                       # Capturas dos testes executados
```

---

## Pré-Requisitos

- **Java JDK** 8 ou superior instalado e configurado no `PATH`
- **ANTLR 4.13.2** — arquivo JAR completo (`antlr-4.13.2-complete.jar`) disponível localmente
  - Download: [https://www.antlr.org/download/antlr-4.13.2-complete.jar](https://www.antlr.org/download/antlr-4.13.2-complete.jar)
  - Recomenda-se salvar em `C:\antlr\antlr-4.13.2-complete.jar`
- Variável de ambiente `CLASSPATH` configurada para incluir o JAR do ANTLR e o diretório atual:
  ```
  set CLASSPATH=.;C:\antlr\antlr-4.13.2-complete.jar
  ```

---

## Setup

No PowerShell (dentro de `ETAPA02_AST_Symbol_Table_Type_Checking`):

```powershell
.\build.ps1
```

O script faz automaticamente:

1. Gera parser/lexer/visitor do ANTLR no pacote `parser`;
2. Compila todos os fontes Java da etapa.

Se preferir executar manualmente:

```powershell
java -jar "C:\antlr\antlr-4.13.2-complete.jar" -visitor -package parser -o ".\parser" ".\MiniJava.g4"
javac -cp ".;C:\antlr\antlr-4.13.2-complete.jar" parser\*.java syntaxtree\*.java visitor\*.java symboltable\*.java Main.java
```

---

## Execução do Programa

Execucao geral:

```powershell
java -cp ".;C:\antlr\antlr-4.13.2-complete.jar" Main .\testes\semantico_valido_01_factorial.mj
```

Executar todos os testes validos:

```powershell
java -cp ".;C:\antlr\antlr-4.13.2-complete.jar" Main .\testes\semantico_valido_01_factorial.mj
java -cp ".;C:\antlr\antlr-4.13.2-complete.jar" Main .\testes\semantico_valido_02_arrays_while.mj
java -cp ".;C:\antlr\antlr-4.13.2-complete.jar" Main .\testes\semantico_valido_03_objetos_logica.mj
```

Executar testes invalidos (esperado: erro semantico):

```powershell
java -cp ".;C:\antlr\antlr-4.13.2-complete.jar" Main .\testes\semantico_invalido_01_if_com_int.mj
java -cp ".;C:\antlr\antlr-4.13.2-complete.jar" Main .\testes\semantico_invalido_02_while_com_int.mj
java -cp ".;C:\antlr\antlr-4.13.2-complete.jar" Main .\testes\semantico_invalido_03_atribuicao_tipo_errado.mj
java -cp ".;C:\antlr\antlr-4.13.2-complete.jar" Main .\testes\semantico_invalido_04_variavel_nao_declarada.mj
java -cp ".;C:\antlr\antlr-4.13.2-complete.jar" Main .\testes\semantico_invalido_05_soma_de_booleanos.mj
java -cp ".;C:\antlr\antlr-4.13.2-complete.jar" Main .\testes\semantico_invalido_06_metodo_inexistente.mj
```

---

## Testes Realizados

Foram executados **3 testes semanticos validos** e **6 testes semanticos invalidos**.

---

### Entradas Válidas

#### `semantico_valido_01_factorial.mj`
- Cobre chamada de metodo, recursao, `if/else`, operacoes aritmeticas e retorno inteiro.
- Resultado esperado: AST + tabela de simbolos + verificacao de tipos sem erros.
![Execucao teste semantico_valido_01_factorial](imgs/semantico_valido_01_factorial.png)

#### `semantico_valido_02_arrays_while.mj`
- Cobre `new int[size]`, acesso e atribuicao em array, `while`, `.length`, soma acumulada.
- Resultado esperado: nenhum erro semantico.
![Execucao teste semantico_valido_02_arrays_while](imgs/semantico_valido_02_arrays_while.png)

#### `semantico_valido_03_objetos_logica.mj`
- Cobre comparacao `<`, operador `&&`, parametro `boolean`, bloco `if/else`.
- Resultado esperado: nenhum erro semantico.
![Execucao teste semantico_valido_03_objetos_logica](imgs/semantico_valido_03_objetos_logica.png)

---

### Entradas Inválidas

#### `semantico_invalido_01_if_com_int.mj`
- Erro esperado: condicao do `if` com `int`.
- Mensagem esperada: `Condicao do 'if' deve ser boolean, recebeu: int`
![Execucao teste semantico_invalido_01_if_com_int](imgs/semantico_invalido_01_if_com_int.png)

#### `semantico_invalido_02_while_com_int.mj`
- Erro esperado: condicao do `while` com `int`.
- Mensagem esperada: `Condicao do 'while' deve ser boolean, recebeu: int`
![Execucao teste semantico_invalido_02_while_com_int](imgs/semantico_invalido_02_while_com_int.png)

#### `semantico_invalido_03_atribuicao_tipo_errado.mj`
- Erro esperado: atribuicao de `boolean` em variavel `int`.
- Mensagem esperada: `Tipo incompativel em atribuicao de 'x': esperado int, recebeu boolean`
![Execucao teste semantico_invalido_03_atribuicao_tipo_errado](imgs/semantico_invalido_03_atribuicao_tipo_errado.png)

#### `semantico_invalido_04_variavel_nao_declarada.mj`
- Erro esperado: uso de variavel nao declarada.
- Mensagem esperada: `Variavel nao declarada: 'z'`
![Execucao teste semantico_invalido_04_variavel_nao_declarada](imgs/semantico_invalido_04_variavel_nao_declarada.png)

#### `semantico_invalido_05_soma_de_booleanos.mj`
- Erro esperado: uso de `+` com operandos booleanos.
- Mensagens esperadas:
  - `'+' exige int no lado esquerdo`
  - `'+' exige int no lado direito`
![Execucao teste semantico_invalido_05_soma_de_booleanos](imgs/semantico_invalido_05_soma_de_booleanos.png)

#### `semantico_invalido_06_metodo_inexistente.mj`
- Erro esperado: chamada de metodo nao declarado em classe.
- Mensagem esperada: `Metodo 'voar' nao encontrado na classe 'Carro'`
![Execucao teste semantico_invalido_06_metodo_inexistente](imgs/semantico_invalido_06_metodo_inexistente.png)

---

## Dificuldades Encontradas

- **Mapeamento Parse Tree para AST:** transformar cada regra da gramatica ANTLR em nos da AST exigiu cuidado para preservar estrutura e precedencia das expressoes.
- **Contexto semantico (classe/metodo atual):** a verificacao de tipos depende de contexto correto para buscar variaveis locais, parametros e campos.
- **Compatibilidade de tipos com heranca:** foi necessario tratar compatibilidade entre classes (`subclasse` atribuivel para tipo da `superclasse`).
- **Organizacao dos visitors:** separar responsabilidades entre construcao da AST, construcao da tabela e type checking foi importante para manter codigo legivel e extensivel.
- **Fluxo de diagnostico:** padronizar mensagens de erro semantico para facilitar validacao com os casos de teste.

---

## Participação

| Membro | Participação |
|---|---|
| Werbster Marques Teixeira [537205] | Implementacao do pipeline da etapa 02 em `Main.java`, construcao/ajustes de AST e tabela de simbolos, elaboracao e validacao dos testes semanticos, documentacao do README |
| Guilherme Gomes Botelho [539008] | Implementacao e revisao da verificacao de tipos, apoio na modelagem da AST e da tabela de simbolos, revisao dos casos de teste e do README |
