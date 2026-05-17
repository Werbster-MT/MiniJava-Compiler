# Compilador MiniJava para arquitetura MIPS — Seleção de Instruções [Etapa 04]

**Equipe 19**
- Werbster Marques Teixeira [537205]
- Guilherme Gomes Botelho [539008]

---

## Descrição

Esta etapa corresponde à **quarta fase** do desenvolvimento do compilador MiniJava para MIPS. Partindo da Árvore de Código Intermediário Canônica (Canonizada) gerada na ETAPA03, o objetivo agora é **realizar a Seleção de Instruções** (Instruction Selection).

Nesta etapa, traduzimos as operações genéricas da IR (como `MOVE`, `BINOP`, `MEM`) para instruções específicas e literais da arquitetura MIPS (`lw`, `sw`, `add`, `addi`, `beq`, `jal`, etc.). O processo é feito através do algoritmo **Maximal Munch** (Ladrilhamento Guloso), que varre a árvore top-down casando os maiores padrões de instrução possíveis para minimizar o número de instruções geradas.

---

## Status da Etapa

A etapa foi **completamente concluída**.

Foram implementadas/concluídas as seguintes funcionalidades:

- Refatoração dos arquivos da arquitetura no pacote `Assem/` (`Instr`, `OPER`, `MOVE`, `LABEL`, `Targets`, `InstrList`).
- Implementação completa da classe geradora de código `mips/Codegen.java` baseada no padrão de Maximal Munch.
- Implementação dos métodos de varredura `munchStm` e `munchExp`.
- Tradução otimizada de acessos à memória (`MOVE(MEM(BINOP(...)), ...)` para `sw` com offset, e `MEM(BINOP(...))` para `lw` com offset).
- Aproveitamento de constantes em operações aritméticas com `addi`.
- Integração da ETAPA04 com todas as etapas anteriores no `Main.java` (mantendo o estado persistente do `Frame` para recuperar o registrador `RV`).
- Scripts de build e execução (`build.ps1` e `run.ps1`) devidamente atualizados para a nova etapa.
- **Preparação para Liveness Analysis (Etapa 05):** Propagação correta dos registradores `caller-saves` e `RA` na lista de sujos (`calleeDefs`) nas instruções de chamada (`jal`) em `Codegen.java`.
- **Mapeamento de Registradores MIPS:** Adição do suporte a registradores como `$sp`, `$zero` e implementação do método `tempMap` no `MipsFrame.java` para traduzir temps em strings da arquitetura.

---

## Erros de Execução Encontrados

Nenhum erro de execução (*runtime exception*) foi identificado nas entradas testadas. O compilador continua seguro e tolerante, não prosseguindo com a geração de assembly caso o código tenha erros léxicos, sintáticos ou semânticos da etapa 02 e 01.

| Entrada | Tipo de erro reportado | Houve exception? |
|---|---|---|
| `ir_invalido_01_sintaxe.mj` | Erro sintático: `mismatched input ')' expecting ...` | Não |
| `ir_invalido_02_semantica.mj` | Erro semântico: `Tipo incompatível em atribuição de 'b': esperado boolean, recebeu int` | Não |

---

## O que o Framework já fornece

Os arquivos abaixo **já estão implementados** e servem de suporte para gerar o Assembly abstrato com temporários antes da alocação de registradores (que ocorrerá na Etapa 05).

### Pacote `Assem` — `Assem/`

| Arquivo | Papel |
|---|---|
| `Instr.java` | Classe base abstrata para representar uma instrução assembly não formatada. |
| `OPER.java` | Extensão de `Instr` para operações regulares (como `add`, `lw`, `jal`) que possuem registradores de entrada (`use()`) e saída (`def()`). |
| `MOVE.java` | Extensão de `Instr` específica para transferência direta de dados (`move`). |
| `LABEL.java` | Extensão de `Instr` para definir rótulos de desvio (`L0:`). |
| `Targets.java` | Representa uma lista de destinos de desvio para instruções jump. |
| `InstrList.java`| Lista encadeada de instruções. |

### Infraestrutura da ETAPA03 — `Tree/`, `frame/`, `Temp/`
A infraestrutura legada das árvores IR e de ativação de memória permanece operante e alimenta o gerador desta etapa, tendo as mesmas funcionalidades descritas na etapa passada.

---

## O que foi implementado

### 1. Maximal Munch — O Gerador de Código (`mips/Codegen.java`)

Este é o **núcleo da etapa**: a classe `Codegen` consome a IR canônica do método `Canon.traces` e converte recursivamente as instruções `Tree.Stm` em uma lista de `Assem.Instr`.

#### Exemplos de "Ladrilhos" Mapeados (Tiles)

| Padrão IR Tree | Instrução MIPS Gerada |
|---|---|
| `MOVE(MEM(BINOP(PLUS, e, CONST(c))), src)` | `sw src, c(base_e)` |
| `MOVE(TEMP(t), MEM(BINOP(PLUS, e, CONST(c))))` | `lw t, c(base_e)` |
| `MOVE(TEMP(t), CONST(c))` | `li t, c` |
| `MOVE(TEMP(t), e)` | `move t, reg_e` |
| `BINOP(PLUS, e, CONST(c))` | `addi dst, reg_e, c` |
| `BINOP(MINUS, e, CONST(c))` | `addi dst, reg_e, -c` |
| `CALL(NAME(func), args)` | `jal func` |
| `CJUMP(LT, e1, e2, lt, lf)` | `blt reg1, reg2, lt` |

---

## Estrutura do Projeto

```
ETAPA04_Instruction_Selection/
│
├── Assem/                       # Pacote de formatação de instruções abstratas
│   ├── Instr.java, OPER.java, MOVE.java, LABEL.java, Targets.java, InstrList.java
│
├── mips/                        # [IMPLEMENTADO] Código MIPS e Maximal Munch
│   ├── Codegen.java             # Gerador de código MIPS
│   ├── MipsFrame.java, InFrame.java, InReg.java
│
├── visitor/
│   └── IRGenVisitor.java        # Atualizado para reter frames
│
├── Main.java                    # Ponto de entrada atualizado para emitir Assembly
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

## Integração com Etapas Anteriores

O `Main.java` da ETAPA04 executa todas as etapas progressivamente:
1. Lexer e Parser (ETAPA01)
2. Verificação Semântica e Tabelas (ETAPA02)
3. Geração de IR Tree e Código Canônico (ETAPA03)
4. **Seleção de Instruções e Impressão (ETAPA04)**

---

## Pré-Requisitos

- **Java JDK** 8 ou superior instalado e configurado no `PATH`
- **ANTLR 4.13.2** — arquivo JAR completo (`antlr-4.13.2-complete.jar`) disponível localmente
  - Download: [https://www.antlr.org/download/antlr-4.13.2-complete.jar](https://www.antlr.org/download/antlr-4.13.2-complete.jar)
  - Recomenda-se salvar em `C:\antlr\antlr-4.13.2-complete.jar`
- ETAPA02 disponível no mesmo workspace (classes de `syntaxtree`, `visitor` e `symboltable`)
- Variável de ambiente `CLASSPATH` configurada para incluir o JAR do ANTLR e o diretório da Etapa02:
  ```
  set CLASSPATH=.;C:\antlr\antlr-4.13.2-complete.jar;..\ETAPA04_Instruction_Selection
  ```

---

## Setup

No diretório `ETAPA04_Instruction_Selection`, você pode compilar rodando:

```powershell
.\build.ps1
```

Isso compilará as classes do diretório base e da ETAPA02. Saída esperada:

```text
Build ETAPA04 concluido com sucesso!
```

---

## Execução do Programa

Recomenda-se compilar o projeto e executar os arquivos de teste via powershell usando:

```powershell
> powershell

> .\build.ps1

> .\run.ps1
```

Mas também é possível executar manualmente e testar arquivos um por um apontando o arquivo desejado:

```powershell
java -cp ".;C:\antlr\antlr-4.13.2-complete.jar;..\ETAPA02_AST_Symbol_Table_Type_Checking" Main "testes\ir_valido_01_factorial.mj"
```

---

## Testes Realizados e Geração de Assembly

---

### Entradas Válidas

Programas que passam pela gramática e chegam a ter seu código selecionado corretamente, imprimindo a lista de instruções Assembly não-alocadas.

#### `ir_valido_01_factorial.mj`

Demonstra o uso extensivo de alocação, parâmetros (identificados nos temps via registradores simulados) e recursão `CALL` virando `jal`.

```assembly
===== Fac_ComputeFac (Assembly) =====
L9:
	move t27, t0
	move t28, t1
...
Fac_ComputeFac:
	li t26, 0
	li t45, 1
	blt t24, t45, L3
L4:
L5:
	li t46, 0
	bne t26, t46, L0
L1:
	move t44, t24
	addi t47, t24, -1
	jal Fac_ComputeFac
	move t48, t22      # RV (registrador de retorno extraido)
	move t43, t48
	mul t49, t44, t43
	move t25, t49
...
```

---

#### `ir_valido_02_arrays_while.mj`

Valida a instrução ótima de arranjo com memórias (offset em arrays) `lw` e `sw`, aproveitando cálculos estáticos sempre que possível.

```assembly
===== Arr_run (Assembly) =====
...
L1:
	li t51, 4
	mul t50, t26, t51
	add t49, t25, t50
	sw t26, 0(t49)     # Ladrilho para Store word com offset 0
	li t56, 4
	mul t55, t26, t56
	add t54, t25, t55
	lw t53, 0(t54)     # Ladrilho para Load word
	add t52, t27, t53
...
```

---

## Dificuldades Encontradas

- **Aproveitamento Ótimo de Instruções (Maximal Munch):** Encontrar o balanceamento adequado no ladrilhamento (onde o offset deve ser capturado num `lw` vs calcular previamente o endereço de memória e usar offset 0) exigiu refinar os padrões de `Tree.BINOP`.
- **Registrador de Retorno (RV) e Frames:** O `Codegen` utiliza `Frame.RV()` para identificar onde a chamada de função deixará o resultado. Na ETAPA03, a árvore estava isolada da persistência do `MipsFrame`. Foi necessário refatorar o gerador da IR (`IRGenVisitor`) para guardar as referências de frames de cada método analisado e consumi-las no `Main`.
- **Conflito de Nomes de Classes:** O conflito de namespace de classes homônimas da base fornecida (como `Assem.MOVE` e `Tree.MOVE` ou `Assem.LABEL` e `Tree.LABEL`) forçou o uso de caminhos totalmente qualificados na lógica do Maximal Munch para evitar ambiguidades semânticas de qual tipo do compilador estava sendo tratado.

---

## Participação

| Membro | Participação |
|---|---|
| Werbster Marques Teixeira [537205] | Implementação da classe `mips/Codegen.java` (Maximal Munch), refatoração e cópia dos componentes de infraestrutura (IR e Canon), interligação no `Main.java` |
| Guilherme Gomes Botelho [539008] | Execução dos testes via script, análise visual de cobertura dos nós e do Assembly gerado e estruturação do README referenciando a etapa. |
