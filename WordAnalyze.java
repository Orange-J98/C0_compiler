import java.io.File;
import java.io.FileReader;

public class WordAnalyze {
	private String keyWord[] = { "BEGIN", "END", "FOR", "IF", "THEN", "ELSE" };
	private char ch;

	boolean isKey(String str) {
		for (int i = 0; i < keyWord.length; i++) {
			if (keyWord[i].equals(str))
				return true;
		}
		return false;
	}

	boolean isLetter(char letter) {
		if ((letter >= 'a' && letter <= 'z') || (letter >= 'A' && letter <= 'Z'))
			return true;
		else
			return false;
	}

	boolean isDigit(char digit) {
		if (digit >= '0' && digit <= '9')
			return true;
		else
			return false;
	}

	public static String removeZero(String str) {
		int len = str.length(), i = 0;
		while (i < len && str.charAt(i) == '0') {
			i++;
		}
		return str.substring(i);
	}

	public void analyze(char[] chs) {
		String token = "";
		for (int i = 0; i < chs.length - 1; i++) {
			ch = chs[i];
			token = "";
			if (ch == ' ' || ch == '\n' || ch == '\r' || ch == '\t') {
				continue;
			} else if (isLetter(ch)) {
				while (isLetter(ch) || isDigit(ch)) {
					token += ch;
					ch = chs[++i];
				}
				i--;
				if (isKey(token)) {
					System.out.println(token.substring(0, 1).toUpperCase() + token.substring(1).toLowerCase());
				} else {
					System.out.println("Ident(" + token + ")");
				}
			} else if (isDigit(ch)) {
				while (isDigit(ch)) {
					token += ch;
					ch = chs[++i];
				}
				i--;
				token = removeZero(token);
				if (token.equals("")) {
					token = "0";
				}
//				int num=Integer.parseInt(token);
				System.out.println("Int(" + token + ")");
			} else if (ch == ':') {
				ch = chs[++i];
				if (ch == '=') {
					System.out.println("Assign");
				} else {
					i--;
					System.out.println("Colon");
				}
			} else if (ch == '+') {
				System.out.println("Plus");
			} else if (ch == '*') {
				System.out.println("Star");
			} else if (ch == ',') {
				System.out.println("Comma");
			} else if (ch == '(') {
				System.out.println("LParenthesis");
			} else if (ch == ')') {
				System.out.println("RParenthesis");
			} else {
				System.out.println("Unknown");
				break;
			}
		}
	}

	public static void main(String[] args) throws Exception {
		File file = new File(args[0]);
		FileReader reader = new FileReader(file);
		int length = (int) file.length();
		char buf[] = new char[length + 1];
		buf[buf.length-1]=' ';
		buf[buf.length-2]=' ';
		reader.read(buf);
		reader.close();
		new WordAnalyze().analyze(buf);
	}
}