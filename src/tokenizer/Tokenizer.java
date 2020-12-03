package tokenizer;
import  util.Pos;

public class Tokenizer {
    private StringIter it;
    public Tokenizer(StringIter it){ this.it = it;}
}
    /**
     * 获取下一个 Token
     *
     * @return
     * @throws TokenizeError 如果解析有异常则抛出
     */