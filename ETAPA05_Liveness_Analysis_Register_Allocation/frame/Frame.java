package frame;

import java.util.ArrayList;
import java.util.List;

public abstract class Frame {
    public Temp.Label name;
    public List<Access> formals = new ArrayList<>();

    public abstract Access allocLocal(boolean escape);
    public abstract Tree.Exp exp(Access acc, Tree.Exp fp);
    public abstract Tree.Stm procEntryExit1(Tree.Stm body);
    public abstract Tree.Exp externalCall(String func, Tree.ExpList args);
    public abstract Temp.Temp FP();
    public abstract Temp.Temp RV();
    public abstract int wordSize();
    public abstract Temp.TempList registers();
}
