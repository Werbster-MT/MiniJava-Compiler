package mips;

public class MipsFrame extends frame.Frame {
    private int offset = 0;
    public static final int WORD_SIZE = 4;

    private final Temp.Temp fp = new Temp.Temp();
    private final Temp.Temp rv = new Temp.Temp();
    private static final Temp.Temp RA = new Temp.Temp();
    private static final Temp.Temp[] CALLEE_SAVES = new Temp.Temp[] {
        new Temp.Temp(), new Temp.Temp(), new Temp.Temp(), new Temp.Temp(), 
        new Temp.Temp(), new Temp.Temp(), new Temp.Temp(), new Temp.Temp()
    };

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
        Temp.Temp tRA = new Temp.Temp();
        Tree.Stm saveRA = new Tree.MOVE(new Tree.TEMP(tRA), new Tree.TEMP(RA));
        Tree.Stm restoreRA = new Tree.MOVE(new Tree.TEMP(RA), new Tree.TEMP(tRA));

        Temp.Temp[] tSaves = new Temp.Temp[CALLEE_SAVES.length];
        Tree.Stm saveSaves = null;
        Tree.Stm restoreSaves = null;

        for (int i = 0; i < CALLEE_SAVES.length; i++) {
            tSaves[i] = new Temp.Temp();
            Tree.Stm save = new Tree.MOVE(new Tree.TEMP(tSaves[i]), new Tree.TEMP(CALLEE_SAVES[i]));
            Tree.Stm restore = new Tree.MOVE(new Tree.TEMP(CALLEE_SAVES[i]), new Tree.TEMP(tSaves[i]));
            saveSaves = saveSaves == null ? save : new Tree.SEQ(saveSaves, save);
            restoreSaves = restoreSaves == null ? restore : new Tree.SEQ(restoreSaves, restore);
        }

        Tree.Stm entry = new Tree.SEQ(saveRA, saveSaves);
        Tree.Stm exit = new Tree.SEQ(restoreSaves, restoreRA);

        return new Tree.SEQ(
            entry,
            new Tree.SEQ(
                body,
                exit
            )
        );
    }
}
