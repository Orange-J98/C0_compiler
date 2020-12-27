package miniplc0java;

import java.io.*;
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
    public static void main(String[] args) throws CompileError, IOException {
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
        ArrayList<Byte> Binary = new ArrayList<>();

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
            HashMap<String,SymbolEntry> globalTable= analyzer.getGlobalSymbolTable();
            ArrayList<String> funcName = analyzer.getFuncName();
            ArrayList<String> globalName = analyzer.getGlobalName();
            //转换成二进制！
            int [] maigicVersion ={0x72,0x30,0x3b,0x3e,0x0,0x0,0x0,0x01};
            for (int num:maigicVersion){
                toBinary_8(Binary,num);
            }
            toBinary_32(Binary,globalName.size());
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
            toBinary_32(Binary,funcName.size());
            for (String tempFuncName:funcName){
                FuncEntry funcEntry = funcTable.get(tempFuncName);
                toBinary_32(Binary,funcEntry.getFunc_name());
                toBinary_32(Binary,funcEntry.getRet_num());
                toBinary_32(Binary,funcEntry.getParam_num());
                toBinary_32(Binary,funcEntry.getLocVarNum());
                toBinary_32(Binary,funcEntry.getBodyCnt());
                for (Instruction instruction:funcEntry.getInstructions()){
                    if (instruction.getX()==-1||instruction.getX()==null) {
                        toBinary_8(Binary,instruction.getOptNum());
                    }else {
                        toBinary_8(Binary,instruction.getOptNum());
                        toBinary_64(Binary,instruction.getX());
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

        } else {
            System.err.println("Please specify either '--analyse' or '--tokenize'.");
            System.exit(-1);
        }
    }
    private  static void toBinary_8 (ArrayList<Byte>Binary,int num)
    {
            Binary.add((byte) (num& 0xff));
    }

    private  static void toBinary_32 (ArrayList<Byte>Binary,int num)
    {
        for (int i=3;i>=0;i--) {
            Binary.add((byte) (num >> (8 * i) & 0xff));
        }
    }

    private  static void toBinary_64 (ArrayList<Byte>Binary,int num)
    {
        for (int i=3;i>=0;i--) {
            Binary.add((byte) (num >> (8 * i) & 0xff));
        }
        for (int i=3;i>=0;i--){
            Binary.add((byte)0);
        }
    }

    private static void StrToBinary(ArrayList<Byte>Binary,String str) {
        byte[] s = str.getBytes();
        for (byte tempS : s) {
            Binary.add(tempS);
        }
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
