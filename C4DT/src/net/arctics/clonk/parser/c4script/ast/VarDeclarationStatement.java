package net.arctics.clonk.parser.c4script.ast;

import java.util.Arrays;
import java.util.List;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.EntityRegion;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.TypeUtil;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.Variable.Scope;
import net.arctics.clonk.util.ArrayUtil;

import org.eclipse.jface.text.Region;

/**
 * Variable declaration statement, containing multiple variable declarations which optionally include an initial assignment.
 * @author madeen
 *
 */
public class VarDeclarationStatement extends KeywordStatement {
	
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	VarInitialization[] varInitializations;
	private final Scope scope;
	
	public VarDeclarationStatement(List<VarInitialization> varInitializations, Scope scope) {
		super();
		this.varInitializations = varInitializations.toArray(new VarInitialization[varInitializations.size()]);
		this.scope = scope;
		assignParentToSubElements();
	}
	public VarDeclarationStatement(Scope scope, VarInitialization... varInitializations) {
		this(Arrays.asList(varInitializations), scope);
	}
	public VarDeclarationStatement(String varName, ExprElm initialization, int namePos, Scope scope) {
		this(ArrayUtil.list(new VarInitialization(varName, initialization, namePos, namePos+varName.length(), null)), scope);
	}
	@Override
	public String keyword() {
		return scope.toKeyword();
	}
	@Override
	public VarInitialization[] subElements() {
		return varInitializations;
	}
	@Override
	public void setSubElements(ExprElm[] elms) {
		VarInitialization[] newElms = new VarInitialization[elms.length];
		System.arraycopy(elms, 0, newElms, 0, elms.length);
		varInitializations = newElms;
	}
	public final VarInitialization[] variableInitializations() {
		return varInitializations;
	}
	@Override
	public void doPrint(ExprWriter builder, int depth) {
		builder.append(keyword());
		builder.append(" "); //$NON-NLS-1$
		int counter = 0;
		for (VarInitialization var : varInitializations) {
			var.print(builder, depth);
			if (++counter < varInitializations.length)
				builder.append(", "); //$NON-NLS-1$
			else
				builder.append(";"); //$NON-NLS-1$
		}
	}
	@Override
	public EntityRegion entityAt(int offset, C4ScriptParser parser) {
		Function activeFunc = parser.currentFunction();
		if (activeFunc != null) {
			int addToMakeAbsolute = activeFunc.bodyLocation().start() + this.start();
			offset += addToMakeAbsolute;
			for (VarInitialization pair : varInitializations) {
				String varName = pair.name;
				Variable var = activeFunc.findVariable(varName);
				if (var != null && var.isAt(offset))
					return new EntityRegion(var, new Region(var.location().start()-activeFunc.bodyLocation().start(), var.location().getLength()));
			}
		}
		return super.entityAt(offset, parser);
	}
	@Override
	public void reportProblems(C4ScriptParser parser) throws ParsingException {
		super.reportProblems(parser);
		for (VarInitialization initialization : varInitializations)
			if (initialization.variable != null)
				if (initialization.expression != null) {
					IType initializationType = initialization.expression.unresolvedType(parser);
					if (
						initialization.variable.staticallyTyped() &&
						!initialization.variable.type().canBeAssignedFrom(TypeUtil.resolve(initializationType, parser, parser.script()))
					)
						parser.incompatibleTypes(
							initialization.expression,
							initialization.variable.type(), initializationType
						);
					else {
						switch (scope) {
						case VAR: case PARAMETER:
							initializationType = TypeUtil.resolve(initializationType, parser, parser.script());
							break;
						default:
							break;
						}
						new AccessVar(initialization.variable).typingJudgement(initializationType, parser, TypingJudgementMode.Unify);
					}
				}
	}
}