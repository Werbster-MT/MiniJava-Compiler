package symboltable;

import syntaxtree.*;
import visitor.Visitor;

/**
 * Percorre a AST uma vez e constrói a SymbolTable.
 * Implementa o Visitor clássico (void) para varrer declarações.
 */
public class SymbolTableBuilder implements Visitor {

    private final SymbolTable table;
    private String currentClass;
    private String currentMethod;

    public SymbolTableBuilder(SymbolTable table) { this.table = table; }

    // ---- Programa ----
    public void visit(Program n) {
        n.m.accept(this);
        for (int i = 0; i < n.cl.size(); i++) n.cl.elementAt(i).accept(this);
    }

    public void visit(MainClass n) {
        ClassBinding cb = new ClassBinding(n.i1.s, null);
        table.addClass(cb);
    }

    public void visit(ClassDeclSimple n) {
        currentClass = n.i.s;
        ClassBinding cb = new ClassBinding(n.i.s, null);
        table.addClass(cb);
        for (int i = 0; i < n.vl.size(); i++) n.vl.elementAt(i).accept(this);
        for (int i = 0; i < n.ml.size(); i++) n.ml.elementAt(i).accept(this);
        currentClass = null;
    }

    public void visit(ClassDeclExtends n) {
        currentClass = n.i.s;
        ClassBinding cb = new ClassBinding(n.i.s, n.j.s);
        table.addClass(cb);
        for (int i = 0; i < n.vl.size(); i++) n.vl.elementAt(i).accept(this);
        for (int i = 0; i < n.ml.size(); i++) n.ml.elementAt(i).accept(this);
        currentClass = null;
    }

    public void visit(VarDecl n) {
        ClassBinding cb = table.lookupClass(currentClass);
        if (cb == null) return;
        if (currentMethod != null) {
            MethodBinding mb = cb.getMethod(currentMethod);
            if (mb != null) mb.addLocal(n.i.s, n.t);
        } else {
            cb.addField(n.i.s, n.t);
        }
    }

    public void visit(MethodDecl n) {
        currentMethod = n.i.s;
        ClassBinding cb = table.lookupClass(currentClass);
        if (cb != null) {
            MethodBinding mb = new MethodBinding(n.i.s, n.t);
            // parâmetros
            for (int i = 0; i < n.fl.size(); i++) {
                Formal f = n.fl.elementAt(i);
                mb.addFormal(f.i.s, f.t);
            }
            cb.addMethod(mb);
            // variáveis locais
            for (int i = 0; i < n.vl.size(); i++) n.vl.elementAt(i).accept(this);
        }
        currentMethod = null;
    }

    // ---- Stubs obrigatórios (não usados na 1ª passagem) ----
    public void visit(Formal n)       {}
    public void visit(IntArrayType n) {}
    public void visit(BooleanType n)  {}
    public void visit(IntegerType n)  {}
    public void visit(IdentifierType n){}
    public void visit(Block n)        {}
    public void visit(If n)           {}
    public void visit(While n)        {}
    public void visit(Print n)        {}
    public void visit(Assign n)       {}
    public void visit(ArrayAssign n)  {}
    public void visit(And n)          {}
    public void visit(LessThan n)     {}
    public void visit(Plus n)         {}
    public void visit(Minus n)        {}
    public void visit(Times n)        {}
    public void visit(ArrayLookup n)  {}
    public void visit(ArrayLength n)  {}
    public void visit(Call n)         {}
    public void visit(IntegerLiteral n){}
    public void visit(True n)         {}
    public void visit(False n)        {}
    public void visit(IdentifierExp n){}
    public void visit(This n)         {}
    public void visit(NewArray n)     {}
    public void visit(NewObject n)    {}
    public void visit(Not n)          {}
    public void visit(Identifier n)   {}
}
