# Compilador MiniJava para arquitetura MIPS — Registros de Ativação, Código Intermediário e Código Canônico [Etapa 03]

**Equipe 19**
- Werbster Marques Teixeira [537205]
- Guilherme Gomes Botelho [539008]

---

## Descrição

Esta etapa corresponde à **terceira fase** do desenvolvimento do compilador MiniJava para MIPS. Com a AST validada semanticamente pela ETAPA02, o objetivo agora é **traduzir a AST para uma representação intermediária de baixo nível** (IR Tree), independente de arquitetura, e em seguida **normalizar essa representação** para código canônico, pronto para a seleção de instruções da ETAPA04.

As três grandes responsabilidades desta etapa são:

1. **Registros de Ativação (Activation Records / Stack Frames):** modelar como cada chamada de método organiza sua memória na pilha — parâmetros, variáveis locais, endereço de retorno e `static link` (para acesso a variáveis de escopo externo).

2. **Geração de Código Intermediário (IR Tree):** traduzir cada nó da AST em subárvores da IR Tree (`Tree.Stm` / `Tree.Exp`) usando o framework já fornecido no pacote `Tree` (`BINOP`, `CALL`, `CJUMP`, `CONST`, `ESEQ`, `EXP`, `JUMP`, `LABEL`, `MEM`, `MOVE`, `NAME`, `SEQ`, `TEMP`).

3. **Canonização (Código Canônico):** aplicar as transformações `Canon.linearize` → `BasicBlocks` → `TraceSchedule` para eliminar `SEQ` e `ESEQ`, garantir que cada `CALL` salve o resultado num `TEMP`, e reorganizar os blocos em traces, gerando uma lista linear de instruções sem efeitos colaterais aninhados.

---

## Status da Etapa

A etapa foi **completamente concluída**.

Foram implementadas/concluídas as seguintes funcionalidades:

- Implementação de `frame/Access.java` e `frame/Frame.java`
- Implementação de `mips/InFrame.java`, `mips/InReg.java` e `mips/MipsFrame.java`
- Implementação de `visitor/IRGenVisitor.java` para traduzir os principais nós da AST para IR
- Integração da ETAPA03 com a ETAPA02 em `Main.java` (parse + AST + tabela + typecheck + IR + canonização)
- Pipeline canônico funcionando: `Canon.linearize` → `BasicBlocks` → `TraceSchedule`
- Scripts de build e execução: `build.ps1` e `run.ps1`
- `procEntryExit1` implementado em `MipsFrame`, sendo responsável por adicionar o rótulo de entrada do método e garantir a estrutura básica do corpo na IR.
- Como em MiniJava as variáveis e parâmetros não escapam, o *escape analysis* atribui `false` para o escape de todos os locais e parâmetros de forma segura.

---

## Erros de Execução Encontrados

Nenhum erro de execução (*runtime exception*) foi identificado nas entradas testadas. Todas as entradas válidas geraram as árvores IR e código canônico corretamente. As entradas inválidas foram interceptadas nas fases anteriores (sintática ou semântica) abortando a execução com erro no `stderr` sem interromper abruptamente (*crash*) e sem chegar a gerar uma IR mal formada.

| Entrada | Tipo de erro reportado | Houve exception? |
|---|---|---|
| `ir_invalido_01_sintaxe.mj` | Erro sintático: `mismatched input ')' expecting ...` | Não |
| `ir_invalido_02_semantica.mj` | Erro semântico: `Tipo incompatível em atribuição de 'b': esperado boolean, recebeu int` | Não |

---

## O que o Framework já fornece

Os arquivos abaixo **já estão implementados** no projeto e devem ser utilizados sem alteração:

### Pacote `Temp` — `activation records/`

| Arquivo | Papel |
|---|---|
| `Temp.java` | Registrador virtual (`t0`, `t1`, …); gerado por `new Temp.Temp()` |
| `Label.java` | Endereço de assembly (`L0`, `L1`, … ou nome explícito); gerado por `new Temp.Label()` |
| `TempList.java` | Lista encadeada de `Temp` |
| `LabelList.java` | Lista encadeada de `Label` |
| `TempMap.java` | Interface: mapeia `Temp → String` (nome de registrador) |
| `DefaultMap.java` | Implementação padrão de `TempMap` (usa `t.toString()`) |
| `CombineMap.java` | Composição de dois `TempMap` (tenta o primeiro, cai no segundo) |
| `BoolList.java` | Lista de booleanos (usada para marcar escape de variáveis) |

### Pacote `Tree` — `IR Tree/`

| Arquivo | Tipo | Semântica |
|---|---|---|
| `Stm.java` | Abstrata | Base de todos os *statements* IR |
| `Exp1.java` (classe `Exp`) | Abstrata | Base de todas as *expressões* IR |
| `ExpList.java` | Lista | Lista encadeada de `Exp` |
| `StmList.java` | Lista | Lista encadeada de `Stm` |
| `SEQ.java` | `Stm` | Sequência de dois `Stm` |
| `LABEL.java` | `Stm` | Define um rótulo no código |
| `JUMP.java` | `Stm` | Desvio incondicional |
| `CJUMP.java` | `Stm` | Desvio condicional (EQ, NE, LT, GT, LE, GE, …) |
| `MOVE.java` | `Stm` | Atribuição: `dst ← src` |
| `EXP.java` | `Stm` | Avalia `Exp` e descarta resultado |
| `BINOP.java` | `Exp` | Operação binária (PLUS, MINUS, MUL, DIV, AND, OR, …) |
| `MEM.java` | `Exp` | Leitura/escrita de memória no endereço `exp` |
| `TEMP.java` | `Exp` | Referência a um registrador virtual |
| `NAME.java` | `Exp` | Valor de um `Label` (endereço) |
| `CONST.java` | `Exp` | Constante inteira |
| `CALL.java` | `Exp` | Chamada de função: `func(args…)` |
| `ESEQ.java` | `Exp` | Executa `stm`, depois avalia `exp` |
| `Print.java` | Utilitário | Impressão formatada da IR Tree (debug) |

### Pacote `Canon` — `basic blocks/`

| Arquivo | Papel |
|---|---|
| `Canon.java` | `Canon.linearize(stm)` → `Tree.StmList` sem `SEQ`/`ESEQ` |
| `BasicBlocks.java` | Divide `StmList` em blocos básicos (`StmListList`) |
| `StmListList.java` | Lista de listas de `Stm` (representa os blocos) |
| `TraceSchedule.java` | Reordena blocos em traces; garante que `CJUMP` cai no ramo falso |

---

## O que precisa ser implementado

### 1. Frame — Registro de Ativação MIPS (`mips/MipsFrame.java`)

O **frame** descreve como cada método aloca sua memória na pilha. Para MIPS, é preciso criar:

#### 1.1. Interface `frame/Frame.java`

```java
package frame;

public abstract class Frame {
    public Temp.Label name;       // rótulo de entrada do método
    public Temp.TempList formals; // Temps dos parâmetros (após escaping)
    
    public abstract frame.Access allocLocal(boolean escape);
    public abstract Tree.Exp exp(frame.Access acc, Tree.Exp fp);
    public abstract Tree.Stm procEntryExit1(Tree.Stm body);
    public abstract Tree.Exp externalCall(String func, Tree.ExpList args);
    public abstract Temp.Temp FP();  // frame pointer
    public abstract Temp.Temp RV();  // return value
    public abstract int wordSize();
}
```

#### 1.2. Interface `frame/Access.java`

Representa onde uma variável vive (em registrador ou na memória):

```java
package frame;

public abstract class Access {
    public abstract Tree.Exp exp(Tree.Exp framePtr);
}
```

#### 1.3. `mips/InFrame.java` — variável no frame (na pilha)

```java
package mips;

class InFrame extends frame.Access {
    int offset; // deslocamento em relação ao FP
    InFrame(int o) { offset = o; }

    public Tree.Exp exp(Tree.Exp framePtr) {
        // MEM(BINOP(PLUS, fp, CONST(offset)))
        return new Tree.MEM(
            new Tree.BINOP(Tree.BINOP.PLUS, framePtr, new Tree.CONST(offset))
        );
    }
}
```

#### 1.4. `mips/InReg.java` — variável em registrador virtual

```java
package mips;

class InReg extends frame.Access {
    Temp.Temp temp;
    InReg(Temp.Temp t) { temp = t; }

    public Tree.Exp exp(Tree.Exp framePtr) {
        return new Tree.TEMP(temp);
    }
}
```

#### 1.5. `mips/MipsFrame.java` — frame concreto MIPS

Responsabilidades:
- Gerenciar o deslocamento corrente (`offset`) para alocar variáveis na pilha
- Decidir se cada parâmetro/local *escapa* (vai para memória) ou não (fica em registrador)
- Implementar `procEntryExit1` para salvar/restaurar `$ra`, callee-saves e mover parâmetros do frame para os temps

```java
package mips;

public class MipsFrame extends frame.Frame {
    // ...
}
```

---

### 2. Visitor de Geração de Código Intermediário (`visitor/IRGenVisitor.java`)

Este é o **núcleo da etapa**: um visitor que percorre cada nó da AST (do pacote `syntaxtree`) e devolve um nó da IR Tree.

```java
package visitor;

import syntaxtree.*;
import Tree.*;

public class IRGenVisitor {
    // ...
}
```

#### Mapeamentos dos nós AST → IR Tree

| Nó AST | IR Tree resultante |
|---|---|
| `IntegerLiteral(n)` | `CONST(n)` |
| `True` | `CONST(1)` |
| `False` | `CONST(0)` |
| `This` | `TEMP(fp)` |
| `IdentifierExp(x)` | `frame.Access.exp(TEMP(fp))` → `TEMP(t)` ou `MEM(BINOP(+, fp, CONST(off)))` |
| `Plus(e1, e2)` | `BINOP(PLUS, transExp(e1), transExp(e2))` |
| `Minus(e1, e2)` | `BINOP(MINUS, transExp(e1), transExp(e2))` |
| `Times(e1, e2)` | `BINOP(MUL, transExp(e1), transExp(e2))` |
| `LessThan(e1, e2)` | `ESEQ(CJUMP(LT,e1,e2,t,f), ...)` com dois `LABEL`s e um `TEMP` resultado |
| `And(e1, e2)` | Short-circuit: `ESEQ(SEQ(...), TEMP(result))` |
| `Not(e)` | `BINOP(XOR, transExp(e), CONST(1))` |
| `NewObject(C)` | `CALL(NAME("malloc"), CONST(campos * wordSize))` |
| `NewArray(e)` | `CALL(NAME("malloc"), BINOP(MUL, transExp(e), CONST(4)))` |
| `ArrayLookup(arr, i)` | `MEM(BINOP(PLUS, transExp(arr), BINOP(MUL, transExp(i), CONST(4))))` |
| `ArrayLength(arr)` | `MEM(BINOP(MINUS, transExp(arr), CONST(4)))` (tamanho antes do arr) |
| `Call(obj, m, args)` | `CALL(NAME(label_do_método), ExpList(transExp(obj), args...))` |
| `Assign(x, e)` | `MOVE(acesso_de_x, transExp(e))` |
| `ArrayAssign(a,i,e)` | `MOVE(MEM(endereço), transExp(e))` |
| `If(cond, s1, s2)` | `SEQ(CJUMP(NE,cond,0,lt,lf), SEQ(LABEL(lt),SEQ(s1,SEQ(JUMP(ldone),SEQ(LABEL(lf),SEQ(s2,LABEL(ldone)))))))` |
| `While(cond, s)` | `SEQ(LABEL(ltest), SEQ(CJUMP(EQ,cond,0,ldone,lbody), SEQ(LABEL(lbody), SEQ(s, SEQ(JUMP(ltest), LABEL(ldone))))))` |
| `Print(e)` | `EXP(CALL(NAME("print_int"), ExpList(transExp(e), null)))` |
| `Block(stmts)` | `SEQ(transStm(s0), SEQ(transStm(s1), ...))` |

---

### 3. Canonização — aplicar o pipeline do `Canon`

Após gerar a IR Tree de cada método, aplica-se o pipeline de três passos para obter código canônico:

```java
// 1. Linearizar: elimina SEQ e ESEQ, retorna StmList
Tree.StmList linear = Canon.linearize(irTreeDoMetodo);

// 2. Blocos básicos: divide em blocos com exatamente 1 LABEL no início
//    e 1 JUMP/CJUMP no final
Canon.BasicBlocks blocos = new Canon.BasicBlocks(linear);

// 3. Trace schedule: reordena blocos para que CJUMP caia sempre no ramo falso
Canon.TraceSchedule traces = new Canon.TraceSchedule(blocos);

// traces.stms é a StmList final — entrada para a ETAPA04
Tree.StmList codigoCanônico = traces.stms;
```

---

## Estrutura do Projeto

```
ETAPA03_Activation_Records_Intermediate_Code_Canonical_Code/
│
├── activation records/          # Pacote Temp (fornecido — não alterar)
│   ├── Temp.java                # Registrador virtual
│   ├── Label.java               # Rótulo de assembly
│   ├── TempList.java
│   ├── LabelList.java
│   ├── TempMap.java             # Interface mapeamento Temp→String
│   ├── DefaultMap.java
│   ├── CombineMap.java
│   └── BoolList.java
│
├── IR Tree/                     # Pacote Tree (fornecido — não alterar)
│   ├── Stm.java / Exp.java     # Classes base abstratas
│   ├── SEQ, LABEL, JUMP, CJUMP, MOVE, EXP  (Stm)
│   ├── BINOP, MEM, TEMP, NAME, CONST, CALL, ESEQ  (Exp)
│   ├── ExpList.java / StmList.java
│   └── Print.java               # Impressão da IR Tree para debug
│
├── basic blocks/                # Pacote Canon (fornecido — não alterar)
│   ├── Canon.java               # Linearização
│   ├── BasicBlocks.java         # Particionamento em blocos
│   ├── TraceSchedule.java       # Ordenação em traces
│   └── StmListList.java
│
├── frame/                       # [A IMPLEMENTAR] Interface do frame
│   ├── Frame.java
│   └── Access.java
│
├── mips/                        # [A IMPLEMENTAR] Frame concreto MIPS
│   ├── MipsFrame.java
│   ├── InFrame.java
│   └── InReg.java
│
├── visitor/                     # [A IMPLEMENTAR] Geração de IR Tree
│   └── IRGenVisitor.java
│
├── Main.java                    # Ponto de entrada
├── build.ps1
├── run.ps1
└── testes/
    ├── ir_valido_01_factorial.mj
    ├── ir_valido_02_arrays_while.mj
    ├── ir_valido_03_objetos_logica.mj
    ├── ir_invalido_01_sintaxe.mj
    └── ir_invalido_02_semantica.mj
```

---

## Integração com ETAPA02

O compilador já possui, da ETAPA02:
- **AST** completa (`syntaxtree/`)
- **Tabela de Símbolos** (`symboltable/SymbolTable`)
- **Verificação Semântica** (`visitor/TypeCheckVisitor`)

O `Main.java` da ETAPA03 reutiliza o mesmo pipeline de parsing e construção da AST, e em seguida invoca o novo `IRGenVisitor`.

---

## Pré-Requisitos

- **Java JDK** 8 ou superior instalado e configurado no `PATH`
- **ANTLR 4.13.2** — arquivo JAR completo (`antlr-4.13.2-complete.jar`) disponível localmente
  - Download: [https://www.antlr.org/download/antlr-4.13.2-complete.jar](https://www.antlr.org/download/antlr-4.13.2-complete.jar)
  - Recomenda-se salvar em `C:\antlr\antlr-4.13.2-complete.jar`
- ETAPA02 disponível no mesmo workspace (classes de `syntaxtree`, `visitor` e `symboltable`)
- Variável de ambiente `CLASSPATH` configurada para incluir o JAR do ANTLR e o diretório da Etapa02:
  ```
  set CLASSPATH=.;C:\antlr\antlr-4.13.2-complete.jar;..\ETAPA03_Activation_Records_Intermediate_Code_Canonical_Code
  ```

---

## Setup

No diretório `ETAPA03_Activation_Records_Intermediate_Code_Canonical_Code`, você pode gerar o build rodando:

```powershell
.\build.ps1
```

Isso compilará as classes dos diretórios e da ETAPA02. Saída esperada:

```text
Build ETAPA03 concluido com sucesso!
```

---

## Execução do Programa

Recomenda-se compilar o projeto e executar os arquivos de teste via powershell usando:

```powershell
> powershell

> .\build.ps1

> .\run.ps1
```

Mas também é possível compilar manualmente e testar arquivos um por um:

```powershell
java -cp ".;C:\antlr\antlr-4.13.2-complete.jar;..\ETAPA03_Activation_Records_Intermediate_Code_Canonical_Code" Main "testes\ir_valido_01_factorial.mj"
```

---

## Testes Realizados

---

### Entradas Válidas

Programas que seguem a gramática MiniJava, passam pela análise semântica e chegam a ter sua Árvore de Código Intermediário (IR) gerada.

---

#### `ir_valido_01_factorial.mj`

Testa: chamadas de método e recursão.

```text
===== Factorial_main =====
LABEL L7
MOVE(
 TEMP t12,
 TEMP t0)
...
MOVE(
 TEMP t37,
 CALL(
  NAME malloc,
   CONST 0))
MOVE(
 TEMP t36,
 CALL(
  NAME Fac_ComputeFac,
   TEMP t37,
   CONST 10))
EXP(
 CALL(
  NAME print_int,
   TEMP t36))
...
```

---

#### `ir_valido_02_arrays_while.mj`

Testa: arrays (`new int[]`, `arr[i]`, `arr.length`) e laços `while`.

```text
===== Arr_run =====
LABEL Arr_run
MOVE(
 TEMP t25,
 CALL(
  NAME malloc,
   BINOP(MUL,
    TEMP t24,
    CONST 4)))
...
MOVE(
 TEMP t22,
 BINOP(PLUS,
  TEMP t27,
  MEM(
   BINOP(MINUS,
    TEMP t25,
    CONST 4))))
...
```

---

#### `ir_valido_03_objetos_logica.mj`

Testa: operadores aritméticos e lógicos (`<`, `&&`, `!`) e passagem de `this` como argumento implícito em chamadas de método.

```text
===== Calc_run =====
LABEL Calc_run
...
CJUMP(LT,
 TEMP t24,
 TEMP t25,
 L6,L7)
...
CJUMP(EQ,
 BINOP(XOR,
  TEMP t29,
  CONST 1),
 CONST 0,
 L4,L5)
...
```

---

### Entradas Inválidas

Programas com erros propositais (seja de sintaxe ou semântica) que são detectados antes que a IR seja gerada. O programa aborta para não gerar representações mal-formadas.

---

#### `ir_invalido_01_sintaxe.mj`

```text
line 3:31 mismatched input ')' expecting {'(', 'true', 'false', 'this', 'new', '!', INTEGER_LITERAL, Identifier}
Erros sintaticos encontrados. Abortando.
```

---

#### `ir_invalido_02_semantica.mj`

```text
[ERRO SEMÂNTICO] Tipo incompatível em atribuição de 'b': esperado boolean, recebeu int
Erros semanticos encontrados. Abortando.
```

---

## Dificuldades Encontradas

- **Decisão de escape:** embora a análise de escape ideal exija uma passagem prévia na AST para linguagens complexas, em MiniJava (onde variáveis não escapam) foi possível adotar uma estratégia determinística e segura (`escape = false`).
- **Tradução de booleanos com curto-circuito (`&&`, `!`):** foi necessário modelar `&&` com `CJUMP`s encadeados e labels intermediários.
- **`LessThan` como expressão:** como `CJUMP` é `Stm`, foi necessário encapsular em `ESEQ` com `TEMP` resultado.
- **Ordem dos argumentos em `CALL`:** em MiniJava, `this` é argumento implícito e precisa ser o primeiro na `ExpList`.
- **`procEntryExit1` completo:** a implementação de salvar/restaurar `$ra` e callee-saves exigiu o entendimento correto de como mapear registradores para Temporários dentro da IR Tree.
- **Compatibilidade de nomes de arquivos no Windows:** conflito entre nomes que diferem apenas por caixa (como `Exp` e `EXP`) exigiu ajuste para manter build estável no ambiente local.

---

## Participação

| Membro | Participação |
|---|---|
| Werbster Marques Teixeira [537205] | Implementação de `MipsFrame`, `IRGenVisitor`, `Main` da ETAPA03, scripts de build/run e integração do pipeline de canonização |
| Guilherme Gomes Botelho [539008] | Elaboração/organização dos testes da etapa, revisão da IR canônica gerada, validação dos resultados e atualização do README |
