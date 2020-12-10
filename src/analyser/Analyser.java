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
    ArrayList<Instruction> instructions;

    /** 当前偷看的 token */
    Token peekedToken = null;

    /** 符号表 */
    HashMap<String, SymbolEntry> symbolTable = new HashMap<>();

    /** 下一个变量的栈偏移 */
    int nextOffset = 0;

    public Analyser(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
        this.instructions = new ArrayList<>();
    }

    public List<Instruction> analyse() throws CompileError {
        analyseProgram();
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

    /**
     * 添加一个符号
     *
     * @param name          名字
     * @param isInitialized 是否已赋值
     * @param isConstant    是否是常量
     * @param curPos        当前 token 的位置（报错用）
     * @throws AnalyzeError 如果重复定义了则抛异常
     */
    private void addSymbol(String name, boolean isInitialized, boolean isConstant, Pos curPos) throws AnalyzeError {
        if (this.symbolTable.get(name) != null) {
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, curPos);
        } else {
            this.symbolTable.put(name, new SymbolEntry(isConstant, isInitialized, getNextVariableOffset()));
        }
    }

    /**
     * 设置符号为已赋值
     *
     * @param name   符号名称
     * @param curPos 当前位置（报错用）
     * @throws AnalyzeError 如果未定义则抛异常
     */
    private void initializeSymbol(String name, Pos curPos) throws AnalyzeError {
        var entry = this.symbolTable.get(name);
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
        var entry = this.symbolTable.get(name);
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
        var entry = this.symbolTable.get(name);
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
        while(check(TokenType.EOF) != true){
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
        if(Objects.requireNonNull(nextIf(TokenType.IDENT)).getValue().equals("void")|| Objects.requireNonNull(nextIf(TokenType.IDENT)).getValue().equals("int")){
            return;
        }else{
            throw new AnalyzeError(ErrorCode.InvalidInput,next().getStartPos());
        }
    }

    //block_stmt -> '{' stmt* '}'

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

//    expr_stmt -> expr ';'

//    decl_stmt -> let_decl_stmt | const_decl_stmt
//        let_decl_stmt -> 'let' IDENT ':' ty ('=' expr)? ';'
//        const_decl_stmt -> 'const' IDENT ':' ty '=' expr ';'

//    if_stmt -> 'if' expr block_stmt ('else' 'if' expr block_stmt)* ('else' block_stmt)?
//    while_stmt -> 'while' expr block_stmt
//    break_stmt -> 'break' ';'
//    continue_stmt -> 'continue' ';'
//    return_stmt -> 'return' expr? ';'
//    block_stmt -> '{' stmt* '}'
//    empty_stmt -> ';'
    private void analyseStmt() throws CompileError{
        /** 还没实现！*/
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
        expect(TokenType.L_BRACE);
    }
    private boolean checkNextIfStmt() throws CompileError{
        if(checkNextifExpr()){
            return true;
        }
        var nextToken = peek();
        switch (nextToken.getTokenType()){
            case LET_KW:
                return true;
            case CONST_KW:
                return true;
            case IF_KW:
                return true;
            case WHILE_KW:
                return true;
            case BREAK_KW:
                return true;
            case CONTINUE_KW:
                return true;
            case RETURN_KW:
                return true;
            case L_PAREN:
                return true;
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
    private boolean checkNextifExpr() throws CompileError{
        var nextToken = peek();
        switch (nextToken.getTokenType()){
            case MINUS:
                return true;
            case IDENT:
                return true;
            case UINT_LITERAL:
                return true;
            case DOUBLE_LITERAL:
                return true;
            case STRING_LITERAL:
                return true;
            case CHAR_LITERAL:
                return true;
            case L_PAREN:
                return true;
            default:
                return false;

        }
    }

}
