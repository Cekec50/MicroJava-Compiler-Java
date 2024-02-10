package rs.ac.bg.etf.pp1;

import rs.etf.pp1.symboltable.concepts.Struct;

public class Sender {

	protected int codeOp;
	
	public int value;
	
	public Struct struct;
	
	public int getCodeOp() {
		return codeOp;
	}
	
	
	
	public static class CodeSender extends Sender{
		
		public CodeSender(int codeOp) {
			this.codeOp = codeOp;
		}
	}
	
	public static class ValueTypeSender extends Sender{
		
		public ValueTypeSender(int value, Struct struct) {
			this.value = value;
			this.struct = struct;
		}
	}
	
}
