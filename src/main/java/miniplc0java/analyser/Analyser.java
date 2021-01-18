package miniplc0java.analyser;

import miniplc0java.error.AnalyzeError;
import miniplc0java.error.CompileError;
import miniplc0java.error.ErrorCode;
import miniplc0java.error.ExpectedTokenError;
import miniplc0java.error.TokenizeError;
import miniplc0java.instruction.Instruction;
import miniplc0java.instruction.Operation;
import miniplc0java.tokenizer.Token;
import miniplc0java.tokenizer.TokenType;
import miniplc0java.tokenizer.Tokenizer;
import miniplc0java.util.Pos;

import java.util.*;

public final class Analyser {

    Tokenizer tokenizer;
    /** 当前偷看的 token */
    Token peekedToken = null;
    /** 供全局变量使用的指令集 */
    ArrayList<Instruction> globalInstructions = new ArrayList<>();

    /** 局部变量表 */
    HashMap<String, SymbolEntry> localSymbolTable;
    /** 参数表 */
    HashMap<String,SymbolEntry> paramTable;
    /** 全局变量表 */
    HashMap<String, SymbolEntry> globalSymbolTable = new HashMap<>();
    /** 函数表 */
    HashMap<String,FuncEntry> funcTable;
    /** 函数体内部使用指令集 */
    ArrayList<Instruction> localInstructions;

    ArrayList<String> funcName = new ArrayList<>();
    ArrayList<String> globalName = new ArrayList<>();

    /** 下一个变量的栈偏移 */
    int nextGlobalOffset = 0;
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
        this.funcTable = new HashMap<>();
    }

    public HashMap<String, FuncEntry> analyse() throws CompileError {
        analyseProgram();
        return funcTable;
    }

    private Token peek() throws TokenizeError {
        if (peekedToken == null) {
            peekedToken = tokenizer.nextToken();
        }
        return peekedToken;
    }

    private Token next() throws TokenizeError {
        if (peekedToken != null) {
            var token = peekedToken;
            peekedToken = null;
            return token;
        } else {
            return tokenizer.nextToken();
        }
    }

    private boolean check(TokenType tt) throws TokenizeError {
        var token = peek();
        return token.getTokenType() == tt;
    }
    private Token nextIf(TokenType tt) throws TokenizeError {
        var token = peek();
        if (token.getTokenType() == tt) {
            return next();
        } else {
            return null;
        }
    }

    public HashMap<String, SymbolEntry> getGlobalSymbolTable() {
        return globalSymbolTable;
    }

    public ArrayList<String> getFuncName() {
        return funcName;
    }

    public ArrayList<String> getGlobalName() {
        return globalName;
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

    private int getNextGlobalOffset() {
        return this.nextGlobalOffset++;
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

    private void addLocalSymbol(String name, boolean isInitialized, boolean isConstant, int variableType,Pos curPos) throws AnalyzeError {
        if (this.localSymbolTable.get(name) != null) {
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, curPos);
        }else if (variableType == 0){
            throw new AnalyzeError(ErrorCode.InvalidInput,curPos);
        }else {
            this.localSymbolTable.put(name, new SymbolEntry(isConstant, isInitialized, getNextLocOff(),variableType));
        }
    }
    private void addParamSymbol(String name,boolean isConstant, int variableType,Pos curPos) throws AnalyzeError {
        if (this.paramTable.get(name) != null) {
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, curPos);
        }else if (variableType == 0){
            throw new AnalyzeError(ErrorCode.InvalidInput,curPos);
        }else{
            //函数的参数应该是默认没有初始化的，即不可能有初始值
            //TODO：这里默认设置参数的初始化为True，之后根据情况再进行改动！
            this.paramTable.put(name, new SymbolEntry(isConstant, true, getNextParamOff(),variableType));
        }
    }
    private void addGlobalSymbol(String name, boolean isFunc,boolean isStr, boolean isConstant, int type,Pos curPos) throws AnalyzeError {
        if (this.globalSymbolTable.get(name) != null&&!isStr) {
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, curPos);
        }else if (type == 0&&!isStr&&!isFunc){
            throw new AnalyzeError(ErrorCode.EOF,curPos);
        }else {
            if (isFunc) {
                //对于函数或者String，value为其全拼
                this.globalSymbolTable.put(name,new SymbolEntry(true,getNextGlobalOffset(),name.length(),name,type));
            }else if(isStr){
                if (this.globalSymbolTable.get(name)!=null){
                    name  = name + (globalSymbolTable.size()+1);
                }
                //把String的key值设为空字符串，防止在检索funcName时造成干扰
                this.globalSymbolTable.put(name,new SymbolEntry(true,getNextGlobalOffset(),name.length(),name,0));
            }else {
                //对于变量和常量，value为0，这里用空字符串表示
                this.globalSymbolTable.put(name, new SymbolEntry(isConstant, getNextGlobalOffset(), 8, "",type));
            }
        }
    }
    private void addFuncSymbol(String name, int func_global_num,int haveRet,int retType,int param_num, int locVarNum, int bodyCnt, ArrayList<Instruction> instructions,HashMap<String,SymbolEntry> paramSymbolEntry,Pos curPos)throws AnalyzeError{
        if (this.funcTable.get(name)!=null){
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration,curPos);
        }else {
            //func_global_num为函数在全局变量表中的offset，也即为func_name，是要最终输出为一个slot的东西。一个slot为1字节，即8位2进制，2位16进制
            this.funcTable.put(name,new FuncEntry(func_global_num,haveRet,retType,param_num,locVarNum,bodyCnt,instructions,getNextFuncOff(),paramSymbolEntry));
        }
    }

    private void analyseProgram() throws CompileError {
        // 程序
        //program -> item*
        //      item -> function | decl_stmt

        addFuncSymbol("_start",-1,0, 0,0,0,-1,null,null,peek().getStartPos());
        funcName.add("_start");
        while(!check(TokenType.EOF)){
            analyseItem();
        }

        if(funcTable.get("main")==null){
            throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
        }
        long mainOff = funcTable.get("main").getFuncOffset();
        long mainRetType = funcTable.get("main").getReturnType();
        long mainHaveRet = 0;
        if (mainRetType>=1){
            mainHaveRet = 1;
        }
        globalInstructions.add(new Instruction(Operation.stackalloc,mainHaveRet));
        globalInstructions.add(new Instruction(Operation.call,mainOff));
        if(funcTable.get("main").getHaveRet()>0) {
            globalInstructions.add(new Instruction(Operation.popn, 1L));
        }
        addGlobalSymbol("_start",true,false,true,0,peek().getStartPos());
        globalName.add("_start");
        int _startGlobalOff = globalSymbolTable.get("_start").stackOffset;
        funcTable.get("_start").setFunc_name(_startGlobalOff);
        funcTable.get("_start").setBodyCnt(globalInstructions.size());
        funcTable.get("_start").setInstructions(globalInstructions);
        funcTable.get("_start").setParamSymbolEntry(new HashMap<>());
        expect(TokenType.EOF);
    }

    // 程序
    //item -> function | decl_stmt
    //program -> item*
    String CurFuncName = "";
    int shouldRetType = 0;

    private void analyseItem() throws CompileError {
        if(nextIf(TokenType.FN_KW)!=null){
            //标记现在处于在函数中的状态！
            isInFunc = true;
            //# 函数
            //function -> 'fn' IDENT '(' function_param_list? ')' '->' ty block_stmt
            //      function_param -> 'const'? IDENT ':' ty
            //      function_param_list -> function_param (',' function_param)*
            Pos curPos = peek().getStartPos();
            //首先是函数名
            var nameToken = expect(TokenType.IDENT);
            String func_name = (String) nameToken.getValue();
            CurFuncName = func_name;
            expect(TokenType.L_PAREN);
            //这里是定义一个参数表，局部变量表，以及函数内的指令集，参数表和局部变量表应该会在函数编译结束后释放，指令集则被保存至函数表中
            localInstructions = new ArrayList<>();
            localSymbolTable = new HashMap<>();
            paramTable = new HashMap<>();
            nextParamOff = 0;
            shouldRetType = 0;
            //      function_param -> 'const'? IDENT ':' ty
            //      function_param_list -> function_param (',' function_param)*
            if(check(TokenType.CONST_KW)||check(TokenType.IDENT)){
                analyseFunctionParamList();
            }
            expect(TokenType.R_PAREN);
            expect(TokenType.ARROW);
            int returnType = analyseTy();
            shouldRetType = returnType;


            if (returnType>0){
                for (SymbolEntry tempParam:paramTable.values()){
                    tempParam.setStackOffset(tempParam.getStackOffset()+1);
                }
                addParamSymbol("return",false,returnType,peek().getStartPos());
                paramTable.get("return").setStackOffset(0);
            }
            //要将函数加入到全局变量表里面嗷
            addGlobalSymbol(func_name,true,false,true,returnType,curPos);
            globalName.add(func_name);
            int haveRet = 0;
            if (returnType==2){
                haveRet = 1;
            }

            //先将函数加入到函数表中，以用于递归，之后再完善指令集！
            if (haveRet>0){
                addFuncSymbol(func_name, globalSymbolTable.get(func_name).getStackOffset(), haveRet,returnType, paramTable.size()-1, localSymbolTable.size(), localInstructions.size(), localInstructions, paramTable, curPos);
                funcName.add(func_name);
            }else {
                addFuncSymbol(func_name, globalSymbolTable.get(func_name).getStackOffset(), haveRet,returnType, paramTable.size(), localSymbolTable.size(), localInstructions.size(), localInstructions, paramTable, curPos);
                funcName.add(func_name);
            }

            // * 这里是进入到一个函数体里面 {body} *//
            boolean [] bool = analyseBlockStmt(false,0,null);
            //将函数加入到函数表里面嗷
            //参数表的使命应该已经完成了，要加入到函数表里面
            boolean isReturn = bool[0];
            if (haveRet>0&&!isReturn) {
                throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
            }else {
                localInstructions.add(new Instruction(Operation.ret));
            }

            funcTable.get(func_name).setInstructions(localInstructions);
            funcTable.get(func_name).setLocVarNum(localSymbolTable.size());
            funcTable.get(func_name).setBodyCnt(localInstructions.size());

            isInFunc = false;
            nextLocOff = 0;
            CurFuncName = "";
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
        boolean isConst = false;
        if (check(TokenType.CONST_KW)){
            next();
            isConst = true;
        }
        var paramToken = expect(TokenType.IDENT);
        Pos curPos = paramToken.getStartPos();
        String paramName = (String) paramToken.getValue();
        expect(TokenType.COLON);
        int variableType = analyseTy();
        //将函数参数加入到变量表中
        addParamSymbol(paramName,isConst,variableType,curPos);
    }

    // ## 类型
    //ty -> IDENT
    private int analyseTy() throws CompileError{
        //c0 有一个十分简单的类型系统。在基础 C0 中你会用到的类型有两种：
        //      64 位有符号整数 int
        //      空类型 void
        //扩展 C0 增加了一种类型：
        //      64 位 IEEE-754 浮点数 double

        if(check(TokenType.IDENT)){
            var nameToken = next();
            String name =(String) nameToken.getValue();
            return switch (name) {
                case "void" -> 0;
                case "int" -> 1;
                case "double" -> 2;
                default -> throw new AnalyzeError(ErrorCode.InvalidInput, peek().getStartPos());
            };
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
    private boolean [] analyseBlockStmt(boolean isInWhile,int whileStart, ArrayList<Integer>breakOffset) throws CompileError{

        expect(TokenType.L_BRACE);
        boolean[] oldBool = {false,false,false};
        while(checkNextIfStmt()){
            boolean[] newBool = analyseStmt(isInWhile,whileStart,breakOffset);
            for (int i = 0; i<3; i++) {
                oldBool[i] = newBool[i] || oldBool[i];
            }
        }
        expect(TokenType.R_BRACE);
        return oldBool;
    }
    private boolean[] analyseStmt(boolean isInWhile, int whileStart, ArrayList<Integer>breakOffset) throws CompileError{
        boolean [] bool = {false,false,false};
        if(checkNextIfExpr()){
            //    expr_stmt -> expr ';'
            analyseExpr();
            expect(TokenType.SEMICOLON);
        }else if (check(TokenType.LET_KW)||check(TokenType.CONST_KW)){
            //    decl_stmt -> let_decl_stmt | const_decl_stmt
            //        let_decl_stmt -> 'let' IDENT ':' ty ('=' expr)? ';'
            //        const_decl_stmt -> 'const' IDENT ':' ty '=' expr ';'
            /* 下面这个是定义语句 */
            analyseDeclStmt();
        }else if (check(TokenType.IF_KW)){
            //    if_stmt -> 'if' expr block_stmt ('else' 'if' expr block_stmt)* ('else' block_stmt)?
            bool = analyseIfStmt(isInWhile,whileStart,breakOffset);
        }else if(check(TokenType.WHILE_KW)){
            //    while_stmt -> 'while' expr block_stmt
            analyseWhile();
        }else if(check(TokenType.BREAK_KW)){
            //    break_stmt -> 'break' ';'
            bool[2] = true;
            if (!isInWhile){
                throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
            }
            analyseBreakStmt();
            if (isInFunc){
                breakOffset.add(localInstructions.size());
                localInstructions.add(new Instruction(Operation.br, 0L));
            }else {
                throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
            }
        }else if(check(TokenType.CONTINUE_KW)){
            //    continue_stmt -> 'continue' ';'
            if (!isInWhile){
                throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
            }
            bool[1] = true;
            analyseContinueStmt();
            if (isInFunc){
                localInstructions.add(new Instruction(Operation.br, (long) (whileStart-localInstructions.size())));
            }else {
                throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
            }
        }else if (check(TokenType.RETURN_KW)){
            //    return_stmt -> 'return' expr? ';'
            bool[0] = true;
            analyseReturnStmt();
        }else if (check(TokenType.L_BRACE)){
            //    block_stmt -> '{' stmt* '}'
            bool = analyseBlockStmt(isInWhile,whileStart,breakOffset);
        }else if(check(TokenType.SEMICOLON)){
            //    empty_stmt -> ';'
            next();
        }else{
            throw new AnalyzeError(ErrorCode.InvalidInput,next().getStartPos());
        }
        return bool;
    }

    //    if_stmt -> 'if' expr block_stmt ('else' 'if' expr block_stmt)* ('else' block_stmt)?
    boolean isInIf = false;
    private boolean [] analyseIfStmt(boolean isInWhile, int whileStart, ArrayList<Integer>breakOffset) throws CompileError{
        boolean isReturn;
        boolean isContinue;
        boolean isBreak;

        expect(TokenType.IF_KW);
        //这里只可能是比较语句
        analyseCompareExpr();
        localInstructions.add(new Instruction(Operation.br_true, 1L));
        isInIf = true;

        int startIf = localInstructions.size();
        //这里需要填入当判断为false时的跳转，需要跳转到结构体结尾的下一个操作符
        localInstructions.add(new Instruction(Operation.br, 0L));

        boolean [] bool = analyseBlockStmt(isInWhile,whileStart,breakOffset);
        isReturn = bool[0];
        isContinue = bool[1];
        isBreak = bool[2];

        int jump = localInstructions.size()-startIf;
        localInstructions.get(startIf).setX(jump);

        isInIf = false;

        //这里需要设置结构体执行完后的跳转,finish跳转
        localInstructions.add(new Instruction(Operation.br, 0L));
        int tempIndex = localInstructions.size()-1;
        Stack<Integer> Index = new Stack<>();
        Index.push(tempIndex);
        int tempStartOff = tempIndex+1;
        Stack<Integer>Offset = new Stack<>();
        Offset.push(tempStartOff);

        boolean isElse = false;
        while (check(TokenType.ELSE_KW)){
            next();
            if (check(TokenType.IF_KW)){
                next();
                analyseCompareExpr();
                localInstructions.add(new Instruction(Operation.br_true, 1L));
                //这里是为false时候的跳转
                isInIf = true;
                int newStartIf = localInstructions.size();
                //这里需要填入当判断为false时的跳转，需要跳转到结构体结尾的下一个操作符
                localInstructions.add(new Instruction(Operation.br, 0L));

                bool = analyseBlockStmt(isInWhile,whileStart,breakOffset);
                isReturn &= bool[0];
                isContinue &=bool [1];
                isBreak &= bool[2];

                int newJump = localInstructions.size()-newStartIf;
                localInstructions.get(newStartIf).setX(newJump);
                isInIf = false;
                //这里应该是执行结束后的跳转
                localInstructions.add(new Instruction(Operation.br, 0L));
                int NewIndex = localInstructions.size()-1;
                Index.push(NewIndex);
                int NewStartOff = NewIndex+1;
                Offset.push(NewStartOff);
            }else{
                isInIf = true;
                bool = analyseBlockStmt(isInWhile,whileStart,breakOffset);
                isReturn &= bool[0];
                isContinue &=bool [1];
                isBreak &= bool[2];
                isElse =true;
                isInIf = false;
                break;
            }
        }
        if (!isElse){
            isReturn = false;
            isBreak = false;
            isContinue = false;
        }

        int EndOffset = localInstructions.size();
        while (!Offset.empty()&&!Index.empty()){
            int OneOff =Offset.pop();
            int OneIndex = Index.pop();
            localInstructions.get(OneIndex).setX(EndOffset-OneOff);
        }
        return new boolean[]{isReturn,isContinue,isBreak};
    }

    //    while_stmt -> 'while' expr block_stmt

    private void analyseWhile() throws CompileError{
        //分析比较式
        expect(TokenType.WHILE_KW);
        int whileStartOff=localInstructions.size();
        if (isInFunc){
            localInstructions.add(new Instruction(Operation.br, 0L));
        }else {
            //while只能在函数中出现
            throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
        }
        if (nextIf(TokenType.L_PAREN)!=null){
            analyseCompareExpr();
            localInstructions.add(new Instruction(Operation.br_true, 1L));
            expect(TokenType.R_PAREN);
        }else {
            analyseCompareExpr();
            localInstructions.add(new Instruction(Operation.br_true, 1L));
        }

        //分析结构体
        //TODO:break,continue
        int judgeFalseOff = localInstructions.size();
        localInstructions.add(new Instruction(Operation.br, 0L));
        expect(TokenType.L_BRACE);
        boolean[] bool = {false,false,false};

        ArrayList<Integer> breakOffset = new ArrayList<>();
        while(checkNextIfStmt()){
            bool = analyseStmt(true,whileStartOff,breakOffset);
            if (bool[0]||bool[1]||bool[2]){
                break;
            }
        }

        if (bool[0]||bool[1]||bool[2]){
            var nextToken = next();
            while (!nextToken.getTokenType().equals(TokenType.R_BRACE)){
                nextToken = next();
            }
        }else{
            expect(TokenType.R_BRACE);
            int whileEndOff = localInstructions.size();
            localInstructions.add(new Instruction(Operation.br, (long) (whileStartOff-whileEndOff)));
        }


        int finishOff = localInstructions.size();
        localInstructions.get(judgeFalseOff).setX(finishOff-judgeFalseOff-1);

        int nowOff = localInstructions.size()-1;
        for (int tempOff:breakOffset){
            localInstructions.get(tempOff).setX(nowOff-tempOff);
        }

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

        int ret_num = globalSymbolTable.get(CurFuncName).getVariableType();

        if (checkNextIfExpr()){
            if (ret_num<=0){
                throw new AnalyzeError(ErrorCode.NoEnd,peek().getStartPos());
            }
            //arga 0 默认为返回值的Offset;
            localInstructions.add(new Instruction(Operation.arga, 0L));
            analyseAddMinusExpr();
            localInstructions.add(new Instruction(Operation.store_64));
        }else if(ret_num>0){
            throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
        }
        if (isInFunc){
            localInstructions.add(new Instruction(Operation.ret));
        }else{
            throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
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

        boolean isInit = false;
        Pos curPos = peek().getStartPos();

        var nameToken = expect(TokenType.IDENT);

        String name = (String) nameToken.getValue();

        expect(TokenType.COLON);

        int variableType = analyseTy();

        if (nextIf(TokenType.ASSIGN)!=null){
            isInit = true;
        }

        if (isInFunc){
            //如果在函数体内则加入到函数体的局部变量表中，否则加入到全局变量
            addLocalSymbol(name,isInit,false,variableType,curPos);
        }else{
            addGlobalSymbol(name,false,false,false,variableType,curPos);
            globalName.add(name);
        }

        if (isInit){
            if (isInFunc){
                int localOff = localSymbolTable.get(name).getStackOffset();
                localInstructions.add(new Instruction(Operation.loca, (long) localOff));
                analyseAddMinusExpr();

                localInstructions.add(new Instruction(Operation.store_64));
            }else{
                int globalOff = globalSymbolTable.get(name).getStackOffset();

                globalInstructions.add(new Instruction(Operation.globa, (long) globalOff));
                analyseAddMinusExpr();

                globalInstructions.add(new Instruction(Operation.store_64));
            }
        }
        expect(TokenType.SEMICOLON);
    }

    //        const_decl_stmt -> 'const' IDENT ':' ty '=' expr ';'
    private void analyseConstDeclStmt() throws CompileError{
        expect(TokenType.CONST_KW);
        Pos curPos = peek().getStartPos();
        var constNameToken = expect(TokenType.IDENT);
        String constName = (String) constNameToken.getValue();
        expect(TokenType.COLON);
        int variableType = analyseTy();
        expect(TokenType.ASSIGN);
        if (isInFunc){
            addLocalSymbol(constName,true,true,variableType,curPos);
            //获取当前局部变量的地址；loca：加载off个slot处局部变量
            int localOff = localSymbolTable.get(constName).getStackOffset();
            localInstructions.add(new Instruction(Operation.loca, (long) localOff));
            analyseExpr();
            localInstructions.add(new Instruction(Operation.store_64));
        }else{
            addGlobalSymbol(constName,false,false,true,variableType,curPos);
            globalName.add(constName);
            int globalOff = globalSymbolTable.get(constName).getStackOffset();
            globalInstructions.add(new Instruction(Operation.globa, (long) globalOff));
            analyseExpr();
            globalInstructions.add(new Instruction(Operation.store_64));
        }
        expect(TokenType.SEMICOLON);
    }

    private boolean checkNextIfStmt() throws CompileError{
        if(checkNextIfExpr()){
            return true;
        }
        var nextToken = peek();
        return switch (nextToken.getTokenType()) {
            case LET_KW, L_BRACE, CONST_KW, IF_KW, WHILE_KW, BREAK_KW, CONTINUE_KW, RETURN_KW, SEMICOLON -> true;
            default -> false;
        };
    }

    /** 表达式部分 */
    private void analyseExpr() throws CompileError{
        if (check(TokenType.IDENT)){
            var nameToken = expect(TokenType.IDENT);
            String LeftName =(String) nameToken.getValue();
            if (nextIf(TokenType.ASSIGN)!=null){
                //这里是赋值语句的左值！
                if (isInFunc){
                    if (localSymbolTable.get(LeftName)!=null){
                        if (localSymbolTable.get(LeftName).isConstant){
                            throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
                        }
                        int localOff = localSymbolTable.get(LeftName).getStackOffset();
                        localSymbolTable.get(LeftName).setInitialized(true);
                        localInstructions.add(new Instruction(Operation.loca, (long) localOff));
                    }else if (paramTable.get(LeftName)!=null){
                        if (paramTable.get(LeftName).isConstant){
                            throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
                        }
                        int paramOff = paramTable.get(LeftName).getStackOffset();
                        localInstructions.add(new Instruction(Operation.arga, (long) paramOff));
                    }else if (globalSymbolTable.get(LeftName)!=null){
                        if (globalSymbolTable.get(LeftName).isConstant){
                            throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
                        }
                        int globalOff = globalSymbolTable.get(LeftName).getStackOffset();
                        globalSymbolTable.get(LeftName).setInitialized(true);
                        localInstructions.add(new Instruction(Operation.globa, (long) globalOff));
                    }else{
                        throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
                    }
                }else {
                    int globalOff;

                    if (globalSymbolTable.get(LeftName)==null){
                        throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
                    }
                    globalSymbolTable.get(LeftName).setInitialized(true);
                    globalOff = globalSymbolTable.get(LeftName).getStackOffset();
                    globalInstructions.add(new Instruction(Operation.globa, (long) globalOff));
                }
                /*说明这里是一个赋值语句，下面是对右值的分析，右值不可能是比较式，所以至少从加减法开始*/
//                 赋值表达式是由 左值表达式、等号 =、表达式 组成的表达式。赋值表达式的值类型永远是 void（即不能被使用）。
//                 左值表达式是一个局部或全局的变量名。
//                 赋值表达式的语义是将右侧表达式的计算结果赋给左侧表示的值。
                //这里是赋值语句的右值
                //赋值的右值还可能是函数，TODO:但是函数一定要有返回值！！！这里标准库函数没法验证
                analyseAddMinusExpr();
                /* 对于赋值语句来说这里需要存值！ */
                if (isInFunc) {
                    localInstructions.add(new Instruction(Operation.store_64));
                } else {
                    globalInstructions.add(new Instruction(Operation.store_64));
                }
//                 }
            }else if(nextIf(TokenType.L_PAREN)!=null){
                /*说明这是一个函数说明语句，对函数的一个调用，后面可能跟着运算符，所以要判断一下*/
                /*这里的EmptyExpr并不是空语句 */
                /*就单独一个函数式的情况*/
                int funcOff;
                var funcSymbol = funcTable.get(LeftName);
                if (funcSymbol == null){
                    /* 在isStandardFunc函数里面能够处理标准库函数，已经POP了 */
                    boolean isStd= isStandardFunc(LeftName,true);
                    expect(TokenType.R_PAREN);
                    if (!isStd){
                        throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
                    }
                }else {
                    //不是标准库函数，进行栈处理
                    if (isInFunc) {
                        int ret_num = funcSymbol.getHaveRet();
                        localInstructions.add(new Instruction(Operation.stackalloc, (long) ret_num));
                    } else {
                        throw new AnalyzeError(ErrorCode.InvalidInput, peek().getStartPos());
                    }
                    //说明不是标准库函数
                    //进行参数处理！
                    //对表达式进行分析;对于函数参数的空式子不用popn
                    if (check(TokenType.R_PAREN)){
                        expect(TokenType.R_PAREN);
                    }else{
                        //如果左括号的下一个token不是右括号，则说明函数有参数，这后面就是函数的参数
                        //TODO:进行语义分析，而且参数的类型应该匹配！但是这里只有Int，暂时不考虑
                        int paramNumRight = funcSymbol.getParam_num();
                        int tempParamNum = 0;

                        analyseAddMinusExpr();
                        tempParamNum++;
                        while (nextIf(TokenType.COMMA)!=null){
                            tempParamNum++;
                            analyseAddMinusExpr();
                        }
                        //如果参数数量和传入的参数数量不匹配则会报错！
                        if (paramNumRight!=tempParamNum){
                            throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
                        }
                        expect(TokenType.R_PAREN);
                    }
                    funcOff = funcSymbol.getFuncOffset();
                    localInstructions.add(new Instruction(Operation.call, (long) funcOff));
                    long ret_num = funcSymbol.getHaveRet();
                    if (ret_num>0) {
                        localInstructions.add(new Instruction(Operation.popn, ret_num));
                    }
                }
            }else{
                //这里说明这个语句是一个空语句，并不是赋值语句或者其他；仅仅是一个空语句，类似 b+1;这种
                if (check(TokenType.NEQ)||check(TokenType.EQ)||check(TokenType.LT)||check(TokenType.GT)||check(TokenType.LE)||check(TokenType.GE)){
                    //这里是一个判断语句；
                    //实现一个判断的空语句
                    var CompareSymbol =next();
                    analyseAddMinusExpr();
                    if (isInFunc){
                        localInstructions.add(new Instruction(Operation.cmp_i));
                    }else {
                        throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
                    }
                    //如果!=则没什么处理
                    switch (CompareSymbol.getTokenType()) {
                        case EQ -> localInstructions.add(new Instruction(Operation.not));
                        case LT -> localInstructions.add(new Instruction(Operation.set_lt));
                        case GT -> localInstructions.add(new Instruction(Operation.set_gt));
                        case LE -> {
                            localInstructions.add(new Instruction(Operation.set_gt));
                            localInstructions.add(new Instruction(Operation.not));
                        }
                        case GE -> {
                            localInstructions.add(new Instruction(Operation.set_lt));
                            localInstructions.add(new Instruction(Operation.not));
                        }
                        default -> throw new AnalyzeError(ErrorCode.InvalidInput, peek().getStartPos());
                    }
                    localInstructions.add(new Instruction(Operation.popn,1L));
                }else {
                    //这里是一个空的运算式
                    if (isInFunc){
                        long localOff;
                        if (localSymbolTable.get(LeftName)==null){
                            throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
                        }
                        localOff = localSymbolTable.get(LeftName).getStackOffset();
                        localInstructions.add(new Instruction(Operation.loca, localOff));
                        localInstructions.add(new Instruction(Operation.load_64));
                    }else {
                        //全局没有空运算式;
                        throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
                    }
                    analyseEmptyExpr();
                    localInstructions.add(new Instruction(Operation.popn, 1L));
                }
            }
        }else{
            //这里也是单独一个计算式，不过可能第一个不是变量，而是一个数字；
            analyseCompareExpr();
            if (isInFunc){
                localInstructions.add(new Instruction(Operation.popn, 1L));
            }
        }
    }

    //空式子和函数参数式子中的运算式
    //用于引导空语句和函数参数式子中的运算式；
    //TODO:只考虑了Int
    private void analyseEmptyExpr() throws CompileError {
        var OptToken = next();
        //TODO:只考虑了合法的类型转换
        switch (OptToken.getTokenType()) {
            case PLUS -> {
                analyseAddMinusExpr();
                localInstructions.add(new Instruction(Operation.add_i));
            }
            case MINUS -> {
                analyseAddMinusExpr();
                localInstructions.add(new Instruction(Operation.sub_i));
            }
            case MUL -> {
                analyseMultiDivExpr();
                localInstructions.add(new Instruction(Operation.mul_i));
            }
            case DIV -> {
                analyseMultiDivExpr();
                localInstructions.add(new Instruction(Operation.div_i));
            }
            case AS_KW -> {
                int type = analyseTy();
                if (type == 1) {
                    localInstructions.add(new Instruction(Operation.ftoi));
                } else if (type == 2) {
                    localInstructions.add(new Instruction(Operation.itof));
                } else {
                    throw new AnalyzeError(ErrorCode.InvalidInput, peek().getStartPos());
                }
            }
            default -> throw new AnalyzeError(ErrorCode.InvalidInput, peek().getStartPos());
        }
    }

    //判断语句
    private void analyseCompareExpr() throws CompileError{
        analyseAddMinusExpr();
        if (check(TokenType.NEQ)||check(TokenType.EQ)||check(TokenType.LT)||check(TokenType.GT)||check(TokenType.LE)||check(TokenType.GE)){
            //TODO:这里只考虑了Int
            var CompareSymbolToken =next();

            analyseAddMinusExpr();
            if (isInFunc){
                localInstructions.add(new Instruction(Operation.cmp_i));
            }else {
                throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
            }

            switch (CompareSymbolToken.getTokenType()){
                case NEQ:
                    break;
                case EQ:
                    //如果!=则没什么处理
                    localInstructions.add(new Instruction(Operation.not));
                    break;
                case LT:
                    localInstructions.add(new Instruction(Operation.set_lt));
                    break;
                case GT:
                    localInstructions.add(new Instruction(Operation.set_gt));
                    break;
                case LE:
                    localInstructions.add(new Instruction(Operation.set_gt));
                    localInstructions.add(new Instruction(Operation.not));
                    break;
                case GE:
                    localInstructions.add(new Instruction(Operation.set_lt));
                    localInstructions.add(new Instruction(Operation.not));
                    break;
                default:
                    throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
            }
        }
    }

    private void analyseAddMinusExpr() throws CompileError{
        analyseMultiDivExpr();
        boolean isAdd;
        while (check(TokenType.PLUS)||check(TokenType.MINUS)){
            if (nextIf(TokenType.PLUS)!=null){
                isAdd = true;
            }else if (nextIf(TokenType.MINUS)!=null){
                isAdd = false;
            }else{
                throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
            }
            analyseMultiDivExpr();
            if (isInFunc){
                if (isAdd){
                    localInstructions.add(new Instruction(Operation.add_i));
                }else{
                    localInstructions.add(new Instruction(Operation.sub_i));
                }
            }else{
                if (isAdd){
                    globalInstructions.add(new Instruction(Operation.add_i));
                }else {
                    globalInstructions.add(new Instruction(Operation.sub_i));
                }
            }
        }
    }

    private void analyseMultiDivExpr() throws  CompileError{
        analyseTypeChangeExpr();
        boolean isMul = false;
        while (check(TokenType.MUL)||check(TokenType.DIV)){
            if (nextIf(TokenType.MUL)!=null){
                isMul = true;
            }else if (nextIf(TokenType.DIV)!=null){
                isMul = false;
            }
            analyseTypeChangeExpr();
            if (isInFunc){
                if (isMul){
                    localInstructions.add(new Instruction(Operation.mul_i));
                }else {
                    localInstructions.add(new Instruction(Operation.div_i));
                }
            }else{
                if (isMul){
                    globalInstructions.add(new Instruction(Operation.mul_i));
                }else {
                    globalInstructions.add(new Instruction(Operation.div_i));
                }
            }
        }
    }

    private void analyseTypeChangeExpr() throws CompileError{
        analyseFactor();
        /*或许不能用while*/
        while (check(TokenType.AS_KW)){
            next();
            int type = analyseTy();
            if (type==1){
                if (isInFunc){
                    localInstructions.add(new Instruction(Operation.ftoi));
                }else{
                    globalInstructions.add(new Instruction(Operation.ftoi));
                }
            }else if (type==2){
                if (isInFunc){
                    localInstructions.add(new Instruction(Operation.itof));
                }else {
                    globalInstructions.add(new Instruction(Operation.itof));
                }
            }
        }
    }

    private void analyseFactor() throws CompileError{
        boolean negate = false;
        //这里是一个取翻表达式；
        while (check(TokenType.MINUS)){
            next();
            negate = !negate;
        }
        if (check(TokenType.L_PAREN)){
            next();
            analyseAddMinusExpr();
            expect(TokenType.R_PAREN);
        }else if (check(TokenType.UINT_LITERAL)||check(TokenType.DOUBLE_LITERAL)||check(TokenType.STRING_LITERAL)||check(TokenType.CHAR_LITERAL)){
            if (check(TokenType.UINT_LITERAL)){
                var intToken = expect(TokenType.UINT_LITERAL);
                long intNum = (long) intToken.getValue();
                if (isInFunc){
                    localInstructions.add(new Instruction(Operation.push,intNum));
                }else{
                    globalInstructions.add(new Instruction(Operation.push,intNum));
                }
            }else if (check(TokenType.DOUBLE_LITERAL)){
                //TODO:拓展部分，需要考虑double情况！
                var doubleToken = expect(TokenType.DOUBLE_LITERAL);
                double doubleNum = (double)doubleToken.getValue();
                if (isInFunc){
                    localInstructions.add(new Instruction(Operation.push,Double.doubleToLongBits(doubleNum)));
                }else {

                }
            }else if (check(TokenType.STRING_LITERAL)){
                //对字符串String的处理
                //对于String类型，只会出现在putStr中，而且String要加入到全局变量表当中;
                var strToken = expect(TokenType.STRING_LITERAL);
                String strName = (String) strToken.getValue();
                if (globalSymbolTable.get(strName)==null){
                    addGlobalSymbol(strName,false,true,true,0,peek().getStartPos());
                    globalName.add(strName);
                }

                //获取当前全局变量表的偏移量;

                int globalOff = globalSymbolTable.get(strName).getStackOffset();

                if (isInFunc){
                    localInstructions.add(new Instruction(Operation.push, (long) globalOff));
                }else{
                    //putstr函数一定在某一个函数当中出现，不可能作为全局变量出现，所以应该报错;
                    throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
                }
            }else if (check(TokenType.CHAR_LITERAL)){
                //对字符串char的处理
                //对于char类型，只会出现在putChar中，而且Char要加入到全局变量表当中;
                var charToken = expect(TokenType.CHAR_LITERAL);
                long tempChar = (char) (Character) charToken.getValue();
                //获取当前全局变量表的偏移量;
                if (isInFunc){
                    localInstructions.add(new Instruction(Operation.push,tempChar));
                }else{
                    //putChar函数一定在某一个函数当中出现，不可能作为全局变量出现，所以应该报错;
                    throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
                }
            }else{
                throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
            }
        }else if (check(TokenType.IDENT)){
            var nameToken = expect(TokenType.IDENT);
            String name = (String) nameToken.getValue();
            if (nextIf(TokenType.L_PAREN)!=null){
                //有左括号，说明是函数调用
                var funcSymbol = funcTable.get(name);
                if (funcSymbol==null){
                    //这里要考虑库函数的情况
                    boolean isStd = isStandardFunc(name,false);
                    expect(TokenType.R_PAREN);
                    if (!isStd){
                        throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
                    }
                }else{
                    //这里就不是库函数了，而是自己定义的函数
                    long ret_num = funcSymbol.getHaveRet();
                    if (isInFunc){
                        localInstructions.add(new Instruction(Operation.stackalloc,ret_num));
                    }else {
                        globalInstructions.add(new Instruction(Operation.stackalloc,ret_num));
                    }
                    if (!check(TokenType.R_PAREN)){
                        //如果左括号的下一个token不是右括号，则说明函数有参数，这后面就是函数的参数
                        //TODO:进行语义分析，而且参数的类型应该匹配！但是这里只有Int，暂时不考虑
                        int paramNumRight = funcSymbol.getParam_num();
                        int tempParamNum = 0;
                        analyseAddMinusExpr();
                        tempParamNum++;
                        while (nextIf(TokenType.COMMA)!=null){
                            tempParamNum++;
                            analyseAddMinusExpr();
                        }
                        //如果参数数量和传入的参数数量不匹配则会报错！
                        if (paramNumRight!=tempParamNum){
                            throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
                        }

                        expect(TokenType.R_PAREN);
                    }else{
                        expect(TokenType.R_PAREN);
                    }
                    //这里还应该有call操作啊~
                    long funcOff = funcSymbol.getFuncOffset();
                    if (isInFunc){
                        localInstructions.add(new Instruction(Operation.call,funcOff));
                    }else {
                        globalInstructions.add(new Instruction(Operation.call,funcOff));
                    }
                }
            }else {
                //下一个token不是左括号，说明不是函数，而是变量或者常量
                if (isInFunc){
                    var localSymbol = localSymbolTable.get(name);
                    var paramSymbol = paramTable.get(name);
                    var globalSymbol = globalSymbolTable.get(name);
                    if (localSymbol!=null){
                        if (!localSymbol.isInitialized){
                            //标识符没初始化
                            throw new AnalyzeError(ErrorCode.NotInitialized,peek().getStartPos());
                        }
                        var localOff = localSymbol.getStackOffset();
                        localInstructions.add(new Instruction(Operation.loca, (long) localOff));
                        localInstructions.add(new Instruction(Operation.load_64));
                    }else if (paramSymbol!=null){
                        var paramOff = paramSymbol.getStackOffset();
                        localInstructions.add(new Instruction(Operation.arga, (long) paramOff));
                        localInstructions.add(new Instruction(Operation.load_64));
                    }else if(globalSymbol!=null){
                        var globalOff = globalSymbol.getStackOffset();
                        localInstructions.add(new Instruction(Operation.globa, (long) globalOff));
                        localInstructions.add(new Instruction(Operation.load_64));
                    }else {
                        throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
                    }

                }else{
                    var globalSymbol = globalSymbolTable.get(name);
                    if (globalSymbol!=null){
                        if (!globalSymbol.isInitialized){
                            //标识符没初始化
                            throw new AnalyzeError(ErrorCode.NotInitialized,peek().getStartPos());
                        }
                        var globalOff = globalSymbol.getStackOffset();
                        globalInstructions.add(new Instruction(Operation.loca, (long) globalOff));
                        globalInstructions.add(new Instruction(Operation.load_64));
                    }else {
                        throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
                    }
                }
            }
        }else {
            throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
        }
        if (negate){
            //TODO：这里只考虑了Int的情况
            if (isInFunc){
                localInstructions.add(new Instruction(Operation.neg_i));
            }else {
                globalInstructions.add(new Instruction(Operation.neg_i));
            }
        }
    }
    private boolean checkNextIfExpr() throws CompileError{
        var nextToken = peek();
        return switch (nextToken.getTokenType()) {
            case MINUS, IDENT, UINT_LITERAL, DOUBLE_LITERAL, STRING_LITERAL, CHAR_LITERAL, L_PAREN -> true;
            default -> false;
        };
    }
    //处理标准库函数
    private boolean isStandardFunc(String name,boolean EmptyNoRet) throws CompileError{
        switch (name){
            case "getint":
                if (isInFunc){
                    localInstructions.add(new Instruction(Operation.scan_i));
                    if (EmptyNoRet){
                        localInstructions.add(new Instruction(Operation.popn, 1L));
                    }
                }else{
                    globalInstructions.add(new Instruction(Operation.scan_i));
                    if (EmptyNoRet){
                        throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
                    }
                }
                return true;
            case "getchar":
                if (isInFunc){
                    localInstructions.add(new Instruction(Operation.scan_c));
                    if (EmptyNoRet){
                        localInstructions.add(new Instruction(Operation.popn, 1L));
                    }
                }else{
                    globalInstructions.add(new Instruction(Operation.scan_c));
                    if (EmptyNoRet){
                        throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
                    }
                }
                return true;
            case "getdouble":
                if (isInFunc){
                    localInstructions.add(new Instruction(Operation.scan_f));
                    if (EmptyNoRet){
                        localInstructions.add(new Instruction(Operation.popn, 1L));
                    }
                }else{
                    globalInstructions.add(new Instruction(Operation.scan_f));
                    if (EmptyNoRet){
                        throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
                    }
                }
                return true;
            case "putdouble":
                if (isInFunc){
                    analyseAddMinusExpr();
                    localInstructions.add(new Instruction(Operation.print_f));
                }else {
                    throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
                }
                return  true;
            case "putint":
                if (isInFunc){
                    analyseAddMinusExpr();
                    localInstructions.add(new Instruction(Operation.print_i));
                }else {
                    throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
                }
                return  true;
            case "putchar":
                if (isInFunc){
                    analyseAddMinusExpr();
                    localInstructions.add(new Instruction(Operation.print_c));
                }else {
                    throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
                }
                return  true;
            case "putstr":
                if (isInFunc){
                    analyseAddMinusExpr();
                    localInstructions.add(new Instruction(Operation.print_s));
                }else {
                    throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
                }
                return  true;
            case "putln":
                if (isInFunc){
                    localInstructions.add(new Instruction(Operation.println));
                }else {
                    throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
                }
                return true;
            default:
                return false;
        }
    }
}
