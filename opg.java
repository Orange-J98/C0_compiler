import jdk.dynalink.beans.StaticClass;

import java.io.File;
import java.io.FileReader;
import java.util.Stack;

public class opg {
    private int a[][] = {{1, 0, 0, 0, 1}, {1, 1, 0, 0, 1}, {1, 1, -1, -1, 1}, {0, 0, 0, 0, 0}, {1, 1, -1, -1, 1}};
    private char ch = ' ';
    private Stack<Character> symbol = new Stack<Character>();

    private int index(char ch) {
        switch (ch) {
            case '+':
                return 0;
            case '*':
                return 1;
            case 'i':
                return 2;
            case '(':
                return 3;
            case ')':
                return 4;
            default:
                return -1;
        }
    }

    //返回值为0则移进，为1则规约
    private int judge(char ch) {
        if (index(ch) < 0) {
            return -1;
        } else {
            Stack<Character> temp_stack = (Stack<Character>) symbol.clone();
            if (!temp_stack.isEmpty()) {
                while (index(temp_stack.peek()) < 0) {
                    temp_stack.pop();
                    if (temp_stack.empty()) {
                        return 0;
                    }
                }
                if (a[index(temp_stack.peek())][index(ch)] == 1) {
                    temp_stack.pop();
                    return 1;
                } else if (a[index(temp_stack.peek())][index(ch)] == 0) {
                    temp_stack.pop();
                    return 0;
                } else {
                    return -1;
                }
            } else {
                return 0;
            }
        }
    }

    private int sp() {
        if (symbol.peek() == 'i') {
            symbol.pop();
            //成功规约，输出R
            System.out.println("R");
            symbol.push('E');
            return 1;
        } else {
            if (symbol.peek() == 'E') {
                symbol.pop();
                if(symbol.isEmpty()){
                    return 1;
                }
                if (symbol.peek() == '+' || symbol.peek() == '*') {
                    symbol.pop();
                    if (symbol.pop() == 'E') {
                        symbol.push('E');
                        //成功规约，输出R
                        System.out.println("R");
                        return 1;
                    } else {
                        //规约失败
                        System.out.println("RE");
                        return -1;
                    }
                } else {
                    //规约失败
                    if(symbol.peek()=='('){
                        System.out.println("E");
                        return -1;
                    }else {
                        System.out.println("RE");
                        return -1;
                    }
                }
            } else if (symbol.pop() == ')') {
                if (symbol.pop() == 'E') {
                    if (symbol.pop() == '(') {
                        symbol.push('E');
                        //成功规约，输出R
                        System.out.println("R");
                        return 1;
                    } else {
                        //规约失败
                        System.out.println("RE");
                        return -1;
                    }
                } else {
                    //规约失败
                    System.out.println("RE");
                    return -1;
                }
            } else {
                //规约失败
                System.out.println("RE");
                return -1;
            }
        }
    }

    public void analyze(char[] chs) {
        for (int i = 0; i < chs.length; i++) {
            ch = chs[i];
            if (ch == ' ') {
                continue;
            }
            if (ch == '\n' || ch == '\r' || ch == '\t') {
                //规约
                while (!symbol.isEmpty()) {
                    int num =sp();
                    if(num==-1){
                        return;
                    }
                }
            } else if (judge(ch) == 1) {
                //规约
                int temp_num = sp();
                i--;
                if (temp_num == 1) {
                    continue;
                } else if (temp_num == -1) {
                    return;
                }
            } else if (judge(ch) == 0) {
                //移进,移进非终结符，I{压入的符号}
                System.out.println("I" + ch);
                symbol.push(ch);
            } else {
                //对于不能识别或无法比较符号优先关系的栈顶和读入符号，输出一行 E *(2)*。
                //这里已经输出过E了
                System.out.println("E");
                return;
            }
        }
        while (!symbol.isEmpty()) {
            int num =sp();
            if(num==-1){
                return;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        File file = new File(args[0]);
        FileReader reader = new FileReader(file);
        int length = (int) file.length();
        char buf[] = new char[length + 1];
        buf[buf.length - 1] = ' ';
        reader.read(buf);
        reader.close();
        new opg().analyze(buf);
    }
}
