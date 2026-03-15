import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

public class Main {
    public static void main(String[] args) {
        try {
            // 1. Realiza a leitura da entrada pelo console
            System.out.println("Digite o código MiniJava (aperte Ctrl+Z e Enter no Windows para finalizar):");
            CharStream input = CharStreams.fromStream(System.in);

            // 2. Passa o texto para o Lexer (Analisador Lexico)
            MiniJavaLexer lexer = new MiniJavaLexer(input);

            // 3. Cria um fluxo de tokens
            CommonTokenStream tokens = new CommonTokenStream(lexer);

            // 4. Passa os tokens para o Parser (Analisador Sintatico)
            MiniJavaParser parser = new MiniJavaParser(tokens);

            // 5. Inicia a analise chamando a regra raiz da gramatica
            ParseTree tree = parser.goal();

            System.out.println("\nAnálise Léxica e Sintática concluída com sucesso!");
            
            // Imprime a arvore gerada em texto!
            System.out.println(tree.toStringTree(parser));

        } catch (Exception e) {
            System.err.println("Erro durante a compilação: " + e.getMessage());
        }
    }
}