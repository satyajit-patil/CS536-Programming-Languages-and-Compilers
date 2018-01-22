import java.io.*;

import java_cup.runtime.*;

/**
 * Main program to test the parser.
 *
 * There should be 2 command-line arguments:
 *    1. the file to be parsed
 *    2. the output file into which the AST built by the parser should be
 *       unparsed
 * The program opens the two files, creates a scanner and a parser, and
 * calls the parser.  If the parse is successful, the AST is unparsed.
 */
public class P6 {	
	public static void main(String[] args) throws IOException{

		if(args.length != 2 && args.length != 3)
		{
			System.err.println("No input file");
			System.exit(-1);
		}
		FileReader inFile = null;

		try
		{
			inFile = new FileReader(args[0]);
		}
		catch(FileNotFoundException ex)
		{
			System.err.println("Input file not found");
			System.exit(-1);
		}

		PrintWriter outFile = null;
		if(args.length == 3)
		{
			try
			{
				outFile = new PrintWriter(args[2]);
			}
			catch(FileNotFoundException ex)
			{
				System.err.println("Writing file 2 not opened");
				System.exit(-1);
			}
		}

		PrintWriter codeGenFile = null;
		try
		{
			codeGenFile = new PrintWriter(args[1]);
		}
		catch(FileNotFoundException ex)
		{
			System.err.println("Writing file 1 not opened");
			System.exit(-1);
		}

		parser P = new parser(new Yylex(inFile));	
		Symbol root = null;
		try
		{
			root = P.parse();
		}
		catch(Exception ex)
		{
			System.err.println("Parse Error");
			System.exit(-1);
		}


		((ProgramNode)root.value).nameAnalysis();
		if(ErrMsg.err == true)
		{
			System.err.println("Name Analysis Error");
			System.exit(-1);
		}

		((ProgramNode)root.value).typeCheck();
		if(ErrMsg.err == true)
		{
			System.err.println("Type Check Error");
			System.exit(-1);
		}

//NOT SURE IF NEEDED
		if (args.length == 3)
		{
			((ASTnode)root.value).unparse(outFile, 0);
			outFile.close();
		}


		((ProgramNode)root.value).codeGen(codeGenFile);
		codeGenFile.close();
		return;

	}
}
