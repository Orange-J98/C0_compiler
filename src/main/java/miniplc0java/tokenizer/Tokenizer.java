package miniplc0java.tokenizer;
import miniplc0java.error.*;
import  miniplc0java.util.Pos;

public class Tokenizer {
    private final StringIter it;
    public Tokenizer(StringIter it){ this.it = it;}

    /**
     * 获取下一个 Token
     *
     * @return
     * //@throws TokenizeError 如果解析有异常则抛出
     */
    public Token nextToken() throws TokenizeError {
        it.readAll();

        // 跳过之前的所有空白字符
        skipSpaceCharacters();

        if(it.isEOF()){
            return new Token(TokenType.EOF,"",it.currentPos(),it.currentPos());
        }
        char peek = it.peekChar();

        if(Character.isDigit(peek)){
            return UIntOrDouble();
        }else if(Character.isAlphabetic(peek)||peek=='_'){
            return IdentOrKeyword();
        }else if(peek=='"'||peek=='\''){
            return StringOrChar();
        }else{
            return OperatorOrUnknown();
        }
    }
    //无符号整数或浮点数
    private Token UIntOrDouble() {
        StringBuilder token = new StringBuilder();
        Pos startpos1 = it.currentPos();
        while (Character.isDigit(it.peekChar())) {
            token.append(it.nextChar());
        }
        boolean flag = false;
        if(it.peekChar()=='.'){
            flag =true;
            token.append(it.nextChar());
            while (Character.isDigit(it.peekChar())) {
                token.append(it.nextChar());
            }
            if(it.peekChar()=='e'||it.peekChar()=='E'){
                token.append(it.nextChar());
                if(it.peekChar()=='+'||it.peekChar()=='-'){
                    token.append(it.nextChar());
                }
                while (Character.isDigit(it.peekChar())) {
                    token.append(it.nextChar());
                }
            }
        }

        Pos endpos1 = it.currentPos();
        token = new StringBuilder(removeZero(token.toString()));
        if (token.toString().equals("")) {
            token = new StringBuilder("0");
        }

        if(flag){
            double num = Double.parseDouble(token.toString());
            return new Token(TokenType.DOUBLE_LITERAL,num,startpos1,endpos1);
        }else {
            long num = Long.parseLong(token.toString());
            return new Token(TokenType.UINT_LITERAL,num, startpos1, endpos1);
        }
    }

    //识别关键字和标识符
    private Token IdentOrKeyword() {
        StringBuilder token = new StringBuilder();
        Pos startPos = it.currentPos();
        while(Character.isAlphabetic(it.peekChar())||Character.isDigit(it.peekChar())||it.peekChar()=='_'){
            token.append(it.nextChar());
        }
        Pos endPos = it.currentPos();
        return switch (token.toString()) {
            case "fn" -> new Token(TokenType.FN_KW, token.toString(), startPos, endPos);
            case "let" -> new Token(TokenType.LET_KW, token.toString(), startPos, endPos);
            case "const" -> new Token(TokenType.CONST_KW, token.toString(), startPos, endPos);
            case "as" -> new Token(TokenType.AS_KW, token.toString(), startPos, endPos);
            case "while" -> new Token(TokenType.WHILE_KW, token.toString(), startPos, endPos);
            case "if" -> new Token(TokenType.IF_KW, token.toString(), startPos, endPos);
            case "else" -> new Token(TokenType.ELSE_KW, token.toString(), startPos, endPos);
            case "return" -> new Token(TokenType.RETURN_KW, token.toString(), startPos, endPos);
            case "break" -> new Token(TokenType.BREAK_KW, token.toString(), startPos, endPos);
            case "continue" -> new Token(TokenType.CONTINUE_KW, token.toString(), startPos, endPos);
            default -> new Token(TokenType.IDENT, token.toString(), startPos, endPos);
        };
    }

    private Token StringOrChar() throws TokenizeError{
        String token = "";
        Pos startPos1 =it.currentPos();
        //字符串String
        if (it.peekChar() == '\"'){
            it.nextChar();
            while(it.peekChar()!='\"'){
                token = getEscapeSequence(token);
                if(it.isEOF()){
                    return new Token(TokenType.EOF,"",it.currentPos(),it.currentPos());
                }
            }
            if (it.peekChar()=='\"'){
                it.nextChar();
                Pos endpos1 = it.currentPos();
                return new Token(TokenType.STRING_LITERAL,token,startPos1,endpos1);
            }else{
                throw new TokenizeError(ErrorCode.InvalidInput,it.previousPos());
            }
        }else if(it.peekChar() == '\''){
            //char字符串
            it.nextChar();
            char charToken = 0;
            while(it.peekChar()!='\''){
                charToken = getEscapeSequence(token).charAt(0);
            }
            if (it.peekChar()=='\''){
                it.nextChar();
                Pos endpos1 = it.currentPos();
                return new Token(TokenType.CHAR_LITERAL,charToken,startPos1,endpos1);
            }else{
                throw new TokenizeError(ErrorCode.InvalidInput,it.previousPos());
            }
        }else{
            throw new TokenizeError(ErrorCode.InvalidInput,it.previousPos());
        }
    }
    //识别转义字符
    private String getEscapeSequence(String token) throws TokenizeError {
        if(it.peekChar()=='\\'){
            it.nextChar();
            switch (it.peekChar()) {
                case '\\' -> {
                    token += '\\';
                    it.nextChar();
                    return token;
                }
                case '"' -> {
                    token += '\"';
                    it.nextChar();
                    return token;
                }
                case '\'' -> {
                    token += '\'';
                    it.nextChar();
                    return token;
                }
                case 'n' -> {
                    token += '\n';
                    it.nextChar();
                    return token;
                }
                case 't' -> {
                    token += '\t';
                    it.nextChar();
                    return token;
                }
                case 'r' -> {
                    token += '\r';
                    it.nextChar();
                    return token;
                }
                default -> throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
            }
        }
        token += it.nextChar();
        return token;
    }


    //识别常量
    private Token OperatorOrUnknown() throws TokenizeError{
        switch (it.nextChar()){
            case '+':
                return new Token(TokenType.PLUS, '+', it.previousPos(), it.currentPos());
            case '-':
                if(it.peekChar()=='>'){
                    it.nextChar();
                    return new Token(TokenType.ARROW, "->", it.previousPos(), it.currentPos());
                }else{
                    return new Token(TokenType.MINUS, '-', it.previousPos(), it.currentPos());
                }
            case '*':
                return new Token(TokenType.MUL, '*', it.previousPos(), it.currentPos());
            case '/':
                if (it.peekChar()=='/'){
                    it.nextChar();
                    while (it.peekChar()!='\n'){
                        it.nextChar();
                    }
                    return nextToken();
                }
                return new Token(TokenType.DIV, '/', it.previousPos(), it.currentPos());
            case '=':
                if (it.peekChar()=='='){
                    it.nextChar();
                    return new Token(TokenType.EQ,"==",it.previousPos(),it.currentPos());
                }else{
                    return new Token(TokenType.ASSIGN, '=', it.previousPos(), it.currentPos());
                }
            case '!':
                if (it.peekChar()=='='){
                    it.nextChar();
                    return new Token(TokenType.NEQ,"!=",it.previousPos(),it.currentPos());
                }else{
                    throw new TokenizeError(ErrorCode.InvalidInput,it.previousPos());
                }
            case '<':
                if (it.peekChar()=='='){
                    it.nextChar();
                    return new Token(TokenType.LE, "<=", it.previousPos(), it.currentPos());
                }else{
                    return new Token(TokenType.LT, '<', it.previousPos(), it.currentPos());
                }
            case '>':
                if (it.peekChar()=='='){
                    it.nextChar();
                    return new Token(TokenType.GE,">=",it.previousPos(),it.currentPos());
                }else{
                    return new Token(TokenType.GT, '>', it.previousPos(), it.currentPos());
                }
            case '(':
                return new Token(TokenType.L_PAREN, '(', it.previousPos(), it.currentPos());
            case ')':
                return new Token(TokenType.R_PAREN, ')', it.previousPos(), it.currentPos());
            case '{':
                return new Token(TokenType.L_BRACE, '{', it.previousPos(), it.currentPos());
            case '}':
                return new Token(TokenType.R_BRACE, '}', it.previousPos(), it.currentPos());
            case ',':
                return new Token(TokenType.COMMA, ',', it.previousPos(), it.currentPos());
            case ':':
                return new Token(TokenType.COLON, ':', it.previousPos(), it.currentPos());
            case ';':
                return new Token(TokenType.SEMICOLON, ';', it.previousPos(), it.currentPos());
            default:
                throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
        }
    }

    private void skipSpaceCharacters() {
        while (!it.isEOF() && Character.isWhitespace(it.peekChar())) {
            it.nextChar();
        }
    }
    private String removeZero(String str) {
        int len = str.length();
        int i = 0;
        while (i < len && str.charAt(i) == '0') {
            i++;
        }
        return str.substring(i);
    }
}
