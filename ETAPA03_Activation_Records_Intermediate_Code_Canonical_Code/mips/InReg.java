package mips;

class InReg extends frame.Access {
    private final Temp.Temp temp;

    InReg(Temp.Temp t) {
        temp = t;
    }

    @Override
    public Tree.Exp exp(Tree.Exp framePtr) {
        return new Tree.TEMP(temp);
    }
}
