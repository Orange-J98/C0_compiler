package analyser;

import error.AnalyzeError;
import error.CompileError;
import error.ErrorCode;
import error.ExpectedTokenError;
import error.TokenizeError;
import instruction.Instruction;
import instruction.Operation;
import tokenizer.Token;
import tokenizer.TokenType;
import tokenizer.Tokenizer;
import util.Pos;

import java.util.*;

public final class Analyser {

    Tokenizer tokenizer;
    /** 指令集 */
    ArrayList<Instruction> instructions;
    /** 当前偷看的 token */
    Token peekedToken = null;

    /** 局部变量表 */
    HashMap<String, SymbolEntry> localSymbolTable = new HashMap<>();
    /** 参数表 */
    HashMap<String,SymbolEntry> paramTable = new HashMap<>();
    /** 全局变量表 */
    HashMap<String,SymbolEntry> globalSymbolTable = new HashMap<>();
    /** 函数表 */
    HashMap<String,FuncEntry> funcTable = new HashMap<>();

    /** 下一个变量的栈偏移 */
    int nextOffset = 0;
    /** 下一个局部变量的栈偏移 */
    int nextLocOff = 0;
    /** 下一个函数的栈偏移 */
    int nextFuncOff = 0;
    /** 下一个参数变量的栈偏移 */
    int nextParamOff = 0;

    /** 判断当前状态实在函数体中还是函数体外 */
    boolean isInFunc = false;

    public Analyser(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
        this.instructions = new ArrayList<>();
    }

    public List<Instruction> analyse() throws CompileError {
        analyseProgram();
        /*TODO:需要重构返回值*/
        return instructions;
    }

    /**
     * 查看下一个 Token
     *
     * @return
     * @throws TokenizeError
     */
    private Token peek() throws TokenizeError {
        if (peekedToken == null) {
            peekedToken = tokenizer.nextToken();
        }
        return peekedToken;
    }

    /**
     * 获取下一个 Token
     *
     * @return
     * @throws TokenizeError
     */
    private Token next() throws TokenizeError {
        if (peekedToken != null) {
            var token = peekedToken;
            peekedToken = null;
            return token;
        } else {
            return tokenizer.nextToken();
        }
    }

    /**
     * 如果下一个 token 的类型是 tt，则返回 true
     *
     * @param tt
     * @return
     * @throws TokenizeError
     */
    private boolean check(TokenType tt) throws TokenizeError {
        var token = peek();
        return token.getTokenType() == tt;
    }

    /**
     * 如果下一个 token 的类型是 tt，则前进一个 token 并返回这个 token
     *
     * @param tt 类型
     * @return 如果匹配则返回这个 token，否则返回 null
     * @throws TokenizeError
     */
    private Token nextIf(TokenType tt) throws TokenizeError {
        var token = peek();
        if (token.getTokenType() == tt) {
            return next();
        } else {
            return null;
        }
    }

    /**
     * 如果下一个 token 的类型是 tt，则前进一个 token 并返回，否则抛出异常
     *
     * @param tt 类型
     * @return 这个 token
     * @throws CompileError 如果类型不匹配
     */
    private Token expect(TokenType tt) throws CompileError {
        var token = peek();
        if (token.getTokenType() == tt) {
            return next();
        } else {
            throw new ExpectedTokenError(tt, token);
        }
    }

    /**
     * 获取下一个变量的栈偏移
     *
     * @return
     */
    private int getNextVariableOffset() {
        return this.nextOffset++;
    }
    private int getNextLocOff(){
        return this.nextLocOff++;
    }
    private int getNextFuncOff(){
        return this.nextFuncOff++;
    }
    private int getNextParamOff(){
        return this.nextParamOff++;
    }
    /**
     * 添加一个符号
     *
     * @param name          名字
     * @param isInitialized 是否已赋值
     * @param isConstant    是否是常量
     * @param curPos        当前 token 的位置（报错用）
     * @throws AnalyzeError 如果重复定义了则抛异常
     */
    private void addLocalSymbol(String name, boolean isInitialized, boolean isConstant, Pos curPos) throws AnalyzeError {
        if (this.localSymbolTable.get(name) != null) {
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, curPos);
        } else {
            this.localSymbolTable.put(name, new SymbolEntry(isConstant, isInitialized, getNextVariableOffset()));
        }
    }
    private void addFuncSymbol(String name, int func_name,int ret_num,int param_num, int locVarNum, int bodyCnt, ArrayList<Instruction> instructions,Pos curPos)throws AnalyzeError{
        if (this.funcTable.get(name)!=null){
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration,curPos);
        }else {
            this.funcTable.put(name,new FuncEntry(func_name,ret_num,param_num,locVarNum,bodyCnt,instructions));
        }

    }

    /**
     * 设置符号为已赋值
     *
     * @param name   符号名称
     * @param curPos 当前位置（报错用）
     * @throws AnalyzeError 如果未定义则抛异常
     */
    private void initializeLocalSymbol(String name, Pos curPos) throws AnalyzeError {
        var entry = this.localSymbolTable.get(name);
        if (entry == null) {
            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
        } else {
            entry.setInitialized(true);
        }
    }

    /**
     * 获取变量在栈上的偏移
     *
     * @param name   符号名
     * @param curPos 当前位置（报错用）
     * @return 栈偏移
     * @throws AnalyzeError
     */
    private int getOffset(String name, Pos curPos) throws AnalyzeError {
        var entry = this.localSymbolTable.get(name);
        if (entry == null) {
            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
        } else {
            return entry.getStackOffset();
        }
    }

    /**
     * 获取变量是否是常量
     *
     * @param name   符号名
     * @param curPos 当前位置（报错用）
     * @return 是否为常量
     * @throws AnalyzeError
     */
    private boolean isConstant(String name, Pos curPos) throws AnalyzeError {
        var entry = this.localSymbolTable.get(name);
        if (entry == null) {
            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
        } else {
            return entry.isConstant();
        }
    }

    /**
     * 语法分析从这里开始
     * <程序> ::= 'begin'<主过程>'end'
     */


    private void analyseProgram() throws CompileError {
        // 程序
        //item -> function | decl_stmt
        //program -> item*
        while(!check(TokenType.EOF)){
            analyseItem();
        }
        expect(TokenType.EOF);
    }

    // 程序
    //item -> function | decl_stmt
    //program -> item*
    private void analyseItem() throws CompileError {
        if(nextIf(TokenType.FN_KW)!=null){
            //# 函数
            //function_param -> 'const'? IDENT ':' ty
            //function_param_list -> function_param (',' function_param)*
            //function -> 'fn' IDENT '(' function_param_list? ')' '->' ty block_stmt
            expect(TokenType.IDENT);
            expect(TokenType.L_PAREN);
            if(check(TokenType.CONST_KW)||check(TokenType.IDENT)){
                analyseFunctionParamList();
            }
            expect(TokenType.R_PAREN);
            expect(TokenType.ARROW);
            analyseTy();
            analyseBlockStmt();
        }else if(check(TokenType.LET_KW)||check(TokenType.CONST_KW)){
            //decl_stmt -> let_decl_stmt | const_decl_stmt
            //  let_decl_stmt -> 'let' IDENT ':' ty ('=' expr)? ';'
            //  const_decl_stmt -> 'const' IDENT ':' ty '=' expr ';'
            if (check(TokenType.LET_KW)){
                analyseLetDeclStmt();
            }else if (check(TokenType.CONST_KW)){
                analyseConstDeclStmt();
            }
        }else{
            throw new AnalyzeError(ErrorCode.InvalidInput,next().getStartPos());
        }
    }

    //# 函数
    //function_param -> 'const'? IDENT ':' ty
    //function_param_list -> function_param (',' function_param)*
    private void analyseFunctionParamList() throws CompileError{
        analyseFunctionParam();
        while(nextIf(TokenType.COMMA)!=null){
            analyseFunctionParam();
        }
    }
    //function_param -> 'const'? IDENT ':' ty
    private void analyseFunctionParam() throws CompileError{
        if (check(TokenType.CONST_KW)){
            next();
        }
        expect(TokenType.IDENT);
        expect(TokenType.COLON);
        analyseTy();
    }
    // ## 类型
    //ty -> IDENT
    private void analyseTy() throws CompileError{
        //c0 有一个十分简单的类型系统。在基础 C0 中你会用到的类型有两种：
        //
        //64 位有符号整数 int
        //空类型 void
        //扩展 C0 增加了一种类型：
        //
        //64 位 IEEE-754 浮点数 double
        if(Objects.requireNonNull(nextIf(TokenType.IDENT)).getValue().equals("void")|| Objects.requireNonNull(nextIf(TokenType.IDENT)).getValue().equals("int")||Objects.requireNonNull(nextIf(TokenType.IDENT)).getValue().equals("double")){
            return;
        }else{
            throw new AnalyzeError(ErrorCode.InvalidInput,next().getStartPos());
        }
    }
    // # 语句
//    stmt ->
//    expr_stmt
//    | decl_stmt
//    | if_stmt
//    | while_stmt
//    | break_stmt
//    | continue_stmt
//    | return_stmt
//    | block_stmt
//    | empty_stmt
    private void analyseStmt() throws CompileError{
        if(checkNextifExpr()){
            //    expr_stmt -> expr ';'
            analyseExpr();
            expect(TokenType.SEMICOLON);
        }else if (check(TokenType.LET_KW)||check(TokenType.CONST_KW)){
            //    decl_stmt -> let_decl_stmt | const_decl_stmt
            //        let_decl_stmt -> 'let' IDENT ':' ty ('=' expr)? ';'
            //        const_decl_stmt -> 'const' IDENT ':' ty '=' expr ';'
            analyseDeclStmt();
        }else if (check(TokenType.IF_KW)){
            //    if_stmt -> 'if' expr block_stmt ('else' 'if' expr block_stmt)* ('else' block_stmt)?
            analyseIfStmt();
        }else if(check(TokenType.WHILE_KW)){
            //    while_stmt -> 'while' expr block_stmt
            analyseWhile();
        }else if(check(TokenType.BREAK_KW)){
            //    break_stmt -> 'break' ';'
            analyseBreakStmt();
        }else if(check(TokenType.CONTINUE_KW)){
            //    continue_stmt -> 'continue' ';'
            analyseContinueStmt();
        }else if (check(TokenType.RETURN_KW)){
            //    return_stmt -> 'return' expr? ';'
            analyseReturnStmt();
        }else if (check(TokenType.L_BRACE)){
            //    block_stmt -> '{' stmt* '}'
            analyseBlockStmt();
        }else if(nextIf(TokenType.SEMICOLON)!=null){
            //    empty_stmt -> ';'
            return;
        }else{
            throw new AnalyzeError(ErrorCode.InvalidInput,next().getStartPos());
        }
    }

    //    if_stmt -> 'if' expr block_stmt ('else' 'if' expr block_stmt)* ('else' block_stmt)?
    private void analyseIfStmt() throws CompileError{
        expect(TokenType.IF_KW);
        analyseExpr();
        analyseBlockStmt();
        while (check(TokenType.ELSE_KW)){
            next();
            if (check(TokenType.IF_KW)){
                next();
                analyseExpr();
                analyseBlockStmt();
            }else{
                analyseBlockStmt();
                break;
            }
        }

    }
    //    while_stmt -> 'while' expr block_stmt
    private void analyseWhile() throws CompileError{
        expect(TokenType.WHILE_KW);
        analyseExpr();
        analyseBlockStmt();
    }
    //    break_stmt -> 'break' ';'
    private void analyseBreakStmt() throws CompileError{
        expect(TokenType.BREAK_KW);
        expect(TokenType.SEMICOLON);
    }
    //    continue_stmt -> 'continue' ';'
    private void analyseContinueStmt() throws CompileError{
        expect(TokenType.CONTINUE_KW);
        expect(TokenType.SEMICOLON);
    }
    //    return_stmt -> 'return' expr? ';'
    private void analyseReturnStmt() throws CompileError{
        expect(TokenType.RETURN_KW);
        if (checkNextifExpr()){
            analyseExpr();
        }
        expect(TokenType.SEMICOLON);
    }

    //    decl_stmt -> let_decl_stmt | const_decl_stmt
    //        let_decl_stmt -> 'let' IDENT ':' ty ('=' expr)? ';'
    //        const_decl_stmt -> 'const' IDENT ':' ty '=' expr ';'
    private void analyseDeclStmt() throws CompileError{
        if (check(TokenType.LET_KW)){
            analyseLetDeclStmt();
        }else if(check(TokenType.CONST_KW)) {
            analyseConstDeclStmt();
        }else{
            throw new AnalyzeError(ErrorCode.InvalidInput,next().getStartPos());
        }
    }
    //        let_decl_stmt -> 'let' IDENT ':' ty ('=' expr)? ';'
    private void analyseLetDeclStmt() throws CompileError{
        expect(TokenType.LET_KW);
        expect(TokenType.IDENT);
        expect(TokenType.COLON);
        analyseTy();
        if (nextIf(TokenType.ASSIGN)!=null){
            analyseExpr();
        }
        expect(TokenType.SEMICOLON);
    }
    //        const_decl_stmt -> 'const' IDENT ':' ty '=' expr ';'
    private void analyseConstDeclStmt() throws CompileError{
        expect(TokenType.CONST_KW);
        expect(TokenType.IDENT);
        expect(TokenType.COLON);
        analyseTy();
        expect(TokenType.ASSIGN);
        analyseExpr();
        expect(TokenType.SEMICOLON);
    }

    private void analyseBlockStmt() throws CompileError{
        expect(TokenType.L_BRACE);
        while(checkNextIfStmt()){
            analyseStmt();
        }
        expect(TokenType.R_BRACE);
    }
    private boolean checkNextIfStmt() throws CompileError{
        if(checkNextifExpr()){
            return true;
        }
        var nextToken = peek();
        switch (nextToken.getTokenType()){
            case LET_KW:
            case L_BRACE:
            case CONST_KW:
            case IF_KW:
            case WHILE_KW:
            case BREAK_KW:
            case CONTINUE_KW:
            case RETURN_KW:
            case SEMICOLON:
                return true;
            default:
                return false;
        }
    }

    // # 表达式
//    expr ->
//    operator_expr
//    | negate_expr
//    | assign_expr
//    | as_expr
//    | call_expr
//    | literal_expr
//    | ident_expr
//    | group_expr
//

//    operator_expr -> expr binary_operator expr
//         binary_operator -> '+' | '-' | '*' | '/' | '==' | '!=' | '<' | '>' | '<=' | '>='

//    negate_expr -> '-' expr
//    assign_expr -> l_expr '=' expr
//         l_expr -> IDENT
//    as_expr -> expr 'as' ty
//    call_expr -> IDENT '(' call_param_list? ')'
//        call_param_list -> expr (',' expr)*

//    literal_expr -> UINT_LITERAL | DOUBLE_LITERAL | STRING_LITERAL | CHAR_LITERAL
//    ident_expr -> IDENT
//    group_expr -> '(' expr ')'
    // ## 左值表达式

//    优先级从高到低
//    运算符	            结合性
//    括号表达式	        -
//    函数调用	        -
//    前置               -	-
//    as	            -
//    * /	            左到右
//    + -	            左到右
//    > < >= <= == !=	左到右
//    =	右到左
    private void analyseExpr() throws CompileError{
         if (check(TokenType.IDENT)){
             next();
             if (check(TokenType.ASSIGN)){
                 next();
                 /*说明这里是一个赋值语句，下面是对右值的分析，右值不可能是比较式，所以至少从加减法开始*/
//                 赋值表达式是由 左值表达式、等号 =、表达式 组成的表达式。赋值表达式的值类型永远是 void（即不能被使用）。
//                 左值表达式是一个局部或全局的变量名。
//                 赋值表达式的语义是将右侧表达式的计算结果赋给左侧表示的值。
                 analyseAddMinusExpr();
             }else if(check(TokenType.L_PAREN)){
                 next();
                 /*说明这是一个函数说明语句，对函数的一个调用，后面可能跟着运算符，所以要判断一下*/
                 analyseIdent();
             }else{
                 if (check(TokenType.NEQ)||check(TokenType.EQ)||check(TokenType.LT)||check(TokenType.GT)||check(TokenType.LE)||check(TokenType.GE)){
                     next();
                     analyseAddMinusExpr();
                 }else {
                     analyseIdent();
                 }
             }
         }else{
             analyseCompareExpr();
         }
    }

    private void analyseIdent() throws CompileError {
        if(check(TokenType.PLUS)||check(TokenType.MINUS)){
            next();
            analyseAddMinusExpr();
        }else if (check(TokenType.MUL)||check(TokenType.DIV)){
            next();
            analyseMultiDivExpr();
        }else if (check(TokenType.AS_KW)){
            next();
            analyseTy();
        }else{
            throw new AnalyzeError(ErrorCode.InvalidInput,next().getStartPos());
        }
    }

    private void analyseCompareExpr() throws CompileError{
        analyseAddMinusExpr();
        if (check(TokenType.NEQ)||check(TokenType.EQ)||check(TokenType.LT)||check(TokenType.GT)||check(TokenType.LE)||check(TokenType.GE)){
            next();
            analyseAddMinusExpr();
        }
    }

    private void analyseAddMinusExpr() throws CompileError{
        analyseMultiDivExpr();
        while (check(TokenType.PLUS)||check(TokenType.MINUS)){
            next();
            analyseMultiDivExpr();
        }
    }

    private void analyseMultiDivExpr() throws  CompileError{
        analyseTypeChangeExpr();
        while (check(TokenType.MUL)||check(TokenType.DIV)){
            next();
            analyseTypeChangeExpr();
        }
    }

    private void analyseTypeChangeExpr() throws CompileError{
        analyseFactor();
        /*或许不能用while*/
        while (check(TokenType.AS_KW)){
            next();
            analyseTy();
        }
    }

    private void analyseFactor() throws CompileError{
        if (check(TokenType.MINUS)){
            next();
        }
        if (check(TokenType.L_PAREN)){
            next();
            analyseAddMinusExpr();
            expect(TokenType.R_PAREN);
        }else if (check(TokenType.UINT_LITERAL)||check(TokenType.DOUBLE_LITERAL)||check(TokenType.STRING_LITERAL)||check(TokenType.CHAR_LITERAL)){
            next();
        }else if (check(TokenType.IDENT)){
            next();
            if (check(TokenType.L_PAREN)){
                next();
                if (!check(TokenType.R_PAREN)){
                    analyseAddMinusExpr();
                    while (nextIf(TokenType.COMMA)!=null){
                        analyseAddMinusExpr();
                    }
                }else{
                    next();
                }
            }
        }
    }
    private boolean  checkNextifExpr() throws CompileError{
        var nextToken = peek();
        switch (nextToken.getTokenType()){
            case MINUS:
            case IDENT:
            case UINT_LITERAL:
            case DOUBLE_LITERAL:
            case STRING_LITERAL:
            case CHAR_LITERAL:
            case L_PAREN:
                return true;
            default:
                return false;

        }
    }

}
