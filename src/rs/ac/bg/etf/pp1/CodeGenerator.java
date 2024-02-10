package rs.ac.bg.etf.pp1;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.CounterVisitor.*;
import rs.ac.bg.etf.pp1.Sender.CodeSender;
import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

public class CodeGenerator extends VisitorAdaptor {
	
	
	private int varCount;
	
	private int paramCnt;
	
	private int mainPc;

    private  Stack<Integer> operationStack = new Stack<>();
    
	private  Stack<ArrayList<Integer>> falseJumpFixup = new Stack<>();
	
    private  Stack<ArrayList<Integer>> trueJumpFixup = new Stack<>();
    
    private  Stack<Integer> postElseFixupAdrStack = new Stack<>();
    
    private List<Object> dontGenerateDesignator = new ArrayList<Object>();
    
	Logger log = Logger.getLogger(getClass());
	
	public int getMainPc() {
		return mainPc;
	}
	
	public CodeGenerator() {
		dontGenerateDesignator.add(DesignatorAssignmentClass.class);
		dontGenerateDesignator.add(FuncCallClass.class);
		dontGenerateDesignator.add(DesignatorFunctionClass.class);
		dontGenerateDesignator.add(StatementReadClass.class);
		dontGenerateDesignator.add(DesignatorArrayDesignatorArrayClass.class);
		dontGenerateDesignator.add(DesignatorLeftClass.class);
		dontGenerateDesignator.add(DesignatorRightClass.class);
	}
	 
	public void report_info(String message, SyntaxNode info) {
		StringBuilder msg = new StringBuilder(message); 
		int line = (info == null) ? 0: info.getLine();
		if (line != 0)
			msg.append (" na liniji ").append(line);
		log.info(msg.toString());
	}
	
	public void visit(ProgNameClass programStart) {
		Obj chr = Tab.find("chr");
		chr.setAdr(Code.pc);
		Code.put(Code.enter);
		Code.put(1);
		Code.put(1);
		Code.put(Code.load);
		Code.put(0);
		Code.put(Code.exit);
		Code.put(Code.return_);
		
		Obj ord = Tab.find("ord");
		ord.setAdr(Code.pc);
		Code.put(Code.enter);
		Code.put(1);
		Code.put(1);
		Code.put(Code.load);
		Code.put(0);
		Code.put(Code.exit);
		Code.put(Code.return_);
		
		Obj len = Tab.find("len");
		len.setAdr(Code.pc);
		Code.put(Code.enter);
		Code.put(1);
		Code.put(1);
		Code.put(Code.load);
		Code.put(0);
		Code.put(Code.arraylength);
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	public void visit(MethodTypeNameClass methodTypeName) {
		methodTypeName.obj.setAdr(Code.pc);
		
		// Collect arguments and local variables.
		SyntaxNode methodNode = methodTypeName.getParent();
		VarCounter varCnt = new VarCounter();
		methodNode.traverseTopDown(varCnt);
		FormParamCounter fpCnt = new FormParamCounter();
		methodNode.traverseTopDown(fpCnt);
		
		// Generate the entry.
		Code.put(Code.enter);
		Code.put(fpCnt.getCount());
		Code.put(varCnt.getCount() + fpCnt.getCount());
	}
	
	 
	public void visit(MethodVoidNameClass methodTypeName) {
		if ("main".equalsIgnoreCase(methodTypeName.getMethName())) {
			mainPc = Code.pc;
		}
		methodTypeName.obj.setAdr(Code.pc);
		
		// Collect arguments and local variables.
		SyntaxNode methodNode = methodTypeName.getParent();
		VarCounter varCnt = new VarCounter();
		methodNode.traverseTopDown(varCnt);
		FormParamCounter fpCnt = new FormParamCounter();
		methodNode.traverseTopDown(fpCnt);
		
		// Generate the entry.
		Code.put(Code.enter);
		Code.put(fpCnt.getCount());
		Code.put(varCnt.getCount() + fpCnt.getCount());
	}

	public void visit(MethodDeclClass methodDecl) {
		// umesto ovoga treba Code.trap ?
		if(methodDecl.obj.getType() == Tab.noType) {
			Code.put(Code.exit);
			Code.put(Code.return_);
		}
		else {
			Code.put(Code.trap);
			Code.put(-1);
		}
	}

	// ********************Statement*****************************
	public void visit(PrintStmtClass printStmt) {
		if(printStmt.getExpr().struct == Tab.charType) {
			Code.put(Code.const_1);
			Code.put(Code.bprint);
		}
		else {
			Code.put(Code.const_5);
			Code.put(Code.print);
		}
	}
	
	public void visit(PrintStmtNumberClass printStmt) {
		if(printStmt.getExpr().struct == Tab.charType) {
			Code.load(new Obj(Obj.Con, "$", Tab.intType, printStmt.getNumberAttr(), 0));
			Code.put(Code.bprint);
		}
		else {
			Code.load(new Obj(Obj.Con, "$", Tab.intType, printStmt.getNumberAttr(), 0));
			Code.put(Code.print);
		}
	}
	 
	public void visit(StatementReadClass statementRead) {
		Obj obj = statementRead.getDesignator().obj;
		if(obj.getType() == Tab.charType)
			Code.put(Code.bread);
		else
			Code.put(Code.read);
		Code.store(statementRead.getDesignator().obj);
	}
	
	public void visit(ReturnExprClass returnExpr) {
		// Expr vec postavljen na stack ?
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	 
	public void visit(ReturnNoExprClass returnNoExpr) {
		Code.put(Code.const_m1); // beskorisna vrednost koja ce biti pop()
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	//******************Statement (If Condition)***************************
	public void visit(IfConditionStartClass ifConditionStart) {
		falseJumpFixup.push(new ArrayList<Integer>());
        trueJumpFixup.push(new ArrayList<Integer>());
	}
	
	public void visit(IfConditionEndClass ifConditionEnd) {
		int op = operationStack.pop();
		Code.putFalseJump(op, 0);
		falseJumpFixup.peek().add(Code.pc - 2);
	}
	public void visit(ThenStartClass thenStartAddr) {
		for (int putBackAdr : trueJumpFixup.pop()) {
            Code.fixup(putBackAdr);
        }
        //trueJumpFixup.peek().clear();
	}
	
	public void visit(ThenEndClass thenEndAddr) {
		// samo ako ne postoji ELSE
		for (int putBackAdr : falseJumpFixup.pop()) {
            Code.fixup(putBackAdr);
        }
        //fixupAdrStack.peek().clear();
	}
	
	public void visit(ElseStartClass elseStartAddr) {
		Code.putJump(0);
        postElseFixupAdrStack.push(Code.pc - 2);
        
        for (int putBackAdr : falseJumpFixup.pop()) {
            Code.fixup(putBackAdr);
        }
        //falseJumpFixup.peek().clear();
        
	}

	public void visit(ElseEndClass thenEndAddr) {
		int elseFixupAdr = postElseFixupAdrStack.pop();
        Code.fixup(elseFixupAdr);
	}
	
	private Stack<Integer> condFactStartAddr = new Stack<Integer>();
	
	private Stack<ArrayList<Integer>> statementStartJumpFixup = new Stack<>();
	
	private Stack<ArrayList<Integer>> statementEndJumpFixup = new Stack<>();
	
	private Stack<Integer> i_plus_plus = new Stack<Integer>();
	
	
	//*********************Statement (For Loop)******************************
//	FOR LPAREN ForLoopStartClass SEMI CondFactStartClass ForLoopCondFactClass SEMI ForLoopStatementClass RPAREN Statement
//	FOR LPAREN ForLoopStartClass2 SEMI CondFactStartClass ForLoopNoCondFactClass SEMI ForLoopStatementEpsilonClass RPAREN Statement
	public void visit(ForLoopStartClass forLoopStart) {
		statementStartJumpFixup.push(new ArrayList<Integer>());
		statementEndJumpFixup.push(new ArrayList<Integer>());
	}
	
	public void visit(ForLoopStartClass2 forLoopStart) {
		statementStartJumpFixup.push(new ArrayList<Integer>());
		statementEndJumpFixup.push(new ArrayList<Integer>());
	}
	
	public void visit(CondFactStartClass condFactStartAdr) {
		condFactStartAddr.push(Code.pc);
	}
	
	public void visit(ForLoopCondFactClass forLoopCond) {
		int op = operationStack.pop();
		Code.putFalseJump(op, 0);
		statementEndJumpFixup.peek().add(Code.pc - 2); // TO STATEMENT END
		
		Code.putJump(0); 
		statementStartJumpFixup.peek().add(Code.pc - 2); //TO STATEMENT START
		
		i_plus_plus.push(Code.pc);
	}
	
	public void visit(ForLoopNoCondFactClass forLoopCond) {
		Code.put(Code.const_1);
		Code.put(Code.const_1);
		int op = Code.eq;
		Code.putFalseJump(op, 0);
		statementEndJumpFixup.peek().add(Code.pc - 2); // TO STATEMENT END
		
		Code.putJump(0); 
		statementStartJumpFixup.peek().add(Code.pc - 2); //TO STATEMENT START
		
		i_plus_plus.push(Code.pc);
	}
	
	public void visit(ForLoopStatementClass forLoopStatement) {
		Code.putJump(condFactStartAddr.peek());
		for (int fixup: statementStartJumpFixup.peek())
			Code.fixup(fixup);
		statementStartJumpFixup.peek().clear();
		
	}
	
	public void visit(ForLoopStatementEpsilonClass forLoopStatement) {
		Code.putJump(condFactStartAddr.peek());
		for(int fixup: statementStartJumpFixup.peek())
			Code.fixup(fixup);
		statementStartJumpFixup.peek().clear();
	}
	
	public void visit(StatementForLoopClass statement) {
		Code.putJump(i_plus_plus.peek());
		for(int fixup: statementEndJumpFixup.peek())
			Code.fixup(fixup);
		statementEndJumpFixup.peek().clear();
		
		// ciscenje
		condFactStartAddr.pop();
		statementStartJumpFixup.pop();
		statementEndJumpFixup.pop();
		i_plus_plus.pop();
		
	}
	
	public void visit(StatementBreakClass breakStatement) {
		Code.putJump(0);
		statementEndJumpFixup.peek().add(Code.pc - 2);
	}
	
	public void visit(StatementContinueClass continueStatement) {
		Code.putJump(i_plus_plus.peek());
	}
	//********************Statement (Conditions)*****************************
	public void visit(EqualEqualClass eqEq) {
		operationStack.push(Code.eq);
	}
	
	public void visit(NotEqualClass notEq) {
		operationStack.push(Code.ne);	
	}
	
	public void visit(GreaterThanClass eqEq) {
		operationStack.push(Code.gt);
	}
	
	public void visit(GreaterEqualClass notEq) {
		operationStack.push(Code.ge);
	}
	
	public void visit(LesserThanClass eqEq) {
		operationStack.push(Code.lt);
	}
	
	public void visit(LesserEqualClass notEq) {
		operationStack.push(Code.le);
	}
			 
	public void visit(ORoperatorClass orOperator) {
        int op = operationStack.pop();
        Code.putFalseJump(Code.inverse[op], 0); // Code.putJump(op, 0)
        trueJumpFixup.peek().add(Code.pc - 2);

        for (int adrJump : falseJumpFixup.peek()) {
            Code.fixup(adrJump);
        }
        falseJumpFixup.peek().clear();
	}
	 
	public void visit(ANDoperatorClass andOperator) {
		int op = operationStack.pop();
        Code.putFalseJump(op, 0);
        falseJumpFixup.peek().add(Code.pc - 2);
	}
	
	
	public void visit(CondFactNoRelOpClass expr) {
		Code.loadConst(1);
        operationStack.push(Code.eq);
	}
	// ********************Designator Statement*****************************
	public void visit(DesignatorAssignmentClass assignment) {
		Code.store(assignment.getDesignator().obj);
	}
	
	public void visit(DesignatorFunctionClass func) {
		// Parametri su postavljeni
		Obj functionObj = func.getDesignator().obj;
		int offset = functionObj.getAdr() - Code.pc; 
		Code.put(Code.call);
		Code.put2(offset);
		Code.put(Code.pop); // valjda treba ovo ????????????
	}
	
	public void visit(DesignatorIncClass designatorInc) {
		Code.put(Code.const_1);
		Code.put(Code.add);
		Code.store(designatorInc.getDesignator().obj);
		
	}
	
	public void visit(DesignatorDecClass designatorDec) {
		Code.put(Code.const_m1);
		Code.put(Code.add);
		Code.store(designatorDec.getDesignator().obj);
		
	}
	
	ArrayList<Obj> designatorsLeft = new ArrayList<Obj>();
	Obj array_left;
	Obj array_right;
	int storedDesignators= 0;
	int storedArrayLeft = 0;
	int storingAddress = -1;
	int fixupDesignatorArrayEnd = -1;

	// ***********Designator Statement(Designator Array)********************
	public void visit(DesignatorArrayAssignmentClass designatorArrayAssign) {
		Code.load(new Obj(Obj.Con, "$", Tab.intType, designatorsLeft.size(), 0));
		Code.load(array_right);
		Code.put(Code.arraylength);
		Code.putFalseJump(Code.ge, Code.pc + 5);
		Code.put(Code.trap);
		Code.put(-2);
		
		for(int i = designatorsLeft.size() - 1; i >= 0; i--){
			storedDesignators++;
			if(designatorsLeft.get(i) == null) continue;
			Code.load(array_right);
			Code.load(new Obj(Obj.Con, "$", Tab.intType, i, 0));
			if(array_right.getType().getElemType().getKind() == Struct.Char) Code.put(Code.baload); else Code.put(Code.aload);
			Code.store(designatorsLeft.get(i));
		}
		
		Code.load(new Obj(Obj.Con, "$", Tab.intType, storedDesignators, 0));
		Code.load(new Obj(Obj.Con, "$", Tab.intType, storedArrayLeft, 0));

		storingAddress = Code.pc;

		Code.put(Code.dup2);
		Code.put(Code.dup2);
		Code.put(Code.pop);

		Code.load(array_right);
		Code.put(Code.arraylength);
		Code.put(Code.dup_x1);
		Code.put(Code.pop);
		/*//Code.load(new Obj(Obj.Con, "$", Tab.intType, storedDesignators, 0));*/
		Code.putFalseJump(Code.ne, 0);
		fixupDesignatorArrayEnd = Code.pc - 2;

		 
		Code.load(array_left);
		Code.put(Code.dup_x1);
		Code.put(Code.pop);
		/*//Code.load(new Obj(Obj.Con, "$", Tab.intType, storedArrayLeft, 0));*/

		Code.put(Code.dup_x2);
		Code.put(Code.pop);
		Code.put(Code.dup_x2);
		Code.put(Code.pop);

		Code.load(array_right);
		Code.put(Code.dup_x1);
		Code.put(Code.pop);
		/*//Code.load(new Obj(Obj.Con, "$", Tab.intType, storedDesignators, 0));*/
		if(array_right.getType().getElemType().getKind() == Struct.Char) Code.put(Code.baload); else Code.put(Code.aload);
		if(array_left.getType().getElemType().getKind() == Struct.Char) Code.put(Code.bastore); else Code.put(Code.astore);

		

		Code.load(new Obj(Obj.Con, "$", Tab.intType, 1, 0));
		Code.put(Code.add);

		Code.put(Code.dup_x1);
		Code.put(Code.pop);


		Code.load(new Obj(Obj.Con, "$", Tab.intType, 1, 0));
		Code.put(Code.add);

		Code.put(Code.dup_x1);
		Code.put(Code.pop);


		Code.putJump(storingAddress);
		Code.fixup(fixupDesignatorArrayEnd);
		
		Code.put(Code.pop);
		Code.put(Code.pop);
		Code.put(Code.pop);
		Code.put(Code.pop);
		
		designatorsLeft.clear();
		storedDesignators= 0;
		storedArrayLeft = 0;
		storingAddress = -1;
		fixupDesignatorArrayEnd = -1;
				
	}
	
	public void visit(DesignatorLeftClass designatorLeft) {
		array_left = designatorLeft.getDesignator().obj;
	}
	
	public void visit(DesignatorRightClass designatorLeft) {
		array_right = designatorLeft.getDesignator().obj;
	}
	
	public void visit(DesignatorArrayDesignatorArrayClass designatorArray) {
		designatorsLeft.add(designatorArray.getDesignator().obj);
	}
	
	public void visit(DesignatorArrayDesignatorClass designatorArray) {
		designatorsLeft.add(null);
	}
	 
	// ********************Expr*****************************
	public void visit(ExprMinusTermClass minusTerm) {
		Code.put(Code.neg);
	}
	
	public void visit(AddExprClass addExpr) {
		Code.put(addExpr.getAddOp().sender.getCodeOp());
	}
	
	public void visit(AddOpClass addOp) {
		addOp.sender = new CodeSender(Code.add);
	}
	
	public void visit(SubOpClass subOp) {
		subOp.sender = new CodeSender(Code.sub);
	}
	
	// ********************Term*****************************
	public void visit(TermMulopFactorClass termOpFactor) {
		Code.put(termOpFactor.getMulOp().sender.getCodeOp());
	}
	
	public void visit(MulOpClass mulOp) {
		mulOp.sender = new CodeSender(Code.mul);
	}
	
	public void visit(DivOpClass divOp) {
		divOp.sender = new CodeSender(Code.div);
	}
	
	public void visit(ModOpClass modOp) {
		modOp.sender = new CodeSender(Code.rem);
	}
	
	// ********************Factor*****************************
	public void visit(VarClass var) {
		// Designator je ucitan
	}
	
	public void visit(FuncCallClass FuncCall) {
		Obj functionObj = FuncCall.getDesignator().obj;
		int offset = functionObj.getAdr() - Code.pc; 
		Code.put(Code.call);
		Code.put2(offset);
	}
	
	public void visit(IntClass number) {
		Code.load(new Obj(Obj.Con, "$", number.struct, number.getN1(), 0));
	}
	
	public void visit(CharClass chr) {
		Code.load(new Obj(Obj.Con, "", chr.struct, chr.getC1(), 0));
	}
	
	public void visit(BoolClass bool) {
		Code.load(new Obj(Obj.Con, "", bool.struct, bool.getB1()? 1 : 0, 0));
	}
	
	public void visit(NewArrayClass newArray) {
		// Expr je stavljen
		Code.put(Code.newarray);
		if(newArray.getType().struct == Tab.charType) 
			Code.put(0);
		else 
			Code.put(1);
	}
	
	// ********************Designator*****************************
	public void visit(DesignatorIdentClass designator) {
		SyntaxNode parent = designator.getParent();
		if (!dontGenerateDesignator.contains(parent.getClass())) {
			Code.load(designator.obj);
		}
	}

	public void visit(DesignatorNmspIdentClass designator) {
		SyntaxNode parent = designator.getParent();
		if (!dontGenerateDesignator.contains(parent.getClass())) {
			Code.load(designator.obj);
		}
	}
	
	public void visit(DesignatorArrayClass designatorArray) {
		if(designatorArray.getParent().getClass() == VarClass.class) {
			Code.load(designatorArray.obj);
		}
	}
	
	
}

//public void visit(DesignatorMonkeyClass designatorMonkey) {
//Obj array = designatorMonkey.getDesignator().obj;
//
//Code.put(Code.arraylength);
//
//// PHASE1_START
//phase1Start = Code.pc;
//Code.loadConst(1);
//Code.put(Code.sub);
//
//Code.put(Code.dup);
//Code.put(Code.dup);
//Code.loadConst(0);
//Code.putFalseJump(Code.ge, -1);	// skok na sledecu fazu
//phase1Fixup = Code.pc - 2;
//
//Code.load(array);
//Code.put(Code.dup_x1);
//Code.put(Code.pop);
//Code.load(new Obj(Obj.Elem, "", array.getType().getElemType()));
//
//
//Code.put(Code.dup_x1);
//Code.put(Code.pop);
//
//Code.putJump(phase1Start);
//
//Code.fixup(phase1Fixup);
//
//Code.put(Code.pop);
//Code.put(Code.pop);
////PHASE1_END
//
//
//
//// PHASE2_INIT
//Code.load(array);
//Code.put(Code.arraylength);
//
//// PHASE2_START
//phase2Start = Code.pc;
//Code.loadConst(1);
//Code.put(Code.sub);
//
//Code.put(Code.dup);
//Code.loadConst(0);
//Code.putFalseJump(Code.gt, -1);
//phase2Fixup = Code.pc - 2;
//
//Code.put(Code.dup_x2);
//Code.put(Code.pop);
//
//Code.put(Code.dup2);
//
//Code.putFalseJump(Code.lt, -1);
//phase2OnlyPopFixup = Code.pc - 2;
//
//Code.put(Code.dup_x1);
//Code.put(Code.pop);
//
//Code.fixup(phase2OnlyPopFixup);
//Code.put(Code.pop);
//
//Code.put(Code.dup_x1);
//Code.put(Code.pop);
//
//Code.putJump(phase2Start);
//
//Code.fixup(phase2Fixup);
//Code.put(Code.pop);
//
//
//}

// niz@1 = niz[1] + niz[len-1];
//public void visit(DesignatorMonkeyClass designatorMonkey) {
//Obj array = designatorMonkey.getDesignator().obj;
//int number = designatorMonkey.getN2();
//Code.load(new Obj(Obj.Con, "$", Tab.intType, number, 0));
//
//Code.put(Code.dup2);
//
//Code.load(array);
//Code.put(Code.arraylength);
//
//Code.put(Code.dup_x1);
//Code.put(Code.pop);
//Code.put(Code.sub);
//Code.load(new Obj(Obj.Elem, "", array.getType().getElemType()));
//
//Code.put(Code.dup_x2);
//Code.put(Code.pop);
//Code.load(new Obj(Obj.Elem, "", array.getType().getElemType()));
//
//Code.put(Code.add);
//}

//int loopStart = -1;
//int forInRangeStatementEndFixup = -1;
//public void visit(ForInRangeStatementStartClass statementStart) {
//Obj j = ((ForInRangeClass)statementStart.getParent()).getDesignator().obj;
//
//Code.put(Code.dup_x2);
//Code.put(Code.pop);
//Code.put(Code.dup_x2);
//Code.put(Code.pop);
//
//Code.store(j);
//
//
//Code.put(Code.dup_x1);
//Code.put(Code.pop);
//
//loopStart = Code.pc;
//
//Code.load(j);
//Code.put(Code.dup2);
//Code.putFalseJump(Code.gt, 0);
//forInRangeStatementEndFixup = Code.pc - 2;
//Code.store(j);
//}
//
//// for j in range(a, b, inc)
//public void visit(ForInRangeClass statementEnd) {
//Obj j = statementEnd.getDesignator().obj;
//
//Code.put(Code.dup2);
//Code.put(Code.pop);
//Code.load(j);
//Code.put(Code.add);
//Code.store(j);
//Code.putJump(loopStart);
//
//Code.fixup(forInRangeStatementEndFixup);
//
//}
//
//public void visit(UsingNamespaceClass usingNamespaceNode) {
//usingNamespace = usingNamespaceNode.getNmspNameAttr();
//}