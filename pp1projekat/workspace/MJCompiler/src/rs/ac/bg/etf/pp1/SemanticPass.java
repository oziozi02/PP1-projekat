package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.*;

import java.util.Collection;
import java.util.List;
import java.util.Stack;

import org.apache.log4j.Logger;

public class SemanticPass extends VisitorAdaptor {

	Logger log = Logger.getLogger(getClass());

	public static final Struct boolType = new Struct(Struct.Bool);

	private Struct currentType = Tab.noType;

	boolean errorDetected = false;

	public int nVars = 0;

	private boolean isIntOrEnum(Struct struct) {
		if (struct == null) {
			return false;
		}
		return struct == Tab.intType || struct.getKind() == Struct.Enum;
	}

	private String structKindToString(int kind) {
		switch (kind) {
			case Struct.None:
				return "none";
			case Struct.Int:
				return "int";
			case Struct.Char:
				return "char";
			case Struct.Bool:
				return "bool";
			case Struct.Enum:
				return "enum";
			case Struct.Array:
				return "array";
			default:
				return "unknown";
		}
	}

	private String objKindToString(int kind) {
		switch (kind) {
			case Obj.Con:
				return "const";
			case Obj.Var:
				return "var";
			case Obj.Type:
				return "type";
			case Obj.Meth:
				return "method";
			case Obj.Fld:
				return "field";
			case Obj.Prog:
				return "prog";
			case Obj.Elem:
				return "elem";
			default:
				return "unknown";
		}
	}

	public void report_error(String message, int line) {
		errorDetected = true;
		StringBuilder msg = new StringBuilder();
		if (line >= 0)
			msg.append("Greska na liniji ").append(line).append(": ").append(message);
		log.error(msg.toString());
	}

	public void report_info(String message, int line) {
		StringBuilder msg = new StringBuilder(message);
		if (line >= 0)
			msg.append(" na liniji ").append(line);
		log.info(msg.toString());
	}

	public boolean passed() {
		return !errorDetected;
	}

	@Override
	public void visit(ProgName progName) {
		progName.obj = Tab.insert(Obj.Prog, progName.getPName(), Tab.noType);
		Tab.openScope();
	}

	@Override
	public void visit(Program program) {
		Obj mainObj = Tab.find("main");
		nVars = Tab.currentScope.getnVars();
		if (mainObj == Tab.noObj || mainObj.getKind() != Obj.Meth) {
			report_error("Nije pronadjena metoda main", program.getLine());
		}
		Tab.chainLocalSymbols(program.getProgName().obj);
		Tab.closeScope();
	}

	@Override
	public void visit(Type type) {
		Obj typeObj = Tab.find(type.getTypeName());
		if (typeObj == Tab.noObj) {
			report_error("Nije pronadjen tip " + type.getTypeName(), type.getLine());
			type.struct = Tab.noType;
		} else {
			if (Obj.Type == typeObj.getKind()) {
				type.struct = typeObj.getType();
				currentType = type.struct;
			} else {
				report_error("Simbol " + type.getTypeName() + " nije tip", type.getLine());
				type.struct = Tab.noType;
			}
		}
	}

	@Override
	public void visit(Const Const) {
		if (Tab.find(Const.getConstName()) != Tab.noObj) {
			report_error("Simbol " + Const.getConstName() + " je vec definisan", Const.getLine());
			Const.obj = Tab.noObj;
			return;
		}
		if (Const.getLiteral() instanceof IntLiteral && currentType == Tab.intType) {
			Const.obj = Tab.insert(Obj.Con, Const.getConstName(), Tab.intType);
			Const.obj.setAdr(((IntLiteral) Const.getLiteral()).getValue());
		} else if (Const.getLiteral() instanceof BoolLiteral && currentType == boolType) {
			Const.obj = Tab.insert(Obj.Con, Const.getConstName(), boolType);
			Const.obj.setAdr(((BoolLiteral) Const.getLiteral()).getValue() ? 1 : 0);
		} else if (Const.getLiteral() instanceof CharLiteral && currentType == Tab.charType) {
			Const.obj = Tab.insert(Obj.Con, Const.getConstName(), Tab.charType);
			Const.obj.setAdr(((CharLiteral) Const.getLiteral()).getValue());
		} else {
			report_error("Nedozvoljen tip literala za konstantu " + Const.getConstName(), Const.getLine());
			Const.obj = new Obj(Obj.Con, Const.getConstName(), Tab.noType, 0, 0);
		}
	}

	private Obj enumCurrentObj = Tab.noObj;
	private int currentEnumConstValue = 0;

	@Override
	public void visit(EnumName EnumName) {
		if (Tab.find(EnumName.getEnumName()) != Tab.noObj) {
			report_error("Simbol " + EnumName.getEnumName() + " je vec definisan", EnumName.getLine());
			EnumName.struct = Tab.noType;
			return;
		}
		EnumName.struct = new Struct(Struct.Enum);
		enumCurrentObj = Tab.insert(Obj.Type, EnumName.getEnumName(), EnumName.struct);
	}

	@Override
	public void visit(EnumAssigned EnumAssigned) {
		if (enumCurrentObj.getType().getMembersTable().searchKey(EnumAssigned.getEnumConstName()) != null) {
			report_error("Simbol " + EnumAssigned.getEnumConstName() + " je vec definisan u okviru ovog enum-a",
					EnumAssigned.getLine());
			return;
		}
		for (Obj constObj : enumCurrentObj.getType().getMembers()) {
			if (constObj.getAdr() == EnumAssigned.getEnumConstValue()) {
				report_error(
						"Vrednost " + EnumAssigned.getEnumConstValue()
								+ " je vec dodeljena nekoj konstanti u okviru ovog enum-a",
						EnumAssigned.getLine());
				return;
			}
		}
		Obj enumObj = new Obj(Obj.Con, EnumAssigned.getEnumConstName(), Tab.intType);
		enumObj.setAdr(EnumAssigned.getEnumConstValue());
		enumCurrentObj.getType().getMembersTable().insertKey(enumObj);
		EnumAssigned.obj = enumObj;
		currentEnumConstValue = EnumAssigned.getEnumConstValue() + 1;
	}

	@Override
	public void visit(EnumDefined EnumDefined) {
		if (enumCurrentObj.getType().getMembersTable().searchKey(EnumDefined.getEnumConstName()) != null) {
			report_error("Simbol " + EnumDefined.getEnumConstName() + " je vec definisan u okviru ovog enum-a",
					EnumDefined.getLine());
			return;
		}
		for (Obj constObj : enumCurrentObj.getType().getMembers()) {
			if (constObj.getAdr() == currentEnumConstValue) {
				report_error(
						"Vrednost " + currentEnumConstValue
								+ " je vec dodeljena nekoj konstanti u okviru ovog enum-a",
						EnumDefined.getLine());
				return;
			}
		}
		Obj enumObj = new Obj(Obj.Con, EnumDefined.getEnumConstName(), Tab.intType);
		enumObj.setAdr(currentEnumConstValue);
		enumCurrentObj.getType().getMembersTable().insertKey(enumObj);
		EnumDefined.obj = enumObj;
		currentEnumConstValue++;
	}

	@Override
	public void visit(EnumDecl EnumDecl) {
		currentEnumConstValue = 0;
		enumCurrentObj = Tab.noObj;
	}

	@Override
	public void visit(ScalarDecl ScalarDecl) {
		if (Tab.currentScope.findSymbol(ScalarDecl.getVarName()) != null) {
			report_error("Simbol " + ScalarDecl.getVarName() + " je vec definisan", ScalarDecl.getLine());
			return;
		}
		Obj varObj = Tab.insert(Obj.Var, ScalarDecl.getVarName(), currentType);
		ScalarDecl.obj = varObj;
	}

	@Override
	public void visit(VectorDecl VectorDecl) {
		if (Tab.currentScope.findSymbol(VectorDecl.getVarName()) != null) {
			report_error("Simbol " + VectorDecl.getVarName() + " je vec definisan", VectorDecl.getLine());
			return;
		}
		Obj varObj = Tab.insert(Obj.Var, VectorDecl.getVarName(), new Struct(Struct.Array, currentType));
		VectorDecl.obj = varObj;
	}

	private Struct currentReturnType = Tab.noType;
	private Obj currentMethod = Tab.noObj;
	private int currentMethodFormalParamsCount = 0;

	@Override
	public void visit(RegularReturnType RegularReturnType) {
		Obj typeObj = Tab.find(RegularReturnType.getType().getTypeName());
		if (typeObj == Tab.noObj) {
			report_error("Nije pronadjen tip " + RegularReturnType.getType().getTypeName(),
					RegularReturnType.getType().getLine());
			RegularReturnType.getType().struct = Tab.noType;
		} else {
			if (Obj.Type == typeObj.getKind()) {
				RegularReturnType.getType().struct = typeObj.getType();
				currentReturnType = RegularReturnType.getType().struct;
			} else {
				report_error("Simbol " + RegularReturnType.getType().getTypeName() + " nije tip",
						RegularReturnType.getType().getLine());
				RegularReturnType.getType().struct = Tab.noType;
			}
		}
	}

	@Override
	public void visit(MethodName MethodName) {
		if (Tab.find(MethodName.getMethName()) != Tab.noObj) {
			report_error("Metoda " + MethodName.getMethName() + " je vec definisana", MethodName.getLine());
			MethodName.obj = Tab.noObj;
			return;
		}
		if (MethodName.getMethName().equals("main") && currentReturnType != Tab.noType) {
			report_error("Metoda main ne sme imati povratni tip", MethodName.getLine());
			MethodName.obj = Tab.noObj;
			return;
		}
		MethodName.obj = Tab.insert(Obj.Meth, MethodName.getMethName(), currentReturnType);
		currentMethod = MethodName.obj;
		Tab.openScope();
	}

	@Override
	public void visit(FormalParamScalar FormalParamScalar) {
		currentMethodFormalParamsCount++;
		if (currentMethodFormalParamsCount > 0 && currentMethod.getName().equals("main")) {
			report_error("Metoda main ne sme imati formalne parametre", FormalParamScalar.getLine());
			return;
		}
		if (Tab.currentScope.findSymbol(FormalParamScalar.getParamName()) != null) {
			report_error("Formalni parametar " + FormalParamScalar.getParamName() + " je vec definisan",
					FormalParamScalar.getLine());
			return;
		}
		Obj varObj = Tab.insert(Obj.Var, FormalParamScalar.getParamName(), currentType);
		FormalParamScalar.obj = varObj;
	}

	@Override
	public void visit(FormalParamVector FormalParamVector) {
		currentMethodFormalParamsCount++;
		if (currentMethodFormalParamsCount > 0 && currentMethod.getName().equals("main")) {
			report_error("Metoda main ne sme imati formalne parametre", FormalParamVector.getLine());
			return;
		}
		if (Tab.currentScope.findSymbol(FormalParamVector.getParamName()) != null) {
			report_error("Formalni parametar " + FormalParamVector.getParamName() + " je vec definisan",
					FormalParamVector.getLine());
			return;
		}
		Obj varObj = Tab.insert(Obj.Var, FormalParamVector.getParamName(), new Struct(Struct.Array, currentType));
		FormalParamVector.obj = varObj;
	}

	@Override
	public void visit(ReturnStmt ReturnStmt) {
		if (currentMethod == Tab.noObj) {
			report_error("Nedozvoljena upotreba return naredbe van metode", ReturnStmt.getLine());
			return;
		}
		if (ReturnStmt.getExpr() == null) {
			if (currentReturnType != Tab.noType) {
				report_error("Nedozvoljena upotreba return naredbe bez izraza u metodi sa povratnim tipom",
						ReturnStmt.getLine());
			}
		} else {
			if (currentReturnType == Tab.noType) {
				report_error("Nedozvoljena upotreba return naredbe sa izrazom u metodi bez povratnog tipa",
						ReturnStmt.getLine());
			} else if (!ReturnStmt.getExpr().struct.equals(currentReturnType)) {
				report_error("Tip izraza u return naredbi nije isti kao povratni tip metode", ReturnStmt.getLine());
			}
		}
	}

	@Override
	public void visit(ReturnVoidStmt ReturnVoidStmt) {
		if (currentMethod == Tab.noObj) {
			report_error("Nedozvoljena upotreba return naredbe van metode", ReturnVoidStmt.getLine());
			return;
		}
		if (currentReturnType != Tab.noType) {
			report_error("Nedozvoljena upotreba return naredbe bez izraza u metodi sa povratnim tipom",
					ReturnVoidStmt.getLine());
		}
	}

	@Override
	public void visit(MethodDecl MethodDecl) {
		currentMethod.setLevel(currentMethodFormalParamsCount);
		if (!errorDetected) {
			Tab.chainLocalSymbols(currentMethod);
			Tab.closeScope();
		}
		currentMethod = Tab.noObj;
		currentReturnType = Tab.noType;
		currentMethodFormalParamsCount = 0;
	}

	@Override
	public void visit(FieldIdentDesignator FieldIdentDesignator) {
		FieldIdentDesignator.obj = new Obj(Obj.Con, FieldIdentDesignator.getFieldName(), Tab.intType);
	}

	@Override
	public void visit(FieldLengthDesignator FieldLengthDesignator) {
		FieldLengthDesignator.obj = new Obj(Obj.Var, "length", Tab.intType);
	}

	@Override
	public void visit(StartIdentDesignator StartIdentDesignator) {
		StartIdentDesignator.obj = Tab.find(StartIdentDesignator.getName());
		if (StartIdentDesignator.obj == Tab.noObj || StartIdentDesignator.obj.getKind() == Obj.Prog
				|| (StartIdentDesignator.obj.getKind() == Obj.Type
						&& StartIdentDesignator.obj.getType().getKind() != Struct.Enum)) {
			report_error("Simbol " + StartIdentDesignator.getName() + " nije pronadjen",
					StartIdentDesignator.getLine());
			return;
		}
	}

	@Override
	public void visit(StartArrayDesignator StartArrayDesignator) {
		StartArrayDesignator.obj = Tab.noObj;
		Obj arrayObj = StartArrayDesignator.getDesignatorStart().obj;
		if (arrayObj.getType().getKind() != Struct.Array) {
			report_error("Simbol nije niz", StartArrayDesignator.getLine());
			return;
		}
		Struct indexType = StartArrayDesignator.getExpr().struct;
		if (indexType != Tab.intType && indexType.getKind() != Struct.Enum) {
			report_error("Indeks niza nije tipa int", StartArrayDesignator.getLine());
			return;
		}
		StartArrayDesignator.obj = new Obj(Obj.Elem, arrayObj.getName(), arrayObj.getType().getElemType());
		StartArrayDesignator.obj.setLevel(arrayObj.getLevel());
	}

	@Override
	public void visit(StartFieldDesignator StartFieldDesignator) {
		StartFieldDesignator.obj = Tab.noObj;
		Obj recordObj = StartFieldDesignator.getDesignatorStart().obj;
		Obj fieldObj = StartFieldDesignator.getDesignatorField().obj;
		if (recordObj.getType().getKind() == Struct.Enum && fieldObj.getKind() == Obj.Con) {
			Obj newFieldObj = recordObj.getType().getMembersTable().searchKey(fieldObj.getName());
			if (newFieldObj == null) {
				report_error("Simbol " + fieldObj.getName() + " nije pronadjen u okviru ovog enum-a",
						StartFieldDesignator.getLine());
				return;
			}
			StartFieldDesignator.obj = new Obj(Obj.Con, recordObj.getName() + "." + fieldObj.getName(),
					recordObj.getType());
			StartFieldDesignator.obj.setAdr(newFieldObj.getAdr());
		} else if (recordObj.getType().getKind() == Struct.Array && fieldObj.getName().equals("length")) {
			StartFieldDesignator.obj = new Obj(Obj.Var, fieldObj.getName(), Tab.intType);
		} else {
			report_error("Simbol " + fieldObj.getName() + " nije pronadjen u okviru ovog tipa",
					StartFieldDesignator.getLine());
		}
	}

	@Override
	public void visit(IdentDesignator IdentDesignator) {
		IdentDesignator.obj = Tab.find(IdentDesignator.getName());
		if (IdentDesignator.obj == Tab.noObj || IdentDesignator.obj.getKind() == Obj.Prog) {
			report_error("Simbol " + IdentDesignator.getName() + " nije pronadjen", IdentDesignator.getLine());
			return;
		}
		if (IdentDesignator.obj.getKind() == Obj.Type && IdentDesignator.obj.getType().getKind() == Struct.Enum) {
			report_error("Enum tip " + IdentDesignator.getName() + " ne moze biti korišćen kao promenljiva",
					IdentDesignator.getLine());
		}
	}

	@Override
	public void visit(ArrayDesignator ArrayDesignator) {
		ArrayDesignator.obj = Tab.noObj;
		Obj arrayObj = ArrayDesignator.getDesignatorStart().obj;
		if (arrayObj.getType().getKind() != Struct.Array) {
			report_error("Simbol nije niz", ArrayDesignator.getLine());
			return;
		}
		Struct indexType = ArrayDesignator.getExpr().struct;
		if (indexType != Tab.intType && indexType.getKind() != Struct.Enum) {
			report_error("Indeks niza nije tipa int", ArrayDesignator.getLine());
			return;
		}
		ArrayDesignator.obj = new Obj(Obj.Elem, arrayObj.getName(), arrayObj.getType().getElemType());
		ArrayDesignator.obj.setLevel(arrayObj.getLevel());
	}

	@Override
	public void visit(FieldDesignator FieldDesignator) {
		FieldDesignator.obj = Tab.noObj;
		Obj recordObj = FieldDesignator.getDesignatorStart().obj;
		Obj fieldObj = FieldDesignator.getDesignatorField().obj;
		if (recordObj.getType().getKind() == Struct.Enum && fieldObj.getKind() == Obj.Con) {
			Obj newFieldObj = recordObj.getType().getMembersTable().searchKey(fieldObj.getName());
			if (newFieldObj == null) {
				report_error("Simbol " + fieldObj.getName() + " nije pronadjen u okviru ovog enum-a",
						FieldDesignator.getLine());
				return;
			}
			FieldDesignator.obj = new Obj(Obj.Con, recordObj.getName() + "." + fieldObj.getName(), recordObj.getType());
			FieldDesignator.obj.setAdr(newFieldObj.getAdr());
		} else if (recordObj.getType().getKind() == Struct.Array && fieldObj.getName().equals("length")) {
			FieldDesignator.obj = new Obj(Obj.Var, fieldObj.getName(), Tab.intType);
		} else {
			report_error("Simbol " + fieldObj.getName() + " nije pronadjen u okviru ovog tipa",
					FieldDesignator.getLine());
		}
	}

	@Override
	public void visit(ParenFactor ParenFactor) {
		ParenFactor.struct = ParenFactor.getExpr().struct;
	}

	@Override
	public void visit(NewArrayFactor NewArrayFactor) {
		if (NewArrayFactor.getExpr().struct != Tab.intType
				&& NewArrayFactor.getExpr().struct.getKind() != Struct.Enum) {
			report_error("Indeks niza nije tipa int", NewArrayFactor.getLine());
			NewArrayFactor.struct = Tab.noType;
			return;
		}
		NewArrayFactor.struct = new Struct(Struct.Array, NewArrayFactor.getType().struct);
	}

	@Override
	public void visit(NewFactor NewFactor) {
		report_error("Klase nisu implementirane!", NewFactor.getLine());
		NewFactor.struct = NewFactor.getType().struct;
	}

	@Override
	public void visit(CharFactor CharFactor) {
		CharFactor.struct = Tab.charType;
	}

	@Override
	public void visit(BoolFactor BoolFactor) {
		BoolFactor.struct = boolType;
	}

	@Override
	public void visit(IntFactor IntFactor) {
		IntFactor.struct = Tab.intType;
	}

	boolean listingActPars = false;
	int numActPars = 0;
	List<Struct> currentActPars = null;
	Stack<List<Struct>> actParsStack = new Stack<>();
	Stack<Integer> numActParsStack = new Stack<>();

	@Override
	public void visit(DesignatorFuncCallFactor DesignatorFuncCallFactor) {
		Obj funcObj = DesignatorFuncCallFactor.getDesignator().obj;
		if (funcObj.getKind() != Obj.Meth) {
			report_error("Simbol " + funcObj.getName() + " nije metoda", DesignatorFuncCallFactor.getLine());
			DesignatorFuncCallFactor.struct = Tab.noType;
			return;
		}
		DesignatorFuncCallFactor.struct = funcObj.getType();
		Collection<Obj> formalParams = funcObj.getLocalSymbols();
		if (funcObj == currentMethod) {
			formalParams = Tab.currentScope.getLocals().symbols();
		}
		int processedFormals = 0;
		for (Obj formalParam : formalParams) {
			if (processedFormals >= funcObj.getLevel())
				break;
			if (currentActPars == null || numActPars == 0) {
				report_error("Broj stvarnih parametara ne odgovara broju formalnih parametara",
						DesignatorFuncCallFactor.getLine());
				return;
			}
			Struct param = currentActPars.remove(0);
			numActPars--;
			if (!param.assignableTo(formalParam.getType())) {
				report_error("Tip stvarnog parametra nije kompatibilan sa tipom formalnog parametra",
						DesignatorFuncCallFactor.getLine());
				return;
			}
			processedFormals++;
		}
		if (numActPars > 0) {
			report_error("Broj stvarnih parametara ne odgovara broju formalnih parametara",
					DesignatorFuncCallFactor.getLine());
			return;
		}
		listingActPars = false;
		numActPars = 0;
		currentActPars = null;
		if (!actParsStack.isEmpty()) {
			listingActPars = true;
			currentActPars = actParsStack.pop();
			numActPars = numActParsStack.pop();
		}
	}

	@Override
	public void visit(ActParsDesignatorStatement ActParsDesignatorStatement) {
		Obj funcObj = ActParsDesignatorStatement.getDesignator().obj;
		if (funcObj.getKind() != Obj.Meth) {
			report_error("Simbol " + funcObj.getName() + " nije metoda", ActParsDesignatorStatement.getLine());
			return;
		}
		Collection<Obj> formalParams = funcObj.getLocalSymbols();
		if (funcObj == currentMethod) {
			formalParams = Tab.currentScope.getLocals().symbols();
		}
		int processedFormals = 0;
		for (Obj formalParam : formalParams) {
			if (processedFormals >= funcObj.getLevel())
				break;
			if (currentActPars == null || numActPars == 0) {
				report_error("Broj stvarnih parametara ne odgovara broju formalnih parametara",
						ActParsDesignatorStatement.getLine());
				return;
			}
			Struct param = currentActPars.remove(0);
			numActPars--;
			if (!param.compatibleWith(formalParam.getType())) {
				report_error("Tip stvarnog parametra nije kompatibilan sa tipom formalnog parametra",
						ActParsDesignatorStatement.getLine());
				return;
			}
			processedFormals++;
		}
		if (numActPars > 0) {
			report_error("Broj stvarnih parametara ne odgovara broju formalnih parametara",
					ActParsDesignatorStatement.getLine());
			return;
		}
		listingActPars = false;
		numActPars = 0;
		currentActPars = null;
		if (!actParsStack.isEmpty()) {
			listingActPars = true;
			currentActPars = actParsStack.pop();
			numActPars = numActParsStack.pop();
		}
	}

	@Override
	public void visit(DesignatorFactor DesignatorFactor) {
		Obj designatorObj = DesignatorFactor.getDesignator().obj;
		DesignatorFactor.struct = designatorObj.getType();
	}

	@Override
	public void visit(MulTerm MulTerm) {
		if (!isIntOrEnum(MulTerm.getFactor().struct) || !isIntOrEnum(MulTerm.getTerm().struct)) {
			report_error("Tipovi kod */% operatora nisu int ili enum.", MulTerm.getLine());
			MulTerm.struct = Tab.noType;
			return;
		}
		MulTerm.struct = MulTerm.getFactor().struct;
	}

	@Override
	public void visit(FactorTerm FactorTerm) {
		FactorTerm.struct = FactorTerm.getFactor().struct;
	}

	@Override
	public void visit(AddExpr AddExpr) {
		if (!isIntOrEnum(AddExpr.getTerm().struct) || !isIntOrEnum(AddExpr.getExprTerm().struct)) {
			report_error("Tipovi kod +/- operatora nisu int ili enum.", AddExpr.getLine());
			AddExpr.struct = Tab.noType;
			return;
		}
		AddExpr.struct = AddExpr.getTerm().struct;
	}

	@Override
	public void visit(MinusTermExpr MinusTermExpr) {
		if (MinusTermExpr.getTerm().struct != Tab.intType && MinusTermExpr.getTerm().struct.getKind() != Struct.Enum) {
			report_error("Tipovi kod unarnog - operatora nisu int ili enum.", MinusTermExpr.getLine());
			MinusTermExpr.struct = Tab.noType;
			return;
		}
		MinusTermExpr.struct = MinusTermExpr.getTerm().struct;
	}

	@Override
	public void visit(TermExpr TermExpr) {
		TermExpr.struct = TermExpr.getTerm().struct;
	}

	@Override
	public void visit(ExprWithTerm ExprWithTerm) {
		ExprWithTerm.struct = ExprWithTerm.getExprTerm().struct;
	}

	@Override
	public void visit(ConditionExpr ConditionExpr) {
		if (!ConditionExpr.getExpr().struct.equals(ConditionExpr.getExpr1().struct)) {
			report_error("Tip uslova nije bool", ConditionExpr.getLine());
			ConditionExpr.struct = Tab.noType;
			return;
		}
		ConditionExpr.struct = ConditionExpr.getExpr().struct;
	}

	@Override
	public void visit(CondFactExpr CondFactExpr) {
		if (!CondFactExpr.getExpr().struct.equals(CondFactExpr.getExpr1().struct)) {
			report_error("Tipovi izraza nakon ? nisu isti!", CondFactExpr.getLine());
			CondFactExpr.struct = Tab.noType;
			return;
		}
		CondFactExpr.struct = CondFactExpr.getExpr().struct;
	}

	@Override
	public void visit(ValidAssignmentExpr ValidAssignmentExpr) {
		ValidAssignmentExpr.struct = ValidAssignmentExpr.getExpr().struct;
	}

	@Override
	public void visit(RelopCondFact RelopCondFact) {
		Struct exprType = RelopCondFact.getExprTerm().struct;
		Struct expr1Type = RelopCondFact.getExprTerm1().struct;
		if (!exprType.compatibleWith(expr1Type)) {
			report_error("Tipovi izraza nisu kompatibilni!", RelopCondFact.getLine());
			return;
		}
		if ((exprType.getKind() == Struct.Array || exprType.getKind() == Struct.Class)
				&& !(RelopCondFact.getRelop() instanceof EqualRelop)
				&& !(RelopCondFact.getRelop() instanceof NotEqualRelop)) {
			report_error("Nedozvoljena upotreba operatora " + RelopCondFact.getRelop() + " nad tipom "
					+ structKindToString(exprType.getKind()), RelopCondFact.getLine());
			return;
		}
		RelopCondFact.struct = boolType;
	}

	@Override
	public void visit(ActParsStart ActParsStart) {
		if (listingActPars) {
			actParsStack.push(currentActPars);
			numActParsStack.push(numActPars);
		}
		listingActPars = true;
		numActPars = 0;
		currentActPars = new java.util.ArrayList<>();
	}

	@Override
	public void visit(MultipleExprList MultipleExprList) {
		if (listingActPars) {
			currentActPars.add(MultipleExprList.getExpr().struct);
			numActPars++;
		}
	}

	@Override
	public void visit(SingleExpr SingleExpr) {
		if (listingActPars) {
			currentActPars.add(SingleExpr.getExpr().struct);
			numActPars++;
		}
	}

	@Override
	public void visit(ErrorProneDesignatorAssignment ErrorProneDesignatorAssignment) {
		Obj designatorObj = ErrorProneDesignatorAssignment.getDesignator().obj;
		Struct designatorType = designatorObj.getType();
		if (designatorObj.getKind() != Obj.Var && designatorObj.getKind() != Obj.Elem
				&& designatorType.getKind() != Struct.Enum && designatorObj.getKind() != Obj.Fld) {
			report_error("Nedozvoljena leva strana dodele tipa:" + objKindToString(designatorObj.getKind()),
					ErrorProneDesignatorAssignment.getLine());
			return;
		}
		Struct exprType = ErrorProneDesignatorAssignment.getErrorProneAssignmentExpr().struct;
		if (designatorType.getKind() == Struct.Enum) {
			if (exprType != designatorType && exprType != Tab.nullType) {
				report_error("Nedozvoljena dodela: enum tipovi nisu ekvivalentni",
						ErrorProneDesignatorAssignment.getLine());
				return;
			}
			return;
		}
		if (designatorObj.getType() == Tab.intType && exprType.getKind() == Struct.Enum) {
			return;
		}
		if (!exprType.assignableTo(designatorType)) {
			report_error(
					"Nedozvoljena dodela: nekompatibilni tipovi " + structKindToString(designatorType.getKind()) + " i "
							+ structKindToString(exprType.getKind()),
					ErrorProneDesignatorAssignment.getLine());
			return;
		}
	}

	@Override
	public void visit(IncDesignatorStatement IncDesignatorStatement) {
		Obj designatorObj = IncDesignatorStatement.getDesignator().obj;
		if (designatorObj.getKind() != Obj.Var && designatorObj.getKind() != Obj.Elem
				&& designatorObj.getKind() != Obj.Fld) {
			report_error("Nedozvoljena upotreba ++ operatora nad tipom:" + objKindToString(designatorObj.getKind()),
					IncDesignatorStatement.getLine());
			return;
		}
		if (designatorObj.getType() != Tab.intType) {
			report_error(
					"Nedozvoljena upotreba ++ operatora nad tipom:"
							+ structKindToString(designatorObj.getType().getKind()),
					IncDesignatorStatement.getLine());
			return;
		}
	}

	@Override
	public void visit(DecDesignatorStatement DecDesignatorStatement) {
		Obj designatorObj = DecDesignatorStatement.getDesignator().obj;
		if (designatorObj.getKind() != Obj.Var && designatorObj.getKind() != Obj.Elem
				&& designatorObj.getKind() != Obj.Fld) {
			report_error("Nedozvoljena upotreba -- operatora nad tipom:" + objKindToString(designatorObj.getKind()),
					DecDesignatorStatement.getLine());
			return;
		}
		if (designatorObj.getType() != Tab.intType) {
			report_error(
					"Nedozvoljena upotreba -- operatora nad tipom:"
							+ structKindToString(designatorObj.getType().getKind()),
					DecDesignatorStatement.getLine());
			return;
		}
	}

	private int insideForCounter = 0;
	private int insideSwitchCounter = 0;

	@Override
	public void visit(BreakStmt BreakStmt) {
		if (insideForCounter == 0 && insideSwitchCounter == 0) {
			report_error("Nedozvoljena upotreba break naredbe van petlje ili switch-a", BreakStmt.getLine());
		}
	}

	@Override
	public void visit(ContinueStmt ContinueStmt) {
		if (insideForCounter == 0) {
			report_error("Nedozvoljena upotreba continue naredbe van for petlje", ContinueStmt.getLine());
		}
	}

	@Override
	public void visit(ReadStmt ReadStmt) {
		Obj designatorObj = ReadStmt.getDesignator().obj;
		if (designatorObj.getKind() != Obj.Var && designatorObj.getKind() != Obj.Elem
				&& designatorObj.getKind() != Obj.Fld) {
			report_error("Nedozvoljena upotreba read naredbe nad tipom:" + objKindToString(designatorObj.getKind()),
					ReadStmt.getLine());
			return;
		}
		if (designatorObj.getType() != Tab.intType && designatorObj.getType() != Tab.charType
				&& designatorObj.getType() != boolType) {
			report_error(
					"Nedozvoljena upotreba read naredbe nad tipom:"
							+ structKindToString(designatorObj.getType().getKind()),
					ReadStmt.getLine());
			return;
		}
	}

	@Override
	public void visit(PrintStmt PrintStmt) {
		Struct exprType = PrintStmt.getExpr().struct;
		if (exprType != Tab.intType && exprType != Tab.charType && exprType != boolType) {
			report_error(
					"Nedozvoljena upotreba print naredbe nad tipom:" + structKindToString(exprType.getKind()),
					PrintStmt.getLine());
			return;
		}
	}

	@Override
	public void visit(PrintWithParamStmt PrintWithParamStmt) {
		Struct exprType = PrintWithParamStmt.getExpr().struct;
		if (exprType != Tab.intType && exprType != Tab.charType && exprType != boolType) {
			report_error(
					"Nedozvoljena upotreba print naredbe nad tipom:" + structKindToString(exprType.getKind()),
					PrintWithParamStmt.getLine());
			return;
		}
	}

	@Override
	public void visit(NoRelopCondFact NoRelopCondFact) {
		if (NoRelopCondFact.getExprTerm().struct != boolType) {
			// report_error("Tip uslova nije bool", NoRelopCondFact.getLine());
			NoRelopCondFact.struct = Tab.noType;
			return;
		}
		NoRelopCondFact.struct = NoRelopCondFact.getExprTerm().struct;
	}

	@Override
	public void visit(SingleCondTerm SingleCondTerm) {
		if (SingleCondTerm.getCondFact().struct != boolType) {
			// report_error("Tip uslova nije bool", SingleCondTerm.getLine());
			SingleCondTerm.struct = Tab.noType;
			return;
		}
		SingleCondTerm.struct = SingleCondTerm.getCondFact().struct;
	}

	@Override
	public void visit(MultipleCondTerm MultipleCondTerm) {
		if (MultipleCondTerm.getCondFact().struct != boolType || MultipleCondTerm.getCondTerm().struct != boolType) {
			// report_error("Tip uslova nije bool", MultipleCondTerm.getLine());
			MultipleCondTerm.struct = Tab.noType;
			return;
		}
		MultipleCondTerm.struct = MultipleCondTerm.getCondFact().struct;
	}

	@Override
	public void visit(SingleCondition SingleCondition) {
		if (SingleCondition.getCondTerm().struct != boolType) {
			// report_error("Tip uslova nije bool", SingleCondition.getLine());
			SingleCondition.struct = Tab.noType;
			return;
		}
		SingleCondition.struct = SingleCondition.getCondTerm().struct;
	}

	@Override
	public void visit(MultipleCondition MultipleCondition) {
		if (MultipleCondition.getCondTerm().struct != boolType || MultipleCondition.getCondition().struct != boolType) {
			// report_error("Tip uslova nije bool", MultipleCondition.getLine());
			MultipleCondition.struct = Tab.noType;
			return;
		}
		MultipleCondition.struct = MultipleCondition.getCondTerm().struct;
	}

	@Override
	public void visit(ValidIfCondition ValidIfCondition) {
		if (ValidIfCondition.getCondition().struct != boolType) {
			report_error("Tip uslova nije bool", ValidIfCondition.getLine());
		}
	}

	List<Integer> switchCaseValues = null;
	Stack<List<Integer>> switchCaseValuesStack = new Stack<>();

	@Override
	public void visit(SwitchStart SwitchStart) {
		insideSwitchCounter++;
		if (switchCaseValues != null) {
			switchCaseValuesStack.push(switchCaseValues);
		}
		switchCaseValues = new java.util.ArrayList<>();
	}

	@Override
	public void visit(SwitchStmt SwitchStmt) {
		insideSwitchCounter--;
		if (SwitchStmt.getExpr().struct != Tab.intType && SwitchStmt.getExpr().struct.getKind() != Struct.Enum) {
			report_error("Tip izraza u switch naredbi nije int ili enum", SwitchStmt.getLine());
		}
		switchCaseValues = switchCaseValuesStack.isEmpty() ? null : switchCaseValuesStack.pop();
	}

	@Override
	public void visit(CaseValue CaseValue) {
		int caseValue = CaseValue.getBranchValue();
		if (switchCaseValues.contains(caseValue)) {
			report_error("Vrednost " + caseValue + " je vec dodeljena nekom case-u u okviru ovog switch-a",
					CaseValue.getLine());
		} else {
			switchCaseValues.add(caseValue);
		}
	}

	@Override
	public void visit(ForStart ForStart) {
		insideForCounter++;
	}

	@Override
	public void visit(ForStmt Forstmt) {
		insideForCounter--;
	}

	@Override
	public void visit(ForNonEmptyCondition ForNonEmptyCondition) {
		if (ForNonEmptyCondition.getCondition().struct != boolType) {
			report_error("Tip for uslova nije bool", ForNonEmptyCondition.getLine());
		}
	}
}
