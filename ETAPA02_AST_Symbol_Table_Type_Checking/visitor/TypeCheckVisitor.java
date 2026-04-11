package visitor;

import syntaxtree.*;
import symboltable.*;

/**
 * Analisa tipos percorrendo a AST.
 * Estende TypeDepthFirstVisitor (que retorna null por padrão) e sobrescreve
 * apenas os métodos onde há lógica semântica real.
 */
public class TypeCheckVisitor extends TypeDepthFirstVisitor {

    private final SymbolTable table;
    private String currentClass;
    private String currentMethod;
    private int errorCount = 0;

    public TypeCheckVisitor(SymbolTable table) {
        this.table = table;
    }

    public int getErrorCount() {
        return errorCount;
    }

    // ========== Raiz do programa ==========

    @Override
    public Type visit(Program n) {
    	// Visita a MainClass
    	n.m.accept(this);
    	
        // Visita as classes declaradas 
        for (int i = 0; i < n.cl.size(); i++)
            n.cl.elementAt(i).accept(this);
        return null;
    }

    @Override
    public Type visit(MainClass n) {
        // Visita o statement da main com contexto da classe (para checar chamadas de método)
        currentClass = n.i1.s;
        currentMethod = "main";
        n.s.accept(this);
        currentClass  = null;
        currentMethod = null;
        return null;
    }

    // ========== Contexto de classe / método ==========

    @Override
    public Type visit(ClassDeclSimple n) {
        currentClass = n.i.s;
        for (int i = 0; i < n.ml.size(); i++)
            n.ml.elementAt(i).accept(this);
        currentClass = null;
        return null;
    }

    @Override
    public Type visit(ClassDeclExtends n) {
        currentClass = n.i.s;
        for (int i = 0; i < n.ml.size(); i++)
            n.ml.elementAt(i).accept(this);
        currentClass = null;
        return null;
    }

    @Override
    public Type visit(Block n) {
        for (int i = 0; i < n.sl.size(); i++)
            n.sl.elementAt(i).accept(this);
        return null;
    }

    @Override
    public Type visit(MethodDecl n) {
        currentMethod = n.i.s;
        for (int i = 0; i < n.sl.size(); i++)
            n.sl.elementAt(i).accept(this);
        Type retType = n.e.accept(this);
        checkTypeMatch(n.t, retType, "retorno do método '" + n.i.s + "'");
        currentMethod = null;
        return null;
    }

    // ========== Statements ==========

    @Override
    public Type visit(If n) {
        Type cond = n.e.accept(this);
        if (!(cond instanceof BooleanType))
            error("Condição do 'if' deve ser boolean, recebeu: " + typeName(cond));
        n.s1.accept(this);
        n.s2.accept(this);
        return null;
    }

    @Override
    public Type visit(While n) {
        Type cond = n.e.accept(this);
        if (!(cond instanceof BooleanType))
            error("Condição do 'while' deve ser boolean, recebeu: " + typeName(cond));
        n.s.accept(this);
        return null;
    }

    @Override
    public Type visit(Print n) {
        Type t = n.e.accept(this);
        if (!(t instanceof IntegerType))
            error("System.out.println aceita apenas int, recebeu: " + typeName(t));
        return null;
    }

    @Override
    public Type visit(Assign n) {
        Type declared = table.lookupVariable(n.i.s, currentClass, currentMethod);
        if (declared == null) {
            error("Variável não declarada: '" + n.i.s + "'");
            return null;
        }
        Type assigned = n.e.accept(this);
        checkTypeMatch(declared, assigned, "atribuição de '" + n.i.s + "'");
        return null;
    }

    @Override
    public Type visit(ArrayAssign n) {
        Type arrType = table.lookupVariable(n.i.s, currentClass, currentMethod);
        if (!(arrType instanceof IntArrayType))
            error("'" + n.i.s + "' não é int[]");
        Type idx = n.e1.accept(this);
        if (!(idx instanceof IntegerType))
            error("Índice de array deve ser int");
        Type val = n.e2.accept(this);
        if (!(val instanceof IntegerType))
            error("Valor atribuído ao array deve ser int");
        return null;
    }

    // ========== Expressões ==========

    @Override
    public Type visit(IntegerLiteral n) {
        return new IntegerType();
    }

    @Override
    public Type visit(True n) {
        return new BooleanType();
    }

    @Override
    public Type visit(False n) {
        return new BooleanType();
    }

    @Override
    public Type visit(This n) {
        return new IdentifierType(currentClass);
    }

    @Override
    public Type visit(IdentifierExp n) {
        Type t = table.lookupVariable(n.s, currentClass, currentMethod);
        if (t == null) {
            error("Variável não declarada: '" + n.s + "'");
            return new IntegerType();
        }
        return t;
    }

    @Override
    public Type visit(Plus n) {
        Type t1 = n.e1.accept(this), t2 = n.e2.accept(this);
        if (!(t1 instanceof IntegerType))
            error("'+' exige int no lado esquerdo");
        if (!(t2 instanceof IntegerType))
            error("'+' exige int no lado direito");
        return new IntegerType();
    }

    @Override
    public Type visit(Minus n) {
        Type t1 = n.e1.accept(this), t2 = n.e2.accept(this);
        if (!(t1 instanceof IntegerType))
            error("'-' exige int no lado esquerdo");
        if (!(t2 instanceof IntegerType))
            error("'-' exige int no lado direito");
        return new IntegerType();
    }

    @Override
    public Type visit(Times n) {
        Type t1 = n.e1.accept(this), t2 = n.e2.accept(this);
        if (!(t1 instanceof IntegerType))
            error("'*' exige int no lado esquerdo");
        if (!(t2 instanceof IntegerType))
            error("'*' exige int no lado direito");
        return new IntegerType();
    }

    @Override
    public Type visit(LessThan n) {
        Type t1 = n.e1.accept(this), t2 = n.e2.accept(this);
        if (!(t1 instanceof IntegerType))
            error("'<' exige int no lado esquerdo");
        if (!(t2 instanceof IntegerType))
            error("'<' exige int no lado direito");
        return new BooleanType();
    }

    @Override
    public Type visit(And n) {
        Type t1 = n.e1.accept(this), t2 = n.e2.accept(this);
        if (!(t1 instanceof BooleanType))
            error("'&&' exige boolean no lado esquerdo");
        if (!(t2 instanceof BooleanType))
            error("'&&' exige boolean no lado direito");
        return new BooleanType();
    }

    @Override
    public Type visit(Not n) {
        Type t = n.e.accept(this);
        if (!(t instanceof BooleanType))
            error("'!' exige boolean");
        return new BooleanType();
    }

    @Override
    public Type visit(ArrayLookup n) {
        Type arr = n.e1.accept(this);
        Type idx = n.e2.accept(this);
        if (!(arr instanceof IntArrayType))
            error("Acesso de array em expressão que não é int[]");
        if (!(idx instanceof IntegerType))
            error("Índice de array deve ser int");
        return new IntegerType();
    }

    @Override
    public Type visit(ArrayLength n) {
        Type t = n.e.accept(this);
        if (!(t instanceof IntArrayType))
            error("'.length' aplicado em tipo que não é int[]");
        return new IntegerType();
    }

    @Override
    public Type visit(NewArray n) {
        Type t = n.e.accept(this);
        if (!(t instanceof IntegerType))
            error("Tamanho do array deve ser int");
        return new IntArrayType();
    }

    @Override
    public Type visit(NewObject n) {
        if (table.lookupClass(n.i.s) == null)
            error("Classe não declarada: '" + n.i.s + "'");
        return new IdentifierType(n.i.s);
    }

    @Override
    public Type visit(Call n) {
        Type objType = n.e.accept(this);
        if (!(objType instanceof IdentifierType)) {
            error("Chamada de método em tipo não-objeto: " + typeName(objType));
            return new IntegerType();
        }
        String className = ((IdentifierType) objType).s;
        MethodBinding mb = table.lookupMethod(className, n.i.s);
        if (mb == null) {
            error("Método '" + n.i.s + "' não encontrado na classe '" + className + "'");
            return new IntegerType();
        }
        return mb.returnType;
    }

    // ========== Utilitários ==========

    private void error(String msg) {
        System.err.println("[ERRO SEMÂNTICO] " + msg);
        errorCount++;
    }

    private String typeName(Type t) {
        if (t instanceof IntegerType)
            return "int";
        if (t instanceof BooleanType)
            return "boolean";
        if (t instanceof IntArrayType)
            return "int[]";
        if (t instanceof IdentifierType)
            return ((IdentifierType) t).s;
        return "desconhecido";
    }

    private void checkTypeMatch(Type expected, Type actual, String context) {
        if (expected == null || actual == null)
            return;
        if (!typesCompatible(expected, actual))
            error("Tipo incompatível em " + context +
                    ": esperado " + typeName(expected) + ", recebeu " + typeName(actual));
    }

    private boolean typesCompatible(Type a, Type b) {
        if (a instanceof IntegerType && b instanceof IntegerType)
            return true;
        if (a instanceof BooleanType && b instanceof BooleanType)
            return true;
        if (a instanceof IntArrayType && b instanceof IntArrayType)
            return true;
        if (a instanceof IdentifierType && b instanceof IdentifierType) {
            String sa = ((IdentifierType) a).s;
            String sb = ((IdentifierType) b).s;
            return sa.equals(sb) || isSubclass(sb, sa);
        }
        return false;
    }

    private boolean isSubclass(String sub, String sup) {
        symboltable.ClassBinding cb = table.lookupClass(sub);
        if (cb == null || cb.superClass == null)
            return false;
        return cb.superClass.equals(sup) || isSubclass(cb.superClass, sup);
    }
}
