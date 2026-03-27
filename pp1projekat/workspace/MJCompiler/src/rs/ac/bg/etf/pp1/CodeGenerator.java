package rs.ac.bg.etf.pp1;

import java.util.List;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.*;

public class CodeGenerator extends VisitorAdaptor {

	private int mainPc;

	public int getMainPc() {
		return mainPc;
	}

	// chr(int e) converts an integer to a character, returning the character with
	// the corresponding ASCII code
	private void generateChrInstruction() {
		Obj methodObj = Tab.find("chr");
		methodObj.setAdr(Code.pc);
		Code.put(Code.enter);
		Code.put(methodObj.getLevel());
		Code.put(methodObj.getLocalSymbols().size());
		Code.put(Code.load_n + 0);
		Code.put(Code.exit);
		Code.put(Code.return_);
	}

	private void generateOrdInstruction() {
		Obj methodObj = Tab.find("ord");
		methodObj.setAdr(Code.pc);
		Code.put(Code.enter);
		Code.put(methodObj.getLevel());
		Code.put(methodObj.getLocalSymbols().size());
		Code.put(Code.load_n + 0);
		Code.put(Code.exit);
		Code.put(Code.return_);
	}

	private void generateLenInstruction() {
		Obj methodObj = Tab.find("len");
		methodObj.setAdr(Code.pc);
		Code.put(Code.enter);
		Code.put(methodObj.getLevel());
		Code.put(methodObj.getLocalSymbols().size());
		Code.put(Code.load_n + 0);
		Code.put(Code.arraylength);
		Code.put(Code.exit);
		Code.put(Code.return_);
	}

	@Override
	public void visit(ProgName ProgName) {
		generateChrInstruction();
		generateOrdInstruction();
		generateLenInstruction();
	}

	@Override
	public void visit(MethodName MethodName) {
		if ("main".equalsIgnoreCase(MethodName.getMethName())) {
			mainPc = Code.pc;
		}
		MethodName.obj.setAdr(Code.pc);
		Code.put(Code.enter);
		Code.put(MethodName.obj.getLevel());
		Code.put(MethodName.obj.getLocalSymbols().size());
	}

	@Override
	public void visit(MethodDecl MethodDecl) {
		Code.put(Code.exit);
		Code.put(Code.return_);
	}

	@Override
	public void visit(IntFactor IntFactor) {
		Obj constObj = new Obj(Obj.Con, "$", Tab.intType);
		constObj.setLevel(0);
		constObj.setAdr(IntFactor.getI1());
		Code.load(constObj);
	}

	@Override
	public void visit(CharFactor CharFactor) {
		Obj constObj = new Obj(Obj.Con, "$", Tab.charType);
		constObj.setLevel(0);
		constObj.setAdr(CharFactor.getC1());
		Code.load(constObj);
	}

	@Override
	public void visit(BoolFactor BoolFactor) {
		Obj constObj = new Obj(Obj.Con, "$", SemanticPass.boolType);
		constObj.setLevel(0);
		constObj.setAdr(BoolFactor.getB1() ? 1 : 0);
		Code.load(constObj);
	}

	@Override
	public void visit(DesignatorFuncCallFactor DesignatorFuncCallFactor) {
		Obj methodObj = DesignatorFuncCallFactor.getDesignator().obj;
		int offset = methodObj.getAdr() - Code.pc;
		Code.put(Code.call);
		Code.put2(offset);
	}

	@Override
	public void visit(StartIdentDesignator StartIdentDesignator) {
		SyntaxNode parentObj = StartIdentDesignator.getParent();
		if (parentObj instanceof ArrayDesignator || parentObj instanceof StartArrayDesignator) {
			Code.load(StartIdentDesignator.obj);
		}
	}

	@Override
	public void visit(FieldDesignator FieldDesignator) {
		if (FieldDesignator.getDesignatorField() instanceof FieldLengthDesignator) {
			Code.load(FieldDesignator.getDesignatorStart().obj);
		}
	}

	@Override
	public void visit(StartFieldDesignator StartFieldDesignator) {
		if (StartFieldDesignator.getDesignatorField() instanceof FieldLengthDesignator) {
			Code.load(StartFieldDesignator.getDesignatorStart().obj);
		}
	}

	@Override
	public void visit(DesignatorFactor DesignatorFactor) {
		if (DesignatorFactor.getDesignator().obj.getName().equals("length")) {
			Code.put(Code.arraylength);
		} else {
			Code.load(DesignatorFactor.getDesignator().obj);
		}
	}

	@Override
	public void visit(NewArrayFactor NewArrayFactor) {
		Code.put(Code.newarray);
		Struct elemType = NewArrayFactor.getType().struct;
		if (elemType == Tab.charType) {
			Code.put(0);
		} else {
			Code.put(1);
		}
	}

	@Override
	public void visit(MulTerm MulTerm) {
		if (MulTerm.getMulop() instanceof MulMuloop) {
			Code.put(Code.mul);
		} else if (MulTerm.getMulop() instanceof DivMuloop) {
			Code.put(Code.div);
		} else if (MulTerm.getMulop() instanceof ModMuloop) {
			Code.put(Code.rem);
		}
	}

	@Override
	public void visit(MinusTermExpr MinusTermExpr) {
		Code.put(Code.neg);
	}

	@Override
	public void visit(AddExpr AddExpr) {
		if (AddExpr.getAddop() instanceof PlusAddop) {
			Code.put(Code.add);
		} else if (AddExpr.getAddop() instanceof MinusAddop) {
			Code.put(Code.sub);
		}
	}

	@Override
	public void visit(ErrorProneDesignatorAssignment ErrorProneDesignatorAssignment) {
		Code.store(ErrorProneDesignatorAssignment.getDesignator().obj);
	}

	@Override
	public void visit(ActParsDesignatorStatement ActParsDesignatorStatement) {
		Obj methodObj = ActParsDesignatorStatement.getDesignator().obj;
		int offset = methodObj.getAdr() - Code.pc;
		Code.put(Code.call);
		Code.put2(offset);
		if (methodObj.getType() != Tab.noType) {
			Code.put(Code.pop);
		}
	}

	@Override
	public void visit(IncDesignatorStatement IncDesignatorStatement) {
		Code.load(IncDesignatorStatement.getDesignator().obj);
		Code.loadConst(1);
		Code.put(Code.add);
		Code.store(IncDesignatorStatement.getDesignator().obj);
	}

	@Override
	public void visit(DecDesignatorStatement DecDesignatorStatement) {
		Code.load(DecDesignatorStatement.getDesignator().obj);
		Code.loadConst(1);
		Code.put(Code.sub);
		Code.store(DecDesignatorStatement.getDesignator().obj);
	}

	@Override
	public void visit(PrintStmt PrintStmt) {
		Struct exprType = PrintStmt.getExpr().struct;
		if (exprType == Tab.intType || exprType == SemanticPass.boolType) {
			Code.loadConst(8);
			Code.put(Code.print);
		} else {
			Code.loadConst(1);
			Code.put(Code.bprint);
		}
	}

	@Override
	public void visit(ReadStmt ReadStmt) {
		Struct designatorType = ReadStmt.getDesignator().obj.getType();
		if (designatorType == Tab.intType || designatorType == SemanticPass.boolType) {
			Code.put(Code.read);
			Code.store(ReadStmt.getDesignator().obj);
		} else {
			Code.put(Code.bread);
			Code.store(ReadStmt.getDesignator().obj);
		}
	}

	@Override
	public void visit(ReturnStmt ReturnStmt) {
		Code.put(Code.exit);
		Code.put(Code.return_);
	}

	@Override
	public void visit(ReturnVoidStmt ReturnVoidStmt) {
		Code.put(Code.exit);
		Code.put(Code.return_);
	}

	int currentRelop;

	@Override
	public void visit(EqualRelop EqualRelop) {
		currentRelop = Code.eq;
	}

	@Override
	public void visit(NotEqualRelop NotEqualRelop) {
		currentRelop = Code.ne;
	}

	@Override
	public void visit(LessRelop LessRelop) {
		currentRelop = Code.lt;
	}

	@Override
	public void visit(LessEqualRelop LessEqualRelop) {
		currentRelop = Code.le;
	}

	@Override
	public void visit(GreaterRelop GreaterRelop) {
		currentRelop = Code.gt;
	}

	@Override
	public void visit(GreaterEqualRelop GreaterEqualRelop) {
		currentRelop = Code.ge;
	}

	List<Integer> relopFalseJumpIntegers_InverseOp = new java.util.ArrayList<>();
	List<Integer> relopFalseJumpIntegers_RealOp = new java.util.ArrayList<>();
	int skipSecondExprJumpAddr;
	int skipFirstCondTermJumpAddr;

	boolean nestedAnd = false;

	@Override
	public void visit(RelopCondFact RelopCondFact) {
		SyntaxNode termNode = RelopCondFact.getParent();
		if (termNode instanceof CondFactExpr) {
			Code.putFalseJump(currentRelop, 0);
			relopFalseJumpIntegers_InverseOp.add(Code.pc - 2);
			return;
		}

		while (termNode != null && !(termNode instanceof Condition)) {
			if (termNode instanceof MultipleCondTerm || termNode instanceof BoolAndCondition) {
				nestedAnd = true;
				break;
			}
			termNode = termNode.getParent();
		}

		if (nestedAnd) {
			Code.putFalseJump(currentRelop, 0);
			relopFalseJumpIntegers_InverseOp.add(Code.pc - 2);
		} else {
			Code.putFalseJump(Code.inverse[currentRelop], 0);
			relopFalseJumpIntegers_RealOp.add(Code.pc - 2);
		}
	}

	@Override
	public void visit(NoRelopCondFact NoRelopCondFact) {
		SyntaxNode termNode = NoRelopCondFact.getParent();
		Obj trueObj = new Obj(Obj.Con, "trueObj", SemanticPass.boolType);
		trueObj.setAdr(1);
		Code.load(trueObj);
		if (termNode instanceof CondFactExpr) {
			Code.putFalseJump(Code.eq, 0);
			relopFalseJumpIntegers_InverseOp.add(Code.pc - 2);
			return;
		}
		while (termNode != null && !(termNode instanceof CondTerm)) {
			termNode = termNode.getParent();
		}

		while (termNode != null && !(termNode instanceof Condition) && !(termNode instanceof BoolOrCondition)) {
			if (termNode instanceof MultipleCondTerm || termNode instanceof BoolAndCondition) {
				nestedAnd = true;
				break;
			}
			termNode = termNode.getParent();
		}

		if (nestedAnd) {
			Code.putFalseJump(currentRelop, 0);
			relopFalseJumpIntegers_InverseOp.add(Code.pc - 2);
		} else {
			Code.putFalseJump(Code.inverse[currentRelop], 0);
			relopFalseJumpIntegers_RealOp.add(Code.pc - 2);
		}
	}

	@Override
	public void visit(CondOR CondOR) {
		if (nestedAnd) {
			Code.putJump(0);
			relopFalseJumpIntegers_RealOp.add(Code.pc - 2);
		}
		relopFalseJumpIntegers_InverseOp.forEach(relopFalseJumpAddr -> Code.fixup(relopFalseJumpAddr));
		relopFalseJumpIntegers_InverseOp.clear();
	}

	@Override
	public void visit(FirstExpr FirstExpr) {
		// if (!nestedAnd) {
		// Code.putJump(0);
		// skipFirstCondTermJumpAddr = Code.pc - 2;
		// }
		for (Integer relopFalseJumpAddr : relopFalseJumpIntegers_RealOp) {
			Code.fixup(relopFalseJumpAddr);
		}
		relopFalseJumpIntegers_RealOp.clear();
	}

	@Override
	public void visit(SecondExpr SecondExpr) {
		Code.putJump(0);
		skipSecondExprJumpAddr = Code.pc - 2;
		for (Integer relopFalseJumpAddr : relopFalseJumpIntegers_InverseOp) {
			Code.fixup(relopFalseJumpAddr);
		}
		if (!nestedAnd) {
			Code.fixup(skipFirstCondTermJumpAddr);
		}
		relopFalseJumpIntegers_InverseOp.clear();
	}

	@Override
	public void visit(CondFactExpr CondFactExpr) {
		Code.fixup(skipSecondExprJumpAddr);
	}

	@Override
	public void visit(ConditionExpr ConditionExpr) {
		Code.fixup(skipSecondExprJumpAddr);
	}

}
