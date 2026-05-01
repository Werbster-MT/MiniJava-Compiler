package visitor;

import java.util.LinkedHashMap;
import java.util.Map;

import frame.Access;
import mips.MipsFrame;
import symboltable.ClassBinding;
import symboltable.MethodBinding;
import symboltable.SymbolTable;
import syntaxtree.And;
import syntaxtree.ArrayAssign;
import syntaxtree.ArrayLength;
import syntaxtree.ArrayLookup;
import syntaxtree.Assign;
import syntaxtree.Block;
import syntaxtree.Call;
import syntaxtree.ClassDecl;
import syntaxtree.ClassDeclExtends;
import syntaxtree.ClassDeclSimple;
import syntaxtree.Exp;
import syntaxtree.False;
import syntaxtree.IdentifierExp;
import syntaxtree.If;
import syntaxtree.IntegerLiteral;
import syntaxtree.LessThan;
import syntaxtree.MainClass;
import syntaxtree.MethodDecl;
import syntaxtree.Minus;
import syntaxtree.NewArray;
import syntaxtree.NewObject;
import syntaxtree.Not;
import syntaxtree.Plus;
import syntaxtree.Print;
import syntaxtree.Program;
import syntaxtree.Statement;
import syntaxtree.This;
import syntaxtree.Times;
import syntaxtree.True;
import syntaxtree.While;

public class IRGenVisitor {
    private final SymbolTable table;
    private frame.Frame currentFrame;
    private String currentClass;
    private String currentMethod;
    private Map<String, Access> currentAccesses = new LinkedHashMap<>();

    public IRGenVisitor(SymbolTable table) {
        this.table = table;
    }

    public Map<String, Tree.Stm> transProgram(Program p) {
        Map<String, Tree.Stm> methods = new LinkedHashMap<>();
        methods.put(mainLabelName(p.m), transMainClass(p.m));
        for (int i = 0; i < p.cl.size(); i++) {
            ClassDecl c = p.cl.elementAt(i);
            methods.putAll(transClass(c));
        }
        return methods;
    }

    private Map<String, Tree.Stm> transClass(ClassDecl c) {
        if (c instanceof ClassDeclSimple) {
            return transClassSimple((ClassDeclSimple) c);
        }
        return transClassExtends((ClassDeclExtends) c);
    }

    private Map<String, Tree.Stm> transClassSimple(ClassDeclSimple c) {
        Map<String, Tree.Stm> out = new LinkedHashMap<>();
        currentClass = c.i.s;
        for (int i = 0; i < c.ml.size(); i++) {
            MethodDecl m = c.ml.elementAt(i);
            out.put(methodLabelName(currentClass, m.i.s), transMethodDecl(m, currentClass));
        }
        currentClass = null;
        return out;
    }

    private Map<String, Tree.Stm> transClassExtends(ClassDeclExtends c) {
        Map<String, Tree.Stm> out = new LinkedHashMap<>();
        currentClass = c.i.s;
        for (int i = 0; i < c.ml.size(); i++) {
            MethodDecl m = c.ml.elementAt(i);
            out.put(methodLabelName(currentClass, m.i.s), transMethodDecl(m, currentClass));
        }
        currentClass = null;
        return out;
    }

    private Tree.Stm transMainClass(MainClass m) {
        currentClass = m.i1.s;
        currentMethod = "main";
        currentFrame = new MipsFrame(new Temp.Label(mainLabelName(m)), new Util.BoolList(false, null));
        currentAccesses = new LinkedHashMap<>();
        currentAccesses.put(m.i2.s, currentFrame.formals.get(0));
        Tree.Stm body = transStm(m.s);
        currentClass = null;
        currentMethod = null;
        return currentFrame.procEntryExit1(body);
    }

    public Tree.Stm transMethodDecl(MethodDecl m, String className) {
        currentClass = className;
        currentMethod = m.i.s;
        currentFrame = new MipsFrame(new Temp.Label(methodLabelName(className, m.i.s)), formalEscapes(m));
        currentAccesses = new LinkedHashMap<>();
        bindMethodAccesses(m);

        Tree.Stm body = null;
        for (int i = 0; i < m.sl.size(); i++) {
            body = seq(body, transStm(m.sl.elementAt(i)));
        }
        body = seq(body, new Tree.MOVE(new Tree.TEMP(currentFrame.RV()), transExp(m.e)));
        body = seq(new Tree.LABEL(currentFrame.name), body);
        return currentFrame.procEntryExit1(body);
    }

    private void bindMethodAccesses(MethodDecl m) {
        int idx = 0;
        currentAccesses.put("this", currentFrame.formals.get(idx++));
        for (int i = 0; i < m.fl.size(); i++) {
            currentAccesses.put(m.fl.elementAt(i).i.s, currentFrame.formals.get(idx++));
        }
        for (int i = 0; i < m.vl.size(); i++) {
            currentAccesses.put(m.vl.elementAt(i).i.s, currentFrame.allocLocal(false));
        }
    }

    private Util.BoolList formalEscapes(MethodDecl m) {
        Util.BoolList list = null;
        for (int i = m.fl.size() - 1; i >= 0; i--) {
            list = new Util.BoolList(true, list);
        }
        return new Util.BoolList(true, list);
    }

    public Tree.Exp transExp(Exp e) {
        if (e instanceof IntegerLiteral) return new Tree.CONST(((IntegerLiteral) e).i);
        if (e instanceof True) return new Tree.CONST(1);
        if (e instanceof False) return new Tree.CONST(0);
        if (e instanceof This) return accessExp("this");
        if (e instanceof Plus) return new Tree.BINOP(Tree.BINOP.PLUS, transExp(((Plus) e).e1), transExp(((Plus) e).e2));
        if (e instanceof Minus) return new Tree.BINOP(Tree.BINOP.MINUS, transExp(((Minus) e).e1), transExp(((Minus) e).e2));
        if (e instanceof Times) return new Tree.BINOP(Tree.BINOP.MUL, transExp(((Times) e).e1), transExp(((Times) e).e2));
        if (e instanceof LessThan) return transLessThan((LessThan) e);
        if (e instanceof And) return transAnd((And) e);
        if (e instanceof Not) return new Tree.BINOP(Tree.BINOP.XOR, transExp(((Not) e).e), new Tree.CONST(1));
        if (e instanceof IdentifierExp) return transIdentifier((IdentifierExp) e);
        if (e instanceof NewObject) return transNewObject((NewObject) e);
        if (e instanceof NewArray) return transNewArray((NewArray) e);
        if (e instanceof ArrayLookup) return transArrayLookup((ArrayLookup) e);
        if (e instanceof ArrayLength) return transArrayLength((ArrayLength) e);
        if (e instanceof Call) return transCall((Call) e);
        throw new Error("IRGen: expressao desconhecida: " + e.getClass().getName());
    }

    public Tree.Stm transStm(Statement s) {
        if (s instanceof Block) return transBlock((Block) s);
        if (s instanceof If) return transIf((If) s);
        if (s instanceof While) return transWhile((While) s);
        if (s instanceof Print) return transPrint((Print) s);
        if (s instanceof Assign) return transAssign((Assign) s);
        if (s instanceof ArrayAssign) return transArrayAssign((ArrayAssign) s);
        throw new Error("IRGen: statement desconhecido: " + s.getClass().getName());
    }

    private Tree.Stm transBlock(Block b) {
        Tree.Stm out = null;
        for (int i = 0; i < b.sl.size(); i++) {
            out = seq(out, transStm(b.sl.elementAt(i)));
        }
        return out == null ? new Tree.EXPSTM(new Tree.CONST(0)) : out;
    }

    private Tree.Stm transIf(If i) {
        Temp.Label lThen = new Temp.Label();
        Temp.Label lElse = new Temp.Label();
        Temp.Label lDone = new Temp.Label();
        return seq(
            new Tree.CJUMP(Tree.CJUMP.NE, transExp(i.e), new Tree.CONST(0), lThen, lElse),
            seq(
                new Tree.LABEL(lThen),
                seq(
                    transStm(i.s1),
                    seq(
                        new Tree.JUMP(lDone),
                        seq(new Tree.LABEL(lElse), seq(transStm(i.s2), new Tree.LABEL(lDone)))
                    )
                )
            )
        );
    }

    private Tree.Stm transWhile(While w) {
        Temp.Label lTest = new Temp.Label();
        Temp.Label lBody = new Temp.Label();
        Temp.Label lDone = new Temp.Label();
        return seq(
            new Tree.LABEL(lTest),
            seq(
                new Tree.CJUMP(Tree.CJUMP.EQ, transExp(w.e), new Tree.CONST(0), lDone, lBody),
                seq(
                    new Tree.LABEL(lBody),
                    seq(transStm(w.s), seq(new Tree.JUMP(lTest), new Tree.LABEL(lDone)))
                )
            )
        );
    }

    private Tree.Stm transPrint(Print p) {
        return new Tree.EXPSTM(
            currentFrame.externalCall("print_int", new Tree.ExpList(transExp(p.e), null))
        );
    }

    private Tree.Stm transAssign(Assign a) {
        Tree.Exp dst = accessOrFieldExp(a.i.s);
        return new Tree.MOVE(dst, transExp(a.e));
    }

    private Tree.Stm transArrayAssign(ArrayAssign a) {
        Tree.Exp arr = accessOrFieldExp(a.i.s);
        Tree.Exp index = transExp(a.e1);
        Tree.Exp addr = new Tree.BINOP(
            Tree.BINOP.PLUS,
            arr,
            new Tree.BINOP(Tree.BINOP.MUL, index, new Tree.CONST(currentFrame.wordSize()))
        );
        return new Tree.MOVE(new Tree.MEM(addr), transExp(a.e2));
    }

    private Tree.Exp transIdentifier(IdentifierExp id) {
        return accessOrFieldExp(id.s);
    }

    private Tree.Exp transNewObject(NewObject n) {
        int fields = fieldCount(n.i.s);
        return currentFrame.externalCall("malloc", new Tree.ExpList(new Tree.CONST(fields * currentFrame.wordSize()), null));
    }

    private Tree.Exp transNewArray(NewArray n) {
        Tree.Exp bytes = new Tree.BINOP(Tree.BINOP.MUL, transExp(n.e), new Tree.CONST(currentFrame.wordSize()));
        return currentFrame.externalCall("malloc", new Tree.ExpList(bytes, null));
    }

    private Tree.Exp transArrayLookup(ArrayLookup n) {
        Tree.Exp addr = new Tree.BINOP(
            Tree.BINOP.PLUS,
            transExp(n.e1),
            new Tree.BINOP(Tree.BINOP.MUL, transExp(n.e2), new Tree.CONST(currentFrame.wordSize()))
        );
        return new Tree.MEM(addr);
    }

    private Tree.Exp transArrayLength(ArrayLength n) {
        return new Tree.MEM(new Tree.BINOP(Tree.BINOP.MINUS, transExp(n.e), new Tree.CONST(currentFrame.wordSize())));
    }

    private Tree.Exp transCall(Call n) {
        String recvClass = classNameOf(n.e);
        if (recvClass == null) recvClass = currentClass;
        Tree.ExpList args = buildArgList(n.el);
        args = new Tree.ExpList(transExp(n.e), args);
        return new Tree.CALL(new Tree.NAME(new Temp.Label(methodLabelName(recvClass, n.i.s))), args);
    }

    private Tree.ExpList buildArgList(syntaxtree.ExpList list) {
        Tree.ExpList out = null;
        for (int i = list.size() - 1; i >= 0; i--) {
            out = new Tree.ExpList(transExp(list.elementAt(i)), out);
        }
        return out;
    }

    private Tree.Exp transLessThan(LessThan lt) {
        Temp.Temp result = new Temp.Temp();
        Temp.Label t = new Temp.Label();
        Temp.Label f = new Temp.Label();
        Temp.Label done = new Temp.Label();
        Tree.Stm stm = seq(
            new Tree.MOVE(new Tree.TEMP(result), new Tree.CONST(0)),
            seq(
                new Tree.CJUMP(Tree.CJUMP.LT, transExp(lt.e1), transExp(lt.e2), t, f),
                seq(
                    new Tree.LABEL(t),
                    seq(
                        new Tree.MOVE(new Tree.TEMP(result), new Tree.CONST(1)),
                        seq(new Tree.JUMP(done), seq(new Tree.LABEL(f), new Tree.LABEL(done)))
                    )
                )
            )
        );
        return new Tree.ESEQ(stm, new Tree.TEMP(result));
    }

    private Tree.Exp transAnd(And and) {
        Temp.Temp result = new Temp.Temp();
        Temp.Label evalRhs = new Temp.Label();
        Temp.Label falseL = new Temp.Label();
        Temp.Label done = new Temp.Label();
        Tree.Stm stm = seq(
            new Tree.MOVE(new Tree.TEMP(result), new Tree.CONST(0)),
            seq(
                new Tree.CJUMP(Tree.CJUMP.EQ, transExp(and.e1), new Tree.CONST(0), falseL, evalRhs),
                seq(
                    new Tree.LABEL(evalRhs),
                    seq(
                        new Tree.CJUMP(Tree.CJUMP.EQ, transExp(and.e2), new Tree.CONST(0), falseL, done),
                        seq(
                            new Tree.MOVE(new Tree.TEMP(result), new Tree.CONST(1)),
                            seq(new Tree.JUMP(done), seq(new Tree.LABEL(falseL), new Tree.LABEL(done)))
                        )
                    )
                )
            )
        );
        return new Tree.ESEQ(stm, new Tree.TEMP(result));
    }

    private Tree.Exp accessOrFieldExp(String name) {
        if (currentAccesses.containsKey(name)) {
            return accessExp(name);
        }
        int offset = fieldOffset(currentClass, name);
        if (offset < 0) {
            throw new Error("IRGen: variavel nao encontrada: " + name + " em " + currentClass + "." + currentMethod);
        }
        return new Tree.MEM(
            new Tree.BINOP(Tree.BINOP.PLUS, accessExp("this"), new Tree.CONST(offset))
        );
    }

    private Tree.Exp accessExp(String name) {
        Access acc = currentAccesses.get(name);
        return currentFrame.exp(acc, new Tree.TEMP(currentFrame.FP()));
    }

    private String classNameOf(Exp exp) {
        if (exp instanceof This) return currentClass;
        if (exp instanceof NewObject) return ((NewObject) exp).i.s;
        if (exp instanceof IdentifierExp) {
            syntaxtree.Type t = table.lookupVariable(((IdentifierExp) exp).s, currentClass, currentMethod);
            if (t instanceof syntaxtree.IdentifierType) {
                return ((syntaxtree.IdentifierType) t).s;
            }
        }
        return currentClass;
    }

    private int fieldCount(String className) {
        int count = 0;
        ClassBinding cb = table.lookupClass(className);
        if (cb == null) return 0;
        if (cb.superClass != null) count += fieldCount(cb.superClass);
        count += cb.fields.size();
        return count;
    }

    private int fieldOffset(String className, String field) {
        int[] idx = new int[] {0};
        return fieldOffsetRec(className, field, idx);
    }

    private int fieldOffsetRec(String className, String field, int[] idx) {
        ClassBinding cb = table.lookupClass(className);
        if (cb == null) return -1;
        if (cb.superClass != null) {
            int inParent = fieldOffsetRec(cb.superClass, field, idx);
            if (inParent >= 0) return inParent;
        }
        for (String f : cb.fields.keySet()) {
            if (f.equals(field)) return idx[0] * currentFrame.wordSize();
            idx[0]++;
        }
        return -1;
    }

    private static Tree.Stm seq(Tree.Stm a, Tree.Stm b) {
        if (a == null) return b;
        if (b == null) return a;
        return new Tree.SEQ(a, b);
    }

    private String methodLabelName(String className, String methodName) {
        return className + "_" + methodName;
    }

    private String mainLabelName(MainClass m) {
        return m.i1.s + "_main";
    }
}
