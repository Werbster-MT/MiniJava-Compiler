package visitor;

import syntaxtree.*;
import parser.MiniJavaBaseVisitor;
import parser.MiniJavaParser;

/**
 * Converte a Parse Tree gerada pelo ANTLR em uma AST (syntaxtree.*).
 * Cada visitX visita os filhos e constrói o nó AST correspondente.
 */
public class BuildASTVisitor extends MiniJavaBaseVisitor<Object> {

    // ---- Raiz ----
    @Override
    public Object visitGoal(MiniJavaParser.GoalContext ctx) {
        MainClass mc = (MainClass) visit(ctx.mainClass());
        ClassDeclList cl = new ClassDeclList();
        for (MiniJavaParser.ClassDeclContext cd : ctx.classDecl())
            cl.addElement((ClassDecl) visit(cd));
        return new Program(mc, cl);
    }

    @Override
    public Object visitMainClass(MiniJavaParser.MainClassContext ctx) {
        Identifier className = new Identifier(ctx.Identifier(0).getText());
        Identifier argsName  = new Identifier(ctx.Identifier(1).getText());
        Statement  stmt      = (Statement) visit(ctx.statement());
        return new MainClass(className, argsName, stmt);
    }

    // ---- Declarações de classe ----
    @Override
    public Object visitClassSimple(MiniJavaParser.ClassSimpleContext ctx) {
        Identifier id = new Identifier(ctx.Identifier().getText());
        VarDeclList    vl = buildVarDeclList(ctx.varDecl());
        MethodDeclList ml = buildMethodDeclList(ctx.methodDecl());
        return new ClassDeclSimple(id, vl, ml);
    }

    @Override
    public Object visitClassExtends(MiniJavaParser.ClassExtendsContext ctx) {
        Identifier id    = new Identifier(ctx.Identifier(0).getText());
        Identifier parent = new Identifier(ctx.Identifier(1).getText());
        VarDeclList    vl = buildVarDeclList(ctx.varDecl());
        MethodDeclList ml = buildMethodDeclList(ctx.methodDecl());
        return new ClassDeclExtends(id, parent, vl, ml);
    }

    // ---- Membros ----
    @Override
    public Object visitVarDecl(MiniJavaParser.VarDeclContext ctx) {
        Type       t = (Type)       visit(ctx.type());
        Identifier i = new Identifier(ctx.Identifier().getText());
        return new VarDecl(t, i);
    }

    @Override
    public Object visitMethodDecl(MiniJavaParser.MethodDeclContext ctx) {
        Type       t  = (Type) visit(ctx.type());
        Identifier id = new Identifier(ctx.Identifier().getText());
        FormalList fl = ctx.formalList() != null
                        ? (FormalList) visit(ctx.formalList()) : new FormalList();
        VarDeclList    vl = buildVarDeclList(ctx.varDecl());
        StatementList  sl = buildStatementList(ctx.statement());
        Exp            e  = (Exp) visit(ctx.exp());
        return new MethodDecl(t, id, fl, vl, sl, e);
    }

    @Override
    public Object visitFormalList(MiniJavaParser.FormalListContext ctx) {
        FormalList fl = new FormalList();
        fl.addElement(new Formal((Type) visit(ctx.type()),
                                 new Identifier(ctx.Identifier().getText())));
        for (MiniJavaParser.FormalRestContext fr : ctx.formalRest())
            fl.addElement((Formal) visit(fr));
        return fl;
    }

    @Override
    public Object visitFormalRest(MiniJavaParser.FormalRestContext ctx) {
        return new Formal((Type) visit(ctx.type()),
                          new Identifier(ctx.Identifier().getText()));
    }

    // ---- Tipos ----
    @Override public Object visitTypeIntArray (MiniJavaParser.TypeIntArrayContext  ctx) { return new IntArrayType();  }
    @Override public Object visitTypeBoolean  (MiniJavaParser.TypeBooleanContext   ctx) { return new BooleanType();   }
    @Override public Object visitTypeInt      (MiniJavaParser.TypeIntContext       ctx) { return new IntegerType();   }
    @Override public Object visitTypeIdentifier(MiniJavaParser.TypeIdentifierContext ctx){
        return new IdentifierType(ctx.Identifier().getText());
    }

    // ---- Statements ----
    @Override
    public Object visitStmtBlock(MiniJavaParser.StmtBlockContext ctx) {
        return new Block(buildStatementList(ctx.statement()));
    }

    @Override
    public Object visitStmtIf(MiniJavaParser.StmtIfContext ctx) {
        Exp       e  = (Exp)       visit(ctx.exp());
        Statement s1 = (Statement) visit(ctx.statement(0));
        Statement s2 = (Statement) visit(ctx.statement(1));
        return new If(e, s1, s2);
    }

    @Override
    public Object visitStmtWhile(MiniJavaParser.StmtWhileContext ctx) {
        return new While((Exp) visit(ctx.exp()), (Statement) visit(ctx.statement()));
    }

    @Override
    public Object visitStmtPrint(MiniJavaParser.StmtPrintContext ctx) {
        return new Print((Exp) visit(ctx.exp()));
    }

    @Override
    public Object visitStmtAssign(MiniJavaParser.StmtAssignContext ctx) {
        return new Assign(new Identifier(ctx.Identifier().getText()), (Exp) visit(ctx.exp()));
    }

    @Override
    public Object visitStmtArrayAssign(MiniJavaParser.StmtArrayAssignContext ctx) {
        Identifier id = new Identifier(ctx.Identifier().getText());
        Exp e1 = (Exp) visit(ctx.exp(0));
        Exp e2 = (Exp) visit(ctx.exp(1));
        return new ArrayAssign(id, e1, e2);
    }

    // ---- Expressões ----
    @Override public Object visitExpArrayAccess(MiniJavaParser.ExpArrayAccessContext ctx) {
        return new ArrayLookup((Exp) visit(ctx.exp(0)), (Exp) visit(ctx.exp(1)));
    }
    @Override public Object visitExpLength(MiniJavaParser.ExpLengthContext ctx) {
        return new ArrayLength((Exp) visit(ctx.exp()));
    }
    @Override public Object visitExpMethodCall(MiniJavaParser.ExpMethodCallContext ctx) {
        Exp        obj    = (Exp) visit(ctx.exp());
        Identifier method = new Identifier(ctx.Identifier().getText());
        ExpList    args   = ctx.expList() != null
                            ? (ExpList) visit(ctx.expList()) : new ExpList();
        return new Call(obj, method, args);
    }
    @Override public Object visitExpTimes  (MiniJavaParser.ExpTimesContext   ctx) { return new Times   ((Exp) visit(ctx.exp(0)), (Exp) visit(ctx.exp(1))); }
    @Override public Object visitExpPlus   (MiniJavaParser.ExpPlusContext    ctx) { return new Plus    ((Exp) visit(ctx.exp(0)), (Exp) visit(ctx.exp(1))); }
    @Override public Object visitExpMinus  (MiniJavaParser.ExpMinusContext   ctx) { return new Minus   ((Exp) visit(ctx.exp(0)), (Exp) visit(ctx.exp(1))); }
    @Override public Object visitExpLessThan(MiniJavaParser.ExpLessThanContext ctx){ return new LessThan((Exp) visit(ctx.exp(0)), (Exp) visit(ctx.exp(1))); }
    @Override public Object visitExpAnd    (MiniJavaParser.ExpAndContext     ctx) { return new And     ((Exp) visit(ctx.exp(0)), (Exp) visit(ctx.exp(1))); }
    @Override public Object visitExpIntLiteral (MiniJavaParser.ExpIntLiteralContext  ctx) { return new IntegerLiteral(Integer.parseInt(ctx.INTEGER_LITERAL().getText())); }
    @Override public Object visitExpTrue       (MiniJavaParser.ExpTrueContext        ctx) { return new True(); }
    @Override public Object visitExpFalse      (MiniJavaParser.ExpFalseContext       ctx) { return new False(); }
    @Override public Object visitExpIdentifier (MiniJavaParser.ExpIdentifierContext  ctx) { return new IdentifierExp(ctx.Identifier().getText()); }
    @Override public Object visitExpThis       (MiniJavaParser.ExpThisContext        ctx) { return new This(); }
    @Override public Object visitExpNewArray   (MiniJavaParser.ExpNewArrayContext    ctx) { return new NewArray((Exp) visit(ctx.exp())); }
    @Override public Object visitExpNewObject  (MiniJavaParser.ExpNewObjectContext   ctx) { return new NewObject(new Identifier(ctx.Identifier().getText())); }
    @Override public Object visitExpNot        (MiniJavaParser.ExpNotContext         ctx) { return new Not((Exp) visit(ctx.exp())); }
    @Override public Object visitExpParenthesized(MiniJavaParser.ExpParenthesizedContext ctx) { return visit(ctx.exp()); }

    // ---- ExpList ----
    @Override
    public Object visitExpList(MiniJavaParser.ExpListContext ctx) {
        ExpList el = new ExpList();
        el.addElement((Exp) visit(ctx.exp()));
        for (MiniJavaParser.ExpRestContext er : ctx.expRest())
            el.addElement((Exp) visit(er.exp()));
        return el;
    }

    // ---- Helpers ----
    private VarDeclList buildVarDeclList(java.util.List<MiniJavaParser.VarDeclContext> ctxList) {
        VarDeclList vl = new VarDeclList();
        for (MiniJavaParser.VarDeclContext v : ctxList) vl.addElement((VarDecl) visit(v));
        return vl;
    }

    private MethodDeclList buildMethodDeclList(java.util.List<MiniJavaParser.MethodDeclContext> ctxList) {
        MethodDeclList ml = new MethodDeclList();
        for (MiniJavaParser.MethodDeclContext m : ctxList) ml.addElement((MethodDecl) visit(m));
        return ml;
    }

    private StatementList buildStatementList(java.util.List<MiniJavaParser.StatementContext> ctxList) {
        StatementList sl = new StatementList();
        for (MiniJavaParser.StatementContext s : ctxList) sl.addElement((Statement) visit(s));
        return sl;
    }
}
