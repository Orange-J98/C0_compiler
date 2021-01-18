package miniplc0java.analyser;

import java.util.ArrayList;
import java.util.HashMap;

import miniplc0java.instruction.Instruction;

public class FuncEntry {
    /** func_name即为函数在全局变量表中的偏移量 */
    private int func_name;
    private int haveRet;
    private int returnType;
    private int param_num;
    private int locVarNum;
    private int bodyCnt;
    private int funcOffset;
    HashMap<String,SymbolEntry> paramSymbolEntry;
    ArrayList<Instruction> instructions;

    public FuncEntry(int func_name, int haveRet, int returnType, int param_num, int locVarNum, int bodyCnt, ArrayList<Instruction> instructions, int funcOffset, HashMap<String,SymbolEntry> paramSymbolEntry) {
        this.func_name = func_name;
        this.haveRet = haveRet;
        this.returnType = returnType;
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

    public int getHaveRet() {
        return haveRet;
    }

    public void setHaveRet(int haveRet) {
        this.haveRet = haveRet;
    }

    public int getReturnType() {
        return returnType;
    }

    public void setReturnType(int returnType) {
        this.returnType = returnType;
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

