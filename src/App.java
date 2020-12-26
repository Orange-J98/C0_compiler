import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
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
        StringBuilder Binary = new StringBuilder();
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

//        //转换成二进制！
//        int [] maigicVersion ={0x72,0x30,0x3b,0x3e,0,0,0,0x01};
//        for (int num:maigicVersion){
//            Binary.append(toBinary_8(num));
//        }
//        Binary.append(toBinary_32(analyzer.getGlobalCounts()));
//        for(SymbolEntry tempGlobalEntry:globalTable.values()){
//            if (tempGlobalEntry.isConstant()){
//                Binary.append(toBinary_8(1));
//            }else{
//                Binary.append(toBinary_8(0));
//            }
//            Binary.append(toBinary_32(tempGlobalEntry.getGlobal_count()));
//            if (tempGlobalEntry.getGlobal_value().equals("")){
//                Binary.append(toBinary_64(0));
//            }else{
//                Binary.append(StrToBinary(tempGlobalEntry.getGlobal_value()));
//            }
//        }
//        Binary.append(toBinary_32(funcTable.size()));
//        for (FuncEntry funcEntry:funcTable.values()){
//            Binary.append(toBinary_32(funcEntry.getFunc_name()));
//            Binary.append(toBinary_32(funcEntry.getRet_num()));
//            Binary.append(toBinary_32(funcEntry.getParam_num()));
//            Binary.append(toBinary_32(funcEntry.getLocVarNum()));
//            Binary.append(toBinary_32(funcEntry.getBodyCnt()));
//            for (Instruction instruction:funcEntry.getInstructions()){
//                if (instruction.getX()==-1||instruction.getX()==null) {
//                    Binary.append(toBinary_8(instruction.getOptNum()));
//                }else {
//                    Binary.append(toBinary_8(instruction.getOptNum()));
//                    Binary.append(toBinary_64(instruction.getX()));
//                }
//            }
//        }
//        output.print(Binary);
        //输出指令集
        for (FuncEntry funcEntry:funcTable.values()){
            output.println("fn "+getKey(funcTable,funcEntry.getFunc_name())+" ["+globalTable.get(getKey(funcTable,funcEntry.getFunc_name())).getStackOffset()+"] "+funcEntry.getFuncOffset()+" "+funcEntry.getParam_num()+" -> "+funcEntry.getRet_num());
            for (Instruction instruction : funcEntry.getInstructions()) {
                output.println(instruction.toString());
            }
            output.println();
        }
    }
    public static String getKey(HashMap<String,FuncEntry> map, int value){
        for(Object key: map.keySet()){
            if(map.get(key).getFunc_name()==value){
                return (String) key;
            }
        }
        return "";
    }


    private static Tokenizer tokenize(StringIter iter) {
        var tokenizer = new Tokenizer(iter);
        return tokenizer;
    }
    private  static String toBinary_8 (int num)
    {
        String binary_8=Integer.toBinaryString(num);
        int bit = 8-binary_8.length();
        if(binary_8.length()<8){
            for(int j=0; j<bit; j++){
                binary_8 = "0"+binary_8;
            }
        }
        return binary_8;
    }

    private  static String toBinary_32 (int num)
    {
        String binary_32=Integer.toBinaryString(num);
        int bit = 32-binary_32.length();
        if(binary_32.length()<32){
            for(int j=0; j<bit; j++){
                binary_32 = "0"+binary_32;
            }
        }
        return binary_32;
    }

    private  static String toBinary_64 (int num)
    {
        String binary_64=Integer.toBinaryString(num);
        int bit = 64-binary_64.length();
        if(binary_64.length()<64){
            for(int j=0; j<bit; j++){
                binary_64 = "0"+binary_64;
            }
        }
        return binary_64;
    }
    private static String StrToBinary(String str) {
        char[] strChar = str.toCharArray();
        String result = "";
        for (int i = 0; i < strChar.length; i++) {
            result += Integer.toBinaryString(strChar[i]);
        }
        return result;
    }
}