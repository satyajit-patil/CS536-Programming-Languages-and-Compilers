////////////////////////////////////////////////////////////////////////////////
//Main File: 	P2.java
//This File: 	P2.java
//Semester: 	CS 536 Spring 2017
//
//Author: 	Satyajit Patil
//Email: 	spatil5@wisc.edu
//CS Login: 	jit
//Partner:      James Urban
//CS Login:     urban
//Email:        jrurban@wisc.edu
////////////////////////////////////////////////////////////////////////////////

import java.util.*;
import java.io.*;
import java_cup.runtime.*;  // defines Symbol

/**
 * This program is to be used to test the Scanner.
 */
public class P2 {
	public static void main(String[] args) throws IOException {
		// test all tokens (values associated with tokens) and input that causes errors
		System.out.println("testAllTokens");
		testAllTokens();
		CharNum.num = 1;

		//test char number and line number
		System.out.println("testCharAndLineNum");
		testCharAndLineNum();
		CharNum.num = 1;

		//test end of file 
		System.out.println("eofCheck");
		eofCheck();
		CharNum.num = 1;
	}


	// **********************************************************************
	// eofCheck
	//        
	// open and read from file eof
	//               
	// check that eof is reached and output message to eof.out
	//                   
	// if input file contains eof, then the output file gets a message,
	// if not, the output file is blank
	// 
	// **********************************************************************
	private static void eofCheck() throws IOException {
		FileReader inFile = null;
		PrintWriter outFile = null;
		try {
			inFile = new FileReader("eof.in");
			outFile = new PrintWriter(new FileWriter("eof.out"));
		} catch (FileNotFoundException ex) {
			System.err.println("File eof not found.");
			System.exit(-1);
		} catch (IOException ex) {
			System.err.println("eof.out cannot be opened.");
			System.exit(-1);
		}
		Yylex scanner = new Yylex(inFile);
		Symbol token = scanner.next_token();
		while (token.sym != sym.EOF) {
			token = scanner.next_token();
		}
		if (token.sym == sym.EOF){
			outFile.println("EOF reached");
		}
		outFile.close();
	}


	// **********************************************************************
	//     testCharAndLineNum
	//      
	//     open and read from file inCharNum.in
	//        
	//     for each token read, write the corresponding string to inCharNum.out
	//     
	//     For each token, this method outputs the charnum and line number
	//     to the output file. The output charnum's and line numbers can be
	//     compared to the input file for discrepencies. This differs from
	//     the changes to testAllTokens because the input file mixes chars
	//     on a line to check that CharNum gets updated correctly.
	// **********************************************************************
	private static void testCharAndLineNum() throws IOException {
		// open input and output files
		FileReader inFile = null;
		PrintWriter outFile = null;
		try {
			inFile = new FileReader("inCharNum.in");
			outFile = new PrintWriter(new FileWriter("inCharNum.out"));
		} catch (FileNotFoundException ex) {
			System.err.println("File inCharNum not found.");
			System.exit(-1);
		} catch (IOException ex) {
			System.err.println("inCharNum.out cannot be opened.");
			System.exit(-1);
		}
		// create and call the scanner
		Yylex scanner = new Yylex(inFile);
		Symbol token = scanner.next_token();
		while (token.sym != sym.EOF) {
			//print char number, line number, and the symbol number
			outFile.println(((TokenVal)token.value).charnum + "(" 
					+ ((TokenVal)token.value).linenum + ")    sym # : " + token.sym);
			token = scanner.next_token();
		}
		outFile.close();

	}


	// **********************************************************************
	//     testAllTokens
	//      
	//     open and read from file allTokens.in
	//        
	//     for each token read, write the corresponding string to allTokens.out
	//     
	//     If the input file contains all tokens, one per line, we can verify
	//     correctness of the scanner by comparing the input and output files
	//     (e.g., using a 'diff' command).
	//     
	//     The input file also contains things that should cause errors. The 
	//     output terminal should be checked for things that are errors to 
	//     make sure the correct error message is displayed. The output file
	//     gets the tokens that aren't errors. Keep in mind that when something 
	//     is ignored, the following token could be legal.
	// **********************************************************************
	private static void testAllTokens() throws IOException {
		// open input and output files
		FileReader inFile = null;
		PrintWriter outFile = null;
		try {
			inFile = new FileReader("allTokens.in");
			outFile = new PrintWriter(new FileWriter("allTokens.out"));
		} catch (FileNotFoundException ex) {
			System.err.println("File allTokens.in not found.");
			System.exit(-1);
		} catch (IOException ex) {
			System.err.println("allTokens.out cannot be opened.");
			System.exit(-1);
		}

		// create and call the scanner
		Yylex scanner = new Yylex(inFile);
		Symbol token = scanner.next_token();
		while (token.sym != sym.EOF) {
			switch (token.sym) {
				case sym.BOOL:
					outFile.println("bool"); 
					break;
				case sym.INT:
					outFile.println("int");
					break;
				case sym.VOID:
					outFile.println("void");
					break;
				case sym.TRUE:
					outFile.println("true"); 
					break;
				case sym.FALSE:
					outFile.println("false"); 
					break;
				case sym.STRUCT:
					outFile.println("struct"); 
					break;
				case sym.CIN:
					outFile.println("cin"); 
					break;
				case sym.COUT:
					outFile.println("cout");
					break;				
				case sym.IF:
					outFile.println("if");
					break;
				case sym.ELSE:
					outFile.println("else");
					break;
				case sym.WHILE:
					outFile.println("while");
					break;
				case sym.RETURN:
					outFile.println("return");
					break;
				case sym.ID:
					outFile.println(((IdTokenVal)token.value).idVal);
					break;
				case sym.INTLITERAL:  
					outFile.println(((IntLitTokenVal)token.value).intVal);
					break;
				case sym.STRINGLITERAL: 
					outFile.println(((StrLitTokenVal)token.value).strVal);
					break;    
				case sym.LCURLY:
					outFile.println("{");
					break;
				case sym.RCURLY:
					outFile.println("}");
					break;
				case sym.LPAREN:
					outFile.println("(");
					break;
				case sym.RPAREN:
					outFile.println(")");
					break;
				case sym.SEMICOLON:
					outFile.println(";");
					break;
				case sym.COMMA:
					outFile.println(",");
					break;
				case sym.DOT:
					outFile.println(".");
					break;
				case sym.WRITE:
					outFile.println("<<");
					break;
				case sym.READ:
					outFile.println(">>");
					break;				
				case sym.PLUSPLUS:
					outFile.println("++");
					break;
				case sym.MINUSMINUS:
					outFile.println("--");
					break;	
				case sym.PLUS:
					outFile.println("+");
					break;
				case sym.MINUS:
					outFile.println("-");
					break;
				case sym.TIMES:
					outFile.println("*");
					break;
				case sym.DIVIDE:
					outFile.println("/");
					break;
				case sym.NOT:
					outFile.println("!");
					break;
				case sym.AND:
					outFile.println("&&");
					break;
				case sym.OR:
					outFile.println("||");
					break;
				case sym.EQUALS:
					outFile.println("==");
					break;
				case sym.NOTEQUALS:
					outFile.println("!=");
					break;
				case sym.LESS:
					outFile.println("<");
					break;
				case sym.GREATER:
					outFile.println(">");
					break;
				case sym.LESSEQ:
					outFile.println("<=");
					break;
				case sym.GREATEREQ:
					outFile.println(">=");
					break;
				case sym.ASSIGN:
					outFile.println("=");
					break;
				default:
					outFile.println("UNKNOWN TOKEN");
			} // end switch

			token = scanner.next_token();
		} // end while
		outFile.close();
	}
}
