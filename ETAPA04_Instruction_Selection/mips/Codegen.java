package mips;

import Assem.Instr;
import Assem.InstrList;
import Assem.OPER;
import Temp.Temp;
import Temp.TempList;
import Temp.Label;
import Temp.LabelList;

public class Codegen {
    MipsFrame frame;
    public Codegen(MipsFrame f) {
        frame = f;
    }

    private InstrList ilist = null;
    private InstrList last = null;

    private void emit(Instr inst) {
        if (last != null) {
            last = last.tail = new InstrList(inst, null);
        } else {
            last = ilist = new InstrList(inst, null);
        }
    }

    public InstrList codegen(Tree.Stm s) {
        InstrList l;
        munchStm(s);
        l = ilist;
        ilist = last = null;
        return l;
    }

    private void munchStm(Tree.Stm s) {
        if (s instanceof Tree.MOVE) {
            munchMove((Tree.MOVE) s);
        } else if (s instanceof Tree.EXPSTM) {
            munchExp(((Tree.EXPSTM) s).exp);
        } else if (s instanceof Tree.JUMP) {
            munchJump((Tree.JUMP) s);
        } else if (s instanceof Tree.CJUMP) {
            munchCJump((Tree.CJUMP) s);
        } else if (s instanceof Tree.LABEL) {
            munchLabel((Tree.LABEL) s);
        } else if (s instanceof Tree.SEQ) {
            munchStm(((Tree.SEQ) s).left);
            munchStm(((Tree.SEQ) s).right);
        } else {
            throw new Error("Codegen.munchStm unhandled: " + s.getClass().getName());
        }
    }

    private void munchMove(Tree.MOVE s) {
        Tree.Exp dst = s.dst;
        Tree.Exp src = s.src;

        if (dst instanceof Tree.MEM) {
            Tree.MEM m = (Tree.MEM) dst;
            if (m.exp instanceof Tree.BINOP && ((Tree.BINOP) m.exp).binop == Tree.BINOP.PLUS && ((Tree.BINOP) m.exp).right instanceof Tree.CONST) {
                // MOVE(MEM(BINOP(PLUS, e1, CONST(c))), e2)
                Tree.Exp e1 = ((Tree.BINOP) m.exp).left;
                int c = ((Tree.CONST) ((Tree.BINOP) m.exp).right).value;
                emit(new OPER("sw `s1, " + c + "(`s0)", null, new TempList(munchExp(e1), new TempList(munchExp(src), null))));
            } else if (m.exp instanceof Tree.BINOP && ((Tree.BINOP) m.exp).binop == Tree.BINOP.PLUS && ((Tree.BINOP) m.exp).left instanceof Tree.CONST) {
                // MOVE(MEM(BINOP(PLUS, CONST(c), e1)), e2)
                Tree.Exp e1 = ((Tree.BINOP) m.exp).right;
                int c = ((Tree.CONST) ((Tree.BINOP) m.exp).left).value;
                emit(new OPER("sw `s1, " + c + "(`s0)", null, new TempList(munchExp(e1), new TempList(munchExp(src), null))));
            } else if (m.exp instanceof Tree.CONST) {
                // MOVE(MEM(CONST(c)), e2)
                int c = ((Tree.CONST) m.exp).value;
                emit(new OPER("sw `s0, " + c + "($zero)", null, new TempList(munchExp(src), null)));
            } else {
                // MOVE(MEM(e1), e2)
                emit(new OPER("sw `s1, 0(`s0)", null, new TempList(munchExp(m.exp), new TempList(munchExp(src), null))));
            }
        } else if (dst instanceof Tree.TEMP) {
            Temp t = ((Tree.TEMP) dst).temp;
            if (src instanceof Tree.CALL) {
                // Function call whose result is placed in t
                Temp r = munchCall((Tree.CALL) src);
                emit(new Assem.MOVE("move `d0, `s0", t, r));
            } else if (src instanceof Tree.MEM && ((Tree.MEM) src).exp instanceof Tree.BINOP && ((Tree.BINOP) ((Tree.MEM) src).exp).binop == Tree.BINOP.PLUS && ((Tree.BINOP) ((Tree.MEM) src).exp).right instanceof Tree.CONST) {
                // MOVE(TEMP(t), MEM(BINOP(PLUS, e1, CONST(c))))
                Tree.Exp e1 = ((Tree.BINOP) ((Tree.MEM) src).exp).left;
                int c = ((Tree.CONST) ((Tree.BINOP) ((Tree.MEM) src).exp).right).value;
                emit(new OPER("lw `d0, " + c + "(`s0)", new TempList(t, null), new TempList(munchExp(e1), null)));
            } else if (src instanceof Tree.MEM && ((Tree.MEM) src).exp instanceof Tree.BINOP && ((Tree.BINOP) ((Tree.MEM) src).exp).binop == Tree.BINOP.PLUS && ((Tree.BINOP) ((Tree.MEM) src).exp).left instanceof Tree.CONST) {
                // MOVE(TEMP(t), MEM(BINOP(PLUS, CONST(c), e1)))
                Tree.Exp e1 = ((Tree.BINOP) ((Tree.MEM) src).exp).right;
                int c = ((Tree.CONST) ((Tree.BINOP) ((Tree.MEM) src).exp).left).value;
                emit(new OPER("lw `d0, " + c + "(`s0)", new TempList(t, null), new TempList(munchExp(e1), null)));
            } else if (src instanceof Tree.BINOP && ((Tree.BINOP) src).binop == Tree.BINOP.PLUS && ((Tree.BINOP) src).right instanceof Tree.CONST) {
                // MOVE(TEMP(t), BINOP(PLUS, e1, CONST(c)))
                Tree.Exp e1 = ((Tree.BINOP) src).left;
                int c = ((Tree.CONST) ((Tree.BINOP) src).right).value;
                emit(new OPER("addi `d0, `s0, " + c, new TempList(t, null), new TempList(munchExp(e1), null)));
            } else if (src instanceof Tree.BINOP && ((Tree.BINOP) src).binop == Tree.BINOP.PLUS && ((Tree.BINOP) src).left instanceof Tree.CONST) {
                // MOVE(TEMP(t), BINOP(PLUS, CONST(c), e1))
                Tree.Exp e1 = ((Tree.BINOP) src).right;
                int c = ((Tree.CONST) ((Tree.BINOP) src).left).value;
                emit(new OPER("addi `d0, `s0, " + c, new TempList(t, null), new TempList(munchExp(e1), null)));
            } else if (src instanceof Tree.CONST) {
                // MOVE(TEMP(t), CONST(c))
                emit(new OPER("li `d0, " + ((Tree.CONST) src).value, new TempList(t, null), null));
            } else {
                // MOVE(TEMP(t), e)
                emit(new Assem.MOVE("move `d0, `s0", t, munchExp(src)));
            }
        } else {
            throw new Error("Codegen.munchMove unhandled dst: " + dst.getClass().getName());
        }
    }

    private void munchJump(Tree.JUMP j) {
        if (j.exp instanceof Tree.NAME) {
            Label l = ((Tree.NAME) j.exp).label;
            emit(new OPER("j `j0", null, null, j.targets));
        } else {
            emit(new OPER("jr `s0", null, new TempList(munchExp(j.exp), null), j.targets));
        }
    }

    private void munchCJump(Tree.CJUMP cj) {
        String op = "";
        switch (cj.relop) {
            case Tree.CJUMP.EQ: op = "beq"; break;
            case Tree.CJUMP.NE: op = "bne"; break;
            case Tree.CJUMP.LT: op = "blt"; break;
            case Tree.CJUMP.GT: op = "bgt"; break;
            case Tree.CJUMP.LE: op = "ble"; break;
            case Tree.CJUMP.GE: op = "bge"; break;
            case Tree.CJUMP.ULT: op = "bltu"; break;
            case Tree.CJUMP.ULE: op = "bleu"; break;
            case Tree.CJUMP.UGT: op = "bgtu"; break;
            case Tree.CJUMP.UGE: op = "bgeu"; break;
        }
        Temp left = munchExp(cj.left);
        Temp right = munchExp(cj.right);
        emit(new OPER(op + " `s0, `s1, `j0", null, new TempList(left, new TempList(right, null)), new LabelList(cj.iftrue, new LabelList(cj.iffalse, null))));
    }

    private void munchLabel(Tree.LABEL l) {
        emit(new Assem.LABEL(l.label.toString() + ":", l.label));
    }

    private Temp munchExp(Tree.Exp e) {
        if (e instanceof Tree.MEM) {
            return munchMem((Tree.MEM) e);
        } else if (e instanceof Tree.BINOP) {
            return munchBinop((Tree.BINOP) e);
        } else if (e instanceof Tree.CONST) {
            return munchConst((Tree.CONST) e);
        } else if (e instanceof Tree.TEMP) {
            return ((Tree.TEMP) e).temp;
        } else if (e instanceof Tree.NAME) {
            Temp r = new Temp();
            emit(new OPER("la `d0, " + ((Tree.NAME) e).label.toString(), new TempList(r, null), null));
            return r;
        } else if (e instanceof Tree.CALL) {
            return munchCall((Tree.CALL) e);
        } else {
            throw new Error("Codegen.munchExp unhandled: " + e.getClass().getName());
        }
    }

    private Temp munchMem(Tree.MEM m) {
        Temp r = new Temp();
        if (m.exp instanceof Tree.BINOP && ((Tree.BINOP) m.exp).binop == Tree.BINOP.PLUS && ((Tree.BINOP) m.exp).right instanceof Tree.CONST) {
            Tree.Exp e1 = ((Tree.BINOP) m.exp).left;
            int c = ((Tree.CONST) ((Tree.BINOP) m.exp).right).value;
            emit(new OPER("lw `d0, " + c + "(`s0)", new TempList(r, null), new TempList(munchExp(e1), null)));
        } else if (m.exp instanceof Tree.BINOP && ((Tree.BINOP) m.exp).binop == Tree.BINOP.PLUS && ((Tree.BINOP) m.exp).left instanceof Tree.CONST) {
            Tree.Exp e1 = ((Tree.BINOP) m.exp).right;
            int c = ((Tree.CONST) ((Tree.BINOP) m.exp).left).value;
            emit(new OPER("lw `d0, " + c + "(`s0)", new TempList(r, null), new TempList(munchExp(e1), null)));
        } else if (m.exp instanceof Tree.CONST) {
            int c = ((Tree.CONST) m.exp).value;
            emit(new OPER("lw `d0, " + c + "($zero)", new TempList(r, null), null));
        } else {
            emit(new OPER("lw `d0, 0(`s0)", new TempList(r, null), new TempList(munchExp(m.exp), null)));
        }
        return r;
    }

    private Temp munchBinop(Tree.BINOP b) {
        Temp r = new Temp();
        String op = "";
        switch (b.binop) {
            case Tree.BINOP.PLUS: op = "add"; break;
            case Tree.BINOP.MINUS: op = "sub"; break;
            case Tree.BINOP.MUL: op = "mul"; break;
            case Tree.BINOP.DIV: op = "div"; break;
            case Tree.BINOP.AND: op = "and"; break;
            case Tree.BINOP.OR: op = "or"; break;
            case Tree.BINOP.XOR: op = "xor"; break;
            case Tree.BINOP.LSHIFT: op = "sll"; break;
            case Tree.BINOP.RSHIFT: op = "srl"; break;
            case Tree.BINOP.ARSHIFT: op = "sra"; break;
        }

        if (b.binop == Tree.BINOP.PLUS && b.right instanceof Tree.CONST) {
            emit(new OPER("addi `d0, `s0, " + ((Tree.CONST) b.right).value, new TempList(r, null), new TempList(munchExp(b.left), null)));
        } else if (b.binop == Tree.BINOP.PLUS && b.left instanceof Tree.CONST) {
            emit(new OPER("addi `d0, `s0, " + ((Tree.CONST) b.left).value, new TempList(r, null), new TempList(munchExp(b.right), null)));
        } else if (b.binop == Tree.BINOP.MINUS && b.right instanceof Tree.CONST) {
            emit(new OPER("addi `d0, `s0, " + (-((Tree.CONST) b.right).value), new TempList(r, null), new TempList(munchExp(b.left), null)));
        } else {
            emit(new OPER(op + " `d0, `s0, `s1", new TempList(r, null), new TempList(munchExp(b.left), new TempList(munchExp(b.right), null))));
        }
        return r;
    }

    private Temp munchConst(Tree.CONST c) {
        Temp r = new Temp();
        emit(new OPER("li `d0, " + c.value, new TempList(r, null), null));
        return r;
    }

    private Temp munchCall(Tree.CALL c) {
        TempList args = munchArgs(0, c.args);
        Temp r = new Temp();
        Temp retReg = frame.RV();
        
        TempList calleeDefs = new TempList(retReg, null);

        if (c.func instanceof Tree.NAME) {
            emit(new OPER("jal " + ((Tree.NAME) c.func).label.toString(), calleeDefs, args));
        } else {
            Temp func = munchExp(c.func);
            args = new TempList(func, args);
            emit(new OPER("jalr `s0", calleeDefs, args));
        }

        emit(new Assem.MOVE("move `d0, `s0", r, retReg));
        return r;
    }

    private TempList munchArgs(int i, Tree.ExpList args) {
        if (args == null) return null;
        Temp t = munchExp(args.head);
        return new TempList(t, munchArgs(i + 1, args.tail));
    }
}
