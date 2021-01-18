package miniplc0java.analyser;

import java.util.ArrayList;
import java.util.HashMap;

import miniplc0java.instruction.Instruction;

public class FuncEntry {
    /** func_name即为函数在全局变量表中的偏移量 */
    int func_name;
    int ret_num;
    int retType;
    int param_num;
    int locVarNum;
    int bodyCnt;
    int funcOffset;
    HashMap<String,SymbolEntry> paramSymbolEntry;
    ArrayList<Instruction> instructions;

    public FuncEntry(int func_name, int ret_num, int retType,int param_num, int locVarNum, int bodyCnt, ArrayList<Instruction> instructions, int funcOffset, HashMap<String,SymbolEntry> paramSymbolEntry) {
        this.func_name = func_name;
        this.ret_num = ret_num;
        this.retType = retType;
        this.param_num = param_num;
        this.locVarNum = locVarNum;
        this.bodyCnt = bodyCnt;
        this.instructions = instructions;
        this.funcOffset = funcOffset;
        this.paramSymbolEntry = paramSymbolEntry;
    }

    public HashMap<String, SymbolEntry> getParamSymbolEntry() {
        return paramSymbolEntry;
    }

    public int getRetType() {
        return retType;
    }

    public void setRetType(int retType) {
        this.retType = retType;
    }

    public void setParamSymbolEntry(HashMap<String, SymbolEntry> paramSymbolEntry) {
        this.paramSymbolEntry = paramSymbolEntry;
    }
    public int getFuncOffset() {
        return funcOffset;
    }

    public void setFuncOffset(int funcOffset) {
        this.funcOffset = funcOffset;
    }

    public int getFunc_name() {
        return func_name;
    }

    public void setFunc_name(int func_name) {
        this.func_name = func_name;
    }

    public int getRet_num() {
        return ret_num;
    }

    public void setRet_num(int ret_num) {
        this.ret_num = ret_num;
    }

    public int getParam_num() {
        return param_num;
    }

    public void setParam_num(int param_num) {
        this.param_num = param_num;
    }

    public int getLocVarNum() {
        return locVarNum;
    }

    public void setLocVarNum(int locVarNum) {
        this.locVarNum = locVarNum;
    }

    public int getBodyCnt() {
        return bodyCnt;
    }

    public void setBodyCnt(int bodyCnt) {
        this.bodyCnt = bodyCnt;
    }

    public ArrayList<Instruction> getInstructions() {
        return instructions;
    }

    public void setInstructions(ArrayList<Instruction> instructions) {
        this.instructions = instructions;
    }
}

