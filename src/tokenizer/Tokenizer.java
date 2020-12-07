package tokenizer;
import error.*;
import  util.Pos;

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

    }
    private Token lexComent() throws TokenizeError{

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
