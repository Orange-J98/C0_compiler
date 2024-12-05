package instruction;
import error.CompileError;

import java.util.Objects;

public class Instruction {
    private Operation opt;
    private int OptNum;
    Integer x;

    public Instruction(Operation opt) throws CompileError {
        this.opt = opt;
        this.OptNum = opt.getNum();
        this.x = -1;
    }

    public int getOptNum() {
        return OptNum;
    }

    public void setOptNum(int optNum) {
        OptNum = optNum;
    }

    public Instruction(Operation opt, Integer x) throws CompileError {
        this.opt = opt;
        this.OptNum = opt.getNum();
        this.x = x;
    }



    public Instruction() throws CompileError {
        this.opt = Operation.load_64;
        this.OptNum = this.opt.getNum();
        this.x = -1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Instruction that = (Instruction) o;
        return opt == that.opt && Objects.equals(x, that.x);
    }

    @Override
    public int hashCode() {
        return Objects.hash(opt, x);
    }

    public Operation getOpt() {
        return opt;
    }

    public void setOpt(Operation opt) {
        this.opt = opt;
    }

    public Integer getX() {
        return x;
    }

    public void setX(Integer x) {
        this.x = x;
    }


    public String toNum() throws CompileError {
        return Integer.toString(this.opt.getNum())+Integer.toString(this.x);
    }
    @Override
    public String toString() {
        switch (this.opt) {
            case nop:
            case pop:
            case dup:
            case load_8:
            case load_16:
            case load_32:
            case load_64:
            case store_8:
            case store_16:
            case store_32:
            case store_64:
            case alloc:
            case free:
            case add_i:
            case add_f:
            case sub_i:
            case mul_i:
            case sub_f:
            case mul_f:
            case div_i:
            case div_f:
            case div_u:
            case shl:
            case shr:
            case and:
            case or:
            case xor:
            case not:
            case cmp_i:
            case cmp_u:
            case cmp_f:
            case neg_i:
            case neg_f:
            case itof:
            case ftoi:
            case shrl:
            case set_lt:
            case set_gt:
            case ret:
            case scan_i:
            case scan_c:
            case scan_f:
            case print_i:
            case print_c:
            case print_f:
            case print_s:
            case println:
            case panic:
                return String.format("%s", this.opt.toString().toUpperCase());
            case push:
            case popn:
            case loca:
            case arga:
            case globa:
            case stackalloc:
            case br:
            case br_false:
            case br_true:
            case call:
            case callname:
                return String.format("%s %s", this.opt.toString().toUpperCase(), this.x);
            default:
                return "panic";
        }
    }
}
