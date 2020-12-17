import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import analyser.Analyser;
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
        var tokens = new ArrayList<Token>();
        try {
            instructions = analyzer.analyse();
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
            System.exit(0);
            return;
        }
        for (Instruction instruction : instructions) {
            output.println(instruction.toString());
        }
//        for (Token token : tokens) {
//            output.println(token.toString());
//        }
    }
//
//if (result.getBoolean("tokenize")) {
//        // tokenize
//        var tokens = new ArrayList<Token>();
//        try {
//            while (true) {
//                var token = tokenizer.nextToken();
//                if (token.getTokenType().equals(TokenType.EOF)) {
//                    break;
//                }
//                tokens.add(token);
//            }
//        } catch (Exception e) {
//            // 遇到错误不输出，直接退出
//            System.err.println(e);
//            System.exit(-1);
//            return;
//        }
//        for (Token token : tokens) {
//            output.println(token.toString());
//        }

    private static Tokenizer tokenize(StringIter iter) {
        var tokenizer = new Tokenizer(iter);
        return tokenizer;
    }
}
