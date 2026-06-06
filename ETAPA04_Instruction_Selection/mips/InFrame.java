package mips;

class InFrame extends frame.Access {
    private final int offset;

    InFrame(int o) {
        offset = o;
    }

    @Override
    public Tree.Exp exp(Tree.Exp framePtr) {
        return new Tree.MEM(
            new Tree.BINOP(Tree.BINOP.PLUS, framePtr, new Tree.CONST(offset))
        );
    }
}
