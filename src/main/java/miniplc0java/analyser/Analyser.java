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
//    /** 指令集 */
//    ArrayList<Instruction> instructions = new ArrayList<>();;
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
        /*TODO:到底需要输出什么东西？应该是一个个的函数，初步感觉是输出函数表*/
    }

    public HashMap<String, FuncEntry> analyse() throws CompileError {
        analyseProgram();
        /*TODO:需要重构返回值*/
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
    public int getGlobalCounts() throws TokenizeError{
        return globalSymbolTable.size();
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
                //把String的key值设为空字符串，防止在检索funcName时造成干扰
                this.globalSymbolTable.put(name,new SymbolEntry(true,getNextGlobalOffset(),name.length(),name,0));
            }else {
                //对于变量和常量，value为0，这里用空字符串表示
                this.globalSymbolTable.put(name, new SymbolEntry(isConstant, getNextGlobalOffset(), 8, "",type));
            }
        }
    }

    private void addFuncSymbol(String name, int func_global_num,int ret_num,int param_num, int locVarNum, int bodyCnt, ArrayList<Instruction> instructions,HashMap<String,SymbolEntry> paramSymbolEntry,Pos curPos)throws AnalyzeError{
        if (this.funcTable.get(name)!=null){
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration,curPos);
        }else {
            //func_global_num为函数在全局变量表中的offset，也即为func_name，是要最终输出为一个slot的东西。一个slot为1字节，即8位2进制，2位16进制
            this.funcTable.put(name,new FuncEntry(func_global_num,ret_num,param_num,locVarNum,bodyCnt,instructions,getNextFuncOff(),paramSymbolEntry));
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

    private void analyseProgram() throws CompileError {
        // 程序
        //program -> item*
        //      item -> function | decl_stmt
        //TODO _start()




        addFuncSymbol("_start",-1,0, 0,0,-1,null,null,peek().getStartPos());
        funcName.add("_start");
        while(!check(TokenType.EOF)){
            analyseItem();
        }
        if(funcTable.get("main")==null){
            throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
        }

        int mainOff = funcTable.get("main").funcOffset;
        globalInstructions.add(new Instruction(Operation.stackalloc,1));
        globalInstructions.add(new Instruction(Operation.call,mainOff));
        globalInstructions.add(new Instruction(Operation.popn,1));

        addGlobalSymbol("_start",true,false,true,0,peek().getStartPos());
        globalName.add("_start");
        int _startGlobalOff = globalSymbolTable.size()-1;
        funcTable.get("_start").setFunc_name(_startGlobalOff);
        funcTable.get("_start").setBodyCnt(globalInstructions.size());
        funcTable.get("_start").setInstructions(globalInstructions);
        funcTable.get("_start").setParamSymbolEntry(new HashMap<String,SymbolEntry>());
        expect(TokenType.EOF);
    }

    // 程序
    //item -> function | decl_stmt
    //program -> item*
    String CurfuncName = "";
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
            CurfuncName = func_name;
            expect(TokenType.L_PAREN);
            //这里是定义一个参数表，局部变量表，以及函数内的指令集，参数表和局部变量表应该会在函数编译结束后释放，指令集则被保存至函数表中
            localInstructions = new ArrayList<>();
            localSymbolTable = new HashMap<>();
            paramTable = new HashMap<>();
            nextParamOff = 0;
            //      function_param -> 'const'? IDENT ':' ty
            //      function_param_list -> function_param (',' function_param)*
            if(check(TokenType.CONST_KW)||check(TokenType.IDENT)){
                analyseFunctionParamList();
            }
            expect(TokenType.R_PAREN);
            expect(TokenType.ARROW);
            int ret_num = analyseTy();
            if (ret_num>0){
                for (SymbolEntry tempParam:paramTable.values()){
                    tempParam.setStackOffset(tempParam.getStackOffset()+1);
                }
                addParamSymbol("return",false,ret_num,peek().getStartPos());
                paramTable.get("return").setStackOffset(0);
            }
            //要将函数加入到全局变量表里面嗷
            addGlobalSymbol(func_name,true,false,true,ret_num,curPos);
            globalName.add(func_name);
            if (ret_num==2){
                //Todo:Double;
                ret_num = 1;
            }
            //这里是进入到一个函数体里面 {body}

            analyseBlockStmt();
            //将函数加入到函数表里面嗷
            //参数表的使命应该已经完成了，要加入到函数表里面
            if (ret_num>0){
                addFuncSymbol(func_name, globalSymbolTable.get(func_name).getStackOffset(), ret_num, paramTable.size()-1, localSymbolTable.size(), localInstructions.size(), localInstructions, paramTable, curPos);
                funcName.add(func_name);
            }else {
                addFuncSymbol(func_name, globalSymbolTable.get(func_name).getStackOffset(), ret_num, paramTable.size(), localSymbolTable.size(), localInstructions.size(), localInstructions, paramTable, curPos);
                funcName.add(func_name);
            }//这里应该已经分析函数完函数了
            isInFunc = false;
            nextLocOff = 0;
            CurfuncName = "";
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
            if (name.equals("void")){
                return 0;
            }else if(name.equals("int")){
                return 1;
            }else if (name.equals("double")){
                return 2;
            }else{
                throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
            }
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
    boolean isBreak = false, isContinue = false, isRet = false;
    private void analyseBlockStmt() throws CompileError{

        expect(TokenType.L_BRACE);
        while(checkNextIfStmt()){
            analyseStmt();
        }
        expect(TokenType.R_BRACE);
    }
    private void analyseStmt() throws CompileError{
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
            analyseIfStmt();
        }else if(check(TokenType.WHILE_KW)){
            //    while_stmt -> 'while' expr block_stmt
            analyseWhile();
        }else if(check(TokenType.BREAK_KW)){
            //    break_stmt -> 'break' ';'
            isBreak = true;
            analyseBreakStmt();
            if (isInFunc){
                localInstructions.add(new Instruction(Operation.br,0));
            }
        }else if(check(TokenType.CONTINUE_KW)){
            //    continue_stmt -> 'continue' ';'
            isContinue = true;
            analyseContinueStmt();
        }else if (check(TokenType.RETURN_KW)){
            //    return_stmt -> 'return' expr? ';'
            isRet = true;
            analyseReturnStmt();
        }else if (check(TokenType.L_BRACE)){
            //    block_stmt -> '{' stmt* '}'
            analyseBlockStmt();
        }else if(check(TokenType.SEMICOLON)){
            //    empty_stmt -> ';'
            next();
        }else{
            throw new AnalyzeError(ErrorCode.InvalidInput,next().getStartPos());
        }
    }

    //    if_stmt -> 'if' expr block_stmt ('else' 'if' expr block_stmt)* ('else' block_stmt)?
    boolean isInIf = false;
    private void analyseIfStmt() throws CompileError{
        expect(TokenType.IF_KW);
        //这里只可能是比较语句
        analyseCompareExpr(false);
        isInIf = true;
        int startIf = localInstructions.size();
        //这里需要填入当判断为false时的跳转，需要跳转到结构体结尾的下一个操作符
        localInstructions.add(new Instruction(Operation.br,0));
        int index = startIf;
        analyseBlockStmt();
        int jump = localInstructions.size()-startIf;
        localInstructions.get(index).setX(jump);
        isInIf = false;
        //这里需要设置结构体执行完后的跳转,finish跳转
        localInstructions.add(new Instruction(Operation.br,0));

        int tempIndex = localInstructions.size()-1;
        Stack<Integer> Index = new Stack<>();
        Index.push(tempIndex);
        int tempStartOff = localInstructions.size();
        Stack<Integer>Offset = new Stack<>();
        Offset.push(tempStartOff);

        while (check(TokenType.ELSE_KW)){
            next();
            if (check(TokenType.IF_KW)){
                next();
                analyseCompareExpr(false);
                //这里是为false时候的跳转
                isInIf = true;
                int newStartIf = localInstructions.size();
                //这里需要填入当判断为false时的跳转，需要跳转到结构体结尾的下一个操作符
                localInstructions.add(new Instruction(Operation.br,0));
                int newIndex = newStartIf;

                analyseBlockStmt();

                int newJump = localInstructions.size()-newStartIf;
                localInstructions.get(newIndex).setX(newJump);
                isInIf = false;

                //这里应该是执行结束后的跳转
                localInstructions.add(new Instruction(Operation.br,0));
                int NewIndex = localInstructions.size()-1;
                Index.push(NewIndex);
                int NewStartOff = localInstructions.size();
                Offset.push(NewStartOff);
            }else{
                isInIf = true;
                analyseBlockStmt();
                isInIf = false;
                break;
            }

            int EndOffset = localInstructions.size();
            while (!Offset.empty()&&!Index.empty()){
                int OneOff =Offset.pop();
                int OneIndex = Index.pop();
                localInstructions.get(OneIndex).setX(EndOffset-OneOff);
            }

        }

        if (check(TokenType.ELSE_KW)){
            next();
            analyseBlockStmt();
        }
    }

    //    while_stmt -> 'while' expr block_stmt
    boolean isInWhile = false;
    private void analyseWhile() throws CompileError{
        isInWhile =true;
        expect(TokenType.WHILE_KW);
        int whileStartOff=localInstructions.size();
        if (isInFunc){
            localInstructions.add(new Instruction(Operation.br,0));
        }else {
            //while只能在函数中出现
            throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
        }
        analyseCompareExpr(false);
        //分析结构体
        //TODO:break,continue,return
        int judgeFalseOff = localInstructions.size();
        localInstructions.add(new Instruction(Operation.br,0));
        int judgeIndex = judgeFalseOff;
        expect(TokenType.L_BRACE);
        isBreak = false;
        isContinue = false;
        isRet = false;
        while(checkNextIfStmt()){
            analyseStmt();
            if (isContinue||isRet||isBreak){
                //不在If里面的情况
                if (isContinue){
                    if (isInIf) {
                        int ContinueOff = localInstructions.size();
                        localInstructions.add(new Instruction(Operation.br, whileStartOff - ContinueOff));
                    }
                } else if (isBreak) {
                    localInstructions.add(new Instruction(Operation.br,0));
                }
                break;
            }
        }
        if (isContinue||isRet||isBreak){
            var nextToken = next();
            while (!nextToken.getTokenType().equals(TokenType.R_BRACE)){
                nextToken = next();
            }
        }else{
            expect(TokenType.R_BRACE);
            int whileEndOff = localInstructions.size();
            localInstructions.add(new Instruction(Operation.br,whileStartOff-whileEndOff));
        }
        int finishOff = localInstructions.size();
        localInstructions.get(judgeIndex).setX(finishOff-judgeFalseOff-1);
        //结构体分析结束！
        isRet = false;
        isContinue = false;
        isBreak = false;
        isInWhile = false;
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

        int ret_num = globalSymbolTable.get(CurfuncName).getVariableType();

        if (checkNextIfExpr()){
            if (ret_num<=0){
                throw new AnalyzeError(ErrorCode.NoEnd,peek().getStartPos());
            }
            //arga 0 默认为返回值的Offset;
            localInstructions.add(new Instruction(Operation.arga,0));
            analyseAddMinusExpr();
            localInstructions.add(new Instruction(Operation.store_64));
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
                localInstructions.add(new Instruction(Operation.loca,localOff));

                analyseAddMinusExpr();

                localInstructions.add(new Instruction(Operation.store_64));
            }else{
                int globalOff = globalSymbolTable.get(name).getStackOffset();

                globalInstructions.add(new Instruction(Operation.globa,globalOff));
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
            localInstructions.add(new Instruction(Operation.loca,localOff));
            analyseExpr();
            localInstructions.add(new Instruction(Operation.store_64));
        }else{
            addGlobalSymbol(constName,false,false,true,variableType,curPos);
            globalName.add(constName);
            int globalOff = globalSymbolTable.get(constName).getStackOffset();
            globalInstructions.add(new Instruction(Operation.globa,globalOff));
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

    /** 表达式部分 */
    private void analyseExpr() throws CompileError{
        if (check(TokenType.IDENT)){
            var nameToken = expect(TokenType.IDENT);
            String name =(String) nameToken.getValue();
            if (nextIf(TokenType.ASSIGN)!=null){
                //这里是赋值语句的左值！

                if (isInFunc){
                    int localOff = localSymbolTable.get(name).getStackOffset();

                    if (localSymbolTable.get(name)==null){
                        throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
                    }
                    localSymbolTable.get(name).setInitialized(true);
                    localInstructions.add(new Instruction(Operation.loca, localOff));
                }else {
                    int globalOff;

                    if (globalSymbolTable.get(name)==null){
                        throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
                    }
                    globalSymbolTable.get(name).setInitialized(true);
                    globalOff = globalSymbolTable.get(name).getStackOffset();
                    globalInstructions.add(new Instruction(Operation.globa,globalOff));
                }
                /*说明这里是一个赋值语句，下面是对右值的分析，右值不可能是比较式，所以至少从加减法开始*/
//                 赋值表达式是由 左值表达式、等号 =、表达式 组成的表达式。赋值表达式的值类型永远是 void（即不能被使用）。
//                 左值表达式是一个局部或全局的变量名。
//                 赋值表达式的语义是将右侧表达式的计算结果赋给左侧表示的值。
                //这里是赋值语句的右值
                //赋值的右值还可能是函数，TODO:但是函数一定要有返回值！！！这里标准库函数没法验证
                if (check(TokenType.L_PAREN)){
                    next();
                    int funcOff;
                    var funcSymbol = funcTable.get(name);
                    if (funcSymbol == null){
                        /* 在isStandardFunc函数里面能够处理标准库函数，已经POP了 */
                        boolean isStd= isStandardFunc(name,false);
                        if (!isStd){
                            throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
                        }
                    }else {
                        //不是标准库函数，进行栈处理
                        if (isInFunc) {
                            int ret_num = funcSymbol.getRet_num();
                            if (ret_num<=0){
                                throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
                            }
                            localInstructions.add(new Instruction(Operation.stackalloc,ret_num));
                        } else {
                            throw new AnalyzeError(ErrorCode.InvalidInput, peek().getStartPos());
                        }
                    }
                    //说明不是标准库函数
                    if (funcSymbol!=null){
                        //进行参数处理！
                        //对表达式进行分析;对于函数参数的空式子不用popn
                        int paramNum = funcSymbol.getParam_num();
                        while (paramNum>0) {
                            paramNum --;
                            analyseEmptyExpr();
                        }
                        funcOff = funcSymbol.getFuncOffset();
                        localInstructions.add(new Instruction(Operation.call,funcOff));
                        int ret_num = funcSymbol.getRet_num();
                        if (ret_num>0) {
                            localInstructions.add(new Instruction(Operation.store_64, ret_num));
                        }else {
                            throw new AnalyzeError(ErrorCode.NoEnd,peek().getStartPos());
                        }
                    }
                    expect(TokenType.R_PAREN);
                }else {
                    analyseAddMinusExpr();
                    /* 对于赋值语句来说这里需要存值！ */
                    if (isInFunc) {
                        localInstructions.add(new Instruction(Operation.store_64));
                    } else {
                        globalInstructions.add(new Instruction(Operation.store_64));
                    }
                }
            }else if(nextIf(TokenType.L_PAREN)!=null){
                /*说明这是一个函数说明语句，对函数的一个调用，后面可能跟着运算符，所以要判断一下*/
                /*这里的EmptyExpr并不是空语句 */
                /*就单独一个函数式的情况*/
                int funcOff;
                var funcSymbol = funcTable.get(name);
                if (funcSymbol == null){
                    /* 在isStandardFunc函数里面能够处理标准库函数，已经POP了 */
                    boolean isStd= isStandardFunc(name,true);
                    if (!isStd){
                        throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
                    }
                }else {
                    //不是标准库函数，进行栈处理
                    if (isInFunc) {
                        int ret_num = funcSymbol.getRet_num();
                        localInstructions.add(new Instruction(Operation.stackalloc,ret_num));
                    } else {
                        throw new AnalyzeError(ErrorCode.InvalidInput, peek().getStartPos());
                    }
                }
                //说明不是标准库函数
                if (funcSymbol!=null){
                    //进行参数处理！
                    //对表达式进行分析;对于函数参数的空式子不用popn
                    int paramNum = funcSymbol.getParam_num();
                    while (paramNum-->0) {
                        analyseEmptyExpr();
                    }
                    funcOff = funcSymbol.getFuncOffset();
                    localInstructions.add(new Instruction(Operation.call,funcOff));
                    int ret_num = funcSymbol.getRet_num();
                    if (ret_num>0) {
                        localInstructions.add(new Instruction(Operation.popn, ret_num));
                    }
                }
                expect(TokenType.R_PAREN);
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
                    switch (CompareSymbol.getTokenType()){
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
                    localInstructions.add(new Instruction(Operation.popn,1));
                }else {
                    //这里是一个空的运算式
                    if (isInFunc){
                        int localOff;
                        if (localSymbolTable.get(name)==null){
                            throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
                        }
                        localOff = localSymbolTable.get(name).getStackOffset();
                        localInstructions.add(new Instruction(Operation.loca, localOff));
                        localInstructions.add(new Instruction(Operation.load_64));
                    }else {
                        //全局没有空运算式;
                        throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
                    }
                    analyseEmptyExpr();
                    localInstructions.add(new Instruction(Operation.popn,1));
                }
            }
        }else{
            //这里也是单独一个计算式，不过可能第一个不是变量，而是一个数字；
            analyseCompareExpr(true);
            if (isInFunc){
                localInstructions.add(new Instruction(Operation.popn,1));
            }
        }
    }

    //空式子和函数参数式子中的运算式
    //用于引导空语句和函数参数式子中的运算式；
    //TODO:只考虑了Int
    private void analyseEmptyExpr() throws CompileError {
        var OptToken = next();
        switch (OptToken.getTokenType()){
            case PLUS:
                analyseAddMinusExpr();
                localInstructions.add(new Instruction(Operation.add_i));
                break;
            case MINUS:
                analyseAddMinusExpr();
                localInstructions.add(new Instruction(Operation.sub_i));
                break;
            case MUL:
                analyseMultiDivExpr();
                localInstructions.add(new Instruction(Operation.mul_i));
                break;
            case DIV:
                analyseMultiDivExpr();
                localInstructions.add(new Instruction(Operation.div_i));
                break;
            case AS_KW:
                //TODO:只考虑了合法的类型转换
                int type = analyseTy();
                if (type==1) {
                    localInstructions.add(new Instruction(Operation.ftoi));
                }else if (type == 2){
                    localInstructions.add(new Instruction(Operation.itof));
                }else{
                    throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
                }
                break;
            default:
                throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
        }
    }

    //判断语句
    private void analyseCompareExpr(boolean isEmptyStat) throws CompileError{
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
            if (!isEmptyStat) {
                localInstructions.add(new Instruction(Operation.br_true, 1));
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
        if (check(TokenType.MINUS)){
            next();
            negate = true;
        }
        if (check(TokenType.L_PAREN)){
            next();
            analyseAddMinusExpr();
            expect(TokenType.R_PAREN);
        }else if (check(TokenType.UINT_LITERAL)||check(TokenType.DOUBLE_LITERAL)||check(TokenType.STRING_LITERAL)||check(TokenType.CHAR_LITERAL)){
            if (check(TokenType.UINT_LITERAL)){
                var intToken = expect(TokenType.UINT_LITERAL);
                int intNum = (int) intToken.getValue();
                if (isInFunc){
                    localInstructions.add(new Instruction(Operation.push,intNum));
                }else{
                    globalInstructions.add(new Instruction(Operation.push,intNum));
                }
            }else if (check(TokenType.DOUBLE_LITERAL)){
                //TODO:拓展部分，需要考虑double情况！
                next();
            }else if (check(TokenType.STRING_LITERAL)){
                //对字符串String的处理
                //对于String类型，只会出现在putStr中，而且String要加入到全局变量表当中;
                var strToken = expect(TokenType.STRING_LITERAL);
                String strName = (String) strToken.getValue();
                addGlobalSymbol(strName,false,true,true,0,peek().getStartPos());
                globalName.add(strName);
                //获取当前全局变量表的偏移量;
                int globalOff = globalSymbolTable.size()-1;
                if (isInFunc){
                    localInstructions.add(new Instruction(Operation.push,globalOff));
                }else{
                    //putstr函数一定在某一个函数当中出现，不可能作为全局变量出现，所以应该报错;
                    throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
                }
            }else if (check(TokenType.CHAR_LITERAL)){
                //TODO:拓展部分，需要考虑char情况！
                next();
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
                    if (!isStd){
                        throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
                    }
                }else{
                    //这里就不是库函数了，而是自己定义的函数
                    int ret_num = funcSymbol.getRet_num();
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
                        next();
                    }
                    //这里还应该有call操作啊~
                    int funcOff = funcSymbol.getFuncOffset();
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
                    if (localSymbol!=null){
                        if (!localSymbol.isInitialized){
                            //标识符没初始化
                            throw new AnalyzeError(ErrorCode.NotInitialized,peek().getStartPos());
                        }
                        var localOff = localSymbol.getStackOffset();
                        localInstructions.add(new Instruction(Operation.loca,localOff));
                        localInstructions.add(new Instruction(Operation.load_64));
                    }else if (paramSymbol!=null){
                        var paramOff = paramSymbol.getStackOffset();
                        localInstructions.add(new Instruction(Operation.arga,paramOff));
                        localInstructions.add(new Instruction(Operation.load_64));
                    }else {
                        throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
                    }

                }else{
                    var globalSymbol = globalSymbolTable.get(name);
                    var paramSymbol = paramTable.get(name);
                    if (globalSymbol!=null){
                        if (!globalSymbol.isInitialized){
                            //标识符没初始化
                            throw new AnalyzeError(ErrorCode.NotInitialized,peek().getStartPos());
                        }
                        var globalOff = globalSymbol.getStackOffset();
                        globalInstructions.add(new Instruction(Operation.loca,globalOff));
                        globalInstructions.add(new Instruction(Operation.load_64));
                    }else if (paramSymbol!=null){
                        var paramOff = paramSymbol.getStackOffset();
                        globalInstructions.add(new Instruction(Operation.arga,paramOff));
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
    //处理标准库函数
    private boolean isStandardFunc(String name,boolean EmptyNoRet) throws CompileError{
        int globalOff;
        switch (name){
            case "getint":
            case "getchar":
                if (isInFunc){
                    addGlobalSymbol(name,false,true,true,0,peek().getStartPos());
                    globalName.add(name);
                    localInstructions.add(new Instruction(Operation.stackalloc,1));
                    globalOff = globalSymbolTable.size()-1;
                    localInstructions.add(new Instruction(Operation.callname,globalOff));
                    if (EmptyNoRet){
                        localInstructions.add(new Instruction(Operation.popn,1));
                    }
//                    else{
//                        localInstructions.add(new Instruction(Operation.store_64));
//                    }
                }else{
                    addGlobalSymbol(name,false,true,true,0,peek().getStartPos());
                    globalName.add(name);
                    globalInstructions.add(new Instruction(Operation.stackalloc,1));
                    globalOff = globalSymbolTable.size()-1;
                    globalInstructions.add(new Instruction(Operation.callname,globalOff));
                    if (EmptyNoRet){
                        throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
                    }
//                    else{
//                        globalInstructions.add(new Instruction(Operation.store_64));
//                    }
                }
                return true;
            case "getdouble":
            case "putdouble":
                //TODO:double实现
                return true;
            case "putint":
            case "putchar":
            case "putstr":
                if (isInFunc){
                    localInstructions.add(new Instruction(Operation.stackalloc,0));
                    analyseAddMinusExpr();
                    addGlobalSymbol(name,false,true,true,0,peek().getStartPos());
                    globalName.add(name);
                    globalOff = globalSymbolTable.size()-1;
                    localInstructions.add(new Instruction(Operation.callname,globalOff));
                }else {
                    throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
                }
                return  true;
            case "putln":
                if (isInFunc){
                    localInstructions.add(new Instruction(Operation.stackalloc,0));
                    addGlobalSymbol(name,false,true,true,0,peek().getStartPos());
                    globalName.add(name);
                    globalOff = globalSymbolTable.size()-1;
                    localInstructions.add(new Instruction(Operation.callname,globalOff));
                }else {
                    throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
                }
                return true;
            default:
                return false;
        }
    }
}
