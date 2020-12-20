package instruction;

public enum Operation {
    nop, push, pop, popn, dup, loca, arga, globa, load_8, load_16,
    load_32, load_64, store_8, store_16, store_32, store_64, alloc,
    free, stackalloc, add_i, sub_i, mul_i, div_i, add_f, sub_f, mul_f,
    div_f, div_u, shl, shr, and, or, xor, not, cmp_i, cmp_u, cmp_f, neg_i,
    neg_f, itof, ftoi, shrl, set_lt, set_gt, br, br_false, br_true, call,
    ret, callname, scan_i, scan_c, scan_f, print_i, print_c, print_f,
    print_s, println, panic;

    public int getNum() {
        switch (this) {
            case nop:
                return 0x00;
            case push:
                return 0x01;
            case pop:
                return 0x02;
            case popn:
                return 0x03;
            case dup:
                return 0x04;
            case loca:
                return 0x0a;
            case arga:
                return 0x0b;
            case globa:
                return 0x0c;
            case load_8:
                return 0x10;
            case load_16:
                return 0x11;
            case load_32:
                return 0x12;
            case load_64:
                return 0x13;
            case store_8:
                return 0x14;
            case store_16:
                return 0x15;
            case store_32:
                return 0x16;
            case store_64:
                return 0x17;
            case alloc:
                return 0x18;
            case free:
                return 0x19;
            case stackalloc:
                return 0x1a;
            case add_i:
                return 0x20;
            case sub_i:
                return 0x21;
            case mul_i:
                return 0x22;
            case div_i:
                return 0x23;
            case add_f:
                return 0x24;
            case sub_f:
                return 0x25;
            case mul_f:
                return 0x26;
            case div_f:
                return 0x27;
            case div_u:
                return 0x28;
            case shl:
                return 0x29;
            case shr:
                return 0x2a;
            case and:
                return 0x2b;
            case or:
                return 0x2c;
            case xor:
                return 0x2d;
            case not:
                return 0x2e;
            case cmp_i:
                return 0x30;
            case cmp_u:
                return 0x31;
            case cmp_f:
                return 0x32;
            case neg_i:
                return 0x34;
            case neg_f:
                return 0x35;
            case itof:
                return 0x36;
            case ftoi:
                return 0x37;
            case shrl:
                return 0x38;
            case set_lt:
                return 0x39;
            case set_gt:
                return 0x3a;
            case br:
                return 0x41;
            case br_false:
                return 0x42;
            case br_true:
                return 0x43;
            case call:
                return 0x48;
            case ret:
                return 0x49;
            case callname:
                return 0x4a;
            case scan_i:
                return 0x50;
            case scan_c:
                return 0x51;
            case scan_f:
                return 0x52;
            case print_i:
                return 0x54;
            case print_c:
                return 0x55;
            case print_f:
                return 0x56;
            case print_s:
                return 0x57;
            case println:
                return 0x58;
            case panic:
            default:
                return 0xfe;
        }
    }
}

