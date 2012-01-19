package net.arctics.clonk.command;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IRegion;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.ast.ControlFlowException;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.ReturnException;
import net.arctics.clonk.parser.c4script.ast.Statement;
import net.arctics.clonk.parser.c4script.ast.evaluate.IEvaluationContext;
import net.arctics.clonk.parser.c4script.ast.evaluate.IVariableValueProvider;

public class InvokableFunction extends Function {
	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	@Override
	public Object invoke(final Object... args) {
		final IVariableValueProvider variableProvider = args != null && args.length > 0 && args[0] instanceof IVariableValueProvider ? (IVariableValueProvider)args[0] : null;
		IEvaluationContext context = new IEvaluationContext() {

			@Override
			public Object[] arguments() {
				return args;
			}

			@Override
			public Function function() {
				return InvokableFunction.this;
			}

			@Override
			public Object valueForVariable(String varName) {
				return variableProvider != null ? variableProvider.valueForVariable(varName) : null;
			}

			@Override
			public void reportOriginForExpression(ExprElm expression, IRegion location, IFile file) {
				
			}

			@Override
			public Script getScript() {
				return InvokableFunction.this.getScript();
			}

			@Override
			public int codeFragmentOffset() {
				return InvokableFunction.this.codeFragmentOffset();
			}

		};
		Object lastEvaluation = null;
		for (Statement s : getCodeBlock().statements()) {
			try {
				lastEvaluation = s.evaluate(context);
			} catch (ReturnException e) {
				return e.getResult();
			} catch (ControlFlowException e) {
				switch (e.getControlFlow()) {
				case BreakLoop:
					return null;
				case Continue:
					break;
				default:
					return null;
				}
			}
		}
		return lastEvaluation;
	}
}