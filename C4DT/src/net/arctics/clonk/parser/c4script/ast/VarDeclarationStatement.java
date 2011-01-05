package net.arctics.clonk.parser.c4script.ast;

import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.DeclarationRegion;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4Function;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.C4Variable;
import net.arctics.clonk.parser.c4script.C4Variable.C4VariableScope;
import net.arctics.clonk.util.ArrayUtil;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

public class VarDeclarationStatement extends KeywordStatement {
	
	public static final class VarInitialization implements IRegion {
		public String name;
		public ExprElm expression;
		public int namePos;
		public C4Variable variableBeingInitialized;
		public VarInitialization(String name, ExprElm expression, int namePos) {
			super();
			this.name = name;
			this.expression = expression;
			this.namePos = namePos;
		}
		public int getEnd() {
			if (expression != null) {
				return expression.getExprEnd();
			} else {
				return namePos + name.length();
			}
		}
		@Override
		public int getLength() {
			return getEnd()-namePos;
		}
		@Override
		public int getOffset() {
			return namePos;
		}
	}
	
	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	private List<VarInitialization> varInitializations;
	private C4VariableScope scope;
	
	@Override
	protected void offsetExprRegion(int amount, boolean start, boolean end) {
		super.offsetExprRegion(amount, start, end);
		if (start) {
			for (VarInitialization i : varInitializations) {
				i.namePos += amount;
			}
		}
	}

	public VarDeclarationStatement(List<VarInitialization> varInitializations, C4VariableScope scope) {
		super();
		this.varInitializations = varInitializations;
		this.scope = scope;
		assignParentToSubElements();
	}
	public VarDeclarationStatement(String varName, ExprElm initialization, int namePos, C4VariableScope scope) {
		this(ArrayUtil.list(new VarInitialization(varName, initialization, namePos)), scope);
	}
	@Override
	public String getKeyword() {
		return scope.toKeyword();
	}
	@Override
	public ExprElm[] getSubElements() {
		List<ExprElm> result = new LinkedList<ExprElm>();
		for (VarInitialization initialization : varInitializations) {
			if (initialization.expression != null)
				result.add(initialization.expression);
		}
		return result.toArray(new ExprElm[0]);
	}
	@Override
	public void setSubElements(ExprElm[] elms) {
		int j = 0;
		for (VarInitialization pair : varInitializations) {
			if (pair.expression != null)
				pair.expression = elms[j++];
		}
	}
	public List<VarInitialization> getVarInitializations() {
		return varInitializations;
	}
	public void setVarInitializations(List<VarInitialization> varInitializations) {
		this.varInitializations = varInitializations;
	}
	@Override
	public void doPrint(ExprWriter builder, int depth) {
		builder.append(getKeyword());
		builder.append(" "); //$NON-NLS-1$
		int counter = 0;
		for (VarInitialization var : varInitializations) {
			builder.append(var.name);
			if (var.expression != null) {
				builder.append(" = "); //$NON-NLS-1$
				var.expression.print(builder, depth+1);
			}
			if (++counter < varInitializations.size())
				builder.append(", "); //$NON-NLS-1$
			else
				builder.append(";"); //$NON-NLS-1$
		}
	}
	@Override
	public DeclarationRegion declarationAt(int offset, C4ScriptParser parser) {
		C4Function activeFunc = parser.getCurrentFunc();
		if (activeFunc != null) {				
			int addToMakeAbsolute = activeFunc.getBody().getStart() + this.getExprStart();
			offset += addToMakeAbsolute;
			for (VarInitialization pair : varInitializations) {
				String varName = pair.name;
				C4Variable var = activeFunc.findVariable(varName);
				if (var != null && var.isAt(offset))
					return new DeclarationRegion(var, new Region(var.getLocation().getStart()-activeFunc.getBody().getStart(), var.getLocation().getLength()));
			}
		}
		return super.declarationAt(offset, parser);
	}
	@Override
	public void reportErrors(C4ScriptParser parser) throws ParsingException {
		super.reportErrors(parser);
		for (VarInitialization initialization : varInitializations) {
			if (initialization.variableBeingInitialized != null && initialization.expression != null) {
				new AccessVar(initialization.variableBeingInitialized).expectedToBeOfType(initialization.expression.getType(parser), parser, TypeExpectancyMode.Force);
			}
		}
	}
}