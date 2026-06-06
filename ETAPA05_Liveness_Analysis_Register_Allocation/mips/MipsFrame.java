package mips;

public class MipsFrame extends frame.Frame implements Temp.TempMap {
    private int offset = 0;
    public static final int WORD_SIZE = 4;

    private final Temp.Temp fp = new Temp.Temp();
    private final Temp.Temp rv = new Temp.Temp();
    private static final Temp.Temp RA = new Temp.Temp();
    private static final Temp.Temp ZERO = new Temp.Temp();
    private static final Temp.Temp SP = new Temp.Temp();
    private static final Temp.Temp EXTRA_CALLEE1 = new Temp.Temp();
    private static final Temp.Temp EXTRA_CALLEE2 = new Temp.Temp();
    private static final Temp.Temp EXTRA_CALLEE3 = new Temp.Temp();
    private static final Temp.Temp EXTRA_CALLEE4 = new Temp.Temp();
    private static final Temp.Temp EXTRA_CALLEE5 = new Temp.Temp();
    
    private static final Temp.Temp[] CALLEE_SAVES = new Temp.Temp[] {
        new Temp.Temp(), new Temp.Temp(), new Temp.Temp(), new Temp.Temp(), 
        new Temp.Temp(), new Temp.Temp(), new Temp.Temp(), new Temp.Temp()
    };
    
    private static final Temp.Temp[] CALLER_SAVES = new Temp.Temp[] {
        new Temp.Temp(), new Temp.Temp(), new Temp.Temp(), new Temp.Temp(), // a0-a3
        new Temp.Temp(), new Temp.Temp(), new Temp.Temp(), new Temp.Temp(), // t0-t3
        new Temp.Temp(), new Temp.Temp(), new Temp.Temp(), new Temp.Temp()  // t4-t7 (removed t8-t9)
    };

    public Temp.TempList callerSaves() {
        Temp.TempList list = new Temp.TempList(RA, null); // jal clobbers $ra
        for (int i = CALLER_SAVES.length - 1; i >= 0; i--) {
            list = new Temp.TempList(CALLER_SAVES[i], list);
        }
        return list;
    }

    public Temp.TempList registers() {
        Temp.TempList list = new Temp.TempList(rv, null);
        list = new Temp.TempList(EXTRA_CALLEE1, list);
        list = new Temp.TempList(EXTRA_CALLEE2, list);
        list = new Temp.TempList(EXTRA_CALLEE3, list);
        list = new Temp.TempList(EXTRA_CALLEE4, list);
        list = new Temp.TempList(EXTRA_CALLEE5, list);
        for (int i = CALLEE_SAVES.length - 1; i >= 0; i--) {
            list = new Temp.TempList(CALLEE_SAVES[i], list);
        }
        for (int i = CALLER_SAVES.length - 1; i >= 0; i--) {
            list = new Temp.TempList(CALLER_SAVES[i], list);
        }
        return list;
    }

    public String tempMap(Temp.Temp t) {
        if (t == fp) return "$fp";
        if (t == rv) return "$v0";
        if (t == RA) return "$ra";
        if (t == SP) return "$sp";
        if (t == ZERO) return "$zero";
        if (t == EXTRA_CALLEE1) return "$v1";
        if (t == EXTRA_CALLEE2) return "$t8";
        if (t == EXTRA_CALLEE3) return "$t9";
        if (t == EXTRA_CALLEE4) return "$k0";
        if (t == EXTRA_CALLEE5) return "$k1";
        for (int i = 0; i < CALLEE_SAVES.length; i++) {
            if (t == CALLEE_SAVES[i]) return "$s" + i;
        }
        for (int i = 0; i < CALLER_SAVES.length; i++) {
            if (t == CALLER_SAVES[i]) {
                if (i < 4) return "$a" + i;
                if (i < 12) return "$t" + (i - 4);
                return "$gp"; // fallback
            }
        }
        return null; // ou t.toString()
    }

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
