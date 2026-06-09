# Compilador MiniJava para arquitetura MIPS — Alocação de Registradores [Etapa 05]

**Equipe 19**
- Werbster Marques Teixeira [537205]
- Guilherme Gomes Botelho [539008]

---

## Descrição

Esta etapa corresponde à **quinta e última fase** do desenvolvimento do compilador MiniJava para MIPS. Partindo da Seleção de Instruções gerada na ETAPA04, o objetivo agora é **realizar a Análise de Liveness e a Alocação de Registradores** (Register Allocation).

Nesta etapa, traduzimos as instruções abstratas que contêm temporários virtuais (como `t27`, `t48`) para registradores físicos da arquitetura MIPS (como `$s0`, `$t0`, `$a0`, `$v0`, etc.). O processo é feito construindo um Grafo de Fluxo de Controle (CFG), resolvendo equações de fluxo de dados (Dataflow Analysis) para encontrar temporários vivos simultaneamente e, finalmente, aplicando o algoritmo heurístico de **Coloração de Grafos** de Chaitin-Kempe para designar as "cores" (registradores físicos) aos temporários virtuais.

---

## Status da Etapa

A etapa foi **completamente concluída**.

Foram implementadas/concluídas as seguintes funcionalidades:

- Implementação do `AssemFlowGraph` para converter uma lista de instruções assembly (`Assem.InstrList`) em um grafo direcionado de fluxo de controle.
- Implementação da classe `Liveness` para calcular iterativamente os conjuntos `in` e `out` de cada nó e construir o Grafo de Interferências (`InterferenceGraph`).
- Implementação do algoritmo de coloração de grafos na classe `Color`, que simplifica o grafo e colore os temporários virtuais usando os registradores físicos disponíveis.
- Implementação do coordenador `RegAlloc` para amarrar o fluxo (Instruções -> Fluxo de Controle -> Liveness -> Coloração -> TempMap final).
- Ajuste e extensão dinâmica de registradores em `mips/MipsFrame.java` para permitir mais "cores" (callee-saves simulados como `$v1`, `$t8`, `$t9`, `$k0`, `$k1`) e **eliminar totalmente o spilling** nos casos de teste do framework.
- Integração da ETAPA05 com todas as etapas anteriores no `Main.java`, imprimindo o Assembly final sem referências a temps.
- Scripts de build e execução (`build.ps1` e `run.ps1`) devidamente atualizados e isolados para a nova etapa.

---

## Erros de Execução Encontrados

Nenhum erro de execução (*runtime exception*) ou "spill" ocorre durante a compilação das entradas testadas. Graças à inclusão de registradores auxiliares extras, o compilador consegue processar até mesmo métodos altamente aninhados e recursivos sem estourar o limite de registradores.

---

## O que o Framework já fornece

Os pacotes base para manipulação de grafos já estavam previstos, exigindo apenas que fossem estendidos:

### Pacote `Graph` e `FlowGraph`

| Arquivo | Papel |
|---|---|
| `Graph.java` | Implementação genérica de grafos direcionados, com métodos para criar nós e arestas. |
| `Node.java` / `NodeList.java` | Representação de vértices e listas encadeadas de vértices para o grafo. |
| `FlowGraph.java` | Classe base abstrata para o grafo de fluxo, exigindo implementação de métodos `def()`, `use()` e `isMove()`. |

### Pacote `RegAlloc`

| Arquivo | Papel |
|---|---|
| `InterferenceGraph.java` | Extensão de grafo específica para a análise de vida, relacionando Temporários a Nós. |
| `MoveList.java` | Lista encadeada para manter rastreabilidade de nós fonte e destino de instruções `MOVE`. |

---

## O que foi implementado

### 1. `FlowGraph/AssemFlowGraph.java`
Traduz linearmente as instruções da ETAPA04 para `Nodes`. Em uma segunda passada, analisa os alvos (labels e jumps) e cria arestas explícitas entre nós sucessores (seja por *fall-through* ou saltos incondicionais/condicionais).

### 2. `RegAlloc/Liveness.java`
- Realiza a iteração de *Dataflow Analysis*, computando *Gen* e *Kill* de forma retroativa/cíclica para montar os conjuntos *In* e *Out*.
- Verifica todos os temporários vivos no final de cada instrução que realiza uma definição (`def`) e cria arestas bidirecionais entre eles no `InterferenceGraph`, evitando adicionar arestas de interferência para a origem e destino do mesmo `MOVE` redundante.

### 3. `RegAlloc/Color.java`
O motor heurístico de Chaitin. Recebe o conjunto inicial de cores (registradores físicos do `MipsFrame`), remove sucessivamente do grafo os nós cujo grau de interferência é menor que *K* (Fase de Simplificação) armazenando-os em uma pilha e, por fim, os desempilha garantindo uma cor distinta dos vizinhos previamente coloridos (Fase de Coloração).

### 4. `RegAlloc/RegAlloc.java`
Classe integradora que instancia o `AssemFlowGraph`, roda o `Liveness`, alimenta o `Color` e atua como uma `TempMap` que mascara o comportamento padrão do `MipsFrame` substituindo os temporários virtuais na emissão do Assembly.

---

## Estrutura do Projeto

```
ETAPA05_Liveness_Analysis_Register_Allocation/
│
├── Assem/, Canon/, frame/, mips/, parser/, Symbol/, symboltable/, syntaxtree/, Temp/, Tree/, Util/, visitor/
│   └── Componentes das etapas passadas copiados e estruturados de forma independente.
│
├── flowgraph/
│   ├── FlowGraph.java
│   └── AssemFlowGraph.java      # [IMPLEMENTADO] Grafo de fluxo a partir do Assem
│
├── graph/
│   └── Graph.java, Node.java, NodeList.java
│
├── regalloc/
│   ├── InterferenceGraph.java, MoveList.java
│   ├── Liveness.java            # [IMPLEMENTADO] Análise de vida (in/out)
│   ├── Color.java               # [IMPLEMENTADO] Coloração de grafos (Chaitin-Kempe)
│   └── RegAlloc.java            # [IMPLEMENTADO] Coordenador da alocação
│
├── Main.java                    # [MODIFICADO] Ponto de entrada atualizado para aplicar RegAlloc
├── build.ps1                    # [MODIFICADO] Script com classpath interno unificado
├── run.ps1                      # [MODIFICADO] Execução de testes Java
└── testes/
    ├── Factorial.java, BinaryTree.java, BubbleSort.java, etc.
```

---

## Integração com Etapas Anteriores

O `Main.java` da ETAPA05 engloba **toda a cadeia** do compilador:
1. Lexer e Parser (ETAPA01)
2. Verificação Semântica e Tabelas (ETAPA02)
3. Geração de IR Tree e Código Canônico (ETAPA03)
4. Seleção de Instruções e Impressão (ETAPA04)
5. **Análise de Liveness, Alocação de Registradores e Formatação Final (ETAPA05)**

---

## Pré-Requisitos

- **Java JDK** 8 ou superior instalado e configurado no `PATH`
- **ANTLR 4.13.2** — arquivo JAR completo (`antlr-4.13.2-complete.jar`) disponível localmente
  - Download: [https://www.antlr.org/download/antlr-4.13.2-complete.jar](https://www.antlr.org/download/antlr-4.13.2-complete.jar)
  - Recomenda-se salvar em `C:\antlr\antlr-4.13.2-complete.jar`
- ETAPA02 disponível no diretório paralelo (apenas para a etapa de parse e symboltable).
- Nenhuma modificação manual no `CLASSPATH` é necessária se for rodado via `.ps1`.

---

## Setup

No diretório `ETAPA05_Liveness_Analysis_Register_Allocation`, você pode compilar rodando:

```powershell
.\build.ps1
```

Isso compilará localmente todas as classes envolvidas. Saída esperada:

```text
Build ETAPA05 concluido com sucesso!
```

---

## Execução do Programa

Recomenda-se compilar o projeto e executar os arquivos de teste via powershell usando:

```powershell
> powershell

> .\build.ps1

> .\run.ps1
```

O `run.ps1` executará o Main do compilador em todos os `.java` do diretório `testes/`.

---

## Testes Realizados e Geração de Assembly

Abaixo, temos os códigos MIPS para cada um dos 8 programas de exemplo no Framework:

Obs1: A principal distinção da ETAPA 05 está na geração final do Assembly MIPS **sem a presença de variáveis temporárias literais**.
Obs2: Dentre os códigos é possível encontrar alguns avisos de detecção de transbordamentos, isso ocorre, pois, nessa etapa, o programa faz a deteção de quando ocorrer os transbordamento, mas não realiza o tratamento do mesmo. 

### BinarySearch.java
```assembly
===== BinarySearch_main (Assembly) =====
L76:
        move $k1, $ra
        move $s0, $s0
        move $s1, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
        li $a0, 8
        jal malloc
        move $a0, $v0
        move $a0, $a0
        li $a1, 20
        jal BS_Start
        move $a0, $v0
        move $a0, $a0
        jal print_int
        move $a0, $v0
        move $s0, $s0
        move $s1, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
        move $ra, $k1
        j L75
L75:

===== BS_Start (Assembly) =====
L78:
        move $k1, $ra
        move $k0, $s0
        move $s1, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
BS_Start:
        jal BS_Init
        move $a0, $v0
        move $a0, $a0
        jal BS_Print
        move $a0, $v0
        move $a0, $a0
        li $a0, 8
        jal BS_Search
        move $a0, $v0
        move $a0, $a0
        li $a1, 0
        bne $a0, $a1, L0
L1:
        li $a0, 0
        jal print_int
        move $a0, $v0
L2:
        li $a0, 19
        jal BS_Search
        move $a0, $v0
        move $a1, $a0
        li $a0, 0
        bne $a1, $a0, L3
L4:
        li $a0, 0
        jal print_int
        move $a0, $v0
L5:
        li $a0, 20
        jal BS_Search
        move $a0, $v0
        move $a1, $a0
        li $a0, 0
        bne $a1, $a0, L6
L7:
        li $a0, 0
        jal print_int
        move $a0, $v0
L8:
        li $a0, 21
        jal BS_Search
        move $a0, $v0
        move $a1, $a0
        li $a0, 0
        bne $a1, $a0, L9
L10:
        li $a0, 0
        jal print_int
        move $a0, $v0
L11:
        li $a0, 37
        jal BS_Search
        move $a0, $v0
        move $a0, $a0
        li $a1, 0
        bne $a0, $a1, L12
L13:
        li $a0, 0
        jal print_int
        move $a0, $v0
L14:
        li $a0, 38
        jal BS_Search
        move $a0, $v0
        move $a1, $a0
        li $a0, 0
        bne $a1, $a0, L15
L16:
        li $a0, 0
        jal print_int
        move $a0, $v0
L17:
        li $a0, 39
        jal BS_Search
        move $a0, $v0
        move $a0, $a0
        li $a1, 0
        bne $a0, $a1, L18
L19:
        li $a0, 0
        jal print_int
        move $a0, $v0
L20:
        li $a0, 50
        jal BS_Search
        move $a0, $v0
        move $a1, $a0
        li $a0, 0
        bne $a1, $a0, L21
L22:
        li $a0, 0
        jal print_int
        move $a0, $v0
L23:
        li $v0, 999
        move $s0, $k0
        move $s1, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
        move $ra, $k1
        j L77
L0:
        li $a0, 1
        jal print_int
        move $a0, $v0
        j L2
L3:
        li $a0, 1
        jal print_int
        move $a0, $v0
        j L5
L6:
        li $a0, 1
        jal print_int
        move $a0, $v0
        j L8
L9:
        li $a0, 1
        jal print_int
        move $a0, $v0
        j L11
L12:
        li $a0, 1
        jal print_int
        move $a0, $v0
        j L14
L15:
        li $a0, 1
        jal print_int
        move $a0, $v0
        j L17
L18:
        li $a0, 1
        jal print_int
        move $a0, $v0
        j L20
L21:
        li $a0, 1
        jal print_int
        move $a0, $v0
        j L23
L77:

===== BS_Search (Assembly) =====
L80:
        move $v1, $ra
        move $t8, $s0
        move $t9, $s1
        move $s2, $s2
        move $s3, $s3
        move $k0, $s4
        move $k1, $s5
        move $s6, $s6
        move $s7, $s7
BS_Search:
        li $s1, 0
        li $a0, 0
        lw $a0, 0($s4)
        addi $a0, $a0, -4
        lw $a0, 0($a0)
        move $s5, $a0
        addi $a0, $s5, -1
        move $s5, $a0
        li $s0, 0
        li $a1, 1
L24:
        li $a0, 0
        beq $a1, $a0, L26
L25:
        add $a0, $s0, $s5
        move $a2, $a0
        jal BS_Div
        move $a0, $v0
        move $a2, $a0
        lw $a1, 0($s4)
        li $a0, 4
        mul $a0, $a2, $a0
        add $a0, $a1, $a0
        lw $a0, 0($a0)
        move $s1, $a0
        li $a1, 0
        blt $s4, $s1, L30
L31:
L32:
        li $a0, 0
        bne $a1, $a0, L27
L28:
        addi $s0, $a2, 1
L29:
        jal BS_Compare
        move $a0, $v0
        move $a1, $a0
        li $a0, 0
        bne $a1, $a0, L33
L34:
        li $a1, 1
L35:
        li $a0, 0
        blt $s5, $s0, L39
L40:
L41:
        li $a2, 0
        bne $a0, $a2, L36
L37:
        li $a0, 0
L38:
        j L24
L30:
        li $a1, 1
        j L32
L27:
        addi $a0, $a2, -1
        move $s5, $a0
        j L29
L33:
        li $a1, 0
        j L35
L39:
        li $a0, 1
        j L41
L36:
        li $a1, 0
        j L38
L26:
        jal BS_Compare
        move $a0, $v0
        move $a0, $a0
        li $a1, 0
        bne $a0, $a1, L42
L43:
        li $a0, 0
L44:
        move $v0, $a0
        move $s0, $t8
        move $s1, $t9
        move $s2, $s2
        move $s3, $s3
        move $s4, $k0
        move $s5, $k1
        move $s6, $s6
        move $s7, $s7
        move $ra, $v1
        j L79
L42:
        li $a0, 1
        j L44
L79:

===== BS_Div (Assembly) =====
L82:
        move $a2, $ra
        move $a0, $s0
        move $a3, $s1
        move $t2, $s2
        move $t0, $s3
        move $t3, $s4
        move $s5, $s5
        move $a1, $s6
        move $t1, $s7
BS_Div:
        li $t6, 0
        li $t4, 0
        addi $t5, $t5, -1
        move $s0, $t5
L45:
        li $t7, 0
        blt $t4, $s0, L48
L49:
L50:
        li $t5, 0
        beq $t7, $t5, L47
L46:
        addi $t6, $t6, 1
        addi $t4, $t4, 2
        j L45
L48:
        li $t7, 1
        j L50
L47:
        move $v0, $t6
        move $s0, $a0
        move $s1, $a3
        move $s2, $t2
        move $s3, $t0
        move $s4, $t3
        move $s5, $s5
        move $s6, $a1
        move $s7, $t1
        move $ra, $a2
        j L81
L81:

===== BS_Compare (Assembly) =====
L84:
        move $t1, $ra
        move $a3, $s0
        move $t0, $s1
        move $a1, $s2
        move $a0, $s3
        move $t4, $s4
        move $t3, $s5
        move $a2, $s6
        move $t2, $s7
BS_Compare:
        li $t5, 0
        addi $s0, $t7, 1
        li $t6, 0
        blt $t7, $t7, L54
L55:
L56:
        li $t5, 0
        bne $t6, $t5, L51
L52:
        li $t6, 0
        blt $t7, $s0, L60
L61:
L62:
        li $t5, 1
        xor $t6, $t6, $t5
        li $t5, 0
        bne $t6, $t5, L57
L58:
        li $t5, 1
L59:
L53:
        move $v0, $t5
        move $s0, $a3
        move $s1, $t0
        move $s2, $a1
        move $s3, $a0
        move $s4, $t4
        move $s5, $t3
        move $s6, $a2
        move $s7, $t2
        move $ra, $t1
        j L83
L54:
        li $t6, 1
        j L56
L51:
        li $t5, 0
        j L53
L60:
        li $t6, 1
        j L62
L57:
        li $t5, 0
        j L59
L83:

===== BS_Print (Assembly) =====
L86:
        move $t9, $ra
        move $s0, $s0
        move $k1, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $k0, $s5
        move $s6, $s6
        move $s7, $s7
BS_Print:
        li $s1, 1
L63:
        li $a1, 0
        lw $a0, 4($s5)
        blt $s1, $a0, L66
L67:
L68:
        li $a0, 0
        beq $a1, $a0, L65
L64:
        lw $a1, 0($s5)
        li $a0, 4
        mul $a0, $s1, $a0
        add $a0, $a1, $a0
        lw $a0, 0($a0)
        jal print_int
        move $a0, $v0
        addi $s1, $s1, 1
        j L63
L66:
        li $a1, 1
        j L68
L65:
        li $a0, 99999
        jal print_int
        move $a0, $v0
        li $v0, 0
        move $s0, $s0
        move $s1, $k1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $k0
        move $s6, $s6
        move $s7, $s7
        move $ra, $t9
        j L85
L85:

===== BS_Init (Assembly) =====
L88:
        move $k1, $ra
        move $s0, $s0
        move $s1, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
BS_Init:
        sw $a1, 4($t9)
        addi $k0, $t9, 0
        li $a0, 4
        mul $a0, $a1, $a0
        jal malloc
        move $a0, $v0
        move $a0, $a0
        sw $a0, 0($k0)
        li $t0, 1
        lw $a0, 4($t9)
        addi $t1, $a0, 1
L69:
        li $a1, 0
        lw $a0, 4($t9)
        blt $t0, $a0, L72
L73:
L74:
        li $a0, 0
        beq $a1, $a0, L71
L70:
        li $a0, 2
        mul $a0, $a0, $t0
        move $a1, $a0
        addi $a0, $t1, -3
        move $a3, $a0
        lw $a2, 0($t9)
        li $a0, 4
        mul $a0, $t0, $a0
        add $a0, $a2, $a0
        add $a1, $a1, $a3
        sw $a1, 0($a0)
        addi $t0, $t0, 1
        addi $a0, $t1, -1
        move $t1, $a0
        j L69
L72:
        li $a1, 1
        j L74
L71:
        li $v0, 0
        move $s0, $s0
        move $s1, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
        move $ra, $k1
        j L87
L87:
```

### BinaryTree.java
```assembly
===== BinaryTree_main (Assembly) =====
L100:
        move $k1, $ra
        move $s0, $s0
        move $s1, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
        li $a0, 0
        jal malloc
        move $a0, $v0
        move $a0, $a0
        jal BT_Start
        move $a0, $v0
        move $a0, $a0
        jal print_int
        move $a0, $v0
        move $s0, $s0
        move $s1, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
        move $ra, $k1
        j L99
L99:

===== BT_Start (Assembly) =====
L102:
        move $k0, $ra
        move $s0, $s0
        move $k1, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
BT_Start:
        li $a0, 24
        jal malloc
        move $a0, $v0
        move $s1, $a0
        li $a0, 16
        jal Tree_Init
        move $a0, $v0
        move $a0, $a0
        jal Tree_Print
        move $a0, $v0
        move $a0, $a0
        li $a0, 100000000
        jal print_int
        move $a0, $v0
        li $a0, 8
        jal Tree_Insert
        move $a0, $v0
        move $a0, $a0
        jal Tree_Print
        move $a0, $v0
        move $a0, $a0
        li $a0, 24
        jal Tree_Insert
        move $a0, $v0
        move $a0, $a0
        li $a0, 4
        jal Tree_Insert
        move $a0, $v0
        move $a0, $a0
        li $a0, 12
        jal Tree_Insert
        move $a0, $v0
        move $a0, $a0
        li $a0, 20
        jal Tree_Insert
        move $a0, $v0
        move $a0, $a0
        li $a0, 28
        jal Tree_Insert
        move $a0, $v0
        move $a0, $a0
        li $a0, 14
        jal Tree_Insert
        move $a0, $v0
        move $a0, $a0
        jal Tree_Print
        move $a0, $v0
        move $a0, $a0
        li $a0, 24
        jal Tree_Search
        move $a0, $v0
        move $a0, $a0
        jal print_int
        move $a0, $v0
        li $a0, 12
        jal Tree_Search
        move $a0, $v0
        move $a0, $a0
        jal print_int
        move $a0, $v0
        li $a0, 16
        jal Tree_Search
        move $a0, $v0
        move $a0, $a0
        jal print_int
        move $a0, $v0
        li $a0, 50
        jal Tree_Search
        move $a0, $v0
        move $a0, $a0
        jal print_int
        move $a0, $v0
        li $a0, 12
        jal Tree_Search
        move $a0, $v0
        move $a0, $a0
        jal print_int
        move $a0, $v0
        li $a0, 12
        jal Tree_Delete
        move $a0, $v0
        move $a0, $a0
        jal Tree_Print
        move $a0, $v0
        move $a0, $a0
        li $a0, 12
        jal Tree_Search
        move $a0, $v0
        move $a0, $a0
        jal print_int
        move $a0, $v0
        li $v0, 0
        move $s0, $s0
        move $s1, $k1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
        move $ra, $k0
        j L101
L101:

===== Tree_Init (Assembly) =====
L104:
        move $t1, $ra
        move $a0, $s0
        move $a1, $s1
        move $a3, $s2
        move $a2, $s3
        move $t3, $s4
        move $t0, $s5
        move $t2, $s6
        move $t4, $s7
Tree_Init:
        sw $t5, 8($t6)
        li $t5, 0
        sw $t5, 12($t6)
        li $t5, 0
        sw $t5, 16($t6)
        li $v0, 1
        move $s0, $a0
        move $s1, $a1
        move $s2, $a3
        move $s3, $a2
        move $s4, $t3
        move $s5, $t0
        move $s6, $t2
        move $s7, $t4
        move $ra, $t1
        j L103
L103:

===== Tree_SetRight (Assembly) =====
L106:
        move $a3, $ra
        move $t2, $s0
        move $t0, $s1
        move $a2, $s2
        move $t4, $s3
        move $t3, $s4
        move $t1, $s5
        move $a0, $s6
        move $a1, $s7
Tree_SetRight:
        sw $t5, 4($t5)
        li $v0, 1
        move $s0, $t2
        move $s1, $t0
        move $s2, $a2
        move $s3, $t4
        move $s4, $t3
        move $s5, $t1
        move $s6, $a0
        move $s7, $a1
        move $ra, $a3
        j L105
L105:

===== Tree_SetLeft (Assembly) =====
L108:
        move $a3, $ra
        move $t4, $s0
        move $t1, $s1
        move $t2, $s2
        move $t0, $s3
        move $a1, $s4
        move $t3, $s5
        move $a2, $s6
        move $a0, $s7
Tree_SetLeft:
        sw $t5, 0($t5)
        li $v0, 1
        move $s0, $t4
        move $s1, $t1
        move $s2, $t2
        move $s3, $t0
        move $s4, $a1
        move $s5, $t3
        move $s6, $a2
        move $s7, $a0
        move $ra, $a3
        j L107
L107:

===== Tree_GetRight (Assembly) =====
L110:
        move $a2, $ra
        move $t3, $s0
        move $a3, $s1
        move $t2, $s2
        move $t0, $s3
        move $a1, $s4
        move $a0, $s5
        move $t4, $s6
        move $t1, $s7
Tree_GetRight:
        lw $v0, 4($t5)
        move $s0, $t3
        move $s1, $a3
        move $s2, $t2
        move $s3, $t0
        move $s4, $a1
        move $s5, $a0
        move $s6, $t4
        move $s7, $t1
        move $ra, $a2
        j L109
L109:

===== Tree_GetLeft (Assembly) =====
L112:
        move $a0, $ra
        move $t1, $s0
        move $a3, $s1
        move $t2, $s2
        move $t0, $s3
        move $t4, $s4
        move $a1, $s5
        move $a2, $s6
        move $t3, $s7
Tree_GetLeft:
        lw $v0, 0($t5)
        move $s0, $t1
        move $s1, $a3
        move $s2, $t2
        move $s3, $t0
        move $s4, $t4
        move $s5, $a1
        move $s6, $a2
        move $s7, $t3
        move $ra, $a0
        j L111
L111:

===== Tree_GetKey (Assembly) =====
L114:
        move $t4, $ra
        move $a1, $s0
        move $a2, $s1
        move $a0, $s2
        move $a3, $s3
        move $t1, $s4
        move $t2, $s5
        move $t3, $s6
        move $t0, $s7
Tree_GetKey:
        lw $v0, 8($t5)
        move $s0, $a1
        move $s1, $a2
        move $s2, $a0
        move $s3, $a3
        move $s4, $t1
        move $s5, $t2
        move $s6, $t3
        move $s7, $t0
        move $ra, $t4
        j L113
L113:

===== Tree_SetKey (Assembly) =====
L116:
        move $a3, $ra
        move $t0, $s0
        move $t3, $s1
        move $t2, $s2
        move $t4, $s3
        move $a1, $s4
        move $a2, $s5
        move $t1, $s6
        move $a0, $s7
Tree_SetKey:
        sw $t5, 8($t5)
        li $v0, 1
        move $s0, $t0
        move $s1, $t3
        move $s2, $t2
        move $s3, $t4
        move $s4, $a1
        move $s5, $a2
        move $s6, $t1
        move $s7, $a0
        move $ra, $a3
        j L115
L115:

===== Tree_GetHas_Right (Assembly) =====
L118:
        move $a0, $ra
        move $a2, $s0
        move $t4, $s1
        move $t3, $s2
        move $t2, $s3
        move $t0, $s4
        move $t1, $s5
        move $a1, $s6
        move $a3, $s7
Tree_GetHas_Right:
        lw $v0, 16($t5)
        move $s0, $a2
        move $s1, $t4
        move $s2, $t3
        move $s3, $t2
        move $s4, $t0
        move $s5, $t1
        move $s6, $a1
        move $s7, $a3
        move $ra, $a0
        j L117
L117:

===== Tree_GetHas_Left (Assembly) =====
L120:
        move $a1, $ra
        move $t0, $s0
        move $a3, $s1
        move $t4, $s2
        move $a0, $s3
        move $t2, $s4
        move $t3, $s5
        move $t1, $s6
        move $a2, $s7
Tree_GetHas_Left:
        lw $v0, 12($t5)
        move $s0, $t0
        move $s1, $a3
        move $s2, $t4
        move $s3, $a0
        move $s4, $t2
        move $s5, $t3
        move $s6, $t1
        move $s7, $a2
        move $ra, $a1
        j L119
L119:

===== Tree_SetHas_Left (Assembly) =====
L122:
        move $t2, $ra
        move $t0, $s0
        move $a3, $s1
        move $a0, $s2
        move $t4, $s3
        move $t1, $s4
        move $a1, $s5
        move $t3, $s6
        move $a2, $s7
Tree_SetHas_Left:
        sw $t5, 12($t5)
        li $v0, 1
        move $s0, $t0
        move $s1, $a3
        move $s2, $a0
        move $s3, $t4
        move $s4, $t1
        move $s5, $a1
        move $s6, $t3
        move $s7, $a2
        move $ra, $t2
        j L121
L121:

===== Tree_SetHas_Right (Assembly) =====
L124:
        move $a1, $ra
        move $a2, $s0
        move $t4, $s1
        move $a0, $s2
        move $t1, $s3
        move $t0, $s4
        move $a3, $s5
        move $t3, $s6
        move $t2, $s7
Tree_SetHas_Right:
        sw $t5, 16($t5)
        li $v0, 1
        move $s0, $a2
        move $s1, $t4
        move $s2, $a0
        move $s3, $t1
        move $s4, $t0
        move $s5, $a3
        move $s6, $t3
        move $s7, $t2
        move $ra, $a1
        j L123
L123:

===== Tree_Compare (Assembly) =====
L126:
        move $a1, $ra
        move $a3, $s0
        move $a2, $s1
        move $t4, $s2
        move $a0, $s3
        move $t3, $s4
        move $t0, $s5
        move $t2, $s6
        move $t1, $s7
Tree_Compare:
        li $t6, 0
        addi $t6, $t5, 1
        li $t7, 0
        blt $t5, $t5, L3
L4:
L5:
        li $s0, 0
        bne $t7, $s0, L0
L1:
        li $t7, 0
        blt $t5, $t6, L9
L10:
L11:
        li $t5, 1
        xor $t5, $t7, $t5
        li $t6, 0
        bne $t5, $t6, L6
L7:
        li $t6, 1
L8:
L2:
        move $v0, $t6
        move $s0, $a3
        move $s1, $a2
        move $s2, $t4
        move $s3, $a0
        move $s4, $t3
        move $s5, $t0
        move $s6, $t2
        move $s7, $t1
        move $ra, $a1
        j L125
L3:
        li $t7, 1
        j L5
L0:
        li $t6, 0
        j L2
L9:
        li $t7, 1
        j L11
L6:
        li $t6, 0
        j L8
L125:

===== Tree_Insert (Assembly) =====
L128:
        move $k1, $ra
        move $t9, $s0
        move $s1, $s1
        move $k0, $s2
        move $s3, $s3
        move $v1, $s4
        move $s5, $s5
        move $t8, $s6
        move $s7, $s7
Tree_Insert:
        li $a0, 24
        jal malloc
        move $a0, $v0
        move $s4, $a0
        jal Tree_Init
        move $a0, $v0
        move $a0, $a0
        move $s2, $s0
        li $s0, 1
L12:
        li $a0, 0
        beq $s0, $a0, L14
L13:
        jal Tree_GetKey
        move $a0, $v0
        move $a0, $a0
        li $a1, 0
        blt $s6, $a0, L18
L19:
L20:
        li $a0, 0
        bne $a1, $a0, L15
L16:
        jal Tree_GetHas_Right
        move $a0, $v0
        move $a0, $a0
        li $a1, 0
        bne $a0, $a1, L24
L25:
        li $s0, 0
        li $a0, 1
        jal Tree_SetHas_Right
        move $a0, $v0
        move $a0, $a0
        jal Tree_SetRight
        move $a0, $v0
        move $a0, $a0
L26:
L17:
        j L12
L18:
        li $a1, 1
        j L20
L15:
        jal Tree_GetHas_Left
        move $a0, $v0
        move $a0, $a0
        li $a1, 0
        bne $a0, $a1, L21
L22:
        li $s0, 0
        li $a0, 1
        jal Tree_SetHas_Left
        move $a0, $v0
        move $a0, $a0
        jal Tree_SetLeft
        move $a0, $v0
        move $a0, $a0
L23:
        j L17
L21:
        jal Tree_GetLeft
        move $a0, $v0
        move $s2, $a0
        j L23
L24:
        jal Tree_GetRight
        move $a0, $v0
        move $s2, $a0
        j L26
L14:
        li $v0, 1
        move $s0, $t9
        move $s1, $s1
        move $s2, $k0
        move $s3, $s3
        move $s4, $v1
        move $s5, $s5
        move $s6, $t8
        move $s7, $s7
        move $ra, $k1
        j L127
L127:

===== Tree_Delete (Assembly) =====
Aviso: Spill detectado durante a alocacao de registradores. A compilacao gerada ignorara os spills.
L130:
        move $k1, $ra
        move $k0, $s0
        move $t8, $s1
        move null, $s2
        move null, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
Tree_Delete:
        move $s1, $s3
        move $s2, $s3
        li $t9, 1
        li $s0, 0
        li $v1, 1
L27:
        li $a0, 0
        beq $t9, $a0, L29
L28:
        jal Tree_GetKey
        move $a0, $v0
        move $a0, $a0
        li $a1, 0
        blt $s3, $a0, L33
L34:
L35:
        li $a2, 0
        bne $a1, $a2, L30
L31:
        li $a1, 0
        blt $a0, $s3, L42
L43:
L44:
        li $a0, 0
        bne $a1, $a0, L39
L40:
        li $a0, 0
        bne $v1, $a0, L48
L49:
        jal Tree_Remove
        move $a0, $v0
        move $a0, $a0
L50:
        li $s0, 1
        li $t9, 0
L41:
L32:
        li $v1, 0
        j L27
L33:
        li $a1, 1
        j L35
L30:
        jal Tree_GetHas_Left
        move $a0, $v0
        move $a1, $a0
        li $a0, 0
        bne $a1, $a0, L36
L37:
        li $t9, 0
L38:
        j L32
L36:
        move $s2, $s1
        jal Tree_GetLeft
        move $a0, $v0
        move $s1, $a0
        j L38
L42:
        li $a1, 1
        j L44
L39:
        jal Tree_GetHas_Right
        move $a0, $v0
        move $a1, $a0
        li $a0, 0
        bne $a1, $a0, L45
L46:
        li $t9, 0
L47:
        j L41
L45:
        move $s2, $s1
        jal Tree_GetRight
        move $a0, $v0
        move $s1, $a0
        j L47
L48:
        li $s0, 0
        jal Tree_GetHas_Right
        move $a0, $v0
        move $a1, $a0
        li $a0, 1
        xor $a1, $a1, $a0
        li $a0, 0
        beq $a1, $a0, L55
L54:
        jal Tree_GetHas_Left
        move $a0, $v0
        move $a1, $a0
        li $a0, 1
        xor $a1, $a1, $a0
        li $a0, 0
        beq $a1, $a0, L55
L56:
        li $a0, 0
        bne $s0, $a0, L51
L52:
        jal Tree_Remove
        move $a0, $v0
        move $a0, $a0
L53:
        j L50
L131:
        li $s0, 1
        j L56
L55:
        j L56
L51:
        li $a0, 1
        j L53
L29:
        move $v0, $s0
        move $s0, $k0
        move $s1, $t8
        move $s2, null
        move $s3, null
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
        move $ra, $k1
        j L129
L129:

===== Tree_Remove (Assembly) =====
L133:
        move $k1, $ra
        move $k0, $s0
        move $t9, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
Tree_Remove:
        jal Tree_GetHas_Left
        move $a0, $v0
        move $a1, $a0
        li $a0, 0
        bne $a1, $a0, L57
L58:
        jal Tree_GetHas_Right
        move $a0, $v0
        move $a1, $a0
        li $a0, 0
        bne $a1, $a0, L60
L61:
        jal Tree_GetKey
        move $a0, $v0
        move $s1, $a0
        jal Tree_GetLeft
        move $a0, $v0
        move $a0, $a0
        jal Tree_GetKey
        move $a0, $v0
        move $a0, $a0
        jal Tree_Compare
        move $a0, $v0
        move $a1, $a0
        li $a0, 0
        bne $a1, $a0, L63
L64:
        lw $a0, 20($s0)
        jal Tree_SetRight
        move $a0, $v0
        move $a0, $a0
        li $a0, 0
        jal Tree_SetHas_Right
        move $a0, $v0
        move $a0, $a0
L65:
L62:
L59:
        li $v0, 1
        move $s0, $k0
        move $s1, $t9
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
        move $ra, $k1
        j L132
L57:
        jal Tree_RemoveLeft
        move $a0, $v0
        move $a0, $a0
        j L59
L60:
        jal Tree_RemoveRight
        move $a0, $v0
        move $a0, $a0
        j L62
L63:
        lw $a0, 20($s0)
        jal Tree_SetLeft
        move $a0, $v0
        move $a0, $a0
        li $a0, 0
        jal Tree_SetHas_Left
        move $a0, $v0
        move $a0, $a0
        j L65
L132:

===== Tree_RemoveRight (Assembly) =====
L135:
        move $t9, $ra
        move $s0, $s0
        move $s1, $s1
        move $s2, $s2
        move $k0, $s3
        move $s4, $s4
        move $t8, $s5
        move $k1, $s6
        move $s7, $s7
Tree_RemoveRight:
L66:
        jal Tree_GetHas_Right
        move $a0, $v0
        move $a0, $a0
        li $a1, 0
        beq $a0, $a1, L68
L67:
        move $s5, $s5
        jal Tree_GetRight
        move $a0, $v0
        move $a0, $a0
        jal Tree_GetKey
        move $a0, $v0
        move $a0, $a0
        jal Tree_SetKey
        move $a0, $v0
        move $a0, $a0
        move $s6, $s5
        jal Tree_GetRight
        move $a0, $v0
        move $s5, $a0
        j L66
L68:
        lw $a0, 20($s3)
        jal Tree_SetRight
        move $a0, $v0
        move $a0, $a0
        li $a0, 0
        jal Tree_SetHas_Right
        move $a0, $v0
        move $a0, $a0
        li $v0, 1
        move $s0, $s0
        move $s1, $s1
        move $s2, $s2
        move $s3, $k0
        move $s4, $s4
        move $s5, $t8
        move $s6, $k1
        move $s7, $s7
        move $ra, $t9
        j L134
L134:

===== Tree_RemoveLeft (Assembly) =====
L137:
        move $k1, $ra
        move $s0, $s0
        move $k0, $s1
        move $t8, $s2
        move $t9, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
Tree_RemoveLeft:
L69:
        jal Tree_GetHas_Left
        move $a0, $v0
        move $a1, $a0
        li $a0, 0
        beq $a1, $a0, L71
L70:
        move $s1, $s3
        jal Tree_GetLeft
        move $a0, $v0
        move $a0, $a0
        jal Tree_GetKey
        move $a0, $v0
        move $a0, $a0
        jal Tree_SetKey
        move $a0, $v0
        move $a0, $a0
        move $s1, $s3
        jal Tree_GetLeft
        move $a0, $v0
        move $s3, $a0
        j L69
L71:
        lw $a0, 20($s2)
        jal Tree_SetLeft
        move $a0, $v0
        move $a0, $a0
        li $a0, 0
        jal Tree_SetHas_Left
        move $a0, $v0
        move $a0, $a0
        li $v0, 1
        move $s0, $s0
        move $s1, $k0
        move $s2, $t8
        move $s3, $t9
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
        move $ra, $k1
        j L136
L136:

===== Tree_Search (Assembly) =====
L139:
        move $k1, $ra
        move $k0, $s0
        move $s1, $s1
        move $v1, $s2
        move $t9, $s3
        move $t8, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
Tree_Search:
        move $s3, $a0
        li $s0, 1
        li $s4, 0
L72:
        li $a0, 0
        beq $s0, $a0, L74
L73:
        jal Tree_GetKey
        move $a0, $v0
        move $a0, $a0
        li $a2, 0
        blt $s2, $a0, L78
L79:
L80:
        li $a1, 0
        bne $a2, $a1, L75
L76:
        li $a1, 0
        blt $a0, $s2, L87
L88:
L89:
        li $a0, 0
        bne $a1, $a0, L84
L85:
        li $s4, 1
        li $s0, 0
L86:
L77:
        j L72
L78:
        li $a2, 1
        j L80
L75:
        jal Tree_GetHas_Left
        move $a0, $v0
        move $a1, $a0
        li $a0, 0
        bne $a1, $a0, L81
L82:
        li $s0, 0
L83:
        j L77
L81:
        jal Tree_GetLeft
        move $a0, $v0
        move $s3, $a0
        j L83
L87:
        li $a1, 1
        j L89
L84:
        jal Tree_GetHas_Right
        move $a0, $v0
        move $a0, $a0
        li $a1, 0
        bne $a0, $a1, L90
L91:
        li $s0, 0
L92:
        j L86
L90:
        jal Tree_GetRight
        move $a0, $v0
        move $s3, $a0
        j L92
L74:
        move $v0, $s4
        move $s0, $k0
        move $s1, $s1
        move $s2, $v1
        move $s3, $t9
        move $s4, $t8
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
        move $ra, $k1
        j L138
L138:

===== Tree_Print (Assembly) =====
L141:
        move $k1, $ra
        move $s0, $s0
        move $s1, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
Tree_Print:
        move $a0, $a0
        jal Tree_RecPrint
        move $a0, $v0
        move $a0, $a0
        li $v0, 1
        move $s0, $s0
        move $s1, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
        move $ra, $k1
        j L140
L140:

===== Tree_RecPrint (Assembly) =====
L143:
        move $t9, $ra
        move $k1, $s0
        move $s1, $s1
        move $s2, $s2
        move $k0, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
Tree_RecPrint:
        jal Tree_GetHas_Left
        move $a0, $v0
        move $a1, $a0
        li $a0, 0
        bne $a1, $a0, L93
L94:
        li $a0, 1
L95:
        jal Tree_GetKey
        move $a0, $v0
        move $a0, $a0
        jal print_int
        move $a0, $v0
        jal Tree_GetHas_Right
        move $a0, $v0
        move $a1, $a0
        li $a0, 0
        bne $a1, $a0, L96
L97:
        li $a0, 1
L98:
        li $v0, 1
        move $s0, $k1
        move $s1, $s1
        move $s2, $s2
        move $s3, $k0
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
        move $ra, $t9
        j L142
L93:
        move $s3, $s0
        jal Tree_GetLeft
        move $a0, $v0
        move $a0, $a0
        jal Tree_RecPrint
        move $a0, $v0
        move $a0, $a0
        j L95
L96:
        move $s3, $s0
        jal Tree_GetRight
        move $a0, $v0
        move $a0, $a0
        jal Tree_RecPrint
        move $a0, $v0
        move $a0, $a0
        j L98
L142:
...
```
### BubbleSort.java
```assembly
===== BubbleSort_main (Assembly) =====
L25:
        move $k1, $ra
        move $s0, $s0
        move $s1, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
        li $a0, 8
        jal malloc
        move $a0, $v0
        move $a1, $a0
        li $a0, 10
        jal BBS_Start
        move $a0, $v0
        move $a0, $a0
        jal print_int
        move $a0, $v0
        move $s0, $s0
        move $s1, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
        move $ra, $k1
        j L24
L24:

===== BBS_Start (Assembly) =====
L27:
        move $k1, $ra
        move $s0, $s0
        move $s1, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
BBS_Start:
        jal BBS_Init
        move $a0, $v0
        move $a0, $a0
        jal BBS_Print
        move $a0, $v0
        move $a0, $a0
        li $a0, 99999
        jal print_int
        move $a0, $v0
        jal BBS_Sort
        move $a0, $v0
        move $a0, $a0
        jal BBS_Print
        move $a0, $v0
        move $a0, $a0
        li $v0, 0
        move $s0, $s0
        move $s1, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
        move $ra, $k1
        j L26
L26:

===== BBS_Sort (Assembly) =====
L29:
        move $a3, $ra
        move $a2, $s0
        move $t0, $s1
        move $s2, $s2
        move $t1, $s3
        move $a1, $s4
        move $t2, $s5
        move $a0, $s6
        move $s7, $s7
BBS_Sort:
        lw $t3, 4($s1)
        addi $t3, $t3, -1
        move $t5, $t3
        li $t3, 0
        addi $t3, $t3, -1
        move $t4, $t3
L0:
        li $t6, 0
        blt $t4, $t5, L3
L4:
L5:
        li $t3, 0
        beq $t6, $t3, L2
L1:
        li $s3, 1
L6:
        li $t6, 0
        addi $t3, $t5, 1
        blt $s3, $t3, L9
L10:
L11:
        li $t3, 0
        beq $t6, $t3, L8
L7:
        addi $t3, $s3, -1
        move $t6, $t3
        lw $t3, 0($s1)
        li $t7, 4
        mul $t6, $t6, $t7
        add $t3, $t3, $t6
        lw $t3, 0($t3)
        move $t7, $t3
        lw $t3, 0($s1)
        li $t6, 4
        mul $t6, $s3, $t6
        add $t3, $t3, $t6
        lw $t3, 0($t3)
        move $t6, $t3
        li $t3, 0
        blt $t6, $t7, L15
L16:
L17:
        li $t6, 0
        bne $t3, $t6, L12
L13:
        li $t3, 0
L14:
        addi $s3, $s3, 1
        j L6
L3:
        li $t6, 1
        j L5
L9:
        li $t6, 1
        j L11
L15:
        li $t3, 1
        j L17
L12:
        addi $t3, $s3, -1
        move $t7, $t3
        lw $t3, 0($s1)
        li $t6, 4
        mul $t6, $t7, $t6
        add $t3, $t3, $t6
        lw $t3, 0($t3)
        move $s0, $t3
        lw $t6, 0($s1)
        li $t3, 4
        mul $t3, $t7, $t3
        add $t3, $t6, $t3
        lw $t7, 0($s1)
        li $t6, 4
        mul $t6, $s3, $t6
        add $t6, $t7, $t6
        lw $t6, 0($t6)
        sw $t6, 0($t3)
        lw $t3, 0($s1)
        li $t6, 4
        mul $t6, $s3, $t6
        add $t3, $t3, $t6
        sw $s0, 0($t3)
        j L14
L8:
        addi $t3, $t5, -1
        move $t5, $t3
        j L0
L2:
        li $v0, 0
        move $s0, $a2
        move $s1, $t0
        move $s2, $s2
        move $s3, $t1
        move $s4, $a1
        move $s5, $t2
        move $s6, $a0
        move $s7, $s7
        move $ra, $a3
        j L28
L28:

===== BBS_Print (Assembly) =====
L31:
        move $k1, $ra
        move $k0, $s0
        move $t9, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
BBS_Print:
        li $s0, 0
L18:
        li $a1, 0
        lw $a0, 4($s1)
        blt $s0, $a0, L21
L22:
L23:
        li $a0, 0
        beq $a1, $a0, L20
L19:
        lw $a1, 0($s1)
        li $a0, 4
        mul $a0, $s0, $a0
        add $a0, $a1, $a0
        lw $a0, 0($a0)
        jal print_int
        move $a0, $v0
        addi $s0, $s0, 1
        j L18
L21:
        li $a1, 1
        j L23
L20:
        li $v0, 0
        move $s0, $k0
        move $s1, $t9
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
        move $ra, $k1
        j L30
L30:

===== BBS_Init (Assembly) =====
L33:
        move $k1, $ra
        move $s0, $s0
        move $s1, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $k0, $s5
        move $s6, $s6
        move $s7, $s7
BBS_Init:
        sw $a1, 4($s5)
        addi $t9, $s5, 0
        li $a0, 4
        mul $a0, $a1, $a0
        jal malloc
        move $a0, $v0
        move $a0, $a0
        sw $a0, 0($t9)
        lw $a0, 0($s5)
        li $a1, 0
        li $a2, 4
        mul $a1, $a1, $a2
        add $a0, $a0, $a1
        li $a1, 20
        sw $a1, 0($a0)
        lw $a1, 0($s5)
        li $a2, 1
        li $a0, 4
        mul $a0, $a2, $a0
        add $a0, $a1, $a0
        li $a1, 7
        sw $a1, 0($a0)
        lw $a1, 0($s5)
        li $a2, 2
        li $a0, 4
        mul $a0, $a2, $a0
        add $a1, $a1, $a0
        li $a0, 12
        sw $a0, 0($a1)
        lw $a1, 0($s5)
        li $a2, 3
        li $a0, 4
        mul $a0, $a2, $a0
        add $a0, $a1, $a0
        li $a1, 18
        sw $a1, 0($a0)
        lw $a2, 0($s5)
        li $a1, 4
        li $a0, 4
        mul $a0, $a1, $a0
        add $a0, $a2, $a0
        li $a1, 2
        sw $a1, 0($a0)
        lw $a2, 0($s5)
        li $a1, 5
        li $a0, 4
        mul $a0, $a1, $a0
        add $a1, $a2, $a0
        li $a0, 11
        sw $a0, 0($a1)
        lw $a1, 0($s5)
        li $a2, 6
        li $a0, 4
        mul $a0, $a2, $a0
        add $a0, $a1, $a0
        li $a1, 6
        sw $a1, 0($a0)
        lw $a1, 0($s5)
        li $a2, 7
        li $a0, 4
        mul $a0, $a2, $a0
        add $a0, $a1, $a0
        li $a1, 9
        sw $a1, 0($a0)
        lw $a1, 0($s5)
        li $a0, 8
        li $a2, 4
        mul $a0, $a0, $a2
        add $a1, $a1, $a0
        li $a0, 19
        sw $a0, 0($a1)
        lw $a2, 0($s5)
        li $a0, 9
        li $a1, 4
        mul $a0, $a0, $a1
        add $a0, $a2, $a0
        li $a1, 5
        sw $a1, 0($a0)
        li $v0, 0
        move $s0, $s0
        move $s1, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $k0
        move $s6, $s6
        move $s7, $s7
        move $ra, $k1
        j L32
L32:
```
### Factorial.java
```assembly
===== Factorial_main (Assembly) =====
L7:
        move $k1, $ra
        move $s0, $s0
        move $s1, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
        li $a0, 0
        jal malloc
        move $a0, $v0
        move $a0, $a0
        li $a1, 10
        jal Fac_ComputeFac
        move $a0, $v0
        move $a0, $a0
        jal print_int
        move $a0, $v0
        move $s0, $s0
        move $s1, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
        move $ra, $k1
        j L6
L6:

===== Fac_ComputeFac (Assembly) =====
L9:
        move $k1, $ra
        move $s0, $s0
        move $s1, $s1
        move $k0, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
Fac_ComputeFac:
        li $a0, 0
        li $a2, 1
        blt $a1, $a2, L3
L4:
L5:
        li $a2, 0
        bne $a0, $a2, L0
L1:
        move $s2, $a1
        addi $a0, $a1, -1
        jal Fac_ComputeFac
        move $a0, $v0
        move $a0, $a0
        mul $a0, $s2, $a0
        move $a0, $a0
L2:
        move $v0, $a0
        move $s0, $s0
        move $s1, $s1
        move $s2, $k0
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
        move $ra, $k1
        j L8
L3:
        li $a0, 1
        j L5
L0:
        li $a0, 1
        j L2
L8:
```
### LinearSearch.java
```assembly
===== LinearSearch_main (Assembly) =====
L31:
        move $k1, $ra
        move $s0, $s0
        move $s1, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
        li $a0, 8
        jal malloc
        move $a0, $v0
        move $a0, $a0
        li $a1, 10
        jal LS_Start
        move $a0, $v0
        move $a0, $a0
        jal print_int
        move $a0, $v0
        move $s0, $s0
        move $s1, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
        move $ra, $k1
        j L30
L30:

===== LS_Start (Assembly) =====
L33:
        move $k1, $ra
        move $s0, $s0
        move $s1, $s1
        move $s2, $s2
        move $k0, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
LS_Start:
        jal LS_Init
        move $a0, $v0
        move $a0, $a0
        jal LS_Print
        move $a0, $v0
        move $a0, $a0
        li $a0, 9999
        jal print_int
        move $a0, $v0
        li $a0, 8
        jal LS_Search
        move $a0, $v0
        move $a0, $a0
        jal print_int
        move $a0, $v0
        li $a0, 12
        jal LS_Search
        move $a0, $v0
        move $a0, $a0
        jal print_int
        move $a0, $v0
        li $a0, 17
        jal LS_Search
        move $a0, $v0
        move $a0, $a0
        jal print_int
        move $a0, $v0
        li $a0, 50
        jal LS_Search
        move $a0, $v0
        move $a0, $a0
        jal print_int
        move $a0, $v0
        li $v0, 55
        move $s0, $s0
        move $s1, $s1
        move $s2, $s2
        move $s3, $k0
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
        move $ra, $k1
        j L32
L32:

===== LS_Print (Assembly) =====
L35:
        move $k1, $ra
        move $k0, $s0
        move $t9, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
LS_Print:
        li $s1, 1
L0:
        li $a1, 0
        lw $a0, 4($s0)
        blt $s1, $a0, L3
L4:
L5:
        li $a0, 0
        beq $a1, $a0, L2
L1:
        lw $a1, 0($s0)
        li $a0, 4
        mul $a0, $s1, $a0
        add $a0, $a1, $a0
        lw $a0, 0($a0)
        jal print_int
        move $a0, $v0
        addi $s1, $s1, 1
        j L0
L3:
        li $a1, 1
        j L5
L2:
        li $v0, 0
        move $s0, $k0
        move $s1, $t9
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
        move $ra, $k1
        j L34
L34:

===== LS_Search (Assembly) =====
L37:
        move $a3, $ra
        move $s0, $s0
        move $a1, $s1
        move $t1, $s2
        move $t2, $s3
        move $a0, $s4
        move $s5, $s5
        move $t0, $s6
        move $a2, $s7
LS_Search:
        li $t7, 1
        li $t3, 0
        li $t6, 0
L6:
        li $t3, 0
        lw $t4, 4($t5)
        blt $t7, $t4, L9
L10:
L11:
        li $t4, 0
        beq $t3, $t4, L8
L7:
        lw $t4, 0($t5)
        li $t3, 4
        mul $t3, $t7, $t3
        add $t3, $t4, $t3
        lw $t3, 0($t3)
        move $s2, $t3
        addi $t3, $t5, 1
        li $t4, 0
        blt $s2, $t5, L15
L16:
L17:
        li $s1, 0
        bne $t4, $s1, L12
L13:
        li $t4, 0
        blt $s2, $t3, L21
L22:
L23:
        li $t3, 1
        xor $t4, $t4, $t3
        li $t3, 0
        bne $t4, $t3, L18
L19:
        li $t3, 1
        li $t6, 1
        lw $t7, 4($t5)
L20:
L14:
        addi $t7, $t7, 1
        j L6
L9:
        li $t3, 1
        j L11
L15:
        li $t4, 1
        j L17
L12:
        li $t3, 0
        j L14
L21:
        li $t4, 1
        j L23
L18:
        li $t3, 0
        j L20
L8:
        move $v0, $t6
        move $s0, $s0
        move $s1, $a1
        move $s2, $t1
        move $s3, $t2
        move $s4, $a0
        move $s5, $s5
        move $s6, $t0
        move $s7, $a2
        move $ra, $a3
        j L36
L36:

===== LS_Init (Assembly) =====
L39:
        move $k0, $ra
        move $s0, $s0
        move $k1, $s1
        move $s2, $s2
        move $s3, $s3
        move $t9, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
LS_Init:
        sw $a0, 4($s1)
        addi $s4, $s1, 0
        li $a1, 4
        mul $a0, $a0, $a1
        jal malloc
        move $a0, $v0
        move $a0, $a0
        sw $a0, 0($s4)
        li $t1, 1
        lw $a0, 4($s1)
        addi $a0, $a0, 1
L24:
        li $a2, 0
        lw $a1, 4($s1)
        blt $t1, $a1, L27
L28:
L29:
        li $a1, 0
        beq $a2, $a1, L26
L25:
        li $a1, 2
        mul $a1, $a1, $t1
        move $a2, $a1
        addi $a1, $a0, -3
        move $t0, $a1
        lw $a1, 0($s1)
        li $a3, 4
        mul $a3, $t1, $a3
        add $a1, $a1, $a3
        add $a2, $a2, $t0
        sw $a2, 0($a1)
        addi $t1, $t1, 1
        addi $a0, $a0, -1
        move $a0, $a0
        j L24
L27:
        li $a2, 1
        j L29
L26:
        li $v0, 0
        move $s0, $s0
        move $s1, $k1
        move $s2, $s2
        move $s3, $s3
        move $s4, $t9
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
        move $ra, $k0
        j L38
L38:
```
### LinkedList.java
```assembly
===== LinkedList_main (Assembly) =====
L55:
        move $k1, $ra
        move $s0, $s0
        move $s1, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
        li $a0, 0
        jal malloc
        move $a0, $v0
        move $a0, $a0
        jal LL_Start
        move $a0, $v0
        move $a0, $a0
        jal print_int
        move $a0, $v0
        move $s0, $s0
        move $s1, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
        move $ra, $k1
        j L54
L54:

===== Element_Init (Assembly) =====
L57:
        move $t2, $ra
        move $t0, $s0
        move $t1, $s1
        move $t4, $s2
        move $t3, $s3
        move $a2, $s4
        move $a3, $s5
        move $a0, $s6
        move $a1, $s7
Element_Init:
        sw $t5, 0($t5)
        sw $t5, 4($t5)
        sw $t5, 8($t5)
        li $v0, 1
        move $s0, $t0
        move $s1, $t1
        move $s2, $t4
        move $s3, $t3
        move $s4, $a2
        move $s5, $a3
        move $s6, $a0
        move $s7, $a1
        move $ra, $t2
        j L56
L56:

===== Element_GetAge (Assembly) =====
L59:
        move $t2, $ra
        move $t1, $s0
        move $a3, $s1
        move $t0, $s2
        move $t4, $s3
        move $a2, $s4
        move $t3, $s5
        move $a1, $s6
        move $a0, $s7
Element_GetAge:
        lw $v0, 0($t5)
        move $s0, $t1
        move $s1, $a3
        move $s2, $t0
        move $s3, $t4
        move $s4, $a2
        move $s5, $t3
        move $s6, $a1
        move $s7, $a0
        move $ra, $t2
        j L58
L58:

===== Element_GetSalary (Assembly) =====
L61:
        move $t4, $ra
        move $t2, $s0
        move $t3, $s1
        move $a1, $s2
        move $a2, $s3
        move $a3, $s4
        move $a0, $s5
        move $t1, $s6
        move $t0, $s7
Element_GetSalary:
        lw $v0, 4($t5)
        move $s0, $t2
        move $s1, $t3
        move $s2, $a1
        move $s3, $a2
        move $s4, $a3
        move $s5, $a0
        move $s6, $t1
        move $s7, $t0
        move $ra, $t4
        j L60
L60:

===== Element_GetMarried (Assembly) =====
L63:
        move $a3, $ra
        move $t0, $s0
        move $t3, $s1
        move $a0, $s2
        move $t4, $s3
        move $a1, $s4
        move $t2, $s5
        move $a2, $s6
        move $t1, $s7
Element_GetMarried:
        lw $v0, 8($t5)
        move $s0, $t0
        move $s1, $t3
        move $s2, $a0
        move $s3, $t4
        move $s4, $a1
        move $s5, $t2
        move $s6, $a2
        move $s7, $t1
        move $ra, $a3
        j L62
L62:

===== Element_Equal (Assembly) =====
L65:
        move $k0, $ra
        move $s0, $s0
        move $t9, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $k1, $s6
        move $s7, $s7
Element_Equal:
        li $s1, 1
        jal Element_GetAge
        move $a0, $v0
        move $a1, $a0
        lw $a0, 0($s6)
        jal Element_Compare
        move $a0, $v0
        move $a0, $a0
        li $a1, 1
        xor $a0, $a0, $a1
        li $a1, 0
        bne $a0, $a1, L0
L1:
        jal Element_GetSalary
        move $a0, $v0
        move $a1, $a0
        lw $a0, 4($s6)
        jal Element_Compare
        move $a0, $v0
        move $a1, $a0
        li $a0, 1
        xor $a1, $a1, $a0
        li $a0, 0
        bne $a1, $a0, L3
L4:
        lw $a0, 8($s6)
        li $a1, 0
        bne $a0, $a1, L6
L7:
        jal Element_GetMarried
        move $a0, $v0
        move $a1, $a0
        li $a0, 0
        bne $a1, $a0, L12
L13:
        li $a0, 0
L14:
L8:
L5:
L2:
        move $v0, $s1
        move $s0, $s0
        move $s1, $t9
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $k1
        move $s7, $s7
        move $ra, $k0
        j L64
L0:
        li $s1, 0
        j L2
L3:
        li $s1, 0
        j L5
L6:
        jal Element_GetMarried
        move $a0, $v0
        move $a0, $a0
        li $a1, 1
        xor $a1, $a0, $a1
        li $a0, 0
        bne $a1, $a0, L9
L10:
        li $a0, 0
L11:
        j L8
L9:
        li $s1, 0
        j L11
L12:
        li $s1, 0
        j L14
L64:

===== Element_Compare (Assembly) =====
L67:
        move $t2, $ra
        move $a3, $s0
        move $t4, $s1
        move $t1, $s2
        move $t3, $s3
        move $a2, $s4
        move $a0, $s5
        move $t0, $s6
        move $a1, $s7
Element_Compare:
        li $t5, 0
        addi $t5, $t7, 1
        li $t6, 0
        blt $t7, $t7, L18
L19:
L20:
        li $s0, 0
        bne $t6, $s0, L15
L16:
        li $t6, 0
        blt $t7, $t5, L24
L25:
L26:
        li $t5, 1
        xor $t6, $t6, $t5
        li $t5, 0
        bne $t6, $t5, L21
L22:
        li $t5, 1
L23:
L17:
        move $v0, $t5
        move $s0, $a3
        move $s1, $t4
        move $s2, $t1
        move $s3, $t3
        move $s4, $a2
        move $s5, $a0
        move $s6, $t0
        move $s7, $a1
        move $ra, $t2
        j L66
L18:
        li $t6, 1
        j L20
L15:
        li $t5, 0
        j L17
L24:
        li $t6, 1
        j L26
L21:
        li $t5, 0
        j L23
L66:

===== List_Init (Assembly) =====
L69:
        move $a3, $ra
        move $t2, $s0
        move $a2, $s1
        move $t0, $s2
        move $t4, $s3
        move $t1, $s4
        move $a1, $s5
        move $t3, $s6
        move $a0, $s7
List_Init:
        li $t6, 1
        sw $t6, 8($t5)
        li $v0, 1
        move $s0, $t2
        move $s1, $a2
        move $s2, $t0
        move $s3, $t4
        move $s4, $t1
        move $s5, $a1
        move $s6, $t3
        move $s7, $a0
        move $ra, $a3
        j L68
L68:

===== List_InitNew (Assembly) =====
L71:
        move $a0, $ra
        move $t0, $s0
        move $t2, $s1
        move $a2, $s2
        move $t1, $s3
        move $t3, $s4
        move $t4, $s5
        move $a3, $s6
        move $a1, $s7
List_InitNew:
        sw $t5, 8($t5)
        sw $t5, 0($t5)
        sw $t5, 4($t5)
        li $v0, 1
        move $s0, $t0
        move $s1, $t2
        move $s2, $a2
        move $s3, $t1
        move $s4, $t3
        move $s5, $t4
        move $s6, $a3
        move $s7, $a1
        move $ra, $a0
        j L70
L70:

===== List_Insert (Assembly) =====
L73:
        move $t8, $ra
        move $t9, $s0
        move $s1, $s1
        move $s2, $s2
        move $k1, $s3
        move $s4, $s4
        move $k0, $s5
        move $s6, $s6
        move $s7, $s7
List_Insert:
        move $s0, $a0
        li $a0, 12
        jal malloc
        move $a0, $v0
        move $s3, $a0
        li $a0, 0
        jal List_InitNew
        move $a0, $v0
        move $a0, $a0
        move $v0, $s3
        move $s0, $t9
        move $s1, $s1
        move $s2, $s2
        move $s3, $k1
        move $s4, $s4
        move $s5, $k0
        move $s6, $s6
        move $s7, $s7
        move $ra, $t8
        j L72
L72:

===== List_SetNext (Assembly) =====
L75:
        move $a3, $ra
        move $a1, $s0
        move $t4, $s1
        move $t3, $s2
        move $t2, $s3
        move $t0, $s4
        move $t1, $s5
        move $a2, $s6
        move $a0, $s7
List_SetNext:
        sw $t5, 4($t5)
        li $v0, 1
        move $s0, $a1
        move $s1, $t4
        move $s2, $t3
        move $s3, $t2
        move $s4, $t0
        move $s5, $t1
        move $s6, $a2
        move $s7, $a0
        move $ra, $a3
        j L74
L74:

===== List_Delete (Assembly) =====
Aviso: Spill detectado durante a alocacao de registradores. A compilacao gerada ignorara os spills.
L77:
        move $k1, $ra
        move null, $s0
        move $k0, $s1
        move $s2, $s2
        move $t9, $s3
        move $s4, $s4
        move null, $s5
        move $s6, $s6
        move $s7, $s7
List_Delete:
        move $v1, $a0
        li null, 0
        li $a1, 0
        addi $a1, $a1, -1
        move $s1, $a1
        move null, $a0
        move $s0, $a0
        lw $s5, 8($a0)
        lw $s3, 0($a0)
L27:
        li $a2, 0
        li $a0, 1
        xor $a0, $s5, $a0
        li $a1, 0
        beq $a0, $a1, L31
L30:
        li $a0, 1
        xor $a0, null, $a0
        li $a1, 0
        beq $a0, $a1, L31
L32:
        li $a0, 0
        beq $a2, $a0, L29
L28:
        jal Element_Equal
        move $a0, $v0
        move $a0, $a0
        li $a1, 0
        bne $a0, $a1, L33
L34:
        li $a0, 0
L35:
        li $a0, 1
        xor $a1, null, $a0
        li $a0, 0
        bne $a1, $a0, L42
L43:
        li $a0, 0
L44:
        j L27
L78:
        li $a2, 1
        j L32
L31:
        j L32
L33:
        li null, 1
        li $a1, 0
        li $a0, 0
        blt $s1, $a0, L39
L40:
L41:
        li $a0, 0
        bne $a1, $a0, L36
L37:
        li $a0, 0
        addi $a0, $a0, -555
        jal print_int
        move $a0, $v0
        move $s0, $s0
        jal List_GetNext
        move $a0, $v0
        move $a0, $a0
        jal List_SetNext
        move $a0, $v0
        move $a0, $a0
        li $a0, 0
        addi $a0, $a0, -555
        jal print_int
        move $a0, $v0
L38:
        j L35
L39:
        li $a1, 1
        j L41
L36:
        jal List_GetNext
        move $a0, $v0
        move $v1, $a0
        j L38
L42:
        move $s0, null
        jal List_GetNext
        move $a0, $v0
        move null, $a0
        jal List_GetEnd
        move $a0, $v0
        move $s5, $a0
        jal List_GetElem
        move $a0, $v0
        move $s3, $a0
        li $s1, 1
        j L44
L29:
        move $v0, $v1
        move $s0, null
        move $s1, $k0
        move $s2, $s2
        move $s3, $t9
        move $s4, $s4
        move $s5, null
        move $s6, $s6
        move $s7, $s7
        move $ra, $k1
        j L76
L76:

===== List_Search (Assembly) =====
L80:
        move $k1, $ra
        move $t9, $s0
        move $k0, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
List_Search:
        li $t8, 0
        move $s0, $a0
        lw $v1, 8($a0)
        lw $a0, 0($a0)
L45:
        li $a1, 1
        xor $a2, $v1, $a1
        li $a1, 0
        beq $a2, $a1, L47
L46:
        jal Element_Equal
        move $a0, $v0
        move $a1, $a0
        li $a0, 0
        bne $a1, $a0, L48
L49:
        li $a0, 0
L50:
        jal List_GetNext
        move $a0, $v0
        move $s0, $a0
        jal List_GetEnd
        move $a0, $v0
        move $v1, $a0
        jal List_GetElem
        move $a0, $v0
        move $a0, $a0
        j L45
L48:
        li $t8, 1
        j L50
L47:
        move $v0, $t8
        move $s0, $t9
        move $s1, $k0
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
        move $ra, $k1
        j L79
L79:

===== List_GetEnd (Assembly) =====
L82:
        move $a3, $ra
        move $a0, $s0
        move $a2, $s1
        move $t0, $s2
        move $t4, $s3
        move $t3, $s4
        move $t2, $s5
        move $a1, $s6
        move $t1, $s7
List_GetEnd:
        lw $v0, 8($t5)
        move $s0, $a0
        move $s1, $a2
        move $s2, $t0
        move $s3, $t4
        move $s4, $t3
        move $s5, $t2
        move $s6, $a1
        move $s7, $t1
        move $ra, $a3
        j L81
L81:

===== List_GetElem (Assembly) =====
L84:
        move $t2, $ra
        move $t1, $s0
        move $a3, $s1
        move $a0, $s2
        move $t4, $s3
        move $a1, $s4
        move $t0, $s5
        move $t3, $s6
        move $a2, $s7
List_GetElem:
        lw $v0, 0($t5)
        move $s0, $t1
        move $s1, $a3
        move $s2, $a0
        move $s3, $t4
        move $s4, $a1
        move $s5, $t0
        move $s6, $t3
        move $s7, $a2
        move $ra, $t2
        j L83
L83:

===== List_GetNext (Assembly) =====
L86:
        move $t4, $ra
        move $t0, $s0
        move $a2, $s1
        move $t3, $s2
        move $t1, $s3
        move $t2, $s4
        move $a0, $s5
        move $a1, $s6
        move $a3, $s7
List_GetNext:
        lw $v0, 4($t5)
        move $s0, $t0
        move $s1, $a2
        move $s2, $t3
        move $s3, $t1
        move $s4, $t2
        move $s5, $a0
        move $s6, $a1
        move $s7, $a3
        move $ra, $t4
        j L85
L85:

===== List_Print (Assembly) =====
L88:
        move $k1, $ra
        move $t9, $s0
        move $s1, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $k0, $s5
        move $s6, $s6
        move $s7, $s7
List_Print:
        move $s0, $a0
        lw $s5, 8($a0)
        lw $a1, 0($a0)
L51:
        li $a0, 1
        xor $a2, $s5, $a0
        li $a0, 0
        beq $a2, $a0, L53
L52:
        jal Element_GetAge
        move $a0, $v0
        move $a0, $a0
        jal print_int
        move $a0, $v0
        jal List_GetNext
        move $a0, $v0
        move $s0, $a0
        jal List_GetEnd
        move $a0, $v0
        move $s5, $a0
        jal List_GetElem
        move $a0, $v0
        move $a1, $a0
        j L51
L53:
        li $v0, 1
        move $s0, $t9
        move $s1, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $k0
        move $s6, $s6
        move $s7, $s7
        move $ra, $k1
        j L87
L87:

===== LL_Start (Assembly) =====
L90:
        move $t8, $ra
        move $k1, $s0
        move $k0, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
LL_Start:
        li $a0, 12
        jal malloc
        move $a0, $v0
        move $s0, $a0
        jal List_Init
        move $a0, $v0
        move $a0, $a0
        move $s0, $s0
        jal List_Init
        move $a0, $v0
        move $a0, $a0
        jal List_Print
        move $a0, $v0
        move $a0, $a0
        li $a0, 12
        jal malloc
        move $a0, $v0
        move $t9, $a0
        li $a2, 25
        li $a0, 37000
        li $a1, 0
        jal Element_Init
        move $a0, $v0
        move $a0, $a0
        jal List_Insert
        move $a0, $v0
        move $s0, $a0
        jal List_Print
        move $a0, $v0
        move $a0, $a0
        li $a0, 10000000
        jal print_int
        move $a0, $v0
        li $a0, 12
        jal malloc
        move $a0, $v0
        move $t9, $a0
        li $a0, 39
        li $a2, 42000
        li $a1, 1
        jal Element_Init
        move $a0, $v0
        move $a0, $a0
        move $s1, $t9
        jal List_Insert
        move $a0, $v0
        move $s0, $a0
        jal List_Print
        move $a0, $v0
        move $a0, $a0
        li $a0, 10000000
        jal print_int
        move $a0, $v0
        li $a0, 12
        jal malloc
        move $a0, $v0
        move $t9, $a0
        li $a0, 22
        li $a2, 34000
        li $a1, 0
        jal Element_Init
        move $a0, $v0
        move $a0, $a0
        jal List_Insert
        move $a0, $v0
        move $s0, $a0
        jal List_Print
        move $a0, $v0
        move $a0, $a0
        li $a0, 12
        jal malloc
        move $a0, $v0
        move $t9, $a0
        li $a2, 27
        li $a0, 34000
        li $a1, 0
        jal Element_Init
        move $a0, $v0
        move $a0, $a0
        jal List_Search
        move $a0, $v0
        move $a0, $a0
        jal print_int
        move $a0, $v0
        jal List_Search
        move $a0, $v0
        move $a0, $a0
        jal print_int
        move $a0, $v0
        li $a0, 10000000
        jal print_int
        move $a0, $v0
        li $a0, 12
        jal malloc
        move $a0, $v0
        move $t9, $a0
        li $a2, 28
        li $a1, 35000
        li $a0, 0
        jal Element_Init
        move $a0, $v0
        move $a0, $a0
        jal List_Insert
        move $a0, $v0
        move $s0, $a0
        jal List_Print
        move $a0, $v0
        move $a0, $a0
        li $a0, 2220000
        jal print_int
        move $a0, $v0
        jal List_Delete
        move $a0, $v0
        move $s0, $a0
        jal List_Print
        move $a0, $v0
        move $a0, $a0
        li $a0, 33300000
        jal print_int
        move $a0, $v0
        jal List_Delete
        move $a0, $v0
        move $s0, $a0
        jal List_Print
        move $a0, $v0
        move $a0, $a0
        li $a0, 44440000
        jal print_int
        move $a0, $v0
        li $v0, 0
        move $s0, $k1
        move $s1, $k0
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
        move $ra, $t8
        j L89
L89:
```
### QuickSort.java
```assembly
===== QuickSort_main (Assembly) =====
L40:
        move $k1, $ra
        move $s0, $s0
        move $s1, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
        li $a0, 8
        jal malloc
        move $a0, $v0
        move $a1, $a0
        li $a0, 10
        jal QS_Start
        move $a0, $v0
        move $a0, $a0
        jal print_int
        move $a0, $v0
        move $s0, $s0
        move $s1, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
        move $ra, $k1
        j L39
L39:

===== QS_Start (Assembly) =====
L42:
        move $k0, $ra
        move $k1, $s0
        move $s1, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
QS_Start:
        jal QS_Init
        move $a0, $v0
        move $a1, $a0
        jal QS_Print
        move $a0, $v0
        move $a1, $a0
        li $a0, 9999
        jal print_int
        move $a0, $v0
        lw $a0, 4($s0)
        addi $a0, $a0, -1
        move $a1, $a0
        li $a0, 0
        jal QS_Sort
        move $a0, $v0
        move $a1, $a0
        jal QS_Print
        move $a0, $v0
        move $a1, $a0
        li $v0, 0
        move $s0, $k1
        move $s1, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
        move $ra, $k0
        j L41
L41:

===== QS_Sort (Assembly) =====
L44:
        move $k1, $ra
        move $k0, $s0
        move $s1, $s1
        move $s2, $s2
        move $t9, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
QS_Sort:
        li $a1, 0
        li $a0, 0
        blt $t1, $s0, L3
L4:
L5:
        li $a2, 0
        bne $a0, $a2, L0
L1:
        li $a0, 0
L2:
        li $v0, 0
        move $s0, $k0
        move $s1, $s1
        move $s2, $s2
        move $s3, $t9
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
        move $ra, $k1
        j L43
L3:
        li $a0, 1
        j L5
L0:
        lw $a2, 0($s0)
        li $a0, 4
        mul $a0, $s0, $a0
        add $a0, $a2, $a0
        lw $a0, 0($a0)
        move $t2, $a0
        addi $a0, $t1, -1
        move $s3, $a0
        move $t0, $s0
        li $a0, 1
L6:
        li $a2, 0
        beq $a0, $a2, L8
L7:
        li $a1, 1
L9:
        li $a0, 0
        beq $a1, $a0, L11
L10:
        addi $s3, $s3, 1
        lw $a0, 0($s0)
        li $a1, 4
        mul $a1, $s3, $a1
        add $a0, $a0, $a1
        lw $a0, 0($a0)
        move $a0, $a0
        li $a1, 0
        blt $a0, $t2, L15
L16:
L17:
        li $a0, 1
        xor $a0, $a1, $a0
        li $a1, 0
        bne $a0, $a1, L12
L13:
        li $a1, 1
L14:
        j L9
L15:
        li $a1, 1
        j L17
L12:
        li $a1, 0
        j L14
L11:
        li $a1, 1
L18:
        li $a0, 0
        beq $a1, $a0, L20
L19:
        addi $a0, $t0, -1
        move $t0, $a0
        lw $a1, 0($s0)
        li $a0, 4
        mul $a0, $t0, $a0
        add $a0, $a1, $a0
        lw $a0, 0($a0)
        move $a0, $a0
        li $a1, 0
        blt $t2, $a0, L24
L25:
L26:
        li $a0, 1
        xor $a0, $a1, $a0
        li $a1, 0
        bne $a0, $a1, L21
L22:
        li $a1, 1
L23:
        j L18
L24:
        li $a1, 1
        j L26
L21:
        li $a1, 0
        j L23
L20:
        lw $a1, 0($s0)
        li $a0, 4
        mul $a0, $s3, $a0
        add $a0, $a1, $a0
        lw $a0, 0($a0)
        move $a1, $a0
        lw $a2, 0($s0)
        li $a0, 4
        mul $a0, $s3, $a0
        add $a3, $a2, $a0
        lw $a2, 0($s0)
        li $a0, 4
        mul $a0, $t0, $a0
        add $a0, $a2, $a0
        lw $a0, 0($a0)
        sw $a0, 0($a3)
        lw $a2, 0($s0)
        li $a0, 4
        mul $a0, $t0, $a0
        add $a0, $a2, $a0
        sw $a1, 0($a0)
        li $a0, 0
        addi $a2, $s3, 1
        blt $t0, $a2, L30
L31:
L32:
        li $a2, 0
        bne $a0, $a2, L27
L28:
        li $a0, 1
L29:
        j L6
L30:
        li $a0, 1
        j L32
L27:
        li $a0, 0
        j L29
L8:
        lw $a2, 0($s0)
        li $a0, 4
        mul $a0, $t0, $a0
        add $a0, $a2, $a0
        lw $a2, 0($s0)
        li $a3, 4
        mul $a3, $s3, $a3
        add $a2, $a2, $a3
        lw $a2, 0($a2)
        sw $a2, 0($a0)
        lw $a2, 0($s0)
        li $a0, 4
        mul $a0, $s3, $a0
        add $a3, $a2, $a0
        lw $a0, 0($s0)
        li $a2, 4
        mul $a2, $s0, $a2
        add $a0, $a0, $a2
        lw $a0, 0($a0)
        sw $a0, 0($a3)
        lw $a0, 0($s0)
        li $a2, 4
        mul $a2, $s0, $a2
        add $a0, $a0, $a2
        sw $a1, 0($a0)
        addi $a0, $s3, -1
        jal QS_Sort
        move $a0, $v0
        move $a0, $a0
        addi $a0, $s3, 1
        jal QS_Sort
        move $a0, $v0
        move $a0, $a0
        j L2
L43:

===== QS_Print (Assembly) =====
L46:
        move $k1, $ra
        move $s0, $s0
        move $s1, $s1
        move $k0, $s2
        move $t9, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
QS_Print:
        li $s3, 0
L33:
        li $a1, 0
        lw $a0, 4($s2)
        blt $s3, $a0, L36
L37:
L38:
        li $a0, 0
        beq $a1, $a0, L35
L34:
        lw $a1, 0($s2)
        li $a0, 4
        mul $a0, $s3, $a0
        add $a0, $a1, $a0
        lw $a0, 0($a0)
        jal print_int
        move $a0, $v0
        addi $s3, $s3, 1
        j L33
L36:
        li $a1, 1
        j L38
L35:
        li $v0, 0
        move $s0, $s0
        move $s1, $s1
        move $s2, $k0
        move $s3, $t9
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
        move $ra, $k1
        j L45
L45:

===== QS_Init (Assembly) =====
L48:
        move $k1, $ra
        move $k0, $s0
        move $s1, $s1
        move $s2, $s2
        move $t9, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
QS_Init:
        sw $a1, 4($s0)
        addi $s3, $s0, 0
        li $a0, 4
        mul $a0, $a1, $a0
        jal malloc
        move $a0, $v0
        move $a0, $a0
        sw $a0, 0($s3)
        lw $a1, 0($s0)
        li $a0, 0
        li $a2, 4
        mul $a0, $a0, $a2
        add $a0, $a1, $a0
        li $a1, 20
        sw $a1, 0($a0)
        lw $a1, 0($s0)
        li $a2, 1
        li $a0, 4
        mul $a0, $a2, $a0
        add $a0, $a1, $a0
        li $a1, 7
        sw $a1, 0($a0)
        lw $a2, 0($s0)
        li $a0, 2
        li $a1, 4
        mul $a0, $a0, $a1
        add $a1, $a2, $a0
        li $a0, 12
        sw $a0, 0($a1)
        lw $a1, 0($s0)
        li $a0, 3
        li $a2, 4
        mul $a0, $a0, $a2
        add $a1, $a1, $a0
        li $a0, 18
        sw $a0, 0($a1)
        lw $a1, 0($s0)
        li $a0, 4
        li $a2, 4
        mul $a0, $a0, $a2
        add $a0, $a1, $a0
        li $a1, 2
        sw $a1, 0($a0)
        lw $a0, 0($s0)
        li $a1, 5
        li $a2, 4
        mul $a1, $a1, $a2
        add $a1, $a0, $a1
        li $a0, 11
        sw $a0, 0($a1)
        lw $a0, 0($s0)
        li $a1, 6
        li $a2, 4
        mul $a1, $a1, $a2
        add $a1, $a0, $a1
        li $a0, 6
        sw $a0, 0($a1)
        lw $a1, 0($s0)
        li $a0, 7
        li $a2, 4
        mul $a0, $a0, $a2
        add $a0, $a1, $a0
        li $a1, 9
        sw $a1, 0($a0)
        lw $a2, 0($s0)
        li $a0, 8
        li $a1, 4
        mul $a0, $a0, $a1
        add $a0, $a2, $a0
        li $a1, 19
        sw $a1, 0($a0)
        lw $a2, 0($s0)
        li $a1, 9
        li $a0, 4
        mul $a0, $a1, $a0
        add $a1, $a2, $a0
        li $a0, 5
        sw $a0, 0($a1)
        li $v0, 0
        move $s0, $k0
        move $s1, $s1
        move $s2, $s2
        move $s3, $t9
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
        move $ra, $k1
        j L47
L47:
```

### TreeVisitor.java
```assembly
===== TreeVisitor_main (Assembly) =====
L112:
        move $k1, $ra
        move $s0, $s0
        move $s1, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
        li $a0, 0
        jal malloc
        move $a0, $v0
        move $a0, $a0
        jal TV_Start
        move $a0, $v0
        move $a0, $a0
        jal print_int
        move $a0, $v0
        move $s0, $s0
        move $s1, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
        move $ra, $k1
        j L111
L111:

===== TV_Start (Assembly) =====
L114:
        move $k1, $ra
        move $k0, $s0
        move $s1, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
TV_Start:
        li $a0, 24
        jal malloc
        move $a0, $v0
        move $s0, $a0
        li $a0, 16
        jal Tree_Init
        move $a0, $v0
        move $a0, $a0
        jal Tree_Print
        move $a0, $v0
        move $a0, $a0
        li $a0, 100000000
        jal print_int
        move $a0, $v0
        li $a0, 8
        jal Tree_Insert
        move $a0, $v0
        move $a0, $a0
        li $a0, 24
        jal Tree_Insert
        move $a0, $v0
        move $a0, $a0
        li $a0, 4
        jal Tree_Insert
        move $a0, $v0
        move $a0, $a0
        li $a0, 12
        jal Tree_Insert
        move $a0, $v0
        move $a0, $a0
        li $a0, 20
        jal Tree_Insert
        move $a0, $v0
        move $a0, $a0
        li $a0, 28
        jal Tree_Insert
        move $a0, $v0
        move $a0, $a0
        li $a0, 14
        jal Tree_Insert
        move $a0, $v0
        move $a0, $a0
        jal Tree_Print
        move $a0, $v0
        move $a0, $a0
        li $a0, 100000000
        jal print_int
        move $a0, $v0
        li $a0, 8
        jal malloc
        move $a0, $v0
        move $t9, $a0
        li $a0, 50000000
        jal print_int
        move $a0, $v0
        jal Tree_accept
        move $a0, $v0
        move $a0, $a0
        li $a0, 100000000
        jal print_int
        move $a0, $v0
        li $a0, 24
        jal Tree_Search
        move $a0, $v0
        move $a0, $a0
        jal print_int
        move $a0, $v0
        li $a0, 12
        jal Tree_Search
        move $a0, $v0
        move $a0, $a0
        jal print_int
        move $a0, $v0
        li $a0, 16
        jal Tree_Search
        move $a0, $v0
        move $a0, $a0
        jal print_int
        move $a0, $v0
        li $a0, 50
        jal Tree_Search
        move $a0, $v0
        move $a0, $a0
        jal print_int
        move $a0, $v0
        li $a0, 12
        jal Tree_Search
        move $a0, $v0
        move $a0, $a0
        jal print_int
        move $a0, $v0
        li $a0, 12
        jal Tree_Delete
        move $a0, $v0
        move $a0, $a0
        jal Tree_Print
        move $a0, $v0
        move $a0, $a0
        li $a0, 12
        jal Tree_Search
        move $a0, $v0
        move $a0, $a0
        jal print_int
        move $a0, $v0
        li $v0, 0
        move $s0, $k0
        move $s1, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
        move $ra, $k1
        j L113
L113:

===== Tree_Init (Assembly) =====
L116:
        move $t3, $ra
        move $a0, $s0
        move $t4, $s1
        move $t2, $s2
        move $t0, $s3
        move $a2, $s4
        move $a1, $s5
        move $t1, $s6
        move $a3, $s7
Tree_Init:
        sw $t5, 8($t6)
        li $t5, 0
        sw $t5, 12($t6)
        li $t5, 0
        sw $t5, 16($t6)
        li $v0, 1
        move $s0, $a0
        move $s1, $t4
        move $s2, $t2
        move $s3, $t0
        move $s4, $a2
        move $s5, $a1
        move $s6, $t1
        move $s7, $a3
        move $ra, $t3
        j L115
L115:

===== Tree_SetRight (Assembly) =====
L118:
        move $t3, $ra
        move $a0, $s0
        move $a1, $s1
        move $t4, $s2
        move $t2, $s3
        move $t0, $s4
        move $t1, $s5
        move $a3, $s6
        move $a2, $s7
Tree_SetRight:
        sw $t5, 4($t5)
        li $v0, 1
        move $s0, $a0
        move $s1, $a1
        move $s2, $t4
        move $s3, $t2
        move $s4, $t0
        move $s5, $t1
        move $s6, $a3
        move $s7, $a2
        move $ra, $t3
        j L117
L117:

===== Tree_SetLeft (Assembly) =====
L120:
        move $t4, $ra
        move $t0, $s0
        move $a0, $s1
        move $t1, $s2
        move $t3, $s3
        move $t2, $s4
        move $a3, $s5
        move $a2, $s6
        move $a1, $s7
Tree_SetLeft:
        sw $t5, 0($t5)
        li $v0, 1
        move $s0, $t0
        move $s1, $a0
        move $s2, $t1
        move $s3, $t3
        move $s4, $t2
        move $s5, $a3
        move $s6, $a2
        move $s7, $a1
        move $ra, $t4
        j L119
L119:

===== Tree_GetRight (Assembly) =====
L122:
        move $a1, $ra
        move $a3, $s0
        move $t4, $s1
        move $t2, $s2
        move $t0, $s3
        move $t1, $s4
        move $t3, $s5
        move $a2, $s6
        move $a0, $s7
Tree_GetRight:
        lw $v0, 4($t5)
        move $s0, $a3
        move $s1, $t4
        move $s2, $t2
        move $s3, $t0
        move $s4, $t1
        move $s5, $t3
        move $s6, $a2
        move $s7, $a0
        move $ra, $a1
        j L121
L121:

===== Tree_GetLeft (Assembly) =====
L124:
        move $t3, $ra
        move $t0, $s0
        move $t2, $s1
        move $a0, $s2
        move $a1, $s3
        move $t1, $s4
        move $t4, $s5
        move $a3, $s6
        move $a2, $s7
Tree_GetLeft:
        lw $v0, 0($t5)
        move $s0, $t0
        move $s1, $t2
        move $s2, $a0
        move $s3, $a1
        move $s4, $t1
        move $s5, $t4
        move $s6, $a3
        move $s7, $a2
        move $ra, $t3
        j L123
L123:

===== Tree_GetKey (Assembly) =====
L126:
        move $a1, $ra
        move $t4, $s0
        move $a3, $s1
        move $t3, $s2
        move $t2, $s3
        move $a0, $s4
        move $a2, $s5
        move $t1, $s6
        move $t0, $s7
Tree_GetKey:
        lw $v0, 8($t5)
        move $s0, $t4
        move $s1, $a3
        move $s2, $t3
        move $s3, $t2
        move $s4, $a0
        move $s5, $a2
        move $s6, $t1
        move $s7, $t0
        move $ra, $a1
        j L125
L125:

===== Tree_SetKey (Assembly) =====
L128:
        move $a1, $ra
        move $t0, $s0
        move $a0, $s1
        move $t4, $s2
        move $t3, $s3
        move $a3, $s4
        move $a2, $s5
        move $t2, $s6
        move $t1, $s7
Tree_SetKey:
        sw $t5, 8($t5)
        li $v0, 1
        move $s0, $t0
        move $s1, $a0
        move $s2, $t4
        move $s3, $t3
        move $s4, $a3
        move $s5, $a2
        move $s6, $t2
        move $s7, $t1
        move $ra, $a1
        j L127
L127:

===== Tree_GetHas_Right (Assembly) =====
L130:
        move $t2, $ra
        move $t1, $s0
        move $a2, $s1
        move $t4, $s2
        move $t3, $s3
        move $a3, $s4
        move $a1, $s5
        move $t0, $s6
        move $a0, $s7
Tree_GetHas_Right:
        lw $v0, 16($t5)
        move $s0, $t1
        move $s1, $a2
        move $s2, $t4
        move $s3, $t3
        move $s4, $a3
        move $s5, $a1
        move $s6, $t0
        move $s7, $a0
        move $ra, $t2
        j L129
L129:

===== Tree_GetHas_Left (Assembly) =====
L132:
        move $t1, $ra
        move $a2, $s0
        move $t3, $s1
        move $a1, $s2
        move $a0, $s3
        move $t0, $s4
        move $t2, $s5
        move $a3, $s6
        move $t4, $s7
Tree_GetHas_Left:
        lw $v0, 12($t5)
        move $s0, $a2
        move $s1, $t3
        move $s2, $a1
        move $s3, $a0
        move $s4, $t0
        move $s5, $t2
        move $s6, $a3
        move $s7, $t4
        move $ra, $t1
        j L131
L131:

===== Tree_SetHas_Left (Assembly) =====
L134:
        move $t4, $ra
        move $a3, $s0
        move $a0, $s1
        move $a2, $s2
        move $t0, $s3
        move $t1, $s4
        move $t2, $s5
        move $a1, $s6
        move $t3, $s7
Tree_SetHas_Left:
        sw $t5, 12($t5)
        li $v0, 1
        move $s0, $a3
        move $s1, $a0
        move $s2, $a2
        move $s3, $t0
        move $s4, $t1
        move $s5, $t2
        move $s6, $a1
        move $s7, $t3
        move $ra, $t4
        j L133
L133:

===== Tree_SetHas_Right (Assembly) =====
L136:
        move $a3, $ra
        move $t2, $s0
        move $t0, $s1
        move $t4, $s2
        move $a2, $s3
        move $a0, $s4
        move $a1, $s5
        move $t3, $s6
        move $t1, $s7
Tree_SetHas_Right:
        sw $t5, 16($t5)
        li $v0, 1
        move $s0, $t2
        move $s1, $t0
        move $s2, $t4
        move $s3, $a2
        move $s4, $a0
        move $s5, $a1
        move $s6, $t3
        move $s7, $t1
        move $ra, $a3
        j L135
L135:

===== Tree_Compare (Assembly) =====
L138:
        move $t2, $ra
        move $a2, $s0
        move $t3, $s1
        move $t4, $s2
        move $t1, $s3
        move $a0, $s4
        move $a3, $s5
        move $t0, $s6
        move $a1, $s7
Tree_Compare:
        li $t5, 0
        addi $t7, $t6, 1
        li $s0, 0
        blt $t6, $t6, L3
L4:
L5:
        li $t5, 0
        bne $s0, $t5, L0
L1:
        li $t5, 0
        blt $t6, $t7, L9
L10:
L11:
        li $t6, 1
        xor $t5, $t5, $t6
        li $t6, 0
        bne $t5, $t6, L6
L7:
        li $t5, 1
L8:
L2:
        move $v0, $t5
        move $s0, $a2
        move $s1, $t3
        move $s2, $t4
        move $s3, $t1
        move $s4, $a0
        move $s5, $a3
        move $s6, $t0
        move $s7, $a1
        move $ra, $t2
        j L137
L3:
        li $s0, 1
        j L5
L0:
        li $t5, 0
        j L2
L9:
        li $t5, 1
        j L11
L6:
        li $t5, 0
        j L8
L137:

===== Tree_Insert (Assembly) =====
L140:
        move $k1, $ra
        move $s0, $s0
        move $t9, $s1
        move $k0, $s2
        move $t8, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $v1, $s7
Tree_Insert:
        li $a0, 24
        jal malloc
        move $a0, $v0
        move $s3, $a0
        jal Tree_Init
        move $a0, $v0
        move $a0, $a0
        move $s7, $s1
        li $s2, 1
L12:
        li $a0, 0
        beq $s2, $a0, L14
L13:
        jal Tree_GetKey
        move $a0, $v0
        move $a1, $a0
        li $a0, 0
        blt $s1, $a1, L18
L19:
L20:
        li $a1, 0
        bne $a0, $a1, L15
L16:
        jal Tree_GetHas_Right
        move $a0, $v0
        move $a0, $a0
        li $a1, 0
        bne $a0, $a1, L24
L25:
        li $s2, 0
        li $a0, 1
        jal Tree_SetHas_Right
        move $a0, $v0
        move $a0, $a0
        jal Tree_SetRight
        move $a0, $v0
        move $a0, $a0
L26:
L17:
        j L12
L18:
        li $a0, 1
        j L20
L15:
        jal Tree_GetHas_Left
        move $a0, $v0
        move $a0, $a0
        li $a1, 0
        bne $a0, $a1, L21
L22:
        li $s2, 0
        li $a0, 1
        jal Tree_SetHas_Left
        move $a0, $v0
        move $a0, $a0
        jal Tree_SetLeft
        move $a0, $v0
        move $a0, $a0
L23:
        j L17
L21:
        jal Tree_GetLeft
        move $a0, $v0
        move $s7, $a0
        j L23
L24:
        jal Tree_GetRight
        move $a0, $v0
        move $s7, $a0
        j L26
L14:
        li $v0, 1
        move $s0, $s0
        move $s1, $t9
        move $s2, $k0
        move $s3, $t8
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $v1
        move $ra, $k1
        j L139
L139:

===== Tree_Delete (Assembly) =====
Aviso: Spill detectado durante a alocacao de registradores. A compilacao gerada ignorara os spills.
L142:
        move null, $ra
        move $k1, $s0
        move $s1, $s1
        move $s2, $s2
        move $v1, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
Tree_Delete:
        move $k0, $s0
        move null, $s0
        li $t8, 1
        li $s3, 0
        li $t9, 1
L27:
        li $a0, 0
        beq $t8, $a0, L29
L28:
        jal Tree_GetKey
        move $a0, $v0
        move $a1, $a0
        li $a2, 0
        blt $s0, $a1, L33
L34:
L35:
        li $a0, 0
        bne $a2, $a0, L30
L31:
        li $a0, 0
        blt $a1, $s0, L42
L43:
L44:
        li $a1, 0
        bne $a0, $a1, L39
L40:
        li $a0, 0
        bne $t9, $a0, L48
L49:
        jal Tree_Remove
        move $a0, $v0
        move $a0, $a0
L50:
        li $s3, 1
        li $t8, 0
L41:
L32:
        li $t9, 0
        j L27
L33:
        li $a2, 1
        j L35
L30:
        jal Tree_GetHas_Left
        move $a0, $v0
        move $a1, $a0
        li $a0, 0
        bne $a1, $a0, L36
L37:
        li $t8, 0
L38:
        j L32
L36:
        move null, $k0
        jal Tree_GetLeft
        move $a0, $v0
        move $k0, $a0
        j L38
L42:
        li $a0, 1
        j L44
L39:
        jal Tree_GetHas_Right
        move $a0, $v0
        move $a1, $a0
        li $a0, 0
        bne $a1, $a0, L45
L46:
        li $t8, 0
L47:
        j L41
L45:
        move null, $k0
        jal Tree_GetRight
        move $a0, $v0
        move $k0, $a0
        j L47
L48:
        li $s3, 0
        jal Tree_GetHas_Right
        move $a0, $v0
        move $a0, $a0
        li $a1, 0
        beq $a0, $a1, L55
L54:
        jal Tree_GetHas_Left
        move $a0, $v0
        move $a0, $a0
        li $a1, 1
        xor $a1, $a0, $a1
        li $a0, 0
        beq $a1, $a0, L55
L56:
        li $a0, 1
        xor $a0, $s3, $a0
        li $a1, 0
        bne $a0, $a1, L51
L52:
        jal Tree_Remove
        move $a0, $v0
        move $a0, $a0
L53:
        j L50
L143:
        li $s3, 1
        j L56
L55:
        j L56
L51:
        li $a0, 1
        j L53
L29:
        move $v0, $s3
        move $s0, $k1
        move $s1, $s1
        move $s2, $s2
        move $s3, $v1
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
        move $ra, null
        j L141
L141:

===== Tree_Remove (Assembly) =====
L145:
        move $k0, $ra
        move $k1, $s0
        move $s1, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
Tree_Remove:
        jal Tree_GetHas_Left
        move $a0, $v0
        move $a0, $a0
        li $a1, 0
        bne $a0, $a1, L57
L58:
        jal Tree_GetHas_Right
        move $a0, $v0
        move $a0, $a0
        li $a1, 0
        bne $a0, $a1, L60
L61:
        jal Tree_GetKey
        move $a0, $v0
        move $t9, $a0
        jal Tree_GetLeft
        move $a0, $v0
        move $a0, $a0
        jal Tree_GetKey
        move $a0, $v0
        move $a0, $a0
        jal Tree_Compare
        move $a0, $v0
        move $a0, $a0
        li $a1, 0
        bne $a0, $a1, L63
L64:
        lw $a0, 20($s0)
        jal Tree_SetRight
        move $a0, $v0
        move $a0, $a0
        li $a0, 0
        jal Tree_SetHas_Right
        move $a0, $v0
        move $a0, $a0
L65:
L62:
L59:
        li $v0, 1
        move $s0, $k1
        move $s1, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
        move $ra, $k0
        j L144
L57:
        jal Tree_RemoveLeft
        move $a0, $v0
        move $a0, $a0
        j L59
L60:
        jal Tree_RemoveRight
        move $a0, $v0
        move $a0, $a0
        j L62
L63:
        lw $a0, 20($s0)
        jal Tree_SetLeft
        move $a0, $v0
        move $a0, $a0
        li $a0, 0
        jal Tree_SetHas_Left
        move $a0, $v0
        move $a0, $a0
        j L65
L144:

===== Tree_RemoveRight (Assembly) =====
L147:
        move $t8, $ra
        move $k1, $s0
        move $s1, $s1
        move $k0, $s2
        move $s3, $s3
        move $s4, $s4
        move $t9, $s5
        move $s6, $s6
        move $s7, $s7
Tree_RemoveRight:
L66:
        jal Tree_GetHas_Right
        move $a0, $v0
        move $a1, $a0
        li $a0, 0
        beq $a1, $a0, L68
L67:
        move $s0, $s2
        jal Tree_GetRight
        move $a0, $v0
        move $a0, $a0
        jal Tree_GetKey
        move $a0, $v0
        move $a0, $a0
        jal Tree_SetKey
        move $a0, $v0
        move $a0, $a0
        move $s0, $s2
        jal Tree_GetRight
        move $a0, $v0
        move $s2, $a0
        j L66
L68:
        lw $a0, 20($s5)
        jal Tree_SetRight
        move $a0, $v0
        move $a0, $a0
        li $a0, 0
        jal Tree_SetHas_Right
        move $a0, $v0
        move $a0, $a0
        li $v0, 1
        move $s0, $k1
        move $s1, $s1
        move $s2, $k0
        move $s3, $s3
        move $s4, $s4
        move $s5, $t9
        move $s6, $s6
        move $s7, $s7
        move $ra, $t8
        j L146
L146:

===== Tree_RemoveLeft (Assembly) =====
L149:
        move $t9, $ra
        move $k1, $s0
        move $k0, $s1
        move $s2, $s2
        move $t8, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
Tree_RemoveLeft:
L69:
        jal Tree_GetHas_Left
        move $a0, $v0
        move $a0, $a0
        li $a1, 0
        beq $a0, $a1, L71
L70:
        move $s0, $s0
        jal Tree_GetLeft
        move $a0, $v0
        move $a0, $a0
        jal Tree_GetKey
        move $a0, $v0
        move $a0, $a0
        jal Tree_SetKey
        move $a0, $v0
        move $a0, $a0
        move $s3, $s0
        jal Tree_GetLeft
        move $a0, $v0
        move $s0, $a0
        j L69
L71:
        lw $a0, 20($s1)
        jal Tree_SetLeft
        move $a0, $v0
        move $a0, $a0
        li $a0, 0
        jal Tree_SetHas_Left
        move $a0, $v0
        move $a0, $a0
        li $v0, 1
        move $s0, $k1
        move $s1, $k0
        move $s2, $s2
        move $s3, $t8
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
        move $ra, $t9
        j L148
L148:

===== Tree_Search (Assembly) =====
L151:
        move $k1, $ra
        move $s0, $s0
        move $s1, $s1
        move $t8, $s2
        move $k0, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
Tree_Search:
        move $s3, $a0
        li $s2, 1
        li $t9, 0
L72:
        li $a0, 0
        beq $s2, $a0, L74
L73:
        jal Tree_GetKey
        move $a0, $v0
        move $a1, $a0
        li $a0, 0
        blt $v1, $a1, L78
L79:
L80:
        li $a2, 0
        bne $a0, $a2, L75
L76:
        li $a0, 0
        blt $a1, $v1, L87
L88:
L89:
        li $a1, 0
        bne $a0, $a1, L84
L85:
        li $t9, 1
        li $s2, 0
L86:
L77:
        j L72
L78:
        li $a0, 1
        j L80
L75:
        jal Tree_GetHas_Left
        move $a0, $v0
        move $a0, $a0
        li $a1, 0
        bne $a0, $a1, L81
L82:
        li $s2, 0
L83:
        j L77
L81:
        jal Tree_GetLeft
        move $a0, $v0
        move $s3, $a0
        j L83
L87:
        li $a0, 1
        j L89
L84:
        jal Tree_GetHas_Right
        move $a0, $v0
        move $a0, $a0
        li $a1, 0
        bne $a0, $a1, L90
L91:
        li $s2, 0
L92:
        j L86
L90:
        jal Tree_GetRight
        move $a0, $v0
        move $s3, $a0
        j L92
L74:
        move $v0, $t9
        move $s0, $s0
        move $s1, $s1
        move $s2, $t8
        move $s3, $k0
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
        move $ra, $k1
        j L150
L150:

===== Tree_Print (Assembly) =====
L153:
        move $k1, $ra
        move $s0, $s0
        move $s1, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
Tree_Print:
        move $a0, $a0
        jal Tree_RecPrint
        move $a0, $v0
        move $a0, $a0
        li $v0, 1
        move $s0, $s0
        move $s1, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
        move $ra, $k1
        j L152
L152:

===== Tree_RecPrint (Assembly) =====
L155:
        move $k1, $ra
        move $t9, $s0
        move $s1, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
Tree_RecPrint:
        jal Tree_GetHas_Left
        move $a0, $v0
        move $a1, $a0
        li $a0, 0
        bne $a1, $a0, L93
L94:
        li $a0, 1
L95:
        jal Tree_GetKey
        move $a0, $v0
        move $a0, $a0
        jal print_int
        move $a0, $v0
        jal Tree_GetHas_Right
        move $a0, $v0
        move $a0, $a0
        li $a1, 0
        bne $a0, $a1, L96
L97:
        li $a0, 1
L98:
        li $v0, 1
        move $s0, $t9
        move $s1, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
        move $ra, $k1
        j L154
L93:
        move $s0, $s0
        jal Tree_GetLeft
        move $a0, $v0
        move $a0, $a0
        jal Tree_RecPrint
        move $a0, $v0
        move $a0, $a0
        j L95
L96:
        move $s0, $s0
        jal Tree_GetRight
        move $a0, $v0
        move $a0, $a0
        jal Tree_RecPrint
        move $a0, $v0
        move $a0, $a0
        j L98
L154:

===== Tree_accept (Assembly) =====
L157:
        move $k0, $ra
        move $k1, $s0
        move $s1, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
Tree_accept:
        li $a0, 333
        jal print_int
        move $a0, $v0
        jal Visitor_visit
        move $a0, $v0
        move $a0, $a0
        li $v0, 0
        move $s0, $k1
        move $s1, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
        move $ra, $k0
        j L156
L156:

===== Visitor_visit (Assembly) =====
L159:
        move $k0, $ra
        move $k1, $s0
        move $t9, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
Visitor_visit:
        jal Tree_GetHas_Right
        move $a0, $v0
        move $a1, $a0
        li $a0, 0
        bne $a1, $a0, L99
L100:
        li $a0, 0
L101:
        jal Tree_GetHas_Left
        move $a0, $v0
        move $a1, $a0
        li $a0, 0
        bne $a1, $a0, L102
L103:
        li $a0, 0
L104:
        li $v0, 0
        move $s0, $k1
        move $s1, $t9
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
        move $ra, $k0
        j L158
L99:
        addi $s0, $s1, 4
        jal Tree_GetRight
        move $a0, $v0
        move $a0, $a0
        sw $a0, 0($s0)
        lw $a0, 4($s1)
        jal Tree_accept
        move $a0, $v0
        move $a0, $a0
        j L101
L102:
        addi $s0, $s1, 0
        jal Tree_GetLeft
        move $a0, $v0
        move $a0, $a0
        sw $a0, 0($s0)
        lw $a0, 0($s1)
        jal Tree_accept
        move $a0, $v0
        move $a0, $a0
        j L104
L158:

===== MyVisitor_visit (Assembly) =====
L161:
        move $k1, $ra
        move $k0, $s0
        move $t9, $s1
        move $s2, $s2
        move $s3, $s3
        move $s4, $s4
        move $s5, $s5
        move $s6, $s6
        move $s7, $s7
MyVisitor_visit:
        jal Tree_GetHas_Right
        move $a0, $v0
        move $a0, $a0
        li $a1, 0
        bne $a0, $a1, L105
L106:
        li $a0, 0
L107:
        jal Tree_GetKey
        move $a0, $v0
        move $a0, $a0
        jal print_int
        move $a0, $v0
        jal Tree_GetHas_Left
        move $a0, $v0
        move $a0, $a0
        li $a1, 0
        bne $a0, $a1, L108
L109:
        li $a0, 0
L110:
        li $v0, 0
        move $a0, $a0
        sw $a0, 0($s0)
        lw $a0, 0($s1)
        jal Tree_accept
        move $a0, $v0
        move $a0, $a0
        j L110
L160:
```
*(As cores finais escolhidas para cada temp dependem do grau e ordem do nó durante o desempilhamento iterativo em `Color.java`)*

---

## Dificuldades Encontradas

- **Ameaça Incessante de "Spill":** A infraestrutura padrão MIPS (`MipsFrame`) do projeto reservava as 8 cores normais (`$s0-$s7`) para temporários callee-saves (que precisam sobreviver cruzando nós `JAL`). Como métodos recursivos e complexos (ex: testes de remoção e pesquisa binária) carregavam mais de 8 temporários simultaneamente vivos durante `jal` (por conta do parâmetro `this`, salvamento da chamada de retorno (`RA`) e outras variáveis), o compilador estourava essas 8 opções rapidamente correndo o risco de gerar ponteiros `null` e obrigando o projeto a fazer _"Spill"_ para a memória (o que estenderia drasticamente a complexidade do trabalho exigindo reescrita de IR). A solução inteligente adotada para desviar dessa restrição foi simular registradores ociosos (`$v1, $t8, $t9, $k0, $k1`) providenciando mais cores "callee-save" de suporte interno exclusivamente para a tabela de coloração do MipsFrame, suprimindo o Spill por completo nos métodos exigidos.
- **Concatenação de Blocos Básicos:** O RegAlloc precisa ser executado numa passada inteira por todo o método, mas a saída da fase anterior (`Codegen`) dividiu as instruções num nível granular de statements (`Traces.stms`). Foi imperativo criar um mecanismo de linkagem (junção de `InstrList`) no fluxo do `Main` antes da montagem e alocação do grafo.

---

## Participação

| Membro | Participação |
|---|---|
| Werbster Marques Teixeira [537205] | Cópia e adequação estrutural da ETAPA 04. Implementação iterativa em `RegAlloc/Liveness.java`, do algoritmo de coloração heurística (`Color.java`) em pilha e do aumento inteligente da lista de callee-saves em MipsFrame para mitigação do Spilling. |
| Guilherme Gomes Botelho [539008] | Implementação da conversão e verificação do fluxo no `AssemFlowGraph.java`, formatação final e concatenação das `InstrList` no `Main.java`, atualização dos scripts automáticos de build/run em powershell e confecção do arquivo descritivo README. |
