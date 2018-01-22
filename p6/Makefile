###
# This Makefile can be used to make a parser for the moo language
# (parser.class) and to make a program (P5.class) that tests the parser and
# the unparse methods in ast.java.
#
# make clean removes all generated files.
#
###

JC = javac

P6.class: P6.java parser.class Yylex.class ASTnode.class
	$(JC) -g P6.java

parser.class: parser.java ASTnode.class Yylex.class ErrMsg.class
	$(JC) parser.java

parser.java: moo.cup
	java java_cup.Main < moo.cup

Yylex.class: moo.jlex.java sym.class ErrMsg.class
	$(JC) moo.jlex.java

ASTnode.class: ast.java
	$(JC) -g ast.java

moo.jlex.java: moo.jlex sym.class
	java JLex.Main moo.jlex

sym.class: sym.java
	$(JC) -g sym.java

sym.java: moo.cup
	java java_cup.Main < moo.cup

ErrMsg.class: ErrMsg.java
	$(JC) ErrMsg.java


###
# test
#
test: P6.class
	java P6 helloWorldMoo.moo test.codeGen.s
	spim -file test.codeGen.s

###
# clean
###
clean:
	rm -f *~ *.class parser.java moo.jlex.java sym.java *.out
