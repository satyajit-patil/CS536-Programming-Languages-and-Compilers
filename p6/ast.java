import java.io.*;
import java.util.*;

// **********************************************************************
// The ASTnode class defines the nodes of the abstract-syntax tree that
// represents a Mini program.
//
// Internal nodes of the tree contain pointers to children, organized
// either in a list (for nodes that may have a variable number of 
// children) or as a fixed set of fields.
//
// The nodes for literals and ids contain line and character number
// information; for string literals and identifiers, they also contain a
// string; for integer literals, they also contain an integer value.
//
// Here are all the different kinds of AST nodes and what kinds of children
// they have.  All of these kinds of AST nodes are subclasses of "ASTnode".
// Indentation indicates further subclassing:
//
//     Subclass            Kids
//     --------            ----
//     ProgramNode         DeclListNode
//     DeclListNode        linked list of DeclNode
//     DeclNode:
//       VarDeclNode       TypeNode, IdNode, int
//       FnDeclNode        TypeNode, IdNode, FormalsListNode, FnBodyNode
//       FormalDeclNode    TypeNode, IdNode
//       StructDeclNode    IdNode, DeclListNode
//
//     FormalsListNode     linked list of FormalDeclNode
//     FnBodyNode          DeclListNode, StmtListNode
//     StmtListNode        linked list of StmtNode
//     ExpListNode         linked list of ExpNode
//
//     TypeNode:
//       IntNode           -- none --
//       BoolNode          -- none --
//       VoidNode          -- none --
//       StructNode        IdNode
//
//     StmtNode:
//       AssignStmtNode      AssignNode
//       PostIncStmtNode     ExpNode
//       PostDecStmtNode     ExpNode
//       ReadStmtNode        ExpNode
//       WriteStmtNode       ExpNode
//       IfStmtNode          ExpNode, DeclListNode, StmtListNode
//       IfElseStmtNode      ExpNode, DeclListNode, StmtListNode,
//                                    DeclListNode, StmtListNode
//       WhileStmtNode       ExpNode, DeclListNode, StmtListNode
//       CallStmtNode        CallExpNode
//       ReturnStmtNode      ExpNode
//
//     ExpNode:
//       IntLitNode          -- none --
//       StrLitNode          -- none --
//       TrueNode            -- none --
//       FalseNode           -- none --
//       IdNode              -- none --
//       DotAccessNode       ExpNode, IdNode
//       AssignNode          ExpNode, ExpNode
//       CallExpNode         IdNode, ExpListNode
//       UnaryExpNode        ExpNode
//         UnaryMinusNode
//         NotNode
//       BinaryExpNode       ExpNode ExpNode
//         PlusNode     
//         MinusNode
//         TimesNode
//         DivideNode
//         AndNode
//         OrNode
//         EqualsNode
//         NotEqualsNode
//         LessNode
//         GreaterNode
//         LessEqNode
//         GreaterEqNode
//
// Here are the different kinds of AST nodes again, organized according to
// whether they are leaves, internal nodes with linked lists of kids, or
// internal nodes with a fixed number of kids:
//
// (1) Leaf nodes:
//        IntNode,   BoolNode,  VoidNode,  IntLitNode,  StrLitNode,
//        TrueNode,  FalseNode, IdNode
//
// (2) Internal nodes with (possibly empty) linked lists of children:
//        DeclListNode, FormalsListNode, StmtListNode, ExpListNode
//
// (3) Internal nodes with fixed numbers of kids:
//        ProgramNode,     VarDeclNode,     FnDeclNode,     FormalDeclNode,
//        StructDeclNode,  FnBodyNode,      StructNode,     AssignStmtNode,
//        PostIncStmtNode, PostDecStmtNode, ReadStmtNode,   WriteStmtNode   
//        IfStmtNode,      IfElseStmtNode,  WhileStmtNode,  CallStmtNode
//        ReturnStmtNode,  DotAccessNode,   AssignExpNode,  CallExpNode,
//        UnaryExpNode,    BinaryExpNode,   UnaryMinusNode, NotNode,
//        PlusNode,        MinusNode,       TimesNode,      DivideNode,
//        AndNode,         OrNode,          EqualsNode,     NotEqualsNode,
//        LessNode,        GreaterNode,     LessEqNode,     GreaterEqNode
//
// **********************************************************************

// **********************************************************************
// ASTnode class (base class for all other kinds of nodes)
// **********************************************************************

abstract class ASTnode { 
	// every subclass must provide an unparse operation
	abstract public void unparse(PrintWriter p, int indent);
	// every subclass must provide a codeGen operation
	abstract public void codeGen();

	// this method can be used by the unparse methods to do indenting
	protected void doIndent(PrintWriter p, int indent) {
		for (int k=0; k<indent; k++) p.print(" ");
	}

	static boolean hasMain = false;
	static boolean isGlobal = false;
	protected Map<String, String> strLitMap = new HashMap<String, String>();
}

// **********************************************************************
// ProgramNode,  DeclListNode, FormalsListNode, FnBodyNode,
// StmtListNode, ExpListNode
// **********************************************************************

class ProgramNode extends ASTnode {
	public ProgramNode(DeclListNode L) {
		myDeclList = L;
	}

	/**
	 * nameAnalysis
	 * Creates an empty symbol table for the outermost scope, then processes
	 * all of the globals, struct defintions, and functions in the program.
	 */
	public void nameAnalysis() {
		SymTable symTab = new SymTable();
		isGlobal = true;
		myDeclList.nameAnalysis(symTab);
		if(!hasMain){
			ErrMsg.fatal(0, 0, "No main function");
			System.exit(-1);
		}
	}

	/**
	 * typeCheck
	 */
	public void typeCheck() {
		myDeclList.typeCheck();
	}

	public void unparse(PrintWriter p, int indent) {
		myDeclList.unparse(p, indent);
	}

	public void codeGen(PrintWriter p){ 
		Codegen.p = p;
		myDeclList.codeGen();
	}
	public void codeGen(){
	}

	// 1 kid
	private DeclListNode myDeclList;
}

class DeclListNode extends ASTnode {
	public DeclListNode(List<DeclNode> S) {
		myDecls = S;
	}

	/**
	 * nameAnalysis
	 * Given a symbol table symTab, process all of the decls in the list.
	 */
	public void nameAnalysis(SymTable symTab) {
		nameAnalysis(symTab, symTab);
	}

	/**
	 * nameAnalysis
	 * Given a symbol table symTab and a global symbol table globalTab
	 * (for processing struct names in variable decls), process all of the 
	 * decls in the list.
	 */    
	public void nameAnalysis(SymTable symTab, SymTable globalTab) {
		currentOffset = declListOffset;
		if(declListOffset == -1 && !isGlobal)
			System.out.println("Wrong Offset in DeclListNode");

		for (DeclNode n : myDecls) {
			if (n instanceof VarDeclNode) {
				((VarDeclNode)n).nameAnalysis(symTab, globalTab);
				if(!isGlobal){
					((VarDeclNode)n).setVarDeclOffset(currentOffset);
					currentOffset -= 4;
				}
				else{ 
					((VarDeclNode)n).setVarDeclOffset(-1);
					((VarDeclNode)n).setIsGlobal();
				}
			} else {
				n.nameAnalysis(symTab);
			}
		}
	}    

	public void setOffset(int declListOffset){
		this.declListOffset = declListOffset;
	}
	public int getOffset(){
		return this.currentOffset;
	}

	public int getSize(){
		return (this.currentOffset - this.declListOffset);
	}


	/**
	 * typeCheck
	 */
	public void typeCheck() {
		for (DeclNode n : myDecls) {
			n.typeCheck();
		}
	}

	public void unparse(PrintWriter p, int indent) {
		Iterator it = myDecls.iterator();
		try {
			while (it.hasNext()) {
				((DeclNode)it.next()).unparse(p, indent);
			}
		} catch (NoSuchElementException ex) {
			System.err.println("unexpected NoSuchElementException in DeclListNode.print");
			System.exit(-1);
		}
	}

	public void codeGen(){
		if( myDecls != null){
			for(DeclNode n : myDecls){	
				n.codeGen();
			}
		}
	}
	private int declListOffset = -1;
	private int currentOffset =0;
	private List<DeclNode> myDecls; 	// list of kids (DeclNodes)
}

class FormalsListNode extends ASTnode {
	public FormalsListNode(List<FormalDeclNode> S) {
		myFormals = S;
	}

	/**
	 * nameAnalysis
	 * Given a symbol table symTab, do:
	 * for each formal decl in the list
	 *     process the formal decl
	 *     if there was no error, add type of formal decl to list
	 */
	public List<Type> nameAnalysis(SymTable symTab) {
		List<Type> typeList = new LinkedList<Type>();
		isGlobal = false;
		currentOffset = formalsListOffset;
		for (FormalDeclNode node : myFormals) {
			SemSym sym = node.nameAnalysis(symTab);
			node.setOffset(currentOffset);
			currentOffset = currentOffset -4; 
			if (sym != null) {
				typeList.add(sym.getType());
			}
		}
		return typeList;
	}    

	public void setOffset(int formalsListOffset){
		this.formalsListOffset = formalsListOffset;
	}
	public int getOffset(){
		return this.currentOffset;
	}
	public int getSize(){
		return (this.currentOffset - this.formalsListOffset);
	}


	/**
	 * Return the number of formals in this list.
	 */
	public int length() {
		return myFormals.size();
	}

	public void unparse(PrintWriter p, int indent) {
		Iterator<FormalDeclNode> it = myFormals.iterator();
		if (it.hasNext()) { 
			it.next().unparse(p, indent);
			while (it.hasNext()) { 
				p.print(", ");
				it.next().unparse(p, indent);
			}
		} 
	}
	public void codeGen(){
	}

	// list of kids (FormalDeclNodes)
	private List<FormalDeclNode> myFormals;
	private int formalsListOffset = 0;
	private int currentOffset = 0;
}

class FnBodyNode extends ASTnode {
	public FnBodyNode(DeclListNode declList, StmtListNode stmtList) {
		myDeclList = declList;
		myStmtList = stmtList;
	}

	/**
	 * nameAnalysis
	 * Given a symbol table symTab, do:
	 * - process the declaration list
	 * - process the statement list
	 */
	public void nameAnalysis(SymTable symTab) {
		isGlobal = false;
		myDeclList.setOffset( this.declListOffset );
		myDeclList.nameAnalysis(symTab);
		myStmtList.setOffset( myDeclList.getOffset() );

		myStmtList.nameAnalysis(symTab);
	}  

	public void setOffset(int fnBodyOffset){
		this.declListOffset = fnBodyOffset;
	}
	public int getOffset(){
		return currentOffset;
	}
	public int getSize(){
		return myDeclList.getSize()+myStmtList.getSize();
	}  

	/**
	 * typeCheck
	 */
	public void typeCheck(Type retType) {
		myStmtList.typeCheck(retType);
	}    

	public void unparse(PrintWriter p, int indent) {
		myDeclList.unparse(p, indent);
		myStmtList.unparse(p, indent);
	}
	public void codeGen(){
		myStmtList.codeGen();
	}
	public void codeGen(String retLabel){
		myStmtList.codeGen(retLabel);
	}

	// 2 kids
	private DeclListNode myDeclList;
	private StmtListNode myStmtList;
	private int declListOffset = 0;
	private int stmtListOffset = 0;
	private int currentOffset;
}

class StmtListNode extends ASTnode {
	public StmtListNode(List<StmtNode> S) {
		myStmts = S;
	}

	/**
	 * nameAnalysis
	 * Given a symbol table symTab, process each statement in the list.
	 */
	public void nameAnalysis(SymTable symTab) {
		currentOffset = stmtListOffset;

		for (StmtNode n : myStmts) {
			n.setOffset(currentOffset);

			n.nameAnalysis(symTab);
			currentOffset = n.getOffset();

		}
	}    
	public void setOffset(int stmtListOffset){
		this.stmtListOffset = stmtListOffset;
	}	

	public int getOffset(){
		return this.currentOffset;
	}

	public int getSize(){
		return (this.currentOffset - this.stmtListOffset);
	}

	/**
	 * typeCheck
	 */
	public void typeCheck(Type retType) {
		for(StmtNode node : myStmts) {
			node.typeCheck(retType);
		}
	}

	public void unparse(PrintWriter p, int indent) {
		Iterator<StmtNode> it = myStmts.iterator();
		while (it.hasNext()) {
			it.next().unparse(p, indent);
		}
	}

	public void codeGen(){
		if(myStmts != null){
			for(StmtNode node : myStmts){
				node.codeGen();
			}
		}
	}
	public void codeGen(String retLabel){
		if(myStmts != null){
			for(StmtNode node : myStmts){
				if(node instanceof ReturnStmtNode)
					((ReturnStmtNode)node).codeGen(retLabel);
				else
					node.codeGen();
			}
		}
	}

	// list of kids (StmtNodes)
	private List<StmtNode> myStmts;
	private int stmtListOffset = 0;
	private int currentOffset = 0;
}

class ExpListNode extends ASTnode {
	public ExpListNode(List<ExpNode> S) {
		myExps = S;
	}

	public int size() {
		return myExps.size();
	}

	/**
	 * nameAnalysis
	 * Given a symbol table symTab, process each exp in the list.
	 */
	public void nameAnalysis(SymTable symTab) {
		for (ExpNode n : myExps) {
			n.nameAnalysis(symTab);
		}
	}

	/**
	 * typeCheck
	 */
	public void typeCheck(List<Type> typeList) {
		int k = 0;
		try {
			for (ExpNode n : myExps) {
				Type actualType = n.typeCheck();     // actual type of arg

				if (!actualType.isErrorType()) {        // if this is not an error
					Type formalType = typeList.get(k);  // get the formal type
					if (!formalType.equals(actualType)) {
						ErrMsg.fatal(n.lineNum(), n.charNum(),
								"Type of actual does not match type of formal");
					}
				}
				k++;
			}
		} catch (NoSuchElementException e) {
			System.err.println("unexpected NoSuchElementException in ExpListNode.typeCheck");
			System.exit(-1);
		}
	}

	public void unparse(PrintWriter p, int indent) {
		Iterator<ExpNode> it = myExps.iterator();
		if (it.hasNext()) { // if there is at least one element
			it.next().unparse(p, indent);
			while (it.hasNext()) {  // print the rest of the list
				p.print(", ");
				it.next().unparse(p, indent);
			}
		} 
	}

	public void codeGen(){
		if(myExps != null){
			for(ExpNode node : myExps){
				node.codeGen();
			}
		}
	}

	// list of kids (ExpNodes)
	private List<ExpNode> myExps;
}

// **********************************************************************
// DeclNode and its subclasses
// **********************************************************************

abstract class DeclNode extends ASTnode {
	/**
	 * Note: a formal decl needs to return a sym
	 */
	abstract public SemSym nameAnalysis(SymTable symTab);

	// default version of typeCheck for non-function decls
	public void typeCheck() { }
}

class VarDeclNode extends DeclNode {
	public VarDeclNode(TypeNode type, IdNode id, int size) {
		myType = type;
		myId = id;
		mySize = size;
	}

	/**
	 * nameAnalysis (overloaded)
	 * Given a symbol table symTab, do:
	 * if this name is declared void, then error
	 * else if the declaration is of a struct type, 
	 *     lookup type name (globally)
	 *     if type name doesn't exist, then error
	 * if no errors so far,
	 *     if name has already been declared in this scope, then error
	 *     else add name to local symbol table     
	 *
	 * symTab is local symbol table (say, for struct field decls)
	 * globalTab is global symbol table (for struct type names)
	 * symTab and globalTab can be the same
	 */
	public SemSym nameAnalysis(SymTable symTab) {
		return nameAnalysis(symTab, symTab);
	}

	public SemSym nameAnalysis(SymTable symTab, SymTable globalTab) {
		boolean badDecl = false;
		String name = myId.name();
		SemSym sym = null;
		IdNode structId = null;

		if (myType instanceof VoidNode) {  // check for void type
			ErrMsg.fatal(myId.lineNum(), myId.charNum(), 
					"Non-function declared void");
			badDecl = true;        
		}

		else if (myType instanceof StructNode) {
			structId = ((StructNode)myType).idNode();
			sym = globalTab.lookupGlobal(structId.name());

			// if the name for the struct type is not found, 
			// or is not a struct type
			if (sym == null || !(sym instanceof StructDefSym)) {
				ErrMsg.fatal(structId.lineNum(), structId.charNum(), 
						"Invalid name of struct type");
				badDecl = true;
			}
			else {
				structId.link(sym);
			}
		}

		if (symTab.lookupLocal(name) != null) {
			ErrMsg.fatal(myId.lineNum(), myId.charNum(), 
					"Multiply declared identifier");
			badDecl = true;            
		}

		if (!badDecl) {  // insert into symbol table
			try {
				if (myType instanceof StructNode) {
					sym = new StructSym(structId);
				}
				else {
					sym = new SemSym(myType.type());
				}
				symTab.addDecl(name, sym);
				myId.link(sym);
			} catch (DuplicateSymException ex) {
				System.err.println("Unexpected DuplicateSymException " +
						" in VarDeclNode.nameAnalysis");
				System.exit(-1);
			} catch (EmptySymTableException ex) {
				System.err.println("Unexpected EmptySymTableException " +
						" in VarDeclNode.nameAnalysis");
				System.exit(-1);
			}
		}

		return sym;
	}    

	public void setVarDeclOffset(int varDeclOffset){
		myId.setOffset(varDeclOffset);
	}
	public void setIsGlobal(){
		myId.setIsGlobal();
	}    

	public void unparse(PrintWriter p, int indent) {
		doIndent(p, indent);
		myType.unparse(p, 0);
		p.print(" ");
		p.print(myId.name());
		p.print("("+myId.getOffset()+")");


		p.println(";");
	}

	public void codeGen(){
		myId.codeGen_GlobalDecl();
	}

	// 3 kids
	private TypeNode myType;
	private IdNode myId;
	private int mySize;  // use value NOT_STRUCT if this is not a struct type

	public static int NOT_STRUCT = -1;
}

class FnDeclNode extends DeclNode {
	public FnDeclNode(TypeNode type,
			IdNode id,
			FormalsListNode formalList,
			FnBodyNode body) {
		myType = type;
		myId = id;
		myFormalsList = formalList;
		myBody = body;
	}

	/**
	 * nameAnalysis
	 * Given a symbol table symTab, do:
	 * if this name has already been declared in this scope, then error
	 * else add name to local symbol table
	 * in any case, do the following:
	 *     enter new scope
	 *     process the formals
	 *     if this function is not multiply declared,
	 *         update symbol table entry with types of formals
	 *     process the body of the function
	 *     exit scope
	 */
	public SemSym nameAnalysis(SymTable symTab) {
		String name = myId.name();
		if(name.equals("main")){
			hasMain = true;
		}
		FnSym sym = null;

		if (symTab.lookupLocal(name) != null) {
			ErrMsg.fatal(myId.lineNum(), myId.charNum(),
					"Multiply declared identifier");
		}

		else { // add function name to local symbol table
			try {
				sym = new FnSym(myType.type(), myFormalsList.length());
				symTab.addDecl(name, sym);
				myId.link(sym);
			} catch (DuplicateSymException ex) {
				System.err.println("Unexpected DuplicateSymException " +
						" in FnDeclNode.nameAnalysis");
				System.exit(-1);
			} catch (EmptySymTableException ex) {
				System.err.println("Unexpected EmptySymTableException " +
						" in FnDeclNode.nameAnalysis");
				System.exit(-1);
			}
		}

		symTab.addScope();  // add a new scope for locals and params

		isGlobal = false;
		// process the formals
		myFormalsList.setOffset(0);

		List<Type> typeList = myFormalsList.nameAnalysis(symTab);
		if (sym != null) {
			sym.addFormals(typeList);
		}

		myBody.setOffset( 0-myFormalsList.length()*4 -8);

		myBody.nameAnalysis(symTab); // process the function body

		try {
			symTab.removeScope();  // exit scope
		} catch (EmptySymTableException ex) {
			System.err.println("Unexpected EmptySymTableException " +
					" in FnDeclNode.nameAnalysis");
			System.exit(-1);
		}

		return null;
	} 

	/**
	 * typeCheck
	 */
	public void typeCheck() {
		myBody.typeCheck(myType.type());
	}

	public void unparse(PrintWriter p, int indent) {
		doIndent(p, indent);
		myType.unparse(p, 0);
		p.print(" ");
		p.print(myId.name());
		p.print("(");
		myFormalsList.unparse(p, 0);
		p.println(") {");
		myBody.unparse(p, indent+4);
		p.println("}\n");
	}

	//function decl
	public void codeGen(){
		if(myId.name().equals("main")){
			Codegen.genLabel("main");
			Codegen.genLabel("__start");
		}else{
			Codegen.genLabel("_"+myId.name());
		}
		Codegen.generateComment("Begin Function Prologue");
		Codegen.genPush(Codegen.RA);
		Codegen.genPush(Codegen.FP);
		Codegen.generate("addu", Codegen.FP, Codegen.SP, myFormalsList.length()*4+8); 
		Codegen.generateComment("Begin Function Body");
		String retLabel = Codegen.nextLabel();
		myBody.codeGen(retLabel); 
		Codegen.generateComment("Begin Function Epilogue");
		Codegen.genLabel(retLabel);
		Codegen.generateIndexed("lw", Codegen.RA, Codegen.FP, - myFormalsList.length()*4, "");
		Codegen.generateWithComment("move", "", Codegen.T0, Codegen.FP);
		Codegen.generateIndexed("lw", Codegen.FP, Codegen.FP, -myFormalsList.length()*4-4, "");
		Codegen.generate("move", Codegen.SP, Codegen.T0);
		if(myId.name().equals("main")){
			Codegen.generate("li", Codegen.V0, "10");
			Codegen.generate("syscall");
		}
		else
			Codegen.generateWithComment("jr","Function exit finished", Codegen.RA);
	}

	// 4 kids
	private TypeNode myType;
	private IdNode myId;
	private FormalsListNode myFormalsList;
	private FnBodyNode myBody;
}

class FormalDeclNode extends DeclNode {
	public FormalDeclNode(TypeNode type, IdNode id) {
		myType = type;
		myId = id;
	}

	/**
	 * nameAnalysis
	 * Given a symbol table symTab, do:
	 * if this formal is declared void, then error
	 * else if this formal is already in the local symble table,
	 *     then issue multiply declared error message and return null
	 * else add a new entry to the symbol table and return that Sym
	 */
	public SemSym nameAnalysis(SymTable symTab) {
		String name = myId.name();
		boolean badDecl = false;
		SemSym sym = null;

		if (myType instanceof VoidNode) {
			ErrMsg.fatal(myId.lineNum(), myId.charNum(), 
					"Non-function declared void");
			badDecl = true;        
		}

		if (symTab.lookupLocal(name) != null) {
			ErrMsg.fatal(myId.lineNum(), myId.charNum(), 
					"Multiply declared identifier");
			badDecl = true;
		}

		if (!badDecl) {  // insert into symbol table
			try {
				sym = new SemSym(myType.type());
				symTab.addDecl(name, sym);
				myId.link(sym);
			} catch (DuplicateSymException ex) {
				System.err.println("Unexpected DuplicateSymException " +
						" in VarDeclNode.nameAnalysis");
				System.exit(-1);
			} catch (EmptySymTableException ex) {
				System.err.println("Unexpected EmptySymTableException " +
						" in VarDeclNode.nameAnalysis");
				System.exit(-1);
			}
		}

		return sym;
	}    

	public void setOffset(int formalOffset){
		myId.setOffset(formalOffset);
	}

	public void unparse(PrintWriter p, int indent) {
		myType.unparse(p, 0);
		p.print(" ");
		p.print(myId.name());
		p.print("("+myId.getOffset() + ")");

	}

	public void codeGen(){
	}

	// 2 kids
	private TypeNode myType;
	private IdNode myId;
}

class StructDeclNode extends DeclNode {
	public StructDeclNode(IdNode id, DeclListNode declList) {
		myId = id;
		myDeclList = declList;
	}

	/**
	 * nameAnalysis
	 * Given a symbol table symTab, do:
	 * if this name is already in the symbol table,
	 *     then multiply declared error (don't add to symbol table)
	 * create a new symbol table for this struct definition
	 * process the decl list
	 * if no errors
	 *     add a new entry to symbol table for this struct
	 */
	public SemSym nameAnalysis(SymTable symTab) {
		String name = myId.name();
		boolean badDecl = false;

		if (symTab.lookupLocal(name) != null) {
			ErrMsg.fatal(myId.lineNum(), myId.charNum(), 
					"Multiply declared identifier");
			badDecl = true;            
		}

		SymTable structSymTab = new SymTable();

		// process the fields of the struct
		isGlobal = false;

		myDeclList.nameAnalysis(structSymTab, symTab);

		if (!badDecl) {
			try {   // add entry to symbol table
				StructDefSym sym = new StructDefSym(structSymTab);
				symTab.addDecl(name, sym);
				myId.link(sym);
			} catch (DuplicateSymException ex) {
				System.err.println("Unexpected DuplicateSymException " +
						" in StructDeclNode.nameAnalysis");
				System.exit(-1);
			} catch (EmptySymTableException ex) {
				System.err.println("Unexpected EmptySymTableException " +
						" in StructDeclNode.nameAnalysis");
				System.exit(-1);
			}
		}

		return null;
	}    

	public void unparse(PrintWriter p, int indent) {
		doIndent(p, indent);
		p.print("struct ");
		p.print(myId.name());
		p.println("{");
		myDeclList.unparse(p, indent+4);
		doIndent(p, indent);
		p.println("};\n");

	}

	public void codeGen(){
	}

	// 2 kids
	private IdNode myId;
	private DeclListNode myDeclList;
}

// **********************************************************************
// TypeNode and its Subclasses
// **********************************************************************

abstract class TypeNode extends ASTnode {
	/* all subclasses must provide a type method */
	abstract public Type type();
}

class IntNode extends TypeNode {
	public IntNode() {
	}

	/**
	 * type
	 */
	public Type type() {
		return new IntType();
	}

	public void unparse(PrintWriter p, int indent) {
		p.print("int");
	}
	public void codeGen(){
	}
}

class BoolNode extends TypeNode {
	public BoolNode() {
	}

	/**
	 * type
	 */
	public Type type() {
		return new BoolType();
	}

	public void unparse(PrintWriter p, int indent) {
		p.print("bool");
	}

	public void codeGen(){
	}
}

class VoidNode extends TypeNode {
	public VoidNode() {
	}

	/**
	 * type
	 */
	public Type type() {
		return new VoidType();
	}

	public void unparse(PrintWriter p, int indent) {
		p.print("void");
	}

	public void codeGen(){
	}
}

class StructNode extends TypeNode {
	public StructNode(IdNode id) {
		myId = id;
	}

	public IdNode idNode() {
		return myId;
	}

	/**
	 * type
	 */
	public Type type() {
		return new StructType(myId);
	}

	public void unparse(PrintWriter p, int indent) {
		p.print("struct ");
		p.print(myId.name());
	}

	public void codeGen(){
	}

	// 1 kid
	private IdNode myId;
}

// **********************************************************************
// StmtNode and its subclasses
// **********************************************************************

abstract class StmtNode extends ASTnode {
	abstract public void nameAnalysis(SymTable symTab);
	abstract public void typeCheck(Type retType);
	abstract public void setOffset(int offset);
	abstract public int  getOffset();
	abstract public int  getSize();
}

class AssignStmtNode extends StmtNode {
	public AssignStmtNode(AssignNode assign) {
		myAssign = assign;
	}

	/**
	 * nameAnalysis
	 * Given a symbol table symTab, perform name analysis on this node's child
	 */
	public void nameAnalysis(SymTable symTab) {
		myAssign.nameAnalysis(symTab);
	}

	public void setOffset(int offset){
		this.offset = offset;
	}
	public int getOffset(){
		return this.offset;
	}

	public int getSize(){
		return 0;
	}
	private int offset;

	/**
	 * typeCheck
	 */
	public void typeCheck(Type retType) {
		myAssign.typeCheck();
	}

	public void unparse(PrintWriter p, int indent) {
		doIndent(p, indent);
		myAssign.unparse(p, -1); // no parentheses
		p.println(";");
	}

	public void codeGen(){
		myAssign.codeGen();
		Codegen.genPop(Codegen.T0);	
	}

	// 1 kid
	private AssignNode myAssign;
}

class PostIncStmtNode extends StmtNode {
	public PostIncStmtNode(ExpNode exp) {
		myExp = exp;
	}

	/**
	 * nameAnalysis
	 * Given a symbol table symTab, perform name analysis on this node's child
	 */
	public void nameAnalysis(SymTable symTab) {
		myExp.nameAnalysis(symTab);
	}

	public void setOffset(int offset){
		this.offset = offset;
	}
	public int getOffset(){
		return this.offset;
	}

	public int getSize(){
		return 0;
	}
	private int offset;

	/**
	 * typeCheck
	 */
	public void typeCheck(Type retType) {
		Type type = myExp.typeCheck();

		if (!type.isErrorType() && !type.isIntType()) {
			ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
					"Arithmetic operator applied to non-numeric operand");
		}
	}

	public void unparse(PrintWriter p, int indent) {
		doIndent(p, indent);
		myExp.unparse(p, 0);
		p.println("++;");
	}

	public void codeGen(){
		myExp.codeGen();
		((IdNode)myExp).genAddr();
		Codegen.genPop(Codegen.T1); 
		Codegen.genPop(Codegen.T0); 
		Codegen.generate("add", Codegen.T0, Codegen.T0, "1");
		Codegen.generateIndexed("sw", Codegen.T0, Codegen.T1, 0);
	}

	// 1 kid
	private ExpNode myExp;
}

class PostDecStmtNode extends StmtNode {
	public PostDecStmtNode(ExpNode exp) {
		myExp = exp;
	}

	/**
	 * nameAnalysis
	 * Given a symbol table symTab, perform name analysis on this node's child
	 */
	public void nameAnalysis(SymTable symTab) {
		myExp.nameAnalysis(symTab);
	}

	public void setOffset(int offset){
		this.offset = offset;
	}
	public int getOffset(){
		return this.offset;
	}

	public int getSize(){
		return 0;
	}
	private int offset;

	/**
	 * typeCheck
	 */
	public void typeCheck(Type retType) {
		Type type = myExp.typeCheck();

		if (!type.isErrorType() && !type.isIntType()) {
			ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
					"Arithmetic operator applied to non-numeric operand");
		}
	}

	public void unparse(PrintWriter p, int indent) {
		doIndent(p, indent);
		myExp.unparse(p, 0);
		p.println("--;");
	}

	public void codeGen(){
		myExp.codeGen();
		((IdNode)myExp).genAddr();
		Codegen.genPop(Codegen.T1); 
		Codegen.genPop(Codegen.T0); 
		Codegen.generate("sub", Codegen.T0, Codegen.T0, "1"); 
		Codegen.generateIndexed("sw", Codegen.T0, Codegen.T1, 0); 
	}

	// 1 kid
	private ExpNode myExp;
}

class ReadStmtNode extends StmtNode {
	public ReadStmtNode(ExpNode e) {
		myExp = e;
	}

	/**
	 * nameAnalysis
	 * Given a symbol table symTab, perform name analysis on this node's child
	 */
	public void nameAnalysis(SymTable symTab) {
		myExp.nameAnalysis(symTab);
	}    

	public void setOffset(int offset){
		this.offset = offset;
	}
	public int getOffset(){
		return this.offset;
	}

	public int getSize(){
		return 0;
	}
	private int offset;

	/**
	 * typeCheck
	 */
	public void typeCheck(Type retType) {
		Type type = myExp.typeCheck();

		if (type.isFnType()) {
			ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
					"Attempt to read a function");
		}

		if (type.isStructDefType()) {
			ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
					"Attempt to read a struct name");
		}

		if (type.isStructType()) {
			ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
					"Attempt to read a struct variable");
		}
	}

	public void unparse(PrintWriter p, int indent) {
		doIndent(p, indent);
		p.print("cin >> ");
		myExp.unparse(p, 0);
		p.println(";");
	}

	public void codeGen(){
		assert( myExp instanceof IdNode);
		((IdNode)myExp).genAddr();
		Codegen.generateWithComment("li","", Codegen.V0, "5");
		Codegen.generate("syscall");
		Codegen.generateIndexed("lw", Codegen.T0, Codegen.SP, 4);
		if(((IdNode)myExp).sym().getType().isBoolType()){
			Codegen.generate("sne",Codegen.V0, Codegen.V0, "$zero");
		}
		Codegen.generateIndexed("sw", Codegen.V0, Codegen.T0, 0);
		Codegen.genPop(Codegen.V0);
	}

	// 1 kid (actually can only be an IdNode or an ArrayExpNode)
	private ExpNode myExp;
}

class WriteStmtNode extends StmtNode {
	public WriteStmtNode(ExpNode exp) {
		myExp = exp;
	}

	/**
	 * nameAnalysis
	 * Given a symbol table symTab, perform name analysis on this node's child
	 */
	public void nameAnalysis(SymTable symTab) {
		myExp.nameAnalysis(symTab);
	}

	public void setOffset(int offset){
		this.offset = offset;
	}
	public int getOffset(){
		return this.offset;
	}

	public int getSize(){
		return 0;
	}
	private int offset;

	/**
	 * typeCheck
	 */
	public void typeCheck(Type retType) {
		Type type = myExp.typeCheck();
		writeType = type;


		if (type.isFnType()) {
			ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
					"Attempt to write a function");
		}

		if (type.isStructDefType()) {
			ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
					"Attempt to write a struct name");
		}

		if (type.isStructType()) {
			ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
					"Attempt to write a struct variable");
		}

		if (type.isVoidType()) {
			ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
					"Attempt to write void");
		}
	}

	public void unparse(PrintWriter p, int indent) {
		doIndent(p, indent);
		p.print("cout << ");
		myExp.unparse(p, 0);
		p.println(";");
	}

	public void codeGen(){
		myExp.codeGen();
		Codegen.genPop(Codegen.A0);
		if(writeType.isIntType() || writeType.isBoolType())
			Codegen.generate("li", Codegen.V0, 1);
		else if(writeType.isStringType())
			Codegen.generateWithComment("li", "System call for printing string", Codegen.V0, "4");
		Codegen.generateWithComment("syscall", "");
	}

	// 1 kid
	private ExpNode myExp;
	private Type writeType;

}

class IfStmtNode extends StmtNode {
	public IfStmtNode(ExpNode exp, DeclListNode dlist, StmtListNode slist) {
		myDeclList = dlist;
		myExp = exp;
		myStmtList = slist;
	}

	/**
	 * nameAnalysis
	 * Given a symbol table symTab, do:
	 * - process the condition
	 * - enter a new scope
	 * - process the decls and stmts
	 * - exit the scope
	 */
	public void nameAnalysis(SymTable symTab) {
		myExp.nameAnalysis(symTab);
		symTab.addScope();
		isGlobal = false;
		myDeclList.setOffset(this.offset);

		myDeclList.nameAnalysis(symTab);
		myStmtList.setOffset(myDeclList.getOffset());

		myStmtList.nameAnalysis(symTab);
		try {
			symTab.removeScope();
		} catch (EmptySymTableException ex) {
			System.err.println("Unexpected EmptySymTableException " +
					" in IfStmtNode.nameAnalysis");
			System.exit(-1);        
		}
	}

	public void setOffset(int offset){
		this.offset = offset;
	}
	public int getOffset(){
		return this.offset + myDeclList.getSize() + myStmtList.getSize();
	}

	public int getSize(){
		return myDeclList.getSize() + myStmtList.getSize();
	}
	private int offset;

	/**
	 * typeCheck
	 */
	public void typeCheck(Type retType) {
		Type type = myExp.typeCheck();

		if (!type.isErrorType() && !type.isBoolType()) {
			ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
					"Non-bool expression used as an if condition");        
		}

		myStmtList.typeCheck(retType);
	}

	public void unparse(PrintWriter p, int indent) {
		doIndent(p, indent);
		p.print("if (");
		myExp.unparse(p, 0);
		p.println(") {");
		myDeclList.unparse(p, indent+4);
		myStmtList.unparse(p, indent+4);
		doIndent(p, indent);
		p.println("}");
	}

	public void codeGen(){
		String trueLabel = Codegen.nextLabel();
		String doneLabel = Codegen.nextLabel();
		myExp.genJumpCode(trueLabel, doneLabel);
		Codegen.genLabel(trueLabel);
		myStmtList.codeGen();
		Codegen.genLabel(doneLabel);
	}

	// e kids
	private ExpNode myExp;
	private DeclListNode myDeclList;
	private StmtListNode myStmtList;
}

class IfElseStmtNode extends StmtNode {
	public IfElseStmtNode(ExpNode exp, DeclListNode dlist1,
			StmtListNode slist1, DeclListNode dlist2,
			StmtListNode slist2) {
		myExp = exp;
		myThenDeclList = dlist1;
		myThenStmtList = slist1;
		myElseDeclList = dlist2;
		myElseStmtList = slist2;
	}

	/**
	 * nameAnalysis
	 * Given a symbol table symTab, do:
	 * - process the condition
	 * - enter a new scope
	 * - process the decls and stmts of then
	 * - exit the scope
	 * - enter a new scope
	 * - process the decls and stmts of else
	 * - exit the scope
	 */
	public void nameAnalysis(SymTable symTab) {
		myExp.nameAnalysis(symTab);
		symTab.addScope();
		myThenDeclList.setOffset(offset);

		myThenDeclList.nameAnalysis(symTab);
		myThenStmtList.setOffset(myThenDeclList.getOffset());

		myThenStmtList.nameAnalysis(symTab);
		try {
			symTab.removeScope();
		} catch (EmptySymTableException ex) {
			System.err.println("Unexpected EmptySymTableException " +
					" in IfStmtNode.nameAnalysis");
			System.exit(-1);        
		}
		symTab.addScope();
		myElseDeclList.setOffset(myThenStmtList.getOffset());

		myElseDeclList.nameAnalysis(symTab);
		myElseStmtList.setOffset(myElseDeclList.getOffset());

		myElseStmtList.nameAnalysis(symTab);
		try {
			symTab.removeScope();
		} catch (EmptySymTableException ex) {
			System.err.println("Unexpected EmptySymTableException " +
					" in IfStmtNode.nameAnalysis");
			System.exit(-1);        
		}
	}

	public void setOffset(int offset){
		this.offset = offset;
	}
	public int getOffset(){
		return myElseStmtList.getOffset();
	}

	public int getSize(){
		return myThenDeclList.getSize() + myThenStmtList.getSize()+myElseDeclList.getSize() + myElseStmtList.getSize();
	}
	private int offset;

	/**
	 * typeCheck
	 */
	public void typeCheck(Type retType) {
		Type type = myExp.typeCheck();

		if (!type.isErrorType() && !type.isBoolType()) {
			ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
					"Non-bool expression used as an if condition");        
		}

		myThenStmtList.typeCheck(retType);
		myElseStmtList.typeCheck(retType);
	}

	public void unparse(PrintWriter p, int indent) {
		doIndent(p, indent);
		p.print("if (");
		myExp.unparse(p, 0);
		p.println(") {");
		myThenDeclList.unparse(p, indent+4);
		myThenStmtList.unparse(p, indent+4);
		doIndent(p, indent);
		p.println("}");
		doIndent(p, indent);
		p.println("else {");
		myElseDeclList.unparse(p, indent+4);
		myElseStmtList.unparse(p, indent+4);
		doIndent(p, indent);
		p.println("}");        
	}

	public void codeGen(){
		String trueLabel = Codegen.nextLabel();
		String falseLabel = Codegen.nextLabel();
		String doneLabel = Codegen.nextLabel();

		myExp.genJumpCode(trueLabel, falseLabel);
		Codegen.genLabel(trueLabel);
		myThenStmtList.codeGen();
		Codegen.generate("b", doneLabel);
		Codegen.genLabel(falseLabel);
		myElseStmtList.codeGen();
		Codegen.genLabel(doneLabel);
	}

	// 5 kids
	private ExpNode myExp;
	private DeclListNode myThenDeclList;
	private StmtListNode myThenStmtList;
	private StmtListNode myElseStmtList;
	private DeclListNode myElseDeclList;
}

class WhileStmtNode extends StmtNode {
	public WhileStmtNode(ExpNode exp, DeclListNode dlist, StmtListNode slist) {
		myExp = exp;
		myDeclList = dlist;
		myStmtList = slist;
	}

	/**
	 * nameAnalysis
	 * Given a symbol table symTab, do:
	 * - process the condition
	 * - enter a new scope
	 * - process the decls and stmts
	 * - exit the scope
	 */
	public void nameAnalysis(SymTable symTab) {
		myExp.nameAnalysis(symTab);
		symTab.addScope();
		isGlobal = false;
		myDeclList.setOffset(this.offset);
		myStmtList.setOffset(myDeclList.getOffset());

		myDeclList.nameAnalysis(symTab);
		myStmtList.nameAnalysis(symTab);
		try {
			symTab.removeScope();
		} catch (EmptySymTableException ex) {
			System.err.println("Unexpected EmptySymTableException " +
					" in IfStmtNode.nameAnalysis");
			System.exit(-1);        
		}
	}

	public void setOffset(int offset){
		this.offset = offset;
	}
	public int getOffset(){
		return myStmtList.getOffset();
	}

	public int getSize(){
		return 0;
	}
	private int offset;

	/**
	 * typeCheck
	 */
	public void typeCheck(Type retType) {
		Type type = myExp.typeCheck();

		if (!type.isErrorType() && !type.isBoolType()) {
			ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
					"Non-bool expression used as a while condition");        
		}

		myStmtList.typeCheck(retType);
	}

	public void unparse(PrintWriter p, int indent) {
		doIndent(p, indent);
		p.print("while (");
		myExp.unparse(p, 0);
		p.println(") {");
		myDeclList.unparse(p, indent+4);
		myStmtList.unparse(p, indent+4);
		doIndent(p, indent);
		p.println("}");
	}

	public void codeGen(){
		String whileStartLabel = Codegen.nextLabel();
		String doneLabel = Codegen.nextLabel();

		Codegen.genLabel(whileStartLabel);	
		myExp.codeGen();
		Codegen.genPop(Codegen.T0);
		Codegen.generate("beq",Codegen.T0, "$zero",  doneLabel);
		myStmtList.codeGen();
		Codegen.generate("b", whileStartLabel);	
		Codegen.genLabel(doneLabel);
	}

	// 3 kids
	private ExpNode myExp;
	private DeclListNode myDeclList;
	private StmtListNode myStmtList;
}

class CallStmtNode extends StmtNode {
	public CallStmtNode(CallExpNode call) {
		myCall = call;
	}

	/**
	 * nameAnalysis
	 * Given a symbol table symTab, perform name analysis on this node's child
	 */
	public void nameAnalysis(SymTable symTab) {
		myCall.nameAnalysis(symTab);
	}

	public void setOffset(int offset){
		this.offset = offset;
	}
	public int getOffset(){
		return this.offset;
	}

	public int getSize(){
		return 0;
	}
	private int offset;

	/**
	 * typeCheck
	 */
	public void typeCheck(Type retType) {
		myCall.typeCheck();
	}

	public void unparse(PrintWriter p, int indent) {
		doIndent(p, indent);
		myCall.unparse(p, indent);
		p.println(";");
	}

	public void codeGen(){
		myCall.codeGen();
		Codegen.genPop(Codegen.V0);
	}

	// 1 kid
	private CallExpNode myCall;
}

class ReturnStmtNode extends StmtNode {
	public ReturnStmtNode(ExpNode exp) {
		myExp = exp;
	}

	/**
	 * nameAnalysis
	 * Given a symbol table symTab, perform name analysis on this node's child,
	 * if it has one
	 */
	public void nameAnalysis(SymTable symTab) {
		if (myExp != null) {
			myExp.nameAnalysis(symTab);
		}
	}

	public void setOffset(int offset){
		this.offset = offset;
	}
	public int getOffset(){
		return this.offset;
	}

	public int getSize(){
		return 0;
	}
	private int offset;


	/**
	 * typeCheck
	 */
	public void typeCheck(Type retType) {
		if (myExp != null) {  // return value given
			Type type = myExp.typeCheck();

			if (retType.isVoidType()) {
				ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
						"Return with a value in a void function");                
			}

			else if (!retType.isErrorType() && !type.isErrorType() && !retType.equals(type)){
				ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
						"Bad return value");
			}
		}

		else {  // no return value given -- ok if this is a void function
			if (!retType.isVoidType()) {
				ErrMsg.fatal(0, 0, "Missing return value");                
			}
		}

	}

	public void unparse(PrintWriter p, int indent) {
		doIndent(p, indent);
		p.print("return");
		if (myExp != null) {
			p.print(" ");
			myExp.unparse(p, 0);
		}
		p.println(";");
	}

	public void codeGen(){
	}
	public void codeGen(String retLabel){
		if(myExp != null){
			myExp.codeGen();
			Codegen.genPop(Codegen.V0);
		}
		Codegen.generate("b", retLabel); //jump to the return label
	}

	// 1 kid
	private ExpNode myExp; // possibly null
}

// **********************************************************************
// ExpNode and its subclasses
// **********************************************************************

abstract class ExpNode extends ASTnode {
	/**
	 * Default version for nodes with no names
	 */
	public void nameAnalysis(SymTable symTab) { }

	abstract public Type typeCheck();
	abstract public int lineNum();
	abstract public int charNum();
	abstract public void genJumpCode(String trueLabel,String falseLabel);

}

class IntLitNode extends ExpNode {
	public IntLitNode(int lineNum, int charNum, int intVal) {
		myLineNum = lineNum;
		myCharNum = charNum;
		myIntVal = intVal;
	}

	/**
	 * Return the line number for this literal.
	 */
	public int lineNum() {
		return myLineNum;
	}

	/**
	 * Return the char number for this literal.
	 */
	public int charNum() {
		return myCharNum;
	}

	/**
	 * typeCheck
	 */
	public Type typeCheck() {
		return new IntType();
	}

	public void unparse(PrintWriter p, int indent) {
		p.print(myIntVal);
	}


	public void codeGen(){
		Codegen.generate("li", Codegen.T0, myIntVal);
		Codegen.genPush(Codegen.T0);				
	}
	public void genJumpCode(String trueLabel, String falseLabel){

	}

	private int myLineNum;
	private int myCharNum;
	private int myIntVal;
}

class StringLitNode extends ExpNode {
	public StringLitNode(int lineNum, int charNum, String strVal) {
		myLineNum = lineNum;
		myCharNum = charNum;
		myStrVal = strVal;
	}

	/**
	 * Return the line number for this literal.
	 */
	public int lineNum() {
		return myLineNum;
	}

	/**
	 * Return the char number for this literal.
	 */
	public int charNum() {
		return myCharNum;
	}

	/**
	 * typeCheck
	 */
	public Type typeCheck() {
		return new StringType();
	}

	public void unparse(PrintWriter p, int indent) {
		p.print(myStrVal);
	}

	public void codeGen(){
		Codegen.generate(".data");
		String strLabel ;
		if(strLitMap.containsKey(myStrVal) == false){
			strLabel = Codegen.nextLabel();
			Codegen.generateLabeled(strLabel, ".asciiz", "", " " + myStrVal);
			strLitMap.put(myStrVal, strLabel);
		}else{
			strLabel = strLitMap.get(myStrVal);
		}

		Codegen.generate(".text");
		Codegen.generate("la", Codegen.T0, strLabel);
		Codegen.genPush(Codegen.T0);
	}
	public void genJumpCode(String trueLabel, String falseLabel){

	}

	private int myLineNum;
	private int myCharNum;
	private String myStrVal;
}

class TrueNode extends ExpNode {
	public TrueNode(int lineNum, int charNum) {
		myLineNum = lineNum;
		myCharNum = charNum;
	}

	/**
	 * Return the line number for this literal.
	 */
	public int lineNum() {
		return myLineNum;
	}

	/**
	 * Return the char number for this literal.
	 */
	public int charNum() {
		return myCharNum;
	}

	/**
	 * typeCheck
	 */
	public Type typeCheck() {
		return new BoolType();
	}

	public void unparse(PrintWriter p, int indent) {
		p.print("true");
	}

	public void codeGen(){
		Codegen.generate("li", Codegen.T0, Codegen.TRUE);
		Codegen.genPush(Codegen.T0);				
	}
	public void genJumpCode(String trueLabel, String falseLabel){
		Codegen.generate("b", trueLabel);		
	}

	private int myLineNum;
	private int myCharNum;
}

class FalseNode extends ExpNode {
	public FalseNode(int lineNum, int charNum) {
		myLineNum = lineNum;
		myCharNum = charNum;
	}

	/**
	 * Return the line number for this literal.
	 */
	public int lineNum() {
		return myLineNum;
	}

	/**
	 * Return the char number for this literal.
	 */
	public int charNum() {
		return myCharNum;
	}

	/**
	 * typeCheck
	 */
	public Type typeCheck() {
		return new BoolType();
	}

	public void unparse(PrintWriter p, int indent) {
		p.print("false");
	}

	public void codeGen(){
		Codegen.generate("li", Codegen.T0, Codegen.FALSE);
		Codegen.genPush(Codegen.T0);				
	}
	public void genJumpCode(String trueLabel, String falseLabel){
		Codegen.generate("b", falseLabel);		
	}

	private int myLineNum;
	private int myCharNum;
}

class IdNode extends ExpNode {
	public IdNode(int lineNum, int charNum, String strVal) {
		myLineNum = lineNum;
		myCharNum = charNum;
		myStrVal = strVal;
	}

	public void setIsGlobal(){
		mySym.setIsGlobal();
	}
	public boolean isGlobal(){
		return mySym.isGlobal();
	}

	/**
	 * Link the given symbol to this ID.
	 */
	public void link(SemSym sym) {
		mySym = sym;
	}

	/**
	 * Return the name of this ID.
	 */
	public String name() {
		return myStrVal;
	}

	/**
	 * Return the symbol associated with this ID.
	 */
	public SemSym sym() {
		return mySym;
	}

	/**
	 * Return the line number for this ID.
	 */
	public int lineNum() {
		return myLineNum;
	}

	/**
	 * Return the char number for this ID.
	 */
	public int charNum() {
		return myCharNum;
	}    

	/**
	 * nameAnalysis
	 * Given a symbol table symTab, do:
	 * - check for use of undeclared name
	 * - if ok, link to symbol table entry
	 */
	public void nameAnalysis(SymTable symTab) {
		SemSym sym = symTab.lookupGlobal(myStrVal);
		if (sym == null) {
			ErrMsg.fatal(myLineNum, myCharNum, "Undeclared identifier");
		} else {
			link(sym);
		}
	}

	public void setOffset(int offset){
		mySym.setOffset(offset);
	}
	public int getOffset(){
		return mySym.getOffset();
	}
	public void setFnSize(int size){
	}

	/**
	 * typeCheck
	 */
	public Type typeCheck() {
		if (mySym != null) {
			return mySym.getType();
		} 
		else {
			System.err.println("ID with null sym field in IdNode.typeCheck");
			System.exit(-1);
		}
		return null;
	}

	public void unparse(PrintWriter p, int indent) {
		p.print(myStrVal);
		if (mySym != null) {
			p.print("(" + mySym + ")");
		}
		p.print("("+mySym.getOffset()+")");
	}

	public void codeGen_GlobalDecl(){
		if(mySym.isGlobal() == true){
			Codegen.generate(".data");
			Codegen.generate(".align 2");
			Codegen.generateLabeled("_"+myStrVal, ".space 4",null);
		}
	}
	public void genJumpAndLink(){
		if(myStrVal.equals("main"))
			Codegen.generate("jal", myStrVal);
		else
			Codegen.generate("jal", "_"+myStrVal);
	}
	public void codeGen(){
		if(mySym.isGlobal() == true)
			Codegen.generate("lw", Codegen.T0, "_"+myStrVal);
		else
			Codegen.generateIndexed("lw", Codegen.T0, Codegen.FP, mySym.getOffset());
		Codegen.genPush(Codegen.T0);
	}
	public void genAddr(){
		if(mySym.isGlobal() == true)
			Codegen.generate("la", Codegen.T0, "_"+myStrVal);
		else
			Codegen.generateIndexed("la", Codegen.T0, Codegen.FP, mySym.getOffset());
		Codegen.genPush(Codegen.T0);
	}	
	public void genJumpCode(String trueLabel, String falseLabel){
		assert(mySym.getType().isBoolType());
		if(mySym.isGlobal() == true)
			Codegen.generate("lw", Codegen.T0, "_"+myStrVal);
		else
			Codegen.generateIndexed("lw", Codegen.T0, Codegen.FP, mySym.getOffset());
		Codegen.generate("beq", Codegen.T0, "$zero", falseLabel);
		Codegen.generate("b", trueLabel);
	}

	private int myLineNum;
	private int myCharNum;
	private String myStrVal;
	private SemSym mySym;
	private static boolean isGlobal = false;

}

class DotAccessExpNode extends ExpNode {
	public DotAccessExpNode(ExpNode loc, IdNode id) {
		myLoc = loc;    
		myId = id;
		mySym = null;
	}

	/**
	 * Return the symbol associated with this dot-access node.
	 */
	public SemSym sym() {
		return mySym;
	}    

	/**
	 * Return the line number for this dot-access node. 
	 * The line number is the one corresponding to the RHS of the dot-access.
	 */
	public int lineNum() {
		return myId.lineNum();
	}

	/**
	 * Return the char number for this dot-access node.
	 * The char number is the one corresponding to the RHS of the dot-access.
	 */
	public int charNum() {
		return myId.charNum();
	}

	/**
	 * nameAnalysis
	 * Given a symbol table symTab, do:
	 * - process the LHS of the dot-access
	 * - process the RHS of the dot-access
	 * - if the RHS is of a struct type, set the sym for this node so that
	 *   a dot-access "higher up" in the AST can get access to the symbol
	 *   table for the appropriate struct definition
	 */
	public void nameAnalysis(SymTable symTab) {
		badAccess = false;
		SymTable structSymTab = null; // to lookup RHS of dot-access
		SemSym sym = null;

		myLoc.nameAnalysis(symTab);  // do name analysis on LHS

		// if myLoc is really an ID, then sym will be a link to the ID's symbol
		if (myLoc instanceof IdNode) {
			IdNode id = (IdNode)myLoc;
			sym = id.sym();

			// check ID has been declared to be of a struct type

			if (sym == null) { // ID was undeclared
				badAccess = true;
			}
			else if (sym instanceof StructSym) { 
				// get symbol table for struct type
				SemSym tempSym = ((StructSym)sym).getStructType().sym();
				structSymTab = ((StructDefSym)tempSym).getSymTable();
			} 
			else {  // LHS is not a struct type
				ErrMsg.fatal(id.lineNum(), id.charNum(), 
						"Dot-access of non-struct type");
				badAccess = true;
			}
		}

		// if myLoc is really a dot-access (i.e., myLoc was of the form
		// LHSloc.RHSid), then sym will either be
		// null - indicating RHSid is not of a struct type, or
		// a link to the Sym for the struct type RHSid was declared to be
		else if (myLoc instanceof DotAccessExpNode) {
			DotAccessExpNode loc = (DotAccessExpNode)myLoc;

			if (loc.badAccess) {  // if errors in processing myLoc
				badAccess = true; // don't continue proccessing this dot-access
			}
			else { //  no errors in processing myLoc
				sym = loc.sym();

				if (sym == null) {  // no struct in which to look up RHS
					ErrMsg.fatal(loc.lineNum(), loc.charNum(), 
							"Dot-access of non-struct type");
					badAccess = true;
				}
				else {  // get the struct's symbol table in which to lookup RHS
					if (sym instanceof StructDefSym) {
						structSymTab = ((StructDefSym)sym).getSymTable();
					}
					else {
						System.err.println("Unexpected Sym type in DotAccessExpNode");
						System.exit(-1);
					}
				}
			}

		}

		else { // don't know what kind of thing myLoc is
			System.err.println("Unexpected node type in LHS of dot-access");
			System.exit(-1);
		}

		// do name analysis on RHS of dot-access in the struct's symbol table
		if (!badAccess) {

			sym = structSymTab.lookupGlobal(myId.name()); // lookup
			if (sym == null) { // not found - RHS is not a valid field name
				ErrMsg.fatal(myId.lineNum(), myId.charNum(), 
						"Invalid struct field name");
				badAccess = true;
			}

			else {
				myId.link(sym);  // link the symbol
				// if RHS is itself as struct type, link the symbol for its struct 
				// type to this dot-access node (to allow chained dot-access)
				if (sym instanceof StructSym) {
					mySym = ((StructSym)sym).getStructType().sym();
				}
			}
		}
	}    

	/**
	 * typeCheck
	 */
	public Type typeCheck() {
		return myId.typeCheck();
	}

	public void unparse(PrintWriter p, int indent) {
		myLoc.unparse(p, 0);
		p.print(".");
		myId.unparse(p, 0);
	}

	public void codeGen(){
	}
	public void genJumpCode(String trueLabel, String falseLabel){

	}

	// 2 kids
	private ExpNode myLoc;    
	private IdNode myId;
	private SemSym mySym;          // link to Sym for struct type
	private boolean badAccess;  // to prevent multiple, cascading errors
}

class AssignNode extends ExpNode {
	public AssignNode(ExpNode lhs, ExpNode exp) {
		myLhs = lhs;
		myExp = exp;
	}

	/**
	 * Return the line number for this assignment node. 
	 * The line number is the one corresponding to the left operand.
	 */
	public int lineNum() {
		return myLhs.lineNum();
	}

	/**
	 * Return the char number for this assignment node.
	 * The char number is the one corresponding to the left operand.
	 */
	public int charNum() {
		return myLhs.charNum();
	}

	/**
	 * nameAnalysis
	 * Given a symbol table symTab, perform name analysis on this node's 
	 * two children
	 */
	public void nameAnalysis(SymTable symTab) {
		myLhs.nameAnalysis(symTab);
		myExp.nameAnalysis(symTab);
	}

	/**
	 * typeCheck
	 */
	public Type typeCheck() {
		Type typeLhs = myLhs.typeCheck();
		Type typeExp = myExp.typeCheck();
		Type retType = typeLhs;

		if (typeLhs.isFnType() && typeExp.isFnType()) {
			ErrMsg.fatal(lineNum(), charNum(), "Function assignment");
			retType = new ErrorType();
		}

		if (typeLhs.isStructDefType() && typeExp.isStructDefType()) {
			ErrMsg.fatal(lineNum(), charNum(), "Struct name assignment");
			retType = new ErrorType();
		}

		if (typeLhs.isStructType() && typeExp.isStructType()) {
			ErrMsg.fatal(lineNum(), charNum(), "Struct variable assignment");
			retType = new ErrorType();
		}        

		if (!typeLhs.equals(typeExp) && !typeLhs.isErrorType() && !typeExp.isErrorType()) {
			ErrMsg.fatal(lineNum(), charNum(), "Type mismatch");
			retType = new ErrorType();
		}

		if (typeLhs.isErrorType() || typeExp.isErrorType()) {
			retType = new ErrorType();
		}

		return retType;
	}

	public void unparse(PrintWriter p, int indent) {
		if (indent != -1)  p.print("(");
		myLhs.unparse(p, 0);
		p.print(" = ");
		myExp.unparse(p, 0);
		if (indent != -1)  p.print(")");
	}

	public void codeGen(){
		myExp.codeGen();
		assert(myLhs instanceof IdNode);
		((IdNode)myLhs).genAddr();
		Codegen.genPop(Codegen.T0);	
		Codegen.generateIndexed("lw", Codegen.T1, Codegen.SP, 4);
		Codegen.generateIndexed("sw", Codegen.T1, Codegen.T0, 0);
	}
	public void genJumpCode(String trueLabel, String falseLabel){
		myExp.codeGen();
		assert(myLhs instanceof IdNode);
		((IdNode)myLhs).genAddr();
		Codegen.genPop(Codegen.T0);	
		//generateIndexed("lw", Codegen.T1, Codegen.SP, 4); 
		Codegen.genPop(Codegen.T1);
		Codegen.generateIndexed("sw", Codegen.T1, Codegen.T0, 0);
		Codegen.generate("beq", Codegen.T1, "$zero", falseLabel);
		Codegen.generate("b", trueLabel);
	}

	// 2 kids
	private ExpNode myLhs;
	private ExpNode myExp;
}

class CallExpNode extends ExpNode {
	public CallExpNode(IdNode name, ExpListNode elist) {
		myId = name;
		myExpList = elist;
	}

	public CallExpNode(IdNode name) {
		myId = name;
		myExpList = new ExpListNode(new LinkedList<ExpNode>());
	}

	/**
	 * Return the line number for this call node. 
	 * The line number is the one corresponding to the function name.
	 */
	public int lineNum() {
		return myId.lineNum();
	}

	/**
	 * Return the char number for this call node.
	 * The char number is the one corresponding to the function name.
	 */
	public int charNum() {
		return myId.charNum();
	}

	/**
	 * nameAnalysis
	 * Given a symbol table symTab, perform name analysis on this node's 
	 * two children
	 */
	public void nameAnalysis(SymTable symTab) {
		myId.nameAnalysis(symTab);
		myExpList.nameAnalysis(symTab);
	}  

	/**
	 * typeCheck
	 */
	public Type typeCheck() {
		if (!myId.typeCheck().isFnType()) {  
			ErrMsg.fatal(myId.lineNum(), myId.charNum(), 
					"Attempt to call a non-function");
			return new ErrorType();
		}

		FnSym fnSym = (FnSym)(myId.sym());

		if (fnSym == null) {
			System.err.println("null sym for Id in CallExpNode.typeCheck");
			System.exit(-1);
		}

		if (myExpList.size() != fnSym.getNumParams()) {
			ErrMsg.fatal(myId.lineNum(), myId.charNum(), 
					"Function call with wrong number of args");
			return fnSym.getReturnType();
		}

		myExpList.typeCheck(fnSym.getParamTypes());
		return fnSym.getReturnType();
	}

	// ** unparse **
	public void unparse(PrintWriter p, int indent) {
		myId.unparse(p, 0);
		p.print("(");
		if (myExpList != null) {
			myExpList.unparse(p, 0);
		}
		p.print(")");
	}

	public void codeGen(){
		myExpList.codeGen();
		myId.genJumpAndLink();
		Codegen.genPush(Codegen.V0);
	}
	public void genJumpCode(String trueLabel, String falseLabel){
		myExpList.codeGen();
		myId.genJumpAndLink();
		Codegen.generate("beq", Codegen.V0, "$zero", falseLabel);
		Codegen.generate("b", trueLabel);
	}

	// 2 kids
	private IdNode myId;
	private ExpListNode myExpList;  // possibly null
}

abstract class UnaryExpNode extends ExpNode {
	public UnaryExpNode(ExpNode exp) {
		myExp = exp;
	}

	/**
	 * Return the line number for this unary expression node. 
	 * The line number is the one corresponding to the  operand.
	 */
	public int lineNum() {
		return myExp.lineNum();
	}

	/**
	 * Return the char number for this unary expression node.
	 * The char number is the one corresponding to the  operand.
	 */
	public int charNum() {
		return myExp.charNum();
	}

	/**
	 * nameAnalysis
	 * Given a symbol table symTab, perform name analysis on this node's child
	 */
	public void nameAnalysis(SymTable symTab) {
		myExp.nameAnalysis(symTab);
	}

	// one child
	protected ExpNode myExp;
}

abstract class BinaryExpNode extends ExpNode {
	public BinaryExpNode(ExpNode exp1, ExpNode exp2) {
		myExp1 = exp1;
		myExp2 = exp2;
	}

	/**
	 * Return the line number for this binary expression node. 
	 * The line number is the one corresponding to the left operand.
	 */
	public int lineNum() {
		return myExp1.lineNum();
	}

	/**
	 * Return the char number for this binary expression node.
	 * The char number is the one corresponding to the left operand.
	 */
	public int charNum() {
		return myExp1.charNum();
	}

	/**
	 * nameAnalysis
	 * Given a symbol table symTab, perform name analysis on this node's 
	 * two children
	 */
	public void nameAnalysis(SymTable symTab) {
		myExp1.nameAnalysis(symTab);
		myExp2.nameAnalysis(symTab);
	}

	// two kids
	protected ExpNode myExp1;
	protected ExpNode myExp2;
}

// **********************************************************************
// Subclasses of UnaryExpNode
// **********************************************************************

class UnaryMinusNode extends UnaryExpNode {
	public UnaryMinusNode(ExpNode exp) {
		super(exp);
	}

	/**
	 * typeCheck
	 */
	public Type typeCheck() {
		Type type = myExp.typeCheck();
		Type retType = new IntType();

		if (!type.isErrorType() && !type.isIntType()) {
			ErrMsg.fatal(lineNum(), charNum(),
					"Arithmetic operator applied to non-numeric operand");
			retType = new ErrorType();
		}

		if (type.isErrorType()) {
			retType = new ErrorType();
		}

		return retType;
	}

	public void unparse(PrintWriter p, int indent) {
		p.print("(-");
		myExp.unparse(p, 0);
		p.print(")");
	}

	public void codeGen(){
		myExp.codeGen();
		Codegen.genPop(Codegen.T0);
		Codegen.generate("sub", Codegen.T0, "$zero", Codegen.T0);	
		Codegen.genPush(Codegen.T0);
	}
	public void genJumpCode(String trueLabel, String falseLabel){
	}
}

class NotNode extends UnaryExpNode {
	public NotNode(ExpNode exp) {
		super(exp);
	}

	/**
	 * typeCheck
	 */
	public Type typeCheck() {
		Type type = myExp.typeCheck();
		Type retType = new BoolType();

		if (!type.isErrorType() && !type.isBoolType()) {
			ErrMsg.fatal(lineNum(), charNum(),
					"Logical operator applied to non-bool operand");
			retType = new ErrorType();
		}

		if (type.isErrorType()) {
			retType = new ErrorType();
		}

		return retType;
	}

	public void unparse(PrintWriter p, int indent) {
		p.print("(!");
		myExp.unparse(p, 0);
		p.print(")");
	}

	public void codeGen(){
		myExp.codeGen();
		Codegen.genPop(Codegen.T0);
		Codegen.generate("xor", Codegen.T0, Codegen.T0, Codegen.TRUE);
		Codegen.genPush(Codegen.T0);
	}
	public void genJumpCode(String trueLabel, String falseLabel){
		myExp.genJumpCode(falseLabel, trueLabel);
	}
}

// **********************************************************************
// Subclasses of BinaryExpNode
// **********************************************************************

abstract class ArithmeticExpNode extends BinaryExpNode {
	public ArithmeticExpNode(ExpNode exp1, ExpNode exp2) {
		super(exp1, exp2);
	}

	/**
	 * typeCheck
	 */
	public Type typeCheck() {
		Type type1 = myExp1.typeCheck();
		Type type2 = myExp2.typeCheck();
		Type retType = new IntType();

		if (!type1.isErrorType() && !type1.isIntType()) {
			ErrMsg.fatal(myExp1.lineNum(), myExp1.charNum(),
					"Arithmetic operator applied to non-numeric operand");
			retType = new ErrorType();
		}

		if (!type2.isErrorType() && !type2.isIntType()) {
			ErrMsg.fatal(myExp2.lineNum(), myExp2.charNum(),
					"Arithmetic operator applied to non-numeric operand");
			retType = new ErrorType();
		}

		if (type1.isErrorType() || type2.isErrorType()) {
			retType = new ErrorType();
		}

		return retType;
	}
}

abstract class LogicalExpNode extends BinaryExpNode {
	public LogicalExpNode(ExpNode exp1, ExpNode exp2) {
		super(exp1, exp2);
	}

	/**
	 * typeCheck
	 */
	public Type typeCheck() {
		Type type1 = myExp1.typeCheck();
		Type type2 = myExp2.typeCheck();
		Type retType = new BoolType();

		if (!type1.isErrorType() && !type1.isBoolType()) {
			ErrMsg.fatal(myExp1.lineNum(), myExp1.charNum(),
					"Logical operator applied to non-bool operand");
			retType = new ErrorType();
		}

		if (!type2.isErrorType() && !type2.isBoolType()) {
			ErrMsg.fatal(myExp2.lineNum(), myExp2.charNum(),
					"Logical operator applied to non-bool operand");
			retType = new ErrorType();
		}

		if (type1.isErrorType() || type2.isErrorType()) {
			retType = new ErrorType();
		}

		return retType;
	}
}

abstract class EqualityExpNode extends BinaryExpNode {
	public EqualityExpNode(ExpNode exp1, ExpNode exp2) {
		super(exp1, exp2);
	}

	/**
	 * typeCheck
	 */
	public Type typeCheck() {
		Type type1 = myExp1.typeCheck();
		Type type2 = myExp2.typeCheck();
		Type retType = new BoolType();

		if (type1.isVoidType() && type2.isVoidType()) {
			ErrMsg.fatal(lineNum(), charNum(),
					"Equality operator applied to void functions");
			retType = new ErrorType();
		}

		if (type1.isFnType() && type2.isFnType()) {
			ErrMsg.fatal(lineNum(), charNum(),
					"Equality operator applied to functions");
			retType = new ErrorType();
		}

		if (type1.isStructDefType() && type2.isStructDefType()) {
			ErrMsg.fatal(lineNum(), charNum(),
					"Equality operator applied to struct names");
			retType = new ErrorType();
		}

		if (type1.isStructType() && type2.isStructType()) {
			ErrMsg.fatal(lineNum(), charNum(),
					"Equality operator applied to struct variables");
			retType = new ErrorType();
		}        

		if (!type1.equals(type2) && !type1.isErrorType() && !type2.isErrorType()) {
			ErrMsg.fatal(lineNum(), charNum(),
					"Type mismatch");
			retType = new ErrorType();
		}

		if (type1.isErrorType() || type2.isErrorType()) {
			retType = new ErrorType();
		}

		return retType;
	}
}

abstract class RelationalExpNode extends BinaryExpNode {
	public RelationalExpNode(ExpNode exp1, ExpNode exp2) {
		super(exp1, exp2);
	}

	/**
	 * typeCheck
	 */
	public Type typeCheck() {
		Type type1 = myExp1.typeCheck();
		Type type2 = myExp2.typeCheck();
		Type retType = new BoolType();

		if (!type1.isErrorType() && !type1.isIntType()) {
			ErrMsg.fatal(myExp1.lineNum(), myExp1.charNum(),
					"Relational operator applied to non-numeric operand");
			retType = new ErrorType();
		}

		if (!type2.isErrorType() && !type2.isIntType()) {
			ErrMsg.fatal(myExp2.lineNum(), myExp2.charNum(),
					"Relational operator applied to non-numeric operand");
			retType = new ErrorType();
		}

		if (type1.isErrorType() || type2.isErrorType()) {
			retType = new ErrorType();
		}

		return retType;
	}
}

class PlusNode extends ArithmeticExpNode {
	public PlusNode(ExpNode exp1, ExpNode exp2) {
		super(exp1, exp2);
	}

	public void unparse(PrintWriter p, int indent) {
		p.print("(");
		myExp1.unparse(p, 0);
		p.print(" + ");
		myExp2.unparse(p, 0);
		p.print(")");
	}

	public void codeGen(){
		myExp1.codeGen();
		myExp2.codeGen();
		Codegen.genPop(Codegen.T1);
		Codegen.genPop(Codegen.T0);
		Codegen.generate("add", Codegen.T0, Codegen.T0, Codegen.T1);
		Codegen.genPush(Codegen.T0);
	}
	public void genJumpCode(String trueLabel, String falseLabel){
	}
}

class MinusNode extends ArithmeticExpNode {
	public MinusNode(ExpNode exp1, ExpNode exp2) {
		super(exp1, exp2);
	}

	public void unparse(PrintWriter p, int indent) {
		p.print("(");
		myExp1.unparse(p, 0);
		p.print(" - ");
		myExp2.unparse(p, 0);
		p.print(")");
	}
	public void codeGen(){
		myExp1.codeGen();
		myExp2.codeGen();
		Codegen.genPop(Codegen.T1);
		Codegen.genPop(Codegen.T0);
		Codegen.generate("sub", Codegen.T0, Codegen.T0, Codegen.T1);
		Codegen.genPush(Codegen.T0);
	}
	public void genJumpCode(String trueLabel, String falseLabel){
	}
}

class TimesNode extends ArithmeticExpNode {
	public TimesNode(ExpNode exp1, ExpNode exp2) {
		super(exp1, exp2);
	}


	public void unparse(PrintWriter p, int indent) {
		p.print("(");
		myExp1.unparse(p, 0);
		p.print(" * ");
		myExp2.unparse(p, 0);
		p.print(")");
	}
	public void codeGen(){
		myExp1.codeGen();
		myExp2.codeGen();
		Codegen.genPop(Codegen.T1);
		Codegen.genPop(Codegen.T0);
		Codegen.generate("mulo", Codegen.T0, Codegen.T0, Codegen.T1);
		Codegen.genPush(Codegen.T0);
	}
	public void genJumpCode(String trueLabel, String falseLabel){
	}
}

class DivideNode extends ArithmeticExpNode {
	public DivideNode(ExpNode exp1, ExpNode exp2) {
		super(exp1, exp2);
	}

	public void unparse(PrintWriter p, int indent) {
		p.print("(");
		myExp1.unparse(p, 0);
		p.print(" / ");
		myExp2.unparse(p, 0);
		p.print(")");
	}

	public void codeGen(){
		myExp1.codeGen();
		myExp2.codeGen();
		Codegen.genPop(Codegen.T1);
		Codegen.genPop(Codegen.T0);
		Codegen.generate("div", Codegen.T0, Codegen.T0, Codegen.T1);
		Codegen.genPush(Codegen.T0);
	}
	public void genJumpCode(String trueLabel, String falseLabel){
	}
}

class AndNode extends LogicalExpNode {
	public AndNode(ExpNode exp1, ExpNode exp2) {
		super(exp1, exp2);
	}

	public void unparse(PrintWriter p, int indent) {
		p.print("(");
		myExp1.unparse(p, 0);
		p.print(" && ");
		myExp2.unparse(p, 0);
		p.print(")");
	}

	public void codeGen(){
		String andEndLabel = Codegen.nextLabel();		
		myExp1.codeGen();
		Codegen.generateIndexed("lw", Codegen.T0, Codegen.SP, 4); 
		Codegen.generate("beq", Codegen.T0,"$zero", andEndLabel); 
		myExp2.codeGen();
		Codegen.genPop(Codegen.T1);
		Codegen.genPop(Codegen.T0);
		Codegen.generate("and",Codegen.T0, Codegen.T0, Codegen.T1);
		Codegen.genPush(Codegen.T0);
		Codegen.genLabel(andEndLabel);
	}
	public void genJumpCode(String trueLabel, String falseLabel){
		String newLabel = Codegen.nextLabel();
		myExp1.genJumpCode(newLabel, falseLabel);
		Codegen.genLabel(newLabel);
		myExp2.genJumpCode(trueLabel, falseLabel);
	}
}

class OrNode extends LogicalExpNode {
	public OrNode(ExpNode exp1, ExpNode exp2) {
		super(exp1, exp2);
	}

	public void unparse(PrintWriter p, int indent) {
		p.print("(");
		myExp1.unparse(p, 0);
		p.print(" || ");
		myExp2.unparse(p, 0);
		p.print(")");
	}

	public void codeGen(){
		String orEndLabel = Codegen.nextLabel();
		myExp1.codeGen();
		Codegen.generateIndexed("lw", Codegen.T0, Codegen.SP, 4);
		Codegen.generate("bne", Codegen.T0,"$zero", orEndLabel);
		myExp2.codeGen();
		Codegen.genPop(Codegen.T1);
		Codegen.genPop(Codegen.T0);
		Codegen.generate("or",Codegen.T0, Codegen.T0, Codegen.T1);
		Codegen.genPush(Codegen.T0);
		Codegen.genLabel(orEndLabel);
	}
	public void genJumpCode(String trueLabel, String falseLabel){
		String newLabel = Codegen.nextLabel();
		myExp1.genJumpCode(trueLabel, newLabel);
		Codegen.genLabel(newLabel);
		myExp2.genJumpCode(trueLabel, falseLabel);
	}
}

class EqualsNode extends EqualityExpNode {
	public EqualsNode(ExpNode exp1, ExpNode exp2) {
		super(exp1, exp2);
	}

	public void unparse(PrintWriter p, int indent) {
		p.print("(");
		myExp1.unparse(p, 0);
		p.print(" == ");
		myExp2.unparse(p, 0);
		p.print(")");
	}

	public void codeGen(){
		myExp1.codeGen();
		myExp2.codeGen();
		Codegen.genPop(Codegen.T1);
		Codegen.genPop(Codegen.T0);
		Codegen.generate("seq",Codegen.T0, Codegen.T0, Codegen.T1);
		Codegen.genPush(Codegen.T0);
	}
	public void genJumpCode(String trueLabel, String falseLabel){
		myExp1.codeGen();
		myExp2.codeGen();
		Codegen.genPop(Codegen.T1);
		Codegen.genPop(Codegen.T0);
		Codegen.generate("beq", Codegen.T0, Codegen.T1, trueLabel);
		Codegen.generate("b", falseLabel);
	}
}

class NotEqualsNode extends EqualityExpNode {
	public NotEqualsNode(ExpNode exp1, ExpNode exp2) {
		super(exp1, exp2);
	}

	public void unparse(PrintWriter p, int indent) {
		p.print("(");
		myExp1.unparse(p, 0);
		p.print(" != ");
		myExp2.unparse(p, 0);
		p.print(")");
	}


	public void codeGen(){
		myExp1.codeGen();
		myExp2.codeGen();
		Codegen.genPop(Codegen.T1);
		Codegen.genPop(Codegen.T0);
		Codegen.generate("sne",Codegen.T0, Codegen.T0, Codegen.T1);
		Codegen.genPush(Codegen.T0);
	}
	public void genJumpCode(String trueLabel, String falseLabel){
		myExp1.codeGen();
		myExp2.codeGen();
		Codegen.genPop(Codegen.T1);
		Codegen.genPop(Codegen.T0);
		Codegen.generate("bne", Codegen.T0, Codegen.T1, trueLabel);
		Codegen.generate("b", falseLabel);
	}
}

class LessNode extends RelationalExpNode {
	public LessNode(ExpNode exp1, ExpNode exp2) {
		super(exp1, exp2);
	}

	public void unparse(PrintWriter p, int indent) {
		p.print("(");
		myExp1.unparse(p, 0);
		p.print(" < ");
		myExp2.unparse(p, 0);
		p.print(")");
	}

	public void codeGen(){
		myExp1.codeGen();
		myExp2.codeGen();
		Codegen.genPop(Codegen.T1);
		Codegen.genPop(Codegen.T0);
		Codegen.generate("slt",Codegen.T0, Codegen.T0, Codegen.T1);
		Codegen.genPush(Codegen.T0);
	}
	public void genJumpCode(String trueLabel, String falseLabel){
		myExp1.codeGen();
		myExp2.codeGen();
		Codegen.genPop(Codegen.T1);
		Codegen.genPop(Codegen.T0);
		Codegen.generate("blt", Codegen.T0, Codegen.T1, trueLabel);
		Codegen.generate("b", falseLabel);
	}
}

class GreaterNode extends RelationalExpNode {
	public GreaterNode(ExpNode exp1, ExpNode exp2) {
		super(exp1, exp2);
	}

	public void unparse(PrintWriter p, int indent) {
		p.print("(");
		myExp1.unparse(p, 0);
		p.print(" > ");
		myExp2.unparse(p, 0);
		p.print(")");
	}

	public void codeGen(){
		myExp1.codeGen();
		myExp2.codeGen();
		Codegen.genPop(Codegen.T1);
		Codegen.genPop(Codegen.T0);
		Codegen.generate("sgt",Codegen.T0, Codegen.T0, Codegen.T1);
		Codegen.genPush(Codegen.T0);
	}
	public void genJumpCode(String trueLabel, String falseLabel){
		myExp1.codeGen();
		myExp2.codeGen();
		Codegen.genPop(Codegen.T1);
		Codegen.genPop(Codegen.T0);
		Codegen.generate("bgt", Codegen.T0, Codegen.T1, trueLabel);
		Codegen.generate("b", falseLabel);
	}
}

class LessEqNode extends RelationalExpNode {
	public LessEqNode(ExpNode exp1, ExpNode exp2) {
		super(exp1, exp2);
	}

	public void unparse(PrintWriter p, int indent) {
		p.print("(");
		myExp1.unparse(p, 0);
		p.print(" <= ");
		myExp2.unparse(p, 0);
		p.print(")");
	}

	public void codeGen(){
		myExp1.codeGen();
		myExp2.codeGen();
		Codegen.genPop(Codegen.T1);
		Codegen.genPop(Codegen.T0);
		Codegen.generate("sle",Codegen.T0, Codegen.T0, Codegen.T1);
		Codegen.genPush(Codegen.T0);
	}
	public void genJumpCode(String trueLabel, String falseLabel){
		myExp1.codeGen();
		myExp2.codeGen();
		Codegen.genPop(Codegen.T1);
		Codegen.genPop(Codegen.T0);
		Codegen.generate("ble", Codegen.T0, Codegen.T1, trueLabel);
		Codegen.generate("b", falseLabel);
	}}

class GreaterEqNode extends RelationalExpNode {
	public GreaterEqNode(ExpNode exp1, ExpNode exp2) {
		super(exp1, exp2);
	}

	public void unparse(PrintWriter p, int indent) {
		p.print("(");
		myExp1.unparse(p, 0);
		p.print(" >= ");
		myExp2.unparse(p, 0);
		p.print(")");
	}
	public void codeGen(){
		myExp1.codeGen();
		myExp2.codeGen();
		Codegen.genPop(Codegen.T1);
		Codegen.genPop(Codegen.T0);
		Codegen.generate("sge",Codegen.T0, Codegen.T0, Codegen.T1);
		Codegen.genPush(Codegen.T0);
	}
	public void genJumpCode(String trueLabel, String falseLabel){
		myExp1.codeGen();
		myExp2.codeGen();
		Codegen.genPop(Codegen.T1);
		Codegen.genPop(Codegen.T0);
		Codegen.generate("bge", Codegen.T0, Codegen.T1, trueLabel);
		Codegen.generate("b", falseLabel);
	}
}
