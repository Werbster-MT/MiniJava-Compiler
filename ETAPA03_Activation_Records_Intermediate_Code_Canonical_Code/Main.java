import java.util.Map;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import Canon.BasicBlocks;
import Canon.Canon;
import Canon.TraceSchedule;
import parser.MiniJavaLexer;
import parser.MiniJavaParser;
import symboltable.SymbolTable;
import symboltable.SymbolTableBuilder;
import syntaxtree.Program;
import visitor.BuildASTVisitor;
import visitor.IRGenVisitor;
import visitor.TypeCheckVisitor;

public class Main {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Uso: java Main <arquivo.mj>");
            return;
        }

        CharStream input = CharStreams.fromFileName(args[0]);
        MiniJavaLexer lexer = new MiniJavaLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        MiniJavaParser parser = new MiniJavaParser(tokens);
        ParseTree parseTree = parser.goal();

        if (parser.getNumberOfSyntaxErrors() > 0) {
            System.err.println("Erros sintaticos encontrados. Abortando.");
            System.exit(1);
        }

        BuildASTVisitor builder = new BuildASTVisitor();
        Program ast = (Program) builder.visit(parseTree);

        SymbolTable symbolTable = new SymbolTable();
        SymbolTableBuilder stBuilder = new SymbolTableBuilder(symbolTable);
        ast.accept(stBuilder);

        TypeCheckVisitor typeChecker = new TypeCheckVisitor(symbolTable);
        ast.accept(typeChecker);
        if (typeChecker.getErrorCount() > 0) {
            System.err.println("Erros semanticos encontrados. Abortando.");
            System.exit(1);
        }

        IRGenVisitor irGen = new IRGenVisitor(symbolTable);
        Map<String, Tree.Stm> methods = irGen.transProgram(ast);

        Tree.Print printer = new Tree.Print(System.out);
        for (Map.Entry<String, Tree.Stm> entry : methods.entrySet()) {
            Tree.StmList linear = Canon.linearize(entry.getValue());
            BasicBlocks blocks = new BasicBlocks(linear);
            TraceSchedule traces = new TraceSchedule(blocks);

            System.out.println("===== " + entry.getKey() + " =====");
            for (Tree.StmList l = traces.stms; l != null; l = l.tail) {
                printer.prStm(l.head);
            }
            System.out.println();
        }
    }
}
