bool globalBool;
struct st1 { int fieldInt1; };
struct st1 globalSt;

void function(int para1, bool para2) {
}

void main() {
    int localInt1;

    // Arithmetic, Type Mismatch and Relational Errors
    localInt1 = function + function;
    globalBool = function == 3;
    globalBool = function > main;
    globalBool = function == function;

    // Function Call Errors
    localInt1();
    function(5);
    function(localInt1, localInt1);
    function(globalSt, globalSt.fieldInt1);
    st1();

    // Struct Errors
    globalSt = 5;
    globalSt = globalSt;
    cin >> st1;
    globalBool = st1 && 25;

    // Void Return
    return 10;

    localInt1 = localInt1 + (!false);
    localInt1 = localInt1 + "concat";
    globalBool = localInt1;

    // Multiple Messages
    globalBool = (localInt1 + true) && (globalBool < true);
}	

int globalInt;


void function1(){
	return;
}

void function2(int para1, int para2, bool para3, bool para4){
	//Attempt to write a function	
	cout << function1;

	//Attempt to write a struct name	
	cout << st1;
	//Attempt to write a struct variable	
	cout << globalSt;
	//Attempt to write void	
	cout << function1();
	//Attempt to read a function	
	cin >> function1;
	//Attempt to read a struct name	
	cin >> st1;
	//Attempt to read a struct variable	
	cin >> globalSt;
	//Attempt to call a non-function	
	st1();
	//Function call with wrong number of args	
	function1(para1);
	//Type of actual does not match type of formal	
	function(para1);
	//Arithmetic operator applied to non-numeric operand	
	para1 = para4 + para1;
	para1 = para2 - para3;
	para1 = para4 * para1;
	para1 = -para2 / para3;
	para3++;
	para4--;
	para1 = -para3 - para1* para4 / -para3;

	//Relational operator applied to non-numeric operand	
	para3 = para1 > para3;
	para3 = para3 < para3;
	para3 = para1 >= para3;
	para3 = para3 <= para3;

	//Logical operator applied to non-bool operand	
	para3 = para1 || para2;
	para3 = para1 && para2;
	para3 = para1 || para2 && para1;
	para3 = !para1 || para2;
	para3 = !para1;

	//Non-bool expression used as an if condition	
	if(para1){
		if( para1 + para2){
		}
		else {
		}
	}

	if( para1+para2 * para2 /para1- para1){
	}

	//Non-bool expression used as a while condition	
	while(para1){
		while(para1+para2 * para2 /para1- para1){
		}
	}

	//Type mismatch	
	para3 = para1 == para4;
	para3 =para1 != para4;



	//Equality operator applied to void functions	
	para3 = function1() == function1();
	if(function1() != function1() ) {}
	
	//Equality operator applied to functions	
	while(function1 == function1) {}
	para3 = function1 != function1;

	//Equality operator applied to struct names	
	if(st1 == st1){
		while(st1 != st1)
		{}
	}

	//Equality operator applied to struct variables	
	para3 = globalSt == globalSt;
	para4 = globalSt != globalSt;

	//Function assignment	
	function1 = function1;

	//Struct name assignment	
	st1 = st1;
	//Struct variable assignment	
	globalSt = globalSt;

	return;
}
//Missing return value	
int function3(){
	return;
}
//Return with a value in a void function	
void function4(){
	return 0;
}
//Bad return value	
int function5(){
	return true;
}

//now test some shits
void function6(){
	cout << function + 1 ;         // P + 1 is an error; the write is OK
	globalSt.fieldInt1 = (true + 3) * 4;      // true + 3 is an error; the * is OK
	globalBool = true && (false || 3);   // false || 3 is an error; the && is OK
	function("a" * 4);            // "a" * 4 is an error; the call is OK
	globalSt.fieldInt1 =1 + function();               // p() is an error; the + is OK
	if((true + 3) == globalSt.fieldInt1){}        // true + 3 is an error; the == is OK
                       // regardless of the type of x

	globalSt.fieldInt1 = true + "hello";    // one error for each of the non-int operands of the +
	globalSt.fieldInt1 = 1 + function(true, 1);       // one for the bad arg type and one for the 2nd operand of the +
	globalSt.fieldInt1 =1 + function(1);       // one for the wrong number of args and one for the 2nd operand of the +
	return 3+true;    // in a void function: one error for the 2nd operand to +
                   	  // and one for returning a value
	globalSt.fieldInt1 = "here";
	globalBool = "shit";
	function = "what";
	globalSt.fieldInt1 = globalSt.fieldInt1 + "hahaha";
	globalBool = ( ( (globalSt.fieldInt1 == true) || true) && true) != true;
	globalBool = ( ( (globalSt.fieldInt1 && true) || true) && true) == true;
}
