package rs.ac.bg.etf.pp1;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Collection;


public class SemanticPass extends VisitorAdaptor {

	private Object object;
	public static Struct  boolType = new Struct(5);
	int printCallCount = 0;
	int varDeclCount = 0;
	String namespaceName = null;
	Obj currentMethod = null;
	int formParamCount = 0;
	List<Struct> actParamList = new ArrayList<Struct>();
	boolean returnFound = false;
	boolean errorDetected = false;
	boolean visitingForLoop = false;
	List<Obj> constDeclList = new ArrayList<Obj>();
	List<Obj> varDeclList = new ArrayList<Obj>();
	List<Obj> varArrayDeclList = new ArrayList<Obj>();
	int nVars;
	
	Logger log = Logger.getLogger(getClass());

	public void report_error(String message, SyntaxNode info) {
		errorDetected = true;
		StringBuilder msg = new StringBuilder(message);
		int line = (info == null) ? 0: info.getLine();
		if (line != 0)
			msg.append (" na liniji ").append(line);
		log.error(msg.toString());
	}

	public void report_info(String message, SyntaxNode info) {
		StringBuilder msg = new StringBuilder(message); 
		int line = (info == null) ? 0: info.getLine();
		if (line != 0)
			msg.append (" na liniji ").append(line);
		log.info(msg.toString());
	}
	
	// **********Program**********
	public void visit(ProgNameClass progName){
    	progName.obj = Tab.insert(Obj.Prog, progName.getProgNameAttr(), Tab.noType);
    	Tab.openScope();
	}
	
	public void visit(ProgramClass program){
    	nVars = Tab.currentScope.getnVars();
    	Tab.chainLocalSymbols(program.getProgName().obj);
    	Tab.closeScope();
    }
	
	public void visit(NamespaceNameClass namespace){
		namespaceName = namespace.getNmspNameAttr();
	}
	
	public void visit(NamespaceClass namespace){
		namespaceName = null;
	}
	// **********Declarations**********
	public void visit(ConstDeclClass constDecl){
		Struct type1 = constDecl.getType().struct;
    	Struct type2 = constDecl.getNumCharBool().struct;
    	// treba assignableTo()?
    	if(!type1.equals(type2)) {
    		report_error("Greska na liniji "+ constDecl.getLine()+" : nekompatibilni tipovi u definiciji konstante", null);
    	}
		String objName;

		if(namespaceName == null) objName = constDecl.getConstNameAttr();
		else objName = namespaceName + "::" + constDecl.getConstNameAttr();
    	Tab.insert(Obj.Con, objName, constDecl.getType().struct);
    	
		for (Obj obj : constDeclList){
	    	type2 = obj.getType();
	    	if(!type1.equals(type2)) {
	    		report_error("Greska na liniji "+ constDecl.getLine()+" : nekompatibilni tipovi u definiciji konstante", null);
	    	}
	    	
			if(namespaceName == null) objName = obj.getName();
			else objName = namespaceName + "::" + obj.getName();
			Tab.insert(obj.getKind(), objName, constDecl.getType().struct);
		}
		constDeclList.clear();
    	
    		
    }
	
	public void visit(ConstDeclMultiClass constDecl){
		constDeclList.add(new Obj(Obj.Con, constDecl.getConstNameAttr(), constDecl.getNumCharBool().struct));
	}
	
	public void visit(VarDeclClass varDecl){
		varDeclCount++;
		// proveriti da li ime vec postoji u tabeli simbola
		report_info("Deklarisana promenljiva "+ varDecl.getVarNameAttr(), varDecl);
		String objName;

		if(namespaceName == null) objName = varDecl.getVarNameAttr();
		else objName = namespaceName + "::" + varDecl.getVarNameAttr();
		
		Tab.insert(Obj.Var, objName, varDecl.getType().struct);
		for (Obj obj : varDeclList) {
			if(namespaceName == null) objName = obj.getName();
			else objName = namespaceName + "::" + obj.getName();
			
			Tab.insert(obj.getKind(), objName, varDecl.getType().struct);
		}
		for (Obj obj : varArrayDeclList) {
			if(namespaceName == null) objName = obj.getName();
			else objName = namespaceName + "::" + obj.getName();
			
			Tab.insert(obj.getKind(), objName, new Struct(Struct.Array, varDecl.getType().struct));
		}
		
		varDeclList.clear();
		varArrayDeclList.clear();
	}
	
	public void visit(VarDeclMultiClass varDecl) {
		varDeclCount++;
		report_info("Deklarisana promenljiva "+ varDecl.getVarNameAttr(), varDecl);
		varDeclList.add(new Obj(Obj.Var, varDecl.getVarNameAttr(), null));
		
	}
	
	public void visit(VarArrayDeclClass varDecl) {
		varDeclCount++;
		// proveriti da li ime vec postoji u tabeli simbola
		report_info("Deklarisan niz "+ varDecl.getVarNameAttr(), varDecl);
		String objName;
		
		if(namespaceName == null) objName = varDecl.getVarNameAttr();
		else objName = namespaceName + "::" + varDecl.getVarNameAttr();
		
		Tab.insert(Obj.Var, objName, new Struct(Struct.Array, varDecl.getType().struct));
		
		for (Obj obj : varDeclList) {
			if(namespaceName == null) objName = obj.getName();
			else objName = namespaceName + "::" + obj.getName();
			
			Tab.insert(obj.getKind(), objName, varDecl.getType().struct);
		}
		for (Obj obj : varArrayDeclList) {
			if(namespaceName == null) objName = obj.getName();
			else objName = namespaceName + "::" + obj.getName();
			
			Tab.insert(obj.getKind(), objName, new Struct(Struct.Array, varDecl.getType().struct));
		}
		
		
		varDeclList.clear();
		varArrayDeclList.clear();
	
	}
	
	public void visit(VarArrayDeclMultiClass varDecl) {
		varDeclCount++;
		report_info("Deklarisan niz "+ varDecl.getVarNameAttr(), varDecl);
		varArrayDeclList.add(new Obj(Obj.Var, varDecl.getVarNameAttr(), null));
		
	}
	
	
	
	public void visit(TypeClass type){
    	Obj typeNode = Tab.find(type.getTypeName());
    	if(typeNode == Tab.noObj){
    		report_error("Greska na liniji " + type.getLine()+ "Nije pronadjen tip " + type.getTypeName() + " u tabeli simbola! ", null);
    		type.struct = Tab.noType;
    	}else{
    		if(Obj.Type == typeNode.getKind()){
    			type.struct = typeNode.getType();
    		}else{
    			report_error("Greska: Ime " + type.getTypeName() + " ne predstavlja tip!", type);
    			type.struct = Tab.noType;
    		}
    	}
    }
	
	public void visit(DesignatorIdentClass designator){
    	Obj obj = Tab.find(designator.getNameAttr());
    	if(namespaceName != null) {
    		Obj obj_namesp = Tab.find(namespaceName + "::" + designator.getNameAttr());
    		if(obj_namesp == Tab.noObj && obj == Tab.noObj) {
    			report_error("Greska na liniji " + designator.getLine()+ " : ime "+designator.getNameAttr()+" nije deklarisano! ", null);
    			designator.obj = Tab.noObj;
    		}
    		else if(obj == Tab.noObj)
    			designator.obj = obj_namesp;
    		else
    			designator.obj = obj;
    	}
    	else if(obj == Tab.noObj){
			report_error("Greska na liniji " + designator.getLine()+ " : ime "+designator.getNameAttr()+" nije deklarisano! ", null);
	    	designator.obj = Tab.noObj;
    	}
    	else
    		designator.obj = obj;
    }
	
	public void visit(DesignatorNmspIdentClass designator){
    	Obj obj = Tab.find(designator.getNmspAttr() + "::" + designator.getNameAttr());
    	if(obj == Tab.noObj){
			report_error("Greska na liniji " + designator.getLine()+ " : ime "+designator.getNameAttr()+" u namespace-u " + designator.getNmspAttr() + " nije deklarisano! ", null);
    	}
    	designator.obj = obj;
    }
	
	public void visit(DesignatorArrayClass designator) {
		Obj array = designator.getDesignator().obj;
		if(array.getType().getKind() != Struct.Array) 
			report_error("Greska na liniji " + designator.getLine()+ " : designator mora biti niz!", null);
		if(designator.getExpr().struct != Tab.intType) 
			report_error("Greska na liniji " + designator.getLine()+ " : expr mora biti tipa 'int'!", null);

		//designator.obj = array;
		designator.obj = new Obj(array.getKind(), array.getName(), array.getType().getElemType());
	}
	
	public void visit(DesignatorFieldClass designator) {
		Obj obj = designator.getDesignator().obj;
		designator.obj = obj;
	}
	
	public void visit(IntClass cnst){
    	cnst.struct = Tab.intType;
    }
    public void visit(IntClass2 cnst){
    	cnst.struct = Tab.intType;
    }
    
    public void visit(CharClass chr){
    	chr.struct = Tab.charType;
    }
    
    public void visit(CharClass2 chr){
    	chr.struct = Tab.charType;
    }
    
    public void visit(BoolClass bool){
    	// Koji tip je bool u tabeli simbola?
    	//bool.struct = Tab.boolType;
    	bool.struct = boolType;
    }
    
    public void visit(BoolClass2 bool){
    	// Koji tip je bool u tabeli simbola?
    	//bool.struct = Tab.boolType;
    	bool.struct = boolType;
    }
    
    public void visit(NewArrayClass newArray) {
    	if(newArray.getExpr().struct != Tab.intType) 
			report_error("Greska na liniji " + newArray.getLine()+ " : expr mora biti tipa 'int'!", null);
    	// nije ovako!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    	newArray.struct = new Struct(Struct.Array, newArray.getType().struct);
    	
    }
	
	
	public void visit(FuncCallClass funcCall){
    	Obj func = funcCall.getDesignator().obj;
    	if(Obj.Meth == func.getKind()){
			report_info("Pronadjen poziv funkcije " + func.getName() + " na liniji " + funcCall.getLine(), null);
			funcCall.struct = func.getType();
    	}else{
			report_error("Greska na liniji " + funcCall.getLine()+" : ime " + func.getName() + " nije funkcija!", null);
			funcCall.struct = Tab.noType;
    	}
    	
    	Collection<Obj> localSymbolCollection = func.getLocalSymbols();
    	ArrayList<Obj> localSymbolList = new ArrayList<Obj>(localSymbolCollection);
    	int paramNumber = func.getLevel();
    	if(paramNumber != actParamList.size()) {
			report_error("Greska na liniji " + funcCall.getLine()+": broj formalnih i stvarnih argumenata nije isti! " + paramNumber + " - " + actParamList.size(), null);

    	}
    	else {
    		for(int i = 0; i < paramNumber; i++) {
        		if(!localSymbolList.get(i).getType().compatibleWith(actParamList.get(i))) {
        			report_error("Greska na liniji " + funcCall.getLine()+": argumenti nisu kompatibilni! " + localSymbolList.get(i).getType().getKind() + " - " + actParamList.get(i).getKind() , null);
        		}
        	}
    	}
    	paramNumber = 0;
    	actParamList.clear();
    }
	
	public void visit(MethodTypeNameClass methodTypeName){
		if(namespaceName != null)
			currentMethod = Tab.insert(Obj.Meth, namespaceName + "::" + methodTypeName.getMethName(), methodTypeName.getType().struct);
		else
			currentMethod = Tab.insert(Obj.Meth, methodTypeName.getMethName(), methodTypeName.getType().struct);
    	methodTypeName.obj = currentMethod;
    	Tab.openScope();
		report_info("Obradjuje se funkcija " + methodTypeName.getMethName(), methodTypeName);
    }
	
	public void visit(MethodVoidNameClass methodTypeName){
		if(namespaceName != null)
			currentMethod = Tab.insert(Obj.Meth,  namespaceName + "::" + methodTypeName.getMethName(), Tab.noType);
		else
			currentMethod = Tab.insert(Obj.Meth, methodTypeName.getMethName(), Tab.noType);
    	methodTypeName.obj = currentMethod;
    	Tab.openScope();
		report_info("Obradjuje se funkcija " + methodTypeName.getMethName(), methodTypeName);
    }
	
	public void visit(MethodDeclClass methodDecl){
    	if(!returnFound && currentMethod.getType() != Tab.noType){
			report_error("Semanticka greska na liniji " + methodDecl.getLine() + ": funkcija " + currentMethod.getName() + " nema return iskaz!", null);
    	}
    	Obj function = Tab.find(currentMethod.getName());
    	function.setLevel(formParamCount);
    	//Tab.insert(function.getKind(), function.getName(), function.getType(), function.getAdr(), formParamCount);
    	Tab.chainLocalSymbols(currentMethod);
    	Tab.closeScope();
    	
    	formParamCount = 0;
    	returnFound = false;
    	currentMethod = null;
    }
	
	public void visit(FormParamClass formParam) {
		formParamCount++;
		// proveriti da li ime vec postoji u tabeli simbola
		report_info("Deklarisan parametar "+ formParam.getParamNameAttr(), formParam);
		Tab.insert(Obj.Var, formParam.getParamNameAttr(), formParam.getType().struct);
	}
	
	public void visit(FormParamArrayClass formParam) {
		formParamCount++;
		// proveriti da li ime vec postoji u tabeli simbola
		report_info("Deklarisan parametar "+ formParam.getParamNameAttr(), formParam);
		Tab.insert(Obj.Var, formParam.getParamNameAttr(), new Struct(Struct.Array, formParam.getType().struct));
	}
	
	// **********Conditions**********
	public void visit(TermFactorClass term){
    	term.struct = term.getFactor().struct;
    }
    
    public void visit(ExprTermClass exprTerm){
    	exprTerm.struct = exprTerm.getTerm().struct;
    }
    
    public void visit(ExprMinusTermClass exprTerm) {
    	if(exprTerm.getTerm().struct != Tab.intType)
    		report_error("Greska na liniji " + exprTerm.getLine() + " : " + "expr mora biti tipa 'int'!", null);
    	exprTerm.struct = exprTerm.getTerm().struct;
    }
    
    public void visit(CondFactRelOpEqualClass condFact) {
    	Struct type1 = condFact.getExpr().struct;
    	Struct type2 = condFact.getExpr().struct;
    	if(!type1.compatibleWith(type2)) {
    		report_error("Greska na liniji " + condFact.getLine() + " : " + "tipovi nisu kompatibilni!", null);
    		condFact.struct = Tab.noType;
    	}
    	else
    		condFact.struct = boolType;
    }
    
    public void visit(CondFactRelOpClass condFact) {
    	Struct type1 = condFact.getExpr().struct;
    	Struct type2 = condFact.getExpr().struct;
    	if(!type1.compatibleWith(type2)) {
    		report_error("Greska na liniji " + condFact.getLine() + " : " + "tipovi nisu kompatibilni!", null);
    		condFact.struct = Tab.noType;
    	}
    	else if(type1.getKind() == Struct.Array || type2.getKind() == Struct.Array) {
    		report_error("Greska na liniji " + condFact.getLine() + " : Uz promenljive tipa klase ili niza mogu se koristiti samo != i ==.", null);
    		condFact.struct = Tab.noType;
    	}
    	else
    		condFact.struct = boolType;
    }
    
    public void visit(CondFactNoRelOpClass condFact) {
    	condFact.struct = condFact.getExpr().struct;
    }
    
    public void visit(CondTermAndClass condTerm) {
    	Struct type1 = condTerm.getCondFact().struct;
    	Struct type2 = condTerm.getCondTerm().struct;
    	if(type1 != boolType || type2 != boolType) {
    		report_error("Greska na liniji " + condTerm.getLine() + " : " + "uslovi moraju biti tipa 'bool'!", null);
    		condTerm.struct = Tab.noType;
    	}
    	else 
    		condTerm.struct = type1;
    }
    
    public void visit(CondTermNoAndClass condTerm) {
    	Struct type1 = condTerm.getCondFact().struct;
    	if(type1 != boolType) {
    		report_error("Greska na liniji " + condTerm.getLine() + " : " + "uslov mora biti tipa 'bool'!", null);
    		condTerm.struct = Tab.noType;
    	}
    	else 
    		condTerm.struct = type1;
    }

    public void visit(ConditionOrClass condition) {
    	Struct type1 = condition.getCondTerm().struct;
    	Struct type2 = condition.getCondition().struct;
    	if(type1 != boolType || type2 != boolType) {
    		report_error("Greska na liniji " + condition.getLine() + " : " + "uslovi moraju biti tipa 'bool'!", null);
    		condition.struct = Tab.noType;
    	}
    	else 
    		condition.struct = type1;
    }
    
    public void visit(ConditionNoOrClass condition) {
    	Struct type1 = condition.getCondTerm().struct;
    	if(type1 != boolType) {
    		report_error("Greska na liniji " + condition.getLine() + " : " + "uslov mora biti tipa 'bool'!", null);
    		condition.struct = Tab.noType;
    	}
    	else 
    		condition.struct = type1;
    }

    public void visit(AddExprClass addExpr){
    	Struct te = addExpr.getExpr().struct;
    	Struct t = addExpr.getTerm().struct;
    	if(te.equals(t) && te == Tab.intType){
    		addExpr.struct = te;
    	}else{
			report_error("Greska na liniji "+ addExpr.getLine()+" : nekompatibilni tipovi u izrazu za sabiranje.", null);
			addExpr.struct = Tab.noType;
    	}
    }
    
    public void visit(TermMulopFactorClass mulExpr) {
    	Struct te = mulExpr.getFactor().struct;
    	Struct t = mulExpr.getTerm().struct;
    	if(te.equals(t) && te == Tab.intType){
    		mulExpr.struct = te;
    	}else{
			report_error("Greska na liniji "+ mulExpr.getLine()+" : nekompatibilni tipovi u izrazu za mnozenje.", null);
			mulExpr.struct = Tab.noType;
    	}
    }
    
    public void visit(StatementIfClass statementIf) {
    	if(statementIf.getCondition().struct != boolType)
    		report_error("Greska na liniji " + statementIf.getLine() + " : " + "uslov mora biti tipa 'bool'!", null);
    }
    
    public void visit(StatementIfElseClass statementIfElse) {
    	if(statementIfElse.getCondition().struct != boolType)
    		report_error("Greska na liniji " + statementIfElse.getLine() + " : " + "uslov mora biti tipa 'bool'!", null);
    }
    
    
    
    public void visit(VarClass var){
    	var.struct = var.getDesignator().obj.getType();
    }
    
    public void visit(ReturnExprClass returnExpr){
    	if(currentMethod == null)
			report_error("Greska na liniji " + returnExpr.getLine() + " : " + "return se mora nalaziti unutar funkcije!", null);
    	else {
    		returnFound = true;
        	Struct currMethType = currentMethod.getType();
        	if(!currMethType.compatibleWith(returnExpr.getExpr().struct))
    			report_error("Greska na liniji " + returnExpr.getLine() + " : " + "tip izraza u return naredbi ne slaze se sa tipom povratne vrednosti funkcije " + currentMethod.getName(), null);
    	}
    }
    
    public void visit(ReturnNoExprClass returnExpr){
    	if(currentMethod == null)
			report_error("Greska na liniji " + returnExpr.getLine() + " : " + "return se mora nalaziti unutar funkcije!", null);
    	else {
    	returnFound = true;
    	Struct currMethType = currentMethod.getType();
    	if(currMethType != Tab.noType)
			report_error("Greska na liniji " + returnExpr.getLine() + " : " + "return mora imati povratnu vrednost!", null);
    	}

    }
    
    
    public void visit(DesignatorAssignmentClass assignment){
    	Obj obj = assignment.getDesignator().obj;
    	if(obj.getKind() != Obj.Var)
    		report_error("Greska na liniji " + assignment.getLine() + " : " + "designator nije promenljiva, element niza ni polje objekta! ", null);

    	if(!assignment.getExpr().struct.assignableTo(obj.getType()))
    		report_error("Greska na liniji " + assignment.getLine() + " : " + "nekompatibilni tipovi u dodeli vrednosti! ", null);
    }
    
    public void visit(DesignatorIncClass designatorInc) {
    	Obj obj = designatorInc.getDesignator().obj;
    	if(obj.getKind() != Obj.Var)
    		report_error("Greska na liniji " + designatorInc.getLine() + " : " + "designator nije promenljiva, element niza ni polje objekta! ", null);
    	if(obj.getType() != Tab.intType)
    		report_error("Greska na liniji " + designatorInc.getLine() + " : " + "designator mora biti tipa 'int'!"+obj.getType().getKind(), null);
    }
    
    public void visit(DesignatorDecClass designatorInc) {
    	Obj obj = designatorInc.getDesignator().obj;
    	if(obj.getKind() != Obj.Var)
    		report_error("Greska na liniji " + designatorInc.getLine() + " : " + "designator nije promenljiva, element niza ni polje objekta! ", null);
    	if(obj.getType() != Tab.intType)
    		report_error("Greska na liniji " + designatorInc.getLine() + " : " + "designator mora biti tipa 'int'!", null);
    }
    
    public void visit(DesignatorFunctionClass designatorFunc){
    	Obj func = designatorFunc.getDesignator().obj;
    	if(Obj.Meth == func.getKind())
			report_info("Pronadjen poziv funkcije " + func.getName() + " na liniji " + designatorFunc.getLine(), null);
    	else
			report_error("Greska na liniji " + designatorFunc.getLine()+" : ime " + func.getName() + " nije funkcija!", null);
    	
    	Collection<Obj> localSymbolCollection = func.getLocalSymbols();
    	ArrayList<Obj> localSymbolList = new ArrayList<Obj>(localSymbolCollection);
    	int paramNumber = func.getLevel();
    	if(paramNumber != actParamList.size()) {
			report_error("Greska na liniji " + designatorFunc.getLine()+": broj formalnih i stvarnih argumenata nije isti! " + paramNumber + " - " + actParamList.size(), null);

    	}
    	else {
    		for(int i = 0; i < paramNumber; i++) {
        		if(!localSymbolList.get(i).getType().compatibleWith(actParamList.get(i))) {
        			report_error("Greska na liniji " + designatorFunc.getLine()+": argumenti nisu kompatibilni! " + localSymbolList.get(i).getType().getKind() + " - " + actParamList.get(i).getKind() , null);
        		}
        	}
    	}
    	paramNumber = 0;
    	actParamList.clear();
    	
    }
    
    public void visit(ActParamMultiClass actParam) {
    	actParamList.add(actParam.getExpr().struct);
    }
    
    public void visit(ActParamClass actParam) {
    	actParamList.add(actParam.getExpr().struct);
    }
    
    public void visit(ForLoopStatementClass forLoopStatement) {
    	visitingForLoop = true;
    }
    
    public void visit(ForLoopStatementEpsilonClass forLoopStatement) {
    	visitingForLoop = true;
    }
    
    public void visit(StatementBreakClass statementBreak) {
    	if(!visitingForLoop)
    		report_error("Greska na liniji " + statementBreak.getLine()+" : 'break' moze stajati samo u 'for' petlji!", null);
    		
    }
    
    public void visit(StatementContinueClass statementBreak) {
    	if(!visitingForLoop)
    		report_error("Greska na liniji " + statementBreak.getLine()+" : 'continue' moze stajati samo u 'for' petlji!", null);
    		
    }
    
    public void visit(StatementForLoopClass forLoop) {
    	visitingForLoop = false;
    }
    
    public void visit(StatementReadClass statementRead) {
    	Obj obj = statementRead.getDesignator().obj;
    	if(obj.getKind() != Obj.Var)
    		report_error("Greska na liniji " + statementRead.getLine() + " : " + "designator nije promenljiva, element niza ni polje objekta! ", null);
    	if(obj.getType() != Tab.intType && obj.getType() != Tab.charType && obj.getType() != boolType) // 5 je bool tip
    		report_error("Greska na liniji " + statementRead.getLine() + " : " + "designator mora biti tipa 'int', 'char' ili 'bool'!", null);
     
    }
    public void visit(PrintStmtClass print) {
		printCallCount++;
		Struct type = print.getExpr().struct;
		if(type != Tab.intType && type != Tab.charType && type != boolType)
    		report_error("Greska na liniji " + print.getLine() + " : " + "expr u 'print' mora biti tipa 'int', 'char' ili 'bool'!", null);
		report_info("Print je tipa " + type.getKind(), null);
	
	}
    
    public void visit(OpenScopeClass openScope) {
    	Tab.openScope();
    }
    
    public void visit(CloseScopeClass openScope) {
    	Tab.closeScope();
    }
    
    public boolean passed(){
    	return !errorDetected;
    }
    
}
