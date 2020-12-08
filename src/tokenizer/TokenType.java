package tokenizer;

public enum TokenType {

    /** EOF */
    EOF,
    /*关键字*/

    /**'fn'*/
    FN_KW,
    /**'let'*/
    LET_KW,
    /**'const'*/
    CONST_KW,
    /**'as'*/
    AS_KW,
    /**'while'*/
    WHILE_KW,
    /**'if'*/
    IF_KW,
    /**'else'*/
    ELSE_KW,
    /**'return'*/
    RETURN_KW,

// 这两个是扩展 c0 的
    /**'break'*/
    BREAK_KW,
    /**'continue'*/
    CONTINUE_KW,

    /*字面量*/

    /**[0-9]数字  digit,
     digit+ 无符号整数*/
    UINT_LITERAL,
    /** '\' [\\"'nrt] 转义字符 [\、"、'、n、r、t]
    escape_sequence,
    [^"\\] 除"和\外所有的字符串
    string_regular_char,
    '"' (string_regular_char | escape_sequence)* '"' */
    STRING_LITERAL,

// 扩展 c0
    /** digit+ '.' digit+ ([eE] [+-]? digit+)? */
    DOUBLE_LITERAL,
    /**  [^'\\]
    char_regular_char,
    '\'' (char_regular_char | escape_sequence) '\''  */
    CHAR_LITERAL,

    /*标识符*/

    /** [_a-zA-Z] [_a-zA-Z0-9] */
    IDENT,

    /*运算符*/
    /** '+' */
    PLUS,
    /** '-' */
    MINUS,
    /** '*' */
    MUL,
    /** '/' */
    DIV,
    /** '=' */
    ASSIGN,
    /** '==' */
    EQ,
    /** '!=' */
    NEQ,
    /** '<' */
    LT,
    /** '>' */
    GT,
    /** '<=' */
    LE,
    /** '>=' */
    GE,
    /** '(' */
    L_PAREN,
    /** ')' */
    R_PAREN,
    /** '{' */
    L_BRACE,
    /** '}' */
    R_BRACE,
    /** '->' */
    ARROW,
    /** ',' */
    COMMA,
    /** ':' */
    COLON,
    /** ';' */
    SEMICOLON,
    /***/

    /*注释*/
    /** '//' regex(.*) '\n' */
    COMMENT,
}
