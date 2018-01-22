int globalInt;
bool globalBool;

struct st1{
	int fieldInt1;
	bool fieldBool1;
};

struct st2{
	int fieldInt2;
	bool fieldBool2;
	struct st1 stIns1;
};

struct st3{
	int fieldInt3;
	bool fieldBool3;
	struct st2 stIns2;
};

struct st3 globalSt;

int function1(int para1, bool para2)
{
int localInt1;
int localInt2;
bool globalFlag;

//Test declarations
globalInt = 25;
globalFlag = true;
	para1 = localInt1+ localInt2;
	para1 = localInt1- localInt2;
	para1 = localInt1* localInt2;
	para1 = localInt1/ localInt2;
	para1 = -localInt1;
	para1 = localInt1- -localInt1/localInt2;
	para1 = localInt1= localInt2;
	para1 = localInt1-localInt1*localInt1/localInt1+localInt1;
globalFlag = localInt1== localInt2;
	globalFlag = localInt1!= localInt2;
	globalFlag = localInt1>  localInt2;
	globalFlag = localInt1<  localInt2;
	globalFlag = localInt1>= localInt2;
	globalFlag = localInt1<= localInt2;
	localInt1++;
	localInt1--;
	globalSt.fieldBool3 = false;
	globalSt.stIns2.stIns1.fieldInt1 = 10;
	globalSt.stIns2.fieldInt2 = 22;

//Test Condition Structures
	if(para1 == localInt1) 
{
}

	if(true){
		int testIfDecl1;
		int testIfDecl2;
		testIfDecl1 = testIfDecl2;
	}

	if(false){
		bool testIfElseDecl;

	} else {
		bool testIfElseDecl;

	}

	while(localInt1 == localInt2){
		int testWhileDecl;
		bool testWhileDeclBool;
		cin >> localInt1;
		cout << "OUTPUT";
	}

	return localInt1+localInt2;
}

int function2(int para1, bool para2){
	int x;
x = function1(para1, para2);
return x;
}

bool function3(bool para1){
	para1 = false;
	return para1;
}

void function4(){
}

void function5(){
	globalInt = function1(3, false);
	if(true){
		function4();
		while(true){
			function4();
		}
	}
}

void function6(int para1, bool para2, int para3, int para4){
				int int1;
				bool bool1;
				cout << int1;
				cout << bool1;
				cin >> int1;
				cin >> bool1;
			}
