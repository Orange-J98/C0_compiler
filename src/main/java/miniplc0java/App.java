package miniplc0java;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import miniplc0java.error.CompileError;
import miniplc0java.instruction.Instruction;
import miniplc0java.tokenizer.StringIter;
import miniplc0java.tokenizer.Token;
import miniplc0java.tokenizer.TokenType;
import miniplc0java.tokenizer.Tokenizer;
import miniplc0java.analyser.*;
import net.sourceforge.argparse4j.*;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentAction;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

public class App {
    public static void main(String[] args) throws CompileError {
        var argparse = buildArgparse();
        Namespace result;
        try {
            result = argparse.parseArgs(args);
        } catch (ArgumentParserException e1) {
            argparse.handleError(e1);
            return;
        }

        var inputFileName = result.getString("input");
        var outputFileName = result.getString("output");

        InputStream input;
        if (inputFileName.equals("-")) {
            input = System.in;
        } else {
            try {
                input = new FileInputStream(inputFileName);
            } catch (FileNotFoundException e) {
                System.err.println("Cannot find input file.");
                e.printStackTrace();
                System.exit(-1);
                return;
            }
        }

        PrintStream output;
        if (outputFileName.equals("-")) {
            output = System.out;
        } else {
            try {
                output = new PrintStream(new FileOutputStream(outputFileName));
            } catch (FileNotFoundException e) {
                System.err.println("Cannot open output file.");
                e.printStackTrace();
                System.exit(-1);
                return;
            }
        }

        Scanner scanner;
        scanner = new Scanner(input);
        var iter = new StringIter(scanner);
        var tokenizer = tokenize(iter);
        var analyzer = new Analyser(tokenizer);
        List<Instruction> instructions;
        StringBuilder Binary = new StringBuilder();
        HashMap<String, FuncEntry> funcTable= new HashMap<>();
        if (result.getBoolean("tokenize")) {
            // tokenize
            var tokens = new ArrayList<Token>();
            try {
                while (true) {
                    var token = tokenizer.nextToken();
                    if (token.getTokenType().equals(TokenType.EOF)) {
                        break;
                    }
                    tokens.add(token);
                }
            } catch (Exception e) {
                // 遇到错误不输出，直接退出
                System.err.println(e);
                System.exit(-1);
                return;
            }
            for (Token token : tokens) {
                output.println(token.toString());
            }
        } else if (result.getBoolean("analyse")) {
            // analyze
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
            //转换成二进制！
            int [] maigicVersion ={0x72,0x30,0x3b,0x3e,0x0,0x0,0x0,0x01};
            for (int num:maigicVersion){
                Binary.append(toBinary_8(num));
            }

            Binary.append(toBinary_32(analyzer.getGlobalCounts()));
            HashMap<String,SymbolEntry> globalTable= analyzer.getGlobalSymbolTable();
            ArrayList<String> funcName = analyzer.getFuncName();
            ArrayList<String> globalName = analyzer.getGlobalName();

            for(String tempGlobalName:globalName){
                SymbolEntry tempGlobalEntry = globalTable.get(tempGlobalName);
                if (tempGlobalEntry.isConstant()){
                    Binary.append(toBinary_8(1));
                }else{
                    Binary.append(toBinary_8(0));
                }
                Binary.append(toBinary_32(tempGlobalEntry.getGlobal_count()));
                if (tempGlobalEntry.getGlobal_value().equals("")){
                    Binary.append(toBinary_64(0));
                }else{
                    Binary.append(StrToBinary(tempGlobalEntry.getGlobal_value()));
                }
            }

            Binary.append(toBinary_32(funcTable.size()));
            for (String tempFuncName:funcName){
                FuncEntry funcEntry = funcTable.get(tempFuncName);
                Binary.append(toBinary_32(funcEntry.getFunc_name()));
                Binary.append(toBinary_32(funcEntry.getRet_num()));
                Binary.append(toBinary_32(funcEntry.getParam_num()));
                Binary.append(toBinary_32(funcEntry.getLocVarNum()));
                Binary.append(toBinary_32(funcEntry.getBodyCnt()));
                for (Instruction instruction:funcEntry.getInstructions()){
                    if (instruction.getX()==-1||instruction.getX()==null) {
                        Binary.append(toBinary_8(instruction.getOptNum()));
                    }else {
                        Binary.append(toBinary_8(instruction.getOptNum()));
                        Binary.append(toBinary_64(instruction.getX()));
                    }
                }
            }
            output.print(Binary);
        } else {
            System.err.println("Please specify either '--analyse' or '--tokenize'.");
            System.exit(-1);
        }
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
    private static ArgumentParser buildArgparse() {
        var builder = ArgumentParsers.newFor("miniplc0-java");
        var parser = builder.build();
        parser.addArgument("-t", "--tokenize").help("Tokenize the input").action(Arguments.storeTrue());
        parser.addArgument("-l", "--analyse").help("Analyze the input").action(Arguments.storeTrue());
        parser.addArgument("-o", "--output").help("Set the output file").required(true).dest("output")
                .action(Arguments.store());
        parser.addArgument("file").required(true).dest("input").action(Arguments.store()).help("Input file");
        return parser;
    }

    private static Tokenizer tokenize(StringIter iter) {
        var tokenizer = new Tokenizer(iter);
        return tokenizer;
    }
}
