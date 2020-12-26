package analyser;

public class SymbolEntry {
    boolean isConstant;
    boolean isInitialized;
    int stackOffset;

    //如果全局变量是常量则const为8，否则为函数名的长度；
    int global_count;

    String global_value;
    //变量类型 0为void（变量应该不会有0，但是Func可能为0）,1为int,2为double
    int variableType;
    /**
     * @param isConstant
     * @param isDeclared
     * @param stackOffset
     */
    public SymbolEntry(boolean isConstant, boolean isDeclared, int stackOffset,int type) {
        this.isConstant = isConstant;
        this.isInitialized = isDeclared;
        this.stackOffset = stackOffset;
        this.variableType = type;
    }

    //这种构造方式供全局变量表使用
    public SymbolEntry(boolean isConstant,int stackOffset, int global_count, String global_value,int type) {
        //true为1，false为0
        this.isConstant = isConstant;
        this.isInitialized = true;
        this.stackOffset = stackOffset;
        //如果是变量或常量则为变量常量类型大小，默认int为8，否则为函数名或String的字符个数
        this.global_count = global_count;
        this.variableType = type;
        //如果为变量或常量则为0,赋值过程在_start中实现，否则为函数名或String的字符全拼
        this.global_value = global_value;
    }

    public int getVariableType() {
        return variableType;
    }

    public void setVariableType(int variableType) {
        this.variableType = variableType;
    }

    public int getGlobal_count() {
        return global_count;
    }

    public void setGlobal_count(int global_count) {
        this.global_count = global_count;
    }

    public String getGlobal_value() {
        return global_value;
    }

    public void setGlobal_value(String global_value) {
        this.global_value = global_value;
    }

    /**
     * @return the stackOffset
     */
    public int getStackOffset() {
        return stackOffset;
    }

    /**
     * @return the isConstant
     */
    public boolean isConstant() {
        return isConstant;
    }

    /**
     * @return the isInitialized
     */
    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * @param isConstant the isConstant to set
     */
    public void setConstant(boolean isConstant) {
        this.isConstant = isConstant;
    }

    /**
     * @param isInitialized the isInitialized to set
     */
    public void setInitialized(boolean isInitialized) {
        this.isInitialized = isInitialized;
    }

    /**
     * @param stackOffset the stackOffset to set
     */
    public void setStackOffset(int stackOffset) {
        this.stackOffset = stackOffset;
    }
}

