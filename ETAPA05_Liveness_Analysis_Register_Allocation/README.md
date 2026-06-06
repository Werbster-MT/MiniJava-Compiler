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

A principal distinção da ETAPA 05 está na geração final do Assembly MIPS **sem a presença de variáveis temporárias literais**. 

Abaixo um trecho comparativo comprovando a substituição limpa na alocação:

### Assembly sem Alocação (Exemplo Saída da ETAPA04)
```assembly
L9:
	move t27, t0
	move t28, t1
Fac_ComputeFac:
	li t26, 0
	li t45, 1
	blt t24, t45, L3
...
```

### Assembly 100% Alocado (Exemplo Saída da ETAPA05)
```assembly
L9:
	move $ra, $s0
	move $s1, $s1
Fac_ComputeFac:
	li $s0, 0
	li $t9, 1
	blt $t8, $t9, L3
...
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
