package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;

public class CounterVisitor extends VisitorAdaptor {
	
	protected int count;
	
	public int getCount() {
		return count;
	}
	
	public static class FormParamCounter extends CounterVisitor {

		@Override
		public void visit(FormParamClass formParamDecl1) {
			count++;
		}		

		@Override
		public void visit(FormParamArrayClass formParamDecl1) {
			count++;
		}		
	}
	
	public static class VarCounter extends CounterVisitor {		
		
		@Override
		public void visit(VarDeclClass VarDecl) {
			count++;
		}
		
		@Override
		public void visit(VarArrayDeclClass VarDecl) {
			count++;
		}
	}
}
