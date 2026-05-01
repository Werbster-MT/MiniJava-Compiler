package mips;

public class MipsFrame extends frame.Frame {
    private int offset = 0;
    public static final int WORD_SIZE = 4;

    private final Temp.Temp fp = new Temp.Temp();
    private final Temp.Temp rv = new Temp.Temp();

    public MipsFrame(Temp.Label name, Util.BoolList formalEscapes) {
        this.name = name;
        for (Util.BoolList it = formalEscapes; it != null; it = it.tail) {
            formals.add(allocFormal(it.head));
        }
    }

    private frame.Access allocFormal(boolean escape) {
        if (escape) {
            offset -= WORD_SIZE;
            return new InFrame(offset);
        }
        return new InReg(new Temp.Temp());
    }

    @Override
    public frame.Access allocLocal(boolean escape) {
        if (escape) {
            offset -= WORD_SIZE;
            return new InFrame(offset);
        }
        return new InReg(new Temp.Temp());
    }

    @Override
    public Tree.Exp exp(frame.Access acc, Tree.Exp fpExp) {
        return acc.exp(fpExp);
    }

    @Override
    public Tree.Exp externalCall(String func, Tree.ExpList args) {
        return new Tree.CALL(new Tree.NAME(new Temp.Label(func)), args);
    }

    @Override
    public Temp.Temp FP() {
        return fp;
    }

    @Override
    public Temp.Temp RV() {
        return rv;
    }

    @Override
    public int wordSize() {
        return WORD_SIZE;
    }

    @Override
    public Tree.Stm procEntryExit1(Tree.Stm body) {
        return body;
    }
}
