package tokenizer;
import error.*;
import  util.Pos;
import java.util.regex.*;

public class Tokenizer {
    private StringIter it;
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
        }else if(peek=='/'){
            return lexComent();
        }else{
            return OperatorOrUnknow();
        }
    }
    //无符号整数或浮点数
    private Token UIntOrDouble() throws TokenizeError{
        String token = "";
        Pos startpos1 = it.currentPos();
        while (Character.isDigit(it.peekChar())) {
            token += it.nextChar();
        }
        boolean flag = false;
        if(it.peekChar()=='.'){
            flag =true;
            token += it.nextChar();
            while (Character.isDigit(it.peekChar())) {
                token += it.nextChar();
            }
            if(it.peekChar()=='e'||it.peekChar()=='E'){
                token+=it.nextChar();
                if(it.peekChar()=='+'||it.peekChar()=='-'){
                    token+=it.nextChar();
                }
                while (Character.isDigit(it.peekChar())) {
                    token += it.nextChar();
                }
            }
        }

        Pos endpos1 = it.currentPos();
        token = removeZero(token);
        if (token.equals("")) {
            token = "0";
        }

        if(flag){
            double num = Double.parseDouble(token);
            return new Token(TokenType.DOUBLE_LITERAL,num,startpos1,endpos1);
        }else {
            int num = Integer.parseInt(token);
            return new Token(TokenType.UINT_LITERAL,num, startpos1, endpos1);
        }
    }

    //识别关键字和标识符
    private Token IdentOrKeyword() throws TokenizeError{
        String token = "";
        Pos startpos = it.currentPos();
        while(Character.isAlphabetic(it.peekChar())||Character.isDigit(it.peekChar())||it.peekChar()=='_'){
            token += it.nextChar();
        }
        Pos endpos = it.currentPos();
        switch (token){
            case "fn":
                return new Token(TokenType.FN_KW, token, startpos, endpos);
            case "let":
                return new Token(TokenType.LET_KW, token, startpos, endpos);
            case "const":
                return new Token(TokenType.CONST_KW, token, startpos, endpos);
            case "as":
                return new Token(TokenType.AS_KW, token, startpos, endpos);
            case "while":
                return new Token(TokenType.WHILE_KW, token, startpos, endpos);
            case "if":
                return new Token(TokenType.IF_KW, token, startpos, endpos);
            case "else":
                return new Token(TokenType.ELSE_KW, token, startpos, endpos);
            case "return":
                return new Token(TokenType.RETURN_KW, token, startpos, endpos);
            case "break":
                return new Token(TokenType.BREAK_KW, token, startpos, endpos);
            case "continue":
                return new Token(TokenType.CONTINUE_KW, token, startpos, endpos);
            default:
                return new Token(TokenType.IDENT,token,startpos,endpos);
        }
    }

    private Token StringOrChar() throws TokenizeError{
        String token = "";
        Pos startpos1 =it.currentPos();
        //字符串String
        if (it.peekChar() == '\"'){
            it.nextChar();
            while(it.peekChar()!='\"'){
                token = getEscapeSequence(token);
            }
            if (it.peekChar()=='\"'){
                it.nextChar();
                Pos endpos1 = it.currentPos();
                return new Token(TokenType.STRING_LITERAL,token,startpos1,endpos1);
            }else{
                throw new TokenizeError(ErrorCode.InvalidInput,it.previousPos());
            }
        }else if(it.peekChar() == '\''){
            //char字符串
            it.nextChar();
            while(it.peekChar()!='\''){
                token = getEscapeSequence(token);
            }
            if (it.peekChar()=='\''){
                it.nextChar();
                Pos endpos1 = it.currentPos();
                return new Token(TokenType.CHAR_LITERAL,token,startpos1,endpos1);
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
            token+=it.nextChar();
            if(it.peekChar()=='\\'||it.peekChar()=='\"'||it.peekChar()=='\''||it.peekChar()=='n'||it.peekChar()=='r'||it.peekChar()=='t'){
                token+=it.nextChar();
                return token;
            }else{
                throw new TokenizeError(ErrorCode.InvalidInput,it.previousPos());
            }
        }
        token += it.nextChar();
        return token;
    }

    //识别注释
    private Token lexComent() throws TokenizeError{
        String token = "";
        Pos startpos1 = it.currentPos();
        token+=it.nextChar();
        if(it.peekChar()=='/'){
            token+=it.nextChar();
        }else{
            throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
        }
        while(it.peekChar()!='\n'){
            token+=it.nextChar();
        }
        if(it.peekChar()=='\n'){
            token+=it.nextChar();
        }
        Pos endpos1 = it.currentPos();
        return new Token(TokenType.COMMENT,token,startpos1,endpos1);
    }

    //识别常量
    private Token OperatorOrUnknow() throws TokenizeError{
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
