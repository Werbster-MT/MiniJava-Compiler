import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import parser.MiniJavaLexer;
import parser.MiniJavaParser;

import syntaxtree.Program;
import symboltable.*;
import visitor.*;

public class Main {

    public static void main(String[] args) throws Exception {
        // ── 1. Leitura da entrada ─────────────────────────────────────────
        CharStream input;
        if (args.length > 0) {
            input = CharStreams.fromFileName(args[0]);
        } else {
            System.out.println("Uso: java Main <arquivo.mj>");
            System.out.println("Ou lendo do stdin (Ctrl+Z + Enter para encerrar):\n");
            input = CharStreams.fromStream(System.in);
        }

        // ── 2. Análise léxica e sintática (ANTLR) ────────────────────────
        MiniJavaLexer  lexer  = new MiniJavaLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        MiniJavaParser parser = new MiniJavaParser(tokens);

        ParseTree parseTree = parser.goal();

        if (parser.getNumberOfSyntaxErrors() > 0) {
            System.err.println("\n[ETAPA 01] Erros sintáticos encontrados. Abortando.");
            System.exit(1);
        }
        System.out.println("\n[ETAPA 01] Análise léxica e sintática: OK");

        // ── 3. Construção da AST ──────────────────────────────────────────
        BuildASTVisitor builder = new BuildASTVisitor();
        Program ast = (Program) builder.visit(parseTree);
        System.out.println("\n[ETAPA 02] AST construída com sucesso.");

        // ── 4. Pretty Print da AST ────────────────────────────────────────
        System.out.println("\n──────────────────────────── AST (Pretty Print) ────────────────────────────\n");
        PrettyPrintVisitor printer = new PrettyPrintVisitor();
        ast.accept(printer);
        System.out.println("\n────────────────────────────────────────────────────────────────────────────\n");

        // ── 5. Construção da Tabela de Símbolos ───────────────────────────
        SymbolTable      symTable = new SymbolTable();
        SymbolTableBuilder stBuilder = new SymbolTableBuilder(symTable);
        ast.accept(stBuilder);
        System.out.println("[ETAPA 03] Tabela de símbolos construída.");
        printSymbolTable(symTable);

        // ── 6. Verificação de Tipos ───────────────────────────────────────
        TypeCheckVisitor typeChecker = new TypeCheckVisitor(symTable);
        ast.accept(typeChecker);

        int errors = typeChecker.getErrorCount();
        if (errors == 0) {
            System.out.println("[ETAPA 04] Verificação de tipos: OK — nenhum erro semântico.");
        } else {
            System.err.println("[ETAPA 04] Verificação de tipos: " + errors + " erro(s) semântico(s) encontrado(s).");
            System.exit(1);
        }
    }

    /** Imprime um resumo da tabela de símbolos para diagnóstico. */
    private static void printSymbolTable(SymbolTable st) {
        System.out.println("\n──────────────────────────── Tabela de Símbolos ────────────────────────────\n");
        for (symboltable.ClassBinding cb : st.getClasses().values()) {
            String ext = cb.superClass != null ? " extends " + cb.superClass : "";
            System.out.println("Classe: " + cb.name + ext);
            for (var e : cb.fields.entrySet())
                System.out.println("  campo: " + typeName(e.getValue()) + " " + e.getKey());
            for (symboltable.MethodBinding mb : cb.methods.values()) {
                System.out.print("  método: " + typeName(mb.returnType) + " " + mb.name + "(");
                var params = mb.formals.entrySet().iterator();
                while (params.hasNext()) {
                    var p = params.next();
                    System.out.print(typeName(p.getValue()) + " " + p.getKey());
                    if (params.hasNext()) System.out.print(", ");
                }
                System.out.println(")");
                for (var v : mb.locals.entrySet())
                    System.out.println("    local: " + typeName(v.getValue()) + " " + v.getKey());
            }
        }
        System.out.println("\n────────────────────────────────────────────────────────────────────────────\n");
    }

    private static String typeName(syntaxtree.Type t) {
        if (t instanceof syntaxtree.IntegerType)    return "int";
        if (t instanceof syntaxtree.BooleanType)    return "boolean";
        if (t instanceof syntaxtree.IntArrayType)   return "int[]";
        if (t instanceof syntaxtree.IdentifierType) return ((syntaxtree.IdentifierType) t).s;
        return "?";
    }
}
