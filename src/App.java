import java.io.*;
import java.util.*;

import analyser.*;
import error.CompileError;
import instruction.Instruction;
import tokenizer.StringIter;
import tokenizer.Token;
import tokenizer.TokenType;
import tokenizer.Tokenizer;


public class App {
    public static void main(String[] args) throws CompileError {
        var inputFileName = "C:\\Users\\Arno_ZH\\Desktop\\workspace\\WordAnalyze\\input.txt";
        var outputFileName = "C:\\Users\\Arno_ZH\\Desktop\\workspace\\WordAnalyze\\output.txt";
        InputStream input;
        PrintStream output;

        try {
            input = new FileInputStream(inputFileName);
        } catch (FileNotFoundException e) {
            System.err.println("Cannot find input file.");
            e.printStackTrace();
            System.exit(-1);
            return;
        }
        try {
            output = new PrintStream(new FileOutputStream(outputFileName));
        } catch (FileNotFoundException e) {
            System.err.println("Cannot open output file.");
            e.printStackTrace();
            System.exit(-1);
            return;
        }
        Scanner scanner;
        scanner = new Scanner(input);
        var iter = new StringIter(scanner);

        var tokenizer = tokenize(iter);
        var analyzer = new Analyser(tokenizer);
        List<Instruction> instructions;
        HashMap<String, FuncEntry> funcTable= new HashMap<>();
        var tokens = new ArrayList<Token>();
        try {
            funcTable = analyzer.analyse();
            while (true) {
                var token = tokenizer.nextToken();
                if (token.getTokenType().equals(TokenType.EOF)) {
                    break;
                }
                tokens.add(token);
            }
        } catch (Exception e) {
            // 遇到错误不输出，直接退出
            e.printStackTrace();
            System.err.println(e);
            System.exit(-1);
            return;
        }
        HashMap<String,SymbolEntry> globalTable= analyzer.getGlobalSymbolTable();
        ArrayList<String> funcName = analyzer.getFuncName();
        ArrayList<String> globalName = analyzer.getGlobalName();
        ArrayList<Byte> Binary = new ArrayList<>();
        //转换成二进制！
        int [] maigicVersion ={0x72,0x30,0x3b,0x3e,0x0,0x0,0x0,0x01};
        for (int num:maigicVersion){
            toBinary_8(Binary,num);
        }
        toBinary_32(Binary, globalName.size());
        for(String tempGlobalName:globalName){
            SymbolEntry tempGlobalEntry = globalTable.get(tempGlobalName);
            if (tempGlobalEntry.isConstant()){
                toBinary_8(Binary,1);
            }else{
                toBinary_8(Binary,0);
            }
            toBinary_32(Binary,tempGlobalEntry.getGlobal_count());
            if (tempGlobalEntry.getGlobal_value().equals("")){
                toBinary_64(Binary,0);
            }else{
                StrToBinary(Binary,tempGlobalEntry.getGlobal_value());
            }
        }
        toBinary_32(Binary, funcName.size());
        for (String tempFuncName:funcName){
            FuncEntry funcEntry = funcTable.get(tempFuncName);
            toBinary_32(Binary,funcEntry.getFunc_name());
            toBinary_32(Binary,funcEntry.getRet_num());
            toBinary_32(Binary,funcEntry.getParam_num());
            toBinary_32(Binary,funcEntry.getLocVarNum());
            toBinary_32(Binary,funcEntry.getBodyCnt());
            for (Instruction instruction:funcEntry.getInstructions()){
                if (instruction.getX()==-1) {
                    toBinary_8(Binary,instruction.getOptNum());
                }else {
                    toBinary_8(Binary,instruction.getOptNum());
                    if (instruction.getOptNum()== 0x01){
                        toBinary_64(Binary,instruction.getX());
                    }else{
                        toBinary_32(Binary, Long.valueOf(instruction.getX()).intValue());
                    }
                }
            }
        }
        int length = Binary.size();
        byte [] OutputByte = new byte[Binary.size()];
        int i=0;
        for (byte temp:Binary){
            OutputByte[i++] = temp;
        }
        try {
            output.write(OutputByte);
        } catch (IOException e) {
            e.printStackTrace();
        }

        output.println();
        output.println("Global_counts:"+globalName.size());
        for (String globalN: globalName){
            SymbolEntry globalS = globalTable.get(globalN);
            output.println(globalN+": "+globalS.getStackOffset()+"  "+globalS.getGlobal_value());
        }

        output.println("Func_counts:"+funcName.size());
        output.println();

        for (String tempFuncName:funcName){
            FuncEntry tempFuncEntry = funcTable.get(tempFuncName);
            output.println("fn "+tempFuncName+" ["+globalTable.get(tempFuncName).getStackOffset()+"] "+tempFuncEntry.getLocVarNum()+" "+tempFuncEntry.getParam_num()+" -> "+tempFuncEntry.getRet_num());
            for (Instruction instruction : tempFuncEntry.getInstructions()) {
                output.println(instruction.toString());
            }
            output.println();
        }

    }

    private static Tokenizer tokenize(StringIter iter) {
        var tokenizer = new Tokenizer(iter);
        return tokenizer;
    }
    private  static void toBinary_8 (ArrayList<Byte>Binary,int num)
    {
            Binary.add((byte)num);
    }

    private  static void toBinary_32 (ArrayList<Byte>Binary,int num)
    {
        for (int i=3;i>=0;i--) {
            Binary.add((byte) (num >> (8 * i) & 0xff));
        }
    }

    private  static void toBinary_64 (ArrayList<Byte>Binary,long num)
    {
        long newNum = num;
        for (int i=7;i>=0;i--) {
            Binary.add((byte) (newNum >> (8 * i) & 0xff));
        }
    }

    private static void StrToBinary(ArrayList<Byte>Binary,String str) {
        byte[] s = str.getBytes();
        for (byte tempS : s) {
            Binary.add(tempS);
        }
    }
}